# Spec Delta

## ADDED Requirements

### Requirement: R29 Busy button

A button whose request is in flight SHALL be natively disabled, have `aria-busy="true"` and show its own busy label, with no spinner and a width that never shrinks while busy (it may grow). This covers the form submits, the deactivate "Confirm" and the pressed row button, and nothing else (not Log out, not "Try again"). A busy label ends with "…" and, in Russian, is a noun. Starting a request SHALL announce nothing.

#### Scenario: web-client/R29-S1 A busy button is disabled, marked busy and named by its busy label
- **GIVEN** a `Button` with an idle label and a busy label, rendered idle and then busy (a unit test)
- **WHEN** it is read by role and name
- **THEN** idle, it is found by the idle label, is enabled and has no `aria-busy="true"`, and only the idle label is in the DOM (its `textContent` is the idle label)
- **AND** busy, it is found by the busy label, is `disabled` and has `aria-busy="true"`, the idle label is not part of its accessible name (it is `aria-hidden`), no `svg` was added, and pressing it calls no handler

#### Scenario: web-client/R29-S2 The busy button never gets narrower
- **GIVEN** `Button.module.css`, read as text, and in `en` and in `ru` the login page on the desktop project and, on the 360-pixel phone project, a button that is not full width (profile Save, "Create teacher" or a row button), each with its request held
- **WHEN** the button is pressed
- **THEN** the button's width while the request runs is at least its width before, equal where the busy label is not longer than the idle one, and its height is the same
- **AND** the CSS puts the two labels in one grid cell only while busy, the idle one with `visibility: hidden`, and the button has no `style` attribute

#### Scenario: web-client/R29-S3 Each form's submit button shows its own busy label
- **GIVEN** the login page, the set-password page, the profile form, the profile password form and the "Add teacher" dialog, each with its request held
- **WHEN** each form is submitted
- **THEN** the submit button is found by the text of its own key, `login.submitting`, `setPassword.submitting`, `profile.saving`, `profile.changingPassword` and `adminTeachers.create.submitting` in turn, and is `disabled` with `aria-busy="true"`
- **AND** when the request fails, the button is found by its idle label again and has no `aria-busy="true"`; when it succeeds the button is gone (the route changed or the dialog closed) or, for Save and Change password, idle again

#### Scenario: web-client/R29-S4 The pressed row button and Confirm show their busy labels
- **GIVEN** an admin page with an active teacher without a password, one with a password and a deactivated one, and the deactivate dialog open, each request held
- **WHEN** "New invite link", "Password reset link" or "Reactivate" is pressed on its row, and in the dialog "Confirm" is pressed
- **THEN** the pressed row button shows its own busy label (`adminTeachers.busy.inviteLink`, `adminTeachers.busy.resetLink` or `adminTeachers.busy.reactivate`) with `aria-busy="true"`, and the other buttons of that row keep their idle labels, are disabled and are not marked busy
- **AND** "Confirm" shows `adminTeachers.deactivate.confirming` with `aria-busy="true"`, and "Cancel" keeps its label and is disabled

#### Scenario: web-client/R29-S5 Busy labels exist in both languages and follow the copy rule
- **GIVEN** `en.json` and `ru.json`, read by a bundle test, and the keys `login.submitting`, `setPassword.submitting`, `profile.saving`, `profile.changingPassword`, `adminTeachers.create.submitting`, `adminTeachers.deactivate.confirming`, `adminTeachers.busy.inviteLink`, `adminTeachers.busy.resetLink` and `adminTeachers.busy.reactivate`
- **WHEN** the keys are read in both bundles
- **THEN** each exists in both with a non-empty text that ends with the single character "…", never three dots, and the Russian text differs from the English one
- **AND** the existing parity and copy-rule tests pass with the new keys

#### Scenario: web-client/R29-S6 Russian busy labels fit a phone screen
- **GIVEN** the browser has a 360-pixel-wide screen, the UI language is `ru`, and each request of R29-S3 and R29-S4 is held
- **WHEN** each button is pressed and shows its busy label
- **THEN** on every such page and dialog `document.documentElement.scrollWidth <= clientWidth` and no element is wider than the viewport

#### Scenario: web-client/R29-S7 Starting a request announces nothing and moves no focus
- **GIVEN** the login page, with the request held, and in a second run a form whose request then fails with field errors (R17-S1)
- **WHEN** the submit button is pressed
- **THEN** no element with `role="status"` or `role="alert"` and no live region appears while the request runs, and `document.activeElement` is the same element before the press and while the request runs when the form was submitted from a field (not moved by the busy state)
- **AND** when the request fails with field errors, the focus goes to the topmost invalid field as web-client R17 says, and when it fails without them the button is enabled again with its idle label

