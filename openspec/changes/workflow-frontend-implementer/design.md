# Design

## Context

See `proposal.md` for the decisions.

- `scripts/setup-agents` builds every role from `.agents/roles/*.md`. `body()` copies the role body verbatim into Claude Code, opencode and Codex. Write roles get no tool limits. A body must not contain three single quotes (it breaks the Codex TOML string). `validate_roles` treats every `*.md` in `.agents/roles/` as a role, so a shared include cannot live there. `--check` hashes only `.agents/roles` and `.agents/skills`.
- The permission files (`.claude/settings.json`, `opencode.json`, `.codex/rules/`) do not list `docker` or `npx`. The new role needs no permission change.
- Frontend look rules today: all values are tokens in `frontend/src/styles/tokens.css`; `npm run lint` runs `scripts/check-design-tokens.mjs`, which fails on look values outside that file; pages use `src/ui/` components. UI copy tone and spacing rules are recorded only in archived changes.
- `frontend/playwright.config.ts` hardcodes `baseURL` to `http://localhost:8081`, which serves the built app from `compose.yaml`. Its `outputDir` is `test-results/`, which Playwright empties on each run.
- A frontend-only change has one pass (`openspec/config.yaml` tasks and apply rules). Constitution step 7 speaks of backend tests only.

## Goals / Non-Goals

**Goals:**

- A frontend slice is built by a role that has read the design sources, and its result is checked on screenshots before review.
- Design questions reach the engineer before frontend code is written.
- One file of project rules settles every conflict between the sources and our own rules.
- The backend `implementer` and every other role see none of this.

**Non-Goals:**

- Enforcing the design rules by a script, beyond the existing `check-design-tokens.mjs`.
- Fixing existing UI.
- Keeping the vendored copies in sync with upstream automatically. The README records the pinned commits for a manual re-sync.

## Decisions

### D1. `.agents/design/` layout and sources

| File | Source @ commit | Licence | About |
|---|---|---|---|
| `README.md` | ours | - | Index, ~35 lines: reading order, precedence, a source/path/commit table for re-syncs |
| `project-rules.md` | ours | - | ~80 lines, D2 |
| `checklist.md` | vercel-labs/web-interface-guidelines @434b7f9 `command.md` | MIT | ~130 lines, plus the three impeccable audit points |
| `accessibility.md` | addyosmani/web-quality-skills @afa8da9 `skills/accessibility/SKILL.md` | MIT | ~250 lines |
| `accessibility-wcag22.md` | same, `references/WCAG.md` | MIT | ~100 lines |
| `accessibility-patterns.md` | same, `references/A11Y-PATTERNS.md` | MIT | ~120 lines |
| `refactoring-ui.md` | wondelai/skills @c526558 `refactoring-ui/SKILL.md` | MIT | ~150 lines |
| `refactoring-ui-patterns.md` | same, `refactoring-ui/references/advanced-patterns.md` | MIT | ~250 lines |
| `frontend-design-copy.md` | anthropics/skills @e4bb5d9 `skills/frontend-design/SKILL.md` | Apache-2.0 | ~35 lines |
| `critique-rubric.md` | pbakaus/impeccable @b49177d `skill/reference/critique-rubric.md` | Apache-2.0 | ~200 lines |
| `licenses/<owner>-<repo>.LICENSE` x5 | upstream licence files | - | verbatim |

Total about 1,350 lines (about 14k tokens). Every slice reads `README.md`, `project-rules.md` and `checklist.md` (about 3k tokens); `accessibility.md` for new UI; the rest on demand.

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
- **anthropics/skills** (Apache-2.0): keep only the line length (maps to `--content-width`), restraint, quality floor, screenshots and copy sections. Drop the frontmatter, aesthetic risk, client memory, hero, typefaces, calibration and the hex palette plan. Tone per C10.
- **pbakaus/impeccable** (Apache-2.0, "Copyright 2025 Paul Bakaus"): take the template from `skill/`, not the build output (it has `{{...}}` placeholders). Keep the cognitive-load checklist, the "check for" lists of the Nielsen heuristics and the persona walk-throughs. Drop the `critique.md` link, `PRODUCT.md` and `impeccable init`, the landing and e-commerce personas and the numeric scoring. "44x44pt" becomes the AA floor of 24 CSS px.

