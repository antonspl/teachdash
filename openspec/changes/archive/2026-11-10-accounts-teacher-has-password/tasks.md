# Tasks

## 1. Failing tests

No stubs and no migrations are needed. Each test fails at its THEN step, because the list
items have no `hasPassword` field yet. The R12-S1 and R12-S2 tests build their
setup through the API, with endpoints that already exist; R12-S3 creates users directly.

- [x] 1.1 In `TeacherManagementTest`, add `[accounts/R12-S1]`: a teacher created through the API, without a password, has `hasPassword` equal to `false` in the list; verify it fails at THEN
- [x] 1.2 In `TeacherManagementTest`, add `[accounts/R12-S2]`: a teacher created through the API uses the returned set-password link, then the list shows `hasPassword` equal to `true` and the list item has exactly the expected fields (design D3). Assert `hasPassword` before the field set; verify it fails at THEN on `hasPassword`
- [x] 1.3 In `TeacherManagementTest`, add `[accounts/R12-S3]`: a deactivated teacher with a password has `hasPassword` equal to `true`, and a deactivated teacher without one has `false`; verify it fails at THEN
- [x] 1.4 Run `scripts/check-scenario-coverage.sh` and verify it reports no missing scenario, then push and post the commit range of this pass in the PR

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR

## 3. Teacher list field

- [x] 3.1 Add `hasPassword` to `TeacherResponse`, set from `user.getPasswordHash() != null` (design D1, D2); verify all `accounts/R12-*` tests pass
- [x] 3.2 Run the full backend test suite and `scripts/check-scenario-coverage.sh`; verify all pass with the approved tests unchanged

## Workflow follow-up

- Code review gate: Sonnet reviewer after pass 2, Opus reviewer once before asking for the gate; the engineer approves the code in the PR.
- Archive in the same PR after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
