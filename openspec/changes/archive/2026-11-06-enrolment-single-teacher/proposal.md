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

Question from the spec critique on 2026-11-05, and the owner's answer.

| # | Question | Answer |
|---|----------|--------|
| 6 | A deactivated teacher cannot log in to remove their students, so those students could never get another teacher. What should happen? | Deactivating a teacher removes all their assignments, as if the teacher removed each student. Reactivation does not restore them. |
| 7 | Registration locks the teacher's row so a parallel deactivation cannot leave an inactive teacher with a student. No scenario can test that timing. Keep it? | Yes, without a scenario. A code comment explains the lock, and the reviewer checks it at the code gate. |

Question during the tests on 2026-11-06, and the owner's answer.

| # | Question | Answer |
|---|----------|--------|
| 8 | The coverage script fails on the six removed scenarios until archive. Accept that, or fix the script? | Fix the script in this PR (design D8). |

## Research

Angles agreed with the owner, and their conclusions:

- **A. Backend and spec impact.** One migration (a unique index on the student in
  `teacher_students`), a rewrite of registration for existing students, and a one-line
  change to removal. Two parallel registrations of the same student are handled by the
  unique index. The research expected no explicit locks; the reviews later added row
locks against races with deactivation and password setting (design D2, D7). Seven scenarios assume two teachers or a
  student with no teacher, and their tests must change or go.
- **B. Frontend impact.** None. The frontend has no enrolment screens yet, and no text or
  test assumes several teachers.

Assumptions (owner may override during review):

- Registering an existing student with no teacher returns 201, like a new account. The
  response carries a set-password link only when the student has no password.
- When teachers send parallel requests to register the same existing student, the
  winning teacher's other requests get 409 `ALREADY_ENROLLED` and the other teacher's get
  `STUDENT_EXISTS`. For a new email, the race loser still gets `EMAIL_TAKEN`, as today.
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
- Deactivating a teacher releases all their students (**BREAKING**: a reactivated teacher
  had their students back).
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
  have at most one teacher; R4 removal always invalidates the unused set-password link;
  new R7: deactivating a teacher releases their students.
- `accounts`: R6 says deactivation also releases the teacher's students.

## Impact

- Flyway migration `V4`: a unique index on `teacher_students (student_id)`.
- `StudentRegistrationService`, `StudentRemovalService`, `TeacherStudentRepository`, and
  teacher deactivation in `TeacherAdminService`.
- The register response's `setPasswordLink` becomes optional.
- Tests for R1, R3, R4 and R5 change. R4-S2, R4-S12 and R5-S5 and their tests are
  deleted, and so are R3-S7, R5-S3 and R5-S4, because a deactivated teacher has no
  students. R1-S14 to R1-S18, R3-S9, R4-S14 and R7-S1 to R7-S6 are new.
- `openspec/constitution.md` §8.
- `scripts/check-scenario-coverage.sh` skips scenarios of removed requirements (design D8).
- Issues #29 and #30 get smaller.
