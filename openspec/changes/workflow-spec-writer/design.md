# Design

## Context

See `proposal.md` for the decisions and the #141 problems.

- `AGENTS.md` says the main agent "writes and edits OpenSpec artifacts". There are roles for research, critique, implementation and review, but none for writing. The constitution does not say who writes artifacts, so it needs no change.
- `.agents/roles/implementer.md` does not say how to commit. In #141 the implementer committed the main agent's uncommitted `design.md` and `tasks.md` edits in its own commit.
- `scripts/flow/lib.sh` `guard_branch` refuses a detached HEAD and `main`. Nothing checks that the branch matches its upstream. `gate-request` accepts any range that looks like `abc1234..def5678`. `pr-draft` pushes with `-u`, so a change branch has an upstream from the draft PR on.
- `scripts/check-scenario-coverage.sh` checks main specs and active change deltas the same way. It has no test of its own. A change in the spec phase has no tests yet, so the `coverage` job and `ci-ok` are red until pass 1.
- `scripts/setup-agents` already supports `access: write` with `web: false`. Claude Code and opencode get a deny list. Codex gets instructions only.

## Goals / Non-Goals

**Goals:**

- The main agent's context holds decisions and short reports, not artifact text.
- A phase can start in a fresh session from what is in git and the PR.
- No agent commits another agent's work.
- No gate request points at commits that are not on the pushed branch.
- A red CI check means a real problem.

**Non-Goals:**

- Enforcing the session rule or the commit rule by a script.
- Limiting the spec-writer's writes to the change folder by tool settings. No tool can limit writes to one folder, so the rule is an instruction.

## Decisions

### D1. `spec-writer` role, wired in `AGENTS.md` only

`.agents/roles/spec-writer.md`: class `medium`, access `write`, web `false`.

- **Input**, a brief from the main agent:
  - the change name, the issue and the PR, if it exists;
  - the engineer's decisions as a numbered list;
  - the research conclusions, a few lines per angle;
  - for an edit: the findings or comments to fix, and the decision on each.
- **Reads:**
  - the constitution sections for the step, as the `AGENTS.md` table says;
  - `openspec/config.yaml` and `openspec instructions <artifact> --change <name> --json`;
  - the main specs and the code the change relies on.
- **Writes:** only files under `openspec/changes/<name>/`.
  - It records the brief's decisions in the proposal's Clarifications, as given.
  - It does not take new decisions. When the brief leaves a real choice open, it stops and reports the question.
- **Checks:** `openspec validate <name> --strict` passes before it finishes.
- **Commits** its own files, by path (D3). It does not push.
- **Report** in at most 15 lines:
  - the requirements, one line each;
  - the scenario count;
  - the validate result;
  - the commit;
  - open questions;
  - uncommitted files it did not touch.

`AGENTS.md`, in "Main agent is an orchestrator":

- The main agent no longer "writes and edits OpenSpec artifacts". It agrees decisions with the engineer, briefs the spec-writer, and pushes after reading the writer's report.
- A new bullet for the spec-writer. The main agent briefs it for the proposal step and for every fix after critique or engineer comments. The main agent may make a one- or two-line edit itself.

The constitution stays as it is. It names roles only by their work, and the role list lives in `AGENTS.md`.

Alternative: keep writing in the main agent and compact more often. Rejected: compaction loses detail, and writing is the largest part of the main agent's context.

Trade-off: research and critique findings still pass through the main agent, because the researcher and the spec-critic are read-only and cannot write files. The brief carries only conclusions and decisions, not full reports.

### D2. New session after each approved gate

A short "Sessions" section in `AGENTS.md`:

- After the main agent posts a gate request, it ends its turn with one line: the gate, the range, and "continue in a new session after approval".
- A new session resumes from:
  - the branch, named after the change;
  - `scripts/flow/gate-status`, which also shows whether the branch is pushed (D4);
  - the PR description and the last gate request comment, which give the issue, the ranges and the critique summary;
  - the proposal's Clarifications;
  - `tasks.md`. Its ticks show where a phase stopped, including after a crash in the middle of a phase.
- A new session does not reread the earlier phases' diffs.
- Inside one phase, the session continues as today.

### D3. Commits by agents

