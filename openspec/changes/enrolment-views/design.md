# Design: enrolment-views

## Context

Builds on `enrolment-students`: table `teacher_students`, `TeacherStudentRepository`,
`TeacherStudentController`, and `MalformedIdAdvice`, which covers all of
`com.teachdash.enrolment.api`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:** two small endpoints on the existing package, with no schema change.

**Non-Goals:** a new response shape for the existing admin and teacher lists.

## Decisions

### D1. API

| Method | Path | Success | Spec |
|---|---|---|---|
| DELETE | `/api/teacher/students/{id}` | 204, no body | R4 |
| GET | `/api/student/teachers` | 200, array of `{id, name}` | R5 |

The student comes from the session, so the second path has no id and one student cannot
ask for another's teachers.

### D2. Removal is one scoped delete

`delete from teacher_students where teacher_id = :teacher and student_id = :student`, and
0 rows means 404 `NOT_FOUND`. This keeps the teacher scope in the query (constitution
section 8), gives the same 404 for "not yours", "no such id" and "already removed", and
needs no read first. Of two parallel removals, one gets 204 and the other 404.

Alternative: load with `findStudent`, then delete. It adds a query and a race, and gains
nothing.

### D3. Student list has its own query and record

A new `TeacherStudentRepository` query joins `users`, filters on `u.active`, and uses the
same order as `findTeachers` (`u.name collate "und-x-icu", u.id`). It returns a new record
`TeacherName(UUID id, String name)` in `enrolment.domain`.

Reusing `findTeachers` and `UserSummary` would need filtering in Java and a mapping, and
a later field added to `UserSummary` could reach students by accident. With its own
record, only the listed fields can reach a student.

### D4. Role rule and controller

Add `.requestMatchers("/api/student/**").hasRole("STUDENT")` to `SecurityConfig`, before
`anyRequest`. Without it, any logged-in user reaches the endpoint.

The removal goes in `TeacherStudentController`. The student list goes in a new
`StudentTeacherController` in `enrolment.api`, so the malformed id advice and package
layout stay as they are. The service layer gets `EnrolmentQueryService.teachersForStudent`
and a new `removeStudent` method, `@Transactional`, on a service that writes assignments.

### D5. Dependencies and migrations

No new dependencies. No Flyway migrations: `teacher_students` already has the primary key
and the `student_id` index these queries use.

### D6. Test support

`TestClient` has no `delete` method, so tests add one, with the CSRF header handled like
`post`. Data and sessions come from the `ApiTest` helpers. Stub for pass 1: both endpoints
return 501, and the role rule from D4 waits for pass 2, so R5-S6 reaches the stub and fails
at its THEN step.

## Risks / Trade-offs

- [A removal made by mistake cannot be undone until #29] → The account stays and an admin
  can still see it. Accepted in the proposal's assumptions.
- [A mistyped email stays taken after removal] → An admin can issue a link, and #29 and
  #30 add other ways. Accepted in clarification 3.
- [A student keeps their session after removal] → They still reach only their own data.
  Changes that add teacher-owned data must check the assignment on every request, as
  constitution section 8 already requires.
- [The admin's teacher list shows inactive teachers and the student's does not] → This is
  intended: the admin manages accounts, the student only needs teachers who can work now.
