# Tasks

## 1. Failing tests

- [x] 1.1 Add `V3__teacher_students.sql` (design D7); verify migrations apply in the test container
- [x] 1.2 Add a stub controllers for the five endpoints in design D2, each returning 501 and marked `STUB`, with no new role rule yet so tests reach the stub; verify compilation
- [x] 1.3 Add an `ApiTest` helper that links a teacher and a student directly in the database; verify it compiles and existing tests still pass
- [x] 1.4 Write one test per scenario in `specs/student-enrolment/spec.md` (15 scenarios) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify `scripts/check-scenario-coverage.sh` passes
- [x] 1.5 Run `./gradlew test`; verify every new test fails at its THEN step and all other tests pass
- [x] 1.6 Write tests for the scenarios added in the code review (R1-S7, R2-S6, R3-S5); verify the coverage script passes and each fails at its THEN step with all other tests green
- [ ] 1.7 Write tests for the scenarios added from #47 (R1-S8 to R1-S13, R2-S7, R2-S8, R3-S6, R3-S7, accounts R11-S1 to R11-S3) and remove the stale stub comment from `StudentRegistrationTest`; verify the coverage script passes and report which tests fail at THEN and which already pass, because they guard existing behaviour

## 2. Test review

- [x] 2.1 Reviewer approves tests in the PR. HARD STOP: no production code before approval
- [ ] 2.2 Reviewer approves the spec changes and the tests from 1.6 and 1.7 in the PR. HARD STOP: no fix before approval

## 3. Implementation

- [x] 3.1 Add the `/api/teacher/**` role rule to `SecurityConfig`; verify R2-S3 and R2-S5 pass
- [x] 3.2 Add the shared field rules (design D8) and use them in `CreateTeacherRequest` and `UpdateProfileRequest`; verify all accounts tests still pass unchanged
- [x] 3.3 Replace the stub with registering a student (design D1, D2, D4, D5); verify all R1 tests except the list check in R1-S1 pass
- [x] 3.4 Implement the teacher's list and details with per-teacher scoping (design D6); verify all R1 and R2 tests pass
- [x] 3.5 Implement the admin's lists of a teacher's students and a student's teachers; verify all R3 tests pass and no `STUB` comment is left
- [x] 3.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and approved tests are unchanged
- [ ] 3.7 Reject invisible characters in `@EmailAddress` (design D8) and return 404 for malformed ids (design D9); verify R1-S7, R2-S6 and R3-S5 pass, the full suite is green, and approved tests are unchanged
- [ ] 3.8 Add the name rule, `@ReportAsSingleViolation` and whitespace removal (design D8, D10); verify R1-S8, R1-S9 and accounts R11 pass
- [ ] 3.9 Add the active flag, merge the records and add the shared creation method and role check (design D11, D12, D13); verify R3-S7 passes and the JSON of existing endpoints is unchanged
- [ ] 3.10 Fix the style items in #47 (dense line in `TeacherStudentController`, Javadoc in `StudentRegistrationService`); verify the full suite is green and approved tests are unchanged

## 4. Review

- [x] 4.1 Reviewer agent reviews the diff against the spec and constitution (focus: per-teacher scoping, existing accounts unchanged on 409, shared field rules); confirmed findings are fixed and the suite is green
- [ ] 4.2 Reviewer agent reviews the fixes from 3.7 to 3.10; confirmed findings are fixed and the suite is green

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive in the same PR; verify every scenario ID has a matching test before archiving.
- Mark the PR ready; the owner reviews everything and merges.
