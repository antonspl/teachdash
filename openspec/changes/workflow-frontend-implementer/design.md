# Design

## Context

See `proposal.md` for the decisions. The engineer revised part of the design after the spec gate (proposal Clarifications, G-1 to G-11); D2 (C13), D5, D7, D8, D11 and D12 are revised, D6 is removed, and D13-D16 are added. Tasks 1-4 were done before that revision. After the critique of that revision (G-12 to G-19), D2 (C0), D3, D5, D7, D8, D11, D12, D13, D14 and D15 are revised. After a further critique (G-20, and the fixes F7 and F8), D2 (C13), D3, D5, D7, D8, D11 and D14 are revised.

- `scripts/setup-agents` builds every role from `.agents/roles/*.md`. `body()` copies the role body verbatim into Claude Code, opencode and Codex. Write roles get no tool limits. A body must not contain three single quotes (it breaks the Codex TOML string). `validate_roles` treats every `*.md` in `.agents/roles/` as a role, so a shared include cannot live there. `--check` hashes only `.agents/roles` and `.agents/skills`.
- The permission files (`.claude/settings.json`, `opencode.json`, `.codex/rules/`) do not list `docker` or `npx`, so each such call prompts the engineer. The new role gets no permission change.
- `.agents/roles/implementer.md:3` describes the role as "Use for all code writing in teachdash."
- Frontend look rules today: all values are tokens in `frontend/src/styles/tokens.css`; `npm run lint` runs `scripts/check-design-tokens.mjs`, which fails on look values outside that file; pages live in `frontend/src/pages/` and use `src/ui/` components. UI copy tone and spacing rules are recorded only in archived changes.
- `frontend/playwright.config.ts` hardcodes `baseURL` to `http://localhost:8081`, which serves the built app from `compose.yaml`. Its `outputDir` is `test-results/`, which Playwright empties on each run.
- A frontend-only change has one pass (`openspec/config.yaml` tasks and apply rules), and each of its task groups lands its own "[<scenario ID>]" tests (the frontend-only exception in `rules.tasks`). Constitution step 7 speaks of backend tests only, and the "Section 1 is Failing tests" rule in `rules.tasks` says "one backend test per scenario (JUnit 5, Testcontainers, RestAssured)". In a change with both layers, archived `2026-11-02-login-invalid-credentials`, task 1.5, adds the Vitest test `[web-client/R5-S4]` to the one "Failing tests" section. How frontend tests fit pass 1 (constitution §2, no frontend stubs, coverage by `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`) is #170; this change adds no test rules (decision 3).
- Constitution §1 step 10 and the reviewers' triage: security problems, data loss and spec violations are fixed in the PR; every other finding becomes an issue. This change adds one sentence to step 10 for UI the change adds or edits (D7).
- State after tasks 1-4 (before the post-gate revision): `researcher.md` has a design-angle bullet that returns design questions and existing-UI problems apart; `reviewer.md` has the frontend Check bullet (design sources, screenshots, leftover `zshots.spec.ts` or `frontend/screenshots/`); `code-gate-reviewer.md` has one bullet checking frontend diffs against `project-rules.md`; `AGENTS.md` has the design-angle text and the out-of-scope filing text; constitution §1 step 2 has the design-angle sentence. In `.agents/design/`, `project-rules.md` C13 holds the out-of-scope filing text, and `README.md`, `checklist.md` and `critique-rubric.md` name the `reviewer` and the design-angle `researcher` as readers (`checklist.md:5-6` names "the frontend-implementer before it reports, the reviewer on the diff"). The README and `project-rules.md` C0 state the precedence "the spec, then the project rules, then the sources", without the design code. `frontend-implementer.md` takes screenshots "when the slice changes pages (`frontend/src/pages/`) or `src/ui/` components". `researcher.md`'s description says "Use for all investigation in teachdash", and `CONTRIBUTING.md:52` says the agent "runs the spec-critic".
- Constitution §1 names the spec critic in the Roles paragraph ("helped by the spec-critic (spec gate)") and in step 5 ("the spec-critic agent attacks the draft"). It does not name the slice reviewer; step 10 names only the code-gate review.
- `openspec/config.yaml` `rules.design` has two rules (justify new dependencies, list Flyway migrations) and `rules.specs` two (a scenario per requirement, scenario IDs).

## Goals / Non-Goals

**Goals:**

- A frontend slice is built by a role that has read the design sources, and its result is checked on screenshots before review.
- Design questions reach the engineer before any spec or frontend code is written.
- Every frontend step has a frontend role that accounts for design: research, spec critique, implementation and slice review; the code-gate review covers design for a PR touching `frontend/src`.
- Design is part of the spec: a "UI design" section in `design.md` and design-affecting behaviour in scenarios.
- One file of project rules settles every conflict between the sources and our own rules; the design code of #171 will sit above it.
- Backend and tooling roles (`implementer`, `researcher`, `spec-critic`, `reviewer`) do not load the design sources.
- The design works in Claude Code, opencode and Codex; this change proves it in Claude Code only (D12).

