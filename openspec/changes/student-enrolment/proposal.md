# Proposal: student-enrolment

## Clarifications

Questions asked on 2026-10-28 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | How does a student get an account? | Via a link from the teacher, or created directly by the teacher. |
| 2 | Is email sending needed? | No. The teacher gets links and sends them by hand. |
| 3 | Student invite link: one-time or shared? | Both: a one-time set-password link for a student the teacher creates, and a shared join link per teacher that can be disabled. |
| 4 | Can a student study with several teachers? | Yes (constitution §8). |
| 5 | Group lessons? | Yes, but groups belong to the schedule change, not this one. |

Defaults chosen by Claude (owner may override during review):

- A teacher can issue a password reset link for their own students. Risk: a teacher
  could take over an account of a student who also studies with another teacher.
  Acceptable with a few trusted teachers.
- Removing a student only removes the link; the account stays.

## Why

Teachers need their students on the platform before courses, homework or schedule
make sense. Depends on the `accounts` change.

## What Changes

- Teacher adds a student by name, email and time zone and gets a one-time
  set-password link; an existing student with that email is just linked.
- Teacher join link: show, regenerate, disable. New visitors register through it;
  logged-in students use it to join one more teacher.
- Teacher sees and removes only their own students; issues reset links for them.
- Student sees their teachers. Admin sees any teacher's students.

## Out of Scope

- UI — `cabinets-ui`.
- Groups (schedule change), invitations by email, student self-registration without
  a teacher link.

## Capabilities

### New Capabilities

- `student-enrolment`: links between teachers and students — adding students, join
  links, student lists per teacher, teachers per student.

### Modified Capabilities

None.

## Impact

- New package `com.teachdash.enrolment`, endpoints under `/api/teacher`, `/api/student`,
  `/api/join`, `/api/admin/teachers/{id}/students`.
- New tables: teacher–student links, join links.
