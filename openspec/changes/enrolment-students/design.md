# Design: enrolment-students

## Context

Builds on `accounts`: users with one role, single-use password links, role rules in
`SecurityConfig`, 409 `EMAIL_TAKEN` and a case-insensitive unique index on email. The
teacher creation flow in `TeacherAdminService` is the model for adding a student. See
proposal.md for motivation.

## Goals / Non-Goals

**Goals:** the smallest base that #28, #29 and #30 can extend: the link table, the
package and the teacher role rule.

**Non-Goals:** changing an existing student's account when linking it.

## Decisions

### D1. Package
`com.teachdash.enrolment` with `api`, `api.dto`, `domain` and `persistence`. It calls
the accounts `UserRepository` and `PasswordLinkService` and duplicates neither. Accounts
classes it needs become `public` if they are not already.

### D2. API

| Method | Path | Success | Spec |
|---|---|---|---|
| POST | `/api/teacher/students` | 201 `{id, setPasswordLink}` | R1 |
| GET | `/api/teacher/students` | 200 list of `{id, name, email, timeZone}` | R2 |
| GET | `/api/teacher/students/{id}` | 200 `{id, name, email, timeZone}` | R2 |
| GET | `/api/admin/teachers/{id}/students` | 200 list of `{id, name, email, timeZone}` | R3 |

### D3. Role rule
`/api/teacher/**` gets `hasRole("TEACHER")` in `SecurityConfig`, like `/api/admin/**`.
Alternative: method annotations. Rejected: the project keeps role rules in one place.

Admin reads through its own endpoint, not through `/api/teacher/**`. Alternative:
allowing admin on teacher endpoints. Rejected: every teacher query would need a role
branch, and a mistake there would show one teacher another teacher's students. Teacher
queries stay scoped to the current teacher with no exception.

### D4. Outcomes of adding a student

| Email belongs to | Status | Code |
|---|---|---|
| no account | 201 with id and link | |
| a student not linked to this teacher | 200 with id | |
| a student linked to this teacher | 409 | `ALREADY_ENROLLED` |
| a teacher or admin | 409 | `EMAIL_TAKEN` |

Linking does not issue a link: a new one would cancel the unused link another teacher
already sent, because an account has one unused link at a time. Name and time zone in
the request apply only to a new account, because they belong to the student.

### D5. Adding is one transaction
Creating the user, the link row and the password token happens in one transaction, so a
failure leaves no account without a teacher. Parallel requests are resolved by unique
keys. For one new email, the loser gets 409 `EMAIL_TAKEN`, as in `TeacherAdminService`,
and a retry links the student. For one existing student and one teacher, the loser gets
409 `ALREADY_ENROLLED`.

### D6. Per-teacher scoping
List and details queries join `teacher_students` on the current teacher's id, which
comes from the session principal. A foreign or unknown id gives 404, never 403, so a
teacher cannot probe other teachers' students.

### D7. Flyway migration
- `V3__teacher_students.sql`: `teacher_students` (`teacher_id`, `student_id`,
  `created_at`; primary key on both; both are foreign keys to `users`; index on
  `student_id`, which #28 needs for a student's teachers). No cascade: accounts are
  never deleted.

The schema does not check that the roles are right. Only the service writes this table,
and a check would need a trigger, which is not worth it here.

### D8. One validation rule per field
Each field's rule is defined once in `common.validation` as a composed constraint:
`@PersonName` (not blank, at most 200) and `@EmailAddress` (not blank, valid, at most
320). `@TimeZoneId` also includes not blank. `CreateTeacherRequest`,
`UpdateProfileRequest` and the new request use them, so they cannot drift apart. Errors
stay 400 `VALIDATION_FAILED`, and existing tests guard the refactor. Alternative:
copying the annotations. Rejected: nothing would keep the copies equal.

`LoginRequest` keeps its own email rule: it checks credentials and does not store an
email. A malformed email there must stay an invalid login, not a validation error.

No new dependencies.

## Risks / Trade-offs

- [A teacher can link any student whose email they know, without consent, and then sees
  the student's name, email and time zone] → Accepted with a few trusted teachers, by
  the owner's decision. Teachers manage the list. #28 lets a teacher undo a wrong link.
- [A logged-in teacher can learn whether an email is registered and whether it is a
  student from the response] → Accepted for the same reason. Admin teacher creation
  also shows that an email is taken.
- [A student added by a second teacher has no password and the first teacher's link is
  lost] → The second teacher gets no link. Reset links (#30) cover this; until then the
  admin can issue one.
- [A new student's link is lost or expires after 7 days and the teacher cannot issue
  another until #30] → Accepted. Until then the admin can issue a reset link.
