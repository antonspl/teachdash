# Proposal: enrolment-students

## Clarifications

Questions asked on 2026-11-02 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | A teacher adds an email that already belongs to a student. Link at once, as in the old spec, or reject? | Reject with the same 409 as any other existing email. The teacher sends a join link instead (#29), so the student consents by joining and their details stay hidden. Old R1-S2 is replaced. |
| 2 | That student has not set a password yet. Issue a new set-password link? | No. The link the first teacher already sent keeps working. |
| 3 | Which cases missing from the old spec should be added? | All of them: own student details, admin gets 403, email case ignored, invalid input gets 400. Anything this PR cannot cover moves to another issue. |

Assumptions (owner may override during review):

- The capability stays `student-enrolment`, and requirements keep their old numbers
  R1 and R4, because #28, #29 and #30 refer to the old numbers.
- The change has 11 scenarios, one over the "about 10" guide, because the owner asked
  to cover all the cases above. Nothing had to move to another issue.

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

## Out of Scope

- Linking an existing student. Until #29 lands, a teacher cannot enrol a student who
  already has an account.
- Join links (#29); removing a student, student's teachers and admin view (#28);
  reset links, including a new link when a set-password link is lost or expired (#30).
- UI (#35).
- Paging the student list. A teacher has few students, and the admin's teacher list is
  not paged either.

## Capabilities

### New Capabilities

- `student-enrolment`: links between teachers and students. This change covers adding
  new students and viewing a teacher's own students.

### Modified Capabilities

None.

## Impact

- New package `com.teachdash.enrolment` and endpoints under `/api/teacher/students`.
- New `/api/teacher/**` role rule in `SecurityConfig`.
- New table `teacher_students`.
- #29 becomes the only way to enrol an existing student. #35 should tell the teacher
  to send a join link when adding a student returns 409.
