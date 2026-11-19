# Proposal

## Clarifications

Questions answered by the engineer in the #141 retrospective (2026-11-19):

1. **Who writes the OpenSpec artifacts?** A new `spec-writer` role writes them. The main agent only orchestrates: it talks to the engineer, lists the decisions in a brief for the writer, and pushes. The writer records them in Clarifications.
2. **Does the main agent keep one session for the whole change?** No. After each approved gate the work continues in a new session. All state lives in the PR, in `openspec/changes/<name>/` and in git.
3. **Which #141 problems are fixed here?**
   - The implementer committed the main agent's uncommitted edits.
   - The branch could be left on a detached HEAD or with unpushed commits without any script noticing.
   - The scenario coverage check fails in CI during the spec phase.
4. **Not fixed here:** the `Closes #104` line was correct and GitHub linked the issue, so there is nothing to fix in `pr-draft`. Flaky Maven Central downloads in e2e and the stale agents check at session start are left out.

Assumptions the engineer confirms at the spec gate:

5. The constitution does not change. The new role and the session rule live in `AGENTS.md`, like the other roles (design D1).
6. The coverage check decides that a change is in the spec phase by itself, from the tests it finds, and not from the PR (design D5).

## Research

No research angles. The facts come from the #141 session and from reading the current scripts, roles and CI.

## Why

In #141 the main agent's context overflowed once. The biggest cost was one session that ran the whole change, and the main agent writing and rewriting the artifacts itself. Three small tooling problems also cost time: a mixed commit, a detached HEAD that led to a wrong "everything is pushed", and a red CI check that was expected.

## What Changes

- **New role `spec-writer`** (medium class, write access, no web). It drafts and edits the proposal, spec deltas, design and tasks from a brief, runs `openspec validate --strict` until it passes, and commits only the change's own files.
- **`AGENTS.md`:** the main agent briefs the spec-writer instead of writing artifacts. It makes only one- or two-line edits itself. A new rule says to continue in a new session after each approved gate, with the steps to resume.
- **Commits by agents:** the main agent commits its own edits before it delegates to a writing agent. The implementer and the spec-writer stage files by path and never commit files outside their slice.
- **`scripts/flow`:** `gate-request` and `pr-ready` refuse when the branch has unpushed commits, is behind origin, or the requested range end is not on the pushed branch. `gate-status` prints one line on how the branch stands against origin.
- **`scripts/check-scenario-coverage.sh`:** while none of an active change's new scenarios has a test, the change is in the spec phase. Its scenarios are listed as PENDING and do not fail the check.

## Out of Scope

- Changes to the constitution and to the gate process.
- Caching Maven downloads in CI.
- Enforcing the new-session rule. It is advice, not a check.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **New:** `.agents/roles/spec-writer.md`.
- **Changed:** `AGENTS.md`, `.agents/roles/implementer.md`, `scripts/flow/lib.sh`, `scripts/flow/gate-request`, `scripts/flow/pr-ready`, `scripts/flow/gate-status`, `scripts/flow/test`, `scripts/check-scenario-coverage.sh`.
- **Owner approval:** `.agents/roles/` and `scripts/flow/` need the owner's approval (constitution §1).
- **Process:** after the merge, each developer runs `scripts/setup-agents` and restarts the session so the new role appears.