### Requirement: R30 Field focus, error and states

Every interactive element SHALL show one global `:focus-visible` ring and never `outline: none`. A field with an error SHALL be `aria-invalid` and described by a message under it that has an alert icon and no `role="alert"`, with a `--border-width-strong` danger border that moves nothing; the live hints of web-client R14 take the same path. Fields SHALL have hover, disabled and read-only looks, and the password toggle SHALL be a `Button` (scenarios).

#### Scenario: web-client/R30-S1 One global focus rule, never removed
- **GIVEN** `base.css` and every `.css` file under `frontend/src`, read as text, and the login page, with the keyboard moved by Tab over its link, input and button
- **WHEN** the rules and the computed styles are read
- **THEN** `base.css` has one `:focus-visible` rule with `outline: var(--border-width-strong) solid var(--color-focus-ring)` and `outline-offset: var(--focus-offset)`, and no CSS file contains `outline: none` or `outline: 0`
- **AND** each focused element has a computed outline of 2 pixels, solid, at an offset of 2 pixels

#### Scenario: web-client/R30-S2 A focused invalid field shows the red border and the ring
- **GIVEN** the set-password page with "abc" typed in the new password field, which is focused and shows its "at least 8 characters" hint
- **WHEN** the computed style of the field is read
- **THEN** its border is 2 pixels wide in `--color-danger-text` and its outline is 2 pixels wide in `--color-focus-ring`, so both are visible
- **AND** the same holds for the profile form's wrong current password after a failed change

#### Scenario: web-client/R30-S3 An erroring field is described by its message
- **GIVEN** `TextInput`, `PasswordInput` and `Select` rendered with an `error` text and without one (a unit test)
- **WHEN** each control is read by its label
- **THEN** with an error it has `aria-invalid="true"` and an `aria-describedby` that names the message element, its accessible description equals the message text exactly, and the message has no `role="alert"`
- **AND** the message's first child is an `svg` of the alert icon at the small icon size, `aria-hidden="true"` and `focusable="false"`; without an error none of this exists

#### Scenario: web-client/R30-S4 The error rules keep the field still
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the invalid and message rules are read
- **THEN** the invalid rule has a border of `--border-width-strong` in `--color-danger-text` and `padding-inline-start: calc(var(--space-group) - var(--border-width))`, and no `outline: none`
- **AND** the message rule is a flex row with `gap: var(--space-inline)`, `--text-small`, `--line-height-small` and `--color-danger-text`, has no `text-overflow`, `white-space: nowrap` or `overflow: hidden`, and the icon sits on the first line of the text; a Playwright check shows the icon's vertical centre within the first text line's box

#### Scenario: web-client/R30-S5 A long error message wraps under its text
- **GIVEN** a text input and the time zone select with a long error text (the longest Russian field error), at 360 pixels, and the same controls without an error
- **WHEN** the layout is measured
- **THEN** the message wraps onto more lines inside the field's width, every line's text starts at the same left edge to the right of the icon, and no text is cut
- **AND** the control's height, the text's start and the select arrow's position are the same with and without the error, within 1 pixel

#### Scenario: web-client/R30-S6 The live password hints look like errors
- **GIVEN** the set-password page and the profile password form
- **WHEN** a 3-character new password is typed, and then a repeat that differs
- **THEN** each hint field is `aria-invalid="true"`, has the 2-pixel danger border and the alert icon, and its hint is the field's accessible description, as in R30-S3
- **AND** the wording and timing of the hints stay as web-client R14 says

#### Scenario: web-client/R30-S7 Fields hover, disabled and read-only
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the input and select rules are read
- **THEN** an enabled, writable, valid input or select has a hover border of `--color-text-muted`, in a rule inside `@media (hover: hover)` that does not use `:read-only` (a computed hover check on a select shows the border); an invalid, a disabled or a read-only (`[readonly]`) one has no hover rule; a field keeps `min-height: var(--control-height)`
- **AND** a disabled input or select (and its arrow) has `opacity: var(--opacity-disabled)` and `cursor: not-allowed`, and the read-only issued-link input keeps its `--color-surface-sunken` fill

