# Proposal

## Clarifications

| Question | Engineer's answer |
|---|---|
| Can a lesson be in several courses? | No. A lesson belongs to exactly one course. Lessons are never shared between courses; copying (as with courses between teachers) is a later concern. Delete removes the lesson entirely. |
| What does "cascade" mean here? | Nothing hangs off a lesson yet (no parts, items, files or links). Cascade means only the existing foreign keys; later tables bring their own `ON DELETE CASCADE`. |
| Rename title rule? | Reuses the create rule unchanged: `@Title` plus `Normalize.name`, as `CreateLessonRequest` and `RenameCourseRequest`. |
| Schema? | Fold `course_id` (NOT NULL, FK to `courses`, ON DELETE CASCADE) and `position` (int NOT NULL, CHECK >= 1) into `lessons`. Move the unique `(course_id, position)` DEFERRABLE INITIALLY DEFERRED to `lessons`. Migrate existing rows from `lesson_courses`, then drop `lesson_courses`. New Flyway migration, `V15` now, renumbered at merge (#98). Create and list queries are rewritten onto the new columns with unchanged behaviour; the existing R1 and R2 tests must keep passing. No tests that only prove the migration or backfill ran. The design supersedes the many-to-many parts of lessons-create design D1, D5 and D6. |
| Routes? | Under `/api/teacher/courses/{courseId}/lessons/{lessonId}`. R3 rename: `PATCH` `{title}`, 200 with `{id, title, position, createdAt, updatedAt}`. R4 move: `PATCH .../position` `{position}`, 200 with the moved lesson. R5 delete: `DELETE`, 204 with no body; a second delete gives 404. |
| Not found cases? | Another teacher's lesson, an unknown or malformed lesson or course id, and a lesson of the same teacher addressed under the wrong course all give 404 `NOT_FOUND`, no `errors`, nothing leaked, state unchanged. Order of checks as courses R5 and lessons R1: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign (404). |
| `position` validation? | Missing or null: 400 `VALIDATION_FAILED` with `REQUIRED`. A string, an object or a fraction such as 1.5: `INVALID_TYPE`. Any integer, including 0, negative and huge ones, is clamped to 1..count and gives 200. Broken JSON or an empty body: `MALFORMED_REQUEST` (api-errors R1). |
| `updatedAt`? | Rename and move set it on the lesson itself; a move to the same position still gives 200 and bumps it. Lessons shifted by a move or a delete keep their `updatedAt`. |
| Position integrity and locks? | Positions stay dense and 1-based. Move: one UPDATE shifts the range between the old and the new position. Delete: delete the row, then `position - 1` above the old position. Lock order for move and delete, and for rename to stay consistent with `CourseService.rename`: the teacher's user row (`findActiveByIdForUpdate`, 401 if none), then the course row `for update`, scoped by teacher. The order matters: the foreign key check on `users` deadlocks with course rename otherwise. |
| Scenarios and size? | Cover `en` and `ru` titles (courses with `en` and `ru` instruction language, as in `CreateLessonTest`), another teacher's access at course and lesson level, the wrong-course case, and a parallel move and delete race that keeps positions dense. Target: 3 requirements, about 20 scenarios, about 1000 added lines. |

## Research angles

The engineer agreed the research already done was enough: the existing code in `backend/src/main/java/com/teachdash/lessons/` (create and list), migration `V14__lessons.sql`, the archived `lessons-create` design (D1-D6), the test base (`ApiTest`, `TestClient`, `Parallel`) and `CourseService.rename`. Conclusions: the lock order and the deferrable unique constraint from lessons-create carry over unchanged; the only `DELETE` precedent is `TeacherStudentController` (204); no frontend or OpenAPI change is needed.

## Why

A teacher must be able to fix a lesson's title, reorder lessons in a course and remove a lesson. Create and list exist (#106); without edit and delete a course at 100 lessons cannot shrink and a wrong title cannot be fixed. The decision that a lesson belongs to one course makes the join table pointless, so this change also simplifies the schema.

## What Changes

- `PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}` renames a lesson (R3).
- `PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/position` moves a lesson to a position in its course, clamped to 1..count (R4).
- `DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}` deletes a lesson and closes the gap in positions (R5).
- Migration `V15`: `course_id` and `position` move into `lessons`, the deferrable unique constraint moves with them, `lesson_courses` is dropped.
- Create and list queries are rewritten onto the new columns; behaviour of R1 and R2 does not change.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `lessons`: adds R3 rename, R4 move, R5 delete. R1 and R2 keep their text.

## Out of scope

- Adding a lesson to several courses, copying lessons, the "store" course, and the language check that goes with them.
- Parts, items, files and links of a lesson (#108, #110, #55); their delete cascade comes with their own tables.
- Moving a lesson to another course.
- Deleting a course, and any change to course behaviour.
- Frontend and OpenAPI.

## Impact

- Backend: `lessons` package (controller, service, repository, two request records); `LessonResponse` is reused.
- Database: migration `V15__lessons_one_course.sql`, renumbered at merge if needed.
- API: three new endpoints; no new error code.
- No new dependencies.
