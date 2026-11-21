# Proposal

## Clarifications

Issue #108 (with its comment from lessons-create design D5). The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Endpoints and URL? | Nested under the course, like lessons: `POST` and `GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts`. A lesson with the same id cannot be in several courses; moving a lesson to another course is a copy. The table still references only `lesson_id` (no `course_id` column). |
| 2 | Part title? | Field `title`, the same rule as courses R1 and lessons R1 (1-200 characters, at least one letter or digit, normalised). Not unique. |
| 3 | Limit? | At most 20 parts per lesson; the 21st gives 409 `PART_LIMIT_REACHED` (property `teachdash.parts.max-per-lesson=20`). |
| 4 | Order and scope? | Create appends at the end (dense 1-based position). No reorder, rename or delete of parts in this change. |
| 5 | Deleting a lesson? | Deleting a lesson deletes its parts (foreign key `ON DELETE CASCADE`). Added to lessons R5 as a MODIFIED requirement with one scenario (en and ru content). This is a third requirement touched; `parts` itself has R1 (create) and R2 (list) only. |
| 6 | Response? | Per part `{id, title, position, createdAt, updatedAt}`, like lessons (no `lessonId`). List: unpaginated array ordered by position, empty array when none. |
| 7 | Not found and order of checks? | An unknown, malformed or foreign course, an unknown or malformed lesson, or a lesson of the teacher's other course gives 404 `NOT_FOUND`, with no foreign titles in the body. Order, as lessons R1: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign course or lesson (404), limit (409). Ownership is checked before the limit: a foreign lesson with 20 parts gives 404. |
| 8 | Position integrity? | The design picks (decided): a deferrable, initially deferred unique `(lesson_id, position)` with a check `position >= 1`; lock order user row, then course row, then lesson row (`FOR UPDATE`), matching lessons create and lessons delete and move (which lock user, then course), so a parts create racing a lesson delete either runs first and is cascaded, or gets 404. A parallel scenario like lessons R1-S9 is included (17 parts, 6 parallel creates: 3 times 201 with positions 18-20, 3 times 409, positions 1..20 without gaps). |
| 9 | Migration? | The next Flyway version, currently `V16__parts.sql`, renumbered at merge per the issue. |
| 10 | Scenarios? | Cover `en` and `ru` content; test another teacher's access at the course and the lesson levels; admin and student get 403; CSRF; at most about 20 scenarios. |

## Research angles

The engineer agreed no separate research was needed. The research conclusions came from the lessons code (`com.teachdash.lessons`), which this change mirrors:

- JdbcClient repositories, an `api/` controller with `dto/` records using `@Title` and `Normalize.name`, a `domain/` service, a `persistence/` repository.
- The locked user read is `UserRepository.findActiveByIdForUpdate`; the course lock is `LessonRepository.lockCourse`; the lesson-in-course lookup is like `LessonRepository.findByIdInCourse`.
- Insert at the end is one `insert ... select coalesce(max(position), 0) + 1` under the locks. List runs in a read-only transaction with an unlocked lesson-in-course-of-teacher check.
- Tests extend `support.ApiTest`; shared helpers `createPart`, `countParts`, `partPositions`; parallel requests through `support.Parallel.run`.
- The `api-errors` spec needs no change: limit codes are not listed there.

## Why

A lesson is a container; its content is built from parts (and, later, items in parts). Parts and their order must exist before items (#110), reordering and copying.

## What Changes

- Table `parts` (migration `V16__parts.sql`).
- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts` creates a part at the end of the caller's lesson.
- `GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts` lists the parts of the caller's lesson by position.
- New error code `409 PART_LIMIT_REACHED`.
- Lessons R5 (delete) MODIFIED: deleting a lesson deletes its parts.

## Capabilities

### New Capabilities
- `parts`: parts owned by a teacher, ordered inside a lesson (R1 create, R2 list).

### Modified Capabilities
- `lessons`: R5 (a lesson's parts are deleted with it).

## Out of scope

- Renaming, reordering, deleting and copying parts.
- Items and exercises (#110), files (#55), sharing.
- Moving a lesson to another course (a copy, a later change).
- Frontend.

## Impact

- Backend: new `parts` feature package (`api`, `api/dto`, `domain`, `persistence`); one property in `application.properties`.
- Database: migration `V16__parts.sql`, renumbered at merge if needed.
- API: two new endpoints and one new response code; lesson delete now also removes parts (by the foreign key, no code change in the delete path).
- No new dependencies.
