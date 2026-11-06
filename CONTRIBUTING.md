# Contributing

## Pick a task

The backlog is in [GitHub Issues](https://github.com/antonspl/teachdash/issues).
Labels: `area:backend`, `area:frontend`, `area:devops`, `type:task`, `type:bug`,
`needs-spec`.

1. Take an open issue without an assignee and assign it to yourself.
2. Issues labelled `needs-spec` have no OpenSpec change yet — the spec comes first.
3. Bugs: reproduce, write a failing test, fix.

## How a change is done

Everyone on the project is independent and interchangeable. The constitution, the
specs and these documents show how work is usually done; they are helpers, not
blockers. Read `openspec/constitution.md` to see the usual way. Section 1 lists
the workflow steps.

The **engineer** — whoever took the task — leads the change. No step is mandatory
and the order is not fixed: the engineer decides whether to clarify, research,
write the spec first or the tests first, and when to ask for review.

**One change = one branch = one PR.** Usually the OpenSpec change lives in
`openspec/changes/<name>/`, every scenario has a test with its scenario ID, and the
change is archived in the same PR so `openspec/specs/` matches the code.

What goes into the final version of the PR is agreed between the engineer and the
reviewers through normal GitHub review. The **owner** merges.

**CI.** CI runs on every pull request, and `ci-ok` must be green to merge. On a
draft PR, red is expected while work is in progress, for example `coverage` when
scenarios have no tests yet, or `backend` when tests are written before the code. Pull requests run Playwright on desktop Chromium only. `main` runs all 3
projects, and a phone failure there is fixed later.

**Specs reach `main` only at the end.** A change's spec is not on `main` until
its whole PR merges. A change that depends on another unmerged change branches
from it and targets its branch (stacked PRs, e.g. #2 on `accounts`).

## Reviewing

Anyone on the team can review any PR except their own:
[open PRs](https://github.com/antonspl/teachdash/pulls).
Check against the constitution: one test per scenario, tests assert the scenario's
THEN, per-teacher data scoping, no secrets.

## Git

- `main` is protected. Only the owner merges into it.
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
