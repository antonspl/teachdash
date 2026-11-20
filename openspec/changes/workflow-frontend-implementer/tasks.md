# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0` (as `scripts/flow/change-start`).

## 1. Design sources and project rules

- [ ] 1.1 Add `.agents/design/licenses/` with the five upstream licence files, verbatim, named `<owner>-<repo>.LICENSE`; for anthropics/skills take `skills/frontend-design/LICENSE.txt` @e4bb5d9 (design D10). Verify: each file matches the upstream licence file at the pinned commit (`diff` against a fresh fetch).
- [ ] 1.2 Add `.agents/design/project-rules.md` (design D2). Verify, and record in the PR: it lists the base rules and C0-C14; for each C item, a `grep` over the source files of 1.4 and 1.5 for the dropped pattern finds nothing outside `project-rules.md` and the README (for example C1 `Session Expiring|Save API Key`, C2 `uppercase`, C3 `#[0-9a-fA-F]{3,6}\b|[0-9]+px`, C4 `font-family|fonts\.googleapis|distinctive`, C8 `autocomplete="off"`, C9 `44x44|44 ?px`, C13 `P0|P1|P2|P3|/10|score`, C14 `illustrat|<img`), and a `grep` for the rewritten form finds it (for example `--space-`, `--color-`, `src/ui/`, `<dialog>`, `24 CSS px`, `R14-S12`); items with no searchable pattern (C0, C10) are checked by reading and noted as such.
- [ ] 1.3 Add `.agents/design/README.md`: reading order, precedence, the source/path/commit/licence-file table and the NOTICE note (design D1, D10). Verify: every file in `.agents/design/` is listed, every commit matches D1, and the reading order is `README.md`, `project-rules.md`, `checklist.md`, then `accessibility.md` for slices that add or change interactive elements, forms or dialogs.
- [ ] 1.4 Add `checklist.md` from vercel-labs/web-interface-guidelines @434b7f9, with the three impeccable audit points in our own words (design D1). Verify: the drop list is gone (grep in 1.7), and the three points are present.
- [ ] 1.5 Add `accessibility.md`, `accessibility-wcag22.md`, `accessibility-patterns.md`, `refactoring-ui.md`, `refactoring-ui-patterns.md`, `frontend-design-copy.md` and `critique-rubric.md`, each fetched by its pinned commit and adapted as design D1 says. Verify:
  - `critique-rubric.md` comes from impeccable `skill/reference/`, has no `{{` placeholders and is about 200 lines;
  - the focus-on-error example is replaced by the pointer to web-client R14-S12 and R17-S3;
  - no AAA table is left.
- [ ] 1.6 Check attribution (design D10). Verify: `head -1` of every vendored file names the source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/` and "modified by teachdash", and the named licence file exists.
- [ ] 1.7 Check for leftovers. Verify, and record in the PR: `grep -rniE 'lighthouse|mcp|npx |tailwind|fonts\.googleapis|preconnect|\$ARGUMENTS|impeccable init|PRODUCT\.md|amazon|title case|uppercase|virtua|nuqs|AAA' .agents/design/*.md` finds only lines in `project-rules.md` or the README that name a rule or a source. `wc -l .agents/design/*.md` totals about 1,350.

## 2. frontend-implementer role

- [ ] 2.1 Add `.agents/roles/frontend-implementer.md` (design D3). Verify, and record in the PR:
  - the frontmatter has the `description` from D3, class `medium`, access `write`, web `true`;
  - the body has the reading order, the "this file wins" line, the pass rules and the additions from D3;
  - the body has no three single quotes (`grep -c "'''"` prints 0);
  - `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 3. Reviewers and researcher

- [ ] 3.1 Update `.agents/roles/reviewer.md`: the frontend Check bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Proven by the dry run in 5.1 on the throwaway branch; its last SHA is recorded in the PR.
- [ ] 3.2 Update `.agents/roles/code-gate-reviewer.md`: the project-rules bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Verify: `grep -n '.agents/design' .agents/roles/code-gate-reviewer.md` shows both lines.
- [ ] 3.3 Update `.agents/roles/researcher.md` with the design-angle rule (design D5). Proven by the dry run in 5.1 on the throwaway branch; its last SHA is recorded in the PR.
- [ ] 3.4 After 2.1 and 3.1-3.3, run `scripts/setup-agents`. Verify, and record in the PR: it generates `frontend-implementer` for Claude Code, opencode and Codex without script changes, and `scripts/setup-agents --check` prints `ok`.

## 4. AGENTS.md, constitution, config, .gitignore

- [ ] 4.1 Update `AGENTS.md` (design D4, D5, D6, D7). Verify, and record in the PR, each of the six points with its own check:
  - the commit-before-delegating line names the `frontend-implementer`: `grep -n -A2 'Before delegating' AGENTS.md`;
  - the design angle: proposed at Clarify for a change to UI under `frontend/src`, droppable for a small task, at Explore only if Explore is run, covering the existing screens the change touches: `grep -n 'design angle' AGENTS.md` shows each part;
  - out-of-scope problems filed only after the engineer agrees: `grep -n 'area:frontend' AGENTS.md`;
  - routing of slices touching `frontend/`, including the pass 1 frontend group, to `frontend-implementer`: `grep -n -A4 'Implementation' AGENTS.md`;
  - the frontend review check against `.agents/design/` and the screenshots: `grep -n '.agents/design' AGENTS.md` shows it in the review bullet;
  - `grep -n 'implementer or frontend-implementer report' AGENTS.md` finds the line.
- [ ] 4.2 Edit constitution §1 step 2 with the design-angle sentence (design D5). Verify: `git diff main -- openspec/constitution.md` touches only step 2.
- [ ] 4.3 Add the layer-split rule to `openspec/config.yaml` `rules.tasks` and amend line 27 as design D4 says. Verify: `npx -y @fission-ai/openspec@1.15.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule and the amended line 27, and `git diff main -- openspec/config.yaml` touches only those two rules.
- [ ] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved in `frontend/screenshots/<change-name>/` does not show in `git status`.

## 5. Proof

- [ ] 5.1 Dry run in Claude Code, after 3.4 and a restart (design D12):
  - #165 goes through Clarify and Research up to the design questions; no spec is written for #165;
  - a `researcher` on the design angle returns design questions, and existing-UI problems apart;
  - the main agent asks them before any code;
  - on a throwaway branch off this one, the `frontend-implementer` builds one slice and lists its screenshot paths in `frontend/screenshots/<change-name>/` at desktop and 360 px;
  - the `reviewer` checks the slice against `.agents/design/` and the screenshots.

  Verify, and record in the PR: the questions and when they were asked; the screenshot paths from the report (the engineer attaches images by hand if wanted); the reviewer's design findings; no `zshots` spec in the slice's commit; `implementer.md` unchanged; the throwaway branch's last SHA. Then delete the branch (local, and remote if pushed); no #165 code survives. Out-of-scope findings are listed to the engineer and filed only after agreement (#168 is already filed). opencode and Codex are not run; the PR states they are unverified.

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution.
