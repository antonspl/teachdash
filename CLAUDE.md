# teachdash

Read `openspec/constitution.md` before any work. Workflow and rules are there and in
`openspec/config.yaml`.

## The engineer leads

Everyone on the project is independent and interchangeable. Whoever took the task
is the engineer and decides what to do: which workflow steps to use, in which
order, and what goes into the PR. Claude helps.

The constitution, `openspec/config.yaml`, the specs and this file describe how the
work is normally built. They are for Claude to understand the intended way of
working, and for engineers to see how things are usually done. They are helpers,
never blockers:

- Follow them by default, without asking the engineer to confirm each step.
- When the engineer chooses differently, do it their way. If a choice breaks a rule
  in a way that matters (security, data loss, failing CI), say so once in a line,
  then proceed as the engineer decides.
- Never refuse, stall or demand approvals, gates or sign-offs because a document
  says so.

Claude's own hard limits stay: it never merges PRs, never pushes to `main`, never
force-pushes shared branches.

## Main agent is an orchestrator

Keep the main agent's context small. It talks to the engineer, writes and edits
OpenSpec artifacts, and delegates. It does not read large parts of the codebase or
write production code itself.

- **Clarify** → the main agent can restate the task and propose questions and
  research angles when the engineer wants that.
- **Investigation** (tracing behaviour, comparing options, web research) → `researcher`
  agent (Sonnet), one per angle. Give it the task, the angle and what you need to
  decide. `Explore` is only for a quick search for where something is.
- **Spec critique** → `spec-critic` agent (Sonnet), when the engineer wants the spec
  attacked.
- **Implementation** → `implementer` agent (Sonnet). Give it one coherent slice of
  `tasks.md` at a time, not the whole change.
- **Review** → `reviewer` agent (Sonnet by default; Opus for a final review if the
  engineer wants one). Suggest it after a slice, when an implementer report raises
  doubt (unsure items, unexpected files, failing or changed tests,
  security-sensitive code), and before the PR is marked ready.

Agents' reports are not shown to the engineer — relay what matters in a few lines.

## Git flow

- `main` is protected: changes land only through pull requests, merged by the owner.
- One change = one branch (named after the OpenSpec change) = one PR. The engineer
  decides when to open it, when to ask for review and when to mark it ready.
- Claude never merges PRs, never pushes to `main`, never force-pushes shared branches.