- `AGENTS.md`: before delegating to the implementer or the spec-writer, the main agent commits its own pending edits.
- `implementer.md` and `spec-writer.md`:
  - stage files by path (`git add <paths>`), never `git add -A`, `git add .` or `git commit -a`;
  - leave other uncommitted changes alone and list them in the report;
  - do not push. The main agent pushes.

### D4. Branch sync checks in `scripts/flow`

In `lib.sh`, `guard_synced`:

- reads the branch's upstream (`@{upstream}`) and fails when there is none;
- fetches that upstream branch;
- fails when HEAD is not equal to the upstream. The message gives how many commits are unpushed and how many are behind;
- fails when there are uncommitted changes to tracked files.

Callers:

- `gate-request` calls `guard_synced`. It also refuses a range unless `from` is an ancestor of `to` and `to` is an ancestor of the upstream (`git merge-base --is-ancestor`). After a rebase and a `--force-with-lease` push, the new range passes and a range from before the rebase fails.
- `pr-ready` calls `guard_synced`.
- `gate-status` never fails because of the branch. In live mode only, it prints one first line before the existing gate lines:
  - `branch: in sync with upstream`;
  - `branch: N unpushed, M behind upstream`;
  - `branch: detached HEAD`;
  - `branch: no upstream`;
  - `branch: unknown (fetch failed)`;
  - `branch: not the PR branch`, when the current branch is not the PR's head branch.

  The first matching state in this order wins: detached HEAD, not the PR branch, no upstream, fetch failed, then the counts. In fixture mode (`FLOW_GH_JSON`) the line is not printed, so the existing fixtures stay as they are.

`guard_synced` also blocks on an unrelated tracked edit in the checkout. That is intended: a gate request or a ready PR must describe exactly what is pushed.

`scripts/flow/test` gets cases on a local bare repository as the remote. They need no network, and each temp repository sets `user.name` and `user.email`. The cases:

- in sync;
- unpushed commit;
- behind;
- no upstream;
- uncommitted change;
- range end not pushed;
- `from` not an ancestor of `to`.

### D5. Spec phase in the coverage check

For each active change (`openspec/changes/<name>/`, archive excluded):

- **New IDs** are the scenario IDs in the change's deltas, outside REMOVED sections, that do not appear in any main spec. An ID carries its capability, so one list of main-spec IDs is enough. An ID that a REMOVED and ADDED pair reuses counts as old, so it stays strict.
- A change is **in the spec phase** when all of these hold:
  - it has at least one new ID;
  - none of its new IDs has a test;
  - its `tasks.md` exists and has no ticked task.

  Otherwise it is strict. A change with no new IDs, or with no `tasks.md`, is never in the spec phase.
- In the spec phase, an uncovered **new** ID prints as `PENDING <id> (spec phase: <name>)` and does not fail the check. Every other uncovered ID is `MISSING`, as today.

Both signals must say "not started" so neither can hide a missing test alone:
- In pass 2 tasks are ticked, so a stray `test.only` or a wrong `@DisplayName` prefix still shows as `MISSING`.
- If pass 1 writes tests but forgets to tick, the first test already makes the change strict.

Between slices of pass 1, CI is red. That is a real state: tests are still missing.

Main specs stay strict. The archive moves the deltas into the main specs before merge, so a change cannot reach `main` with a missing test.

A new `scripts/test-scenario-coverage.sh` copies the coverage script into fixture trees in a temp folder and runs it there. The script reads the tree above its own location, so a copy is needed. The fixtures use backend capabilities only, because the rule does not depend on the test runner. The `coverage` CI job runs the new script and adds it to the shellcheck list.

Rejected: reading the gate state from the PR in CI. It needs a token and depends on boxes that are not always ticked. Also rejected: deciding the phase from the tests found alone. A broken test list would then look like the spec phase.

## Risks / Trade-offs

- **The brief loses nuance.** The brief is a numbered list of decisions, and the writer copies them into Clarifications. The spec-critic runs on every change after the writer.
- **`guard_synced` fetches** on every call. That is one small fetch of one branch, under a second.
- **Stale agent files.** Developers must run `scripts/setup-agents` after the merge. `change-start` already warns when the files are stale.
