# Design: enrolment-views

## Context

Builds on `enrolment-students`: table `teacher_students`, `TeacherStudentRepository`,
`TeacherStudentController`, and `MalformedIdAdvice`, which covers all of
`com.teachdash.enrolment.api`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:** three small endpoints on the existing package, with no schema change.

**Non-Goals:** a new response shape for the existing admin and teacher lists.

## Decisions

### D1. API

| Method | Path | Success | Spec |
|---|---|---|---|
| DELETE | `/api/teacher/students/{id}` | 204, no body | R4 |
| GET | `/api/student/teachers` | 200, array of `{id, name}` | R5 |
| POST | `/api/admin/students/lookup` | 200, one person, as in the admin lists | R6 |

The student comes from the session, so the second path has no id and one student cannot
ask for another's teachers.

### D2. Removal is one scoped delete, then a check for a last removal

`delete from teacher_students where teacher_id = :teacher and student_id = :student`, and
0 rows means 404 `NOT_FOUND`. This keeps the teacher scope in the query (constitution
section 8), gives the same 404 for "not yours", "no such id" and "already removed", and
needs no read first. Of two parallel removals, one gets 204 and the other 404.

Alternative: load with `findStudent`, then delete. It adds a query and a race, and gains
nothing.

If the delete removed a row and the student has no assignment left, the same transaction
invalidates the student's unused SET_PASSWORD link only. A reset link comes from an admin,
not the teacher, so it stays. The transaction makes the two writes one
action, so the removal cannot succeed with the link still live. Removal keeps its own
`StudentRemovalService`, because it now coordinates enrolment and accounts.

### D3. Student list has its own query and record

A new `TeacherStudentRepository` query joins `users`, filters on `u.active`, and uses the
same order as `findTeachers` (`u.name collate "und-x-icu", u.id`). It returns a new record
`TeacherName(UUID id, String name)` in `enrolment.domain`, next to `UserSummary`, so
persistence does not depend on the web layer. #51 updates the layout rule in constitution
section 3.

Reusing `findTeachers` and `UserSummary` would need filtering in Java and a mapping, and
a later field added to `UserSummary` could reach students by accident. With its own
record, only the listed fields can reach a student.

### D4. Role rule and controller

Add `.requestMatchers("/api/student/**").hasRole("STUDENT")` to `SecurityConfig`, before
`anyRequest`. Without it, any logged-in user reaches the endpoint.

The removal goes in `TeacherStudentController`. The student list goes in a new
`StudentTeacherController` in `enrolment.api`, so the malformed id advice and package
layout stay as they are. The service layer gets `EnrolmentQueryService.activeTeacherNamesForStudent`
and `StudentRemovalService.removeStudent`, which is `@Transactional`.

### D5. Dependencies and migrations

No new dependencies. No Flyway migrations: `teacher_students` already has the primary key
and the `student_id` index these queries use, and `users` has the unique index on
`lower(email)`.

### D6. Test support

`TestClient` has no `delete` method, so tests add one, with the CSRF header handled like
`post`. Data and sessions come from the `ApiTest` helpers. Stub for pass 1: the removal and
student-list endpoints return 501, and the role rule from D4 waits for pass 2, so R5-S6 reaches the stub and fails
at its THEN step.

### D7. Admin lookup uses POST

`POST /api/admin/students/lookup` with body `{email}`, in `AdminEnrolmentController`. The
request record uses `@EmailAddress` and `Normalize.strip`, as `RegisterStudentRequest`
does, so the field has one rule. A `TeacherStudentRepository` query matches
`lower(email)` and role student and returns `UserSummary`, without `password_hash`. No match
returns 404 `NOT_FOUND`.

It is POST and not `GET ?email=` because a query string reaches Caddy access logs and
OpenTelemetry traces, and constitution section 5 forbids logging full personal data. The
existing `/api/admin/**` role rule covers it, and CSRF applies as to any POST.

The admin then uses the id with the existing `POST /api/admin/users/{id}/reset-link`.
Issuing a link replaces any older unused one, so no new recovery endpoint is needed.

The pass 1 stub reads no body and validates nothing, so R6-S5 reaches it and fails at its
THEN step.

## Risks / Trade-offs

- [A removal made by mistake cannot be undone until #29] → The account stays. A student
  with a password can still log in and can return through the join link from #29.
- [A mistyped email stays taken after removal] → The admin finds the account (D7), and can
  issue a link for it. Nothing frees the email. Accepted in clarification 3.
- [A student without a password whose link was lost or invalidated cannot use #29 or #30]
  → Joining needs
  a login, and #30 covers only a teacher's own students. The admin recovers them through
  D7 and the reset link (R6-S2).
- [A student removed by mistake before setting a password loses the link] → An admin finds
  them (D7) and issues a reset link (R6-S2).
- [With two teachers, a removed teacher's link can stay live: another teacher remains
  (R4-S12), the other teacher is deactivated, or two parallel removals each see the other's
  row] → Not reachable today: the only way to get an assignment is registering a new
  account, so no student has two teachers through the API. Required in #29, which makes it
  reachable: record who issued each link, and invalidate that teacher's links on any
  removal. R4-S12 changes then.
- [A teacher-issued reset link from #30 must also die on removal] → The same #29 work covers
  it, and this change's SET_PASSWORD limit must be revisited then.
- [A student keeps their session after removal] → They still reach only their own data.
  Changes that add teacher-owned data must check the assignment on every request, as
  constitution section 8 already requires.
- [The admin's teacher list shows inactive teachers and the student's does not] → This is
  intended: the admin manages accounts, the student only needs teachers who can work now.
