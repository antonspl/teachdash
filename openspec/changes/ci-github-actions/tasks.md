# Tasks

This change has no spec scenarios and no backend tests, so it follows the exception for a change without backend scenarios: no "Failing tests" section and no test review stop. Tests and code are reviewed together at the code gate.

## 1. Coverage script

- [ ] 1.1 Read frontend titles through `vitest list --json` and `playwright test --list --reporter=json` (design D6), leaving the backend text match unchanged. Verify: the script prints `OK` for all scenarios on this branch.
- [ ] 1.2 Exit with a clear error when `frontend/node_modules` is missing or a list command fails, and mention `test.only` when frontend scenarios are missing. Verify: run with `node_modules` temporarily moved aside, and with a syntax error in a temporary test.
- [ ] 1.3 Update the script header comment to match. Verify: it no longer describes a text match or a planned replacement for frontend tests.
- [ ] 1.4 Check detection on this repo's real tests, one case at a time, restoring each: `test.skip` in a `.test.ts`, a `.test.tsx` and an E2E spec; `describe.skip`; a commented-out test. Verify: each gives `MISSING` for that scenario, and the script is green again once restored. Record the results in the PR.

## 2. Workflow

- [ ] 2.1 Add `.github/workflows/ci.yml` with triggers, concurrency, permissions and the `backend`, `frontend` and `coverage` jobs (design D1, D2, D4, D5), with actions pinned by commit SHA checked against `git ls-remote`. Verify: all three are green on the PR.
- [ ] 2.2 Add the `e2e` job with event-dependent Playwright projects (design D3, D4), and set `forbidOnly` in `frontend/playwright.config.ts`. Verify: the PR run uses only `desktop-chromium`, and a `workflow_dispatch` run on the branch runs all 3 projects.
- [ ] 2.3 Add the `ci-ok` aggregator (design D1). Verify: green when all four jobs pass.

## 3. Prove that failures are caught

- [ ] 3.1 On the PR, push temporary commits that break one job at a time (a failing backend test, a lint error, a scenario with no test, a failing E2E test), then revert them with new commits, without force-pushing. Verify: each makes its own job and `ci-ok` red, and the final run is green. Record the run links in the PR.

## 4. Documentation and follow-up issues

- [ ] 4.1 Update constitution section 2 as approved: CI runs `scripts/check-scenario-coverage.sh` and fails on a missing test. Verify: no text says "until CI exists".
- [ ] 4.2 Update `CONTRIBUTING.md`: CI runs the script, the script needs `npm ci` for hand runs, and which red results are expected in the spec and tests phases. Verify: `grep -n "until CI" CONTRIBUTING.md` finds nothing.
- [ ] 4.3 Open an issue for the backend `@Disabled` gap in the coverage script, and link it in the PR. Verify: the issue exists.

## Workflow follow-up

- Code review gate; the reviewer checks the workflow's permissions and pinned SHAs.
- Archive in the same PR. There are no spec deltas, so `openspec/specs/` does not change.
- Mark the PR ready for the owner.
- After the owner merges and asks: confirm `ci-ok` has reported from `main`, add it to the `protect-main` ruleset, and show the result.
- Prove the block with a throwaway draft PR that has a failing check: its merge state is `BLOCKED`. Close the PR and delete its branch. State that admins can still bypass.
- Close #7, with a link to the proof.
