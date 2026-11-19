# Proposal

## Clarifications

Decisions from the engineer (issue #163):

1. `scripts/flow/gate-status` accepts "test approved" (singular) as approval of the tests gate, in addition to "tests approved". The fix special-cases only the tests gate (for example `tests?`). The spec and code patterns and the flow-marker match (`marker " " g " gate"`) do not change. The PR-description box parser (`gate-status` line 32) is separate and stays unchanged. The other rules stay: case-insensitive, at the start of the comment, not a question, not older than the gate request.
2. No other wording is added (no "code approve" and so on).
3. Proof: new fixtures in `scripts/flow/fixtures`, run by `scripts/flow/test`, and `scripts/flow/test` passes. They cover: "Test approved" approves the tests gate while the spec and code lines stay "no approving comment; box not ticked"; "Test approved?" gives no approval; "Spec approve", "Code approve" and "Test approve" (no "d") do not count. There is no manual spot check.
4. Docs that list the approval wording (`AGENTS.md`, `CONTRIBUTING.md`, the PR template, constitution §1) change only if they would now be wrong. They give "tests approved" as the example, which stays true, so none changes. The constitution is not edited.
5. Changes to `scripts/flow/` need the owner's approval. The engineer is the owner and requested this change.

## Research

No research angles. The engineer agreed none were needed; the facts come from issue #163 and the current `gate-status`.

## Why

On PR #162 the engineer wrote "test approved". `gate-status` matches only the plural "tests approved", so it reported no approving comment for the tests gate.

## What Changes

- **`scripts/flow/gate-status`:** the tests gate also matches "test approved". The match for the other gates and all other rules are unchanged.
- **`scripts/flow/fixtures`:** three new fixtures, each a `.json` with its `.expected`: `approval-singular-test`, `approval-singular-test-question`, `approval-no-d`.

## Out of Scope

- Other wording variants for any gate.
- Changes to the constitution, other docs, and the gate process.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **Changed:** `scripts/flow/gate-status`.
- **New:** `scripts/flow/fixtures/approval-singular-test`, `approval-singular-test-question` and `approval-no-d`, each as `.json` and `.expected`.
- **Owner approval:** `scripts/flow/` needs the owner's approval (constitution §1); the engineer is the owner and requested the change.
