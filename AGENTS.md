# teachdash

## First rule: agent setup

Agent files are generated and not in git. If the tool's agent folder is missing
(`.claude/agents/`, `.opencode/agents/` or `.codex/agents/`), or
`scripts/setup-agents --check` prints `stale` (exit 1), run `scripts/setup-agents`
and restart the session. Roles are defined in `.agents/roles/`, skills in
`.agents/skills/`. Each developer maps model classes to models in
`.agents/models.local` (see `.agents/models.example`).

## Read the constitution by section

`openspec/constitution.md` holds the rules; `openspec/config.yaml` holds the OpenSpec
config. Do not read the constitution all at once. Read the section for the current
step.

- Engineer: the team member who took the task. Approves the three gates.
- Code owner: listed in `.github/CODEOWNERS`. One other than the engineer gives the
  final approval. The owner alone may merge without it.
- One change = one branch = one PR. Three gates, each approved by the engineer
  (spec, tests, code), then a final review by a code owner.

| Step | Read |
|---|---|
| Any step, workflow and roles | §1 Workflow |
| Explore, Clarify, Research | §1 steps 1-3 |
| Propose, spec critique, spec gate | §1 steps 4-6, the tooling-changes paragraph; §8 and §9 if the change touches teacher data or languages |
| Tests (pass 1), test gate | §1 steps 7-8, §2 Testing |
| Code (pass 2), code review, code gate | §1 steps 9-10, §3 Architecture |
| Archive, final review | §1 steps 11-12 |
| Changes touching data, logs, deploy | §4 Secrets, §5 Observability, §6 Deployment |
| Changes touching teacher data or languages | §8 Multiple teachers, §9 Subjects and languages |
| Adding a dependency | §7 Simplicity |
| External services, hosting | §10 Access from Russia and the EU |

A change has at most 6 requirements. Every requirement has at least one
Given/When/Then scenario. A gate is approved by a PR comment naming it (for example
"tests approved") plus the matching box ticked in the PR description. Everything is
in English.

## Main agent is an orchestrator

Keep the main agent's context small. It talks to the engineer (whoever took the
task), decides, and delegates. It does not read large parts of the codebase, write
production code or draft OpenSpec artifacts itself. Before delegating to the
`implementer`, the `frontend-implementer` or the `spec-writer`, it commits its own
pending edits, staged by path, so no agent commits them. Agents do not push: the
main agent pushes their commits after reading the report (the flow scripts push
too).

- **Before a proposal**: the main agent restates the task, proposes research angles
  and agrees them with the engineer. Explore is optional: never ask the engineer to
  run it first. A change touching `frontend/src` gets at least one
  `frontend-researcher` at Clarify; as for any change, the engineer may agree that a
  small task needs no research.
- **Investigation** (tracing behaviour, comparing options, web research): the
  `researcher` agent (medium model class), one per angle. Angles about the UI
  (screens, components, frontend behaviour) go to the `frontend-researcher` agent
  (medium class) instead. Give it the task, the angle and what you need to decide.
  A built-in quick-search agent, where the tool has one, is only for finding where
  something is.
  The `frontend-researcher` returns design questions on the screens the change
  touches. The main agent asks the engineer the questions; the answers go into the
  proposal's Clarifications and the "UI design" section of `design.md`.
- **Writing artifacts** (proposal, spec deltas, design, tasks, and every fix after
  critique or engineer comments): the `spec-writer` agent (medium class). Brief it
  with the change name, the issue and PR, the engineer's decisions as a numbered list,
  the research conclusions and, for a fix, the findings with the decision on each. It
  records decisions as given and does not take new ones. The main agent may make a
  one- or two-line edit in one file itself.
  The spec-writer, not the main agent, follows the OpenSpec propose and update skills
  for writing; the main agent uses them only to agree the plan.
