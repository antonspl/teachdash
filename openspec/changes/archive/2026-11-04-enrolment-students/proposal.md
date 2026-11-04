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
| 4 | How is a student assigned to a teacher? | Two ways, replacing answers 1 and 2. (a) A teacher registers a new student, who is assigned at once. (b) A student assigns themselves through one general link per teacher. A new visitor registers through it and is assigned at once. A teacher who adds an existing student gets that link. There is no teacher search. Way (b) stays in #29; until then, an existing student gets 409 `STUDENT_EXISTS`. |
| 5 | Should admins use teacher endpoints? | No. Teacher endpoints stay teacher-only, and old R7 moves here from #28 as an admin endpoint. Admin does not add students. |
| 6 | How many requirements fit in one change? | At most 6, and fewer is better, with no limit on scenarios. The constitution rule changes in its own PR (#43). |
| 7 | Requirement numbers? | No gaps: R1, R2, R3 here. Later changes continue from the next free number. |
| 8 | Should the rest of #28 join this change? | No, it stays separate. |
| 9 | May login keep its looser email check? | Yes. |
| 10 | Field validation? | One rule per field across the backend, and ideally the frontend. The frontend part moves to #44. |

Assumptions (owner may override during review):

- The capability stays `student-enrolment`.
- Each 409 case has its own code, so the UI can tell the teacher what to do next.
- The active flag is on every person in the lists, for teachers too, because one response
  shape is simpler. Today a student is always active.

Answers to the owner's test review comments on 2026-11-03.

| # | Question | Answer |
|---|----------|--------|
| 11 | Where do admin endpoints go? | In their own controller under an admin package. An admin can also list a student's teachers. |

Answers to the owner's code review on 2026-11-03.

| # | Question | Answer |
|---|----------|--------|
| 12 | A malformed id returns 400, but R2 and R3 require 404. Which is right? | The spec: 404. |
| 13 | Emails with invisible characters pass validation. Should they? | No. It breaks R1-S6 and creates look-alike accounts, so it is fixed here. |
| 14 | Should the other findings stay in #47? | No. Fix everything from #47 in this PR and close the issue. |
| 15 | Should the Purpose mention the link? | Not until #29 builds it. |

Second code review on 2026-11-04. Spec violations are fixed here: name order (R2, R3) and
name characters (R11). Other findings go to #48, as constitution section 1 step 8 says.

## Why

Teachers need students on the platform before courses, homework or schedule make
sense. This is part 1 of the split of #5 (issue #27).

## What Changes

- A teacher registers a student by name, email and time zone. A new email creates an
  account assigned to the teacher, and the response contains a single-use set-password
  link.
- Any email that already has an account gets 409, with a code that says why.
- A teacher lists their own students and reads the details of one of them. Any other
  id returns 404.
- Teacher endpoints are allowed only for teachers.
- An admin lists any teacher's students and any student's teachers.
- The name, email and time zone rules become one shared definition in the backend, and
  whitespace around a name or email is removed before they are checked.
- Admin lists show whether a person is active.
- Account creation moves to one accounts method, and redundant records are removed.

## Out of Scope

- Admin adding students.
- The same validation rules in the frontend (#44).
- The general join link, including returning it when a student already exists (#29); removing a student and student's teachers (#28);
  reset links, including a new link when a set-password link is lost or expired (#30).
- UI (#35).
- Paging the student list. A teacher has few students, and the admin's teacher list is
  not paged either.

## Capabilities

### New Capabilities

- `student-enrolment`: links between teachers and students. This change covers adding
  new students and viewing a teacher's students.

### Modified Capabilities

- `accounts`: one rule for name, email and time zone, including whitespace handling.

## Impact

- New package `com.teachdash.enrolment`, endpoints under `/api/teacher/students`, and
  `GET /api/admin/teachers/{id}/students` and `GET /api/admin/students/{id}/teachers`.
- `CreateTeacherRequest` and `UpdateProfileRequest` move to shared field rules, with no
  change in behaviour.
- New `/api/teacher/**` role rule in `SecurityConfig`.
- New table `teacher_students`.
- #29 adds the join link to the `STUDENT_EXISTS` response.
