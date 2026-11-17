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
| Propose, spec critique, spec gate | §1 steps 4-6, the tooling-changes paragraph |
| Tests (pass 1), test gate | §1 steps 7-8, §2 Testing |
| Code (pass 2), code review, code gate | §1 steps 9-10, §3 Architecture |
| Archive, final review | §1 steps 11-12 |
| Changes touching data, logs, deploy | §4 Secrets, §5 Observability, §6 Deployment |
| Changes touching teacher data or languages | §8 Multiple teachers, §9 Languages |
| Adding a dependency | §7 Simplicity |
| External services, hosting | §10 Access from Russia and the EU |

A change has at most 6 requirements. Every requirement has at least one
Given/When/Then scenario. A gate is approved by a PR comment naming it (for example
"tests approved") plus the matching box ticked in the PR description. Everything is
in English.

## Main agent is an orchestrator

Keep the main agent's context small. It talks to the engineer (whoever took the
task), writes and edits OpenSpec artifacts, decides, and delegates. It does not read
large parts of the codebase or write production code itself.

- **Before a proposal**: the main agent restates the task, proposes research angles
  and agrees them with the engineer. Explore is optional: never ask the engineer to
  run it first.
- **Investigation** (tracing behaviour, comparing options, web research): the
  `researcher` agent (medium model class), one per angle. Give it the task, the
  angle and what you need to decide. A built-in quick-search agent, where the tool
  has one, is only for finding where something is.
- **Spec critique**: the `spec-critic` agent (medium class), after the spec is
  drafted and before asking for the spec gate, on every change.
- **Implementation** (tests in pass 1, code in pass 2): the `implementer` agent
  (medium class). Give it one coherent slice of `tasks.md` at a time, not the whole
  change.
- **Review**: the `reviewer` agent (medium class). Run it:
  - after every pass 2 slice;
  - whenever an implementer report raises doubt (unsure items, unexpected files,
    failing or changed tests, security-sensitive code);
  - once per PR with the `code-gate-reviewer` agent (strong model class), before
    asking for the code gate.

  Fix findings as the Code review step in constitution §1 says, then re-review if
  the fix was non-trivial.

Agents' reports are not shown to the engineer: relay what matters in a few lines.

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
- Never merge PRs, never push to `main`, never force-push.
- Never tick a gate box in the PR description and never write an approval comment
  ("spec approved" and so on). Only the engineer approves. Show the engineer what
  `scripts/flow/gate-status` reports and let them decide.
- Use the scripts in `scripts/flow/` for git steps: `issue-brief` (read the issue),
  `change-start` (branch and new change), `pr-draft` (push and open the draft PR),
  `gate-request` (post the commit range), `gate-status` (check approvals),
  `pr-ready` (mark ready). They print a few lines only.

## Per-tool notes

- **Claude Code**: start a role with the Agent tool and the role name as the agent
  type. Skills are `/openspec-propose` and so on.
- **opencode**: roles are subagents. Start one with `@role-name` or let the main
  agent call it. Skills are `/openspec-propose` and so on.
- **Codex**: ask for the role by name ("spawn the researcher agent"). Skills are
  `$openspec-propose` and so on. Codex roles cannot be made read-only, so read-only
  roles are read-only by instruction only. Trust the project, or `.codex/` is ignored.