- **Spec critique**: the `spec-critic` agent (medium class), after the spec is
  drafted and before asking for the spec gate, on every change. For a change
  touching `frontend/src`, the `frontend-spec-critic` agent (medium class) runs
  instead. The main agent asks the engineer its unanswered design questions, and the
  spec-writer records the answers in the fix round.
- **Implementation** (tests in pass 1, code in pass 2): the `implementer` agent
  (medium class). Give it one coherent slice of `tasks.md` at a time, not the whole
  change. Implementation slices that touch `frontend/` go to the
  `frontend-implementer` agent (medium class) instead. The "Failing tests" section
  (pass 1) of a change with both layers stays with the `implementer`.
- **Review**: the `reviewer` agent (medium class). Run it:
  - after every implementation slice: pass 2, or the single pass of a frontend-only
    change;
  - whenever an implementer or frontend-implementer report raises doubt (unsure
    items, unexpected files, failing or changed tests, security-sensitive code);
  - once per PR with the `code-gate-reviewer` agent (strong model class), before
    asking for the code gate.

  A slice that touches `frontend/` goes to the `frontend-reviewer` agent (medium
  class) instead of the `reviewer`. For a PR touching `frontend/src`, before the
  code-gate review the main agent has the `frontend-implementer` take the final
  screenshot set and passes its paths to the `code-gate-reviewer`. Fix findings as
  the Code review step in constitution §1 says, then re-review if the fix was
  non-trivial.

Agents' reports are not shown to the engineer: relay what matters in a few lines.

## Sessions

After posting a gate request, the main agent ends its turn with one line: the gate and
the range. After approval, it continues in the same session. If the session has ended,
a new one resumes from the branch, `scripts/flow/gate-status` (approvals, and whether the branch is pushed), the PR description and last gate request
comment, the proposal's Clarifications and `tasks.md` (its ticks show where a phase
stopped). It does not reread earlier phases' diffs.

## Git flow

- `main` is protected: changes land only through pull requests, approved by a code
  owner other than the author and merged by anyone on the team.
- One change = one branch (named after the OpenSpec change) = one PR, opened as a
  draft right after the spec is written. Spec, tests and code are added to it in
  turn; each stops at its review gate (constitution §1). Check the PR for a comment
  approving the current gate (for example "tests approved") before starting the next
  phase. When asking for a gate review, post the commit range of that phase.
- When the change is archived and all gates are approved, mark the PR ready for the
  final review by a code owner other than the engineer.
- Never merge PRs, never push to `main`, never force-push a shared branch. After a
  rebase of the change's own branch, push with `--force-with-lease`; the tool asks
  the engineer first. The rebase changes the commit hashes, so post the current
  gate's range again with `gate-request`.
- Never tick a gate box in the PR description and never write an approval comment
  ("spec approved" and so on). Only the engineer approves. Show the engineer what
  `scripts/flow/gate-status` reports and let them decide.
- Use the scripts in `scripts/flow/` for git steps: `issue-brief` (read the issue),
  `change-start` (branch and new change), `pr-draft` (push and open the draft PR),
  `gate-request` (post the commit range), `gate-status` (check approvals),
  `pr-ready` (mark ready). They print a few lines only.
- Do not change `scripts/flow/`, `scripts/setup-agents`, `.agents/roles/` or the permission
  files (`.claude/settings.json`, `opencode.json`, `.codex/rules/`) unless the task
  requires it. The reviewer checks this.

## Per-tool notes

- **Claude Code**: start a role with the Agent tool and the role name as the agent
  type. Skills are `/openspec-propose` and so on.
- **opencode**: roles are subagents. Start one with `@role-name` or let the main
  agent call it. Skills are `/openspec-propose` and so on.
- **Codex**: ask for the role by name ("spawn the researcher agent"). Skills are
  `$openspec-propose` and so on. Codex roles cannot be made read-only, so read-only
  roles are read-only by instruction only. Trust the project, or `.codex/` is ignored.
