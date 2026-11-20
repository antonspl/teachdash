# Design

## Context

See `proposal.md` for the decisions.

- `scripts/setup-agents` builds every role from `.agents/roles/*.md`. `body()` copies the role body verbatim into Claude Code, opencode and Codex. Write roles get no tool limits. A body must not contain three single quotes (it breaks the Codex TOML string). `validate_roles` treats every `*.md` in `.agents/roles/` as a role, so a shared include cannot live there. `--check` hashes only `.agents/roles` and `.agents/skills`.
- The permission files (`.claude/settings.json`, `opencode.json`, `.codex/rules/`) do not list `docker` or `npx`, so each such call prompts the engineer. The new role gets no permission change.
- `.agents/roles/implementer.md:3` describes the role as "Use for all code writing in teachdash."
- Frontend look rules today: all values are tokens in `frontend/src/styles/tokens.css`; `npm run lint` runs `scripts/check-design-tokens.mjs`, which fails on look values outside that file; pages live in `frontend/src/pages/` and use `src/ui/` components. UI copy tone and spacing rules are recorded only in archived changes.
- `frontend/playwright.config.ts` hardcodes `baseURL` to `http://localhost:8081`, which serves the built app from `compose.yaml`. Its `outputDir` is `test-results/`, which Playwright empties on each run.
- A frontend-only change has one pass (`openspec/config.yaml` tasks and apply rules), and each of its task groups lands its own "[<scenario ID>]" tests (the frontend-only exception in `rules.tasks`). Constitution step 7 speaks of backend tests only, and the "Section 1 is Failing tests" rule in `rules.tasks` says "one backend test per scenario (JUnit 5, Testcontainers, RestAssured)". In a change with both layers, archived `2026-11-02-login-invalid-credentials`, task 1.5, adds the Vitest test `[web-client/R5-S4]` to the one "Failing tests" section. How frontend tests fit pass 1 (constitution §2, no frontend stubs, coverage by `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`) is #170; this change adds no test rules (decision 3).
- Constitution §1 step 10 and the reviewers' triage: security problems, data loss and spec violations are fixed in the PR; every other finding becomes an issue. This change adds one sentence to step 10 for UI the change adds or edits (D7).

## Goals / Non-Goals

**Goals:**

- A frontend slice is built by a role that has read the design sources, and its result is checked on screenshots before review.
- Design questions reach the engineer before frontend code is written.
- One file of project rules settles every conflict between the sources and our own rules.
- The backend `implementer` and every other role see none of this.
- The design works in Claude Code, opencode and Codex; this change proves it in Claude Code only (D12).

**Non-Goals:**

