# Design: enrolment-students

## Context

Builds on `accounts`: users with one role, single-use password links, role rules in
`SecurityConfig`, 409 `EMAIL_TAKEN` and a case-insensitive unique index on email. The
teacher creation flow in `TeacherAdminService` is the model for adding a student. See
proposal.md for motivation; the student rule differs only in role and in the new link.

## Goals / Non-Goals

**Goals:** the smallest base that #28, #29 and #30 can extend: the link table, the
package and the teacher role rule.

**Non-Goals:** linking existing accounts, which comes with join links in #29.

## Decisions

### D1. Package
`com.teachdash.enrolment` with `api`, `api.dto`, `domain` and `persistence`. It calls
the accounts `UserRepository` and `PasswordLinkService` and duplicates neither. Accounts
classes it needs become `public` if they are not already.

### D2. API

| Method | Path | Success | Spec |
|---|---|---|---|
| POST | `/api/teacher/students` | 201 `{id, setPasswordLink}` | R1 |
| GET | `/api/teacher/students` | 200 list of `{id, name, email, timeZone}` | R4 |
| GET | `/api/teacher/students/{id}` | 200 `{id, name, email, timeZone}` | R4 |

The request reuses the validation of `CreateTeacherRequest`: name not blank, at most
200; email valid, at most 320; time zone a valid id.

### D3. Role rule
`/api/teacher/**` gets `hasRole("TEACHER")` in `SecurityConfig`, like `/api/admin/**`.
Alternative: method annotations. Rejected: the project keeps role rules in one place.

### D4. One response for every existing email
All existing emails give 409 `EMAIL_TAKEN`, including a student already linked to the
same teacher. A separate "already linked" code is unnecessary and would add one more
way to learn about an account.

### D5. Adding is one transaction
Creating the user, the link row and the password token happens in one transaction, so a
failure leaves no account without a teacher. Two parallel requests for one new email
are resolved by the unique index: the loser gets 409 `EMAIL_TAKEN`, as in
`TeacherAdminService`.

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

No new dependencies.

## Risks / Trade-offs

- [A logged-in teacher can learn that an email is registered from the 409] → Accepted
  with a few trusted teachers. The role and details stay hidden, and admin teacher
  creation behaves the same. Login rate limiting (#12) does not cover this endpoint.
- [Until #29 lands, a student with an account cannot be enrolled at all] → Accepted.
  The API is not in production yet, and #29 is next in the split.
- [A new student's link is lost or expires after 7 days and the teacher cannot issue
  another until #30] → Accepted. Until then the admin can issue a reset link.
