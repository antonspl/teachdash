# Design: lessons-edit-delete

## Context

See proposal.md. Current state on `main`:

- `lessons` (V14) plus the join table `lesson_courses` with `unique (course_id, position)` DEFERRABLE INITIALLY DEFERRED. `LessonService.create` and `list`, `LessonRepository` on `JdbcClient` (`lockCourse`, `courseExists`, `countInCourse`, `insertLesson`, `insertAtEnd`, `findByCourse`, `mapLesson`), `LessonController` with `POST` and `GET`.
- `CourseService.rename` locks the teacher's user row (`findActiveByIdForUpdate`, 401 if none) and then updates the course row.
- `@Title` and `Normalize.name` are used by `CreateCourseRequest`, `RenameCourseRequest` and `CreateLessonRequest`.
- Teacher-owned data carries `teacher_id`; every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** rename, move and delete a lesson; positions stay dense and 1-based under parallel requests; a simpler schema that matches the decision "a lesson belongs to exactly one course".

**Non-Goals:** several courses per lesson, copying, moving a lesson to another course, parts and items, frontend, OpenAPI.

## Decisions

### D1. Migration `V15__lessons_one_course.sql`

Supersedes the many-to-many parts of lessons-create D1, D5 and D6. A lesson has exactly one course, so the join table goes.

```sql
alter table lessons add column course_id uuid references courses (id) on delete cascade;
alter table lessons add column position int;

update lessons l
set course_id = lc.course_id, position = lc.position
from lesson_courses lc
where lc.lesson_id = l.id;

delete from lessons where id not in (select lesson_id from lesson_courses);

alter table lessons alter column course_id set not null;
alter table lessons alter column position set not null;
alter table lessons add constraint lessons_position_check check (position >= 1);
alter table lessons add constraint lessons_course_position_uq
    unique (course_id, position) deferrable initially deferred;

drop table lesson_courses;
```