Alternative: `.agents/skills/`. Rejected: every tool would list the sources to every role, including the main agent and the backend implementer, and `setup-agents` would regenerate on each edit.

### D2. `project-rules.md`

It comes first in the reading order and wins over every source. It holds:

- all colours, fonts, sizes, spacing, radii and shadows are tokens in `frontend/src/styles/tokens.css`; the `npm run lint` (`check-design-tokens.mjs`) rules apply; pages use `src/ui/` components;
- nothing loads from third-party CDNs (constitution §10);
- UI copy goes through i18n, sentence case, informal and friendly, like talking to a friend;
- buttons never touch fields, cards or each other; native parts such as the select arrow are inset like text; controls go exactly where the spec says;
- WCAG 2.2 AA;
- the resolved conflicts:
  - **C0** precedence: spec, then project rules, then the sources;
  - **C1** sentence case, not Title Case;
  - **C2** no all-caps or uppercase-small labels;
  - **C3** tokens only; a new value becomes a role-named token in `tokens.css`;
  - **C4** `--font-body` system stack, no web fonts, no "distinctive typeface";
  - **C5** the one `--radius` and `--shadow-card` are ours; changing them is an engineer decision;
  - **C6** reduced motion removes non-essential motion but keeps state feedback;
  - **C7** native elements first; never add key handlers to a `<button>`;
  - **C8** personal-data fields get autocomplete tokens (WCAG 1.3.5), not `autocomplete="off"`;
  - **C9** touch target floor 24 CSS px (AA); controls use `--control-height`;
  - **C10** friendly, second person, no apologies; a message says what happened and how to fix it;
  - **C11** quotes follow each language's typography in the i18n files (ru uses «»);
  - **C12** focus on error follows the specs (web-client R14, R17);
  - **C13** no scores and no P0-P3: a finding is fixed in the PR if it breaks a spec, WCAG 2.2 AA or the project rules, otherwise it becomes an `area:frontend` issue (constitution §1 step 10);
  - **C14** empty states are text plus a `src/ui/` action, no image assets.

### D3. `frontend-implementer` role

`.agents/roles/frontend-implementer.md`: class `medium`, access `write`, web `true`. The body is short, because `setup-agents` copies it verbatim and cannot include another file:

1. Read `.agents/roles/implementer.md` and follow it.
2. Read `.agents/design/`: `README.md`, `project-rules.md` first, then `checklist.md`; `accessibility.md` for new UI; the rest on demand.
3. One line: where `implementer.md` and this file differ, this file wins.
4. Additions:
   - build new UI to the design sources;
   - it may edit `tokens.css` inside its slice; new values become role-named tokens;
   - take screenshots of the pages its slice touches as D8 says, and check them against the sources before reporting;
   - list the screenshot paths in its report;
   - no new dependencies.

`implementer.md` stays as it is. The body has no three single quotes.

Alternative: one implementer that reads the design sources when a slice touches `frontend/`. Rejected: the backend implementer would carry the rule, and the engineer decided it stays unchanged.

### D4. Routing and the tasks rule

- `AGENTS.md`, in "Main agent is an orchestrator":
  - before delegating, the main agent commits its edits: "the `implementer`" becomes "the `implementer`, the `frontend-implementer`";
  - the implementation bullet: slices that touch `frontend/` go to `frontend-implementer`, others to `implementer`;
  - the review bullet: "implementer report" becomes "implementer or frontend-implementer report".
- `openspec/config.yaml` `rules.tasks` gets one rule: task groups are split by layer; a group touches either `frontend/` or the rest, never both. The spec-writer follows it, so one group is one slice for one role.

### D5. Design angle

