# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Gate status

- [ ] 1.1 Add the fixture `scripts/flow/fixtures/approval-singular-test.json` and its `.expected`: a PR by `alice` with a comment "Test approved" from `alice` after the commits, the tests box not ticked. The expected output reads `tests: approved by alice at <time>; box not ticked`. Verify, and record in the PR: before 1.2, `scripts/flow/test` fails on this fixture.
- [ ] 1.2 In `scripts/flow/gate-status`, let the tests gate match "test approved" as well as "tests approved" (decisions 1 and 2). No other gate and no other rule changes. Verify, and record in the PR:
  - `scripts/flow/test` passes, including the new fixture and all existing ones with unchanged output;
  - `shellcheck` passes as in CI;
  - a spot check: "test approved?" (a question) and "code approve" still do not count.

## 2. Workflow follow-up

- [ ] 2.1 Code review gate: run the `code-gate-reviewer`, fix findings, post the range with `gate-request`.
- [ ] 2.2 Archive the change in the same PR.
- [ ] 2.3 Mark the PR ready for the final code owner review.
