# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs so tests compile: records and controllers for every endpoint in design D4 returning 501; verify compilation
- [ ] 1.2 Write one test per scenario in `specs/student-enrolment/spec.md` (20 scenarios) with the scenario ID in `@DisplayName` and `// given / when / then`; verify `scripts/check-scenario-coverage.sh` passes
- [ ] 1.3 Run `./gradlew test` and verify every new scenario test fails for the right reason, and all `accounts` tests still pass

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR — HARD STOP, no production code before approval

## 3. Implementation

- [ ] 3.1 Flyway `V3__enrolment.sql`; verify the app starts and migrations apply on top of `accounts`
- [ ] 3.2 Teacher creates/links students and lists, details, removal with per-teacher scoping; verify R1, R4, R5 tests pass
- [ ] 3.3 Join links and joining; verify R2, R3 tests pass
- [ ] 3.4 Teacher reset links, student's teachers, admin's view; verify R6, R7, R8 tests pass and full `./gradlew test` is green

## 4. Review

- [ ] 4.1 Reviewer agent reviews the diff against specs and constitution (focus: per-teacher scoping); confirmed findings fixed; verify suite is green

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR; verify every scenario ID has a matching test before archiving.
- Mark the PR ready; the owner reviews everything and merges.
