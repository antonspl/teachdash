# Tasks

## 1. Failing tests

- [x] 1.1 Add `delete` to `TestClient` (design D6); verify it compiles and existing tests still pass
- [x] 1.2 Add stubs for the removal and student-list endpoints in design D1, each returning 501 and marked `STUB`, with no new role rule yet; verify compilation
- [x] 1.3 Write one test per scenario in `specs/student-enrolment/spec.md` (17 scenarios) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [x] 1.4 Run `./gradlew test`; verify every new test fails at its THEN step and all other tests pass. R4-S8, R4-S9, R4-S10 and R5-S7 guard rules that already exist and may pass already: report them to the reviewer
- [x] 1.5 Add a stub for `POST /api/admin/students/lookup` (design D7), returning 501 and marked `STUB`, with no body and no validation; verify compilation
- [x] 1.6 Write tests for R4-S11 and R6-S1 to R6-S8 (9 scenarios) in the same style; verify `scripts/check-scenario-coverage.sh` passes and each fails at its THEN step. R4-S11, R6-S6, R6-S7 and R6-S8 guard behaviour that already exists and may pass already: report them to the reviewer

## 2. Test review

- [x] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval
- [x] 2.2 Reviewer approves the spec changes and the tests from 1.6 in the PR. HARD STOP: no production code for R6 before approval

## 3. Implementation

- [x] 3.1 Implement removing a student (design D2, D4); verify all R4 tests except the student-list checks in R4-S1, R4-S2 and R4-S3 pass
- [x] 3.2 Add the `/api/student/**` role rule and implement the student's list (design D3, D4); verify all R4 and R5 tests pass and no `STUB` comment is left
- [x] 3.3 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged
- [x] 3.4 Implement the admin lookup (design D7); verify all R6 tests pass and no `STUB` comment is left
- [x] 3.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- At archive, add removing a student, a student's list of teachers and the admin's lookup to the `student-enrolment` Purpose in `openspec/specs/student-enrolment/spec.md`.
- Mark the PR ready for the owner's review.
