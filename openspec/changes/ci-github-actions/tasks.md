# Tasks

This is a tooling change with `skip_specs: true`, under the rule that task 4.1 adds (design D8). The spec gate covers the proposal and design. There is no "Failing tests" section and no test review stop. Proof is recorded in the PR and reviewed at the code gate.

## 1. Coverage script

- [x] 1.1 Read frontend titles through `vitest list --json` and `playwright test --list --reporter=json` (design D6), leaving the backend text match unchanged. Verify: the script prints `OK` for all scenarios on this branch.
- [x] 1.2 Exit with a clear error when `frontend/node_modules` is missing or a list command fails, and mention `test.only` when frontend scenarios are missing. Verify: run with `node_modules` temporarily moved aside, and with a syntax error in a temporary test.
- [x] 1.3 Update the script header comment to match. Verify: it no longer describes a text match or a planned replacement for frontend tests.
- [x] 1.4 Check detection on this repo's real tests, one case at a time, restoring each: `test.skip` in a `.test.ts`, a `.test.tsx` and an E2E spec; `describe.skip`; a commented-out test. Verify: each gives `MISSING` for that scenario, and the script is green again once restored. Separately, move a scenario's only test into a nested `describe` temporarily. Verify: it still counts as covered. Record the results in the PR.

## 2. Workflow

- [x] 2.1 Add `.github/workflows/ci.yml` with triggers, concurrency, permissions and the `backend`, `frontend` and `coverage` jobs (design D1, D2, D4, D5), with actions pinned by commit SHA, checked against the version's peeled tag (`git ls-remote` with `^{}`). Verify: all three are green on the PR, and the `backend` job's reports show tests actually ran.
- [x] 2.2 Add the `e2e` job with event-dependent Playwright projects (design D3, D4), and set `forbidOnly` in `frontend/playwright.config.ts`. Verify: the PR run uses only `desktop-chromium` (the all-projects path is proven in 3.2).
- [x] 2.3 Add the `ci-ok` aggregator (design D1). Verify: green when all four jobs pass (red cases are proven in 3.1).

## 3. Prove that failures are caught

- [ ] 3.1 On the PR, push temporary commits that break one job at a time (a failing backend test, a lint error, a format error, a type error, a temporary `spec.md` with a scenario that has no test, a failing E2E test), then revert them with new commits, without force-pushing. Verify: each makes its own job and `ci-ok` red, and the final run is green. Record the run links in the PR.
- [ ] 3.2 In the same way, push a temporary `push` trigger for this branch. Verify: that run installs Chromium and WebKit and runs all 3 projects. A real phone failure here does not block this change: open an issue for it and report it to the owner. Record the run link in the PR.
- [ ] 3.3 In the same way, push a temporary `test.only` in Vitest and in Playwright. Verify: each makes its job and `ci-ok` red. Record the run links in the PR.

## 4. Documentation and follow-up issues

- [ ] 4.1 Update the constitution as approved: section 2 says CI runs `scripts/check-scenario-coverage.sh` and fails on a missing test, and section 1 gains the tooling rule (design D8). Add the matching exception to `openspec/config.yaml`. Verify: `grep -in "until CI" openspec/constitution.md` finds nothing, and `openspec validate ci-github-actions` passes.
- [ ] 4.2 Update `CONTRIBUTING.md`: CI runs the script, the script needs `npm ci` for hand runs, which red results are expected in the spec and tests phases, and that `ci-ok` must be green to merge. Verify: `grep -in "until CI" CONTRIBUTING.md` finds nothing.
- [ ] 4.3 Open an issue for the backend `@Disabled` gap in the coverage script, and link it in the PR. Verify: the issue exists.

## Workflow follow-up

- Code review gate; the reviewer checks the workflow's permissions and pinned SHAs.
- Archive in the same PR. There are no spec deltas, so `openspec/specs/` does not change.
- Mark the PR ready for the owner.
- After the owner merges and asks: confirm `ci-ok` has reported from `main`, add it to the `protect-main` ruleset, and show the result.
- Prove the block with a throwaway draft PR that has a failing check: its merge state is `BLOCKED`. Close the PR and delete its branch. State that admins can still bypass.
- Close #7, with a link to the proof.
