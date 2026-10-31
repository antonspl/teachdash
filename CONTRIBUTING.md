# Contributing

## Pick a task

The backlog is in [GitHub Issues](https://github.com/antonspl/teachdash/issues).
Labels: `area:backend`, `area:frontend`, `area:devops`, `type:task`, `type:bug`,
`needs-spec`.

1. Take an open issue without an assignee and assign it to yourself.
2. Issues labelled `needs-spec` have no OpenSpec change yet — the spec comes first.
3. Bugs: reproduce, write a failing test, fix.

## How a change is done

Read `openspec/constitution.md` first (section 1 is the workflow).

**One change = one branch = one PR.** The PR passes three review gates, each
approved by a reviewer — any team member except the author:

| Phase | Author does | Gate |
|---|---|---|
| Spec | OpenSpec change in `openspec/changes/<name>/`; open a **draft** PR | reviewer approves the spec |
| Tests | one failing backend test per scenario, scenario ID in `@DisplayName`; each fails at its THEN step | reviewer approves the tests |
| Code | implement until all tests pass; approved tests are not changed | reviewer approves the code |

Then the author archives the change (in the same PR) and marks the PR ready. The
**owner** reviews spec, tests and code together and merges.

To ask for a gate review, post a comment with the commit range of that phase
(e.g. `Tests: abc123..def456`), so the reviewer reads only what is new.

To approve a gate: post a comment naming it ("spec approved", "tests approved",
"code approved") and tick the box in the PR description. GitHub's "Approve"
button is used only for the owner's final review. Request changes the usual way.

**Tests phase (pass 1).** Tests "fail for the right reason": each fails at its
THEN step, never in setup. Setup creates data and sessions directly, not through
endpoints that do not exist yet. Production code in pass 1 is limited to stubs
returning 501 (marked `STUB`) and schema migrations; the PR description lists
both. Migrations are permanent — review them as production code. Before asking
for the tests review, run `scripts/check-scenario-coverage.sh` (by hand until CI
exists, #7).

**Specs reach `main` only at the end.** A change's spec is not on `main` until
its whole PR merges. A change that depends on another unmerged change branches
from it and targets its branch (stacked PRs, e.g. #2 on `accounts`).

## Reviewing

Anyone on the team can review any PR except their own. Open PRs waiting for a gate:
[draft PRs](https://github.com/antonspl/teachdash/pulls?q=is%3Apr+is%3Aopen+draft%3Atrue).
Check against the constitution: one test per scenario, tests assert the scenario's
THEN, per-teacher data scoping, no secrets.

## Git

- `main` is protected. Only the owner merges into it.
- Branch name = change name. Link the issue in the PR (`Closes #N`).

## Run locally

See README. Backend tests need Docker (Testcontainers).

**Colima instead of Docker Desktop.** Testcontainers fails with "Could not find a
valid Docker environment". Fix:

- `~/.testcontainers.properties`:
  `docker.host=unix:///Users/<you>/.colima/default/docker.sock`
- environment variable `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
  (IntelliJ: the Gradle and JUnit run configuration templates; terminal: your
  shell profile). Without it, Ryuk fails with
  "mkdir …docker.sock: operation not supported".
