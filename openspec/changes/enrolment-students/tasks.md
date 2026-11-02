# Tasks

## 1. Failing tests

- [ ] 1.1 Add `V3__teacher_students.sql` (design D7); verify migrations apply in the test container
- [ ] 1.2 Add a stub controller for the three endpoints in design D2, each returning 501 and marked `STUB`, with no role rule yet so tests reach the stub; verify compilation
- [ ] 1.3 Add an `ApiTest` helper that links a teacher and a student directly in the database; verify it compiles and existing tests still pass
- [ ] 1.4 Write one test per scenario in `specs/student-enrolment/spec.md` (11 scenarios) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.5 Run `./gradlew test`; verify every new test fails at its THEN step and all other tests pass

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Add the `/api/teacher/**` role rule to `SecurityConfig`; verify R4-S3 and R4-S5 pass
- [ ] 3.2 Replace the stub with adding a student (design D1, D2, D4, D5); verify all R1 tests except the list check in R1-S1 pass
- [ ] 3.3 Implement the list and details with per-teacher scoping (design D6); verify all R1 and R4 tests pass and no `STUB` comment is left
- [ ] 3.4 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged

## 4. Review

- [ ] 4.1 Reviewer agent reviews the diff against the spec and constitution (focus: per-teacher scoping, identical 409 responses); confirmed findings are fixed and the suite is green

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR; verify every scenario ID has a matching test before archiving.
- Mark the PR ready; the owner reviews everything and merges.
