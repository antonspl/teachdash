# Proposal

## Clarifications

Issue #105 (courses R3-R5; R2 belongs to another change and is not used here).

| # | Question | Engineer's answer |
|---|---|---|
| 1 | What does the list return? | `GET /api/teacher/courses` returns all the caller's courses, newest first: `created_at` descending, `id` as the tie-break. No pagination: the 20-per-teacher limit bounds it. Items have the same shape as create's response. |
| 2 | What does view return? | `GET /api/teacher/courses/{id}` returns the course in create's response shape. |
| 3 | What does rename accept? | `PATCH /api/teacher/courses/{id}` with the body `{title}`; the title is required. Only the title changes. Other body fields (for example `instructionLanguage`, `targetLanguage`, `teacherId`) are ignored. Languages never change after creation (constitution §9, courses design D6). A scenario sends a language field and shows it is ignored. |
| 4 | Which title rules does rename use? | Exactly the create rules (`@CourseTitle`, `Normalize.name`): `REQUIRED`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_TYPE`. Titles are unique per teacher ignoring case: a clash is 409 `TITLE_TAKEN` with `errors` `[{field: "title", code: "TITLE_TAKEN"}]`. Renaming a course to its own title, or to a case-only change of it, is allowed (200). |
| 5 | Does rename set `updatedAt`? | Every successful rename sets `updatedAt` from the clock, including a same-title rename (courses-create design D5; `courses` is the first table with that column). `createdAt` never changes. Scenarios advance the test clock so `updatedAt` differs from `createdAt`. |
| 6 | Another teacher's course, unknown or malformed id? | 404 `NOT_FOUND` with no `errors`, on view and rename. The owner is part of the lookup, so existence does not leak (as in enrolment). A malformed id gives 404 before the body is validated (as student-enrolment R14-S3). The list never shows another teacher's courses. |
| 7 | Who may call these endpoints? | Admin and student get 403 `FORBIDDEN`. A teacher deactivated after login and a client with no session get 401 `UNAUTHENTICATED`. This holds for all three endpoints (as courses R1-S15 and student-enrolment R14-S4). |
| 8 | The issue says "`MalformedIdAdvice` moves to common". | Out of date: commit 6df4eeb (error-details) removed it, and `common/ApiExceptionHandler.handleTypeMismatch` already maps a malformed path id to 404 (api-errors R3). Dropped from scope; no code moves. |
| 9 | Which content languages do scenarios cover? | Both: `en` and `ru` titles and courses (a list with a Russian course, a rename to a Cyrillic title, a case-insensitive Cyrillic clash). |
| 10 | Limits? | No new limits. The title limits are those of courses R1 (1-200 characters); the limit of 20 courses per teacher is unchanged. |

## Research angles

Research conclusions from the engineer's session, no further angles were needed:

- **Reuse.** `@CourseTitle`, `Normalize.name`, `CourseResponse.from(Course)` and the translation of `DuplicateKeyException` on `courses_teacher_title_uq` to `TITLE_TAKEN` (create's private code) are reused for rename. Rename relies on the unique index; an application pre-check on `lower(title)` would clash with the course itself unless it excluded `id`.
- **Locking.** Create takes `users.findActiveByIdForUpdate(teacherId)`, which also gives 401 for a deactivated teacher. Rename may take the same lock.
- **Time.** The application sets `updated_at` from `Clock`; tests use `MutableClock`.
- **Code model.** `TeacherStudentController.changeLanguage` is the PATCH model; `/api/teacher/**` already requires TEACHER.

## Why

A teacher who can create courses must be able to see them, open one and fix its title. Lessons, assignments and sharing later hang off these endpoints.

## What Changes

- `GET /api/teacher/courses`: the caller's courses, newest first.
- `GET /api/teacher/courses/{id}`: one course of the caller.
- `PATCH /api/teacher/courses/{id}` with `{title}`: renames the course and sets `updatedAt`.
- No new error codes; `TITLE_TAKEN`, `NOT_FOUND`, `FORBIDDEN` and `UNAUTHENTICATED` exist.
- Repository methods: list by teacher, find by id and teacher, update title and `updated_at`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `courses`: adds R3 (list), R4 (view) and R5 (rename). R2 is reserved for another change.

## Out of scope

- Creating (done, R1), deleting (#131), assigning students (#122).
- Changing course languages: never possible.
- Pagination, search and filters of the list.
- Moving `MalformedIdAdvice` to common (already done by error-details).
- New limits; frontend (#115).

## Impact

- Backend: `CourseController` gains three handlers; `CourseService` gains list, get and rename; `CourseRepository` gains three methods; the `TITLE_TAKEN` translation is shared with create.
- Database: no migration.
- API: three new endpoints, no new codes.
- No new dependencies.
