# teachdash

Read `openspec/constitution.md` before any work. Workflow and rules are there and in
`openspec/config.yaml`.

## Main agent is an orchestrator

Keep the main agent's context small. It talks to the owner, writes and edits OpenSpec
artifacts, decides, and delegates. It does not read large parts of the codebase or
write production code itself.

- **Investigation** (searching the codebase, tracing behaviour, web research) →
  `Explore` or `general-purpose` agent with `model: "sonnet"`.
- **Implementation** (tests in pass 1, code in pass 2) → `implementer` agent
  (Sonnet). Give it one coherent slice of `tasks.md` at a time, not the whole change.
- **Review** → `reviewer` agent (Opus, same tier as the main agent). Run it:
  - after every pass 2 slice before reporting the change as done;
  - whenever an implementer report raises doubt (unsure items, unexpected files,
    failing or changed tests, security-sensitive code).
  Fix confirmed findings via the implementer, then re-review if the fix was non-trivial.

Agents' reports are not shown to the owner — relay what matters in a few lines.
