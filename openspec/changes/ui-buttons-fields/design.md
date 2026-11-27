# Design

## Context

Step 2 (#196) moved `tokens.css` and every `src/ui` CSS file to the design-code tokens and left the button and field states open. Today `Button` has `primary|secondary|danger` and one `icon: 'plus'`; its secondary text is accent; the browser draws the focus ring (and hides the 1px error border); the field error is a coloured paragraph; the password toggle is a hand-styled `<button class="toggle">`; the in-flight state is only `disabled`; the language select is a plain `Select` with a 9rem wrapper. Redesign plan step 3a (#197) completes the button and field primitives per the design code ("States", "Buttons, icons and tooltips", "Fields") and builds the busy label and the icon-led language select.

## Goals / Non-Goals

- Goals: every button role and state from tokens; one global focus ring; the field error with an icon and a 2px border that moves nothing; a busy label that keeps the button width; the icon-led language select; the icon inset on icon-led buttons.
- Non-goals: see the proposal's "Out of scope" (header, Toolbar, row actions, dialog foot, banners, Log out busy state).

## Decisions

### D1 Button API

`Button` gets: `variant` of `'primary' | 'secondary' | 'quiet' | 'dangerText' | 'muted' | 'danger'` (names proposed here; CSS Modules classes use the same names); `icon` of a name from the shared set (a union exported from `icons.tsx` with one name-to-component map, so any icon of the table is allowed, not only `'plus'`); `busy` and `busyLabel` (D2); `fullWidth` (D3); and pass-through `ariaLabel` and `ariaPressed` for the password toggle (Clarification 4). A button given an icon also gets the class `withIcon` (Clarification 6: `padding-inline-start: calc(var(--space-group) - var(--button-icon-inset))`, logical properties, no `:has`; the gap stays `--space-inline`).

State rules (Clarification 19) are written on `.button:hover:not(:disabled)` and `:active:not(:disabled)` per variant: primary `--color-accent-hover` and `--color-accent-active`; secondary and quiet `--color-surface-sunken` and `--color-neutral-background`; danger `--color-danger-action-hover` and `--color-danger-action` (no further step defined); `dangerText` hover `--color-danger-background`; `muted` text `--color-text-muted` and `--color-text` on hover. Disabled is `--opacity-disabled` and `cursor: not-allowed`. Focus comes from the global rule (D4); no button rule sets `outline`. `transition: background-color, border-color, color, opacity var(--duration-fast)` and nothing else. Secondary text is `--color-text` (Clarification 3); no non-primary variant uses `--color-accent` as text.

### D2 Busy state

`Button` with `busy` renders `disabled`, `aria-busy="true"` and two spans in one grid cell (`display: grid`, both with `grid-area: 1 / 1`): the idle label and the busy label. The inactive one has `visibility: hidden` and `aria-hidden="true"`; so the accessible name is the visible one only, the width is the longer label's, and there is no measuring and no inline style (R8-S1 forbids it for the WebKit CSP note anyway). Idle, the busy span is hidden the same way; so both labels are always in the DOM, and the button is found by its idle name when idle and by its busy name when busy. A button without `busyLabel` renders as today.

Focus risk (R14-S12, R17): the submit buttons are already natively `disabled` while a request runs, so the busy state changes no focus behaviour; it only adds `aria-busy` and the label. The focus after a failure is still moved by `useFieldErrors` (R17) once the request ends. R29-S7 pins this. No live region, `role="status"` or `role="alert"` is added at request start (Clarification 13); the result is announced by the existing `StatusMessage`.

`runRowAction` in `AdminTeachersPage.tsx` takes the pressed action (`'invite' | 'reset' | 'reactivate' | 'deactivate'`), and the per-row busy state records it (today `busyIds`, a list of ids, becomes a map from the id to the action), so the row passes `busy` and `busyLabel` to the one pressed button only; the other buttons of the row stay `disabled` with their idle labels. The deactivate dialog's Confirm is busy when the confirming teacher's action is `'deactivate'`. `Log out` is not touched.

### D3 Full-width variant

`fullWidth` adds a class whose rule sits inside `@media (max-width: 63.999rem)` and sets `width: 100%` (below that width, as the header and the single column layout do). It is passed only by the submit of `LoginPage` and `SetPasswordPage`. `ButtonRow` and the dialog foot are not changed.

### D4 Global focus ring

`base.css` gets one rule: `:focus-visible { outline: var(--border-width-strong) solid var(--color-focus-ring); outline-offset: var(--focus-offset); }`. No CSS file may contain `outline: none` or `outline: 0` (a text test over `src`). With it the 2px error border (D5) and the ring are both visible on a focused invalid field: the border is inside, the ring `--focus-offset` outside it.

### D5 Field error

A small shared component `FieldError` in `src/ui` (used by `TextInput` and `Select`) renders `<p id=… class=error><AlertIcon size small aria-hidden/><span>message</span></p>`; the `p` is flex with `gap: var(--space-inline)`, the icon `align-self` on the first line (the icon box is as tall as the line, `--icon-size-small` against `--line-height-small` of `--text-small`, with `margin-top` as the difference over two if the two differ; the implementer measures), and the text in `<span>` with `min-width: 0` so it wraps under itself. `--text-small`, `--line-height-small`, `--color-danger-text`. No `text-overflow`, `nowrap` or `overflow: hidden`. The icon is `aria-hidden`, so the `aria-describedby` description is the message text only. `aria-invalid="true"` and `aria-describedby` are set as today; no `role="alert"` (Clarification 8).

`.invalid`: `border-width: var(--border-width-strong)`, colour `--color-danger-text`, `padding-inline-start: calc(var(--space-group) - var(--border-width))` (the border grows by one `--border-width` on the start side, so the text stays). The end side: an input's text has no end content; a select's arrow is placed relative to the wrapper (`.selectWrap`), so it does not move; the select's `padding-right` loses 1px of area to the thicker border, which is accepted (text is cut earlier by 1px, never hits the arrow). Height is `min-height: var(--control-height)` with `box-sizing: border-box`, unchanged.

### D6 Hover, disabled and read-only fields

`.input:hover:not(:disabled):not(:read-only):not(.invalid)` gets `border-color: var(--color-text-muted)`. `.input:disabled` and `.select:disabled + .arrow` get `opacity: var(--opacity-disabled)` and `cursor: not-allowed` (a `select:disabled` and its arrow already have the opacity; the cursor is new). The IssuedLink input keeps `--color-surface-sunken` (it is `readOnly`), so the hover rule skips it. Transitions on `border-color` only, `--duration-fast`.

### D7 Password toggle

`PasswordInput` passes a `Button variant="secondary"` as the adornment, with `ariaPressed`, `ariaLabel` (the existing `ui.showPassword`/`ui.hidePassword` strings) and `disabled`. The `.toggle` rule and its colour/size are deleted from `Field.module.css`. The visible word and the label keys are unchanged.

### D8 Icon-led select

`Select` gets an optional `icon` prop (the name of a shared icon; the language select passes the globe). The icon renders in a span with its own data attribute (`data-select-icon`), `aria-hidden`, muted (`--color-text-muted`), `pointer-events: none`, placed after `<select>` in the wrapper, outside `[data-select-arrow]` so R28-S3's "no other svg inside the arrow" holds. Class `.selectWithIcon` on the `select` (not on the default one) adds `padding-inline-start: calc(var(--space-group) + var(--icon-size) + var(--space-inline))`; the icon span is `position: absolute; top: 0; bottom: 0; left: calc(var(--border-width) + var(--space-group))`. The arrow rule and `.select` rules of R28-S2 stay as they are, so both gaps are `--space-group` from the inner edges (R13-S5). `UiLanguageSelect` passes the icon; its wrapper uses `--lang-select-width`, now `10.5rem` (Clarification 16). The select keeps `text-overflow: ellipsis` before the chevron, the visually hidden label "Language" / «Язык», and the `lang` of each option.

### D9 Copy: busy labels

One key per button, in both bundles. Proposed strings (shown to the engineer at the spec gate):

| Key | en | ru |
|---|---|---|
| `login.submitting` | Signing in… | Вход… |
| `setPassword.submitting` | Setting password… | Установка пароля… |
| `profile.saving` | Saving… | Сохранение… |
| `profile.changingPassword` | Changing password… | Смена пароля… |
| `adminTeachers.create.submitting` | Creating… | Создание… |
| `adminTeachers.deactivate.confirming` | Deactivating… | Деактивация… |
| `adminTeachers.busy.inviteLink` | Creating invite… | Создание приглашения… |
| `adminTeachers.busy.resetLink` | Creating link… | Создание ссылки… |
| `adminTeachers.busy.reactivate` | Reactivating… | Активация… |

The row keys sit under `adminTeachers.busy`, not `adminTeachers.actions`, so the 24-character bundle test of the action labels is not affected (they are shorter anyway). The ellipsis is the single "…". The Russian labels are nouns: the existing exception list in the design-code verb-form rule grows by this one (D11). The password toggle names stay as they are in the bundles ("Show {{label}}" in English today, «Показать: {{label}}» in Russian); R30-S8 reads them by key.

### D10 Tests

Every Vitest or Playwright test title starts with `[web-client/R<n>-S<n>]` (or the other capability's ID).

- Vitest, components: `Button.test.tsx` (R27-S1, R27-S5, R27-S8, R29-S1), `Select.test.tsx` (R13-S6, R30-S3), a `TextInput`/`PasswordInput` test (R30-S3, R30-S8), page tests for R29-S3, R29-S4, R29-S7 and the modified scenarios.
- Vitest, CSS as text (`cssRules.test.ts`, `tokens.test.ts`): R24-S1, R24-S2 (90 names, the two values), R26-S2 (2px error border), R27-S2, R27-S6, R27-S7, R27-S9, R13-S6, R30-S1 (rule and no `outline: none`), R30-S4, R30-S7, R29-S2's CSS half.
- Bundles: `parity.test.ts` and `copyRules.test.ts` take the new keys; R29-S5 asserts the nine keys, the single "…" and ru differing from en.
- Playwright: R30-S1 and R30-S2 (computed outline and border), R30-S5 (wrap at 360, no movement), R13-S5 (`selectArrow.spec.ts`: its helper at lines 26-48 uses `paddingLeft` as the arrow gap; the icon-led select gets its own measure of icon gap, arrow gap and text clearance; R13-S1 to R13-S4 stay on the default select), R29-S2 (width idle and busy, en and ru, desktop and 360, with `page.route` holding the request), R29-S6 (Russian busy labels fit at 360, in `russianFit.spec.ts`).
- Tests that read the button by its idle name while the request runs now find it by the busy label (Clarification 14): `App.test.tsx:263,264,268` (R1-S4); `ProfilePage.test.tsx:403` (R2-S8) and `:1018` (R3-S9); `AdminTeachersPage.test.tsx`: `submitButton()` where it is called busy (about 345, 455, 998, 1006, including the cancel-while-busy and native-close tests), R5-S4 (`deactivateButton(row)` at 955: hold the element reference) and `confirm()` (`:1042-1055`). Idle-name reads outside a busy state stay: `ProfilePage.test.tsx:305,347,528`, `App.test.tsx:305-320`, `SetPasswordPage.test.tsx:75,166,211`, `PasswordHints.test.tsx:371`, `sessionEnded.test.tsx:79`, `e2e/selectArrow.spec.ts:169`, `fieldErrors.spec.ts` and the `russianFit.spec.ts` clicks. `ProfilePage.test.tsx` near line 525 compares `describedBy` text: it stays equal because the error icon is `aria-hidden` (R30-S3). The implementer re-reads each; a test that cannot be adapted by these two means stops the slice.

### D11 Design docs and tokens

`.agents/design` edits (the engineer's Clarifications 12, 17, 18; the token rows follow from Clarifications 6 and 16 and R24's "exactly the design-code tables"):

- `design-code.md` "Copy and languages", the Verb forms line, ends with `The exception: «Отмена», «Новое приглашение» and «Ссылка для сброса пароля» are named by a noun.`; add after it: `A busy label on a button is a noun too («Вход…», «Сохранение…», «Создание…», «Смена пароля…», «Деактивация…»): it names the process, and the ellipsis is the single «…».`
- `design-code.md` "Controls and icons" table: add the row ``| `--button-icon-inset` | `0.1875rem` | the empty margin inside an icon box; subtracted from the start padding of an icon-led button |``; and in the layout table change `| `--lang-select-width` | `9rem` | language select |` to `| `--lang-select-width` | `10.5rem` | language select (with its icon) |`.
- `design-code.md` "Redesign plan", small items: replace `- 3a and 3b: the space before a full stop after a control, the quiet button icon inset, the 2rem row focus ring meeting the border.` with nothing and replace `- 4a: a tooltip's text always matches its trigger's current label; a tooltip never covers the row below its trigger's row.` with `- 4a: the space before a full stop after a control and the 2rem row focus ring meeting the border (moved from 3a); a tooltip's text always matches its trigger's current label; a tooltip never covers the row below its trigger's row.`
- `refactoring-ui-patterns.md:91`, before `  outline: var(--border-width) solid var(--color-accent);`, after `  outline: var(--border-width-strong) solid var(--color-focus-ring);`.
- `accessibility-patterns.md:29`, before `  padding: var(--space-adjacent) var(--space-inline);`, after `  padding: 0 var(--space-group);`.

`tokens.css` gets `--button-icon-inset: 0.1875rem` in the controls group and `--lang-select-width: 10.5rem`; `tokens.test.ts` has 90 names (R24-S1) and the two values (R24-S2). The new token is listed in the UI design section below.

### D12 Screenshots

Same method as `ui-design-tokens` (its design D6 and tasks): the `frontend-implementer` takes the before set from `main` before the first slice, into `frontend/screenshots/ui-buttons-fields/before/`, with the design-audit state list (login x5, set-password x6, admin x11 including dialogs, profile x9, cabinet x2, not found, startup error), at desktop and 360. The after set has the same states plus: the busy label of each button in en and ru at 360 (request held), a focused invalid field, and the icon-led language select (header and login) in en and ru. Unreachable states: the `quiet`, `dangerText` and `muted` variants, the icon variants other than the plus, the hover and active states of every control (listed, not shot), and the `fullWidth` look above `64rem` (not applied).

## UI design

### Screens touched

Every screen, through the shared `Button`, field, `Select` and the global focus rule: login, set-password, admin teachers (list, create and deactivate dialogs, issued-link box), profile (profile form, password form), cabinet stubs, not found, startup error, and the header (Log out, Profile and the language select). The login and set-password pages also change the lone submit's width at narrow widths. The teacher row keeps today's layout (Clarification 2).

### Layout (desktop and 360)

- Buttons: one line, label centred, minimum width kept; a long Russian label wraps inside the button at 360 (design-code, Copy). Height `--control-height`. The icon-led button starts its padding `--button-icon-inset` earlier. Login and set-password submit: natural width and left-aligned on desktop, full width below `64rem`.
- Password row: input and the Show/Hide Button `--space-button-gap` apart; the toggle is `--control-height` high, so it lines up with the input.
- Field error: label, control, message, `--space-adjacent` apart. A long message wraps at 360 under its own text.
- Icon-led language select: globe, text, chevron, with the globe and the chevron each `--space-group` from the inner edge; the header wraps at 360 as today.

```
Field error (desktop, 360 wraps the text under the text, not under the icon):

  New password
  +----------------------------------------+  [ Show ]
  | ••••                                   |   <- 2px red border, text did not move
  +----------------------------------------+
  (!) The password must be at least         <- icon aligned to the first line
      8 characters.

Icon-led language select (inner gaps are --space-group on both sides):

  +--------------------------------+
  | (o)  English              v    |
  +--------------------------------+
   ^12px                      12px^
```

### States per screen

- Buttons: default, hover, active, focus, disabled for every variant; busy for the submits, Confirm and the pressed row button (disabled, `aria-busy`, busy label, no spinner, width kept).
- Fields: default, hover, focus, error (also the live password hints), disabled, read-only (the issued link). Selects: the same, with the arrow.
- Loading ("Loading…" text), empty and success states of the pages are unchanged.

### Copy

The nine busy keys of D9 in en and ru. The password toggle's names, "Cancel", "Confirm" and the other labels are unchanged. The Russian busy labels are nouns.

### A11y

- Focus order is unchanged. The focus ring is the global `:focus-visible` rule (2px, `--color-focus-ring`, 2px offset), never removed; a focused invalid field shows both the border and the ring.
- A busy button is natively `disabled` and has `aria-busy="true"`; its accessible name is the visible label (the other label is `aria-hidden`). Nothing is announced at request start; the result goes through `StatusMessage`. A disabled focused button may lose key events as before; the focus after a failure moves per R17.
- A field error: `aria-invalid="true"` and `aria-describedby`; the icon is `aria-hidden`, the message has no `role="alert"`. The language select's globe is `aria-hidden`; its visually hidden label and option `lang` stay. The password toggle keeps `aria-pressed` and its "Show: label" style name (visible word first, WCAG 2.5.3).
- Touch targets: `--control-height` (2.75rem, 2.5rem with a mouse); at 200% zoom everything follows rem.

### Components and tokens

Reused: `Button`, `TextInput`, `PasswordInput`, `Select`, `UiLanguageSelect`, `LanguageRow`, the icons `PlusIcon`, `AlertIcon`, `GlobeIcon`, `ChevDownIcon`. New component: `FieldError` (D5). New token, one: `--button-icon-inset` (`0.1875rem`): no existing token fits (the gap is `--space-inline`, a smaller gap or a negative margin each break a rule, #197 review). Changed token: `--lang-select-width` 9rem to 10.5rem. The tokens `--color-accent-hover`, `--color-accent-active`, `--color-focus-ring`, `--focus-offset`, `--border-width-strong`, `--text-small`, `--line-height-small`, `--duration-fast` and `--color-danger-action-hover` are in `tokens.css` since #196 and become used here.

### Visual plan per screen and state

From the frontend-researchers, word for word. The numbers "D2" to "D9" in the original plans were the researchers' own question numbers; where an engineer decision differs, the decision wins and only that line is adjusted (the proposal's Clarifications).

#### Plan A (buttons)

**Button component, all screens (default, hover, active, focus, disabled)**
- Hierarchy: primary (accent fill, on-accent text) is primary; secondary (surface fill, control border, text colour `--color-text`, Clarification 3) is secondary; quiet, muted-word and danger text are tertiary. Danger fill is only for the confirmation inside a dialog. One primary per group.
- Tokens: height `--control-height` (2.5rem pointer fine, 2.75rem touch); padding `0 var(--space-group)`; gap `--space-inline`; `--text-base`, `--weight-strong`, `--line-height-tight`; `--radius`; `--border-width`. Hover is `--color-accent-hover`, `--color-surface-sunken` (secondary, quiet) or `--color-danger-action-hover`. Active is `--color-accent-active` or `--color-neutral-background`. Disabled is `--opacity-disabled` plus `not-allowed`. Focus is a `--border-width-strong` outline in `--color-focus-ring` at `--focus-offset`. Transitions are `--duration-fast` on background-color, border-color, color and opacity only.
- New token: `--button-icon-inset` (`0.1875rem`, the empty margin inside the 20-unit plus icon); no existing token fits, a smaller gap changes `--space-inline`. Applied as `padding-inline-start: calc(var(--space-group) - var(--button-icon-inset))` only when an icon leads, gap stays `--space-inline` (Clarification 6).
- Layout and density: one line, label centred, min width kept. At 360 a long Russian label wraps inside the button (design-code 383). The words-only quiet and muted variants have no border and are left-aligned to their text column.

**Login, set-password, profile Save and Change password, ErrorWithRetry "Try again" (idle, disabled)**
- Primary is the only button, word-only, no icon. It sits `--space-block` below the last field or message (Form and ButtonRow gap tokens). Desktop: natural width, left-aligned. 360: full width for the lone submit on login and set-password (Clarification 5), natural width elsewhere.
- Disabled is `--opacity-disabled`; the busy label is the Loading angle.

**Admin teachers list, desktop and 360 (3a scope only)**
- "Add teacher" (primary + plus) stays in Toolbar, right-aligned; plus is `--icon-size` with the inset applied. Row buttons keep today's layout (secondary + filled danger "Deactivate", Clarification 2) until 6a. Buttons in a row are `--space-button-gap` apart and wrap at 360 with the same gap. The pressed row button shows its busy label (Clarification 10).

**Create and deactivate dialogs, desktop and 360 (button styling only; the foot structure is #198)**
- Create: "Cancel" secondary, then "Create teacher" primary with no icon. Deactivate: "Cancel" secondary, then "Confirm" danger fill (labels change in 6a). Safe choice first in DOM order; first focus lands on it. ButtonRow gap `--space-button-gap`. While busy both are disabled, and the submit or Confirm shows its busy label.

**IssuedLink box and AppHeader**
- Copy (primary) then Close (secondary) in ButtonRow. Header "Profile" or "Back to cabinet" and "Log out" stay secondary, word-only, `--space-button-gap` apart, wrapping at 360, until 5a/5b.

**Password Show/Hide (profile, login, set-password; idle, disabled)**
- Looks like a secondary button: `--control-height` high, weight 600, control border. `--space-button-gap` from the input. It keeps `aria-pressed` and the aria-label "Show: label" (ru «Показать: …»).

Left out of this plan: the quiet, muted and danger-text variants in use (no screen yet, so only unit tests and the unreachable-states list), and the Loading label and field error states (Plan B).

#### Plan B (fields and states)

**Fields, default/hover/focus/disabled/read-only (login, set-password, profile, Add teacher dialog; all widths)**
- Hierarchy: label 600 (secondary) > value (primary) > hint/error `--text-small` (tertiary). Layout: label, control, message, `--space-adjacent` apart; fields `--space-block` apart; password row = input + toggle, `--space-button-gap`.
- Size: `min-height: var(--control-height)` (2.75rem touch, 2.5rem `pointer: fine`), padding `0 var(--space-group)`, `--border-width` `--color-control-border`, `--radius`, `--color-surface`.
- Hover: border `--color-text-muted`. Focus: global `:focus-visible` ring (`--border-width-strong`, `--color-focus-ring`, `--focus-offset`), never `outline: none`. Disabled: `--opacity-disabled`, `cursor: not-allowed`, native `disabled` (inputs, select + arrow, toggle). Read-only: `--color-surface-sunken` fill.
- Tokens: all existing. New: none. Density unchanged; the ring adds 4px outside, inside the `--space-group` gutter at 360.

**Field error (text input, password + toggle, select; desktop and 360, en and ru)**
- Border `--border-width-strong` `--color-danger-text`; `padding-inline-start: calc(var(--space-group) - var(--border-width))` so text and arrow do not move; height unchanged. `aria-invalid="true"`, `aria-describedby` the message id; no `role="alert"`.
- Message below the field: `AlertIcon` at `--icon-size-small` (`aria-hidden`) + text `--text-small` `--line-height-small` in `--color-danger-text`, flex, `--space-inline` gap, icon aligned to the first line, text wraps under the text (not under the icon), never truncated.
- Focused + invalid: 2px red border inside, the 2px accent ring 2px outside it: both visible (fixes #216 follow-up). Hover on invalid: border stays red.
- Tokens: all existing (`--color-danger-text`, `--border-width-strong`, `--icon-size-small`, `--text-small`, `--space-inline`). No new token.

**Button loading (Login, Set password, Save, Change password, Create teacher; the clicked row button and Deactivate confirm too, Clarification 10)**
- Hierarchy unchanged (the button keeps its role). Disabled (`--opacity-disabled`, `cursor: not-allowed`) + `aria-busy="true"` + the progressive label ("Signing in…", "Saving…", «Вход…», «Сохранение…»).
- Width: idle and busy labels stacked in one grid cell (the inactive one `aria-hidden`, `visibility: hidden`), so the button keeps the width of the longer label in both languages; height `--control-height`; no layout shift; no spinner.
- Sibling buttons in the row (Cancel) stay disabled and unchanged. Tokens: existing only.

**Language select, icon-led (header after Log out; login/set-password row above the title in `<main>`; en/ru; desktop and 360)**
- Layout: globe icon `--icon-size`, muted, `--space-group` from the inner left edge; text; chevron `--icon-size`, muted, `--space-group` from the inner right edge (equal gaps); text starts at group + icon + `--space-inline`; `padding-right` as today; ellipsis before the chevron.
- Visually hidden label "Language"/«Язык» stays; icon `aria-hidden`; option `lang` unchanged; the select is the first tab stop on login/set-password.
- Width `--lang-select-width`, raised from 9rem to 10.5rem so «Русский» fits with margin (Clarification 16). Header wraps at 360 as today.
- Tokens: existing; no new token beyond the changed `--lang-select-width`.

## Risks / Trade-offs

- [A focused button that becomes disabled can lose focus] -> the buttons were already disabled while busy; the busy state adds no new loss and R29-S7 pins the focus after a failure (D2).
- [Russian busy labels at 360 with `--space-group` padding] -> R29-S6 and the Russian screenshots; labels wrap and are never cut.
- [Two stacked labels in one cell double the DOM text] -> the hidden one is `aria-hidden` and `visibility: hidden`, so assistive technology and `getByRole` see one name (R29-S1).
- [The 2px error border costs 1px of the select's text area at the end side] -> accepted, D5; the text is cut 1px earlier and never reaches the arrow.
- [Tests that find a button by its idle name mid-request] -> listed in D10; they use the busy key or a held element reference.
- [A new variant nothing renders is dead code] -> accepted (Clarification 1: the builder issues need it); unit tests and the unreachable-states list prove it.
- [The English password toggle name has no colon today] -> the key text is unchanged; the colon form is the Russian one and the design-code wording.

## Migration Plan

None. No backend or data changes, no new dependency, no Flyway migration.
