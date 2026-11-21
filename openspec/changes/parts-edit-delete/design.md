# Design: parts-edit-delete

## Context

See proposal.md. Current state on `main` (parts-create):

- Table `parts` (`V16`) with `lesson_id` (FK, `ON DELETE CASCADE`), `teacher_id`, `position` (check `>= 1`) and `unique (lesson_id, position) deferrable initially deferred`.
- `PartService.create` and `list`, `PartRepository` on `JdbcClient` (`lockCourse`, `lockLesson`, `lessonExists`, `countInLesson`, `insertAtEnd`, `findByLesson`), `PartController` with `POST` and `GET`.
- `LessonService.rename`, `move` and `delete` are the model for the three new writes.
- Teacher-owned data carries `teacher_id`; every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** rename, move and delete a part; positions stay dense and 1-based under parallel requests; the same behaviour and error shape as lessons R3-R5.

**Non-Goals:** items, files and links of a part, moving a part to another lesson, copying, frontend, OpenAPI.

## Decisions

### D1. Endpoints and requests

In `PartController`, under `/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}`, no `SecurityConfig` change:

- `PATCH` with `RenamePartRequest(@Title String title)`, normalised with `Normalize.name` in the compact constructor, as `RenameLessonRequest`. 200 with `PartResponse`.
- `PATCH .../position` with `MovePartRequest(BigInteger position)`, `@NotNull`. 200 with `PartResponse`.
- `DELETE`, `@ResponseStatus(NO_CONTENT)`, no body.

Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`, as for the course and lesson ids.

### D2. `position` input

As lessons-edit-delete D3: the field is a `BigInteger` so that any integer is clamped (`min(max(p, 1), count)` with `BigInteger` comparison before narrowing to `int`). The strict coercion (`Fail` for String, Float, Boolean and EmptyString shapes, mapped to `INVALID_TYPE`) is already configured for `BigInteger` in `StrictIntegerJsonConfig`; no new config. Missing and `null` give `REQUIRED` through `@NotNull`; broken JSON and an empty body give `MALFORMED_REQUEST`.

### D3. Locks and flows

Every write runs in one `@Transactional` method with the lock order of create (parts-create D3 and D4), in one service helper used by all three:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. The course row `for update`, scoped by teacher (`lockCourse`); no row gives 404.
3. The lesson row `for update`, scoped by course and teacher (`lockLesson`); no row gives 404.
4. The part lookup scoped by `lesson_id`, `teacher_id` and the part id; no row gives 404.

The user row comes first to avoid a deadlock with `CourseService.rename` (a write to `parts` takes a KEY SHARE lock on the user row through the `teacher_id` foreign key). The order user, course, lesson matches the lessons writers (user, course) and a lesson delete that cascades to parts, so no cycle can form. Rename takes the same locks although it changes no position: one lock order for every part write, and the returned `position` is consistent with parallel moves.

After the locks:

- **Rename:** `update parts set title, updated_at = :now where id = :partId and lesson_id = :lessonId and teacher_id = :teacherId`; return the row. `now` is truncated to milliseconds, as in create. `updated_at` is set even when the title is unchanged.
- **Move:** read the part's `position` and `count(*)` of the lesson; clamp the target `t` to `1..count`; then
  - `t > old`: `update parts set position = position - 1 where lesson_id = :l and position > :old and position <= :t`
  - `t < old`: `update parts set position = position + 1 where lesson_id = :l and position >= :t and position < :old`
  - then `update parts set position = :t, updated_at = :now where id = :partId`.
  The shifts do not touch `updated_at`. For `t == old` only the last update runs, so the part gets 200 and a new `updatedAt`. The deferrable unique constraint is checked at commit, so the temporary duplicate between the shift and the final update is allowed.
- **Delete:** `delete from parts where id = :partId and lesson_id = :lessonId and teacher_id = :teacherId returning position`; no row gives 404; then `update parts set position = position - 1 where lesson_id = :lessonId and position > :old`. A second delete finds no row: 404.

No write changes the `updated_at` of the lesson or of the course. A part of another lesson of the same teacher, addressed under the wrong lesson, is 404 for the same reason as a foreign part: the lookup is always scoped by lesson and teacher together.

Order of checks: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404). Input validation runs in the controller before the service, so a blank title or a missing position on an unknown id gives 400, and a malformed id gives 404 first. Accepted race, as for lessons: a deactivation in mid-request gives 401 from step 1 after input validation.

### D4. Delete and child tables

Parts have no child tables yet, so delete removes the part row and closes the gap. Items, files and links add their own tables with `ON DELETE CASCADE` foreign keys to `parts` in their own changes; this change adds no placeholder tables. Deleting a part frees a slot under the 20-part limit.

### D5. Positions

The invariant of parts-create holds: in one lesson the positions are 1..n without gaps, enforced by the locks, with the deferrable unique `(lesson_id, position)` from `V16` as a safety net (a violation at commit would be a bug and surfaces as a 500; no code is mapped). Every write that moves or removes a row restores density inside its own transaction.

The user row lock serialises all of one teacher's writes, so the race scenarios do not prove the lesson lock alone; they prove that the combined lock order keeps positions dense and within the limit.

### D6. Tests and helpers

Tests extend `support.ApiTest`: `RenamePartTest`, `MovePartTest`, `DeletePartTest`. Existing helpers `createPart`, `countParts`, `partPositions`, `Parallel` and `MutableClock` are reused; a small helper reading one part's `updatedAt` (and the `updatedAt` of the lesson and the course) is added if missing.

## Dependencies

None added.

## Risks / Trade-offs

- **User row lock for rename.** It serialises a teacher's renames with her other writes. Accepted: one teacher, one editor, one lock order.
- **No cascade test beyond the part itself.** Child tables do not exist yet; their changes test their own cascade from a part delete.
- **`BigInteger` for `position`.** As for lessons; float and string rejection depends on the existing Jackson coercion, so R4-S4 is the proof.

## Migrations

None. The existing `V16__parts.sql` (deferrable unique `(lesson_id, position)`) is enough.