#### Scenario: web-client/R30-S8 The password toggle is a Button
- **GIVEN** `PasswordInput` rendered, hidden and shown, enabled and disabled (a unit test), and `Field.module.css` read as text
- **WHEN** the toggle is read by role
- **THEN** it is a `Button` of the secondary variant with `aria-pressed` that follows the state, and its accessible name is the text of `ui.showPassword` or `ui.hidePassword` with the field's label
- **AND** it is natively `disabled` when the field is, and the CSS has no `.toggle` rule

## RENAMED Requirements

- FROM: `### Requirement: R27 Button roles without a green variant`
- TO: `### Requirement: R27 Button roles and states`

## MODIFIED Requirements

### Requirement: R13 Select arrow

Every select field of the web client SHALL show the app's own dropdown arrow in place of
the browser's. The gap between the arrow and the inner edge of the field's right border
SHALL equal the gap between the text and the inner edge of the left border, and the
selected text SHALL be cut off before the arrow when it does not fit. A select that
leads with an icon (the UI language select, with the globe) SHALL instead put the arrow
and the icon at the same gap, `--space-group`, from the inner edges of the right and
left borders, and its text SHALL start after the icon, so the selected language name
fits with room to spare. A click or tap on
the arrow SHALL reach the field, as a click on its text does.

#### Scenario: web-client/R13-S1 Arrow is inset like the text
- **GIVEN** a logged-in admin, on `/profile` and then in the "Add teacher" dialog
- **WHEN** the time zone select is shown
- **THEN** each time the browser's own arrow is switched off and the app's arrow is visible inside the field
- **AND** the distance from the arrow's right edge to the inner edge of the right border equals the select's left padding, within 1 pixel

#### Scenario: web-client/R13-S2 Long option does not run under the arrow
- **GIVEN** a logged-in teacher with time zone "America/Argentina/ComodRivadavia" and a 360-pixel-wide screen
- **WHEN** `/profile` is opened
- **THEN** the select is as wide as the name field, its text area ends before the arrow's left edge, and text that does not fit is cut off with an ellipsis
- **AND** the page has no horizontal scroll

#### Scenario: web-client/R13-S3 Click on the arrow reaches the select
- **GIVEN** a logged-in teacher on `/profile`
- **WHEN** the teacher clicks in the middle of the arrow
- **THEN** the click lands on the time zone select, not on the arrow, and the select has the focus

#### Scenario: web-client/R13-S4 Disabled select keeps the arrow
- **GIVEN** a logged-in teacher on `/profile`, and the arrow's position is noted
- **WHEN** a profile save is still running, so the time zone select is disabled
- **THEN** the arrow is visible at the same position

#### Scenario: web-client/R13-S5 The icon-led select has equal gaps for the icon and the arrow
- **GIVEN** the UI language select on the login page (in `<main>`, above the title) and in the header of a logged-in teacher, in `en` and in `ru`, on the desktop and the 360-pixel phone projects
- **WHEN** the select is shown
- **THEN** it has a globe icon that is `aria-hidden="true"`, is not inside the `[data-select-arrow]` element, and is muted like the arrow; the `[data-select-arrow]` element still holds only the chevron
- **AND** the distance from the icon's left edge to the inner edge of the left border equals the distance from the arrow's right edge to the inner edge of the right border, within 1 pixel, and both are `--space-group`
- **AND** the select's left padding equals `--space-group` plus `--icon-size` plus `--space-inline`, and the selected language name is not cut off (its text width, measured with `canvas.measureText` using the select's computed font, is at most the width of the text area)
- **AND** with the select disabled, and in a run where it has an error (the test passes one), the icon is dimmed with the select, and the icon and the text start do not move by more than 1 pixel

#### Scenario: web-client/R13-S6 Only a select with an icon gets the icon rules
- **GIVEN** `Select.tsx` and `Field.module.css`, read as text, and rendered selects with and without the `icon` prop
- **WHEN** the select, icon and arrow rules are read, and the markup is read
- **THEN** a select given an icon renders the icon with its own data attribute, `aria-hidden="true"`, outside `[data-select-arrow]`, at `left: calc(var(--border-width) + var(--space-group))`, and a select class with `padding-inline-start: calc(var(--space-group) + var(--icon-size) + var(--space-inline))`
- **AND** the arrow stays the element right after the select (`.select:disabled + .arrow` still matches) and a disabled icon-led select dims the icon too, and an invalid one has a rule that keeps the icon-led start padding minus one `--border-width`
- **AND** a select without the prop renders no icon and gets none of those rules, and the arrow and select rules of web-client R28-S2 are unchanged

### Requirement: R24 Design tokens

