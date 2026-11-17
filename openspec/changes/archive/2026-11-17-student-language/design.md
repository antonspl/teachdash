# Design: student-language

## Context

See proposal.md - Why and research angles. Current state on main:

- One `users` table with `role` and `time_zone`; no students table. `User` is immutable,
  built by `User.create(...)`; existing rows change only through targeted updates in
  `UserRepository` (never `save()`).
- New students come from `AccountService.createPasswordlessUser` (teacher registration,
  `StudentRegistrationService.register`) and `AccountService.createUserWithPassword`
  (visitor join, `JoinLinkService.joinAsVisitor`). Both are shared with teacher creation
  and the admin bootstrap. `assignExisting` and `joinAsStudent` touch no account fields.
- `JoinLinkJoinRequest` is validated only for the token; the visitor fields are
  validated through `JoinVisitor` together with it, before the link is checked.
- `UserSummary(id, name, email, timeZone, active)` from `TeacherStudentRepository` backs
  the teacher's student list and details, the admin's teacher's-students list and
  student lookup, and the admin's student's-teachers list (where it describes teachers).
  `UserResponse` backs `GET/PATCH /api/me`; `UpdateProfileRequest(name, timeZone)`.
- Own-student checks: `assignments.findStudent(teacherId, studentId)` then
  `ApiException.notFound()` (`StudentRemovalService`, `StudentResetLinkService`).
- Validation: one annotation per field in `common/validation/`; `FieldErrorCodes` maps
  unknown constraints to `INVALID_FORMAT`.

## Goals / Non-Goals

**Goals:** every student has a native language from a database-enforced list; one
validation rule for it everywhere; teachers and admins unaffected.

**Non-Goals:** translation, display names, courses.

## Decisions

### D1. Migrations `V9__student_language.sql` (pass 1) and `V10__student_language_required.sql` (pass 2)

```sql
create table languages (
    code    varchar(2) primary key check (code ~ '^[a-z]{2}$'),
    content boolean    not null
);
insert into languages (code, content) values
    ('de', false), ('en', true),  ('es', false), ('fr', false), ('it', false),
    ('pl', false), ('pt', false), ('ru', true),  ('sr', false), ('uk', false);

alter table users add column native_language varchar(2) references languages (code);
alter table users add constraint users_native_language_students_only
    check (native_language is null or role = 'STUDENT');
```

V10, with the production code in pass 2:

```sql
update users set native_language = 'en'
    where role = 'STUDENT' and native_language is null;
alter table users add constraint users_student_has_native_language
    check (role <> 'STUDENT' or native_language is not null);
```

The strict check waits for pass 2 because until then the code inserts students without a
language; with it in V9 every registration and join would fail with 500 during the test
gate (same split as V6/V7 in enrolment-reset-links).

