# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Sources and generator

- [x] 1.1 Add `.agents/roles/` with the five roles (design D2), copying the four prompts from `.claude/agents/`, reworded without tool names. The reviewer description must not say "the main agent asks for Opus". Verify: `grep -riE "claude|sonnet|opus|haiku" .agents/roles` finds nothing.
- [x] 1.2 Add `.agents/models.example` and gitignore entries for `.agents/models.local` and every generated path (design D3, D4). Verify: `git check-ignore` matches each generated path.
- [x] 1.3 Write `scripts/setup-agents` (design D4), including `--check`. Verify, and record the output in the PR:
  - with no map, each tool gets 5 agents with no model field;
  - with a sample map, the models appear as mapped, and Codex has both `model` and `model_reasoning_effort`;
  - Claude spec-critic, reviewer and code-gate-reviewer get no web tools, researcher gets them, and implementer has no `tools` field; the opencode files deny the same;
  - a removed role disappears on the next run;
  - a broken role file makes the script exit non-zero;
  - `--check` prints `stale` after a role edit and `ok` after a rerun.
- [x] 1.4 In one commit, move `.claude/skills/` to `.agents/skills/`, remove `.claude/agents/` from git, and run `setup-agents` (design D5, Migration Plan). Verify:
  - right after the commit, Claude Code still lists the agents and the skills;
  - `openspec update --force` on a fresh clone writes only under `.agents/skills/`.

## 2. Instructions and live runs

Tasks 2.2 and 2.3 need the tool installed and signed in on the engineer's machine.

- [x] 2.1 Write `AGENTS.md` (design D1) and reduce `CLAUDE.md` to `@AGENTS.md`. Verify:
  - `wc -c AGENTS.md` is under 32768;
  - a checklist in the PR maps every rule in the old `CLAUDE.md` to its line in `AGENTS.md`, including the three reviewer triggers, the "never merge, push to `main` or force-push a shared branch" rule, and the no-ticking and no-approval rule;
  - `grep -iE "claude|sonnet|opus" AGENTS.md` finds only the per-tool notes.
- [ ] 2.2 Live run in Claude Code: a new session lists the 5 project agents and loads the `AGENTS.md` rules, and the researcher starts. Verify: record the agent list in the PR.
- [ ] 2.3 Live run in opencode. Check that:
  - the 5 agents appear as subagents, not as primary agents;
  - each OpenSpec skill appears once;
  - a role with no map entry runs on the session model.

  Verify: record the output in the PR, and the D5 fix if one was needed.

Codex has no live run: nobody on the team uses it yet (proposal, clarification 11). The generated Codex files are checked only by task 1.3.

## 3. Flow scripts and permissions

- [x] 3.1 Write `scripts/flow/issue-brief`, `change-start`, `pr-draft` and `pr-ready` (design D6). Verify on a throwaway issue and branch, then close and delete them:
  - each script prints only a few lines;
  - each writing script refuses on `main` and on a detached HEAD;
  - `pr-draft` fails visibly when the push is rejected.
- [x] 3.2 Write `scripts/flow/gate-request`, `gate-status` and `test`, with the fixtures (design D6). Verify:
  - `scripts/flow/test` passes for every fixture case;
  - on this PR, `gate-request` posts the spec gate range and changes only its own line in "Review ranges", with the boxes untouched;
  - `gate-status` reports no approval before the engineer's comment, and the spec gate after it.
- [ ] 3.3 Add `.claude/settings.json`, the `opencode.json` permissions and `.codex/rules/flow.rules` (design D7). Verify in Claude Code and opencode:
  - a flow script runs without a prompt;
  - `git push --force` is refused, and `git push --force-with-lease` asks for confirmation.

## 4. Documentation and CI

- [x] 4.1 Reword constitution §1 as in design D8 (approved by the owner, proposal clarification 10). Verify:
  - `grep -nE "Claude|Opus|Sonnet" openspec/constitution.md` finds nothing;
  - the diff changes wording only.
- [x] 4.2 Update `CONTRIBUTING.md` and the `.github/CODEOWNERS` comment (design D8). Verify: `grep -n "Claude" CONTRIBUTING.md` finds only per-tool notes.
- [ ] 4.3 Add the CI step (design D9). Verify:
  - the step is green on the PR;
  - a temporary broken role file, and then a temporary shellcheck error, each make it red;
  - both temporary changes are reverted with new commits.

  Record the run links.

## Workflow follow-up

- Code review gate: per-slice reviews, then the code-gate review on the strong class. This is the first real use of the `code-gate-reviewer` role.
- Archive in the same PR. There are no spec deltas, so `openspec/specs/` does not change.
- Mark the PR ready for a code owner other than the engineer.
- After merge, each developer runs `scripts/setup-agents`.
