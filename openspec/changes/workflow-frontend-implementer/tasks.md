# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate; the dry run's proof (13.1) is screenshots that the engineer attaches to the PR by hand. All tasks start after the spec gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0` (as `scripts/flow/change-start`).

Tasks 1-4 were done before the post-gate revision (proposal Clarifications, G-1 to G-11) and stay ticked. Groups 5-12 rework part of them: 6.1 removes the design-angle bullet of 3.3, 6.2 moves the frontend bullet of 3.1 into the `frontend-reviewer`, 8.1 replaces the design-angle text of 4.1, 9.1 replaces the step 2 sentence of 4.2, and 11.2 removes the out-of-scope filing line of 1.2. The dry run, formerly 5.1, is now 13.1.

## 1. Design sources and project rules

- [x] 1.1 Add `.agents/design/licenses/` with the five upstream licence files, verbatim, named `<owner>-<repo>.LICENSE`; for anthropics/skills take `skills/frontend-design/LICENSE.txt` @e4bb5d9 (design D10). Verify: each file matches the upstream licence file at the pinned commit (`diff` against a fresh fetch).
- [x] 1.2 Add `.agents/design/project-rules.md` (design D2). Verify, and record in the PR: it lists the base rules and C0-C14; for each C item, a `grep -nE` over the source files of 1.4 and 1.5 for the dropped pattern finds nothing outside `project-rules.md` and the README, and a `grep` for the rewritten form finds it (for example `--space-`, `--color-`, `src/ui/`, `<dialog>`, `24 CSS px`, `R14-S12`). The dropped patterns:
  - C1 `Session Expiring|Save API Key`; C2 `uppercase`; C8 `autocomplete="off"`; C9 `44x44|44 ?px`; C13 `P0|P1|P2|P3|/10|score`; C14 `illustrat|<img`;
  - C3, aligned with `frontend/scripts/check-design-tokens.mjs`: hex `:[[:space:]]*#[0-9a-fA-F]{3,8}\b` (a colon before the value, so issue numbers such as `#168` and anchors do not match), and lengths `[0-9]+(px|rem)\b` on lines that are not `@media` lines (`grep -nE '[0-9]+(px|rem)\b' <files> | grep -v '@media'`), because the script does not check `@media` conditions;
  - C4, aligned with the same script: `fonts\.googleapis|distinctive`, and `font-family:` or `font:` only when the value is not a single `var(...)` (``grep -nE 'font(-family)?:' <files> | grep -vE 'font(-family)?:[[:space:]]*var\([^()]*(\([^()]*\))?[^()]*\)[[:space:]]*(;|\}|`|$)'``).

  Items with no searchable pattern (C0, C10) are checked by reading and noted as such.
- [x] 1.3 Add `.agents/design/README.md`: reading order, precedence, the source/path/commit/licence-file table and the NOTICE note (design D1, D10). Verify: every file in `.agents/design/` is listed, every commit matches D1, and the reading order is `README.md`, `project-rules.md`, `checklist.md`, then `accessibility.md` for slices that add or change interactive elements, forms or dialogs.
- [x] 1.4 Add `checklist.md` from vercel-labs/web-interface-guidelines @434b7f9, with the three impeccable audit points in our own words (design D1). Verify: the drop list is gone (grep in 1.7), and the three points are present.
- [x] 1.5 Add `accessibility.md`, `accessibility-wcag22.md`, `accessibility-patterns.md`, `refactoring-ui.md`, `refactoring-ui-patterns.md`, `frontend-design-copy.md` and `critique-rubric.md`, each fetched by its pinned commit and adapted as design D1 says. Verify:
  - `critique-rubric.md` comes from impeccable `skill/reference/`, has no `{{` placeholders and is about 200 lines;
  - the focus-on-error example is replaced by the pointer to web-client R14-S12 and R17-S3;
  - no AAA table is left.
