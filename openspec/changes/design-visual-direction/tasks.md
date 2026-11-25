# Tasks

This is a tooling change with `skip_specs: true`: no "Failing tests" section and no test review stop. Each task says how it is proven; the proof is recorded in the PR for the code gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0`. Base for the checks below: the commit that adds this change's artifacts (`<base>`), or `main` once the branch is rebased. The "r3 folder" is `~/github/teachdash-design-mockups/r3-b-layout-a-colours/` on the engineer's machine, outside the repository.

## 1. Write the design code

- [ ] 1.1 Write `.agents/design/design-code.md` from the text between the DESIGN-CODE START and DESIGN-CODE END markers of `design.md` (decision D1, as the engineer agreed it at the spec gate), replacing the stub entirely. Proof: the file no longer contains "This file is filled by #171"; `diff` of the file against the marked block of `design.md` (markers removed) shows no difference.
- [ ] 1.2 Check the headings. Proof: `grep -n '^#' .agents/design/design-code.md` is pasted in the PR and shows `# Design code` and the sections listed in its Contents line (Direction, Tokens, Colour, Type, Space and shape, States, Buttons, icons and tooltips, Components, Layout and screens, Copy and languages, Accessibility, Departures from the project rules, Deferred, Redesign plan).
- [ ] 1.3 Check the token values against the r3 folder. Proof, run locally by the implementer or the reviewer: every `--name: value` declaration in the r3 folder's `css/tokens.css` that this file lists (all colour, type, space, shape, control, icon and layout tokens; not `--specimen-min`) appears in the file's token tables with the same value, for example `grep -oE '\-\-[a-z-]+: [^;]+;' <r3>/css/tokens.css` and a check of each against the tables; the differences, if any, are listed in the PR and each is explained (for example the two-value rows for `pointer: fine` and `:lang(ru)`).
- [ ] 1.4 Check the file is self-contained and holds no new decision. Proof: `grep -niE 'see the mockup|see r3|teachdash-design-mockups|\.\./' .agents/design/design-code.md` prints nothing; the reviewer reads each section against proposal decisions 1-22 and finds no value or rule that is neither a decision nor a value from the r3 folder.
- [ ] 1.5 Check the file keeps the lint's assumptions. Proof: it states that custom properties are defined only in `tokens.css`, that a font is one `var(...)`, and that `@media` and container conditions are not tokens (the Tokens section says each); `grep -n 'shadow-card\|color-action-success\|--space-[1-4]' .agents/design/design-code.md` prints only the rename map and the departures lines.
- [ ] 1.6 Check only `design-code.md` changed outside the change folder, and the README is consistent. Proof: `git diff --name-only <base>..HEAD` lists only `.agents/design/design-code.md` and files under `openspec/changes/**` (and, after task 2.2, `openspec/changes/archive/**`); `.agents/design/README.md` is unchanged and its sources table row for `design-code.md` ("ours") is still correct; `git diff <base>..HEAD -- frontend backend scripts .agents/roles` is empty.

## 2. Workflow follow-up

- [ ] 2.1 Spec gate: the engineer decides the pending point in proposal Clarification 23 (where the mockup lives). If the engineer chooses to copy it into the repository, a task is added here before the spec gate; with the default (b) nothing changes.
- [ ] 2.2 Code review gate: run the `code-gate-reviewer` on the PR; the engineer approves the code gate.
- [ ] 2.3 Archive the change in the same PR (`openspec archive`; nothing merges into `openspec/specs/`).
- [ ] 2.4 Mark the PR ready for the final code owner review. The PR closes #171.

## 3. Issues to file after the change (not an implementer task)

The engineer or the main agent files these from the "Redesign plan" section of `design-code.md`, after the change is archived, each saying which small review items it carries:

- [ ] 3.1 File the issues in the plan's order: tokens (folds #168); primitives 2a and 2b; Tooltip, IconButton and Menu; app frame 4a and 4b; admin teachers 5a and 5b (search and paging, with the backend); login, set-password and not found; profile.
- [ ] 3.2 Note on the existing issues that #102 and #103 land first, #172 folds into #102, and #165 is its own change; note on #115-#120, #132, #133 and #138 that the builder follows the design code on phones too (decision 15) and that the lesson tree is built with #116/#117.
- [ ] 3.3 Record the deferred points (design-code "Deferred") on the feature issues named there (#118, #120, #135, #126, #127, #56).