- Constitution §1 step 2 gets one sentence: "A change that touches the frontend always has a design angle (AGENTS.md)."
- `AGENTS.md`, research bullets: for every change that touches the frontend, explore, research and propose include a design angle. A `researcher` reads `.agents/design/` and the screens involved and returns:
  - design questions: layout, states, copy, a11y, tokens and components;
  - existing-UI problems outside the change, separately.

  The main agent asks the engineer. The answers go into the proposal's Clarifications and `design.md`. From the next frontend change on, the angle also covers the existing screens the change touches.
- `researcher.md` gets one rule for the design angle: read `.agents/design/` (project rules first), and `critique-rubric.md` on demand; return the questions and the out-of-scope problems apart.

### D6. Out-of-scope problems in existing UI

`AGENTS.md`: the main agent lists them to the engineer and files each as a separate `area:frontend` issue only after the engineer agrees. They are never added to the current change. #168 is the first expected one, and it is already filed.

### D7. Review

- `reviewer.md`:
  - one Check bullet for slices that touch `frontend/`: read `.agents/design/` (project rules win), check the diff and the screenshots listed in the implementer's report; `critique-rubric.md` on demand;
  - `.agents/design/` joins the "no unrequested changes" list.
- `code-gate-reviewer.md`:
  - one Check bullet: check frontend diffs against `.agents/design/project-rules.md`;
  - `.agents/design/` joins the "no unrequested changes" list.
- `AGENTS.md` review bullet: a frontend slice is also checked against `.agents/design/` and the screenshots.

Findings are sorted as the reviewers do today (fix in the PR, or an issue), which C13 matches.

### D8. Screenshots

- `.gitignore` gets `frontend/screenshots/`. `test-results/` is not used, because Playwright empties it on each run.
- Widths: the desktop project's viewport and a 360 px viewport.
- Method:
  1. `docker compose up --build --wait`, so 8081 serves the slice's code;
  2. a temporary `frontend/e2e/zshots.spec.ts` that logs in with the `e2e/accounts.ts` helpers, opens each touched page at both widths and saves to `frontend/screenshots/`;
  3. `npx playwright test zshots --project=desktop-chromium`;
  4. delete the spec before committing.
- The method is plain shell and Playwright, so it works the same in all three tools. No script or Playwright project is added.

### D9. Constitution step 7

Step 7 gets a note: a change's frontend tests land with the code in pass 2, as for frontend-only changes today (§2), including changes that have both layers. Pass 1 stays backend tests only.

### D10. Licences and attribution

- Each upstream licence file is copied verbatim to `.agents/design/licenses/<owner>-<repo>.LICENSE`.
- Each vendored file starts with one header line: source, path, pinned commit, licence, "modified by teachdash". This states the changes as Apache-2.0 §4(b) requires; MIT needs the licence text, which `licenses/` holds.
- No NOTICE files are needed (proposal, decision 9).

### D11. Approval

The role files, `openspec/config.yaml` and the constitution need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md` and `.gitignore` do not.

## Risks / Trade-offs

- **Stale screenshots** if `--build` is skipped: 8081 serves the old build. → D8 step 1, and the role says to rebuild.
- **Lost screenshots** in `test-results/`, which Playwright empties. → They go to `frontend/screenshots/`.
- **The temporary spec gets committed.** → The role says to delete it, and the reviewer sees any `zshots` file in the diff.
- **Leftover network-tool or Tailwind lines** in a copy, which an agent might follow. → Task 1.7 greps for them.
- **impeccable build output copied** instead of `skill/`. → D1 names the path; task 1.5 checks it.
- **A missing "modified by teachdash" header** breaks Apache-2.0 §4(b). → Task 1.6 checks every file.
- **#168 will be the first design finding** in the dry run. → It is filed already and stays out of this change.
- **More reading per frontend slice**, about 3k tokens always. → The rest is read on demand.
- **The rules are instructions, not checks.** → The reviewers guard `.agents/design/` against unrequested changes.

## Open Questions

- Whether the dry-run branch for #165 is kept as the start of that change or deleted. The engineer decides after the run; it does not change this change.
