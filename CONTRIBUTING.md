# Contributing

## Pick a task

The backlog is in [GitHub Issues](https://github.com/antonspl/teachdash/issues).
Labels: `area:backend`, `area:frontend`, `area:devops`, `type:task`, `type:bug`,
`needs-spec`.

1. Take an open issue without an assignee and assign it to yourself.
2. Issues labelled `needs-spec` have no OpenSpec change yet — the spec comes first
   (see below) and the owner approves it before any code.
3. Bugs: reproduce, write a failing test, fix.

## How work is done

Read `openspec/constitution.md` first. In short:

1. Spec: an OpenSpec change in `openspec/changes/<name>/` (proposal, specs, design,
   tasks). The owner approves it in a PR.
2. Red: one failing backend test per spec scenario, scenario ID in `@DisplayName`.
3. Test review: the owner approves the tests. No production code before that.
4. Green: implement until all tests pass. Approved tests are not changed.
5. Archive the change.

## Git

- `main` is protected. Work on a branch named after the change or issue, open a PR,
  link the issue (`Closes #N`).
- Only the owner merges into `main`.

## Run locally

See README.
