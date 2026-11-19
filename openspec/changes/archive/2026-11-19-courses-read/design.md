# Design: courses-read

## Context

See proposal.md. State on main after courses-create:

- `courses` (V11) has `teacher_id`, `title`, languages, `created_at`, `updated_at` and the unique index `courses_teacher_title_uq` on `(teacher_id, lower(title))`.
- `CourseController` (package-private, `courses/api`) has `POST /api/teacher/courses`. `CourseService.create` takes `users.findActiveByIdForUpdate(teacherId)`, inserts, and translates a `DuplicateKeyException` on that index to `ApiException.field(CONFLICT, "TITLE_TAKEN", "title")`.
- `CourseResponse.from(Course)` builds the response; `@CourseTitle` and `Normalize.name` define the title rules.
- `common/ApiExceptionHandler.handleTypeMismatch` maps a malformed path id to 404 `NOT_FOUND` without `errors`, before the body is read or validated.

## Goals / Non-Goals

**Goals:** list, view and rename with teacher scoping at the query, one title rule, correct `updatedAt`.

**Non-Goals:** delete, pagination, language changes, frontend.

## Decisions

### D1. Endpoints

`GET /api/teacher/courses` returns a JSON array of `CourseResponse`, ordered `created_at desc, id`. `GET /api/teacher/courses/{id}` returns one `CourseResponse`. Both are in `CourseController`; no `SecurityConfig` change. The list is unbounded in code because create caps a teacher at 20 courses (courses R1); lowering or raising that property needs no change here.

### D2. Owner in the query

`CourseRepository` gains `findByTeacher(teacherId)`, `findByIdAndTeacher(id, teacherId)` and `updateTitle(id, teacherId, title, updatedAt)` (returns the number of updated rows). All have `teacher_id` in the `where` clause, so another teacher's course and an unknown id are the same empty result and become `NOT_FOUND` (no `errors`). Teacher scoping is thus at the query, not a check after loading.

### D3. Rename

`PATCH /api/teacher/courses/{id}`, body `RenameCourseRequest(title)`, with `@CourseTitle` and the title normalised with `Normalize.name` in the compact constructor, as in create. Unknown fields are ignored (Spring Boot default), so languages and `teacherId` cannot change anything: the request has no field for them and the update statement sets only `title` and `updated_at`.

`CourseService.rename(teacherId, id, title)`, `@Transactional`:

1. `users.findActiveByIdForUpdate(teacherId)` (401 for a deactivated teacher; serialises the teacher's writes).
2. `updateTitle(id, teacherId, title, clock.instant().truncatedTo(MILLIS))`; zero rows means 404.
3. A `DuplicateKeyException` on `courses_teacher_title_uq` becomes 409 `TITLE_TAKEN` with `errors`; the translation is extracted from create into one private method used by both.
4. Return the stored course (`findByIdAndTeacher`) as `CourseResponse`.

No application pre-check on `lower(title)`: it would find the course itself on a same-title or case-only rename. The index only rejects another row, so own-title and case-only renames pass, and a race between two renames is decided by the index (parallel scenario). The update always sets `updated_at`, also for an unchanged title (proposal, clarification 5). Order of failures, as in student-enrolment R14: CSRF and role (403), deactivated teacher (401), malformed id (404), input (400), then a foreign or unknown id (404). The malformed-id 404 comes before body validation because the handler declares `@PathVariable UUID id` before `@Valid @RequestBody`, as `TeacherStudentController.changeLanguage` does; a type mismatch on the path variable fails before the body is read.

### D4. Time

`updated_at` comes from the injected `Clock`, like `created_at` (courses-create D5). Tests advance `MutableClock` between creating the course and renaming it.

### D5. No scalar coercion to String

Scalar coercion to `String` is turned off in the shared `ObjectMapper` (Spring Boot Jackson config), so every endpoint rejects a number or boolean where a string is expected with `INVALID_TYPE` (api-errors R1). Today Jackson saves a title of `123` as "123". It is an older gap that create also had; R5-S11 covers both endpoints. Any other endpoint that relied on the coercion is checked in task 4.3.

## Dependencies

None added.

## Migrations

None.

## Risks / Trade-offs

- **Deactivated teachers.** `SessionRevocationFilter` drops an inactive teacher's session before any controller or validation, so a deactivated teacher gets 401 even with an invalid body. The `findActiveByIdForUpdate` row lock in rename only covers the race where the teacher is deactivated after the filter.
- **Same-title rename touches `updatedAt`.** Chosen by the engineer; the row is written even if the title is unchanged.
- **Unbounded list.** Safe only while the per-teacher limit holds; revisit with pagination if the limit grows large.