- [x] 1.6 Check attribution (design D10). Verify: `head -1` of every vendored file names the source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/` and "modified by teachdash", and the named licence file exists.
- [x] 1.7 Check for leftovers. Verify, and record in the PR: `grep -rniE 'lighthouse|mcp|npx |tailwind|fonts\.googleapis|preconnect|\$ARGUMENTS|impeccable init|PRODUCT\.md|amazon|title case|uppercase|virtua|nuqs|AAA' .agents/design/*.md` finds only lines in `project-rules.md` or the README that name a rule or a source. `wc -l .agents/design/*.md` totals about 1,350.

## 2. frontend-implementer role

- [x] 2.1 Add `.agents/roles/frontend-implementer.md` (design D3). Verify, and record in the PR:
  - the frontmatter has the `description` from D3, class `medium`, access `write`, web `true`;
  - the body has the reading order, the "this file wins" line, the pass rule (pass 2 of a change with both layers, or the single pass of a frontend-only change; no pass 1 failing tests) and the additions from D3 (only new role-named tokens; screenshots of each touched page and each changed state);
  - the body has no three single quotes (`grep -c "'''"` prints 0);
  - `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 3. Reviewers and researcher

- [x] 3.1 Update `.agents/roles/reviewer.md`: the frontend Check bullet, `.agents/design/` on the "no unrequested changes" list, and the own-UI case in the triage line ("Mark each finding as one to fix in the PR ..."): design-rule and WCAG 2.2 AA violations in UI the change adds or edits are also marked fix in the PR (design D7). Verify, and record in the PR: ``tr '\n' ' ' < .agents/roles/reviewer.md | tr -s ' ' | grep -oE 'Mark each finding.{0,250}WCAG 2\.2 AA violations? in UI the change adds or edits'`` finds the triage line. The Check bullet moves into the `frontend-reviewer` in 5.3 and 6.2.
- [x] 3.2 Update `.agents/roles/code-gate-reviewer.md`: the project-rules bullet, `.agents/design/` on the "no unrequested changes" list, and the same own-UI case in the triage line (design D7). Verify: `grep -n '.agents/design' .agents/roles/code-gate-reviewer.md` shows both lines, and ``tr '\n' ' ' < .agents/roles/code-gate-reviewer.md | tr -s ' ' | grep -oE 'Mark each finding.{0,250}WCAG 2\.2 AA violations? in UI the change adds or edits'`` finds the triage line.
- [x] 3.3 Update `.agents/roles/researcher.md` with the design-angle rule (design D5, before the post-gate revision). Removed again by 6.1.
- [x] 3.4 After 2.1 and 3.1-3.3, run `scripts/setup-agents`. Verify, and record in the PR: it generates `frontend-implementer` for Claude Code, opencode and Codex without script changes, and `scripts/setup-agents --check` prints `ok`.

## 4. AGENTS.md, constitution, config, .gitignore

- [x] 4.1 Update `AGENTS.md` (design D4, D5, D7, before the post-gate revision; the design-angle text is replaced by 8.1). Verify, and record in the PR, each of the five points with its own check, followed by reading the matched paragraph. The checks run on the file with line wraps removed, so a wrapped line still matches, and use no repetition count above 255 (the macOS `grep` limit):

  ```sh
  flat() { tr '\n' ' ' < AGENTS.md | tr -s ' '; }
  impl() { flat | grep -oE 'Implementation\*\*.*- \*\*Review\*\*'; }   # the Implementation bullet
  review() { flat | grep -oE '\*\*Review\*\*.*Agents. reports'; }       # the Review bullet
  ```

  - the commit-before-delegating line names the `frontend-implementer`: `flat | grep -oE 'Before delegating[^.]*frontend-implementer'`;
  - the design angle: `flat | grep -oi '<term>'` finds each of `design angle`, `frontend/src`, `Clarify`, `small task`, `Explore is run` and `existing screens`, and reading shows they belong to the design-angle text;
  - routing of implementation slices touching `frontend/` to `frontend-implementer`: `impl | grep -o 'frontend-implementer'` finds it; and the "Failing tests" (pass 1) text stays with `implementer`: ``impl | grep -oiE 'failing tests[^.]*`implementer`|`implementer`[^.]*failing tests'`` finds it, and reading shows pass 1 is not given to `frontend-implementer`;
  - the frontend review check: `review | grep -oE '\.agents/design/|screenshots'` finds both terms;
  - `flat | grep -o 'implementer or frontend-implementer report'` finds the phrase.
- [x] 4.2 Edit constitution §1 step 2 with the design-angle sentence (design D5, before the post-gate revision; replaced by 9.1) and step 10 with the own-UI sentence (design D7). Verify: `git diff main -- openspec/constitution.md` touches only steps 2 and 10, one sentence each.
- [x] 4.3 Add the layer-split rule for implementation task groups to `openspec/config.yaml` `rules.tasks` as design D4 says, including the order (in a change with both layers, backend implementation groups come before the frontend groups that call them); the "Section 1 is Failing tests" rule in `rules.tasks` and the "Pass 1 production code" apply guidance are not changed. Verify: `npx -y @fission-ai/openspec@1.15.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule, and `git diff main -- openspec/config.yaml` adds only that one rule.
- [x] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved in `frontend/screenshots/<change-name>/` does not show in `git status`.

## 5. New frontend roles

- [ ] 5.1 Add `.agents/roles/frontend-researcher.md` (design D13, D5). Verify, and record in the PR:
  - the frontmatter has `name: frontend-researcher`, a `description` that says to use it instead of `researcher` for research angles about the UI, class `medium`, access `read`, web `true`;
  - the body says to read `.agents/roles/researcher.md` and follow it, has the "this file wins" line, and has the reading order `README.md`, `project-rules.md`, `checklist.md`, then `accessibility.md` when the change adds or changes interactive elements, forms or dialogs, the rest on demand;
  - it always returns design questions on the screens the change touches: `grep -oE` finds each of `layout`, `states`, `copy`, `a11y`, `tokens`, `components`, and reading shows they belong to that rule;
  - `grep -c "'''"` prints 0.
- [ ] 5.2 Add `.agents/roles/frontend-spec-critic.md` (design D13, D15). Verify, and record in the PR:
  - the frontmatter has `name: frontend-spec-critic`, a `description` that says to use it instead of `spec-critic` for a change touching `frontend/src`, class `medium`, access `read`, web `false`;
  - the body says to read `.agents/roles/spec-critic.md` and follow it and has the "this file wins" line;
  - the design pass: `grep -oE` finds each of `project-rules.md`, `checklist.md`, `accessibility.md`, `UI design`, `states`, `focus`, `i18n`, `tone` and `design questions`, and reading shows the five checks of D13 (missing states, scenarios without focus behaviour, copy not through i18n or not per the tone rules, contradictions with `project-rules.md`, unanswered design questions);
  - `grep -c "'''"` prints 0.
- [ ] 5.3 Add `.agents/roles/frontend-reviewer.md` (design D13, D7). Verify, and record in the PR:
  - the frontmatter has `name: frontend-reviewer`, a `description` that says to use it instead of `reviewer` for any slice touching `frontend/`, class `medium`, access `read`, web `false`;
  - the body says to read `.agents/roles/reviewer.md` and follow it and has the "this file wins" line;
  - the report has a "Logic" and a "Design" section, and the Design section always says what was checked, even with no findings: `grep -nE 'Logic|Design'` and reading;
  - the Design pass: `grep -oE` finds each of `project-rules.md`, `checklist.md`, `360`, `keyboard`, `focus`, `aria-`, `contrast` and `UI design`, and reading shows that the role opens the screenshot PNGs itself;
  - the leftover finding: `grep -n 'zshots.spec.ts'` and `grep -n 'frontend/screenshots/'` each find a line;
  - `grep -c "'''"` prints 0.

## 6. Generic researcher and reviewer

- [ ] 6.1 Remove the design-angle bullet from `.agents/roles/researcher.md` (design D5, D6). Verify: `grep -cE '\.agents/design|design angle|critique-rubric' .agents/roles/researcher.md` prints 0, and `git diff` of the file shows only that bullet removed.
- [ ] 6.2 Remove the frontend Check bullet from `.agents/roles/reviewer.md` (design D7). Verify, and record in the PR: `grep -cE 'zshots|frontend/screenshots|critique-rubric|frontend-implementer' .agents/roles/reviewer.md` prints 0; `.agents/design/` is still on the "no unrequested changes" list (`grep -n '\.agents/design/'` finds that line only); and the triage check of 3.1 still finds the own-UI case.

## 7. code-gate-reviewer

- [ ] 7.1 Strengthen the frontend bullet of `.agents/roles/code-gate-reviewer.md` (design D7). Verify, and record in the PR, on the file with line wraps removed (`tr '\n' ' ' < .agents/roles/code-gate-reviewer.md | tr -s ' '`):
  - for a PR touching `frontend/src` it reads `README.md`, `project-rules.md`, `checklist.md` and `accessibility.md` (for forms and dialogs): `grep -oE` finds each name;
  - it checks against the "UI design" section of `design.md` and the screenshots from the frontend slices: `grep -oE 'UI design|screenshots'` finds both;
  - its report gets a "Design" section that says what was checked: reading;
  - the leftover `zshots.spec.ts` / `frontend/screenshots/` finding, `.agents/design/` on the "no unrequested changes" list and the triage check of 3.2 are still there;
  - `git diff` of the file leaves the frontmatter (class `strong`) unchanged.

## 8. AGENTS.md routing

- [ ] 8.1 Update "Main agent is an orchestrator" in `AGENTS.md` (design D5, D6, D7, D13). Verify, and record in the PR, each point with its own check on the file with line wraps removed (the `flat` helper of 4.1), followed by reading the matched paragraph:
  - the design-angle text and the out-of-scope filing text are gone: `flat | grep -ciE 'design angle|area:frontend|engineer agrees'` prints 0;
  - research: a change touching `frontend/src` gets at least one `frontend-researcher` at Clarify, angles about the UI go to it, and the rest to `researcher`: `flat | grep -o '<term>'` finds each of `frontend-researcher`, `frontend/src` and `Clarify`;
  - the design questions go to the engineer, and the answers into the proposal's Clarifications and the "UI design" section of `design.md`: `flat | grep -o 'UI design'` finds it;
  - spec critique: `frontend-spec-critic` instead of `spec-critic` when the change touches `frontend/src`: ``flat | grep -oE 'Spec critique\*\*.{0,200}frontend-spec-critic'`` finds it;
  - review: slices touching `frontend/` go to `frontend-reviewer`, and the `code-gate-reviewer` runs once per PR: `review | grep -oE 'frontend-reviewer|code-gate-reviewer'` (the `review` helper of 4.1) finds both;
  - the Implementation bullet is unchanged: `git diff` shows no change in it.

## 9. Constitution

- [ ] 9.1 Edit constitution §1 (design D5, D13): in step 2, replace the design-angle sentence with one saying that a change touching `frontend/src` gets at least one `frontend-researcher` at Clarify (AGENTS.md); where the spec critic is named (the Roles paragraph and step 5), the `frontend-spec-critic` runs instead of the `spec-critic` for a change touching `frontend/src`. Verify, and record in the PR: `grep -c 'design angle' openspec/constitution.md` prints 0; `grep -n 'frontend-researcher\|frontend-spec-critic' openspec/constitution.md` shows step 2, the Roles paragraph and step 5; `git diff` of the file touches only those three places, and step 10 is unchanged.

## 10. Spec format rules

- [ ] 10.1 Add the `rules.design` and `rules.specs` rules of design D15 to `openspec/config.yaml`. Verify, and record in the PR: `npx -y @fission-ai/openspec@1.15.0 instructions design --change workflow-frontend-implementer --json` lists the "UI design" rule with all six headings and the ASCII sketch, `npx -y @fission-ai/openspec@1.15.0 instructions specs --change workflow-frontend-implementer --json` lists the design-affecting scenarios rule, and `git diff` of the file adds only these two rules.

## 11. Design sources

- [ ] 11.1 Update `.agents/design/README.md` (design D14): the precedence (design code from #171, not yet written, then `project-rules.md`, then the vendored community sources) and the list of readers (the four frontend roles and the `code-gate-reviewer`). Verify: `grep -nE 'design code.*project-rules\.md.*sources'` finds the precedence line; `grep -oE 'frontend-(implementer|researcher|spec-critic|reviewer)|code-gate-reviewer'` finds all five names; `grep -c 'design-angle'` prints 0.
- [ ] 11.2 Remove the out-of-scope filing line from `project-rules.md` C13 (design D2, D6). Verify: `grep -cE 'area:frontend|engineer agrees' .agents/design/project-rules.md` prints 0, and `grep -n 'UI the change adds or edits'` still finds the own-UI rule.
- [ ] 11.3 In `checklist.md` and `critique-rubric.md`, name the `frontend-reviewer` and the `frontend-researcher` where they name the reviewer and the design-angle researcher (design D14). Verify: `grep -niE 'design-angle|the reviewer' .agents/design/checklist.md .agents/design/critique-rubric.md` finds nothing, and `grep -n 'frontend-reviewer'` finds a line in both files.

## 12. Generate the roles

- [ ] 12.1 After 5.1-5.3, 6.1, 6.2 and 7.1, run `scripts/setup-agents`. Verify, and record in the PR: it generates `frontend-researcher`, `frontend-spec-critic` and `frontend-reviewer` for Claude Code, opencode and Codex without script changes; `scripts/setup-agents --check` prints `ok`; `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 13. Proof

- [ ] 13.1 Dry run in Claude Code with all new roles, a full run, after 12.1 and a restart (design D12):
  - the `frontend-researcher` (and the generic `researcher` only if a non-UI angle exists) returns design questions for #165;
  - the main agent asks them before any spec or code;
  - a throwaway branch is made off this one with `git switch -c <throwaway name, for example dry-run-frontend-implementer> workflow-frontend-implementer` (not `scripts/flow/change-start`, which branches from `origin/main` and would take the real #165 branch name); on it a scratch change is created with `npx -y @fission-ai/openspec@1.15.0 new change ui-required-fields`, never by hand;
  - the `spec-writer` writes a minimal proposal (Clarifications with the answers), `design.md` with the "UI design" section, one spec delta and a minimal `tasks.md`;
  - the `frontend-spec-critic` attacks it;
  - the `frontend-implementer` builds the slice and lists its screenshot paths in `frontend/screenshots/ui-required-fields/`: each page the slice touches and each state it changes (for example the error shown after an empty submit, or a dialog open), at desktop and 360 px;
  - the `frontend-reviewer` reviews it, with a Logic and a Design section; design-rule and WCAG 2.2 AA violations in UI the slice adds or edits are fixed on the throwaway branch.

  Verify (design D12): the proof is screenshots only. The engineer attaches screenshots to PR #169 by hand, showing how the new flow works (for example the design questions asked before any spec or code, and the frontend-spec-critic's, the frontend-implementer's and the frontend-reviewer's work) and what result it gives (the UI). They are also the proof for 5.1-5.3, 6.1 and 6.2. No throwaway-branch SHA, no pushed branch and no text logs are required. The throwaway branch is not pushed and is deleted right after the run, with the scratch change and the code; no #165 code or spec survives. opencode and Codex (including whether they can read screenshot images) are not run, and routing in a change with both layers is not exercised (#165 is frontend-only); the PR states both are unverified.

## Workflow follow-up

- Code review gate: the PR holds the proofs (the recorded checks of tasks 1-12 and the engineer's dry-run screenshots of 13.1); the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution (§1 Roles paragraph and steps 2, 5 and 10).
