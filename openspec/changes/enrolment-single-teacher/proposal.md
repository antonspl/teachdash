# Proposal: enrolment-single-teacher

## Clarifications

Questions asked on 2026-11-05, while planning #29, and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | Many of the problems in #29 and #30 come from a student having several teachers: shared logins, who issued a link, removal races. Is that needed? | No. A student has at most one teacher. Private tutors' students study only with them. Going from one teacher to many later is cheap; going back is hard. Nothing is deployed, so no data needs migrating. |
| 2 | Can a student with no teacher, for example one whose teacher removed them, be registered by another teacher? | Yes. Registering their email assigns the existing account. The student keeps their name, time zone and password. |
| 3 | Should the student's and the admin's lists of a student's teachers become a single object? | No. They stay lists, now with 0 or 1 entries. No client change, and it keeps "spaces" possible. |
| 4 | How many teachers does the platform target? | About 1000. Constitution §8 changes from "a small number, not thousands". |
| 5 | Should a person be able to study with several teachers later? | Maybe, as "spaces": one login, with a separate student profile per teacher. Not in this change. |

## Research

Angles agreed with the owner, and their conclusions:

- **A. Backend and spec impact.** One migration (a unique index on the student in
  `teacher_students`), a rewrite of registration for existing students, and a one-line
  change to removal. Two parallel registrations of the same student are handled by the
  unique index, with no explicit locks. Seven scenarios assume two teachers or a
  student with no teacher, and their tests must change or go.
- **B. Frontend impact.** None. The frontend has no enrolment screens yet, and no text or
  test assumes several teachers.

Assumptions (owner may override during review):

- Registering an existing student with no teacher returns 201, like a new account. The
  response carries a set-password link only when the student has no password.
- When the same teacher sends parallel requests for the same student, the requests that
  lose get 409 `ALREADY_ENROLLED`. When different teachers do, they get
  `STUDENT_EXISTS`.
- Issuing a new set-password link to a student without a password voids any unused link
  they hold, including an admin's reset link, because a user has at most one unused
  link. The new link sets the password in the same way.
- Removed scenarios keep their IDs unused. Scenario IDs are not renumbered.

## Why

A student shared by several teachers makes logins, password links and removal much harder
to get right, and the owner's tutors do not need it. Settling it before #29 removes most
of that change's open problems.

## What Changes

- A student has at most one teacher, enforced in the database.
- A teacher registering the email of a student with no teacher assigns that account. The
  response carries a new set-password link if the student has no password.
- Registering a student of another teacher still returns 409 `STUDENT_EXISTS`.
- Removing a student always invalidates their unused set-password link. There is no
  "last teacher" check any more.
- A removed student can be registered again, by the same or another teacher
  (**BREAKING**: R4-S7 returned 409).
- The lists of a student's teachers return at most one entry.
- Constitution §8: one teacher per student, and about 1000 teachers.

## Out of Scope

- "Spaces" (one login with a student profile per teacher).
- The join link (#29) and teacher-issued reset links (#30). Both are replanned on top of
  this change.
- Telling a teacher that a student already has another teacher, beyond the existing 409.
- UI.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `student-enrolment`: R1 assigns an existing student with no teacher; R3 and R5 lists
  have at most one teacher; R4 removal always invalidates the unused set-password link.

## Impact

- Flyway migration `V4`: a unique index on `teacher_students (student_id)`.
- `StudentRegistrationService`, `StudentRemovalService`, `TeacherStudentRepository`.
- The register response's `setPasswordLink` becomes optional.
- Tests for R1, R3, R4 and R5 change. R4-S2, R4-S12 and R5-S5 and their tests are
  deleted. R1-S14 to R1-S17 and R3-S9 are new.
- `openspec/constitution.md` §8.
- Issues #29 and #30 get smaller.
