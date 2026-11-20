# Tasks

This is a tooling change with `skip_specs: true`. The spec gate covers the proposal and the design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate; the dry run's proof (5.1) is screenshots that the engineer attaches to the PR by hand. All tasks start after the spec gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0` (as `scripts/flow/change-start`).

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

- [x] 3.1 Update `.agents/roles/reviewer.md`: the frontend Check bullet, `.agents/design/` on the "no unrequested changes" list, and the own-UI case in the triage line ("Mark each finding as one to fix in the PR ..."): design-rule and WCAG 2.2 AA violations in UI the change adds or edits are also marked fix in the PR (design D7). Verify, and record in the PR: ``tr '\n' ' ' < .agents/roles/reviewer.md | tr -s ' ' | grep -oE 'Mark each finding.{0,250}WCAG 2\.2 AA violations? in UI the change adds or edits'`` finds the triage line. The Check bullet is proven by the dry-run screenshots of 5.1.
- [x] 3.2 Update `.agents/roles/code-gate-reviewer.md`: the project-rules bullet, `.agents/design/` on the "no unrequested changes" list, and the same own-UI case in the triage line (design D7). Verify: `grep -n '.agents/design' .agents/roles/code-gate-reviewer.md` shows both lines, and ``tr '\n' ' ' < .agents/roles/code-gate-reviewer.md | tr -s ' ' | grep -oE 'Mark each finding.{0,250}WCAG 2\.2 AA violations? in UI the change adds or edits'`` finds the triage line.
- [x] 3.3 Update `.agents/roles/researcher.md` with the design-angle rule (design D5). Proven by the dry-run screenshots of 5.1.
- [x] 3.4 After 2.1 and 3.1-3.3, run `scripts/setup-agents`. Verify, and record in the PR: it generates `frontend-implementer` for Claude Code, opencode and Codex without script changes, and `scripts/setup-agents --check` prints `ok`.

## 4. AGENTS.md, constitution, config, .gitignore

- [ ] 4.1 Update `AGENTS.md` (design D4, D5, D6, D7). Verify, and record in the PR, each of the six points with its own check, followed by reading the matched paragraph. The checks run on the file with line wraps removed, so a wrapped line still matches, and use no repetition count above 255 (the macOS `grep` limit):

  ```sh
  flat() { tr '\n' ' ' < AGENTS.md | tr -s ' '; }
  impl() { flat | grep -oE 'Implementation\*\*.*- \*\*Review\*\*'; }   # the Implementation bullet
  review() { flat | grep -oE '\*\*Review\*\*.*Agents. reports'; }       # the Review bullet
  ```

  - the commit-before-delegating line names the `frontend-implementer`: `flat | grep -oE 'Before delegating[^.]*frontend-implementer'`;
  - the design angle: `flat | grep -oi '<term>'` finds each of `design angle`, `frontend/src`, `Clarify`, `small task`, `Explore is run` and `existing screens`, and reading shows they belong to the design-angle text;
  - design and UI problems in existing UI outside the change filed only after the engineer agrees, other findings by step 10: `flat | grep -o '<term>'` finds each of `area:frontend`, `engineer agrees` and `step 10`;
  - routing of implementation slices touching `frontend/` to `frontend-implementer`: `impl | grep -o 'frontend-implementer'` finds it; and the "Failing tests" (pass 1) text stays with `implementer`: ``impl | grep -oiE 'failing tests[^.]*`implementer`|`implementer`[^.]*failing tests'`` finds it, and reading shows pass 1 is not given to `frontend-implementer`;
  - the frontend review check: `review | grep -oE '\.agents/design/|screenshots'` finds both terms;
  - `flat | grep -o 'implementer or frontend-implementer report'` finds the phrase.
- [ ] 4.2 Edit constitution §1 step 2 with the design-angle sentence (design D5) and step 10 with the own-UI sentence (design D7). Verify: `git diff main -- openspec/constitution.md` touches only steps 2 and 10, one sentence each.
- [ ] 4.3 Add the layer-split rule for implementation task groups to `openspec/config.yaml` `rules.tasks` as design D4 says, including the order (in a change with both layers, backend implementation groups come before the frontend groups that call them); the "Section 1 is Failing tests" rule in `rules.tasks` and the "Pass 1 production code" apply guidance are not changed. Verify: `npx -y @fission-ai/openspec@1.15.0 instructions tasks --change workflow-frontend-implementer --json` lists the new rule, and `git diff main -- openspec/config.yaml` adds only that one rule.
- [ ] 4.4 Add `frontend/screenshots/` to `.gitignore` (design D8). Verify: a file saved in `frontend/screenshots/<change-name>/` does not show in `git status`.

## 5. Proof

- [ ] 5.1 Dry run in Claude Code, after 3.4 and a restart (design D12):
  - #165 goes through Clarify and Research up to the design questions; no spec deltas are written for #165;
  - a `researcher` on the design angle returns design questions, and existing-UI problems apart;
  - the main agent asks them before any code;
  - a throwaway branch is made off this one with `git switch -c <throwaway name, for example dry-run-frontend-implementer> workflow-frontend-implementer` (not `scripts/flow/change-start`, which branches from `origin/main` and would take the real #165 branch name); on it a scratch change is created with `npx -y @fission-ai/openspec@1.15.0 new change ui-required-fields`, never by hand; its proposal Clarifications hold the design answers and its minimal `tasks.md` holds the slice;
  - the `frontend-implementer` builds that slice and lists its screenshot paths in `frontend/screenshots/ui-required-fields/`: each page the slice touches and each state it changes (for example the error shown after an empty submit, or a dialog open), at desktop and 360 px;
  - the `reviewer` checks the slice against `.agents/design/` and the screenshots; design-rule and WCAG 2.2 AA violations in UI the slice adds or edits are fixed on the throwaway branch; design and UI problems in existing UI outside the slice are listed to the engineer and filed only after agreement (#168 is already filed).

  Verify (design D12): the proof is screenshots only. The engineer attaches screenshots to PR #169 by hand, showing how the new flow works (for example the design questions asked before code, the frontend-implementer's work and the reviewer's work) and what result it gives (the UI). They are also the proof for 3.1 and 3.3. No throwaway-branch SHA, no pushed branch and no text logs are required. The throwaway branch is not pushed and is deleted right after the run, with the scratch change and the code; no #165 code survives. opencode and Codex are not run, and routing in a change with both layers is not exercised (#165 is frontend-only); the PR states both are unverified.

## Workflow follow-up

- Code review gate: the PR holds the proofs (the recorded checks of tasks 1-4 and the engineer's dry-run screenshots of 5.1); the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR; nothing merges into `openspec/specs/`.
- Mark the PR ready for the final review by a code owner other than the engineer; the owner approves the role files, `openspec/config.yaml` and the constitution (§1 steps 2 and 10).
