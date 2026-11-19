# Proposal

## Clarifications

Decisions from the engineer (issue #163):

1. `scripts/flow/gate-status` accepts "test approved" (singular) as approval of the tests gate, in addition to "tests approved". The other rules stay: case-insensitive, at the start of the comment, not a question, not older than the gate request.
2. No other wording is added (no "code approve" and so on).
3. Proof: a new fixture pair in `scripts/flow/fixtures` (`approval-singular-test.json` and `.expected`), run by `scripts/flow/test`, and `scripts/flow/test` passes.
4. Docs that list the approval wording (`AGENTS.md`, `CONTRIBUTING.md`, the PR template, constitution §1) change only if they would now be wrong. They give "tests approved" as the example, which stays true, so none changes. The constitution is not edited.
5. Changes to `scripts/flow/` need the owner's approval. The engineer is the owner and requested this change.

## Research

No research angles. The engineer agreed none were needed; the facts come from issue #163 and the current `gate-status`.

## Why

On PR #162 the engineer wrote "test approved". `gate-status` matches only the plural "tests approved", so it reported no approving comment for the tests gate.

## What Changes

- **`scripts/flow/gate-status`:** the tests gate also matches "test approved". The match for the other gates and all other rules are unchanged.
- **`scripts/flow/fixtures`:** new fixture `approval-singular-test.json` with its `.expected`.

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
- **New:** `scripts/flow/fixtures/approval-singular-test.json`, `scripts/flow/fixtures/approval-singular-test.expected`.
- **Owner approval:** `scripts/flow/` needs the owner's approval (constitution §1); the engineer is the owner and requested the change.
