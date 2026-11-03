# Design: enrolment-students

## Context

Builds on `accounts`: users with one role, single-use password links, role rules in
`SecurityConfig`, 409 `EMAIL_TAKEN` and a case-insensitive unique index on email. The
teacher creation flow in `TeacherAdminService` is the model for adding a student. See
proposal.md for motivation.

## Goals / Non-Goals

**Goals:** the smallest base that #28, #29 and #30 can extend: the link table, the
package and the teacher role rule.

**Non-Goals:** assigning existing students, which comes with the join link in #29.

## Decisions

### D1. Package
`com.teachdash.enrolment` with `api`, `api.admin`, `api.dto`, `domain` and `persistence`.
`TeacherStudentController` in `api` holds the teacher endpoints, and
`AdminEnrolmentController` in `api.admin` holds the admin endpoints, so the feature stays
one package as the constitution requires. It calls
the accounts `UserRepository` and `PasswordLinkService` and duplicates neither. Accounts
classes it needs become `public` if they are not already.

### D2. API

| Method | Path | Success | Spec |
|---|---|---|---|
| POST | `/api/teacher/students` | 201 `{id, setPasswordLink}` | R1 |
| GET | `/api/teacher/students` | 200 list of `{id, name, email, timeZone, active}` | R2 |
| GET | `/api/teacher/students/{id}` | 200 `{id, name, email, timeZone, active}` | R2 |
| GET | `/api/admin/teachers/{id}/students` | 200 list of `{id, name, email, timeZone, active}` | R3 |
| GET | `/api/admin/students/{id}/teachers` | 200 list of `{id, name, email, timeZone, active}` | R3 |

### D3. Role rule
`/api/teacher/**` gets `hasRole("TEACHER")` in `SecurityConfig`, like `/api/admin/**`.
Alternative: method annotations. Rejected: the project keeps role rules in one place.

Admin reads through its own endpoint, not through `/api/teacher/**`. Alternative:
allowing admin on teacher endpoints. Rejected: every teacher query would need a role
branch, and a mistake there would show one teacher another teacher's students. Teacher
queries stay scoped to the current teacher with no exception.

### D4. Outcomes of registering a student

| Email belongs to | Status | Code |
|---|---|---|
| no account | 201 with id and link | |
| a student not assigned to this teacher | 409 | `STUDENT_EXISTS` |
| a student assigned to this teacher | 409 | `ALREADY_ENROLLED` |
| a teacher or admin | 409 | `EMAIL_TAKEN` |

Separate codes let the UI say what to do next. #29 adds the teacher's join link to the
`STUDENT_EXISTS` response; this change adds no extra field. An existing account never changes here, because it belongs to the student.

### D5. Adding is one transaction
Creating the user, the assignment row and the password token happens in one transaction,
so a failure leaves no account without a teacher. For parallel requests with one new
email, the unique index decides: the loser gets 409 `EMAIL_TAKEN`, as in
`TeacherAdminService`, and a retry gets the exact code.

### D6. Per-teacher scoping
List and details queries join `teacher_students` on the current teacher's id, which
comes from the session principal. A foreign or unknown id gives 404, never 403, so a
teacher cannot probe other teachers' students.

### D7. Flyway migration
- `V3__teacher_students.sql`: `teacher_students` (`teacher_id`, `student_id`,
  `created_at`; primary key on both; both are foreign keys to `users`; index on
  `student_id` for a student's teachers). Lookups by `teacher_id` use the primary key,
  whose first column it is, so a second index would only slow writes. No cascade: accounts are
  never deleted.

The schema does not check that the roles are right. Only the service writes this table,
and a check would need a trigger, which is not worth it here.

### D8. One validation rule per field
Each field's rule is defined once in `common.validation` as a composed constraint:
`@PersonName` (not blank, at most 200, at least one character outside `\p{Z}` and
`\p{C}`) and `@EmailAddress` (not blank, valid, at most
320, no Unicode separator or control or format characters). `@TimeZoneId` also includes not blank. `CreateTeacherRequest`,
`UpdateProfileRequest` and the new request use them, so they cannot drift apart. Errors
stay 400 `VALIDATION_FAILED`. `@ReportAsSingleViolation` makes each constraint's message
the one reported. Scenarios R1-S8 and accounts R11-S1 and R11-S2 guard the refactor. Alternative:
copying the annotations. Rejected: nothing would keep the copies equal.

`LoginRequest` keeps its own email rule: it checks credentials and does not store an
email. A malformed email there must stay an invalid login, not a validation error.

The invisible-character rule uses `\p{Z}` and `\p{C}` rather than ASCII only, so an
internationalized address stays valid. It is shared, so it also applies to admin teacher
creation and `ADMIN_EMAIL`. Addresses with these characters could not be typed at login
anyway.

### D9. Malformed id
The enrolment controllers map a path id that is not a UUID to 404 `NOT_FOUND` with a local
`@ExceptionHandler`. Alternative: a global handler. Rejected: accounts endpoints have
their own behaviour here, and this change does not alter them.

### D10. Whitespace is removed before validation
Compact constructors of `RegisterStudentRequest`, `CreateTeacherRequest`,
`UpdateProfileRequest` and `LoginRequest` strip name and email, keeping null as null. The
`strip()` calls leave the services, so the value checked is the value stored.
`AdminBootstrap` keeps its own strip because it reads environment variables, not requests.

### D11. One account creation method
A public accounts method, `createPasswordlessUser(role, name, email, timeZone)`, inserts the
user, maps a lost race to 409 `EMAIL_TAKEN` and issues the set-password link.
`TeacherAdminService` and `StudentRegistrationService` both call it and join its
transaction. Each still does its own check for an existing email, because their 409 codes
differ.

### D12. Records
`Person` (with `active`) and one `CreatedAccount(id, setPasswordLink)` replace `PersonResponse`,
`RegisteredStudent`, `RegisterStudentResponse`, `CreatedTeacher` and `CreateTeacherResponse`.
Both live in the domain and controllers return them directly, so no record copies another
field by field. The JSON of existing endpoints does not change. This is a small exception
to the constitution's `api/dto` layout, and the owner reviews it here.

### D13. Role check without loading the user
`UserRepository.existsByIdAndRole` answers the admin 404 checks and teacher deactivation, so
these paths no longer load `password_hash`. `reactivate` still loads the teacher, because
it needs the fields.

No new dependencies.

## Risks / Trade-offs

- [A logged-in teacher can learn whether an email is registered and whether it is a
  student from the response] → Accepted with a few trusted teachers, because the teacher
  needs to know what to do next. Details stay hidden until the student joins. Admin
  teacher creation also shows that an email is taken.
- [Until #29 lands, an existing student cannot be assigned to another teacher] →
  Accepted. The API is not in production yet, and #29 is next in the split.
- [A new student's link is lost or expires after 7 days and the teacher cannot issue
  another until #30] → Accepted. Until then the admin can issue a reset link.