- `content` marks course languages (constitution §9: initially `en`, `ru`). courses-create
  (#104) drops its own `languages` table and checks `content = true` in validation; a bare
  foreign key cannot enforce the flag.
- The backfill is for local data only; there are no real students (proposal).
- The checks keep the column null for teachers and admins, so teacher creation and the
  admin bootstrap stay unchanged, and (after V10) make a student without a language
  impossible.
- A new language is a new migration (reviewed in a PR, deployed by CI).

### D2. Language list endpoint

`com.teachdash.languages` (`api`, `persistence`, plus the validator in `api`):
`GET /api/languages` returns `List<LanguageResponse(code, content)>` ordered by code.
`SecurityConfig` adds `requestMatchers(GET, "/api/languages").permitAll()`. A GET needs no
CSRF token. The list is static and small; no cache, no rate limit.

### D3. One validation rule: `@NativeLanguage`

A `ConstraintValidator` in `languages/api` (public) that accepts `null` and otherwise
requires a row in `languages` (any `content`). The request records strip surrounding
whitespace with `Normalize.strip` in their compact constructors, as they do for email
(R13-S1). An array or object is `INVALID_TYPE` through the existing JSON-reading handler
(api-errors R1); Jackson turns a number or boolean into a string, which is then
`INVALID_FORMAT`. It is composed with `@NotBlank`, like name, email and time zone, so a
missing, `null` or blank value is `REQUIRED` and anything else not listed is `INVALID_FORMAT`.
Used on `RegisterStudentRequest.nativeLanguage`, `JoinVisitor.nativeLanguage` and the new
`UpdateStudentLanguageRequest.nativeLanguage`. Because it is Bean Validation, it runs with
the other fields and is reported with them (R13-S3), and on the join path before the link
check (R13-S6). Rejected values are never echoed (api-errors R1).

`JoinLinkJoinRequest` gains a nullable `nativeLanguage`; the logged-in student path
ignores it as it ignores the other visitor fields (R13-S7).

### D4. Creating students

`AccountService.createPasswordlessUser` and `createUserWithPassword` take a nullable
`nativeLanguage`; teacher creation and the bootstrap pass `null`, the two student paths
pass the request value. `User` and `User.create` gain the field. `assignExisting` stays as
it is: an existing account keeps its language (R13-S4), even though the request must still
carry a valid one (the request is validated before the service decides new vs. existing).

### D5. Teacher changes the language

`PATCH /api/teacher/students/{id}` in `TeacherStudentController`, body
`UpdateStudentLanguageRequest(nativeLanguage)`; unknown fields such as `name` are ignored
(Spring Boot default, R14-S1). `StudentLanguageService.change(teacherId, studentId, code)`,
`@Transactional`:

1. `users.findActiveByIdForUpdate(teacherId)`: the teacher row lock, as removal and reset
   links take it, so a removal or deactivation running at the same time cannot interleave.
   No active teacher (deactivated after the session check) is 401 `UNAUTHENTICATED`, as in
   `StudentResetLinkService`.
2. `assignments.findStudent(teacherId, studentId)` or `ApiException.notFound()`.
3. `UserRepository.updateNativeLanguage(studentId, code)` (targeted update).
4. Return the student's `UserSummary`.

The order in R14 follows from the layers: CSRF and role rules in the filter chain, the
deactivated session in `SessionRevocationFilter` (401), then argument resolution in
declaration order: `@PathVariable UUID id` first, as in every existing controller, so a
malformed id is 404 (api-errors R3) whatever the body (R14-S3); then `@Valid` on the body
(400); then step 2 (404).
The lock in step 1 matches reset links; it has no scenario of its own.

### D6. Showing the language

- `UserSummary` gains `nativeLanguage`; the four `TeacherStudentRepository` selects read
  `u.native_language`. The admin's student's-teachers list reuses `UserSummary` for
  teachers; the field is `@JsonInclude(NON_NULL)`, and teachers never have a value
  (D1 check), so it is absent there (R15-S2).
- `UserResponse` gains `nativeLanguage`, `@JsonInclude(NON_NULL)`: present for students,
  absent for teachers and admins (R15-S3). It is also returned by login and by the
  set-password endpoint, which therefore carry it too.
- `UpdateProfileRequest` does not get the field, so `PATCH /api/me` ignores it (R15-S3).

### D7. Tests and helpers

- `ApiTest` keeps `languages` out of the truncation; nothing in tests inserts languages,
  so no reset is needed.
- `ApiTest.createUser` / `createStudent` insert `native_language` (default `en` for
  students, overridable) so the D1 check holds. A new `studentLanguage(id)` reads it back.
- Existing registration and join test bodies get `nativeLanguage`: `JoinLinkTest.joinBody`,
  `JoinLinkRateLimitTest.joinBody`, a new body helper in `StudentRegistrationTest`, and the
  inline bodies in `JoinLinkFailureTest`, `StudentRegistrationFailureTest`,
  `StudentRemovalTest`, `StudentListTest`, `TeacherJoinLinkTest`, `TeacherDeactivationTest`,
  `common/ApiErrorsTest` (R1-S3, R1-S5) and `common/FieldRejectionErrorsTest` (R2-S2, R2-S3,
  R2-S5).
  Without it, those exact `errors` lists and 409s would turn into 400s with a new entry. This changes
  approved tests in body only: the extra field is ignored by the current code, so they keep
  passing in pass 1 and keep passing after pass 2. The engineer reviews these edits at the
  test gate.

## Dependencies

None added.

## Risks / Trade-offs

- **Constitution.** The rule that a student has a native language is in PR #145 (#142).
  This change should merge after it.
- **#104 rework.** courses-create planned its own `languages` table and a
  teachers-and-admins-only endpoint. When it resumes, it reuses this table, drops R1 from
  its spec and adds a `content = true` check.
- **Required field on the existing-student path.** A teacher who re-registers an existing
  student must send a language that is then ignored. Accepted: the request shape is the
  same for both outcomes and the teacher cannot know in advance which one happens.
