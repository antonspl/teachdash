# teachdash

Read `openspec/constitution.md` before any work. Workflow and rules are there and in
`openspec/config.yaml`.

## Main agent is an orchestrator

Keep the main agent's context small. It talks to the engineer (whoever took the task), writes and edits OpenSpec
artifacts, decides, and delegates. It does not read large parts of the codebase or
write production code itself.

- **Before a proposal** → the main agent restates the task, proposes research angles
  and agrees them with the engineer.
- **Investigation** (tracing behaviour, comparing options, web research) → `researcher`
  agent (Sonnet), one per angle. Give it the task, the angle and what you need to
  decide. `Explore` is only for a quick search for where something is.
- **Spec critique** → `spec-critic` agent (Sonnet), after the spec is drafted and before
  asking for the spec gate, on every change.
- **Implementation** (tests in pass 1, code in pass 2) → `implementer` agent
  (Sonnet). Give it one coherent slice of `tasks.md` at a time, not the whole change.
- **Review** → `reviewer` agent (Sonnet by default). Run it:
  - after every pass 2 slice;
  - whenever an implementer report raises doubt (unsure items, unexpected files,
    failing or changed tests, security-sensitive code);
  - once per PR with `model: "opus"`, before asking for the code gate.
  Fix findings as the Code review step in constitution §1 says, then re-review if the
  fix was non-trivial.

Agents' reports are not shown to the engineer — relay what matters in a few lines.

## Git flow

- `main` is protected: changes land only through pull requests, merged by the owner.
- One change = one branch (named after the OpenSpec change) = one PR, opened as a
  draft right after the spec is written. Spec, tests and code are added to it in
  turn; each stops at its review gate (constitution §1). Check the PR for a
  comment approving the current gate (e.g. "tests approved") before starting the
  next phase. When asking for a gate review, post the commit range of that phase.
- When the change is archived and all gates are approved, mark the PR ready for
  the final review by a code owner other than the engineer.
- Claude never merges PRs, never pushes to `main`, never force-pushes shared branches.
