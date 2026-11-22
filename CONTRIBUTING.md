# Contributing

## Pick a task

The backlog is in [GitHub Issues](https://github.com/antonspl/teachdash/issues).
Labels: `area:backend`, `area:frontend`, `area:devops`, `type:task`, `type:bug`,
`needs-spec`.

1. Take an open issue without an assignee and assign it to yourself.
2. Issues labelled `needs-spec` have no OpenSpec change yet — the spec comes first.
3. Bugs: reproduce, write a failing test, fix.

## Agent setup

The workflow runs in Claude Code, opencode or Codex. Agent files are generated, not
in git. After cloning:

1. Copy `.agents/models.example` to `.agents/models.local` and set a model for each
   tool and class (`strong`, `medium`) you use. Without an entry, a role runs on the
   session's model.
2. Run `scripts/setup-agents`.
3. Codex only: trust the project, or Codex ignores `.codex/`.

Rerun `scripts/setup-agents` after you pull a change to `.agents/` (roles or
skills), after `openspec update`, and after you edit `.agents/models.local`.
`scripts/setup-agents --check` prints `ok` or `stale`. The agent runs it for you at
the start of a session and restarts if needed.

## How a change is done

Read `openspec/constitution.md` first (section 1 is the workflow).

**One change = one branch = one PR.** The PR passes three review gates. You, the
engineer who took the task, approve each one after checking what the agent produced:

| Phase | Author does | Gate |
|---|---|---|
| Spec | OpenSpec change in `openspec/changes/<name>/`; open a **draft** PR | you approve the spec |
| Tests | one failing backend test per scenario, scenario ID in `@DisplayName`; each fails at its THEN step | you approve the tests |
| Code | implement until all tests pass; approved tests are not changed | you approve the code |

Then the author archives the change (in the same PR) and marks the PR ready. A
**code owner** (`.github/CODEOWNERS`) other than the author reviews spec, tests and
code together and approves the PR. Then anyone on the team merges it.

### Steps and commands

| Step | How |
|---|---|
| Explore (optional, for a big or unclear feature; skip when the issue already has it) | `/openspec-explore` (Codex: `$openspec-explore`) |
| Clarify, Research | the agent restates the task and asks only what the issue does not answer; you answer and agree the research angles (or none; a change touching `frontend/src` always gets at least one `frontend-researcher`) |
| Propose + spec critique | `/openspec-propose` (Codex: `$openspec-propose`); the agent runs one critic, the frontend-spec-critic when the change touches `frontend/src`, otherwise the spec-critic, then stops for "spec approved" |
| Tests, then code | `/openspec-apply-change` (Codex: `$openspec-apply-change`). Backend: pass 1 writes failing tests and stops for "tests approved"; pass 2 writes the code. Frontend-only: tests and code in one pass. The agent runs the code-gate review (with the visual-reviewer alongside it when the PR has a final screenshot set), then stops for "code approved" |
| Archive | `/openspec-archive-change` (Codex: `$openspec-archive-change`), then mark the PR ready |
| Final review | another code owner approves; then anyone merges |

The author answers the clarifying questions and decides trade-offs for their change.
Anyone can improve the workflow (`AGENTS.md`, `CLAUDE.md`, `CONTRIBUTING.md`,
the other files in `.agents/`, the PR template) through a normal PR. Changes to the constitution,
`openspec/config.yaml`, `.github/CODEOWNERS`, the permission files
(`.claude/settings.json`, `opencode.json`, `.codex/rules/`) and the scripts they
allow without asking (`scripts/flow/`, `scripts/setup-agents`) and the role
definitions (`.agents/roles/`) need the owner's approval.

At each gate the agent stops and posts the commit range of that phase
(e.g. `Tests: abc123..def456`), so you read only what is new.

To approve a gate: post a comment naming it ("spec approved", "tests approved",
"code approved") and tick the box in the PR description. GitHub's "Approve"
button is used only for the final review. Request changes the usual way.

**Tests phase (pass 1).** Tests "fail for the right reason": each fails at its
THEN step, never in setup. Setup creates data and sessions directly, not through
endpoints that do not exist yet. Production code in pass 1 is limited to stubs
returning 501 (marked `STUB`) and schema migrations; the PR description lists
both. Migrations are permanent — review them as production code. Before asking
for the tests review, check that `scripts/check-scenario-coverage.sh` passes. CI runs it; for a hand
run, do `npm ci` in `frontend/` first.

**CI.** CI runs on every pull request, and `ci-ok` must be green to merge. On a
draft PR, red is expected until the code phase: `coverage` in the spec phase
(scenarios with no tests yet) and `backend` in the tests phase (tests fail by
design). Pull requests run Playwright on desktop Chromium only. `main` runs all 3
projects, and a phone failure there is fixed later.

**Specs reach `main` only at the end.** A change's spec is not on `main` until
its whole PR merges. A change that depends on another unmerged change branches
from it and targets its branch (stacked PRs, e.g. #2 on `accounts`).

## Reviewing

Any code owner can give the final approval on any PR except their own. PRs waiting
for it: [open PRs ready for review](https://github.com/antonspl/teachdash/pulls?q=is%3Apr+is%3Aopen+draft%3Afalse).
Check against the constitution: one test per scenario, tests assert the scenario's
THEN, per-teacher data scoping, no secrets.

## Git

- `main` is protected: changes land only through PRs with a code owner's approval and
  green `ci-ok`. Anyone on the team can merge such a PR; only the owner may merge
  without an approval.
- Branch name = change name. Link the issue in the PR (`Closes #N`).

## Run locally

See README. Backend tests need Docker (Testcontainers).

**Colima instead of Docker Desktop.** Testcontainers fails with "Could not find a
valid Docker environment". Fix: create `~/.testcontainers.properties` with

```
docker.host=unix:///Users/<you>/.colima/default/docker.sock
```

`./gradlew test` sets `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
itself (Ryuk needs it), unless you set it yourself. In IntelliJ, run tests through
Gradle, or set the variable in the JUnit run configuration template.
