# Spec Delta

## ADDED Requirements

### Requirement: R21 Shared icon set

The web client SHALL have one shared set of inline SVG icons: one component for each icon
of the design-code icon table and none beyond it. Each icon SHALL have the shared root
attributes and the table's shapes (scenarios), SHALL be hidden from assistive technology,
and has no title and no accessible name of its own. The plus of "Add teacher" SHALL use
the set's plus and stay before the label; "Create teacher" and the other buttons SHALL
have no icon.

#### Scenario: web-client/R21-S1 Every icon of the table exists and none is extra
- **GIVEN** the icon table of the design code
- **WHEN** the icon components are listed
- **THEN** there is one component for each icon of the table, named in PascalCase from the table name with the suffix `Icon`
- **AND** there is no other icon component

#### Scenario: web-client/R21-S2 Every icon has the shared root attributes
- **GIVEN** each icon is rendered
- **WHEN** its root element is read
- **THEN** it is an `svg` with `viewBox` `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round line caps and joins, `aria-hidden="true"` and `focusable="false"`
- **AND** it has no `title` element and no `aria-label`

#### Scenario: web-client/R21-S3 Every icon's shapes equal the table
- **GIVEN** each icon is rendered
- **WHEN** its paths and shapes are read
- **THEN** they equal the path data, circles and rectangles of the design-code table for that icon
- **AND** `play` and `more` have `fill="currentColor"` and `stroke="none"` on their elements, and `todo` has `stroke-dasharray="2.5 2.5"` on its circle

#### Scenario: web-client/R21-S4 The plus of Add teacher uses the set's plus
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button is read
- **THEN** its first child is an `svg` that is `aria-hidden="true"` and `focusable="false"`, followed by the label
- **AND** its path is `M10 4v12M4 10h12`

#### Scenario: web-client/R21-S5 Other buttons stay icon-free
- **GIVEN** a logged-in admin with the "Add teacher" dialog open
- **WHEN** the "Create teacher" button and the other buttons without an `icon` are read
- **THEN** none of them contains an `svg`

### Requirement: R22 Icon size tokens

The web client SHALL define the icon sizes as the tokens `--icon-size` (1.25rem),
`--icon-size-small` (1rem) and `--icon-size-rail` (1.5rem) in `tokens.css`. An icon
component SHALL take a size of `default`, `small` or `rail`, `default` when none is given,
and its width and height SHALL come only from the token of that size, never from the
font size.

#### Scenario: web-client/R22-S1 The three tokens have the specified values
- **GIVEN** `frontend/src/styles/tokens.css`
- **WHEN** the icon size tokens are read
- **THEN** `--icon-size` is `1.25rem`, `--icon-size-small` is `1rem` and `--icon-size-rail` is `1.5rem`
- **AND** there is no `--icon-size-placeholder`

#### Scenario: web-client/R22-S2 The size prop selects the size class
- **GIVEN** an icon is rendered with no size, with `default`, with `small` and with `rail`
- **WHEN** its root element's class is read
- **THEN** no size and `default` give the same class, and `small` and `rail` each give a different one

#### Scenario: web-client/R22-S3 The plus on Add teacher is 20 pixels
- **GIVEN** a logged-in admin on `/admin`, on the desktop and phone projects
- **WHEN** the plus on "Add teacher" is measured by its computed style
- **THEN** its width and its height are 20 pixels
