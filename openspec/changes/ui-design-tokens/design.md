# Design

## Context

`frontend/src/styles/tokens.css` holds the pre-redesign tokens (blue accent, 0.5rem radius, `--space-1` to `--space-4`, `--shadow-card`, `--color-border`, the green `--color-action-success-*`, `--arrow-width`). The design code (`.agents/design/design-code.md`, "Tokens" and "Rename map from today's tokens") defines the new tables and says the rename is done in one PR with no aliases. Redesign plan step 2 (#196) does it for `tokens.css`, `base.css` and all `src/ui` CSS, folds #168 and removes the Button `success` variant, the card shadows and the hand-drawn select chevron.

## Goals / Non-Goals

- Goals: `tokens.css` equal to the design-code tables; every old name mapped by role; the lint and tests that stop a leftover or undefined name; control borders at 3:1 (#168).
- Non-goals: hover, active and focus rules, field and button states (3a); badge, banner and Dialog restyle (3b); layout components (4a, 4b, 5a, 5b); `text-wrap`, `tabular-nums`, `--text-small` use (Clarification 6).

## Decisions

### D1 One slice for the rename

Success tokens, `--space-1` to `--space-4`, `--color-border`, `--shadow-card` and `--arrow-width` leave `tokens.css` in the same slice as their last use. With no aliases, the lint (D4) fails on any leftover, so a half-done rename cannot be committed green. The slices are in `tasks.md`.

### D2 Token tables

`tokens.css` holds exactly the tables of the design code: colour, type, space, shape and motion, controls and icons, layout. This includes tokens nothing uses yet (listed in the UI design section), so later steps add no token. The `@media (pointer: fine)` override of `--control-height` and `--row-action-size`, and the `:root:lang(ru)` override of `--type-label-width`, are in `tokens.css`. Values that use `cqi` or `var()` are copied as written. `base.css` keeps reading tokens (body font, size, line height, colours); it needs no new rule, and R25 covers its references.

### D3 Rename map applied by role

Colour: `--color-border` splits (control edges to `--color-control-border`, edges to `--color-divider`); `--color-action-success-*` are removed; `Button.danger` fill uses `--color-danger-action`; the field error border keeps `--color-danger-text`.

| Use | Token |
|---|---|
| `Field.module.css:51` select arrow box right | `calc(var(--border-width) + var(--space-group))` |
| `Field.module.css:44` select `padding-right` | `calc(var(--space-group) + var(--icon-size) + var(--space-inline))` |
| `Field.module.css:13`, `StatusBadge:5`, `Button:10`, `AppHeader:14`, `IssuedLink:14` raw `font-weight: 600` | `var(--weight-strong)` |
| `Field:30,85`, `Button:24`, `IssuedLink:26` border | `--color-control-border` |
| `List:13`, `IssuedLink:8`, `Dialog:7` border | `--color-divider` |
| `List:15`, `StatusMessage:5`, `AppHeader:9`, `IssuedLink:10` `box-shadow` | removed; `AppHeader` gets `border-bottom` of `--border-width` solid `--color-divider` |
| `Button:29-30` danger fill and border | `--color-danger-action` |
| `Button:33-41` `.success` rules, `Button.tsx:17` variant | removed; `AdminTeachersPage.tsx:299,460` use the default (primary) |
| `IssuedLink:23` input fill | `--color-surface-sunken` |
| `Title:3,9` | `--text-title` at every width, the `40rem` step removed, plus `--weight-strong` |
| `Section:7-8`, `Dialog:13-14` | `--text-large`, `--line-height-large`, plus `--weight-strong` |

Space, each use by role (file:line, old to new):

| Use | New |
|---|---|
| `Field:4` gap label to control (`--space-1`) | `--space-adjacent` |
| `Field:5` margin below a field (`--space-2`) | `--space-block` |
| `Field:18` input row gap (`--space-1`) | `--space-button-gap` |
| `Field:25` input padding (`--space-1`) | `--space-group` |
| `Field:80` password toggle padding (`--space-2`) | `--space-group` |
| `StatusMessage:3` padding | `--space-group` |
| `ButtonRow:5` margin-top | `--space-block` |
| `Section:2` margin-top | `--space-section` |
| `Section:6` heading margin-bottom | `--space-group` (Clarification 13) |
| `Button:5` icon and label gap | `--space-inline` |
| `Button:7` padding | `0 var(--space-group)` |
| `StatusBadge:3` padding | `--space-inline` |
| `AppHeader:6` gap | `var(--space-inline) var(--space-group)` |
| `AppHeader:7` padding (two-axis, vertical then horizontal) | `var(--space-inline) var(--space-group)` (Clarification 16) |
| `Page:4` padding (narrow) | `var(--space-block) var(--space-group)` |
| `Page:9` padding (from 40rem) | `var(--space-page)` |
| `List:4` gap between rows | `--space-group` (Clarification 14) |
| `List:5` margin-bottom | `--space-block` (Clarification 14) |
| `List:11` item padding | `--space-group` |
| `List:23` head gap | `--space-inline` |
| `List:36` badges gap | `--space-inline` |
| `List:41` details margin-top | `--space-adjacent` |
| `List:46` extra margin-top | `--space-group` |
| `List .details` (a column of detail lines, each its own `Text`) | `display: flex; flex-direction: column; gap: var(--space-adjacent)`, and `.item .details > * { margin: 0; }` resets the children's margins (Clarification 18; scoped by `.item` so it outranks `Text.module.css`, which loads later at equal specificity); the buttons keep their ButtonRow spacing |
| `Page .page > [role='status']` (new rule, kept from 7bec8f3) | `margin-bottom: var(--space-block)`: space below every StatusMessage placed directly in `Page` (visible on login session-ended and unreachable, admin action-error, cabinet and profile logout-error; invisible where the banner is the last child); follows `.form > [role='status']` (Clarification 17) |
| `ErrorWithRetry:5` gap | `--space-group` |
| `Text:2` margin-bottom | `--space-block` |
| `Form:2` margin-bottom | `--space-block` (Clarification 14) |
| `Form:6` status margin-bottom | `--space-block` |
| `Dialog:2` width margin (`2 *`) | `--space-group` |
| `Dialog:4` padding | `--space-block` |
| `Dialog:12` title margin-bottom | `--space-group` (Clarification 13) |
| `Toolbar:4` padding | `var(--space-group) var(--space-group) 0` |
| `IssuedLink:4` gap | `--space-inline` (Clarification 15) |
| `IssuedLink:6` padding | `--space-group` |
| `IssuedLink:21` input padding | `--space-group` |
| `IssuedLink:37` margin-bottom | `--space-block` |
| `Title:2` margin-bottom | `--space-block` |

The outer margins of Field, Section, Form, List, Text and ButtonRow are consciously kept in step 2, although the design code says components carry no outer margins (parents lay out with `gap`); that move belongs to a later step. `AppHeader:7` and `StatusMessage:3` were two-axis paddings: AppHeader stays two-axis (Clarification 16); StatusMessage collapses to `--space-group` on both axes, which matches the design-code Banner rule.

`ButtonRow:4`, `AppHeader:22` (`--space-button-gap`) and `LanguageRow:4` (`--space-block`) keep their tokens. Line numbers are those of `main` today; the implementer re-reads each rule. A row that cannot be mapped by role stops the slice and is reported.

### D4 Undefined tokens fail the lint

`frontend/scripts/check-design-tokens.mjs` collects the names defined in `tokens.css` and reports every `var(--name)` in `.css` files and in non-test `.ts` and `.tsx` files whose name is not among them, with file and line. Test files (`*.test.ts` and `*.test.tsx`) are excluded from this check, because they hold removed names and fixtures as text (R25). A `var()` with a fallback is checked too. The script takes one optional argument, the folder that is the equivalent of `src` (default: `src` of the package). Its layout is fixed: the tokens are at `<folder>/styles/tokens.css` (skipped from the raw-value checks as today), the `src/pages` rules apply to `<folder>/pages`, and paths in the output are relative to the folder and go to stderr; the exit code is 1 on any problem. The R25-S3 test writes such a folder to a temporary directory at run time, building the token name in the test, so no source file contains a literal undefined `var(--...)` for the lint to find. A leftover removed name is caught because the removed names are not defined.

### D5 Select chevron

`Select.tsx` renders `<ChevDownIcon />` (default size) inside the existing `<span data-select-arrow aria-hidden="true">`. `Field.module.css` drops `.arrowIcon` and `--arrow-width`, sets the arrow colour to `--color-text-muted` and applies the Clarification 5 insets. The R8-S1 WebKit note is unchanged: the icon uses SVG attributes, never an inline style. R13 stays as written (left padding equals the arrow gap, both `--space-group` now).

### D6 Tests

- `frontend/src/styles/tokens.test.ts` reads `tokens.css` as text: the token name list and values (R24-S1, R24-S2; one literal list of 89 names, copied by hand from the design-code tables, as in `ui-icon-set`: it proves `tokens.css` equals the test's list, not `design-code.md` itself), the contrast ratios of `--color-control-border` (R26-S1) and the text pairs (R26-S3), with one WCAG relative-luminance helper in the test (the tokens are hex).
- `frontend/src/styles/cssRules.test.ts` reads `src/ui/*.module.css` and the TSX files as text for R24-S3, R24-S4, R25-S1, R25-S2, R26-S2, R27-S2, R27-S3, R28-S1 and R28-S2. It spawns `check-design-tokens.mjs` on a temporary folder for R25-S3.
- Page and component tests (`AdminTeachersPage.test.tsx`, `Select.test.tsx` or the existing `Field` tests) read buttons by role and name for R27-S1 and R27-S4 (class names are distinct in jsdom, see `ui-icon-set`) and the chevron markup for R28-S3.
- One Playwright check for R28-S4 in `frontend/e2e/`, next to `icons.spec.ts`.
- Every test title starts with `[web-client/R<n>-S<n>]`.
- Re-run in en and ru at 360: R7-S1, R13-S2, `skeleton.spec.ts`, `russianFit.spec.ts` (R15-S6). No approved test should change; if one does, the slice stops. Watch the R13-S2 text-fit assertion at 360 in Russian on the profile time zone select: its text area shrinks by about 16px (the arrow clearance grows from `--space-1` + 0.75rem + `--space-1` to `--space-group` + 1.25rem + `--space-inline`).

### D7 Decorative edges (#168)

The decorative edges are below 3:1 by design (design-code "Colour"), so `--color-divider` (1.39:1 on surface) and `--color-tree-guide` (2.25:1) are not tested for 3:1. They mark: the List item edge, the IssuedLink box, the Dialog edge, the AppHeader bottom edge, rows, panes, bars and the progress track. Everything a person operates (text input, select, password toggle, secondary button, issued-link input) uses `--color-control-border` (R26). This list goes in the PR as #168 requires.

### D8 Vendored design docs

`refactoring-ui.md`, `refactoring-ui-patterns.md`, `accessibility.md`, `accessibility-patterns.md` and any other file under `.agents/design` that names a removed token (`--shadow-card`, `--color-border`, `--space-1` to `--space-4`, `--color-action-success-*`, the success variant or `--arrow-width`; `design-code.md` itself describes the rename and is kept) are updated in a separate commit, with the engineer's approval (Clarification 7). `.agents/design/ui-ux-pro-max/**` is excluded outright: it is upstream, byte-identical, with generic output. A passage that would be false after the rename is rewritten to match the design code, not just renamed: for example `refactoring-ui.md:102,107` ("teachdash has one shadow", "Cards get `--shadow-card`") becomes shadows on overlays only with `--shadow-overlay`, and the `--space-1` to `--space-4` scale table at `refactoring-ui.md:49-60` becomes the space-by-role names (`--space-adjacent`, `--space-inline`, `--space-group`, `--space-block`, `--space-section`); the same applies to the card shadows at `refactoring-ui.md:145,164` and to the card shadow and success tokens in `refactoring-ui-patterns.md`.

### D9 Interim deviations for 3b

Two things differ from the design code on purpose until 3b (#198) finishes them: the Dialog edge is `--color-divider` (the design code wants no border and `--shadow-overlay`, Clarification 3), and the IssuedLink input has the sunken fill (3b territory, pulled in by Clarification 10). The PR description mentions both. Also for 3b: the `Page` banner rule `.page > [role='status']` (D3, Clarification 17) and the older `Form` rule `.form > [role='status']` must both be extended to `[role='alert']` when #198 turns error banners into `role="alert"`.

## UI design

### Screens touched

Every screen, through shared tokens and `src/ui` components: login, set-password, admin teachers (list and dialogs), profile, cabinet stubs, not found and the startup error. No layout or markup changes except the select chevron (D5), the removed green variant, the header edge and two spacing rules: the space below a banner directly in the page (`Page`, login, admin and cabinet banners; Clarification 17) and the column of detail lines in an admin card (`List .details`; Clarification 18).

### Layout (desktop and 360)

Unchanged structure. Density tightens: button padding 24px to 12px, controls 44px to 40px with a mouse (`pointer: fine`), Title 2rem to 1.5rem, `--radius` 8px to 4px, spacing by role (D3). At 360 Russian labels must still wrap or fit as before (`russianFit.spec.ts`).

### States per screen

Login, set-password, admin list and dialogs, profile: empty, filled, errors, loading, disabled and success as they exist today; their behaviour is unchanged. The startup error and the "Loading..." text get the new tokens only (their restyle is 3b). Screenshot states follow Clarification 11.

### Copy

None. No i18n key or text changes.

### A11y

Focus order, keyboard and `aria-*` are unchanged. The select chevron stays `aria-hidden` and inert to the pointer (`pointer-events: none`). Control edges pass 3:1 (R26); text pairs are at least 4.5:1 (design-code "Colour"); the header edge and card edges are decorative. 200% zoom follows rem.

### Components and tokens

Reused: all of `src/ui`, `ChevDownIcon`. New components: none. New tokens: the design-code tables (D2); each is listed in the visual plan below with its reason.

### Visual plan per screen and state

(From the frontend-researcher, word for word; "Q1" to "Q5" are Clarifications 1 to 5.)

- Scope: tokens.css and base.css to the design-code tables, "exactly the tables" (colour, type, space, shape, controls and icons, layout), rename map across `src/ui` CSS, no aliases; Button `success`, `--arrow-width`, `--shadow-card`, `--color-action-success-*`, `--color-border`, `--space-1..4` gone.
- New tokens, one reason: the design-code tables list them and no existing token fits. Used by step 2: `--color-control-border`, `--color-divider`, `--color-surface-sunken`, `--color-danger-action`, `--weight-strong`, `--line-height-large`, `--space-adjacent/-inline/-group/-section/-page`. Added unused until later: accent-hover/-active/-subtle, rail, tree-guide, focus-ring, backdrop, text-small/-reading, line-height-small/-reading, border-width-strong, selection-bar-width, focus-offset, shadow-overlay, duration-fast, tooltip-delay, row-action-size, icon-size-placeholder, and all layout tokens except `--content-width` and `--lang-select-width`.
- Value changes, same name: background/text/text-muted/accent/neutral/success/danger pairs, radius 0.25rem, text-large 1.25rem, text-title 1.5rem, line-height 1.45, line-height-tight 1.2, `--control-height` 2.75rem and 2.5rem under `@media (pointer: fine)`.
- Control edges, `--color-control-border` (all at least 3:1, closes #168): text input, select, password toggle, secondary button, issued-link input. Decorative edges, `--color-divider`: List item, IssuedLink box, Dialog (Q3), AppHeader bottom (Q2). The decorative list goes in the PR as #168 requires.
- Type: Title `--text-title` at every width (the 40rem step is dropped); Dialog and Section h2 `--text-large`/`--line-height-large`; the five raw `font-weight: 600` become `--weight-strong`; Q4 for headings. Hierarchy unchanged: Title, then h2, then base text, then muted.
- Space (Q1): fields `--space-block` apart with `--space-adjacent` between label and control; input padding and button padding `--space-group`; icon/word and chips `--space-inline`; ButtonRow `--space-button-gap`; sections `--space-section`; Page padding `--space-block` by `--space-group`, then `--space-page` from 40rem.
- Login and set-password (empty, filled, errors, language select): primary (accent) is the submit button, secondary is the field edges, tertiary is the language select and help text. Control border darker, 40px controls on mouse, petrol links, error border still `--color-danger-text` (`--border-width`; 2px is 3a).
- Select chevron (login language, profile time zone, create dialog): `ChevDownIcon` default size (`--icon-size`), `--color-text-muted`, span `right: calc(var(--border-width) + var(--space-group))`, `pointer-events: none`, `data-select-arrow` kept, disabled keeps `--opacity-disabled` (R13-S3, S4); native `<select>` kept with `appearance: none` and `text-overflow: ellipsis`; zoom 200% follows rem; RTL not needed; the R8-S1 WebKit note unchanged (SVG uses attributes, no inline style).
- Admin list (loading, empty, several, deactivated, just-added, errors): primary is "Add teacher" (accent, plus kept), secondary are row buttons, tertiary are badges and muted details. Rows white with a divider edge, `--radius`, no shadow; row padding `--space-group`; the card layout and data table stay for 4b/6a. IssuedLink: surface plus divider, sunken read-only input with a control edge. StatusMessage and StatusBadge: tone fill and text, no shadow, icons and padding are 3b.
- Admin dialogs (create empty/filled/errors, deactivate): "Create teacher" accent primary, "Cancel" secondary, "Deactivate" `--color-danger-action` fill; dialog edge per Q3; padding `--space-block`, title `--text-large`.
- Profile, cabinet stubs, not found: tokens and spacing only; header per Q2, "Log out"/"Profile" stay secondary buttons (text accent until 3a).
- Out of this step: hover/active/focus rules, field error icon and 2px border, busy label (3a); badge/banner icons, Dialog width/border/shadow/backdrop, "Loading…" (3b); rail and tabs (5a/5b); `text-small` use and `text-wrap` (3a/3b); row Deactivate to danger text (3a/6a).
- Density: tighter than today (button padding 24 to 12px, 44 to 40px controls on mouse); check 360 and Russian wrapping in screenshots (`russianFit.spec.ts`).

## Risks / Trade-offs

- [A leftover removed name resolves silently to nothing] -> the lint and R25 fail on any undefined `var(--name)` (D4).
- [Mapping `--space-*` by value instead of role] -> D3 lists each use by role; the slice stops on a use it cannot map.
- [Success tokens and `--space-1..4` must go together with their uses] -> one slice (D1); otherwise lint fails mid-way.
- [Screens tighten visibly] -> accepted (Clarification 1); the screenshots show it.
- [Russian wrapping at 360 with 12px button padding] -> `russianFit.spec.ts` and ru 360 screenshots.
- [The unused tokens are never flagged] -> expected: `tokens.css` holds exactly the tables (Clarification 10).
- [Dark mode is not covered] -> out of scope; contrast is computed for light values only.

## Migration Plan

None. No backend or data changes, no new dependency, no Flyway migration.
