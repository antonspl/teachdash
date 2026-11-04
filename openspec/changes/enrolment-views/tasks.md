# Tasks

## 1. Failing tests

- [x] 1.1 Add `delete` to `TestClient` (design D6); verify it compiles and existing tests still pass
- [x] 1.2 Add stubs for both endpoints in design D1, each returning 501 and marked `STUB`, with no new role rule yet; verify compilation
- [x] 1.3 Write one test per scenario in `specs/student-enrolment/spec.md` (17 scenarios) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [x] 1.4 Run `./gradlew test`; verify every new test fails at its THEN step and all other tests pass. R4-S8, R4-S9, R4-S10 and R5-S7 guard rules that already exist and may pass already: report them to the reviewer

## 2. Test review

- [x] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 Implement removing a student (design D2, D4); verify all R4 tests except the student-list checks in R4-S1, R4-S2 and R4-S3 pass
- [ ] 3.2 Add the `/api/student/**` role rule and implement the student's list (design D3, D4); verify all R4 and R5 tests pass and no `STUB` comment is left
- [ ] 3.3 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- At archive, add removing a student and a student's list of teachers to the `student-enrolment` Purpose in `openspec/specs/student-enrolment/spec.md`.
- Mark the PR ready for the owner's review.
