# Design

## Context

See `proposal.md` for the decisions.

- `scripts/setup-agents` builds every role from `.agents/roles/*.md`. `body()` copies the role body verbatim into Claude Code, opencode and Codex. Write roles get no tool limits. A body must not contain three single quotes (it breaks the Codex TOML string). `validate_roles` treats every `*.md` in `.agents/roles/` as a role, so a shared include cannot live there. `--check` hashes only `.agents/roles` and `.agents/skills`.
- The permission files (`.claude/settings.json`, `opencode.json`, `.codex/rules/`) do not list `docker` or `npx`, so each such call prompts the engineer. The new role gets no permission change.
- `.agents/roles/implementer.md:3` describes the role as "Use for all code writing in teachdash."
- Frontend look rules today: all values are tokens in `frontend/src/styles/tokens.css`; `npm run lint` runs `scripts/check-design-tokens.mjs`, which fails on look values outside that file; pages live in `frontend/src/pages/` and use `src/ui/` components. UI copy tone and spacing rules are recorded only in archived changes.
- `frontend/playwright.config.ts` hardcodes `baseURL` to `http://localhost:8081`, which serves the built app from `compose.yaml`. Its `outputDir` is `test-results/`, which Playwright empties on each run.
- A frontend-only change has one pass (`openspec/config.yaml` tasks and apply rules). Constitution step 7 speaks of backend tests only, and `openspec/config.yaml` line 27 says "one backend test per scenario (JUnit 5, Testcontainers, RestAssured)". Yet a change with both layers already writes its frontend tests in pass 1: archived `2026-11-02-login-invalid-credentials`, task 1.5 adds the Vitest test `[web-client/R5-S4]` under "Failing tests". `scripts/check-scenario-coverage.sh` already covers a frontend capability's scenario only by a Vitest or Playwright title.
- Constitution §1 step 10 and the reviewers' triage: security problems, data loss and spec violations are fixed in the PR; every other finding becomes an issue.

## Goals / Non-Goals

**Goals:**

- A frontend slice is built by a role that has read the design sources, and its result is checked on screenshots before review.
- Design questions reach the engineer before frontend code is written.
- One file of project rules settles every conflict between the sources and our own rules.
- The backend `implementer` and every other role see none of this.
- The design works in Claude Code, opencode and Codex; this change proves it in Claude Code only (D12).

**Non-Goals:**

- Enforcing the design rules by a script, beyond the existing `check-design-tokens.mjs`.
- Fixing existing UI.
- Keeping the vendored copies in sync with upstream automatically. The README records the pinned commits for a manual re-sync.
- Proving the role in opencode and Codex.

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
  - **C5** the one `--radius` and `--shadow-card` are ours; changing them, or any existing token's value, is an engineer decision;
  - **C6** reduced motion removes non-essential motion but keeps state feedback;
  - **C7** native elements first; never add key handlers to a `<button>`;
  - **C8** personal-data fields get autocomplete tokens (WCAG 1.3.5), not `autocomplete="off"`;
  - **C9** touch target floor 24 CSS px (AA); controls use `--control-height`;
  - **C10** friendly, second person, no apologies; a message says what happened and how to fix it;
  - **C11** quotes follow each language's typography in the i18n files (ru uses «»);
  - **C12** focus on error follows the specs (web-client R14, R17);
  - **C13** no scores and no P0-P3. Findings at review are triaged as constitution §1 step 10 says: security problems, data loss and spec violations are fixed in the PR; every other finding, including WCAG 2.2 AA and project-rule findings in existing code, is listed to the engineer and filed as an `area:frontend` issue only after the engineer agrees (D6). That new UI meets the project rules and WCAG 2.2 AA is how the frontend-implementer builds (D3), not a triage rule;
  - **C14** empty states are text plus a `src/ui/` action, no image assets.

### D3. `frontend-implementer` role

`.agents/roles/frontend-implementer.md` frontmatter: `name: frontend-implementer`, class `medium`, access `write`, web `true`, and

> description: Implements one bounded slice that touches frontend/, building the UI to the design sources in .agents/design/. Use instead of implementer for any slice touching frontend/.

The body is short, because `setup-agents` copies it verbatim and cannot include another file:

