# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V4__one_teacher_per_student.sql` (design D1). It is a schema migration, which pass 1 may add; the reviewer reviews it as production code at the test gate. Verify the app starts; until 1.2, the tests that assign a second teacher fail on the unique index
- [ ] 1.2 Delete the tests for R3-S7, R4-S2, R4-S12, R5-S3, R5-S4 and R5-S5, and rewrite the tests for R1-S2, R1-S5, R1-S7, R1-S9, R3-S3, R3-S8, R4-S3, R4-S7, R4-S13 and R5-S1 to match `specs/student-enrolment/spec.md` (design D5); verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.3 Write tests for R1-S14 to R1-S18, R3-S9, R4-S14 and R7-S1 to R7-S6, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.4 Run `./gradlew test`; verify R1-S14 to R1-S18, R4-S7, R4-S14 R7-S1, R7-S2, R7-S3 and R7-S4 fail at their THEN step, and every other test passes. R3-S9, R7-S5, R7-S6 and the rewritten tests guard behaviour that already holds under one teacher and pass already; report them to the reviewer

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Assign an existing student with no teacher in registration, with the conflict handling and link issuing of design D2 and the response of D3; verify all R1 tests pass
- [ ] 3.2 Always invalidate the unused set-password link on removal (design D4); verify all R4 tests pass
- [ ] 3.3 Release a deactivated teacher's students through an event handled in enrolment, with the new `delete ... returning student_id` query and the teacher-row lock in registration (design D7); verify all R7 tests (R7-S2 also needs 3.1) and the accounts R6 tests pass
- [ ] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- At archive, move R3, R4 and R5 back after R2 in `openspec/specs/student-enrolment/spec.md`, and change the Purpose to say a student has at most one teacher.
- Mark the PR ready for the owner's review.
