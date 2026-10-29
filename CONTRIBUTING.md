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
| Tests | one failing backend test per scenario, scenario ID in `@DisplayName` | reviewer approves the tests |
| Code | implement until all tests pass; approved tests are not changed | reviewer approves the code |

Then the author archives the change (in the same PR) and marks the PR ready. The
**owner** reviews spec, tests and code together and merges.

To approve a gate: leave a GitHub review "Approve" or a comment such as
"tests approved", and tick the box in the PR description. Request changes the
usual way.

## Reviewing

Anyone on the team can review any PR except their own. Open PRs waiting for a gate:
[draft PRs](https://github.com/antonspl/teachdash/pulls?q=is%3Apr+is%3Aopen+draft%3Atrue).
Check against the constitution: one test per scenario, tests assert the scenario's
THEN, per-teacher data scoping, no secrets.

## Git

- `main` is protected. Only the owner merges into it.
- Branch name = change name. Link the issue in the PR (`Closes #N`).

## Run locally

See README.
