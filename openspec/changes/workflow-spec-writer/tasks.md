# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Roles and instructions

- [ ] 1.1 Add `.agents/roles/spec-writer.md` (design D1, D3). Verify:
  - `scripts/setup-agents` generates it for all three tools;
  - in Claude Code it has write tools and no web tools;
  - `scripts/setup-agents --check` prints `ok`.
- [ ] 1.2 Update `.agents/roles/implementer.md` with the commit rules (design D3). Verify: the diff adds only the commit rules.
- [ ] 1.3 Update `AGENTS.md`: the spec-writer bullet, the main agent's narrower role, the commit-before-delegating rule and the "Sessions" section (design D1, D2, D3). Verify:
  - `wc -c AGENTS.md` is under 32768;
  - `grep -n "writes and edits OpenSpec artifacts" AGENTS.md` finds nothing.
- [ ] 1.4 Live run in Claude Code after a restart. Brief the spec-writer for a throwaway change, with two decisions and one requirement. Verify, and record in the PR:
  - the report fits 15 lines;
  - `openspec validate --strict` passed;
  - the commit holds only that change's files, with an unrelated uncommitted file left alone.

  Then delete the throwaway change and its commit.

## 2. Flow scripts

- [ ] 2.1 Add `guard_synced` to `lib.sh` and use it in `gate-request` and `pr-ready`. Add the range-end check to `gate-request` (design D4). Verify: `shellcheck` passes as in CI.
- [ ] 2.2 Add the branch line to `gate-status` (design D4). Verify, on this PR: the line reads `in sync`, and after a local commit `1 unpushed`.
- [ ] 2.3 Add the D4 cases to `scripts/flow/test`. Verify: `scripts/flow/test` passes. Each new case fails when its check is removed (record one such run in the PR).

## 3. Coverage check

- [ ] 3.1 Implement the spec phase rule in `scripts/check-scenario-coverage.sh` and update its header comment (design D5). Verify, and record in the PR:
  - a throwaway change with a new scenario and no test prints `PENDING` and exits 0;
  - adding a test for one of two new scenarios makes the other `MISSING` with exit 1;
  - a main-spec scenario with its test removed is still `MISSING`;
  - the check is green on `main`.

## 4. CI

- [ ] 4.1 The `coverage` job is green on this PR, and `ci-ok` is green.