- `teacher_id` stays on `lessons` (§8): queries scope by it without a join.
- `lessons_teacher_idx` stays. The unique constraint's index starts with `course_id`, so it serves "lessons of a course in order" and `max(position)`.
- `on delete cascade` on `course_id` means deleting a course deletes its lessons; there is no course delete endpoint today.
- The `delete` guards development databases against lessons without a link, which would break `set not null`. A lesson linked to several courses (not possible through the API today) keeps an arbitrary one of its links: the `update ... from` picks one row. Only V14 is on `main`, with no teachers yet (M2), so the backfill is a safety step for development databases. It is not tested (project rule: no tests that only prove a migration ran); the existing R1 and R2 tests prove the new columns work.
- The version is `V15` now and is renumbered at merge if needed (#98).

Migrations this change adds: `V15__lessons_one_course.sql`.

**Pass 1 exception.** Constitution §1 limits pass 1 production code to stubs and schema migrations. This migration drops `lesson_courses`, so the create and list queries in `LessonRepository` (and the test helpers) cannot stay on the old schema and cannot be split from the migration. The engineer chose to rewrite those queries in pass 1 as a required exception. The repository diff is production code: the test-gate request lists it for review (task 1.1, 1.8).

### D2. Endpoints and requests

In `LessonController`, under `/api/teacher/courses/{courseId}/lessons/{lessonId}`, no `SecurityConfig` change (`/api/teacher/**` is already TEACHER):

- `PATCH` with `RenameLessonRequest(@Title String title)`, normalised with `Normalize.name` in the compact constructor, as `RenameCourseRequest`. 200 with `LessonResponse`.
- `PATCH .../position` with `MoveLessonRequest(position)`. 200 with `LessonResponse`.
- `DELETE`, `@ResponseStatus(NO_CONTENT)`, no body (precedent: `TeacherStudentController`).

Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`, as in create and list (courses R4), so lesson ids go through the same handling as `courseId`.

### D3. `position` input

`MoveLessonRequest.position` is a `@NotNull` integer that accepts any JSON integer, however large. A plain `int` or `Long` would reject a huge value with a type error, but the engineer decided that every integer is clamped, so the field is a `BigInteger` and is clamped in the service (`min(max(p, 1), count)`, using `BigInteger` comparison before narrowing to `int`). Coercion is configured in `StrictStringJsonConfig` or a sibling customizer in `common`: `builder.withCoercionConfig(BigInteger.class, ...)` with `CoercionAction.Fail` for the `String`, `Float` and `Boolean` input shapes, mapped to `INVALID_TYPE` like the title coercion. All JSON floats are rejected, including `2.0` and `1e2`, as well as `1.5` and `"2"` (R4-S4). A number longer than Jackson's number-length limit (1000 digits) is rejected by the parser and gives `MALFORMED_REQUEST`; there is no scenario for it, and R4 says "any integer within the JSON number-length limit". Missing and `null` give `REQUIRED` through `@NotNull`. Broken JSON and an empty body are `MALFORMED_REQUEST` (api-errors R1) by the existing handler. No new error code.

### D4. Locks and flows

Every write below runs in one `@Transactional` method with the same lock order as create (lessons-create D4):

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. `select id from courses where id = :courseId and teacher_id = :teacherId for update`; no row gives 404.

Why this order: a write to `lessons` takes a KEY SHARE lock on the user row through the `teacher_id` foreign key, and `CourseService.rename` locks the user row before the course row. The course row first would deadlock with it. The user row lock also serialises all of one teacher's writes; the course lock keeps the 404 scoping and the position correctness.

Rename takes the same two locks even though it does not change a position: it keeps one lock order for every lesson write, and the returned `position` is then consistent with parallel moves. After the locks:

- **Rename:** `update lessons set title, updated_at = :now where id = :lessonId and course_id = :courseId and teacher_id = :teacherId`; zero rows gives 404; return the row (`findByIdInCourse`). Using the same `now` as `updated_at` in the response, truncated to milliseconds like `create`.
- **Move:** read `position` of the lesson (scoped by course and teacher; none gives 404) and `count(*)` of the course; clamp the target `t` to `1..count`; then
  - `t > old`: `update lessons set position = position - 1 where course_id = :c and position > :old and position <= :t`
  - `t < old`: `update lessons set position = position + 1 where course_id = :c and position >= :t and position < :old`
  - then `update lessons set position = :t, updated_at = :now where id = :lessonId`.
  The shifts do not touch `updated_at`. For `t == old` only the last update runs, so the lesson gets 200 and a new `updatedAt`. The deferrable unique constraint is checked at commit, so the temporary duplicate between the shift and the final update is allowed (lessons-create D5). The range shift is one UPDATE, as the issue says.
- **Delete:** `delete from lessons where id = :lessonId and course_id = :courseId and teacher_id = :teacherId returning position`; no row gives 404; then `update lessons set position = position - 1 where course_id = :courseId and position > :old`. No `updated_at` change. A second delete finds no row: 404.

The create flow keeps its order and checks. Its limit check and `insertAtEnd` move onto the new columns: `count(*) from lessons where course_id = :c`, and one `insert into lessons (..., course_id, position) select ..., coalesce(max(position), 0) + 1 from lessons where course_id = :c`. `findByCourse` selects from `lessons` where `course_id = :c and teacher_id = :t order by position`. Behaviour of R1 and R2 does not change. `insertLesson` and `insertAtEnd` become one repository method, since a lesson cannot exist without a course.

A lesson of the same teacher addressed under another course of hers is 404 for the same reason as a foreign one: the lesson lookup is always scoped by `course_id` and `teacher_id` together, and nothing in the response tells them apart.

Accepted race: the session filter rejects a deactivated teacher first (401). Only when deactivation happens mid-request does step 1 give 401, after input validation, so in that race a 400 can come before the 401. Accepted risk, same as create.

Order of checks, as courses R5 and lessons R1: CSRF and role (403), deactivated (401, from step 1 or the session filter), malformed id (404), input (400), unknown or foreign (404). Input validation runs in the controller before the service, so a blank title or a missing position on an unknown lesson id gives 400 and a malformed id gives 404 first.

### D5. Positions

The invariant of lessons-create D5 holds: in one course the positions are 1..n without gaps, enforced by the locks, with the deferrable unique constraint as a safety net (a violation at commit would be a bug and surfaces as a 500, no code is mapped). Every write that adds, moves or removes a row restores density inside its own transaction. Deleting a lesson now frees a slot under the 100-lesson limit, which closes the gap noted in lessons-create "Out of scope".

The multi-course lock rule of lessons-create D5 (lock several courses in `id` order) no longer applies to lessons, because a lesson has one course. It stays the rule for any future write that touches several parents.

### D6. Language rule

Unchanged in effect: a lesson never leaves its course, so it never meets a course with another (instruction, target) pair. The rule returns with copying (§9).

## Dependencies

None added.

## Risks / Trade-offs

- **Schema change after V14.** Dropping `lesson_courses` removes the future "same lesson in several courses" option; the engineer decided copying replaces sharing. V14 and V15 are both unreleased in practice (no teachers before M2).
- **`BigInteger` for `position`.** Slightly unusual, needed so huge integers clamp instead of failing; the cost is one comparison. Float and string rejection depends on Jackson coercion settings, so R4-S4 is the proof.
- **User row lock for rename.** It serialises a teacher's renames with her other writes. Accepted: one teacher, one editor, and it avoids a second lock order.
- **Race scenarios** (R4-S8, R5-S7) no longer prove the course lock alone, as in lessons-create; they prove that the combined lock order keeps positions dense.
- **Owner consistency.** `lessons.teacher_id` must equal the owner of `course_id`. Only create inserts, with the owner taken from the locked course row; no other write changes either column.
