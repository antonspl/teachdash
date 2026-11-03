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
| 4 | Should a teacher link an existing student by email, so a student can have several teachers? | Only if no upcoming issue covers it. #29 does: a logged-in student joins another teacher through the join link. So this change keeps the 409. |
| 5 | Should admins use teacher endpoints? | No. Teacher endpoints stay teacher-only, and old R7 moves here from #28 as an admin endpoint. Admin does not add students. |
| 6 | How many requirements fit in one change? | About 6, with no limit on scenarios. The constitution rule changes in its own PR (#43). |
| 7 | Requirement numbers? | No gaps: R1, R2, R3 here. Later changes continue from the next free number. |
| 8 | Field validation? | One rule per field across the backend, and ideally the frontend. The frontend part moves to #44. |

Assumptions (owner may override during review):

- The capability stays `student-enrolment`.
- Issues #28, #29 and #30 stay separate. This change has 3 requirements, so #28 could still
  join it under the new limit if the owner wants that.

## Why

Teachers need students on the platform before courses, homework or schedule make
sense. This is part 1 of the split of #5 (issue #27).

## What Changes

- A teacher adds a student by name, email and time zone. A new account is created and
  linked to the teacher, and the response contains a single-use set-password link.
- Any email that already has an account is rejected with one identical 409.
- A teacher lists their own students and reads the details of one of them. Any other
  id returns 404.
- Teacher endpoints are allowed only for teachers.
- An admin lists any teacher's students.
- The name, email and time zone rules become one shared definition in the backend.

## Out of Scope

- Linking an existing student by email. A student joins another teacher through a join
  link (#29). Until #29 lands, a student who already has an account cannot be enrolled.
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
- #29 becomes the only way to enrol an existing student. #35 should tell the teacher
  to send a join link when adding a student returns 409.
