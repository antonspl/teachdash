# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Design sources and project rules

- [ ] 1.1 Add `.agents/design/licenses/` with the five upstream licence files, verbatim, named `<owner>-<repo>.LICENSE` (design D10). Verify: each file matches the upstream licence at the pinned commit (`diff` against a fresh fetch).
- [ ] 1.2 Add `.agents/design/project-rules.md` (design D2). Verify: it lists the base rules and C0-C14, and every C item is applied in the source files of 1.4 and 1.5.
- [ ] 1.3 Add `.agents/design/README.md`: reading order, precedence and the source/path/commit table (design D1). Verify: every file in `.agents/design/` is listed, and every commit matches D1.
- [ ] 1.4 Add `checklist.md` from vercel-labs/web-interface-guidelines @434b7f9, with the three impeccable audit points in our own words (design D1). Verify: the drop list is gone (grep in 1.7), and the three points are present.
- [ ] 1.5 Add `accessibility.md`, `accessibility-wcag22.md`, `accessibility-patterns.md`, `refactoring-ui.md`, `refactoring-ui-patterns.md`, `frontend-design-copy.md` and `critique-rubric.md`, each fetched by its pinned commit and adapted as design D1 says. Verify:
  - `critique-rubric.md` comes from impeccable `skill/reference/`, has no `{{` placeholders and is about 200 lines;
  - the focus-on-error example is replaced by the pointer to web-client R14-S12 and R17-S3;
  - no AAA table is left.
- [ ] 1.6 Check attribution (design D10). Verify: `head -1` of every vendored file names the source, path, pinned commit, licence and "modified by teachdash".
- [ ] 1.7 Check for leftovers. Verify, and record in the PR: `grep -rniE 'lighthouse|mcp|npx |tailwind|fonts\.googleapis|preconnect|\$ARGUMENTS|impeccable init|PRODUCT\.md|amazon|title case|uppercase|virtua|nuqs|AAA' .agents/design/*.md` finds only lines in `project-rules.md` or the README that name a rule or a source. `wc -l .agents/design/*.md` totals about 1,350.

## 2. frontend-implementer role

- [ ] 2.1 Add `.agents/roles/frontend-implementer.md` (design D3). Verify, and record in the PR:
  - `scripts/setup-agents` generates it for Claude Code, opencode and Codex without script changes;
  - `scripts/setup-agents --check` prints `ok`;
  - the body has no three single quotes;
  - `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 3. Reviewers and researcher

- [ ] 3.1 Update `.agents/roles/reviewer.md`: the frontend Check bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Proven by the dry run in 5.1.
- [ ] 3.2 Update `.agents/roles/code-gate-reviewer.md`: the project-rules bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Verify: `grep -n '.agents/design' .agents/roles/code-gate-reviewer.md` shows both lines.
- [ ] 3.3 Update `.agents/roles/researcher.md` with the design-angle rule (design D5). Proven by the dry run in 5.1.

## 4. AGENTS.md, constitution, config, .gitignore

- [ ] 4.1 Update `AGENTS.md` (design D4, D5, D6, D7): the commit-before-delegating line, the design angle, out-of-scope problems filed after agreement, frontend routing, the frontend review check, and "implementer or frontend-implementer report". Verify: `grep -n 'frontend-implementer\|design angle\|area:frontend' AGENTS.md` shows each point.
- [ ] 4.2 Edit constitution §1 step 2 and step 7 (design D5, D9). Verify: `git diff main -- openspec/constitution.md` touches only those two steps.
- [ ] 4.3 Add the layer-split rule to `openspec/config.yaml` `rules.tasks` (design D4). Verify: `npx -y @fission-ai/openspec@1.16.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule.
- [ ] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved there does not show in `git status`.

## 5. Proof

- [ ] 5.1 Dry run in Claude Code, after `scripts/setup-agents` and a restart, on a small real frontend issue (for example #165), on a branch off this one, not in this PR. Run it up to the design questions and one slice:
  - a `researcher` on the design angle returns design questions, and existing-UI problems apart;
  - the main agent asks them before any code;
  - the `frontend-implementer` builds one slice and lists its screenshots in `frontend/screenshots/` at desktop and 360 px;
  - the `reviewer` checks the slice against `.agents/design/` and the screenshots.

  Verify, and record in the PR: the questions and when they were asked; the screenshot paths, with the screenshots attached; the reviewer's design findings; no `zshots` spec in the slice's commit; `implementer.md` unchanged. Out-of-scope findings are listed to the engineer and filed only after agreement (#168 is already filed).

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution.
