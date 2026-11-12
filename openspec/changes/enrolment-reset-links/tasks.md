# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V6__token_issuer.sql` with a nullable `issued_by` (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [x] 1.2 Add a stub returning 501, marked `STUB`, for `POST /api/teacher/students/{id}/reset-link` (design D3)
- [x] 1.3 Give `ApiTest.createToken` an `issuerId` argument and pass in every existing caller the issuer its scenario describes (design D5); also state the issuer in the setup of R4-S3, R4-S4 and R7-S1 as the delta spec now does. Verify the existing suite still passes
- [x] 1.4 Change the test for accounts R8-S4 in `PasswordLinkTest` to expect code `TARGET_ACCOUNT_DEACTIVATED` (design D7); verify it now fails at that assertion only
- [ ] 1.5 Write tests for student-enrolment R12-S1 to R12-S9, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; create links and sessions in the GIVEN step directly, never through the stubbed endpoint. The WHEN step only captures responses and asserts nothing; where a later call needs the token from the stub's response, read it without failing (use a made-up token when the response has none), so the test fails in THEN and not in WHEN
- [ ] 1.6 Write tests for student-enrolment R4-S15 and R7-S7, creating the teacher's reset link with `createToken` and teacher A as issuer
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new or changed test fails at its THEN step (not in setup), every other test passes, and the coverage script passes. Check in particular that R12-S1, S4, S5 and S9 fail on a THEN assertion and not on reading the token. Report any new test that passes anyway to the engineer (R12-S6, R12-S7 and R12-S8 are rejected before the controller and may pass against the stub)

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Add the issuer argument to `PasswordLinkService.issue`, `OneTimeToken`, `AccountService.createPasswordlessUser`, `TeacherAdminService.createTeacher` and `reactivateTeacher`, and pass the acting user's id from `AdminController` and the registration path (design D2). STOP and ask the engineer before changing the two `issue` stubs in `StudentRegistrationFailureTest` and any direct test caller of the three changed service methods (design D7). Add `V7__token_issuer_required.sql` (design D1); verify the accounts and enrolment tests that passed before still pass
- [ ] 3.2 Add the guarded `PasswordLinkService.issueResetLink(userId, issuerId)` with the shared private insert path and code `TARGET_ACCOUNT_DEACTIVATED`; make `issue` reject `RESET_PASSWORD`; reduce `TeacherAdminService.issueResetLink` to the own-account check plus delegation (design D6); verify all accounts R8 tests pass
- [ ] 3.3 Replace `invalidateLinks(userId, purpose)` with `invalidateLinks(userId, issuerId)` and use it in removal and release (design D4); make removal lock the teacher row first, with a comment explaining the race (design D3); verify all R4 and R7 tests pass
- [ ] 3.4 Implement the teacher reset endpoint with the lock order of design D3 and a comment at the teacher-row lock; verify all R12 tests pass
- [ ] 3.5 Check that no token appears in logs or in `toString` on the new endpoint (constitution §5); fix any that does
- [ ] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged except for those the engineer approved in 3.1

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, add a sentence about reset links to the Purpose of `openspec/specs/student-enrolment/spec.md`.
- Mark the PR ready for the final review by a code owner other than the engineer.
