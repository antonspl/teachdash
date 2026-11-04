# Proposal: enrolment-views

## Clarifications

Questions asked on 2026-11-04 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | Which fields does a student see for each teacher? | Id and name only. A teacher's email and time zone stay private. |
| 2 | Does a student see a deactivated teacher? | No. The teacher is hidden, and appears again after reactivation. |
| 3 | A teacher removes a student who has no other teacher and never set a password. What happens to the account? | It stays, and the set-password link keeps working. Removal only deletes the assignment. |

Assumptions (owner may override during review):

- Removing a student returns 204 and has no body.
- A student has no way to remove a teacher. Only the teacher's removal is in the issue.
- A removed student keeps their session. Nothing they can reach today depends on the
  assignment, except their own list of teachers.
- Removal has no undo. Registering the student's email again returns 409 `STUDENT_EXISTS`,
  as for any existing student. The student can return through the join link once #29
  builds it.

## Why

A teacher needs to remove a student who has left or was added by mistake, and a student
needs to see who teaches them. This is part 2 of the split of #5 (issue #28).

## What Changes

- A teacher removes a student from their list. Only that assignment is deleted.
- A student lists their own active teachers, with id and name, in name order.
- The student endpoint is allowed only for students, through a new role rule for
  `/api/student/**`.

## Out of Scope

- Deleting or deactivating student accounts.
- Removing teacher-owned student data, such as homework or results, when a student is
  removed. No such data exists yet. The changes that add it (for example #13) decide
  what happens to it.
- Getting a removed student back through a link (#29), and a new link for a mistyped or
  lost one (#30).
- UI (#35, #36).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `student-enrolment`: adds removing a student and a student's own list of teachers.

## Impact

- New endpoints `DELETE /api/teacher/students/{id}` and `GET /api/student/teachers`.
- New `/api/student/**` role rule in `SecurityConfig`.
- `TeacherStudentRepository` gets two new queries. No schema change.
- #35 and #36 use these endpoints.
