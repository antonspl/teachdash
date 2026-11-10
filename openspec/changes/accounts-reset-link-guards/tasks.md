# Tasks

## 1. Failing tests

No stubs and no migrations are needed. Each test fails at its THEN step, because both
requests return 201 today. Test setup creates users and sessions directly.

- [x] 1.1 In `PasswordLinkTest`, add `[accounts/R8-S4]`: a deactivated teacher with a password and an unused link inserted with `createToken` (design D6); the admin's reset-link request gets 409 `ACCOUNT_DEACTIVATED`, the seeded link is still unused and it is the teacher's only row in `one_time_tokens`. Assert the status and code before the rows; verify it fails at THEN
- [x] 1.2 In `PasswordLinkTest`, add `[accounts/R8-S5]`: create the admin with `createAdmin` and log in with `loggedInAs`, because `loggedInAdmin()` does not return the id; insert an unused reset link for the admin with `createToken` (design D6) and create a second admin with `createAdmin`. The admin's request for their own account gets 409 `OWN_ACCOUNT`, the seeded link is still unused and it is the admin's only row in `one_time_tokens`; then the request for the second admin gets 201 with a link (design D5). Assert the first response before everything else; verify it fails at THEN
- [x] 1.3 Run `scripts/check-scenario-coverage.sh` and verify it reports no missing scenario, then push and post the commit range of this pass in the PR

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR

## 3. Reset-link guards

- [x] 3.1 In `TeacherAdminService.issueResetLink`, add the current admin's id as an argument and the checks in the order of design D1; make the method transactional and load the user with `findByIdForUpdate` (design D3); verify `[accounts/R8-S4]` and `[accounts/R8-S5]` pass
- [x] 3.2 In `AdminController.resetLink`, pass the current user's id from the `Principal` (design D2); verify `[accounts/R8-S1]`, `[accounts/R8-S2]` and `[accounts/R8-S3]` still pass unchanged
- [x] 3.3 Run the full backend test suite and `scripts/check-scenario-coverage.sh`; verify all pass with the approved tests unchanged

## Workflow follow-up

- Code review gate: Sonnet reviewer after pass 2, then the whole-PR review once before asking for the gate; the engineer approves the code in the PR.
- Archive in the same PR after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