1. Read `.agents/roles/implementer.md` and follow it.
2. Read `.agents/design/` in this order: `README.md`, then `project-rules.md`, then `checklist.md`; `accessibility.md` when the slice adds or changes interactive elements, forms or dialogs; the rest on demand.
3. One line: where `implementer.md` and this file differ, this file wins.
4. Passes: in pass 1 it writes its group's frontend failing tests (Vitest or Playwright, "[<scenario ID>]" at the start of the title). In pass 2, or in a frontend-only change's single pass, it writes the code (and, for a frontend-only change, the group's "[<scenario ID>]" tests).
5. Additions:
   - build new UI to the design sources; new UI meets the project rules and WCAG 2.2 AA;
   - it may add new role-named tokens to `tokens.css` inside its slice; changing an existing token's value is an engineer decision (C5), so it stops and reports instead;
   - when the slice changes pages (`frontend/src/pages/`) or `src/ui/` components, take screenshots of the pages it touches as D8 says, and check them against the sources before reporting;
   - list the screenshot paths in its report; the list does not count toward `implementer.md`'s 15-line report limit;
   - no new dependencies.

`implementer.md` stays as it is, including line 3 ("Use for all code writing in teachdash."). Routing relies on the frontend-implementer's description and the `AGENTS.md` routing (D4); the remaining line is a risk (see Risks). The body has no three single quotes.

Alternative: one implementer that reads the design sources when a slice touches `frontend/`. Rejected: the backend implementer would carry the rule, and the engineer decided it stays unchanged.

### D4. Routing and the tasks rule

- `AGENTS.md`, in "Main agent is an orchestrator":
  - before delegating, the main agent commits its edits: "the `implementer`" becomes "the `implementer`, the `frontend-implementer`";
  - the implementation bullet: slices that touch `frontend/` go to `frontend-implementer`, others to `implementer`; in pass 1 of a change with both layers, the frontend group of failing tests goes to `frontend-implementer`;
  - the review bullet: "implementer report" becomes "implementer or frontend-implementer report".
- `openspec/config.yaml` `rules.tasks` gets one layer-split rule: task groups are split by layer; a group touches either `frontend/` or the rest, never both. In a change with both layers, "Failing tests" has a backend group and a frontend group (Vitest or Playwright tests with "[<scenario ID>]" at the start of the title); both are written in pass 1 and reviewed at the test review gate. The spec-writer follows it, so one group is one slice for one role.
- Line 27 check: line 27 says Section 1 is "Failing tests - one backend test per scenario (JUnit 5, Testcontainers, RestAssured)". Read literally, it asks for a backend test for every scenario, including a frontend capability's scenario, which `check-scenario-coverage.sh` covers only by a Vitest or Playwright title. The new rule's frontend group would contradict it, so line 27 needs a minimal amendment: "one backend test per scenario (JUnit 5, Testcontainers, RestAssured)" becomes "one test per scenario: a backend test (JUnit 5, Testcontainers, RestAssured) for a backend scenario, a Vitest or Playwright test for a frontend scenario". The rest of line 27 stays. It needs the owner's approval with the rest of `config.yaml` (D11). The frontend-only exception (line 31) and the apply guidance stay unchanged.

### D5. Design angle

- Constitution §1 step 2 gets one sentence: "For a change to UI under `frontend/src`, the proposed angles always include a design angle (AGENTS.md)." Step 2 already lets the engineer agree that a small task needs no angle.
- `AGENTS.md`, "Before a proposal" and research bullets: for a change to UI under `frontend/src`, the main agent always proposes a design angle at Clarify; the engineer may drop it for a small task. At Explore only if Explore is run; Explore stays optional. A `researcher` reads `.agents/design/` and the screens involved and returns:
  - design questions: layout, states, copy, a11y, tokens and components;
  - existing-UI problems outside the change, separately.

  The main agent asks the engineer. The answers go into the proposal's Clarifications and `design.md`. The angle always covers the existing screens the change touches; problems outside the change's scope go through D6.
- Scopes, kept apart: the design angle is for UI under `frontend/src`; routing to `frontend-implementer` is for any slice touching `frontend/`; screenshots are for a slice that changes pages or `src/ui/` components.
- `researcher.md` gets one rule for the design angle: read `.agents/design/` (project rules first), and `critique-rubric.md` on demand; return the questions and the out-of-scope problems apart.

### D6. Out-of-scope problems in existing UI

`AGENTS.md`: the main agent lists them to the engineer and files each as a separate `area:frontend` issue only after the engineer agrees. They are never added to the current change. #168 is the first expected one; it is already filed and stays out of this change.

### D7. Review

- `reviewer.md`:
  - one Check bullet for slices that touch `frontend/`: read `.agents/design/` (project rules win), check the diff and the screenshots listed in the frontend-implementer's report; `critique-rubric.md` on demand;
  - `.agents/design/` joins the "no unrequested changes" list.
- `code-gate-reviewer.md`:
  - one Check bullet: check frontend diffs against `.agents/design/project-rules.md`;
  - `.agents/design/` joins the "no unrequested changes" list.
- `AGENTS.md` review bullet: a frontend slice is also checked against `.agents/design/` and the screenshots.

Findings are triaged as constitution §1 step 10 and the reviewers' current rule say: security problems, data loss and spec violations are fixed in the PR; everything else is listed to the engineer and filed as an `area:frontend` issue only after the engineer agrees (D6). C13 says the same.

### D8. Screenshots

- Folder: `frontend/screenshots/<change-name>/`, cleared before each run, so a stale PNG cannot survive a failed run. `.gitignore` gets `frontend/screenshots/`. `test-results/` is not used, because Playwright empties it on each run.
- Widths: the desktop project's viewport and a 360 px viewport.
- Method:
  1. `docker compose up --build --wait`, so 8081 serves the slice's code;
  2. clear `frontend/screenshots/<change-name>/`;
  3. a temporary `frontend/e2e/zshots.spec.ts` that logs in with the `e2e/accounts.ts` helpers, opens each touched page at both widths and saves to that folder;
  4. `npx playwright test zshots --project=desktop-chromium`;
  5. delete the spec before committing.
- `docker` and `npx` are in no allow list, so each call prompts the engineer.
- The report lists the screenshot paths. The engineer attaches images to the PR by hand if wanted; `gh` cannot upload images.
- No script or Playwright project is added. The method is plain shell and Playwright, meant for all three tools, but only proven in Claude Code (D12).

### D9. Pass 1 in a change with both layers

Constitution step 7 is not edited and `scripts/check-scenario-coverage.sh` is not changed. Frontend tests are written in pass 1 and go through the test review gate, as today (archived `2026-11-02-login-invalid-credentials`, task 1.5). The "Failing tests" section is split into a backend group (`implementer`) and a frontend group (`frontend-implementer`), as the layer-split rule says (D4). The frontend-only exception (one pass, constitution §2) stays.

### D10. Licences and attribution

- Each upstream licence file is copied verbatim to `.agents/design/licenses/<owner>-<repo>.LICENSE`. For anthropics/skills, which has no root LICENSE, the source is `skills/frontend-design/LICENSE.txt` @e4bb5d9.
- Each vendored file starts with one header line: source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/`, and "modified by teachdash". This states the changes as Apache-2.0 §4(b) requires; MIT needs the licence text, which `licenses/` holds.
- impeccable has a `NOTICE.md`. Its notices cover none of what goes into `critique-rubric.md` (they are about iOS and Android references, fonts and consent rules). Apache-2.0 §4(d) excludes notices that do not pertain to any part of the derivative work, so no copy is included. The README records this reason.

### D11. Approval

The role files, `openspec/config.yaml` and the constitution need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md` and `.gitignore` do not.

So any engineer can change the design angle's content in `AGENTS.md` and the rules in `.agents/design/` without the owner (constitution §1, Roles). This is accepted: the gate triage is set by constitution §1 step 10, which C13 only points to, and the reviewers flag unrequested changes to `.agents/design/`.

### D12. Dry run and tool support

- The dry run is in Claude Code, after `scripts/setup-agents` and a restart. #165 (ui-required-fields) goes through Clarify and Research up to the design questions; no spec is written for #165. Then one slice is built on a throwaway branch off this one. The branch's last SHA is recorded in the PR, then the branch is deleted; no #165 code survives. The run proves the `reviewer.md` and `researcher.md` edits.
- opencode and Codex are unverified. The Codex sandbox may block `docker` or Chromium, and whether opencode and Codex can show a PNG to the reviewer is unchecked.

## Risks / Trade-offs

- **Stale screenshots** if `--build` is skipped: 8081 serves the old build. → D8 step 1, and the role says to rebuild.
- **A stale PNG from an earlier run** passes as new. → The change's folder is cleared before each run (D8).
- **Lost screenshots** in `test-results/`, which Playwright empties. → They go to `frontend/screenshots/<change-name>/`.
- **The temporary spec gets committed.** → The role says to delete it, and the reviewer sees any `zshots` file in the diff.
- **A frontend slice goes to `implementer`**, because `implementer.md:3` still says "Use for all code writing in teachdash." → The frontend-implementer's description says "Use instead of implementer for any slice touching frontend/", and `AGENTS.md` routes such slices to it.
- **Constitution step 7 still says backend tests** while a change with both layers has a frontend group in pass 1. → The practice already exists (login-invalid-credentials), and `config.yaml` states it (D4, D9).
- **opencode and Codex may not run the screenshot method** (sandbox, PNG display). → Marked unverified (D12); the first frontend change in those tools shows it.
- **Each `docker` and `npx` call prompts.** → Accepted; no permission change.
- **Leftover network-tool or Tailwind lines** in a copy, which an agent might follow. → Task 1.7 greps for them.
- **impeccable build output copied** instead of `skill/`. → D1 names the path; task 1.5 checks it.
- **A missing "modified by teachdash" header** breaks Apache-2.0 §4(b). → Task 1.6 checks every file.
- **#168 will be the first design finding** in the dry run. → It is filed already and stays out of this change.
- **More reading per frontend slice**, about 3k tokens always. → The rest is read on demand.
- **The rules are instructions, not checks, and any engineer may change them.** → The reviewers guard `.agents/design/` against unrequested changes; gate triage stays in the constitution (D11).

## Open Questions

None.
