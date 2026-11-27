# Spec Delta

## ADDED Requirements

### Requirement: R24 Design tokens

`frontend/src/styles/tokens.css` SHALL declare exactly the design-code token tables, with
their names and values, and no other custom property. Titles and headings SHALL use the
type tokens and `--weight-strong`, not raw sizes or weights, and the Page padding SHALL
follow the space roles (scenarios).

#### Scenario: web-client/R24-S1 tokens.css declares exactly the token names
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of 89 design-code token names copied by hand from the "Tokens" tables (82 table rows; a row such as `--color-neutral-background` / `-text`, `--color-danger-action` / `-hover`, `--weight-regular` / `--weight-strong`, `--border-width` / `--border-width-strong` or `--side-panel-min` / `--side-panel-max` gives two names)
- **WHEN** its custom property definitions are listed
- **THEN** every name of the list is defined
- **AND** no other name is defined

#### Scenario: web-client/R24-S2 tokens.css has the design-code values
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of the design-code values copied by hand, including `--control-height` and `--row-action-size` under `@media (pointer: fine)` and `--type-label-width` under `:root:lang(ru)`
- **WHEN** each token's value is read
- **THEN** it equals the list's value, with `--radius` `0.25rem`, `--text-large` `1.25rem`, `--text-title` `1.5rem`, `--line-height` `1.45`, `--line-height-tight` `1.2` and `--control-height` `2.75rem` (`2.5rem` under `@media (pointer: fine)`)

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

### Requirement: R25 No removed or undefined token

No file under `frontend/src` SHALL name a removed token: `--color-border`,
`--color-action-success-*`, `--shadow-card`, `--space-1` to `--space-4` or `--arrow-width`.
Every `var(--name)` in `src/**/*.css` and in the TSX files SHALL name a token that
`tokens.css` defines. `npm run lint` SHALL fail on a `var(--name)` that `tokens.css` does
not define.

#### Scenario: web-client/R25-S1 No removed token name is left
- **GIVEN** every `.css`, `.ts` and `.tsx` file under `frontend/src` except the test files (`*.test.ts` and `*.test.tsx`), read as text
- **WHEN** they are searched for the removed names
- **THEN** none of `--color-border`, `--color-action-success`, `--shadow-card`, `--space-1`, `--space-2`, `--space-3`, `--space-4` or `--arrow-width` occurs

#### Scenario: web-client/R25-S2 Every referenced token is defined
- **GIVEN** every `var(--name)` in `frontend/src/**/*.css` and the non-test TSX files (test files are excluded), and the names defined in `tokens.css`
- **WHEN** each referenced name is looked up
- **THEN** it is defined in `tokens.css`

#### Scenario: web-client/R25-S3 Lint fails on an undefined token
- **GIVEN** a temporary folder that the test writes at run time, laid out like `src` (`styles/tokens.css` and a CSS file that reads an undefined token, the name built in the test so no source file under `src` contains the literal `var(--...)`)
- **WHEN** `frontend/scripts/check-design-tokens.mjs` runs with that folder as its argument
- **THEN** it exits with a non-zero code and its stderr names the file (relative to the folder), the line and the token
- **AND** with the name defined in the folder's `tokens.css` it exits with code 0

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
- **AND** the field error border still uses `--color-danger-text`

#### Scenario: web-client/R26-S3 Text pairs meet their ratios
- **GIVEN** the colour values of `tokens.css`, read as text, and the contrast helper of R26-S1
- **WHEN** the ratios of the pairs listed in the design-code "Colour" section are computed
- **THEN** `--color-text` and `--color-text-muted` on `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail` and `--color-accent-subtle` are at least 4.5
- **AND** `--color-accent` (link text) on `--color-surface` and on `--color-rail`, `--color-on-accent` on `--color-accent` and on `--color-danger-action`, and the neutral, success and danger text on their own backgrounds are at least 4.5

### Requirement: R27 Button roles without a green variant

`Button` SHALL have the variants `primary`, `secondary` and `danger` only. "Add teacher"
(with its plus icon) and "Create teacher" SHALL be the primary variant (accent fill with
`--color-on-accent` text). The danger variant SHALL fill with `--color-danger-action`. The
row "Deactivate" button SHALL stay a filled danger button.

#### Scenario: web-client/R27-S1 Add teacher and Create teacher are the accent primary
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button and, in the open dialog, the "Create teacher" button are read by role and name
- **THEN** each has the primary class of `Button`, and not any other variant class
- **AND** the primary rule fills with `--color-accent` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S2 There is no success variant
- **GIVEN** `Button.tsx` and `Button.module.css`, read as text
- **WHEN** the variants are read
- **THEN** the variant type is `'primary' | 'secondary' | 'danger'`
- **AND** the CSS has no `.success` rule and no token starting `--color-action-success`

#### Scenario: web-client/R27-S3 The danger rule fills with the danger action colour
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the `.danger` rule is read
- **THEN** it fills and edges with `--color-danger-action` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S4 The row Deactivate button stays a danger button
- **GIVEN** an admin page with an active teacher
- **WHEN** the row "Deactivate" button is read by role and name
- **THEN** it has the danger class of `Button`

### Requirement: R28 No card shadows, shared select chevron

Only overlays SHALL use a shadow, and only `--shadow-overlay`; no CSS file under `src`
SHALL define a `box-shadow` for the List item, StatusMessage, AppHeader or IssuedLink. The
chevron of every `Select` SHALL be the shared `ChevDownIcon` at `--icon-size`, in
`--color-text-muted`, inside the existing `data-select-arrow` box, which stays inert to the
pointer.

#### Scenario: web-client/R28-S1 No card has a shadow
- **GIVEN** the CSS files under `frontend/src/ui`, read as text
- **WHEN** their `box-shadow` declarations are read
- **THEN** none exists, or each one is `var(--shadow-overlay)`
- **AND** the List item, the StatusMessage, the AppHeader and the IssuedLink rules have no `box-shadow`

#### Scenario: web-client/R28-S2 The select chevron rules
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the arrow and select rules are read
- **THEN** the arrow rule has `pointer-events: none`, colour `--color-text-muted` and `right: calc(var(--border-width) + var(--space-group))`
- **AND** the select rule has `appearance: none` and `padding-right: calc(var(--space-group) + var(--icon-size) + var(--space-inline))`
- **AND** the file has no `.arrowIcon` rule

#### Scenario: web-client/R28-S3 The select chevron is the shared icon
- **GIVEN** a rendered `Select` (the create-teacher dialog's time zone select, the profile time zone, the language select)
- **WHEN** the `[data-select-arrow]` element is read
- **THEN** it is `aria-hidden="true"` and contains the `svg` of `ChevDownIcon` (path `M5 8l5 5 5-5` as in the icon table) with the default size class, and no other `svg`

#### Scenario: web-client/R28-S4 The chevron renders at the icon size
- **GIVEN** a logged-in admin with the "Add teacher" dialog open, on the desktop and phone projects
- **WHEN** the chevron in the time zone select is measured by its computed style
- **THEN** its width and its height are 20 pixels