- Enforcing the design rules by a script, beyond the existing `check-design-tokens.mjs`.
- Fixing existing UI that the change does not add or edit.
- Test rules for a change with both layers (#170).
- Keeping the vendored copies in sync with upstream automatically. The README records the pinned commits for a manual re-sync.
- Proving the role in opencode and Codex, and proving routing in a change with both layers (D12).

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

It comes first after the index and wins over every source. It holds:

- all colours, fonts, sizes, spacing, radii and shadows are tokens in `frontend/src/styles/tokens.css`; the `npm run lint` (`check-design-tokens.mjs`) rules apply; pages use `src/ui/` components;
- nothing loads from third-party CDNs (constitution §10);
- UI copy goes through i18n, sentence case, informal and friendly, like talking to a friend;
- buttons never touch fields, cards or each other; native parts such as the select arrow are inset like text; controls go exactly where the spec says;
- new UI built in a change meets these rules and WCAG 2.2 AA;
- the resolved conflicts:
  - **C0** precedence: spec, then project rules, then the sources;
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
  - **C13** no scores and no P0-P3. Findings at review are triaged as constitution §1 step 10 says, with this change's step 10 sentence (D7): security problems, data loss, spec violations, and design-rule and WCAG 2.2 AA violations in UI the change itself adds or edits are fixed in the PR. "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits; touching an existing component for an unrelated reason does not pull in its existing problems (#168, field border contrast, stays out unless the change edits that border). Design and UI problems in existing UI outside the change are listed to the engineer and filed as `area:frontend` issues only after the engineer agrees (D6); every other finding becomes an issue as step 10 says;
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
   - when the slice changes pages (`frontend/src/pages/`) or `src/ui/` components, take screenshots of each page it touches and each state the slice changes as D8 says, and check them against the sources before reporting;
   - list the screenshot paths in its report; the list does not count toward `implementer.md`'s 15-line report limit;
   - no new dependencies.

`implementer.md` stays as it is, including line 3 ("Use for all code writing in teachdash."). Routing relies on the frontend-implementer's description and the `AGENTS.md` routing (D4); the remaining line is a risk (see Risks). The body has no three single quotes.

Alternative: one implementer that reads the design sources when a slice touches `frontend/`. Rejected: the backend implementer would carry the rule, and the engineer decided it stays unchanged.

### D4. Routing and the tasks rule

- `AGENTS.md`, in "Main agent is an orchestrator":
  - before delegating, the main agent commits its edits: "the `implementer`" becomes "the `implementer`, the `frontend-implementer`";
  - the implementation bullet: implementation slices that touch `frontend/` go to `frontend-implementer`, others to `implementer`; the "Failing tests" section of a change with both layers stays with `implementer`;
  - the review bullet: "implementer report" becomes "implementer or frontend-implementer report".
- `openspec/config.yaml` `rules.tasks` gets one layer-split rule: implementation task groups (after the test review task, or every group in a frontend-only change) are split by layer; a group touches either code under `frontend/` or code under `backend/`, never both. Repository tooling (`scripts/`, for example adding a capability to `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`, and config files) may go with either group. In a change with both layers, backend implementation groups come before the frontend groups that call them, so screenshots show real states, not 501 stubs (proposal decision 3). The spec-writer follows it, so one group is one slice for one role.
- No test rules change: the "Section 1 is Failing tests" rule in `rules.tasks` and the "Pass 1 production code" apply guidance stay as they are, and the "Failing tests" section of a change with both layers stays one section written by the backend `implementer` (decision 3). The frontend-only exception in `rules.tasks` and the rest of the apply guidance stay unchanged. Frontend tests in pass 1 are #170.

### D5. Design angle

- Constitution §1 step 2 gets one sentence: "For a change to UI under `frontend/src`, the proposed angles always include a design angle (AGENTS.md)." Step 2 already lets the engineer agree that a small task needs no angle.
- `AGENTS.md`, "Before a proposal" and research bullets: for a change to UI under `frontend/src`, the main agent always proposes a design angle at Clarify; the engineer may drop it for a small task. At Explore only if Explore is run; Explore stays optional. A `researcher` reads `.agents/design/` and the screens involved and returns:
  - design questions: layout, states, copy, a11y, tokens and components;
  - existing-UI problems outside the change, separately.

  The main agent asks the engineer. The answers go into the proposal's Clarifications and `design.md`. The angle always covers the existing screens the change touches; problems outside the change's scope go through D6.
- Scopes, kept apart: the design angle is for UI under `frontend/src`; routing to `frontend-implementer` is for any slice touching `frontend/`; screenshots are for a slice that changes pages or `src/ui/` components.
- `researcher.md` gets one rule for the design angle: read `.agents/design/` (project rules first), and `critique-rubric.md` on demand; return the questions and the out-of-scope problems apart.

### D6. Out-of-scope design and UI problems in existing UI

`AGENTS.md`: the main agent lists design and UI problems in existing UI outside the change to the engineer and files each as a separate `area:frontend` issue only after the engineer agrees. They are never added to the current change. This applies only to those findings; every other finding follows constitution §1 step 10 unchanged. #168 is the first expected one; it is already filed and stays out of this change unless a change edits that border.

### D7. Review

- `reviewer.md`:
  - one Check bullet for slices that touch `frontend/`: read `.agents/design/` (project rules win), check the diff and the screenshots listed in the frontend-implementer's report; `critique-rubric.md` on demand;
  - `.agents/design/` joins the "no unrequested changes" list;
  - the triage line ("Mark each finding as one to fix in the PR (security problem, data loss, spec violation) or one for an issue") adds the own-UI case to "fix in the PR": design-rule and WCAG 2.2 AA violations in UI the change adds or edits.
- `code-gate-reviewer.md`:
  - one Check bullet: check frontend diffs against `.agents/design/project-rules.md`;
  - `.agents/design/` joins the "no unrequested changes" list;
  - the same triage line gets the same own-UI case.
- `AGENTS.md` review bullet: a frontend slice is also checked against `.agents/design/` and the screenshots.
- Constitution §1 step 10 gets one sentence, next to "Security problems, data loss and spec violations are fixed in the PR": design-rule (`.agents/design/project-rules.md`) and WCAG 2.2 AA violations in UI the change adds or edits (its markup, CSS, copy or behaviour) are fixed in the PR too. This sentence goes beyond issue #166's original "Done when"; the engineer decided to keep it in this change as the core of the requirement that new features come with good design (proposal decision 7).

Findings are triaged as constitution §1 step 10 says, with that sentence: security problems, data loss, spec violations, and design-rule and WCAG 2.2 AA violations in UI the change itself adds or edits are fixed in the PR. "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits (proposal decision 6); touching an existing component for an unrelated reason does not pull in its existing problems (#168 stays out unless the change edits that border). Design and UI problems in existing UI outside the change go through D6; every other finding becomes an issue as step 10 says. C13 says the same.

### D8. Screenshots

- Folder: `frontend/screenshots/<change-name>/`, cleared before each run, so a stale PNG cannot survive a failed run. `.gitignore` gets `frontend/screenshots/`. `test-results/` is not used, because Playwright empties it on each run.
- Widths: the desktop project's viewport and a 360 px viewport.
- Method:
  1. `docker compose up --build --wait`, so 8081 serves the slice's code;
  2. clear `frontend/screenshots/<change-name>/`;
  3. a temporary `frontend/e2e/zshots.spec.ts` that logs in with the `e2e/accounts.ts` helpers and captures each page the slice touches and each state the slice changes (for example the error shown after an empty submit, or a dialog open), at both widths, saving to that folder;
  4. `npx playwright test zshots --project=desktop-chromium`;
  5. delete the spec before committing.
- `docker` and `npx` are in no allow list, so each call prompts the engineer.
- The report lists the screenshot paths. The engineer attaches images to the PR by hand if wanted; `gh` cannot upload images. For this change's dry run, screenshots attached by hand are the proof (D12).
- No script or Playwright project is added. The method is plain shell and Playwright, meant for all three tools, but only proven in Claude Code (D12).

### D9. Test phases

No change to test phases; see #170.

### D10. Licences and attribution

- Each upstream licence file is copied verbatim to `.agents/design/licenses/<owner>-<repo>.LICENSE`. For anthropics/skills, which has no root LICENSE, the source is `skills/frontend-design/LICENSE.txt` @e4bb5d9.
- Each vendored file starts with one header line: source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/`, and "modified by teachdash". This states the changes as Apache-2.0 §4(b) requires; MIT needs the licence text, which `licenses/` holds.
- impeccable has a `NOTICE.md`. Its notices cover none of what goes into `critique-rubric.md` (they are about iOS and Android references, fonts and consent rules). Apache-2.0 §4(d) excludes notices that do not pertain to any part of the derivative work, so no copy is included. The README records this reason.

### D11. Approval

The role files, `openspec/config.yaml` and the constitution (§1 steps 2 and 10) need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md` and `.gitignore` do not.

So any engineer can change the design angle's content in `AGENTS.md` and the rules in `.agents/design/`, including `project-rules.md`, without the owner (constitution §1, Roles), even though step 10 now makes violations of `project-rules.md` in the change's own UI fix-in-PR, so such an edit changes what a gate must fix. The engineer accepted this (proposal decision 11): every PR still needs approval from a code owner other than the author, and the reviewers flag unrequested changes to `.agents/design/`.

### D12. Dry run and tool support

- The dry run is in Claude Code, after `scripts/setup-agents` and a restart. #165 (ui-required-fields) goes through Clarify and Research up to the design questions; no spec deltas are written for #165. Then one slice is built on a throwaway branch off this one, made with `git switch -c <throwaway name, for example dry-run-frontend-implementer> workflow-frontend-implementer`. `scripts/flow/change-start` is not used: it branches from `origin/main` and would take the real #165 branch name. On that branch a scratch change is created with `npx -y @fission-ai/openspec@1.15.0 new change ui-required-fields`, never by hand, so the slice has proposal Clarifications for the design answers, a minimal `tasks.md` for the slice, and a name for `frontend/screenshots/ui-required-fields/`.
- The proof is screenshots only (proposal decision 10): the engineer attaches screenshots to PR #169 by hand, showing how the new flow works (for example the design questions asked before code, the frontend-implementer's work and the reviewer's work) and what result it gives (the UI). No throwaway-branch SHA, no pushed branch and no text logs are required. The same screenshots prove the `reviewer.md` and `researcher.md` edits. The throwaway branch is not pushed and is deleted right after the run, with the scratch change and the code; no #165 code survives.
- opencode and Codex are unverified. The Codex sandbox may block `docker` or Chromium, and whether opencode and Codex can show a PNG to the reviewer is unchecked.
- Routing in a change with both layers (implementation groups split by layer, both roles in one change) is unproven: #165 is frontend-only, so the dry run does not exercise it.

## Risks / Trade-offs

- **Stale screenshots** if `--build` is skipped: 8081 serves the old build. → D8 step 1, and the role says to rebuild.
- **A stale PNG from an earlier run** passes as new. → The change's folder is cleared before each run (D8).
- **Lost screenshots** in `test-results/`, which Playwright empties. → They go to `frontend/screenshots/<change-name>/`.
- **The temporary spec gets committed.** → The role says to delete it, and the reviewer sees any `zshots` file in the diff.
- **A frontend slice goes to `implementer`**, because `implementer.md:3` still says "Use for all code writing in teachdash." → The frontend-implementer's description says "Use instead of implementer for any slice touching frontend/", and `AGENTS.md` routes such slices to it.
- **Frontend tests in pass 1 of a change with both layers stay unclear** (constitution §2 and step 7 against practice). → Out of scope, filed as #170; this change adds no test rules (D4, D9).
- **Routing in a change with both layers is unproven** (the dry run on #165 is frontend-only). → Marked unproven (D12); the first change with both layers shows it.
- **Where "UI the change adds or edits" ends** may be argued at review. → It is defined as markup, CSS, copy (i18n strings) or behaviour (handlers, focus, states) that the change adds or edits (D7, C13).
- **opencode and Codex may not run the screenshot method** (sandbox, PNG display). → Marked unverified (D12); the first frontend change in those tools shows it.
- **Each `docker` and `npx` call prompts.** → Accepted; no permission change.
- **Leftover network-tool or Tailwind lines** in a copy, which an agent might follow. → Task 1.7 greps for them.
- **impeccable build output copied** instead of `skill/`. → D1 names the path; task 1.5 checks it.
- **A missing "modified by teachdash" header** breaks Apache-2.0 §4(b). → Task 1.6 checks every file.
- **#168 will be the first design finding** in the dry run. → It is filed already and stays out of this change unless a change edits that border.
- **More reading per frontend slice**, about 3k tokens always. → The rest is read on demand.
- **The rules are instructions, not checks, and any engineer may change `project-rules.md` without the owner**, although step 10 makes its violations in the change's own UI fix-in-PR. → Accepted (D11): every PR still needs approval from a code owner other than the author, and the reviewers guard `.agents/design/` against unrequested changes.

## Open Questions

None.
