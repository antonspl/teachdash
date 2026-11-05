# Design

## Context

See `proposal.md` for motivation and the owner's decisions.

- Backend: JDK 25 with no Gradle toolchain, so the runner must provide it. Only `backend/gradlew` is tracked; the root wrapper is gitignored. Tests need no `.env`, and Testcontainers starts Postgres.
- Frontend: Node 26 from `frontend/.nvmrc`, npm with a lockfile. `npm run build` already runs `tsc --noEmit`.
- E2E: `frontend/e2e/global-setup.ts` requires the full stack on `http://localhost:8081`. The root `compose.yaml` provides it with committed dev settings.
- `protect-main` has no required status check today. Repository admins can bypass its pull request rules.

## Goals / Non-Goals

**Goals:**

- A red check is visible on every PR, and after the ruleset step it blocks a normal merge.
- A failure shows which area failed, and leaves enough output to diagnose it.
- The required check name does not change when jobs change.

**Non-Goals:**

- See "Out of Scope" in `proposal.md`.

## Decisions

### D1. Four jobs and one aggregator

One workflow, `ci`, with these jobs running in parallel:

| Job | Runs |
|---|---|
| `backend` | `./gradlew cleanTest test` in `backend/`, so a later Gradle build cache cannot turn the tests into a no-op |
| `frontend` | `npm ci`, `lint`, `format:check`, `test`, `build` |
| `coverage` | `setup-node`, `npm ci`, `scripts/check-scenario-coverage.sh` |
| `e2e` | `docker compose up --build --wait`, `playwright install --with-deps`, then Playwright |
| `ci-ok` | Runs with `if: always()`, needs the other four, and fails unless all four results are `success`, so `skipped` and `cancelled` count as red. It has no other condition that could skip it |

`coverage` is separate so a reviewer at the test gate sees "every scenario has a test" go green while `backend` is red by design.

Only `ci-ok` becomes required, so other jobs can be renamed or added without editing the ruleset. Alternative: requiring each job. Rejected, because every job change would then need a ruleset change.

### D2. Triggers, and no path filters

Triggers are `pull_request`, `push` to `main`, and `workflow_dispatch`. Concurrency is grouped by PR number, cancelling older runs of that PR. Other events are grouped by commit SHA: GitHub keeps only one pending run per group, so a shared `main` group could drop a commit's run even without cancelling.

There are no path filters. A required check from a workflow skipped by a path filter stays "Pending" and blocks merging. Docs-only PRs therefore pay for a full run, which is acceptable on a public repo.

### D3. Playwright projects depend on the event

- `pull_request`: `--project=desktop-chromium`, installing only Chromium.
- `push` to `main` and `workflow_dispatch`: all 3 projects, installing Chromium and WebKit.

A phone failure on `main` shows as a red run and blocks no PR. It is fixed later. No retries, so every failure is visible.

Both tests with the ID `[web-client/R7-S1]` skip themselves on desktop, and nothing else does, so they run only on `main`. The coverage script still counts them, because it is a runtime skip.

`frontend/playwright.config.ts` gains `forbidOnly: !!process.env.CI`. Without it, a stray `test.only` would make E2E pass while running one test. Vitest already refuses `.only` in CI by default.

### D4. Failure output

- `backend`: upload `backend/build/reports/tests/` on failure.
- `e2e`: upload `frontend/playwright-report/` and `frontend/test-results/`, and print `docker compose logs`, on failure.
- Artifacts are kept for 7 days. Every job has a 20-minute timeout.

### D5. Actions and permissions

These are the first GitHub Actions dependencies (constitution section 7):

| Action | Why |
|---|---|
| `actions/checkout` | Gets the code |
| `actions/setup-java` | JDK 25 (`temurin`, matching the backend image) |
| `gradle/actions/setup-gradle` | Gradle cache (with `cache-provider: basic`) and wrapper validation. It replaces `cache:` in `setup-java`, which its docs say not to combine with it |
| `actions/setup-node` | Node from `frontend/.nvmrc`, npm cache |
| `actions/upload-artifact` | Failure reports |

`setup-gradle` v6 defaults to a closed-source caching component under Gradle's terms of use. The owner chose the MIT-licensed basic provider, which uses GitHub's cache.

- Each action is pinned to a full commit SHA with a version comment, resolved when the file is written. #39 keeps them updated later.
- The workflow has `permissions: contents: read`, and checkout uses `persist-credentials: false`.
- It uses `pull_request`, never `pull_request_target`, and needs no secrets. Fork PRs get a read-only token, and outside contributors' runs need approval.

### D6. Coverage script

Backend scenarios keep the text match. For capabilities in `FRONTEND_CAPABILITIES`, the script reads titles from:

- `npx vitest list --json`, using each entry's `name`. Skipped tests are not listed.
- `npx playwright test --list --reporter=json`, walking suites recursively. Tests with `expectedStatus: skipped` do not count. A title appears once per project, and any non-skipped entry counts.

`vitest list --json` prefixes a name with its `describe` titles (`Suite > [id] title`), so only the part after the last ` > ` is compared. A scenario is covered when that test title starts with `[<id>]`. A scenario ID only in a `describe` title no longer counts, which matches the rule in `openspec/config.yaml`.

Node parses the JSON, because the script's users already need it and `jq` would be a new requirement. The script exits with a clear error when `frontend/node_modules` is missing or a list command fails, instead of reporting every scenario as `MISSING`. When frontend scenarios are missing, the message also says to look for a stray `test.only`, because that hides other Vitest tests.

### D7. Required check after merge

The ruleset is changed after merge, once `ci-ok` has reported from `main`. This PR therefore says "Refs #7", and #7 closes only after the check is required and proven (see `tasks.md`).

### D8. Rule for tooling changes

Text to add to constitution section 1, after the list of steps:

> **Tooling changes.** A change that does not change product behaviour (CI, scripts, repository tooling) may set `skip_specs: true`. It has no spec deltas or scenarios. The spec critique and spec review gate (steps 5 and 6) apply to its proposal and design. It has no Red step or test review gate. Its tasks say how each part is proven, and the proof is recorded in the PR for the code review. Archive merges nothing into `openspec/specs/`.

`openspec/config.yaml` gains a matching exception in the `tasks` rules and in `apply` guidance: no "Failing tests" section and no test review stop. The rule covers tooling only; it is not a way to skip a spec for product behaviour.

## Risks / Trade-offs

- [Admins can bypass the pull request rules, so the owner can still merge past a red check on purpose] → Accepted unless the owner wants it changed. The proof shows a blocked normal merge, and this is stated when reporting it.
- [CI is red on draft PRs until pass 2 by design, so red can start to look normal] → `CONTRIBUTING.md` explains which red is expected in each phase, and only the code gate requires all green.
- [Phone bugs reach `main`] → The owner accepted this. `main` still runs all 3 projects, so they show up.
- [The backend healthcheck allows about 100 seconds, which a slow runner could exceed] → Compose logs are printed on failure. Adjust only if it really happens.
- [Docker Hub rate limits on shared runners] → Re-run the job. Add a mirror only if it repeats.
- [The list commands were tested on sample files, not this repo's tests] → Task 1.4 checks them on the real tests, including `.tsx`.
- [`workflow_dispatch` works only once the file is on `main`, so the all-projects path cannot use it before merge] → Task 3.2 proves that path with a temporary branch `push` trigger.
- [Runner support for Node 26 and Temurin 25, and `setup-gradle` with a `backend/` subdirectory, are not confirmed] → The first run on the PR proves or disproves them.

## Open Questions

- Real run times. If E2E stays slow, consider Docker layer caching in a follow-up.
