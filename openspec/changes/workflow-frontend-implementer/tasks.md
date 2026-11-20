# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0` (as `scripts/flow/change-start`).

## 1. Design sources and project rules

- [ ] 1.1 Add `.agents/design/licenses/` with the five upstream licence files, verbatim, named `<owner>-<repo>.LICENSE`; for anthropics/skills take `skills/frontend-design/LICENSE.txt` @e4bb5d9 (design D10). Verify: each file matches the upstream licence file at the pinned commit (`diff` against a fresh fetch).
- [ ] 1.2 Add `.agents/design/project-rules.md` (design D2). Verify, and record in the PR: it lists the base rules and C0-C14; for each C item, a `grep` over the source files of 1.4 and 1.5 for the dropped pattern finds nothing outside `project-rules.md` and the README (for example C1 `Session Expiring|Save API Key`, C2 `uppercase`, C3 `:[[:space:]]*#[0-9a-fA-F]{3,8}\b|[0-9]+px` (the hex part needs a CSS context, a colon before the value, so issue numbers such as `#168` and anchors do not match), C4 `font-family|fonts\.googleapis|distinctive`, C8 `autocomplete="off"`, C9 `44x44|44 ?px`, C13 `P0|P1|P2|P3|/10|score`, C14 `illustrat|<img`), and a `grep` for the rewritten form finds it (for example `--space-`, `--color-`, `src/ui/`, `<dialog>`, `24 CSS px`, `R14-S12`); items with no searchable pattern (C0, C10) are checked by reading and noted as such.
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
  - the body has the reading order, the "this file wins" line, the pass rule (pass 2 of a change with both layers, or the single pass of a frontend-only change; no pass 1 failing tests) and the additions from D3 (only new role-named tokens; screenshots of each touched page and each changed state);
  - the body has no three single quotes (`grep -c "'''"` prints 0);
  - `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 3. Reviewers and researcher

- [ ] 3.1 Update `.agents/roles/reviewer.md`: the frontend Check bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Proven by the dry run in 5.1 on the throwaway branch; its last SHA is recorded in the PR.
- [ ] 3.2 Update `.agents/roles/code-gate-reviewer.md`: the project-rules bullet and `.agents/design/` on the "no unrequested changes" list (design D7). Verify: `grep -n '.agents/design' .agents/roles/code-gate-reviewer.md` shows both lines.
- [ ] 3.3 Update `.agents/roles/researcher.md` with the design-angle rule (design D5). Proven by the dry run in 5.1 on the throwaway branch; its last SHA is recorded in the PR.
- [ ] 3.4 After 2.1 and 3.1-3.3, run `scripts/setup-agents`. Verify, and record in the PR: it generates `frontend-implementer` for Claude Code, opencode and Codex without script changes, and `scripts/setup-agents --check` prints `ok`.

## 4. AGENTS.md, constitution, config, .gitignore

- [ ] 4.1 Update `AGENTS.md` (design D4, D5, D6, D7). Verify, and record in the PR, each of the six points with its own check. The checks run on the file with line wraps removed, so a wrapped line still matches: `flat` below means `tr '\n' ' ' < AGENTS.md | tr -s ' '`, and each check is `flat | grep -o '<term>'` for every term listed, followed by reading the matched paragraph:
  - the commit-before-delegating line names the `frontend-implementer`: `flat | grep -oE 'Before delegating[^.]*frontend-implementer'`;
  - the design angle: the terms `design angle`, `frontend/src`, `Clarify`, `small task`, `Explore is run` and `existing screens` are found, and reading shows they belong to the design-angle text;
  - design and UI problems in existing UI outside the change filed only after the engineer agrees, other findings by step 10: the terms `area:frontend`, `engineer agrees` and `step 10` are found;
  - routing of implementation slices touching `frontend/` to `frontend-implementer`, with "Failing tests" staying with `implementer`: `flat | grep -oE 'Implementation[^*]*frontend-implementer'`;
  - the frontend review check against `.agents/design/` and the screenshots: the terms `.agents/design/` and `screenshots` are found in the review bullet;
  - `flat | grep -o 'implementer or frontend-implementer report'` finds the phrase.
- [ ] 4.2 Edit constitution §1 step 2 with the design-angle sentence (design D5) and step 10 with the own-UI sentence (design D7). Verify: `git diff main -- openspec/constitution.md` touches only steps 2 and 10, one sentence each.
- [ ] 4.3 Add the layer-split rule for implementation task groups to `openspec/config.yaml` `rules.tasks` as design D4 says; lines 27 and 38 are not changed. Verify: `npx -y @fission-ai/openspec@1.15.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule, and `git diff main -- openspec/config.yaml` adds only that one rule.
- [ ] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved in `frontend/screenshots/<change-name>/` does not show in `git status`.

## 5. Proof

- [ ] 5.1 Dry run in Claude Code, after 3.4 and a restart (design D12):
  - #165 goes through Clarify and Research up to the design questions; no spec deltas are written for #165;
  - a `researcher` on the design angle returns design questions, and existing-UI problems apart;
  - the main agent asks them before any code;
  - on a throwaway branch off this one, a scratch change `ui-required-fields` is created by `scripts/flow/change-start` or `openspec new change`, never by hand; its proposal Clarifications hold the design answers and its minimal `tasks.md` holds the slice;
  - the `frontend-implementer` builds that slice and lists its screenshot paths in `frontend/screenshots/ui-required-fields/`: each page the slice touches and each state it changes (for example the error shown after an empty submit, or a dialog open), at desktop and 360 px;
  - the `reviewer` checks the slice against `.agents/design/` and the screenshots.

  Verify, and record in PR #169: the questions and when they were asked; the screenshot paths from the report (the engineer attaches images by hand if wanted); the reviewer's design findings and their triage (design-rule and WCAG 2.2 AA violations in UI the slice adds or changes are fixed on the throwaway branch); no `zshots` spec in the slice's commit; `implementer.md` unchanged; the throwaway branch's last SHA. Then delete the branch (local, and remote if pushed), the scratch change and the code; no #165 code survives. Design and UI problems in existing UI outside the slice are listed to the engineer and filed only after agreement (#168 is already filed). opencode and Codex are not run, and routing in a change with both layers is not exercised (#165 is frontend-only); the PR states both are unverified.

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution (§1 steps 2 and 10).
