# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate; the dry run's proof (13.1) is screenshots that the engineer attaches to the PR by hand. All tasks start after the spec gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0` (as `scripts/flow/change-start`).

Tasks 1-4 were done before the post-gate revision (proposal Clarifications, G-1 to G-11, and G-12 to G-19 after its critique) and stay ticked; their recorded checks are not rewritten. Groups 5-12 rework part of them (G-20 is the engineer's decision after a further critique):

- 5.4 reworks the screenshot trigger of the `frontend-implementer` role written by 2.1 and adds the final screenshot set (G-16, G-17);
- 6.1 removes the design-angle bullet of 3.3 and changes the `researcher.md` description (G-15);
- 6.2 moves the frontend bullet of 3.1 into the `frontend-reviewer` (5.3), and 6.3 rewords the triage line of 3.1 to the generic design-rule wording (G-20);
- 7.1 reworks the project-rules bullet of 3.2, and 7.2 rewords the triage line of 3.2 to the generic design-rule wording (G-20);
- 8.1 replaces the design-angle text and the out-of-scope filing text of 4.1, and changes the review-bullet sentence that 4.1's `review | grep` check matched;
- 9.1 replaces the step 2 sentence of 4.2 and edits the Roles paragraph and step 5, and 9.2 rewords the step 10 sentence of 4.2 (G-20), so 4.2's recorded check ("only steps 2 and 10", against `main`) no longer holds after it;
- 10.1 adds two rules to `openspec/config.yaml`, so 4.3's recorded check ("adds only that one rule", against `main`) no longer holds after it;
- 11.1 reworks the README of 1.3; 11.2 removes the out-of-scope filing line of 1.2 and rewords its C13 triage sentence (G-20), and 11.4 changes C0 of 1.2;
- 11.3 reworks the readers named in `checklist.md` (1.4) and `critique-rubric.md` (1.5);
- 12.1 replaces the generation of 3.4;
- 14.1 adds one line to `implementer.md`, so 2.1's and 12.1's recorded check (`git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty) no longer holds for `implementer.md` after it (G-22), and 14.2 reworks the "For the engineer" text of the `frontend-researcher` written by 5.1 (G-23); 14.3 replaces the generation of 12.1.
- 15.1 reworks the reading order and the report of the `frontend-researcher` written by 5.1 and changed by 14.2 (G-24), so 14.2's check (`git diff d4190f8 -- .agents/roles/frontend-researcher.md` changes only that bullet) no longer holds for that file after it; 15.2 replaces the generation of 14.3.

The dry run, formerly 5.1, is now 13.1.

Every `git diff` check in groups 5-12 uses base `3355250`, the last commit before these groups (the commits after it and before group 5 touch only `openspec/changes/`), and names the file. Each file so checked is changed by one group only. `main` is not the base, because tasks 1-4 already changed most of these files; it stays the base only for `scripts/setup-agents`, which this change never edits (and for `.agents/roles/implementer.md` until 14.1, which adds one line, checked against `d4190f8`).

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

- [x] 4.1 Update `AGENTS.md` (design D4, D5, D6, D7, before the post-gate revision; the design-angle text and the out-of-scope filing text are replaced by 8.1). Verify, and record in the PR, each of the six points with its own check, followed by reading the matched paragraph. The checks run on the file with line wraps removed, so a wrapped line still matches, and use no repetition count above 255 (the macOS `grep` limit):

  ```sh
  flat() { tr '\n' ' ' < AGENTS.md | tr -s ' '; }
  impl() { flat | grep -oE 'Implementation\*\*.*- \*\*Review\*\*'; }   # the Implementation bullet
  review() { flat | grep -oE '\*\*Review\*\*.*Agents. reports'; }       # the Review bullet
  ```

  - the commit-before-delegating line names the `frontend-implementer`: `flat | grep -oE 'Before delegating[^.]*frontend-implementer'`;
  - the design angle: `flat | grep -oi '<term>'` finds each of `design angle`, `frontend/src`, `Clarify`, `small task`, `Explore is run` and `existing screens`, and reading shows they belong to the design-angle text;
  - design and UI problems in existing UI outside the change filed only after the engineer agrees, other findings by step 10: `flat | grep -o '<term>'` finds each of `area:frontend`, `engineer agrees` and `step 10` (superseded by G-3 after the spec gate: 8.1 removes this text, and its check there expects none of these terms);
  - routing of implementation slices touching `frontend/` to `frontend-implementer`: `impl | grep -o 'frontend-implementer'` finds it; and the "Failing tests" (pass 1) text stays with `implementer`: ``impl | grep -oiE 'failing tests[^.]*`implementer`|`implementer`[^.]*failing tests'`` finds it, and reading shows pass 1 is not given to `frontend-implementer`;
  - the frontend review check: `review | grep -oE '\.agents/design/|screenshots'` finds both terms;
  - `flat | grep -o 'implementer or frontend-implementer report'` finds the phrase.
- [x] 4.2 Edit constitution §1 step 2 with the design-angle sentence (design D5, before the post-gate revision; replaced by 9.1) and step 10 with the own-UI sentence (design D7). Verify: `git diff main -- openspec/constitution.md` touches only steps 2 and 10, one sentence each.
- [x] 4.3 Add the layer-split rule for implementation task groups to `openspec/config.yaml` `rules.tasks` as design D4 says, including the order (in a change with both layers, backend implementation groups come before the frontend groups that call them); the "Section 1 is Failing tests" rule in `rules.tasks` and the "Pass 1 production code" apply guidance are not changed. Verify: `npx -y @fission-ai/openspec@1.15.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule, and `git diff main -- openspec/config.yaml` adds only that one rule.
- [x] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved in `frontend/screenshots/<change-name>/` does not show in `git status`.

## 5. Frontend roles

- [x] 5.1 Add `.agents/roles/frontend-researcher.md` (design D13, D5). Verify, and record in the PR:
  - the frontmatter has `name: frontend-researcher`, a `description` that says to use it instead of `researcher` for research angles about the UI, class `medium`, access `read`, web `true`;
  - the body says to read `.agents/roles/researcher.md` and follow it, and has the "this file wins" line;
  - the reading order starts from the README: ``tr '\n' ' ' < .agents/roles/frontend-researcher.md | tr -s ' ' | grep -oE 'README\.md.{0,120}project-rules\.md.{0,120}checklist\.md'`` finds it, and reading shows `accessibility.md` when the change adds or changes interactive elements, forms or dialogs, and the rest on demand;
  - it always returns design questions on the screens the change touches: `grep -oE` finds each of `layout`, `states`, `copy`, `a11y`, `tokens`, `components`, and reading shows they belong to that rule;
  - `grep -c "'''"` prints 0.
- [x] 5.2 Add `.agents/roles/frontend-spec-critic.md` (design D13, D15). Verify, and record in the PR:
  - the frontmatter has `name: frontend-spec-critic`, a `description` that says to use it instead of `spec-critic` for a change touching `frontend/src`, class `medium`, access `read`, web `false`;
  - the body says to read `.agents/roles/spec-critic.md` and follow it and has the "this file wins" line;
  - the reading order starts from the README: ``tr '\n' ' ' < .agents/roles/frontend-spec-critic.md | tr -s ' ' | grep -oE 'README\.md.{0,120}project-rules\.md.{0,120}checklist\.md'`` finds it, and reading shows `accessibility.md` and the rest on demand as D13 says;
  - the design pass: `grep -oE` finds each of `UI design`, `states`, `focus`, `i18n`, `tone` and `design questions`, and reading shows the five checks of D13 (missing states, scenarios without focus behaviour, copy not through i18n or not per the tone rules, contradictions with `project-rules.md`, unanswered design questions) and the `rules.design` format, including the "UI design: none, <reason>" line;
  - unanswered design questions (G-18): reading shows they are reported as questions for the engineer, which the main agent asks, with the answers recorded by the spec-writer in the fix round;
  - one report (G-19): reading shows the report keeps the full `spec-critic` checks, also for a change with both layers, and the "Design" section sits outside the base 25-line limit with its own cap: `grep -n '15 lines'` finds that line;
  - `grep -c "'''"` prints 0.
- [x] 5.3 Add `.agents/roles/frontend-reviewer.md` (design D13, D7). Verify, and record in the PR:
  - the frontmatter has `name: frontend-reviewer`, a `description` that says to use it instead of `reviewer` for any slice touching `frontend/`, class `medium`, access `read`, web `false`;
  - the body says to read `.agents/roles/reviewer.md` and follow it and has the "this file wins" line;
  - the reading order starts from the README: ``tr '\n' ' ' < .agents/roles/frontend-reviewer.md | tr -s ' ' | grep -oE 'README\.md.{0,120}project-rules\.md.{0,120}checklist\.md'`` finds it;
  - the report has a "Logic" and a "Design" section, and the Design section always says what was checked, even with no findings: `grep -nE 'Logic|Design'` and reading;
  - the Design section's own cap (G-14): `grep -n '15 lines'` finds it, and reading shows it does not count toward the 25-line limit of `reviewer.md`;
  - a frontend slice with no screenshots (G-17): `grep -n 'no screenshots: '` finds the line that the Design section then says "no screenshots: <reason>";
  - the Design pass: `grep -oE` finds each of `checklist.md`, `360`, `keyboard`, `focus`, `aria-`, `contrast` and `UI design`, and reading shows that the role opens the screenshot PNGs itself;
  - the leftover finding: `grep -n 'zshots.spec.ts'` and `grep -n 'frontend/screenshots/'` each find a line;
  - `grep -c "'''"` prints 0.
- [x] 5.4 Rework `.agents/roles/frontend-implementer.md` (design D3, D8; reworks 2.1, G-16, G-17, F7): the screenshot trigger becomes "when the slice changes rendered UI: any change to `frontend/src/**/*.tsx` except `*.test.tsx`, `frontend/src/**/*.css`, or `frontend/src/i18n/*.json`" (so CSS modules such as `frontend/src/ui/*.module.css` count), and a bullet adds the final screenshot set: before the code gate, when the main agent asks, take a final set of every screen and state listed in the change's "UI design" section, at desktop and 360 px, into `frontend/screenshots/<change-name>/` by the same steps, and list the paths. Verify, and record in the PR:
  - the old trigger is gone: `grep -c 'frontend/src/pages/' .agents/roles/frontend-implementer.md` prints 0;
  - the new trigger: ``tr '\n' ' ' < .agents/roles/frontend-implementer.md | tr -s ' ' | grep -oE 'frontend/src/\*\*/\*\.tsx|\*\.test\.tsx|frontend/src/\*\*/\*\.css|frontend/src/i18n/\*\.json' | sort -u`` prints all four, and reading shows `*.test.tsx` is the exception;
  - the final set: `grep -oE 'UI design|code gate'` finds both, and reading shows the set covers every screen and state of the "UI design" section at both widths and lists the paths;
  - `git diff 3355250 -- .agents/roles/frontend-implementer.md` changes only the trigger sentence and adds the final-set bullet; the frontmatter is unchanged;
  - `grep -c "'''"` prints 0.

## 6. Generic researcher and reviewer

- [x] 6.1 Remove the design-angle bullet from `.agents/roles/researcher.md` and change its description to: for all investigation, except UI angles, which go to `frontend-researcher` (design D5, D6; G-15). Verify, and record in the PR:
  - the design bullet is gone: `grep -cE '\.agents/design|design angle|critique-rubric' .agents/roles/researcher.md` prints 0;
  - the description: `grep -nE '^description:.*except UI angles.*frontend-researcher' .agents/roles/researcher.md` finds it;
  - `git diff 3355250 -- .agents/roles/researcher.md` shows only that bullet removed and the description line changed.
- [x] 6.2 Remove the frontend Check bullet from `.agents/roles/reviewer.md` (design D7). Verify, and record in the PR: `grep -cE 'zshots|frontend/screenshots|critique-rubric|frontend-implementer' .agents/roles/reviewer.md` prints 0; `.agents/design/` is still on the "no unrequested changes" list (`grep -n '\.agents/design/'` finds that line only); the triage check of 3.1 still finds the own-UI case; the diff check of the file is in 6.3.
- [x] 6.3 Reword the triage line of `.agents/roles/reviewer.md` ("Mark each finding as one to fix in the PR ...") so the own-UI case speaks of design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change adds or edits, instead of "a design-rule ... violation" (design D7; G-20; reworks 3.1). The `frontend-reviewer` inherits it. Verify, and record in the PR:
  - ``tr '\n' ' ' < .agents/roles/reviewer.md | tr -s ' ' | grep -o 'design rules in `.agents/design/`, in the precedence order of its README'`` finds it;
  - the triage check of 3.1 still finds the own-UI case;
  - `grep -c 'Mark each finding' .agents/roles/frontend-reviewer.md` prints 0 (no triage line of its own);
  - `git diff 3355250 -- .agents/roles/reviewer.md` shows only the frontend Check bullet removed (6.2) and the triage line reworded.

## 7. code-gate-reviewer

- [x] 7.1 Strengthen the frontend bullet of `.agents/roles/code-gate-reviewer.md` (design D7, D8; reworks 3.2). Verify, and record in the PR, on the file with line wraps removed (`tr '\n' ' ' < .agents/roles/code-gate-reviewer.md | tr -s ' '`):
  - for a PR touching `frontend/src` it reads `README.md` first, then `project-rules.md`, `checklist.md`, and `accessibility.md` for forms and dialogs: `grep -oE 'README\.md.{0,120}project-rules\.md.{0,120}checklist\.md'` finds the order, and `grep -o 'accessibility.md'` finds it;
  - it checks against the "UI design" section of `design.md` and the final screenshot set (G-16): `grep -oE 'UI design|final screenshot set'` finds both, and reading shows the main agent passes the paths in `frontend/screenshots/<change-name>/`;
  - its report gets a "Design" section that says what was checked: reading;
  - with no final set (for example "UI design: none"), the Design section says "no screenshots: <reason>" (F8): `grep -o 'no screenshots: '` finds it, and reading shows that case;
  - the leftover `zshots.spec.ts` / `frontend/screenshots/` finding, `.agents/design/` on the "no unrequested changes" list and the triage check of 3.2 are still there;
  - `git diff 3355250 -- .agents/roles/code-gate-reviewer.md` leaves the frontmatter (class `strong`) and the other Check bullets unchanged, and changes the triage line only as 7.2 says.
- [x] 7.2 Reword the triage line of `.agents/roles/code-gate-reviewer.md` as 6.3 does for `reviewer.md`: the own-UI case speaks of design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change adds or edits (design D7; G-20; reworks 3.2). Verify, and record in the PR: ``tr '\n' ' ' < .agents/roles/code-gate-reviewer.md | tr -s ' ' | grep -o 'design rules in `.agents/design/`, in the precedence order of its README'`` finds it, and the triage check of 3.2 still finds the own-UI case.
- [x] 7.3 Record that 22f1a98 applied G-21: the `code-gate-reviewer`'s Design section sits outside its 25-line limit with its own cap of 15 lines (design D7), and the opening paragraph of `.agents/design/project-rules.md` says it wins over the vendored sources only and points to C0 for the full order (design D2, D14). Verify: `grep -n '15 lines' .agents/roles/code-gate-reviewer.md` finds the cap; `grep -n 'C0' .agents/design/project-rules.md | head -2` shows the opening-paragraph pointer and the C0 line.

## 8. AGENTS.md and CONTRIBUTING.md routing

- [x] 8.1 Update "Main agent is an orchestrator" in `AGENTS.md` (design D5, D6, D7, D8, D13; reworks 4.1). Verify, and record in the PR, each point with its own check on the file with line wraps removed (the `flat`, `impl` and `review` helpers of 4.1, plus `spec() { flat | grep -oE '\*\*Spec critique\*\*.*- \*\*Implementation'; }`), followed by reading the matched paragraph:
  - the design-angle text and the out-of-scope filing text are gone: `flat | grep -ciE 'design angle|area:frontend|engineer agrees'` prints 0;
  - research: a change touching `frontend/src` gets at least one `frontend-researcher` at Clarify, angles about the UI go to it, and the rest to `researcher`: `flat | grep -o '<term>'` finds each of `frontend-researcher`, `frontend/src` and `Clarify`;
  - the design questions go to the engineer, and the answers into the proposal's Clarifications and the "UI design" section of `design.md`: `flat | grep -o 'UI design'` finds it;
  - spec critique: `frontend-spec-critic` instead of `spec-critic` when the change touches `frontend/src`, and its unanswered design questions go to the engineer through the main agent, with the answers recorded by the spec-writer in the fix round (G-18): `spec | grep -oE 'frontend-spec-critic|design question|spec-writer|fix round'` finds all four;
  - review: slices touching `frontend/` go to `frontend-reviewer` after every implementation slice (pass 2 of a change with both layers, or the single pass of a frontend-only change), and the `code-gate-reviewer` runs once per PR: `review | grep -oE 'frontend-reviewer|code-gate-reviewer|single pass of a frontend-only change'` finds all three; the sentence matched by 4.1's `review | grep -oE '\.agents/design/|screenshots'` check becomes this routing;
  - the final screenshot set (G-16): for a PR touching `frontend/src`, before the code-gate review the main agent has the `frontend-implementer` take the final set and passes its paths to the `code-gate-reviewer`: `review | grep -o 'final screenshot set'` finds it;
  - the Implementation bullet is unchanged: ``diff <(git show 3355250:AGENTS.md | tr '\n' ' ' | tr -s ' ' | grep -oE 'Implementation\*\*.*- \*\*Review\*\*') <(impl)`` prints nothing.
- [x] 8.2 Update `CONTRIBUTING.md:52`, the "Propose + spec critique" row (design D13; G-15): the agent runs one critic, the `frontend-spec-critic` when the change touches `frontend/src`, otherwise the `spec-critic`. Verify, and record in the PR: `grep -n 'frontend-spec-critic' CONTRIBUTING.md` finds that row, which also names `spec-critic` and `frontend/src`; `git diff 3355250 -- CONTRIBUTING.md` changes only that line.

## 9. Constitution

- [x] 9.1 Edit constitution §1 (design D5, D13; reworks 4.2): in step 2, replace the design-angle sentence with one saying that a change touching `frontend/src` gets at least one `frontend-researcher` at Clarify (AGENTS.md); where the spec critic is named (the Roles paragraph and step 5), the `frontend-spec-critic` runs instead of the `spec-critic` for a change touching `frontend/src`. Verify, and record in the PR: `grep -c 'design angle' openspec/constitution.md` prints 0; `grep -n 'frontend-researcher\|frontend-spec-critic' openspec/constitution.md` shows step 2, the Roles paragraph and step 5; the diff check of the file is in 9.2.
- [x] 9.2 In constitution §1 step 10, reword the own-UI sentence so it speaks of design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change adds or edits (its markup, CSS, copy or behaviour), instead of naming only `.agents/design/project-rules.md` (design D7; G-20; reworks 4.2). Verify, and record in the PR: ``tr '\n' ' ' < openspec/constitution.md | tr -s ' ' | grep -o 'design rules in `.agents/design/`, in the precedence order of its README'`` finds it; `grep -c 'project-rules' openspec/constitution.md` prints 0; `git diff 3355250 -- openspec/constitution.md` touches only step 2, the Roles paragraph and step 5 (9.1) and this sentence, and the other sentences of step 10 are unchanged.

## 10. Spec format rules

- [x] 10.1 Add the `rules.design` and `rules.specs` rules of design D15 to `openspec/config.yaml`. Verify, and record in the PR: `npx -y @fission-ai/openspec@1.15.0 instructions design --change workflow-frontend-implementer --json` lists the "UI design" rule with all six headings, the ASCII sketch and the one-line "UI design: none, <reason>" option for a change with no rendered UI change (G-18); `npx -y @fission-ai/openspec@1.15.0 instructions specs --change workflow-frontend-implementer --json` lists the design-affecting scenarios rule; and `git diff 3355250 -- openspec/config.yaml` adds only these two rules.

## 11. Design sources

- [x] 11.1 Update `.agents/design/README.md` (design D14; reworks 1.3): the precedence (the change's spec, then design code from #171, not yet written, then `project-rules.md`, then the vendored community sources; G-12) and the list of readers (the four frontend roles and the `code-gate-reviewer`). Verify: `grep -nE 'spec.*design code.*project-rules\.md.*sources'` finds the precedence line; `grep -oE 'frontend-(implementer|researcher|spec-critic|reviewer)|code-gate-reviewer'` finds all five names; `grep -c 'design-angle'` prints 0; the reading order still starts with `README.md`.
- [x] 11.2 In `project-rules.md` C13, remove the out-of-scope filing line, and reword the triage sentence so it speaks of design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations (design D2, D6; G-20). Verify: `grep -cE 'area:frontend|engineer agrees' .agents/design/project-rules.md` prints 0; `grep -n 'UI the change adds or edits'` still finds the own-UI rule; ``tr '\n' ' ' < .agents/design/project-rules.md | tr -s ' ' | grep -o 'design rules in `.agents/design/`, in the precedence order of its README'`` finds it.
- [x] 11.3 Name the readers in `checklist.md` and `critique-rubric.md` (design D14; reworks 1.4 and 1.5): `checklist.md:5-6` names all its readers, the `frontend-implementer`, the `frontend-reviewer`, the `frontend-spec-critic` and the `code-gate-reviewer`; `critique-rubric.md` names the `frontend-reviewer` and the `frontend-researcher` where it names the reviewer and the design-angle researcher. Verify: `grep -niE 'design-angle|the reviewer' .agents/design/checklist.md .agents/design/critique-rubric.md` finds nothing; `grep -oE 'frontend-implementer|frontend-reviewer|frontend-spec-critic|code-gate-reviewer' .agents/design/checklist.md | sort -u` prints all four names; `grep -oE 'frontend-reviewer|frontend-researcher' .agents/design/critique-rubric.md | sort -u` prints both.
- [x] 11.4 Change `project-rules.md` C0 to the precedence of design D14 (the change's spec, then design code from #171, then `project-rules.md`, then the vendored sources), or to a pointer to the README, which states it (design D2; G-12). Verify: `grep -n 'C0' .agents/design/project-rules.md` shows the line, and either `grep -nE 'C0.*spec.*design code.*sources'` or `grep -nE 'C0.*README'` finds it.

## 12. Generate the roles

- [x] 12.1 After 5.1-5.4, 6.1-6.3, 7.1 and 7.2, run `scripts/setup-agents` (replaces 3.4). Verify, and record in the PR: it generates `frontend-researcher`, `frontend-spec-critic` and `frontend-reviewer` for Claude Code, opencode and Codex without script changes, and regenerates `frontend-implementer`; `scripts/setup-agents --check` prints `ok`; `git diff main -- .agents/roles/implementer.md scripts/setup-agents` is empty.

## 13. Proof

- [x] 13.1 Dry run in Claude Code with all new roles, a full run, after 12.1 and a restart (design D12):
  - the `frontend-researcher` (and the generic `researcher` only if a non-UI angle exists) returns design questions for #165;
  - the main agent asks them before any spec or code;
  - a throwaway branch is made off this one with `git switch -c <throwaway name, for example dry-run-frontend-implementer> workflow-frontend-implementer` (not `scripts/flow/change-start`, which branches from `origin/main` and would take the real #165 branch name); on it a scratch change is created with `npx -y @fission-ai/openspec@1.15.0 new change ui-required-fields`, never by hand;
  - the `spec-writer` writes a minimal proposal (Clarifications with the answers), `design.md` with the "UI design" section, one spec delta and a minimal `tasks.md`;
  - the `frontend-spec-critic` attacks it;
  - the `frontend-implementer` builds the slice and lists its screenshot paths in `frontend/screenshots/ui-required-fields/`: each page the slice touches and each state it changes (for example the error shown after an empty submit, or a dialog open), at desktop and 360 px;
  - the `frontend-reviewer` reviews it, with a Logic and a Design section; design-rule and WCAG 2.2 AA violations in UI the slice adds or edits are fixed on the throwaway branch;
  - the `frontend-implementer` takes the final screenshot set of every screen and state in the scratch change's "UI design" section into `frontend/screenshots/ui-required-fields/`, and the main agent passes those paths to the `code-gate-reviewer`, which reviews the throwaway slice (its diff against `workflow-frontend-implementer`) with its "Design" section (G-13, G-16).

  Verify (design D12): the proof is screenshots only. The engineer attaches screenshots to PR #169 by hand, showing how the new flow works (for example the design questions asked before any spec or code, and the frontend-spec-critic's, the frontend-implementer's, the frontend-reviewer's and the code-gate-reviewer's work) and what result it gives (the UI). They are also the proof for 5.1-5.4, 6.1-6.3, 7.1 and 7.2, next to their text checks. No throwaway-branch SHA, no pushed branch and no text logs are required. The throwaway branch is not pushed and is deleted right after the run, with the scratch change and the code; no #165 code or spec survives. opencode and Codex (including whether they can read screenshot images) are not run, and routing in a change with both layers is not exercised (#165 is frontend-only); the PR states both are unverified.

## 14. After the dry run

Checks in this group use base `d4190f8` (the last commit before it) for the role files, and name the file.

- [x] 14.1 Add one line to each of `.agents/roles/implementer.md`, `.agents/roles/frontend-implementer.md` and `.agents/roles/spec-writer.md`, the only roles that commit (`grep -l 'git commit' .agents/roles/*` lists no other; design D3; G-22): commit messages are a single line with no `Co-Authored-By` or other trailer; this overrides any attribution reminder from the tool. Verify, and record in the PR: ``for f in implementer frontend-implementer spec-writer; do tr '\n' ' ' < .agents/roles/$f.md | tr -s ' ' | grep -o 'no `Co-Authored-By` or other trailer'; done`` finds it three times; `grep -o 'overrides any attribution reminder' .agents/roles/{implementer,frontend-implementer,spec-writer}.md` finds it in each; `git diff d4190f8 -- .agents/roles/implementer.md .agents/roles/frontend-implementer.md .agents/roles/spec-writer.md` adds only these lines, and the frontmatter of each is unchanged.
- [x] 14.2 In `.agents/roles/frontend-researcher.md`, split the "For the engineer" part of the report into "Design questions" (only layout, states, copy, a11y, tokens and components on the touched screens) and "Other open questions" (behaviour, scope, process), keeping the existing list of design topics for the first (G-23; reworks 5.1). Verify, and record in the PR: `grep -oE 'Design questions|Other open questions' .agents/roles/frontend-researcher.md` finds both; reading shows that behaviour, scope and process go under "Other open questions"; `git diff d4190f8 -- .agents/roles/frontend-researcher.md` changes only this bullet, and the frontmatter is unchanged.
- [x] 14.3 After 14.1 and 14.2, run `scripts/setup-agents` (as 12.1). Verify, and record in the PR: `scripts/setup-agents --check` prints `ok`; `git diff d4190f8 -- scripts/setup-agents` is empty; the generated `implementer`, `frontend-implementer`, `spec-writer` and `frontend-researcher` for Claude Code carry the new text (`grep -l 'overrides any attribution reminder' .claude/agents/*` lists the three, and `grep -l 'Other open questions' .claude/agents/*` lists the researcher); restart the session afterwards.

## 15. Researcher grounded in the design sources

Checks in this group use base `0fd266d` (the last commit before it) for the role file, and name the file.

- [x] 15.1 In `.agents/roles/frontend-researcher.md` (reworks 5.1 and 14.2; design D5; G-24): (a) add required reading by the topic the change touches: layout, spacing and tokens, `refactoring-ui.md` and `refactoring-ui-patterns.md`; accessibility, `accessibility.md`, `accessibility-patterns.md` and `accessibility-wcag22.md`; copy, `frontend-design-copy.md` (all in `.agents/design/`); (b) every design recommendation cites its source (file and section or item), "matches current code" alone is not enough; (c) a recommendation that departs from a source, or sources that disagree, is stated explicitly in the question; (d) points the sources fully decide are given as "decided by sources: X (source)" for the engineer to confirm, not as open questions. Verify, and record in the PR: ``tr '\n' ' ' < .agents/roles/frontend-researcher.md | tr -s ' ' | grep -oE 'refactoring-ui-patterns\.md|accessibility-wcag22\.md|frontend-design-copy\.md|decided by sources'`` finds all four; reading shows (b) and (c); each file named exists in `.agents/design/`; `git diff 0fd266d -- .agents/roles/frontend-researcher.md` changes only the reading step and the report bullet, and the frontmatter is unchanged; no other role file or `.agents/design/` file changes.
- [x] 15.2 After 15.1, run `scripts/setup-agents` (as 14.3). Verify, and record in the PR: `scripts/setup-agents --check` prints `ok`; `git diff 0fd266d -- scripts/setup-agents` is empty; `grep -l 'decided by sources' .claude/agents/*` lists the researcher; restart the session afterwards.
- [x] 15.3 Reflow `.agents/roles/frontend-researcher.md` lines about 28-32 to the file's wrapping: the G-24 sentences of 15.1 were inserted mid-line. No wording change. Then run `scripts/setup-agents`. Verify, and record in the PR: `scripts/setup-agents --check` prints `ok`; the text of the file is unchanged apart from line breaks (compare with `tr '\n' ' ' < .agents/roles/frontend-researcher.md | tr -s ' '` before and after); no other role file changes.

## 16. After the second dry run

Checks in this group use base `ecfaa9e` (the last commit before it) for the role and design files, and name the file.

- [ ] 16.1 Add one step to `.agents/roles/spec-writer.md`, before it reports (design D4; G-25): read the written delta files back and report the requirement and scenario IDs from that read (for example `grep` of the `### Requirement:` and `#### Scenario:` headers), not from memory; if something in the brief is missing from the files, fix it before reporting. Verify, and record in the PR: ``tr '\n' ' ' < .agents/roles/spec-writer.md | tr -s ' ' | grep -oE 'read the written delta files back|not from memory'`` finds both; `git diff ecfaa9e -- .agents/roles/spec-writer.md` adds only this step, and the frontmatter is unchanged.
- [ ] 16.2 Add the design-file upkeep rule (design D1, D13; G-26): (a) in `.agents/design/README.md`, a line saying a change which alters a behaviour stated in a `.agents/design/` file updates that file in the same PR; (b) in `.agents/roles/frontend-spec-critic.md`, a check that a spec delta changing behaviour stated in `.agents/design/` without a task updating the file is a finding. `accessibility.md` and `project-rules.md` C12 are not edited. Verify, and record in the PR: ``tr '\n' ' ' < .agents/design/README.md | tr -s ' ' | grep -o 'same PR'`` finds the rule; ``tr '\n' ' ' < .agents/roles/frontend-spec-critic.md | tr -s ' ' | grep -o 'without a task updating'`` finds the check; `git diff ecfaa9e --stat` for these two files shows only added lines, and the frontmatter of the role is unchanged; `git diff ecfaa9e -- .agents/design/accessibility.md .agents/design/project-rules.md` is empty.
- [ ] 16.3 Add one line each to `.agents/roles/implementer.md` and `.agents/roles/frontend-implementer.md` (design D3; G-27): when part of a task turns out unnecessary, reword the task line to say so with the reason ("not needed: <reason>") before ticking it, and say so in the report; never tick work that was not done as if it were. Verify, and record in the PR: ``for f in implementer frontend-implementer; do tr '\n' ' ' < .agents/roles/$f.md | tr -s ' ' | grep -o 'not needed: <reason>'; done`` finds it twice; `git diff ecfaa9e -- .agents/roles/implementer.md .agents/roles/frontend-implementer.md` adds only these lines, and the frontmatter of each is unchanged.
- [ ] 16.4 After 16.1-16.3, run `scripts/setup-agents` (as 15.2). Verify, and record in the PR: `scripts/setup-agents --check` prints `ok`; `git diff ecfaa9e -- scripts/setup-agents` is empty; the generated Claude Code agents carry the new text (`grep -l 'read the written delta files back' .claude/agents/*` lists the `spec-writer`, `grep -l 'without a task updating' .claude/agents/*` lists the `frontend-spec-critic`, and `grep -l 'not needed: <reason>' .claude/agents/*` lists the `implementer` and `frontend-implementer`); restart the session afterwards.

## Workflow follow-up

- Code review gate: the PR holds the proofs (the recorded checks of tasks 1-12 and 14 and the engineer's dry-run screenshots of 13.1); the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution (§1 Roles paragraph and steps 2, 5 and 10).
