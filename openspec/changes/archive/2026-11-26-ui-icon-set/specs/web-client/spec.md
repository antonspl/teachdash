# Spec Delta

## ADDED Requirements

### Requirement: R21 Shared icon set

The web client SHALL have one shared set of inline SVG icons: one component for each icon
of the design-code icon table and none beyond it. Each icon SHALL carry the shared
attributes on its root `svg`, SHALL have the table's shapes (scenarios), SHALL be hidden
from assistive technology, and has no title and no accessible name of its own. A change to
the design-code icon table needs a spec change.

#### Scenario: web-client/R21-S1 Every icon of the table exists and none is extra
- **GIVEN** the icon table of the design code
- **WHEN** the icon components are listed
- **THEN** there is one component for each icon of the table, named in PascalCase from the table name with the suffix `Icon`
- **AND** there is no other icon component

#### Scenario: web-client/R21-S2 Every icon has the shared attributes on its root svg
- **GIVEN** each icon is rendered
- **WHEN** its root element is read
- **THEN** it is an `svg` with `viewBox` `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round line caps and joins, `aria-hidden="true"` and `focusable="false"`
- **AND** it has no `title` element and no `aria-label`
- **AND** these shared attributes are not repeated on its shapes

#### Scenario: web-client/R21-S3 Every icon's shapes equal the table
- **GIVEN** each icon is rendered and the test holds one literal list per icon, copied by hand from the design-code table in the table's element order, each element as a tag and its attributes (`d` for paths; `cx`, `cy`, `r` for circles; `x`, `y`, `width`, `height`, `rx` for rects)
- **WHEN** the icon's elements are read
- **THEN** they equal its list in number, order, tags and attributes, with `d` compared as an exact string
- **AND** the elements carry no attributes beyond those listed, except the `fill` and `stroke` overrides of `play` and `more` (`fill="currentColor"`, `stroke="none"`) and the `stroke-dasharray="2.5 2.5"` of the `todo` circle

### Requirement: R22 Icon size tokens

The web client SHALL define the icon sizes as the tokens `--icon-size` (1.25rem),
`--icon-size-small` (1rem) and `--icon-size-rail` (1.5rem) in `tokens.css`. An icon
component SHALL take a size of `default`, `small` or `rail`, `default` when none is given,
and its width and height SHALL come only from the token of that size, never from the
font size.

#### Scenario: web-client/R22-S1 The three tokens have the specified values
- **GIVEN** `frontend/src/styles/tokens.css`, read as text
- **WHEN** the icon size tokens are read
- **THEN** `--icon-size` is `1.25rem`, `--icon-size-small` is `1rem` and `--icon-size-rail` is `1.5rem`

#### Scenario: web-client/R22-S2 The size prop selects the size class
- **GIVEN** an icon is rendered with no size, with `default`, with `small` and with `rail`, and the icon CSS module is read as text
- **WHEN** its root element's class is read
- **THEN** no size and `default` give the same class, and `small` and `rail` each give a different one
- **AND** each size class (`default`, `small`, `rail`) sets its width and height from its own token (`--icon-size`, `--icon-size-small`, `--icon-size-rail`) and contains no `em`

#### Scenario: web-client/R22-S3 The plus on Add teacher is 20 pixels
- **GIVEN** a logged-in admin on `/admin`, on the desktop and phone projects
- **WHEN** the plus on "Add teacher" is measured by its computed style
- **THEN** its width and its height are 20 pixels
- **AND** on the phone project at 360 pixels `/admin` has no horizontal scroll

### Requirement: R23 Icons on buttons

The plus of "Add teacher" SHALL use the set's plus (`M10 4v12M4 10h12`), SHALL be the first
child of the button before the label, and SHALL be `aria-hidden="true"` and
`focusable="false"`. Icons SHALL NOT appear on primary, form or dialog buttons, except a
create action's plus; "Create teacher" (the dialog's submit) SHALL have no icon.

#### Scenario: web-client/R23-S1 The plus of Add teacher uses the set's plus
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button is read by role and name
- **THEN** its first child is an `svg` that is `aria-hidden="true"` and `focusable="false"`, followed by the label
- **AND** its path is `M10 4v12M4 10h12`

#### Scenario: web-client/R23-S2 Create teacher and the other dialog buttons have no icon
- **GIVEN** a logged-in admin with the "Add teacher" dialog open
- **WHEN** the "Create teacher" button and the other buttons of the dialog are read by role and name
- **THEN** none of them contains an `svg`
