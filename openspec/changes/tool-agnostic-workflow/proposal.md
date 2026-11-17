# Proposal

## Clarifications

Questions asked before this proposal, and the engineer's answers (2026-11-17):

1. **Which tools are supported?** Claude Code, opencode and Codex CLI. Cursor is not a target, but it reads `.claude/agents` by itself.
2. **Do generated tool files go into git?** No. A setup script generates them locally, and they are gitignored.
3. **Which models do the agent roles use?** A role names a model class (`strong` or `medium`), not a model. Each developer maps classes to models in a personal, gitignored file. Decided after research angle 4.
4. **Do git and GitHub routines move to scripts?** Yes, in this change, with permission rules so they run without prompts in all three tools.
5. **Is a cheap-model git agent needed?** No. Research angle 3 showed that a script is cheaper than spawning an agent.
6. **Is there a rule for signing agent commits?** No. Each tool signs commits in its own way.
7. **Is the constitution re-read in scope?** Yes, in this change. `AGENTS.md` carries a short summary, and the agent reads the full constitution by section when a step needs it.
8. **Which platforms?** macOS and Linux only. Windows is not supported for now.
9. **Is a fifth role for the code-gate review acceptable, and may skills move out of `.claude/`?** To be confirmed by the engineer at the spec gate (design D2, D5).
10. **May the constitution be edited here?** Yes, approved by the owner (2026-11-17). The wording changes in design D8, done after the spec gate.
11. **Is Codex proven by a live run?** No. Nobody on the team uses Codex yet. Its files are generated but not tried in Codex; problems are fixed when someone starts using it, without a follow-up issue.

## Research

All five angles were agreed with the engineer.

1. **What each tool reads.** No single layout works for all three tools. `AGENTS.md` is read by opencode and Codex. opencode reads `CLAUDE.md` only when there is no `AGENTS.md`. Agent files differ in path, format and model naming. Claude Code uses `.claude/agents/*.md`, opencode uses `.opencode/agents/*.md` with `mode: subagent`, and Codex uses `.codex/agents/*.toml`. Conclusion: one neutral source, plus a generator.
2. **Claude-specific content in the repo.** The coupling sits in `CLAUDE.md`, `.claude/agents/` and `.claude/skills/`, plus about ten phrases in the constitution and `CONTRIBUTING.md` that name Claude, Opus or slash commands. `.github/` has no Claude text. `scripts/` has no git routines.
3. **Cost of git and GitHub output.** Across the last 15 sessions, git and gh calls returned about 370k characters, up to 34% of all tool output. That figure is an upper bound: 8 of the 10 largest outputs also printed the constitution and the config. Spawning a subagent costs 7k to 30k tokens, which is more than a typical git output. Conclusion: use scripts only, with short output.
4. **Model mapping.** A model ID in a repo agent file beats the developer's own config in opencode and Codex. A developer without that provider cannot override it. When the model is omitted, the agent inherits the session's model in all three tools. Conclusion: a personal class map, read by the generator.
5. **Codex details.** Codex reads skills from `.agents/skills`, not from `.claude/skills`. Project config lives in `.codex/config.toml` and allowed commands in `.codex/rules/*.rules`. Codex loads `.codex/` only for a trusted project. A Codex subagent cannot be made read-only. It inherits the parent's sandbox. `AGENTS.md` is capped at 32 KiB and has no imports.

## Why

The workflow depends on Claude Code. A ban, an outage or a price change for one developer stops their work, and a new developer must use Claude Code to follow the process. The process itself, meaning the gates, roles and OpenSpec, does not need any specific tool.

## What Changes

- **`AGENTS.md`** becomes the instruction file for every tool. It holds the content of today's `CLAUDE.md` in neutral wording, plus a short summary of the constitution that says which section to read for which step. `CLAUDE.md` becomes a one-line import of `AGENTS.md`.
- **Agent roles** (researcher, spec-critic, implementer, reviewer, code-gate reviewer) are defined once in `.agents/roles/`. Each role has a prompt, a model class, an access level and a web flag. The code-gate reviewer is today's per-PR review on Opus, made a role of its own.
- **Skills** are kept once in `.agents/skills/`. Codex and opencode read this folder directly, and `openspec update` refreshes it. `.claude/skills/` is a generated copy.
- **`scripts/setup-agents`** generates the agent files for each tool and the Claude skills copy from the sources and the personal model map (`.agents/models.local`). Its output is gitignored.
- **Git and GitHub scripts** in `scripts/flow/` cover issue brief, change start, draft PR, gate request, gate status and PR ready. Their output is short, and they exit non-zero on any failure.
- **Permission rules** let the flow scripts run without prompts and forbid force push and push to `main`: `.claude/settings.json`, `opencode.json` and `.codex/rules/`.
- **Neutral wording** in the constitution, `CONTRIBUTING.md` and `.github/CODEOWNERS`. "Claude" becomes "the agent", and "the Opus review" becomes "the code-gate review". Slash commands are named per tool.
- **`CONTRIBUTING.md`** gets a setup section for each tool: run `setup-agents`, fill in the model map, and trust the project in Codex.

## Out of Scope

- Windows support.
- Cursor and other tools beyond the three named. They can be added later through the same generator.
- A rule for signing agent commits.
- Read-only enforcement in Codex beyond instructions and permission rules. Codex offers no per-agent sandbox.
- Changes to OpenSpec itself, or to the gate process.
- Rewording archived changes under `openspec/changes/archive/`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling, with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **New:** `AGENTS.md`, `.agents/roles/`, `.agents/skills/` (moved from `.claude/skills/`), `.agents/models.example`, `scripts/setup-agents`, `scripts/flow/*`, `.claude/settings.json`, `opencode.json`, `.codex/rules/flow.rules`.
- **Changed:** `CLAUDE.md`, `openspec/constitution.md`, `CONTRIBUTING.md`, `.github/CODEOWNERS`, `.gitignore`, `.github/workflows/ci.yml`.
- **Removed from git:** `.claude/agents/` and `.claude/skills/`, which become generated output.
- **Process:** after cloning, and after any role or skill change, each developer runs `scripts/setup-agents`. If they don't, the tool runs with stale or missing agents.
- **OpenSpec updates:** `openspec update` refreshes every skills folder it finds. With only `.agents/skills/` in git, that is the folder that changes. Then `setup-agents` refreshes the Claude copy.
- **Live runs** in tasks 2.2 to 2.4 need each tool installed and signed in on the engineer's machine.