`frontend/src/styles/tokens.css` SHALL declare exactly the design-code token tables, with
their names and values, and no other custom property. Titles and headings SHALL use the
type tokens and `--weight-strong`, not raw sizes or weights, and the Page padding SHALL
follow the space roles (scenarios).

#### Scenario: web-client/R24-S1 tokens.css declares exactly the token names
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of 90 design-code token names copied by hand from the "Tokens" tables (83 table rows, with `--button-icon-inset` in "Controls and icons"; a row such as `--color-neutral-background` / `-text`, `--color-danger-action` / `-hover`, `--weight-regular` / `--weight-strong`, `--border-width` / `--border-width-strong` or `--side-panel-min` / `--side-panel-max` gives two names)
- **WHEN** its custom property definitions are listed
- **THEN** every name of the list is defined
- **AND** no other name is defined

#### Scenario: web-client/R24-S2 tokens.css has the design-code values
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of the design-code values copied by hand, including `--control-height` and `--row-action-size` under `@media (pointer: fine)` and `--type-label-width` under `:root:lang(ru)`
- **WHEN** each token's value is read
- **THEN** it equals the list's value, with `--radius` `0.25rem`, `--text-large` `1.25rem`, `--text-title` `1.5rem`, `--line-height` `1.45`, `--line-height-tight` `1.2`, `--control-height` `2.75rem` (`2.5rem` under `@media (pointer: fine)`), `--button-icon-inset` `0.1875rem` and `--lang-select-width` `10.5rem`

#### Scenario: web-client/R24-S3 Headings use the type tokens
- **GIVEN** the CSS of Title, Section and Dialog, read as text
- **WHEN** the heading rules are read
- **THEN** the Title `font-size` is `--text-title` with no `@media (min-width: 40rem)` size step, its line height is `--line-height-tight` and its weight is `--weight-strong`
- **AND** the Section heading and the Dialog title use `--text-large`, `--line-height-large` and `--weight-strong`
- **AND** no CSS file under `src/` contains a raw `font-weight` number

#### Scenario: web-client/R24-S4 Page padding follows the space roles
- **GIVEN** the CSS of Page, read as text
- **WHEN** its padding rules are read
- **THEN** the padding is `var(--space-block) var(--space-group)`
- **AND** from `@media (min-width: 40rem)` it is `var(--space-page)`

### Requirement: R26 Control borders have 3:1 contrast

`--color-control-border` SHALL have a contrast of at least 3:1 against every background it
can sit on, and the text pairs of the design code SHALL meet their ratios. Edges a person
operates SHALL use the control border; decorative edges SHALL use `--color-divider`, below
3:1 by design (scenarios).

#### Scenario: web-client/R26-S1 The control border passes 3:1 on every background
- **GIVEN** the colour values of `tokens.css`, read as text
- **WHEN** the contrast ratio of `--color-control-border` against each of `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail`, `--color-accent-subtle`, `--color-neutral-background`, `--color-success-background` and `--color-danger-background` is computed
- **THEN** every ratio is at least 3

#### Scenario: web-client/R26-S2 Control edges and decorative edges use their own tokens
- **GIVEN** the CSS of Field, Button, IssuedLink, List, Dialog and AppHeader, read as text
- **WHEN** their border rules are read
- **THEN** the text input, the select, the password toggle, the secondary button and the issued-link input use `--color-control-border`
- **AND** the List item, the IssuedLink box and the Dialog use `--color-divider`, and the AppHeader has a bottom border of `--border-width` solid `--color-divider`
- **AND** the field error border uses `--border-width-strong` and `--color-danger-text` (web-client R30)

#### Scenario: web-client/R26-S3 Text pairs meet their ratios
- **GIVEN** the colour values of `tokens.css`, read as text, and the contrast helper of R26-S1
- **WHEN** the ratios of the pairs listed in the design-code "Colour" section are computed
- **THEN** `--color-text` and `--color-text-muted` on `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail` and `--color-accent-subtle` are at least 4.5
- **AND** `--color-accent` (link text) on `--color-surface` and on `--color-rail`, `--color-on-accent` on `--color-accent` and on `--color-danger-action`, and the neutral, success and danger text on their own backgrounds are at least 4.5

### Requirement: R27 Button roles and states

