# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V4__one_teacher_per_student.sql` (design D1); verify the app starts and the existing suite runs, with only the tests that assign a second teacher failing
- [ ] 1.2 Delete the tests for R4-S2, R4-S12 and R5-S5, and rewrite the tests for R1-S2, R1-S5, R1-S7, R1-S9, R3-S3, R3-S7, R3-S8, R4-S3, R4-S7, R4-S13, R5-S1 and R5-S3 to match `specs/student-enrolment/spec.md` (design D5); verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.3 Write tests for R1-S14 to R1-S17 and R3-S9, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.4 Run `./gradlew test`; verify R1-S14, R1-S15, R1-S16, R1-S17 and R4-S7 fail at their THEN step, and every other test passes. R3-S9 and the rewritten tests guard behaviour that already holds under one teacher and pass already; report them to the reviewer

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Assign an existing student with no teacher in registration, with the conflict handling and link issuing of design D2 and the response of D3; verify all R1 tests pass
- [ ] 3.2 Always invalidate the unused set-password link on removal (design D4); verify all R4 tests pass
- [ ] 3.3 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- At archive, move R4 and R5 back after R3 in `openspec/specs/student-enrolment/spec.md`, and change the Purpose to say a student has at most one teacher.
- Mark the PR ready for the owner's review.
