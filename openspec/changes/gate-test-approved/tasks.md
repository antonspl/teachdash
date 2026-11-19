# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Gate status

- [ ] 1.1 Add the fixtures in `scripts/flow/fixtures`, each a `.json` with its `.expected`, all with the PR by `alice`, commits before the comments and no box ticked:
  - `approval-singular-test`: comment "Test approved" from `alice`. Expected: `tests: approved by alice at <time>; comment without a ticked box`, and the spec and code lines read `no approving comment; box not ticked` (the other gates are unaffected).
  - `approval-singular-test-question`: comment "Test approved?". Expected: all three gates `no approving comment; box not ticked`.
  - `approval-no-d`: three comments "Spec approve", "Code approve" and "Test approve". Expected: all three gates `no approving comment; box not ticked`.
  Plural "tests approved" is already covered by the live-mode check "gate-status reads paginated comments and reviews" in `scripts/flow/test` (a review "Tests approved" approves the tests gate); no new fixture is needed for it. The existing `marker-comment` fixture must keep passing. Verify, and record in the PR: before 1.2, `scripts/flow/test` fails on `approval-singular-test` (the singular case) and passes on the other new fixtures; after 1.2 all pass.
- [ ] 1.2 In `scripts/flow/gate-status`, let the tests gate match "test approved" as well as "tests approved" (decisions 1 and 2), special-casing only the tests gate. The spec and code patterns, the flow-marker match and the PR-description box parser do not change. Verify, and record in the PR:
  - `scripts/flow/test` passes, including the new fixtures and all existing ones (`marker-comment` among them) with unchanged output;
  - `shellcheck` passes as in CI.

## 2. Workflow follow-up

- [ ] 2.1 Code review gate: run the `code-gate-reviewer`, fix findings, post the range with `gate-request`.
- [ ] 2.2 Archive the change in the same PR.
- [ ] 2.3 Mark the PR ready for the final code owner review.
