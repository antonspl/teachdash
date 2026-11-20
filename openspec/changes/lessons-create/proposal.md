# Proposal

## Clarifications

| Question | Engineer's answer |
|---|---|
| Endpoints? | `POST /api/teacher/courses/{courseId}/lessons` (create, 201) and `GET /api/teacher/courses/{courseId}/lessons` (list, JSON array, no pagination, ordered by position ascending). Both are under `/api/teacher/**`, so the existing TEACHER rule applies; no `SecurityConfig` change. |
| How is a lesson stored? | Table `lessons`: id (uuid, generated in the app), `teacher_id` (references `users`, on delete cascade, constitution §8), title (varchar 200), `created_at` and `updated_at` (set from `Clock`, like courses); index on `teacher_id`. No `course_id` and no language columns: the language pair comes from the course. |
| How is a lesson linked to a course? | Only through `lesson_courses`: `lesson_id` and `course_id` (both cascade on delete), `position` (int, at least 1), primary key (`lesson_id`, `course_id`), and a unique (`course_id`, `position`) constraint that is DEFERRABLE INITIALLY DEFERRED. Positions are dense and 1-based per course. |
| Reordering? | Not implemented here. The tables are designed now so later changes need no migration rework; design.md describes the intended move, remove and delete behaviour (for #107 and later). The same pattern (parent row lock, dense position, deferrable unique (parent, position)) is meant for parts (#108) and items (#110). |
| How is position integrity kept on create? | In one transaction, lock the course row (`select ... from courses where id = :courseId and teacher_id = :teacherId for update`). No row gives 404 `NOT_FOUND`, the same for an unknown and for another teacher's course, never 403, and nothing about the foreign course leaks. Under the lock check the limit, then insert the lesson and its `lesson_courses` row at `coalesce(max(position), 0) + 1`. The active-teacher check is a plain read without a lock (deactivated teacher gives 401 `UNAUTHENTICATED`); the course row is the only lock. |
| Order of checks? | As courses R5: CSRF and role (403), deactivated teacher (401), malformed course id (404), input (400), unknown or another teacher's course (404), limit (409). List has no input step. |
| Lesson title rule? | As courses R1: trimmed, 1-200 characters, at least one letter or digit, same pattern, `REQUIRED` / `TOO_LONG` / `INVALID_FORMAT`. One rule per field: the shared annotation `CourseTitle` is renamed to a neutral name (e.g. `Title`) and used by courses and lessons; course behaviour does not change. Lesson titles are not unique (no `TITLE_TAKEN` for lessons). |
| Limits? | At most 100 lessons per course (`teachdash.lessons.max-per-course=100` in `application.properties`); more gives 409 `LESSON_LIMIT_REACHED` with no `errors`. There is no payload limit beyond the title length: a lesson has no other field. The limit applies to every course, including the future "store" course. |
| Response? | `{id, title, position, createdAt, updatedAt}`. No `teacherId`, no `courseId`. A `teacherId` in the create body is ignored; the owner is the principal. |
| Scenarios? | The courses set: trimming, 200 vs 201 characters, `en` and `ru` titles, blank / missing / invalid title, unknown / another teacher's / malformed course id (404, nothing leaked), admin and student 403, anonymous and deactivated 401, body `teacherId` ignored, list order by position, list of another teacher's course 404, empty list, list shows only that course's lessons. About 20 across R1 and R2, including broken JSON, wrong types, CSRF and the order of checks. Teacher scoping is tested at both levels the change touches (course, lesson). |
| Language rule? | A lesson joins only a course with the same (instruction, target) pair. This change satisfies it trivially, because a lesson is created inside its course and has no language of its own. No scenario. |
| Migration version? | The next Flyway version, `V14` (V13 is the latest on `main`). It is renumbered at merge if needed (#98). |

Context, not requirements:

- **Store course.** The engineer decided that lessons without a real course will live in a dummy "store" course (a copyable lessons store) owned by the teacher. That is why `lesson_courses` stays the only link between a lesson and a course. Open question for later: one store course per language pair?
- **Where content lives.** Content does not sit on the lesson: course, lesson, part, item. Exercise data is item JSONB (#110), images and audio are files on our server referenced by items (#55), video is an item type. This change has no content column.
- **Open questions for later changes.**
  - When a lesson with files is copied, are file records duplicated, or shared with a reference count? Decide before item-image-audio, or media-cleanup could delete a shared file.
  - Parts and items must hang off `lesson_id` only, never `course_id`.

## Research angles

The engineer agreed none were needed beyond the archived `courses-create` change, whose code and spec this change mirrors (JdbcClient repositories, row lock plus limit check, `@CourseTitle`, `Parallel` race test). Postgres facts used by the design: a non-deferrable unique constraint is checked row by row, so one `UPDATE` that shifts positions can fail halfway; a deferrable constraint cannot be an `ON CONFLICT` arbiter (not needed here).

## Why

A course is empty until it has lessons. Lessons are the unit a teacher builds, orders and later copies, so the lesson tables, the order inside a course and the first two endpoints (create and list) must exist before parts, items, reordering and sharing.

## What Changes

- Tables `lessons` and `lesson_courses` (migration `V14__lessons.sql`).
- `POST /api/teacher/courses/{courseId}/lessons` creates a lesson at the end of the caller's course.
- `GET /api/teacher/courses/{courseId}/lessons` lists the lessons of the caller's course by position.
- New error code `409 LESSON_LIMIT_REACHED`.
- The validation annotation `CourseTitle` is renamed to a neutral name and reused for lesson titles; no behaviour change for courses.
- Stated exception to "one new table per change": the join table is a lesson's only link to a course.

## Capabilities

### New Capabilities
- `lessons`: lessons owned by a teacher, ordered inside a course (R1 create, R2 list).

### Modified Capabilities
None.

## Out of scope

- Viewing, renaming, reordering, removing from a course, deleting and copying lessons (#107 and later).
- Parts, items and exercises (#108, #110), files (#55), sharing.
- The "store" course and lessons without a course.
- Adding an existing lesson to another course, and the language check that goes with it.
- Frontend.
- Freeing a slot in a full course: until #107 adds delete and remove, a course at 100 lessons cannot get below the limit. Acceptable, because #107 ships before teachers arrive (M2). The limit stays a property.

## Impact

- Backend: new `lessons` feature package (`api`, `api/dto`, `domain`, `persistence`); `@CourseTitle` renamed in `common/validation` and its uses in courses; one property in `application.properties`.
- Database: migration `V14__lessons.sql`, renumbered at merge if needed.
- API: two new endpoints and one new response code.
- No new dependencies.
