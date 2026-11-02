# Tasks

## 1. Failing tests

No stubs and no migrations are needed. Each test fails at its THEN step, because login still returns the old codes.

- [x] 1.1 In `LoginTest`, change R2-S2, R2-S3, R2-S7 and R2-S8 to expect 401 `INVALID_CREDENTIALS`; verify each fails at THEN
- [x] 1.2 In `LoginTest`, add R2-S9 (active, no password) and R2-S10 (deactivated, wrong password); verify each fails at THEN
- [x] 1.3 In `TeacherManagementTest`, change R5-S1 to expect `INVALID_CREDENTIALS` on login before the password is set; verify it fails at THEN
- [x] 1.4 Add R10-S1 with a spy on the `PasswordEncoder` bean (design D4), covering unknown email, active and deactivated accounts with no password, and a 73-character password on an account with a password; verify it fails at THEN
- [x] 1.5 Add the Vitest test `[web-client/R5-S4]`, asserting the `INVALID_CREDENTIALS` message mentions both the incorrect credentials and the link; verify it fails at THEN
- [x] 1.6 Run `scripts/check-scenario-coverage.sh` and verify it reports no missing scenario, then push and post the commit range of this pass in the PR

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR

## 3. Login behaviour

- [x] 3.1 Rewrite `AccountService.authenticate` per design D1-D3 and remove the old codes from it; verify all `accounts/R2-*`, `accounts/R5-S1` and `accounts/R10-S1` tests pass
- [x] 3.2 Confirm `ACCOUNT_DEACTIVATED` tests for R2-S5, R6-S1 and R7-S6, and `WRONG_PASSWORD` tests for R9, still pass unchanged; verify with the full backend test suite

## 4. Client text and local tools

- [x] 4.1 In `frontend/src/i18n/en.json`, add `INVALID_CREDENTIALS` ("The email or password is incorrect. If you have not set a password yet, use the link you were given.") and remove `UNKNOWN_EMAIL` and `PASSWORD_NOT_SET`; verify `[web-client/R5-S4]` and the full frontend test command pass
- [ ] 4.2 Update the login expectations in `backend/local-check.sh` (wrong password, unknown email, login before the password is set, old password) and the example in `backend/local.http`; verify `local-check.sh` passes against the local stack
- [x] 4.3 Run `scripts/check-scenario-coverage.sh` and the full backend and frontend test suites; verify all pass

## Workflow follow-up

- Code review gate: Sonnet reviewer after pass 2, Opus reviewer once before asking for the gate; a reviewer approves the code in the PR.
- Archive in the same PR after confirming every scenario ID has a matching test.
- Mark the PR ready for the owner's review.
