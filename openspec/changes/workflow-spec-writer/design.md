# Design

## Context

See `proposal.md` for the decisions and the #141 problems.

- `AGENTS.md` says the main agent "writes and edits OpenSpec artifacts". Roles exist for research, critique, implementation and review, but not for writing.
- `.agents/roles/implementer.md` does not say how to commit. In #141 the implementer committed the main agent's uncommitted `design.md` and `tasks.md` edits in its own commit.
- `scripts/flow/lib.sh` `guard_branch` refuses a detached HEAD and `main`. Nothing checks that the branch matches origin. `gate-request` accepts any range that looks like `abc1234..def5678`.
- `scripts/check-scenario-coverage.sh` checks main specs and active change deltas the same way. A change in the spec phase has no tests yet, so the `coverage` job and `ci-ok` are red until pass 1.

## Goals / Non-Goals

**Goals:**

- The main agent's context holds decisions and short reports, not artifact text.
- A phase can start in a fresh session from what is in git and the PR.
- No agent commits another agent's work, and no gate request points at commits that are not on the pushed branch.
- A red CI check means a real problem.

**Non-Goals:**

- Enforcing the session rule or the commit rule by a script.

## Decisions

### D1. `spec-writer` role, wired in `AGENTS.md` only

`.agents/roles/spec-writer.md`: class `medium`, access `write`, web `false`.

- **Input:** a brief from the main agent:
  - the change name and the issue;
  - the engineer's decisions as a numbered list;
  - the research findings;
  - for an edit: what to change and why.
- **Reads:** the constitution sections for the step (as the `AGENTS.md` table says), `openspec/config.yaml`, `openspec instructions <artifact> --change <name> --json`, and the main specs and code the change relies on.
- **Writes:** only files under `openspec/changes/<name>/`. It records the decisions in the proposal's Clarifications as given and does not take new decisions. When the brief leaves a real choice open, it stops and reports the question.
- **Checks:** `openspec validate <name> --strict` passes before it finishes.
- **Commits:** its own files, by path.
- **Report:** at most 15 lines:
  - the requirements, one line each;
  - the scenario count;
  - the validate result;
  - the commit;
  - open questions.

`AGENTS.md`, in "Main agent is an orchestrator":
- the main agent no longer "writes and edits OpenSpec artifacts";
- a new bullet for the spec-writer. The main agent briefs it for the proposal step and for every fix after critique or engineer comments. The main agent may make a one- or two-line edit itself.

The constitution stays as it is. It does not name the researcher either; roles live in `AGENTS.md`.

Alternative: keep writing in the main agent and only compact more often. Rejected: compaction loses detail, and writing is the largest part of the main agent's context.

### D2. New session after each approved gate

A short "Sessions" section in `AGENTS.md`:

- After the main agent posts a gate request, it ends its turn with one line: the gate, the range, and "continue in a new session after approval".
- A new session resumes from:
  - the branch, named after the change;
  - `scripts/flow/gate-status`;
  - the proposal's Clarifications;
  - `tasks.md`.

  It does not reread the earlier phases' diffs.
- Inside one phase, the session continues as today.

### D3. Commits by agents

- `AGENTS.md`: before delegating to the implementer or the spec-writer, the main agent commits its own pending edits, or tells the agent which uncommitted files are not its own.
- `implementer.md` and `spec-writer.md`:
  - stage files by path (`git add <paths>`), never `git add -A`, `git add .` or `git commit -a`;
  - leave other uncommitted changes alone and list them in the report.

### D4. Branch sync checks in `scripts/flow`

In `lib.sh`, `guard_synced <branch>`:

- runs `git fetch --quiet origin <branch>`;
- fails when there is no `origin/<branch>`;
- fails when HEAD is not equal to `origin/<branch>`. The message says how many commits are unpushed or behind.

Callers:

- `gate-request` calls `guard_synced`. It also refuses when the range end is not an ancestor of `origin/<branch>` (`git merge-base --is-ancestor`).
- `pr-ready` calls `guard_synced` and refuses uncommitted changes.
- `gate-status` does not fail. It prints one first line:
  - `branch: in sync with origin`;
  - `branch: N unpushed, M behind origin`;
  - `branch: detached HEAD`.

  The existing gate lines follow it.

`scripts/flow/test` gets cases on a local bare repo as `origin`, so no network is needed:
- in sync;
- unpushed commit;
- behind;
- no upstream;
- range end not pushed.

### D5. Spec phase in the coverage check

For each active change (`openspec/changes/<name>/`, archive excluded):

- its **new** scenario IDs are those in its deltas that are not in the main spec of the same capability;
- if none of the new IDs has a test, the change is in the spec phase. Its uncovered scenarios print as `PENDING <id> (spec phase: <name>)` and do not fail the check;
- once one new ID has a test, every uncovered scenario of that change is `MISSING`, as today.

Main specs stay strict. Archive moves the deltas into the main specs before merge, so a change cannot reach `main` with a missing test.

Rejected: reading the gate state from the PR in CI. It needs a token and depends on boxes that are not always ticked.

## Risks / Trade-offs

- **Brief loses nuance** → the brief is a numbered list of decisions, the writer copies them into Clarifications, and the spec-critic runs on every change after the writer.
- **Pass 1 with zero tests stays green** until the archive puts the scenarios into the main specs. The test gate is the human check for this.
- **A new scenario ID that reuses an ID from the main spec** counts as existing. IDs are not reused in this repo, so this is accepted.
- **`guard_synced` fetches** on every call → one small fetch of one branch, a fraction of a second.
- **Stale agent files** → developers must run `scripts/setup-agents` after the merge. `change-start` already warns when they are stale.