**Non-Goals:**

- Enforcing the design rules by a script, beyond the existing `check-design-tokens.mjs`.
- Fixing existing UI that the change does not add or edit, and sweeping or filing design and UI problems in existing UI: existing UI design is #171.
- The design code itself, and `@axe-core/playwright` automated a11y checks (a new dependency, constitution §7): both go with #171.
- A frontend code-gate role: the `code-gate-reviewer` stays one per PR for both layers.
- Test rules for a change with both layers (#170).
- Keeping the vendored copies in sync with upstream automatically. The README records the pinned commits for a manual re-sync.
- Proving the roles in opencode and Codex, and proving routing in a change with both layers (D12).

## Decisions

### D1. `.agents/design/` layout and sources

| File | Source @ commit | Licence | About |
|---|---|---|---|
| `README.md` | ours | - | Index, ~35 lines: reading order, precedence, a source/path/commit/licence-file table for re-syncs, the NOTICE note (D10) |
| `project-rules.md` | ours | - | ~80 lines, D2 |
| `checklist.md` | vercel-labs/web-interface-guidelines @434b7f9 `command.md` | MIT | ~130 lines, plus the three impeccable audit points |
| `accessibility.md` | addyosmani/web-quality-skills @afa8da9 `skills/accessibility/SKILL.md` | MIT | ~250 lines |
| `accessibility-wcag22.md` | same, `references/WCAG.md` | MIT | ~100 lines |
| `accessibility-patterns.md` | same, `references/A11Y-PATTERNS.md` | MIT | ~120 lines |
| `refactoring-ui.md` | wondelai/skills @c526558 `refactoring-ui/SKILL.md` | MIT | ~150 lines |
| `refactoring-ui-patterns.md` | same, `refactoring-ui/references/advanced-patterns.md` | MIT | ~250 lines |
| `frontend-design-copy.md` | anthropics/skills @e4bb5d9 `skills/frontend-design/SKILL.md` | Apache-2.0 | ~35 lines |
| `critique-rubric.md` | pbakaus/impeccable @b49177d `skill/reference/critique-rubric.md` | Apache-2.0 | ~200 lines |
| `licenses/<owner>-<repo>.LICENSE` x5 | upstream licence files (D10) | - | verbatim |

Total about 1,350 lines (about 14k tokens). Reading order: `README.md`, then `project-rules.md`, then `checklist.md` (about 3k tokens, every slice); `accessibility.md` when the slice adds or changes interactive elements, forms or dialogs (this is what "new UI" means for the reading order); the rest on demand.

The implementer fetches each file by the pinned commit, not from a branch. Per source:

- **addyosmani/web-quality-skills** (MIT, "Copyright (c) 2026 Addy Osmani"):
  - drop: frontmatter; `npx lighthouse` and the global axe install (SKILL.md 405-418); links to sibling skills; AAA columns and the AAA table; the external tools table; the hand-made focus trap (`src/ui/Dialog` does it);
  - rewrite: the Lighthouse / DevTools MCP workflow (SKILL.md 14-23) to Playwright `npm run test:e2e`, a keyboard pass and screenshots; raw px and hex CSS samples to tokens; "Session Expiring" to sentence case; the global 0.01ms motion kill per C6; dialog examples to native `<dialog>` and `src/ui/Dialog`;
  - replace: the focus-on-error example (it focuses the field and then the summary, so it announces twice) with a pointer to `openspec/specs/web-client/spec.md` R14-S12 and R17-S3.
- **wondelai/skills** (MIT, "Copyright (c) 2025 Wondel.ai sp. z o.o."):
  - drop: Tailwind triggers; 10/10 scoring; Amazon affiliate links (SKILL.md 244-251); image, avatar and hero advice; radius "systems"; Tailwind breakpoints; hover-to-reveal card titles; its other references (accessibility-depth, theming-dark-mode, data-visualization, animation);
  - rewrite: remove uppercase-small labels; px scales, Tailwind classes and hex to `--space-*`, `--text-*`, `--color-*`, `--shadow-card`, `--content-width`; illustrated empty states to text plus a `src/ui/` action; input and button states to the `src/ui/` Button and Field variants.
- **vercel-labs/web-interface-guidelines** (MIT, "Copyright (c) 2025 Vercel Labs"):
  - drop: the `$ARGUMENTS` header; virtua and nuqs (new dependencies); CDN preconnect and font preload (§10, system font); dark mode; hydration (Vite SPA); "&" over "and";
  - rewrite: Title Case and "Save API Key" (C1); `focus-visible:ring-*` to a token outline; Tailwind to plain CSS; C7, C8, C11;
  - keep: native select colours and the output format;
  - add, in our own words, the three impeccable audit points: reduced motion keeps state feedback; interrupted touch gestures; `will-change`.
- **anthropics/skills** (Apache-2.0): keep only the line length (maps to `--content-width`), restraint, quality floor, screenshots and copy sections. Drop the frontmatter, aesthetic risk, client memory, hero, typefaces, calibration and the hex palette plan. Tone per C10. The repository has no root LICENSE; the licence is `skills/frontend-design/LICENSE.txt` @e4bb5d9.
- **pbakaus/impeccable** (Apache-2.0, "Copyright 2025 Paul Bakaus"): take the template from `skill/`, not the build output (it has `{{...}}` placeholders). Keep the cognitive-load checklist, the "check for" lists of the Nielsen heuristics and the persona walk-throughs. Drop the `critique.md` link, `PRODUCT.md` and `impeccable init`, the landing and e-commerce personas and the numeric scoring. "44x44pt" becomes the AA floor of 24 CSS px. Its `NOTICE.md` is handled in D10.

Alternative: `.agents/skills/`. Rejected: every tool would list the sources to every role, including the main agent and the backend implementer, and `setup-agents` would regenerate on each edit.

### D2. `project-rules.md`

It comes first after the index and wins over every vendored source. Its opening paragraph says it wins over the vendored sources only and points to C0 for the full order; it used to say it wins over every file in `.agents/design/`, which would contradict the design code from #171 (G-21, applied in 22f1a98). It holds:

- all colours, fonts, sizes, spacing, radii and shadows are tokens in `frontend/src/styles/tokens.css`; the `npm run lint` (`check-design-tokens.mjs`) rules apply; pages use `src/ui/` components;
- nothing loads from third-party CDNs (constitution §10);
- UI copy goes through i18n, sentence case, informal and friendly, like talking to a friend;
- buttons never touch fields, cards or each other; native parts such as the select arrow are inset like text; controls go exactly where the spec says;
- new UI built in a change meets these rules and WCAG 2.2 AA;
- the resolved conflicts:
  - **C0** precedence: as written by task 1.2, the spec, then project rules, then the sources; task 11.4 changes it to the order of D14 (the change's spec, then design code from #171, then project rules, then the sources), or to a pointer to the README, which states it (G-12);
  - **C1** sentence case, not Title Case;
  - **C2** no all-caps or uppercase-small labels;
  - **C3** tokens only; a new value becomes a new role-named token added to `tokens.css`;
  - **C4** `--font-body` system stack, no web fonts, no "distinctive typeface";
  - **C5** the one `--radius` and `--shadow-card` are ours; changing, renaming or deleting them, or any existing token, is an engineer decision; the frontend-implementer only adds new role-named tokens;
  - **C6** reduced motion removes non-essential motion but keeps state feedback;
  - **C7** native elements first; never add key handlers to a `<button>`;
  - **C8** personal-data fields get autocomplete tokens (WCAG 1.3.5), not `autocomplete="off"`;
  - **C9** touch target floor 24 CSS px (AA); controls use `--control-height`;
  - **C10** friendly, second person, no apologies; a message says what happened and how to fix it;
  - **C11** quotes follow each language's typography in the i18n files (ru uses «»);
  - **C12** focus on error follows the specs (web-client R14, R17);
  - **C13** no scores and no P0-P3. Findings at review are triaged as constitution §1 step 10 says, with this change's step 10 sentence (D7): security problems, data loss, spec violations, and design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change itself adds or edits are fixed in the PR. Task 1.2 wrote it as "design-rule" only; task 11.2 changes it to this generic wording (G-20). "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits; touching an existing component for an unrelated reason does not pull in its existing problems (#168, field border contrast, stays out unless the change edits that border). Every other finding becomes an issue as step 10 says; there is no sweep or filing of problems in existing UI outside the change (D6 is removed, existing UI design is #171). The file holds the out-of-scope filing line from task 1.2, which task 11.2 removes;
  - **C14** empty states are text plus a `src/ui/` action, no image assets.

### D3. `frontend-implementer` role

`.agents/roles/frontend-implementer.md` frontmatter: `name: frontend-implementer`, class `medium`, access `write`, web `true`, and

> description: Implements one bounded slice that touches frontend/, building the UI to the design sources in .agents/design/. Use instead of implementer for any slice touching frontend/.

The body is short, because `setup-agents` copies it verbatim and cannot include another file:

1. Read `.agents/roles/implementer.md` and follow it.
2. Read `.agents/design/` in this order: `README.md`, then `project-rules.md`, then `checklist.md`; `accessibility.md` when the slice adds or changes interactive elements, forms or dialogs; the rest on demand.
3. One line: where `implementer.md` and this file differ, this file wins.
4. Passes: it works only in pass 2 of a change with both layers, where it writes its group's code, or in the single pass of a frontend-only change, where its group writes its code and its own "[<scenario ID>]" tests (the frontend-only exception in `rules.tasks` of `openspec/config.yaml`). It writes no pass 1 failing tests; the "Failing tests" section of a change with both layers stays with the backend `implementer` (decision 3, #170).
5. Additions:
   - build new UI to the design sources; new UI meets the project rules and WCAG 2.2 AA;
   - it may only add new role-named tokens to `tokens.css` inside its slice; changing an existing token's value, renaming it or deleting it is an engineer decision (C5), so it stops and reports instead;
   - when the slice changes rendered UI (any change to `frontend/src/**/*.tsx` except `*.test.tsx`, `frontend/src/**/*.css`, or `frontend/src/i18n/*.json`; G-17), take screenshots of each page it touches and each state the slice changes as D8 says, and check them against the sources before reporting;
   - list the screenshot paths in its report; the list does not count toward `implementer.md`'s 15-line report limit;
   - before the code gate, when the main agent asks, take the final screenshot set of every screen and state listed in the change's "UI design" section, at desktop and 360 px, into `frontend/screenshots/<change-name>/` as D8 says, and list the paths (G-16);
   - no new dependencies.

Task 2.1 wrote the body with the earlier trigger (pages or `src/ui/` components) and without the final set; task 5.4 reworks both.

`implementer.md` stays as it is, including line 3 ("Use for all code writing in teachdash."). Routing relies on the frontend-implementer's description and the `AGENTS.md` routing (D4); the remaining line is a risk (see Risks). The body has no three single quotes.

Alternative: one implementer that reads the design sources when a slice touches `frontend/`. Rejected: the backend implementer would carry the rule, and the engineer decided it stays unchanged.

### D4. Routing and the tasks rule

- `AGENTS.md`, in "Main agent is an orchestrator":
  - before delegating, the main agent commits its edits: "the `implementer`" becomes "the `implementer`, the `frontend-implementer`";
  - the implementation bullet: implementation slices that touch `frontend/` go to `frontend-implementer`, others to `implementer`; the "Failing tests" section of a change with both layers stays with `implementer`;
  - the review bullet: "implementer report" becomes "implementer or frontend-implementer report".
- `openspec/config.yaml` `rules.tasks` gets one layer-split rule: implementation task groups (after the test review task, or every group in a frontend-only change) are split by layer; a group touches either code under `frontend/` or code under `backend/`, never both. Repository tooling (`scripts/`, for example adding a capability to `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`, and config files) may go with either group. In a change with both layers, backend implementation groups come before the frontend groups that call them, so screenshots show real states, not 501 stubs (proposal decision 3). The spec-writer follows it, so one group is one slice for one role.
- No test rules change: the "Section 1 is Failing tests" rule in `rules.tasks` and the "Pass 1 production code" apply guidance stay as they are, and the "Failing tests" section of a change with both layers stays one section written by the backend `implementer` (decision 3). The frontend-only exception in `rules.tasks` and the rest of the apply guidance stay unchanged. Frontend tests in pass 1 are #170.

### D5. Frontend research

Revised after the spec gate (G-2), and after the critique of the post-gate revision (G-15, G-17); it replaces the design angle.

- The `frontend-researcher` (D13) takes any research angle about the UI: screens, components, frontend behaviour. It reads `.agents/design/`: `README.md` first, then `project-rules.md`, `checklist.md`; `accessibility.md` when the change adds or changes interactive elements, forms or dialogs; the rest (including `critique-rubric.md`) on demand. It always returns design questions on the screens the change touches: layout, states, copy, a11y, tokens and components.
- The generic `researcher` keeps backend, web research and tooling angles. Its design-angle bullet is removed from `researcher.md`, and its description changes from "Use for all investigation in teachdash" to: for all investigation, except UI angles, which go to `frontend-researcher` (G-15).
- Constitution §1 step 2: the design-angle sentence is replaced by one saying that a change touching `frontend/src` gets at least one `frontend-researcher` at Clarify (AGENTS.md). Step 2 already lets the engineer agree that a small task needs no research.
- `AGENTS.md`: the design-angle text in "Before a proposal" and in the research bullet is replaced by the routing above. The research bullet sends angles about the UI to the `frontend-researcher` and the rest to the `researcher`. The main agent asks the engineer the design questions; the answers go into the proposal's Clarifications and the "UI design" section of `design.md` (D15).
- Scopes, kept apart: the `frontend-researcher` and the `frontend-spec-critic` are for a change touching `frontend/src`; routing to `frontend-implementer` and `frontend-reviewer` is for any slice touching `frontend/`; screenshots are for a slice that changes rendered UI, that is any change to `frontend/src/**/*.tsx` except `*.test.tsx`, `frontend/src/**/*.css`, or `frontend/src/i18n/*.json` (G-17); the final screenshot set before the code gate is for a PR touching `frontend/src` whose "UI design" section lists screens (G-16).

### D6. Removed after the spec gate

Removed by G-3. There is no sweep or filing of design and UI problems in existing UI outside the change; existing UI design is #171. The out-of-scope filing text comes out of `AGENTS.md`, `researcher.md` (with its design-angle bullet) and `project-rules.md` C13. The own-UI rule (D7) is unchanged; other findings follow constitution §1 step 10 unchanged.

### D7. Review

Revised after the spec gate (G-3, G-6, G-7), and after the critique of the post-gate revision (G-14, G-16, G-17).

- `frontend-reviewer` (D13) does the slice review for every slice that touches `frontend/` (medium class), after every implementation slice (pass 2 of a change with both layers, or the single pass of a frontend-only change). It follows `reviewer.md`, reads `.agents/design/README.md` first, then `project-rules.md` and `checklist.md` (the README's reading order), and its report has two sections:
  - **Logic:** the `reviewer.md` checks;
  - **Design:** a pass against `project-rules.md` and `checklist.md`, the screenshots at desktop and 360 px, and a11y (keyboard, focus, `aria-*`, contrast). It opens the screenshot PNGs listed in the frontend-implementer's report itself and checks them against the change's "UI design" section (D15). The Design section always says what was checked, even when there are no findings. For a frontend slice with no screenshots it says "no screenshots: <reason>", so a slice that needed none differs from one that skipped them (G-17). The Design section does not count toward the 25-line report limit of `reviewer.md`; it has its own cap of 15 lines (G-14).

  A leftover `frontend/e2e/zshots.spec.ts` or any file under `frontend/screenshots/` in the diff is a finding (moved from `reviewer.md`).
- `reviewer.md`: the frontend Check bullet is removed; it reviews backend and tooling slices. `.agents/design/` stays on its "no unrequested changes" list, which the `frontend-reviewer` inherits, and its triage line keeps the own-UI case.
- `code-gate-reviewer.md` stays one per PR, strong class, both layers. Its frontend bullet is strengthened: for a PR touching `frontend/src` it reads `README.md` first, then `project-rules.md`, `checklist.md` (and `accessibility.md` for forms and dialogs), checks the diff against the "UI design" section of `design.md` and the final screenshot set (D8) whose paths the main agent passes, and its report gets a "Design" section that says what was checked. When there is no final set (for example the change's design says "UI design: none"), the Design section says "no screenshots: <reason>", so "not needed" differs from "skipped" (F8). Per-slice screenshots are not used for the code-gate review: the folder is cleared on each slice's run, the paths may be missing, they may predate review fixes, and, being gitignored, they are gone in a new session (G-16). The Design section does not count toward the 25-line report limit; it has its own cap of 15 lines, as for the `frontend-reviewer` and the `frontend-spec-critic` (G-21). The leftover `zshots.spec.ts` / `frontend/screenshots/` finding stays. `.agents/design/` stays on its "no unrequested changes" list, and its triage line keeps the own-UI case.
- `AGENTS.md` review bullet: slices touching `frontend/` go to the `frontend-reviewer`, other slices to the `reviewer`, after every implementation slice (pass 2 of a change with both layers, or the single pass of a frontend-only change); the `code-gate-reviewer` runs once per PR. For a PR touching `frontend/src`, before the code-gate review the main agent has the `frontend-implementer` take the final screenshot set (D8) and passes its paths to the `code-gate-reviewer` (G-16). The sentence "A slice that touches `frontend/` is also checked against `.agents/design/` and the screenshots" becomes the `frontend-reviewer` routing.
- Constitution §1 step 10 keeps its sentence, next to "Security problems, data loss and spec violations are fixed in the PR": design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change adds or edits (its markup, CSS, copy or behaviour) are fixed in the PR too. Task 4.2 wrote it naming only `.agents/design/project-rules.md`; task 9.2 makes it generic (G-20), so the design code of #171 is covered without a constitution edit. This sentence goes beyond issue #166's original "Done when"; the engineer decided to keep it in this change as the core of the requirement that new features come with good design (proposal decision 7).

Findings are triaged as constitution §1 step 10 says, with that sentence: security problems, data loss, spec violations, and design-rule violations (the design rules in `.agents/design/`, in the precedence order of its README) and WCAG 2.2 AA violations in UI the change itself adds or edits are fixed in the PR. The triage lines of `reviewer.md` (task 6.3) and `code-gate-reviewer.md` (task 7.2) use the same generic wording instead of "a design-rule ... violation"; the `frontend-reviewer` has no triage line of its own and inherits the one in `reviewer.md` (G-20). "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits (proposal decision 6); touching an existing component for an unrelated reason does not pull in its existing problems (#168 stays out unless the change edits that border). Every other finding becomes an issue as step 10 says. C13 says the same.

### D8. Screenshots

Revised after the critique of the post-gate revision (G-16, G-17).

- Trigger: a slice takes screenshots when it changes rendered UI, that is any change to `frontend/src/**/*.tsx` except `*.test.tsx`, `frontend/src/**/*.css`, or `frontend/src/i18n/*.json` (G-17, widened by F7 so CSS modules such as `frontend/src/ui/*.module.css` count and tests do not; it replaces "pages or `src/ui/` components"). These per-slice screenshots serve the slice review.
- Final set (G-16): before the code gate of a PR touching `frontend/src`, the `frontend-implementer` takes, by the method below, a final set of every screen and state listed in the change's "UI design" section, at both widths, into `frontend/screenshots/<change-name>/`, and lists the paths; the main agent passes them to the `code-gate-reviewer`. It is taken after all slices and their review fixes, so it shows the code under review, and it can be retaken in a new session. When the change's design says "UI design: none", there is no final set, and the `code-gate-reviewer`'s Design section says "no screenshots: <reason>" (D7, F8).
- Folder: `frontend/screenshots/<change-name>/`, cleared before each run, so a stale PNG cannot survive a failed run. `.gitignore` gets `frontend/screenshots/`. `test-results/` is not used, because Playwright empties it on each run.
- Widths: the desktop project's viewport and a 360 px viewport.
- Method:
  1. `docker compose up --build --wait`, so 8081 serves the slice's code;
  2. clear `frontend/screenshots/<change-name>/`;
  3. a temporary `frontend/e2e/zshots.spec.ts` that logs in with the `e2e/accounts.ts` helpers and captures each page the slice touches and each state the slice changes (for example the error shown after an empty submit, or a dialog open), at both widths, saving to that folder;
  4. `npx playwright test zshots --project=desktop-chromium`;
  5. delete the spec before committing.
- `docker` and `npx` are in no allow list, so each call prompts the engineer.
- The report lists the screenshot paths. The engineer attaches images to the PR by hand if wanted; `gh` cannot upload images. The `frontend-reviewer` (per-slice screenshots) and, for a PR touching `frontend/src`, the `code-gate-reviewer` (the final set) open the PNGs themselves (D7). For this change's dry run, screenshots attached by hand are the proof (D12).
- No script or Playwright project is added. The method is plain shell and Playwright, meant for all three tools, but only proven in Claude Code (D12).

### D9. Test phases

No change to test phases; see #170.

### D10. Licences and attribution

- Each upstream licence file is copied verbatim to `.agents/design/licenses/<owner>-<repo>.LICENSE`. For anthropics/skills, which has no root LICENSE, the source is `skills/frontend-design/LICENSE.txt` @e4bb5d9.
- Each vendored file starts with one header line: source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/`, and "modified by teachdash". This states the changes as Apache-2.0 §4(b) requires; MIT needs the licence text, which `licenses/` holds.
- impeccable has a `NOTICE.md`. Its notices cover none of what goes into `critique-rubric.md` (they are about iOS and Android references, fonts and consent rules). Apache-2.0 §4(d) excludes notices that do not pertain to any part of the derivative work, so no copy is included. The README records this reason.

### D11. Approval

The role files (including the three new ones of D13), `openspec/config.yaml` and the constitution (§1 Roles paragraph and steps 2, 5 and 10) need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md`, `CONTRIBUTING.md` and `.gitignore` do not.

So any engineer can change the frontend routing in `AGENTS.md` and `CONTRIBUTING.md` and the rules in `.agents/design/`, including `project-rules.md` and later the design code of #171, without the owner (constitution §1, Roles), even though step 10 now makes violations of the design rules in `.agents/design/` in the change's own UI fix-in-PR (G-20), so such an edit changes what a gate must fix. The engineer accepted this (proposal decision 11): every PR still needs approval from a code owner other than the author, and the reviewers flag unrequested changes to `.agents/design/`.

### D12. Dry run and tool support

Revised after the spec gate (G-9, G-11), and after the critique of the post-gate revision (G-13, G-16).

- The dry run is in Claude Code, after `scripts/setup-agents` and a restart, with all new roles, a full run on #165 (ui-required-fields):
  1. the `frontend-researcher` (and the generic `researcher` only if a non-UI angle exists) returns design questions;
  2. the main agent asks them before any spec or code;
  3. a throwaway branch is made off this one with `git switch -c <throwaway name, for example dry-run-frontend-implementer> workflow-frontend-implementer`. `scripts/flow/change-start` is not used: it branches from `origin/main` and would take the real #165 branch name. On that branch a scratch change is created with `npx -y @fission-ai/openspec@1.15.0 new change ui-required-fields`, never by hand;
  4. the `spec-writer` writes a minimal proposal (Clarifications with the answers), `design.md` with the "UI design" section (D15), one spec delta and a minimal `tasks.md`;
  5. the `frontend-spec-critic` attacks it;
  6. the `frontend-implementer` builds the slice, with screenshots in `frontend/screenshots/ui-required-fields/` (D8);
  7. the `frontend-reviewer` reviews it (Logic and Design);
  8. the `frontend-implementer` takes the final screenshot set of every screen and state in the scratch change's "UI design" section into `frontend/screenshots/ui-required-fields/` (D8), and the main agent passes those paths to the `code-gate-reviewer`, which reviews the throwaway slice (its diff against `workflow-frontend-implementer`) with its "Design" section (G-13, G-16).
- The proof is screenshots only (proposal decision 10): the engineer attaches screenshots to PR #169 by hand, showing how the new flow works (for example the design questions asked before any spec or code, and the frontend-spec-critic's, the frontend-implementer's, the frontend-reviewer's and the code-gate-reviewer's work) and what result it gives (the UI). No throwaway-branch SHA, no pushed branch and no text logs are required. The same screenshots prove the new role files, the `frontend-implementer.md` rework, and the `researcher.md`, `reviewer.md` and `code-gate-reviewer.md` edits, in addition to their text checks. The throwaway branch is not pushed and is deleted right after the run, with the scratch change, its spec delta and the code; no #165 code or spec survives.
- opencode and Codex are unverified. The Codex sandbox may block `docker` or Chromium, and whether opencode and Codex can read screenshot images (for the `frontend-reviewer` and the `code-gate-reviewer`) is unverified.
- Routing in a change with both layers (implementation groups split by layer, both roles in one change) is unproven: #165 is frontend-only, so the dry run does not exercise it.

### D13. Frontend roles

Added after the spec gate (G-1, G-2, G-5, G-6); revised after the critique of the post-gate revision (G-14, G-18, G-19). Three new role files, alongside `frontend-implementer.md` (D3):

| Role | Base role | Class | Access | Web |
|---|---|---|---|---|
| `frontend-researcher` | `researcher.md` | medium | read | as the base (`true`) |
| `frontend-spec-critic` | `spec-critic.md` | medium | read | as the base (`false`) |
| `frontend-reviewer` | `reviewer.md` | medium | read | as the base (`false`) |

Each body is short, as in D3, because `setup-agents` copies it verbatim and cannot include another file: read the base role and follow it; then read `.agents/design/` in the README's order: `README.md` first, then `project-rules.md`, then `checklist.md`, `accessibility.md` when the change or slice adds or changes interactive elements, forms or dialogs, and the rest on demand, as the `frontend-implementer` does (D3); one line says that where the base role and this file differ, this file wins; then only the additions. Because every frontend role and the code-gate-reviewer's frontend part start from the README, #171 only adds a file (D14). Each `description` says to use it instead of the base role for frontend work, as the frontend-implementer's does. No body has three single quotes.

- `frontend-researcher` additions: D5 (any UI angle, the reading order, design questions on the screens the change touches).
- `frontend-spec-critic` additions: the `spec-critic` checks plus a design pass against `.agents/design/` (project rules, checklist, accessibility) and the `rules.design` format (D15): missing states, scenarios without focus behaviour, copy not through i18n or not per the tone rules, contradictions with `project-rules.md`, unanswered design questions. It keeps one report with the full `spec-critic` checks, also for a change with both layers; its "Design" section does not count toward the base 25-line limit and has its own cap of 15 lines (G-19). Its "unanswered design question" findings are questions for the engineer: the main agent asks them, and the spec-writer records the answers in the fix round (G-18). One critic per change: when the change touches `frontend/src`, the `frontend-spec-critic` runs instead of the `spec-critic`; otherwise the `spec-critic`. `AGENTS.md`'s spec-critique bullet (with the design-question route), `CONTRIBUTING.md:52` (G-15) and constitution §1 (the Roles paragraph and step 5, where the critic is named) say so.
- `frontend-reviewer` additions: D7 (Logic and Design sections, the Design section's own 15-line cap and its "no screenshots: <reason>" line, screenshots opened by the role, the "UI design" section, the leftover-screenshot finding).

Alternative: design bullets in the generic roles (the state after tasks 1-4). Rejected by the engineer after the spec gate: such a bullet is easy to forget and loads the design sources into backend work.

### D14. Precedence

Added after the spec gate (G-8); revised after the critique of the post-gate revision (G-12). `.agents/design/README.md` states the precedence: the change's spec > design code (from #171, not yet written) > `project-rules.md` > vendored community sources. `project-rules.md` C0 is changed to the same order, or to a pointer to the README, which states it, and the file's opening paragraph says it wins over the vendored sources only and points to C0 for the full order (D2; G-21). Every frontend role and the code-gate-reviewer's frontend part read the README first (D7, D13), and constitution §1 step 10 and the triage lines name the design rules in `.agents/design/` in the README's precedence order (D7, G-20), so #171 then adds a file and changes no roles and no constitution text. The README's list of readers names the four frontend roles and the `code-gate-reviewer`. `checklist.md:5-6` names all its readers: the `frontend-implementer`, the `frontend-reviewer`, the `frontend-spec-critic` and the `code-gate-reviewer`. `critique-rubric.md` names the `frontend-reviewer` and the `frontend-researcher` where it names the reviewer and the design-angle researcher today.

### D15. Spec format

Added after the spec gate (G-4); revised after the critique of the post-gate revision (G-18). Two rule groups in `openspec/config.yaml`:

- `rules.design`: a change touching `frontend/src` has a "UI design" section in `design.md` with fixed headings: screens touched; layout (desktop and 360 px); states per screen (empty, error, loading, disabled, success); copy (i18n keys and text); a11y (focus order, keyboard, `aria-*`); components and tokens (reused or new); plus an ASCII sketch for a new screen or a significant layout change. A change touching `frontend/src` with no rendered UI change may instead have one line: "UI design: none, <reason>".
- `rules.specs`: design-affecting behaviour goes into scenarios (an error message and where it appears, focus after an error, empty and loading states, what is disabled during a request); visual layout stays in `design.md`. This adds scenarios, not requirements: the 6-requirement limit holds.

The existing rules in both groups and the rest of `openspec/config.yaml` stay unchanged. This change touches no `frontend/src`, so its own `design.md` has no "UI design" section.

### D16. Postponed

Added after the spec gate (G-9). `@axe-core/playwright` automated a11y checks go with #171 (a new dependency, constitution §7). Whether opencode and Codex can read screenshot images is unverified (D12).

## Risks / Trade-offs

- **Stale screenshots** if `--build` is skipped: 8081 serves the old build. → D8 step 1, and the role says to rebuild.
- **A stale PNG from an earlier run** passes as new. → The change's folder is cleared before each run (D8).
- **Lost screenshots** in `test-results/`, which Playwright empties. → They go to `frontend/screenshots/<change-name>/`.
- **The temporary spec gets committed.** → The role says to delete it, and the `frontend-reviewer` and the `code-gate-reviewer` see any `zshots` file in the diff.
- **A frontend slice goes to `implementer`**, because `implementer.md:3` still says "Use for all code writing in teachdash." → The frontend-implementer's description says "Use instead of implementer for any slice touching frontend/", and `AGENTS.md` routes such slices to it.
- **Frontend tests in pass 1 of a change with both layers stay unclear** (constitution §2 and step 7 against practice). → Out of scope, filed as #170; this change adds no test rules (D4, D9).
- **Routing in a change with both layers is unproven** (the dry run on #165 is frontend-only). → Marked unproven (D12); the first change with both layers shows it.
- **Where "UI the change adds or edits" ends** may be argued at review. → It is defined as markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits (D7, C13).
- **opencode and Codex may not run the screenshot method** (sandbox, PNG display). → Marked unverified (D12); the first frontend change in those tools shows it.
- **Each `docker` and `npx` call prompts.** → Accepted; no permission change.
- **Leftover network-tool or Tailwind lines** in a copy, which an agent might follow. → Task 1.7 greps for them.
- **impeccable build output copied** instead of `skill/`. → D1 names the path; task 1.5 checks it.
- **A missing "modified by teachdash" header** breaks Apache-2.0 §4(b). → Task 1.6 checks every file.
- **#168 will be the first design finding** in the dry run. → It is filed already and stays out of this change unless a change edits that border; existing UI design is #171.
- **A frontend change goes to a generic role** (`spec-critic`, `reviewer`), whose design bullets are gone. → Each frontend role's description says to use it instead of the base role, and `AGENTS.md`, `CONTRIBUTING.md` and constitution §1 route frontend work to them (D5, D7, D13). The `researcher.md` description now sends UI angles to `frontend-researcher` (G-15).
- **A Design section that says nothing**, so a skipped design pass looks like a clean one. → The `frontend-reviewer` and the `code-gate-reviewer` always say what was checked, and each says "no screenshots: <reason>" when it has none (a frontend slice without them, or a PR with no final set) (D7).
- **A long Design section crowds out logic findings** in a 25-line report. → The Design sections of the `frontend-reviewer`, the `frontend-spec-critic` and the `code-gate-reviewer` sit outside the base limit, with their own cap of 15 lines (D7, D13; G-21).
- **The code-gate review sees stale or missing screenshots** (per-slice runs clear the folder, shots predate review fixes, gitignored shots are gone in a new session). → The final screenshot set is taken before the code gate and its paths are passed by the main agent (D8).
- **Base role and frontend file drift apart** (a base edit contradicts a frontend addition). → The frontend file holds only additions and wins where they differ (D13).
- **More reading per frontend step**, about 3k tokens always, now in research, spec critique, implementation and review. → The rest is read on demand; backend and tooling roles read none of it.
- **The rules are instructions, not checks, and any engineer may change `project-rules.md` without the owner**, although step 10 makes its violations in the change's own UI fix-in-PR. → Accepted (D11): every PR still needs approval from a code owner other than the author, and the reviewers guard `.agents/design/` against unrequested changes.

## Open Questions

None.
