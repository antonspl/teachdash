# Proposal: enrolment-students

## Clarifications

Questions asked on 2026-11-02 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | A teacher adds an email that already belongs to a student. Link at once, as in the old spec, or reject? | Reject with the same 409 as any other existing email. The teacher sends a join link instead (#29), so the student consents by joining and their details stay hidden. Old R1-S2 is replaced. |
| 2 | That student has not set a password yet. Issue a new set-password link? | No. The link the first teacher already sent keeps working. |
| 3 | Which cases missing from the old spec should be added? | All of them: own student details, admin gets 403, email case ignored, invalid input gets 400. Anything this PR cannot cover moves to another issue. |

Answers to the owner's spec review comments on 2026-11-03.

| # | Question | Answer |
|---|----------|--------|
| 4 | Should a teacher link an existing student by email, so a student can have several teachers? | Yes. This replaces answer 1. Teachers manage their student list and can add a new or an existing student. Students only log in, see their teachers' materials, join lessons and do exercises. They do not choose or search for teachers. |
| 5 | Should admins use teacher endpoints? | No. Teacher endpoints stay teacher-only, and old R7 moves here from #28 as an admin endpoint. Admin does not add students. |
| 6 | How many requirements fit in one change? | At most 6, and fewer is better, with no limit on scenarios. The constitution rule changes in its own PR (#43). |
| 7 | Requirement numbers? | No gaps: R1, R2, R3 here. Later changes continue from the next free number. |
| 8 | Should the rest of #28 join this change? | No, it stays separate. |
| 9 | May login keep its looser email check? | Yes. |
| 10 | Field validation? | One rule per field across the backend, and ideally the frontend. The frontend part moves to #44. |

Assumptions (owner may override during review):

- The capability stays `student-enrolment`.
- A student already linked to the same teacher gets its own 409 code, `ALREADY_ENROLLED`,
  so the UI can tell the teacher what happened.

## Why

Teachers need students on the platform before courses, homework or schedule make
sense. This is part 1 of the split of #5 (issue #27).

## What Changes

- A teacher adds a student by name, email and time zone. A new email creates an account
  linked to the teacher, and the response contains a single-use set-password link.
- The email of an existing student links that account to the teacher, so a student can
  have several teachers. The account does not change and no link is returned.
- The email of a teacher or admin, or of a student already on the list, gets 409.
- A teacher lists their own students and reads the details of one of them. Any other
  id returns 404.
- Teacher endpoints are allowed only for teachers.
- An admin lists any teacher's students.
- The name, email and time zone rules become one shared definition in the backend.

## Out of Scope

- Admin adding students.
- The same validation rules in the frontend (#44).
- Join links (#29); removing a student and student's teachers (#28);
  reset links, including a new link when a set-password link is lost or expired (#30).
- UI (#35).
- Paging the student list. A teacher has few students, and the admin's teacher list is
  not paged either.

## Capabilities

### New Capabilities

- `student-enrolment`: links between teachers and students. This change covers adding
  new students and viewing a teacher's students.

### Modified Capabilities

None.

## Impact

- New package `com.teachdash.enrolment`, endpoints under `/api/teacher/students`, and
  `GET /api/admin/teachers/{id}/students`.
- `CreateTeacherRequest` and `UpdateProfileRequest` move to shared field rules, with no
  change in behaviour.
- New `/api/teacher/**` role rule in `SecurityConfig`.
- New table `teacher_students`.
- #29 needs another look: a teacher can now add an existing student directly.