`Button` SHALL have the variants `primary`, `secondary`, `quiet`, `dangerText`, `muted`
and `danger`, and no green variant. "Add teacher" (with its plus icon) and "Create
teacher" SHALL be the primary variant (accent fill with `--color-on-accent` text). The
danger variant SHALL fill with `--color-danger-action`. The row "Deactivate" button SHALL
stay a filled danger button until the admin row actions are restyled. The secondary
variant SHALL have `--color-text` text. Every variant SHALL have hover (only under `@media (hover: hover)`), disabled and focus states, and an active state except the danger fill, made only of design tokens. The `icon` prop SHALL accept any icon of the
shared set, and a button that leads with an icon SHALL start its padding
`--button-icon-inset` closer to the edge than `--space-group`. A full-width variant for
phones SHALL be used only by the lone submit button of the login and set-password pages.

#### Scenario: web-client/R27-S1 Add teacher and Create teacher are the accent primary
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button and, in the open dialog, the "Create teacher" button are read by role and name
- **THEN** each has the primary class of `Button`, and not any other variant class
- **AND** the primary rule fills with `--color-accent` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S2 There is no success variant
- **GIVEN** `Button.tsx` and `Button.module.css`, read as text
- **WHEN** the variants are read
- **THEN** the variant type is `'primary' | 'secondary' | 'quiet' | 'dangerText' | 'muted' | 'danger'`
- **AND** the CSS has no `.success` rule and no token starting `--color-action-success`

#### Scenario: web-client/R27-S3 The danger rule fills with the danger action colour
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the `.danger` rule is read
- **THEN** it fills and edges with `--color-danger-action` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S4 The row Deactivate button stays a danger button
- **GIVEN** an admin page with an active teacher
- **WHEN** the row "Deactivate" button is read by role and name
- **THEN** it has the danger class of `Button`

#### Scenario: web-client/R27-S5 The new variants have their own look
- **GIVEN** `Button.tsx` and `Button.module.css`, read as text, and a `Button` rendered with each of `quiet`, `dangerText` and `muted` in a unit test (no screen uses them yet)
- **WHEN** each button's classes and the CSS rules are read
- **THEN** each carries its own variant class and no other, and the label is shown
- **AND** `quiet` is transparent with `--color-text` text and the `--color-surface-sunken` hover fill, `dangerText` has `--color-danger-text` text and a `--color-danger-background` hover fill, and `muted` has `--color-text-muted` text that goes to `--color-text` on hover; both have an active fill of `--color-neutral-background`

#### Scenario: web-client/R27-S6 The secondary button has text colour
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the `.secondary` rule is read
- **THEN** its text colour is `--color-text`, its fill `--color-surface` and its border `--color-control-border`
- **AND** no rule of a variant other than `primary` uses `--color-accent` as a text colour

#### Scenario: web-client/R27-S7 Every button state is a token
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the base, hover, active and disabled rules are read
- **THEN** the base has `min-height: var(--control-height)`, `padding: 0 var(--space-group)`, `font-weight: var(--weight-strong)` and `line-height: var(--line-height-tight)`
- **AND** hover (on an enabled button only) is `--color-accent-hover` for primary, `--color-surface-sunken` for secondary and quiet, and `--color-danger-action-hover` for danger; active is `--color-accent-active` for primary and `--color-neutral-background` for secondary, quiet, `dangerText` and `muted`, and the danger fill has no active step; primary and danger change fill and border together on hover and active, and every hover rule is inside `@media (hover: hover)`
- **AND** disabled is `opacity: var(--opacity-disabled)` with `cursor: not-allowed`, and the transitions use `--duration-fast` on `background-color`, `border-color`, `color` and `opacity` only

#### Scenario: web-client/R27-S8 The icon prop takes any icon and insets an icon-led button
- **GIVEN** `Button` rendered with `icon` set to the plus and to other icons of the shared set (a unit test), and `Button.module.css` read as text
- **WHEN** the markup and the rules are read
- **THEN** each icon is the first child, `aria-hidden="true"` and `focusable="false"`, with its own path from the icon table, and the button has the icon-led class; a button without an icon has no such class
- **AND** the icon-led rule has `padding-inline-start: calc(var(--space-group) - var(--button-icon-inset))`, written with logical properties and without `:has`, and the gap stays `--space-inline`
- **AND** "Add teacher" has the icon-led class and "Create teacher" does not

#### Scenario: web-client/R27-S9 The full-width variant is for the lone submit on phones
- **GIVEN** `Button.module.css` and the TSX files under `frontend/src`, read as text
- **WHEN** the full-width rule and its uses are read
- **THEN** the rule is inside `@media (max-width: 63.999rem)` and makes the button fill its row, and above that width the button keeps its natural width
- **AND** only the submit buttons of `LoginPage` and `SetPasswordPage` use it, and the `ButtonRow` rules are unchanged
