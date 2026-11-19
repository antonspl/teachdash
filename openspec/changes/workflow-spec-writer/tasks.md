# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Roles and instructions

- [x] 1.1 Add `.agents/roles/spec-writer.md` (design D1, D3). Verify:
  - `scripts/setup-agents` generates it for all three tools;
  - in Claude Code it has write tools and no web tools;
  - `scripts/setup-agents --check` prints `ok`.
- [x] 1.2 Add the commit rules to `.agents/roles/implementer.md` (design D3). Proven by the live run in 1.4.
- [x] 1.3 Update `AGENTS.md` (design D1, D2, D3):
  - the spec-writer bullet;
  - the main agent's narrower role;
  - the commit-before-delegating rule;
  - the "Sessions" section.

  Verify: `grep -n "writes and edits OpenSpec artifacts" AGENTS.md` finds nothing.
- [ ] 1.4 Live run in Claude Code, after a restart, on a throwaway branch with no PR. Leave one unrelated uncommitted file in the checkout. Run two agents:
  - the spec-writer, briefed for a throwaway change with two decisions and one requirement;
  - the implementer, with a one-line slice.

  Verify, and record in the PR:
  - each report fits 15 lines;
  - `openspec validate --strict` passed;
  - each commit holds only its own files;
  - the unrelated file is still uncommitted and is listed in both reports;
  - neither agent pushed.

  Then delete the throwaway branch.

## 2. Flow scripts

- [x] 2.1 Add `guard_synced` to `lib.sh` and use it in `gate-request` and `pr-ready`. Add the range checks to `gate-request` (design D4). Verify: `shellcheck` passes as in CI.
- [x] 2.2 Add the live-mode branch line to `gate-status` (design D4). Verify, and record in the PR:
  - on this PR, the line reads `in sync`, and after a local commit `1 unpushed`;
  - on `main`, it reads `not the PR branch`, when given this PR's number;
  - the existing fixture outputs are unchanged.
- [x] 2.3 Add the D4 cases to `scripts/flow/test`. Verify: `scripts/flow/test` passes. Each new case fails when its check is removed. Record one such run in the PR.

## 3. Coverage check

- [ ] 3.1 Implement the spec phase rule in `scripts/check-scenario-coverage.sh` and update its header comment (design D5). Verify: the check is green on `main` and on this PR.
- [ ] 3.2 Add `scripts/test-scenario-coverage.sh` (design D5). It copies the coverage script into fixture trees. Run it in the `coverage` CI job and add it to the shellcheck list there. Cases and expected results:
  - a change with a new scenario, no test and no ticked task: `PENDING <id> (spec phase: <name>)`, exit 0;
  - the same change with one task ticked: `MISSING <id>`, exit 1;
  - the same change with no ticked task, but a test for one of two new scenarios: the other is `MISSING`, exit 1;
  - the same change with no `tasks.md`: `MISSING`, exit 1;
  - a MODIFIED delta with one new and one old ID, where the old ID's test is removed: the old ID is `MISSING`, exit 1;
  - a REMOVED-only change: the removed main scenario needs no test, and another main scenario without a test is `MISSING`, exit 1;
  - two active changes, A in the spec phase and B with a ticked task, each with one uncovered new ID: `PENDING` for A's ID, `MISSING` for B's ID, exit 1;
  - pass 2 state (tasks ticked) with a backend test whose `@DisplayName` prefix has a typo: `MISSING`, exit 1;
  - a main-spec scenario with its test removed: `MISSING`, exit 1.

  Verify: the script passes, and each case fails when its part of the rule is changed. Record one such run in the PR.
