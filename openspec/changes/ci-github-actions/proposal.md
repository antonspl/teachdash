# Proposal

## Clarifications

Questions asked before this proposal, and the owner's answers (2026-11-05):

1. **Does this change have spec scenarios?** No. It uses `skip_specs`: proposal, design and tasks only. Proof is real workflow runs on a PR, because JUnit cannot test a workflow file.
2. **Which checks run?** All of them: backend tests, the coverage script, frontend lint, format check, type check and build, Vitest and Playwright.
3. **Can the frontend part be faster, or Playwright be left out?** Keep Playwright. Pull requests run only `desktop-chromium`; pushes to `main` run all 3 projects. Phone problems do not block a PR and are fixed later, if they appear.
4. **Should the coverage script catch skipped frontend tests?** Yes, static skips and commented-out tests, through the list commands. Runtime and conditional skips stay a review item. The same gap in backend tests (`@Disabled`) goes to a new issue.
5. **Should Playwright retry in CI?** No retries at first.
6. **Who makes the check required in `protect-main`?** Claude, after the owner merges this PR and asks.
7. **May constitution section 2 be edited here?** Yes, approved by the owner, so it no longer says the script is run by hand until CI exists.

## Research

Both angles were agreed with the owner.

- **Skip detection:** tested with vitest 5.0.5 and @playwright/test 1.64.1 on sample tests. `vitest list --json` leaves out static skips. `playwright test --list --reporter=json` lists them with `expectedStatus: skipped`. Neither needs a browser or a running stack. Neither detects a skip made at runtime. Conclusion: use them for static skips.
- **CI setup:** separate jobs plus one required aggregator job, no path filters, actions pinned by commit SHA, no secrets. E2E needs the full compose stack, and building its images is the slowest part. Conclusion: see `design.md`. Timings are estimates until the first real run.

## Why

No workflow exists, so tests and the scenario coverage check run only when someone remembers to run them, and nothing stops a red change from merging. Deployment (#9) also depends on this.

## What Changes

- Add a GitHub Actions workflow that runs on pull requests, on pushes to `main`, and by hand. The issue says "every push and PR", but a push plus a PR on the same branch would run everything twice.
- Add one aggregator job, `ci-ok`, as the only check that later becomes required.
- Change `scripts/check-scenario-coverage.sh` so frontend scenarios are found through the list commands. It then needs `npm ci` in `frontend/` first.
- Set `forbidOnly` in CI for Playwright, so a stray `test.only` cannot silently turn off the other E2E tests.
- Update constitution section 2, `CONTRIBUTING.md`, and the script header.
- **After merge:** add `ci-ok` to the `protect-main` ruleset, and show that a red check blocks merging.

## Out of Scope

- Detecting runtime or conditional skips.
- Detecting `@Disabled` backend tests (new issue).
- Automated updates of pinned actions (#39).
- Deployment, and whether a phone failure on `main` may hold back a deploy (#9).
- Docker layer caching and Playwright retries, until real runs show a need.
- Blocking PRs on phone browser failures.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling: no product behaviour changes, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **New:** `.github/workflows/ci.yml`.
- **Changed:** `scripts/check-scenario-coverage.sh`, `frontend/playwright.config.ts`, `openspec/constitution.md`, `CONTRIBUTING.md`.
- **Process:** every PR waits for CI. CI is expected to be red on a draft PR until pass 2 is finished, because the spec phase has scenarios without tests and the tests phase has failing tests.
- **Repository settings:** the `protect-main` ruleset gains a required status check after merge.
- **Hand runs:** the coverage script needs `npm ci` in `frontend/` first.
