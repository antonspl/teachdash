# Design code

The chosen visual direction for the teachdash web client (issue #171, part 2), and the order in which it reaches the code. It ranks above `project-rules.md` (README, C0): where the two differ, this file wins, and "Departures from the project rules" names each difference. The mockup it was chosen from is not part of the repository and nothing here depends on it; every value and rule is written out here. Where the mockup did something that no one has decided as a rule, the text says "in the chosen mockup: ...; decided in <issue>", and a builder or component issue decides it.

Contents: Direction; Tokens; Colour; Type; Space and shape; States; Buttons, icons and tooltips; Components; Layout and screens; Copy and languages; Accessibility; Departures from the project rules; Deferred; Redesign plan.

## Direction

Base "r3": the layout of variant "Mulberry" with the colours of variant "Ink". A calm working tool, not a playful one (product brief): petrol ink on cool paper, a rail on the left, compact controls, flat surfaces with thin dividers.

- One accent, petrol `#155770`, used rarely, for actions and place, never for status: primary buttons, links, the focus ring, the current-place and selected bars. Everything else is text colour, muted text and neutral fills. Item type icons are muted.
- "Current place" = in the rail, the current page is a white tab plus a 3px accent bar; in the tree, the current lesson is a white tab only and the current part is the 3px accent bar over the guide line only. "Selected" (an item row in the builder) = `accent-subtle` fill plus the same 3px bar. The 3px bar is reserved for these two meanings and is never used as decoration.
- Dark mode is out of scope.
- Status is never colour alone: success, neutral and danger always carry an icon and a word.
- No dead space (see Layout): data tables and the builder reach the edge of the main area, the editor takes the remaining width, a text page has a text column and a side panel. The one exception is the white space around lesson text at 1920: it stays.

## Tokens

All values live in `frontend/src/styles/tokens.css` and are the only place custom properties are defined (project rules, base rules). Names are roles, not values.

### Colour

| Token | Value | Role |
|---|---|---|
| `--color-background` | `#f3f8fa` | page; builder outline pane |
| `--color-surface` | `#ffffff` | rows, inputs, editor pane, dialog, reading area |
| `--color-surface-sunken` | `#e9f0f4` | hover fill, read-only boxes, preview box, table row hover |
| `--color-rail` | `#e1eaef` | rail, phone title bar, lesson tree pane, login band |
| `--color-text` | `#152127` | text; tooltip fill |
| `--color-text-muted` | `#4b5c65` | secondary text, type icons, hints |
| `--color-accent` | `#155770` | primary button, links, focus ring, bars |
| `--color-accent-hover` | `#04465d` | primary hover |
| `--color-accent-active` | `#00394d` | primary pressed |
| `--color-on-accent` | `#ffffff` | text on accent and on danger-action |
| `--color-accent-subtle` | `#d9ebf4` | selected row |
| `--color-control-border` | `#6b808b` | edges a person operates: inputs, selects, secondary buttons, menu and picker edges (at least 3:1) |
| `--color-divider` | `#d3dce1` | dividers between rows, panes and bars; the edge of builder item rows; the progress track. An edge a person must find to operate uses `--color-control-border` |
| `--color-tree-guide` | `#9db0ba` | the nesting line under a lesson's parts |
| `--color-focus-ring` | `var(--color-accent)` | focus outline |
| `--color-neutral-background` / `-text` | `#e3e9ec` / `#283840` | neutral badge and banner; pressed quiet button |
| `--color-success-background` / `-text` | `#dcf1e3` / `#135a31` | success |
| `--color-danger-background` / `-text` | `#fbe4e1` / `#981d15` | danger badge, banner, error text and error border |
| `--color-danger-action` / `-hover` | `#a8261b` / `#8c1c13` | danger button fill |
| `--color-backdrop` | `rgb(21 33 39 / 50%)` | dialog backdrop |

### Type

| Token | Value | Use |
|---|---|---|
| `--font-body` | `system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif` | all text (project rules C4) |
| `--text-small` | `0.8125rem` | the smallest size, never undercut: metadata, hints, table headers, badges, rail labels, item type names |
| `--text-base` | `1rem` | default |
| `--text-reading` | `1.0625rem` | student lesson and homework text, the builder preview |
| `--text-large` | `1.25rem` | dialog title and section heading, course name, item editor title, lesson text headings, phone title bar, question numbers |
| `--text-title` | `1.5rem` | page title |
| `--weight-regular` / `--weight-strong` | `400` / `600` | the only two weights |
| `--line-height` | `1.45` | default |
| `--line-height-small` | `1.4` | with `--text-small` |
| `--line-height-tight` | `1.2` | titles, buttons, rail labels |
| `--line-height-large` | `1.3` | with `--text-large` |
| `--line-height-reading` | `1.6` | reading text |

### Space (named by role)

| Token | Value | Use |
|---|---|---|
| `--space-adjacent` | `0.25rem` | parts of one thing: label and field, buttons in a row group |
| `--space-inline` | `0.5rem` | siblings on one line: words, chips, a control and its note |
| `--space-group` | `0.75rem` | inside a group: row padding, narrow page gutter |
| `--space-block` | `1.25rem` | between blocks of a group: fields, pane padding |
| `--space-section` | `2rem` | between sections |
| `--space-page` | `2rem` | page gutter on desktop (narrow: `--space-block` vertical by `--space-group` horizontal, see Layout) |
| `--space-button-gap` | `0.75rem` | between buttons side by side |

### Shape and motion

| Token | Value | Use |
|---|---|---|
| `--radius` | `0.25rem` | the one radius |
| `--border-width` / `--border-width-strong` | `1px` / `2px` | edges; focus ring and error border |
| `--selection-bar-width` | `3px` | current place and selected thing only |
| `--focus-offset` | `2px` | focus ring offset |
| `--opacity-disabled` | `0.6` | disabled controls |
| `--shadow-overlay` | `0 6px 20px rgb(21 33 39 / 20%)` | overlays only: dialog, tooltip, menu, picker |
| `--duration-fast` | `120ms` | colour, background and opacity transitions |
| `--tooltip-delay` | `400ms` | hover delay before a tooltip shows (the mockup's value; decided in the Tooltip issue) |

### Controls and icons

| Token | Value | Use |
|---|---|---|
| `--control-height` | `2.75rem`; `2.5rem` where `@media (pointer: fine)` | buttons, inputs, selects, menu items |
| `--row-action-size` | `var(--control-height)`; `2rem` where `pointer: fine` | row icon buttons, tree chevrons, tree part rows |
| `--icon-size` | `1.25rem` | icons |
| `--icon-size-small` | `1rem` | icons inside badges, tree chevrons, field errors |
| `--icon-size-rail` | `1.5rem` | rail icons |
| `--icon-size-placeholder` | `3rem` | image placeholder |

### Layout

| Token | Value | Use |
|---|---|---|
| `--rail-width` | `5rem` | desktop rail |
| `--rail-item-height` | `4.5rem` | rail item |
| `--tree-width` | `14rem` | lesson tree pane (fixed) |
| `--outline-width` | `24rem` | builder outline pane |
| `--content-width` | `40rem` | forms and text pages |
| `--reading-width` | `40rem` | lesson and homework text column |
| `--list-width` | `72rem` | admin list rows and page header |
| `--home-width` | `44rem` | student home list and header |
| `--form-width` | `22rem` | login and set-password form |
| `--dialog-width` | `28rem` | dialog |
| `--picker-width` | `20rem` | item type picker |
| `--menu-width` | `15rem` | profile menu |
| `--tooltip-max-width` | `16rem` | tooltip |
| `--lang-select-width` | `9rem` | language select |
| `--answer-width` | `10ch` | inline answer input |
| `--type-label-width` | `3rem`; `5.5rem` under `:root:lang(ru)` | the short item type name column (longest name per language) |
| `--topbar-height` | `3.5rem` | phone title bar, login band |
| `--tabbar-height` | `4rem` | phone tab bar |
| `--action-bar-height` | `calc(var(--control-height) + 2 * var(--space-group))` | sticky action bar |
| `--side-panel-min` / `--side-panel-max` | `15rem` / `24rem` | lesson and homework side panel |
| `--side-panel-now` | `clamp(var(--side-panel-min), calc(100cqi - var(--reading-width) - 2 * var(--space-page)), var(--side-panel-max))` | panel width inside the main container |
| `--list-gutter` | `max(var(--space-page), calc(100cqi - var(--space-page) - var(--list-width)))` | right gutter of a full-width list row |
| `--editor-column-min` | `24rem` | the mockup's editor two-column layout (decided in the builder issues #116-#118) |
| `--progress-height` | `0.375rem` | lesson step bar |
| `--tree-indent` | `calc(var(--space-adjacent) + var(--row-action-size) / 2)` | the guide line sits under the centre of a lesson's chevron |
| `--login-top-offset` | `12vh` | login form top, fixed per window (the mockup's value; decided in the login issue) |
| `--name-min-width` | `8rem` | admin name on phones |
| `--underline-offset` | `0.15em` | link underline |
| `--size-visually-hidden` | `1px` | visually hidden text |

Media and container conditions are not tokens (the lint skips `@media` and `@container` conditions, because `var()` does not work in them): wide is `@media (min-width: 64rem)`, narrow is `@media (max-width: 63.999rem)`; compact sizes use `@media (pointer: fine)` (the touch size is the default); the tree is a drawer between `64rem` and `79.999rem` (below `64rem` there is no drawer: the back button leads to the course screen; see Builder).

The other conditions in the chosen mockup: `@media (hover: none)` (tooltips), `@media (prefers-reduced-motion: reduce)` (transitions), `@media (max-width: 63.999rem) and (pointer: coarse)` (the sticky editor's scroll padding), which is not carried into this design code: the rule on sticky and fixed bars in Accessibility replaces it at every width and pointer type; and `@container main (max-width: 56rem)`, which is in the mockup only: it is unreachable on the admin pages at `64rem` and wider (they have no lesson tree, so the main container is always at least `59rem` wide there) and is not carried into this design code, because English admin actions never wrap on wide.

Two containers:

- `container: main / inline-size` on the app main area (the page area right of the rail; the lesson tree is not part of it); the app frame issue (plan step 5a, #201) sets it. Its queries: `@container main (min-width: 59rem)` for the side panel (text column plus the page gutters plus `--side-panel-min`) and `@container main (max-width: 72rem)` for Russian admin actions. Tokens that use `cqi` for it: `--side-panel-now` and `--list-gutter`.
- `container: editor / inline-size` on the builder editor. Its query in the mockup: `@container editor (min-width: 50rem)` (two `--editor-column-min` plus `--space-section`) for the two-column layout with the sticky preview. `--editor-column-min` is used only in that condition's arithmetic and in the column tracks.

A custom property whose value uses `cqi` is not resolved where it is defined (in `tokens.css`, at `:root`): it is substituted and resolved at the point of use, against the nearest container of the element that reads it. `cqi` measures the nearest ancestor that is a size container, whatever its name. So `--list-gutter` and `--side-panel-now` must not be read inside `editor` or any other nested container, and the `main` element must not read them itself.

### Rename map from today's tokens

Done across all `src/ui` CSS in one PR with before and after screenshots; no alias period (the lint would break mid-way otherwise). When a step's `design.md` asks for a before set, the frontend-implementer takes it from `main` before the first slice, the same states by the same method as the after set, into `frontend/screenshots/<change-name>/before/` (an exception in its role, `.agents/roles/frontend-implementer.md`). Map each use by its role, not by its value: some names keep their name and change meaning.

- `--color-border` splits: `--color-control-border` for fields, selects, secondary buttons, the password toggle, the issued-link input, menu and picker edges; `--color-divider` for rows, panes, bars and card edges. This folds #168.
- `--color-action-success-*` are removed, and with them the Button `success` variant (green): "Add teacher" and "Create teacher" become the accent primary. The tokens issue (plan step 2) makes this change, because the rename is done in one PR with no aliases.
- A danger button's fill moves from `--color-danger-text` to `--color-danger-action` (and `-hover`).
- `--space-1..4` (0.5, 1, 1.5, 2.5rem) are replaced by the role names above; `--space-button-gap` stays at `0.75rem`.
- Type: map each use by its role.
  - The page title (`Title.module.css`, the h1) takes the new `--text-title` (1.5rem) at every width; it used the old `--text-large` below `40rem` and the old `--text-title` (2rem) from `40rem`, and the width step is dropped. Today this is the only use of the old `--text-title`; the 2rem size has no counterpart.
  - The dialog title (`Dialog.module.css`, `.title`) and the section heading (`Section.module.css`, `.heading`) are h2 and used the old `--text-large` (1.5rem): they keep the name `--text-large`, whose value is now 1.25rem.
  - `--text-small`, `--text-reading`, the two weights and `--line-height-*` are new. The raw `font-weight: 600` in `Field`, `StatusBadge`, `Button`, `AppHeader` and `IssuedLink` becomes `--weight-strong`.
- `--radius` goes from `0.5rem` to `0.25rem`; `--shadow-card` is replaced by `--shadow-overlay`, used on overlays only; `--line-height` goes from 1.5 to 1.45 and `--line-height-tight` from 1.25 to 1.2.
- `--control-height` keeps its name; it is 2.5rem with a mouse. `--arrow-width` is removed, so the tokens issue (plan step 2) also reworks the select chevron in `Field.module.css` (it uses `--arrow-width` for its padding, width and height): the chevron becomes an inline icon at `--icon-size` (see Fields; #86). The rest of the buttons and fields is plan step 3a.
- New: the accent, rail, tree-guide, focus, tooltip, icon and the page, gutter and layout tokens above. A token is still added only when no existing one fits (project rules C3).

## Colour

- Page text on surfaces, `--color-text` and `--color-text-muted` both pass 4.5:1 on surface, background, sunken, rail and accent-subtle (16.4, 7.0; 15.3, 6.5; 14.3, 6.0; 13.5, 5.7; 13.4, 5.7).
- `--color-accent` on surface is 8.0:1 and on the rail 6.6:1: it is safe as link text and as a focus ring (3:1) on every neutral and tinted fill (6.5:1 or more).
- `--color-control-border` is 4.1:1 on surface, 3.9 on background, 3.6 on sunken, 3.4 on the rail: a control edge meets 3:1 wherever it sits. `--color-divider` is below 3:1: it marks dividers between rows, panes and bars, the builder item rows (found by their fill and text, not by their edge) and the progress track; fields, selects, buttons, menu and picker edges use the control border. `--color-tree-guide` sits between them (2.25:1 on surface).
- Pairs, all at least 4.5:1: on-accent on accent 8.0 (hover 10.3, active 12.4); on-accent on danger-action 7.1 (hover 9.2); neutral text on neutral background 9.9; success text on success background 7.0; danger text on danger background 6.9; surface on text (tooltip) 16.4.
- The accent is for actions and place, never for status. Success is green and danger is red, because right and wrong are a domain convention; neutral is the blue-grey. There is no second chromatic role: the focus ring is the accent.

## Type

- Page title: `--text-title`, weight 600, `--line-height-tight`. A page with a count puts it beside the title in muted text (`Teachers 42 teachers`).
- `--text-large` 600: dialog titles and section headings (h2), the course name in the tree, the item editor title, headings inside lesson text, the phone title bar, question numbers.
- Body and field text: `--text-base`. Student reading text (lesson, homework, builder preview): `--text-reading` with `--line-height-reading`.
- Never below `--text-small`. It is the size for hints, table headers, badges, rail labels, item type names, metadata and tooltips; student content is never small.
- Labels are 600 at base size. Headings use `text-wrap: balance`, paragraphs `text-wrap: pretty`. Numbers in columns and counters use `font-variant-numeric: tabular-nums`.
- Hierarchy comes from size, weight and colour, never a second typeface (project rules C4, C23). Sentence case, no all-caps (C1, C2).

## Space and shape

- One radius, one border width, one focus ring. A box is a surface fill plus a divider; a card is not wrapped around everything. Rows in a data table have no outer box (see Layout).
- Space by role: `--space-adjacent` inside a thing, `--space-inline` between siblings on a line, `--space-group` inside a group and as the narrow page gutter, `--space-block` between blocks and as pane padding, `--space-section` between sections; `--space-page` is the desktop page gutter. Parents lay children out with `gap`; components do not carry outer margins.
- Shadow only on overlays: dialog, tooltip, menu, picker. Not on cards, banners, the header or rows. The inline picker on phones is not an overlay and has no shadow.
- Disabled = `--opacity-disabled`, `cursor: not-allowed` and the native `disabled` attribute.
- Transitions: background-color, border-color, color and opacity only, `--duration-fast`. Reduced motion keeps state changes visible as an instant change (project rules C6).
- Buttons never touch fields, cards or each other (`--space-button-gap` between buttons; `--space-adjacent` inside a word-button group). Native parts such as the select arrow are inset like text, by `--space-group`.

## States

The same grammar for every component.

- Focus: `:focus-visible` shows a `--border-width-strong` outline in `--color-focus-ring` at `--focus-offset`, on every interactive element; `outline: none` is never used. Where a row meets a pane edge the outline is inset so it is not clipped: rail items by `calc(-1 * var(--border-width-strong) - var(--focus-offset))` (that is -(2px + 2px)); menu items, tree lesson names, tree parts and picker items by `calc(-1 * var(--border-width-strong))` (-2px).
- Hover: one step. Primary goes to `-hover`. Secondary, quiet and row-action controls take the `--color-surface-sunken` fill (links and text controls on a tinted pane too). Input borders go to `--color-text-muted`.
- Active: one more step (`--color-accent-active`, `--color-neutral-background`), with no layout shift.
- Disabled: see Space and shape.
- Loading (a request in flight): the button is disabled, its label becomes the progressive form ("Saving…", "Sending…", "Signing in…"), `aria-busy="true"`, and its minimum width is kept so the layout does not jump. Page loading is the text "Loading…" in the page body, with the layout space kept: no spinner or skeleton until lists are real.
- Error: an input gets a `--border-width-strong` `--color-danger-text` border (padding reduced by one border width so the text does not move), an alert icon at `--icon-size-small` and the message below it in danger text at `--text-small`; `aria-invalid="true"` and `aria-describedby`. A form-level error is a banner.
- Empty: text that says what is missing and, where there is one, a `src/ui/` action; no image assets (project rules C14).
- Selected / current: `aria-current` (`page` in the rail, `true` for a part or item) plus the look in Direction.

## Buttons, icons and tooltips

### Button roles

- Primary: accent fill, on-accent text; one primary per group. Secondary: surface fill, control border. Quiet: transparent, text colour, hover fill. Danger: `--color-danger-action` fill and on-accent text, only for the destructive confirmation inside a dialog. Danger text: `--color-danger-text`, only for admin account actions such as Deactivate.
- Height `--control-height`, padding `0 var(--space-group)`, weight 600, `--space-inline` between icon and word (`--space-adjacent` and `--space-inline` padding for a compact one). A full-width variant exists for phones.
- Create and page-level quiet actions are icon + word in text colour, including repeated "Add item", "Add lesson", "Add part" and "Add option". Their icon is `plus`.
- Edit actions on part and item heads (Rename, Up, Down, Delete) are words only, in muted text colour (hover: text colour). Move actions are words ("Up", "Down") at every width, 360 included.
- Icon-only buttons only for repeated row actions (Remove option ×) and universal symbols (close, chevrons, back, pager, play, pause, menu). Form buttons, primary buttons, dialog buttons, rail items, status badges and "Send to Anna" keep their text.
- Admin row actions are icon + word at every width, in `--text-small`, icon muted, hover on `--color-surface-sunken` (the one hover step in States), pressed on `--color-neutral-background`, "Deactivate" in danger text with a danger-background hover. On wide screens of the admin pages (no lesson tree) the main container is always at least `59rem`, so English actions sit beside the name and never wrap there; in Russian, when the main container is `72rem` or less, the actions take their own line under the name, still icon + word. On narrow screens they always sit under the name. The width is fixed per language, no measuring script.
- Danger and Delete: danger (red) text is only for admin account actions such as Deactivate; the builder's Delete is a muted, non-red word button. Both always open a confirmation dialog (#116-#118 ship a plain confirmation dialog for Delete; #137 adds the affected-student count; #121), and the danger button appears only for the destructive confirmation inside that dialog. A delete that acts at once is never muted.
- Icons are never on primary, form or dialog buttons, except a create action's `plus`; never an arrow appended to button text. "Add teacher" (accent primary, a create action) keeps its plus; "Create teacher" (the dialog's submit) has no icon.

### Icons

One set of inline SVG components in `src/ui/` (project rules C16; `PlusIcon` is the model). viewBox `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width="1.75"`, round caps and joins, size `--icon-size` (`--icon-size-small` inside badges, `--icon-size-rail` in the rail). Decorative icons carry `aria-hidden="true"` and `focusable="false"`. Path data, as `d` strings unless noted:

| Icon | Geometry |
|---|---|
| plus | `M10 4v12M4 10h12` |
| close | `M5 5l10 10M15 5L5 15` |
| chev-down / chev-right / chev-left | `M5 8l5 5 5-5` / `M8 5l5 5-5 5` / `M12 5l-5 5 5 5` |
| arrow-up / arrow-down | `M10 16V4M5 9l5-5 5 5` / `M10 4v12M5 11l5 5 5-5` |
| trash | `M4 6h12M8 6V4h4v2M6 6l1 10h6l1-10M9 9v4.5M11 9v4.5` |
| pencil | `M4 16l1-4 8-8 3 3-8 8zM11 6l3 3` |
| search | circle cx 9 cy 9 r 5.5 + `M13 13l4 4` |
| link | `M8.5 11.5l3-3M9 6.5l1.5-1.5a3 3 0 0 1 4.2 4.2l-1.5 1.5M11 13.5l-1.5 1.5a3 3 0 0 1-4.2-4.2l1.5-1.5` |
| user-off | circle cx 9 cy 7 r 3 + `M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M13 13l4 4M17 13l-4 4` |
| user-check | circle cx 9 cy 7 r 3 + `M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M12.5 15l2 2 3.5-4` |
| ban | circle cx 10 cy 10 r 7 + `M5 15L15 5` |
| home | `M3.5 9.5L10 4l6.5 5.5V16h-4.5v-4h-4v4H3.5z` |
| book | `M10 6v10M10 6C8.5 4.8 6.5 4.5 4 4.5v10c2.5 0 4.5.3 6 1.5 1.5-1.2 3.5-1.5 6-1.5v-10c-2.5 0-4.5.3-6 1.5z` |
| people | circle cx 7.5 cy 7 r 2.5 + `M3 15.5c.4-2.3 2.2-3.8 4.5-3.8s4.1 1.5 4.5 3.8` + circle cx 13.5 cy 7.5 r 2 + `M13.5 11.7c1.8 0 3.2 1.2 3.5 3.3` |
| profile | circle cx 10 cy 10 r 7 + circle cx 10 cy 8.5 r 2.5 + `M5.5 15.2c.9-1.6 2.6-2.6 4.5-2.6s3.6 1 4.5 2.6` |
| log-out | `M8 4H5v12h3M12 6.5l3.5 3.5-3.5 3.5M15.5 10H8` |
| globe | circle cx 10 cy 10 r 7 + `M3 10h14M10 3c2 2 2.8 4.4 2.8 7s-.8 5-2.8 7c-2-2-2.8-4.4-2.8-7S8 5 10 3z` |
| menu | `M3.5 6h13M3.5 10h13M3.5 14h13` |
| sidebar | rect x 3.5 y 4 w 13 h 12 rx 1.5 + `M8 4v12` |
| t-theory | `M5 3.5h7l3 3v10H5zM12 3.5v3h3M7.5 10h5M7.5 13h5` |
| t-image | rect x 3.5 y 4.5 w 13 h 11 rx 1.5 + circle cx 7.5 cy 8.5 r 1.25 + `M4 14l4-4 3 3 2-2 3.5 3.5` |
| t-pick | rect x 3.5 y 6 w 13 h 8 rx 1.5 + `M11 9.25l1.5 1.5 1.5-1.5M6 10h3` |
| t-type (a text cursor, not a pencil) | `M10 5v10M7 3.5c1.6 0 3 .6 3 1.5 0-.9 1.4-1.5 3-1.5M7 16.5c1.6 0 3-.6 3-1.5 0 .9 1.4 1.5 3 1.5` |
| t-audio | `M4 8h2.5L10 5v10l-3.5-3H4zM13 7.5c.8.7 1.2 1.6 1.2 2.5s-.4 1.8-1.2 2.5M15 5.5c1.3 1.2 2 2.8 2 4.5s-.7 3.3-2 4.5` |
| play (filled: `fill="currentColor"`, `stroke="none"`) | `M7 5v10l8-5z` |
| pause | `M7 5v10M13 5v10` |
| more (filled) | three circles cy 10 r 1.4 at cx 5, 10, 15 |
| check | `M4.5 10.5l3.5 3.5 7.5-8` |
| right | circle cx 10 cy 10 r 7 + `M7 10.2l2 2 4-4.4` |
| wrong | circle cx 10 cy 10 r 7 + `M7.5 7.5l5 5M12.5 7.5l-5 5` |
| clock | circle cx 10 cy 10 r 7 + `M10 6v4l2.5 2` |
| lock | rect x 5 y 9 w 10 h 7.5 rx 1.5 + `M7 9V7a3 3 0 0 1 6 0v2` |
| todo | circle cx 10 cy 10 r 7 with `stroke-dasharray="2.5 2.5"` |
| send | `M3.5 10L16.5 4l-4 12.5-2.5-5.5zM10 11l6.5-7` |
| eye | `M2.5 10s2.8-5 7.5-5 7.5 5 7.5 5-2.8 5-7.5 5-7.5-5-7.5-5z` + circle cx 10 cy 10 r 2.25 |
| info | circle cx 10 cy 10 r 7 + `M10 9v4.5M10 6.5v.01` |
| alert | `M10 3.5l7 12.5H3zM10 8.5v3.5M10 14v.01` |

Meanings are fixed: `t-*` are item types; `right`, `wrong`, `clock` are answer results; `todo`, `send`, `right` are homework status; `lock` is a closed lesson; `user-off`, `user-check`, `link` are admin row actions; `eye` is "Preview as student"; `sidebar` is "Show lessons" (and "Hide" where it exists); `chev-left` is the back button; `profile` is the profile menu; `check` marks the chosen language, a saved state and "just added". Never emoji (C16).

### Tooltips and icon-only buttons

- An icon-only button is a native `<button>` with `aria-label`. The `aria-label` is the tooltip's i18n text plus the object, built from the same key ("Remove option: <the option's text>"); the tooltip shows the key text alone ("Remove option"), so the visible text is the start of the accessible name (WCAG 2.5.3). No `title` attribute.
- The tooltip is an `aria-hidden` sibling of the trigger (the name is already in `aria-label`). It meets WCAG 1.4.13: it can be dismissed without moving focus, it can be hovered, and it stays until dismissed or the trigger is left. It never covers its trigger.
- In the chosen mockup the tooltip showed on hover after 400ms (`--tooltip-delay`) and at once on `:focus-visible`; Escape hid it without moving focus and leaving the trigger reset it; it stayed while the pointer was on the trigger or on the tooltip (they touch); it sat above the trigger, centred, flipped below when there was no room above, slid sideways to stay half a rem inside the window, and sat to the right in the rail and above the item in the phone tab bar. These timings and placements are not rules; they are decided in the Tooltip, IconButton and Menu issue (plan step 4a).
- Position (tooltip, menu and picker): a small helper in `src/ui` sets the position from code with `style.setProperty`, the only inline-style exception (see Departures).
- Look: `--color-text` fill, `--color-surface` text, `--text-small`, `--line-height-small`, padding `--space-adjacent` by `--space-inline`, `--radius`, `--shadow-overlay`, `max-width: var(--tooltip-max-width)`, no arrow.
- On `(hover: none)` the tooltip is not shown, so on a phone an icon-only button is used only for close, chevrons, back, pager, play, pause and menu, and for "Remove option ×" (the option's text sits next to it); everything else shows its word at 360.
- A touch device at `64rem` or wider (an iPad in landscape) shows the wide layout, and its icon-only buttons have no tooltip; their `aria-label` still applies (open, decided in the components issue, plan step 4a).
- A rail item with a visible word has no tooltip. The tooltip is hidden while the trigger's menu is open.

## Components

### Fields

- A field is a label (600), the control, an optional hint (`--text-small`, muted) and an error, with `--space-adjacent` between them; fields in a form are `--space-block` apart.
- Input, textarea and select: height `--control-height` (textarea: padding `--space-inline` and `--space-group`, vertical resize), padding `0 var(--space-group)`, `--border-width` `--color-control-border`, `--radius`, surface fill, text colour; placeholder muted.
- Select: native `<select>` with `appearance: none` and an inset chevron icon (`--icon-size`, muted, `--space-group` from the right, `padding-right` for it) so the arrow sits as far in as the text (#86). A select that leads with an icon (language) has the icon on the left; there the arrow's gap from the right border equals the icon's gap from the left border (both `--space-group`), which redefines the gap of `web-client` R13 for it (plan step 3a). An inline select inside a question uses `--text-reading`.
- A text input with a search icon pads its text past the icon. The password toggle is the word "Show"/"Hide".
- Optional fields get "(optional)" in the label (#165, its own change): the exceptions are marked, not the required fields.
- An input row (input + button) uses `--space-button-gap`.

### Badge

Icon (`--icon-size-small`) + word, `--text-small`, weight 600, padding `--space-adjacent` by `--space-inline`, `--radius`, no wrapping. Tones: neutral, success, danger (the pairs in Colour). Teacher status: Active (success, `right`), Waiting for password (neutral, `clock`), Deactivated (neutral, `ban`). "Students send it" (homework part, `send`) is a neutral badge in the part heading.

### Banner

A block with the tone's fill and text, padding `--space-group`, `--radius`, icon at `--icon-size` and text side by side, no shadow. Error banners are `role="alert"`; success and neutral banners are `role="status"`. The icon is aligned to the first text line.

### Dialog

Native `<dialog>`: width `min(var(--dialog-width), 100% - 2 * var(--space-group))`, no border, `--radius`, surface fill, `--shadow-overlay`, backdrop `--color-backdrop`. Head: the title (h2) and a close icon button pulled `--space-inline` into the right padding; body; foot: buttons right-aligned, `--space-button-gap` apart, stacked in a column when narrow. The safe choice comes first ("Keep active", then "Deactivate" with the danger fill). First focus skips the head close button and goes to the first field, or to the safe choice when there are no fields. Padding is `--space-block`.

### Menu

A popover (profile menu): `--menu-width`, padding `--space-adjacent`, `--border-width` `--color-control-border`, `--radius`, surface fill, `--shadow-overlay`. Items are `--control-height` high, icon + word, hover and focus on the sunken fill. A menu starts with a non-clickable head (the signed-in person's name in 600 and the role in muted `--text-small`); the menu's `aria-labelledby` points at that head. Then come the items, separated by dividers: Profile, the language choice, and Log out last. Opening the menu focuses the first actionable item (not the head); Arrow keys, Home and End move; Tab closes the menu and moves focus on as usual; Escape closes it and returns focus to the trigger; an outside click closes it and leaves focus where the click put it; for a chosen item, focus follows the Accessibility focus rule. In the chosen mockup the language choice was a muted label and `menuitemradio` items inside the menu, a `check` icon on the chosen one, each name in its own `lang`; decided in the app frame issue (plan step 5a).

### Rail and phone tab bar

- Wide: a sticky full-height rail, `--rail-width`, `--color-rail`, a divider on its right. The wordmark ("teachdash", `--text-small` 600, centred, `translate="no"`) sits at the top; items are `--rail-item-height` high, the icon (`--icon-size-rail`, muted) above a `--text-small` label, centred, hyphens auto and `overflow-wrap: break-word` as the fallback for long Russian words. Hover: sunken fill. Current: white surface, 3px accent bar on the left, weight 600, accent icon (`aria-current="page"`).
- Narrow: the rail becomes a fixed bottom tab bar, `--tabbar-height`, a divider on top, the current bar on the top edge; no wordmark. A title bar (`--color-rail`, at least `--topbar-height`, a divider below; it scrolls with the page and is not sticky or fixed, because it wraps and grows) shows the page title (`--text-large` 600, wrapping onto as many lines as it needs, never an ellipsis; the bar grows, at least `--topbar-height`), a small muted parent line above it when there is one, and a back icon button when the page has a parent.
- The rail ships in plan step 5a with one item per role plus the Profile menu; the other tabs arrive with their features. At 5a: admin: Teachers (opens `/admin`) and Profile; teacher and student: a single item carrying the cabinet's name until real tabs arrive (it opens the cabinet page), and Profile. Courses (teacher) is added by #115, Students (teacher) by #35 and Home (student) by #134, each replacing that item; each depends on #201 and #202 (steps 5a and 5b), not #57. The cabinet title (`cabinet.teacher`, `cabinet.student`, as `CabinetPage` shows today) is the page heading (h1) of the teacher and student cabinet pages, not a rail item; `/admin` has one h1, "Teachers" with the count beside it (the page header), and "Admin cabinet" (`cabinet.admin`) is no longer shown there (plan step 6a); desktop has no top bar. The language select moves into the Profile menu once the person is logged in, and stays on the login band while logged out. Below `64rem` the current header, with its Log out and language select, stays until step 5b ships the phone tab bar and title bar and removes it.
- Profile is a menu button (`aria-haspopup="menu"`, `aria-expanded`, `aria-controls`) at every width, with a visible word. The menu opens to the right of the rail item on wide screens and above the tab bar, right edges aligned, on narrow ones. Log out lives only in this menu once the frame is complete. The menu ships with the desktop rail in step 5a, so desktop never lacks Log out; below `64rem` the current header keeps its Log out and language select until step 5b ships the phone tab bar and title bar and removes it.

### Page header

A flex row: the title and the count (muted) on the left, the page action on the right, `--space-block` below. The action is the accent primary ("Add teacher"). Below `64rem` the action sits under the title bar next to the count and wraps under it when long. The header is capped to the same width as its list. Temporary: the language select placement of `ui-i18n-locale` (header, and inside `<main>` on login and set-password, not the login band) stays until #201, #202 and #205.

### Data table

- Semantics: the table always carries explicit ARIA roles, because its component always changes the display of table elements (grid and subgrid on wide screens, block on phones) and Safari then stops exposing it as a table: `role="table"` on the table, `role="rowgroup"` on `thead` and `tbody`, `role="row"` on each `tr`, `role="columnheader"` on header cells, `role="cell"` on data cells. Header cells stay `th` with `scope="col"`. Columns: name with the email under it, status badge, time zone, row actions.
- Wide: one grid whose first and last tracks are the gutters, so a row's divider and hover fill run to the edge of the main area while the content stops at `--list-width`; no outer box, a divider above and below the table and between rows, `--color-surface` rows, hover `--color-surface-sunken`. Spare width is shared name 2 : status 1 : time zone 1. Header cells are `--text-small`, muted, 600. The left edge lines up with the page header.
- Issued link and "Just added": the issued-link box sits in its own row directly after the teacher's row, one cell spanning all columns (`grid-column: 1 / -1` on wide; a block on narrow), with the row and cell roles; its content is aligned with the name column and there is no divider between the teacher's row and its link row. "Just added" is a neutral badge with the `check` icon and the words "Just added", on the name line after the name (it wraps under the name when narrow), on the pinned row at the top.
- Narrow: the header row is visually hidden with the visually hidden technique (never `display: none`, see Semantics), so its cells stay in the accessibility tree; each row is a block with the name and status on the first line, the email and the time zone on the second (time zone on the email line at 360), the actions below; dividers between rows, the table edge to edge.
- Search and paging (a search field with a label, "Page 1 of 5" with previous and next buttons above and below) are part of the direction but ship later with the backend (a separate issue).

### Short link list

For short lists (student home): inset on wide screens, rows on the page background, capped to `--home-width` plus one `--space-group` each side so the text lines up with the headings; dividers and hover as wide as the content; rows have `--radius`. On phones it is a full-width band with dividers. Row: icon, name (600), state on the right (muted, `--text-small`); a closed lesson has a `lock` icon, muted text and "Closed".

### Tree (lesson tree)

Built with the first builder issue (#116/#117). Rail-coloured pane, `--tree-width`, padding `--space-group`. Top: a "Course" label (muted, `--text-small`) above the course name (`--text-large` 600); at `80rem` and wider a "Hide" button (icon `sidebar` + word) on its own line above the name. Then a nested list:

- Lesson row: a chevron button (`aria-expanded`, `aria-controls`, `--icon-size-small`, turns 90 degrees when open) and the lesson name, a link that opens the lesson's first part; a muted state or count on the right. The current lesson is an expanded white tab (surface fill, `--radius`), its name 600.
- Parts: a list indented by `--tree-indent` with a `--border-width` guide line in `--color-tree-guide` on its left; part names start where the lesson name starts; part rows are `--row-action-size` high, muted `--text-small`, hover on the sunken fill and text colour. The current part is text colour, 600, with a 3px accent bar over the guide line (`aria-current`). Below the lessons: a quiet "Add lesson" button (icon + word).
- Closed lessons (student view) are muted with a `lock` icon.

### Builder

Three panes, full height, each scrolling alone: tree (`--tree-width`) | outline (`--outline-width`, `--color-background`) | editor (the rest, surface).

- At `80rem` and wider the tree sits in place. "Hide" (icon `sidebar` + word, only at this width) collapses it in place and a "Show lessons" button replaces it; the same button opens it again.
- Between `64rem` and `79.999rem` the lesson tree is a modal drawer opened by a "Show lessons" button (icon `sidebar` + word), and the course appears as a small muted line above the lesson title. There is no "Hide" at this width. The drawer is a native `<dialog>` opened with `showModal()`, with the backdrop `--color-backdrop` and the rest of the page inert; it has its own close icon button (`close`, "×" at the top of the drawer, a universal symbol). "Show lessons" is under the backdrop while the drawer is open, so it never toggles it.
- Below `64rem` there is no drawer. The builder is two views, the outline and the editor, and to change lesson the back button in the title bar leads to a course screen with the lesson list. Focus: a tap on an item row (outline to editor) focuses the editor's item title (heading, `tabindex="-1"`); the back button of the editor (editor to outline) focuses the item row that was open; the back button of the outline (to the course screen, where it disappears) focuses the current lesson in the course screen's lesson list.
- Focus: opening the drawer focuses the current lesson or part in the tree. The drawer closes on Escape, on a tap or click on the backdrop outside it, on its close icon button and on picking a part; focus then returns to "Show lessons", the button that opened it. Collapsing the tree with "Hide" at `80rem` and wider moves focus to the "Show lessons" button that replaces it; at `80rem` and wider "Show lessons" (shown after "Hide") expands the tree in place and disappears, and focus goes to the current lesson or part in the tree, as when the drawer opens.

- Outline head: the lesson title (`--text-title`, the page title of the builder; on phones it is in the title bar) and "Rename" and "Preview" quiet buttons (icon + word). Parts are `--space-section` apart. A part head: the part name (600) with its item count (`--text-small`, muted) and, for a homework part, the "Students send it" badge; "Delete" (muted word) on the name line at the right; "Rename", "Up", "Down" (muted words) on a second line. On desktop several parts can show their actions at once (up to three): the part holding the selected item, the part with keyboard focus and the part of the last action; an empty part always shows its actions, so they are reachable without a pointer trigger. The focus trigger is keyboard focus only, so a mouse down never adds an action line; on phones every part head shows them. When a selection removes the action line above the row the person clicked, the outline pane's scroll is adjusted so the clicked row stays under the pointer (the scroll compensation stays for selection).
- Item row: a `button` row (no buttons inside): surface fill, divider edge, `--radius`; the type column is the type icon (`--icon-size`, muted) and the short type name (`--text-small`, muted, width `--type-label-width`, so the text of all rows lines up), then the item text (the first line, a gap shown as `___`, the answer options in bold), then metadata on the right (an audio length). Hover: sunken fill. Selected: `--color-accent-subtle` fill and a 3px accent bar over the left edge (in the chosen mockup the type icon also turned accent on the selected row; decided in the builder issues #116-#118). Phones have no selected row. After the rows: a quiet "Add item" button (icon + word) and, after the last part, "Add part".
- Item type picker: `--picker-width` wide, edge `--color-control-border`, `--shadow-overlay` when it floats; each entry is the type icon, the full name (600) and a muted `--text-small` line. "Add item" has `aria-expanded`. In the chosen mockup it was a popover to the right of "Add item", top aligned with it and flipped up when there was no room, on desktop, and an inline panel under the button without a shadow on phones; decided in the item type picker issue (#118). Its roles and keyboard behaviour are open, decided in #118 (in the chosen mockup it was a plain list of buttons with an `aria-label`, and no keyboard rules).
- Editor: head: the type icon (muted), the item title (`--text-large`), "Item n of N in Part" (muted `--text-small`) and the word buttons "Up", "Down", "Delete" (muted); the whole line drops under the title when it does not fit. On phones "Delete" is on the title line at the right and "Up"/"Down" on the line below. Below: the form fields. The student preview is "How students see it", a `--color-surface-sunken` box in `--text-reading`. In the chosen mockup it sat below the fields, and beside them with the preview sticky when the editor was `50rem` or wider (an `@container editor` condition on `--editor-column-min`); decided in #119, the first item editor, which builds the editor footer and the "How students see it" preview. A phone shows the item type's name on a small muted line above the preview, in every language.
- Answer options: a three-column grid (the radio for the right answer, the text input, a Remove icon button ×), a column heading row, then a quiet "Add option" button (icon + word).
- Open, decided in builder issue #116/#117: an empty lesson and an empty course in the tree (the mockup showed none); the tree's disabled and loading states (the mockup showed none). Open, decided in #118: autosave, including its failure state (the mockup showed only "Saved" and "Unsaved changes").
- Editor footer: "Save" (primary), "Discard" (secondary) and a status note ("Saved" with `check`, or "Unsaved changes", `--text-small`, muted). In the chosen mockup it was sticky at the bottom with a divider on top only while pinned over the form; on phones it sat above the tab bar, the buttons shared the width and the status note took its own line above them; decided in #119.

### Lesson and homework (student)

- The lesson pane (the same tree, with the student's current lesson open and closed lessons locked) is a sticky rail-coloured pane beside the text on wide screens; on phones an "In this lesson" disclosure above the text replaces it. #135 uses the tree from #116/#117 and covers only the lesson panel; the homework panel follows #56.
- The text column is `--reading-width`, centred in a white reading area, `--text-reading` on `--line-height-reading`; headings `--text-large`; images in a 4:3 frame (a muted placeholder with a `--icon-size-placeholder` icon until loaded), a caption in muted `--text-base`; audio is a native `<audio controls>` with a "Show transcript" `<details>`.
- Side panel: lesson: progress (a step bar, `--progress-height`, current step in accent, the rest `--color-divider`) and what is next; homework: the question list ("Your answers": number, state, link to the question) and "When you send". It sits beside the text from a main width of `59rem` (text column plus the page gutters plus `--side-panel-min`), at `--side-panel-now` wide (between `--side-panel-min` and `--side-panel-max`), with a left divider and the page background; it is sticky and scrolls inside. Below `59rem` it moves below the text; on a homework page "Your answers" then folds behind a disclosure button, and beside the text it is always open. White space around the text at 1920 stays.
- Homework: questions in a grid `--space-section` apart: a number column of `--control-height` (`--space-block` on phones) with the number in `--text-large` 600, a muted `--text-base` instruction, then the question text; inline answers are an inline select or a short input (`--answer-width`), and the full-sentence answer a textarea. Result lines are icon + word: right (success), not quite (danger), waiting for the teacher (neutral). A listening block is indented with the text and opened by a divider; it never uses the bar.
- Action bar: sticky at the bottom of the text area (above the tab bar on phones), a divider on top, progress and the saved note on the left, "Send to Anna" (primary, text only) on the right.

### Login and set-password

A band at the top (`--color-rail`, `--topbar-height`, divider below): the wordmark in `--text-large` 600 on the left and the language select on the right. The form is centred, `--form-width` wide, with its top at `--login-top-offset` (in the chosen mockup `12vh`, fixed per window, so an error banner grows the form downward only; decided in the login issue, plan step 7); labelled fields, one primary button, the help line muted. On phones the tagline is hidden and the form starts `--space-section` below the band. Temporary: this change puts the language select in the page header, and inside `<main>` on login and set-password (not in this band); that placement stays until #201, #202 and #205.

## Layout and screens

- Wide is `64rem` and up (rail); narrow is below (tab bar, title bar). The page gutter is `--space-page` wide; narrow, the page padding is `--space-block` vertical by `--space-group` horizontal.
- No dead space: a data table's rows reach the edge of the main area and their content is capped at `--list-width`; the builder panes fill the window; the editor takes what is left; a text page is a text column plus a side panel. A page whose content is narrow by nature (login, the short student list) is centred or left-aligned and capped on purpose.
- Two list types: the data table (admin teachers; capped at `--list-width`; left-aligned beside the rail at 1920) and the short link list (student home; `--home-width`).
- Forms and text pages (profile, not found) use `--content-width`; set-password uses `--form-width`, like login.
- At 360 every page fits without horizontal scroll (`specs/web-client` R7). The builder follows the same layout on phones: tree hidden, outline and editor as separate views, item reorder and delete inside the item view.
- Russian runs 20 to 40 percent longer: layouts absorb it with wrapping and fixed per-language widths (`--type-label-width`, the Russian admin actions' `72rem` container condition), not by truncating.

## Copy and languages

- UI copy is neutral, sentence case, second person, no apologies (project rules C1, C10).
- Item type names (builder rows use the short name, the editor and picker the full one): Theory / Theory; Image / Image; Audio / Audio; Pick / Pick from a list; Type / Type the answer. Russian: Теория, Изображение, Аудио, Выбор / Выбор из списка, Ввод / Ответ своими словами. The final names are decided in #118 and #120.
- Russian rail labels are short words chosen for the rail: Курсы, Ученики, Главная, Учителя, Профиль; `hyphens: auto` is only the fallback. Russian uses "Учителя" everywhere (rail, page title, headings). The profile menu role is lowercase ("учитель").
- Quotes follow each language (ru «»); a language switch item shows its own language name in its own `lang`.

## Accessibility

- `aria-current="page"` on the current rail item; `aria-current="true"` on the current part and the selected item; `aria-expanded` and `aria-controls` on every disclosure (tree chevron, "Show lessons" (the drawer button), "Add item", "Your answers", the profile menu button).
- Error banners `role="alert"`, success and neutral `role="status"` (component rule of the banners issue). Icons that carry no information are `aria-hidden`.
- Keyboard: native elements first (project rules C7); the profile menu follows the Menu rules above; the picker's roles and keyboard are open (builder issue #118); Escape hides a tooltip without moving focus; it closes an overlay (menu, drawer, picker, dialog) and returns focus to its opener.
- Target size: controls are `--control-height`; row icon buttons are `2rem` with a mouse, above the 24px floor (WCAG 2.5.8).
- Contrast: every text and control pair in Colour meets WCAG 2.2 AA; focus ring 3:1 on every fill it meets.
- Focus after actions. An item delete moves focus to the next item row in the same part, else the previous item row, else that part's "Add item"; a part delete moves focus to the next part's head (its name heading, `tabindex="-1"`), else the previous part's head, else "Add part". Picking an item type moves focus to the new item's first field (built by #118). Closing the picker with Escape returns focus to its opener. After a Profile menu item: only an item that keeps the person on the page (the language switch) closes the menu and returns focus to the menu button. Profile opens the Profile page and focus goes to that page's heading (h1, `tabindex="-1"`). Log out opens the login page and focus goes to the login page's heading (h1, `tabindex="-1"`). A dialog that closes returns focus to the element that opened it, unless the confirmed action removed that element; then the delete rule applies. When the pressed control disappears, focus moves to a named target, never to `<body>`; a view change focuses the new view's heading unless a rule names another target (picking a part in the drawer returns focus to "Show lessons").
- Every sticky or fixed bottom bar (phone tab bar, homework action bar, builder editor footer) reserves its height on the scrolling container with `scroll-padding-bottom`, at every width and pointer type (WCAG 2.4.11). The phone title bar scrolls with the page, so it needs no scroll padding.
- Content blocks in a lesson carry the course's `lang`; the UI carries the UI language (#102 carries #172's acceptance: the page `lang` is the UI language, with tests).

## Departures from the project rules

These follow from the engineer's decisions; this file ranks above `project-rules.md` (C0). The stale lines in `project-rules.md`, the product brief and `visual-criteria.md` are brought in line in the same PR as this file.

- C5 and C21 (the one `--radius` and `--shadow-card`): the radius is the new `0.25rem`; `--shadow-card` is replaced by `--shadow-overlay` (overlays only); the renames in the rename map are decided. A frontend-implementer may rename as that map says without stopping.
- C9 and C18 (a control is not smaller than `--control-height`): `--control-height` is `2.5rem` with a mouse and `2.75rem` on touch; row icon buttons are `2rem` with a mouse. The 24px floor is still met.
- Base rules ("a font is exactly one `var(...)`", "custom properties only in `tokens.css`") are unchanged.
- Shadow: the vendored sources `refactoring-ui.md` and `refactoring-ui-patterns.md` advise a `--shadow-card` card; this file wins: shadow only on overlays.
- Inline style: project rules forbid it. The one exception is the position helper in `src/ui` for the tooltip, menu and picker, which sets `left`, `top` and the like from code with `style.setProperty`; no other inline style is allowed.
- The product brief's Devices line said teacher editors are designed for desktop; the builder here follows the same layout on phones, and the line is corrected.

## Deferred

- Item type names, including the "verbs" objection to Pick and Type ("Answer" would clash with students' answers): #118, #120.
- The right answer after a wrong pick, blank answers on send, the transcript: #126, #135.
- The homework concept and due dates: #56, #135.
- The roles and reading-list workflow: open, needs the code owner.
- Admin search and paging: a separate issue with a backend change (`GET /api/admin/teachers` is unpaged).
- The "(optional)" marker: its own change (#165).
- Dark mode.
- The mockup-only behaviours listed above and the open builder items: decided in the issues named where each appears.

## Redesign plan

Order of the issues to file after this change. The builder, student home, lesson and homework are not built (#115-#120, #134-#136, #56, #126): they get the rules above, not redesign issues, and the first screen of each follows this file when its feature issue builds it. The feature issues #115-#120, #132, #133, #138, #134 and #135 depend on #196, #197, #199 and #201 (steps 2, 3a, 4a and 5a); the builder issues #115-#120 also depend on #202 (step 5b). The real app today is login, set-password, admin teachers, profile, not found and the cabinet stubs.

0. #102 (locale switch), then #103 (Russian), land first; #102 carries #172's acceptance (the page `lang` is the UI language, with tests); the content language goes with #114.
1. The shared icon set in `src/ui` (only `PlusIcon` exists today), first, because the steps after it use icons. It also adds the new role-named tokens `--icon-size`, `--icon-size-small` and `--icon-size-rail` (project rules C3) to `tokens.css`, and moves `PlusIcon` (sized `1em` in `PlusIcon.module.css` today) to the 20-unit, 1.75-stroke geometry, with before and after screenshots; its `design.md` asks for a before set (see the rename map). The steps after it use icons: the select chevron (step 2), badge and banner with icon + word (3b) and the Dialog close button (3b).
2. Tokens: `frontend/src/styles/tokens.css` and `base.css` to the values above, including the page, gutter and layout tokens (the icon size tokens are already added by step 1); the rename map across `src/ui` CSS in the same PR with before and after screenshots (its `design.md` asks for a before set, see the rename map); folds #168. Because the rename has no aliases, this step also removes the Button `success` variant (green becomes the accent primary), reworks the select chevron (`Field.module.css` uses `--arrow-width`; it becomes an inline icon from step 1; #86) and removes the shadows: dropping `--shadow-card` touches `List`, `StatusMessage`, `AppHeader` and `IssuedLink`, so their shadows go in this step.
3. Primitives:
   - 3a (#197): the rest of buttons (role classes, muted Delete; no green Button variant) and fields. It owns the button Loading state (the busy-label rule: the button reads "Saving…", "Sending…" or "Signing in…" while a request is in flight) and the field error (`aria-invalid`, icon), and it builds the icon-led language select, which redefines `web-client` R13's gap. Specs and tests affected: `web-auth` R1-S4 (`App.test.tsx`, it reads the submit button by its idle name while a login is in flight); `web-profile` R2-S8 and R3-S9 (`ProfilePage.test.tsx`, the same for "Save" and "Change password"); `web-admin-teachers` R2-S5 (`AdminTeachersPage.test.tsx`, `submitButton()` reads the dialog's submit by its idle name); `web-client` R13 and its scenarios R13-S1 to R13-S4 (`selectArrow.spec.ts`). #165 is separate.
   - 3b (#198): badge, banner (icon + text; it owns `role="alert"` on error banners), empty state, "Loading…", Dialog (its close button is built here as a plain button with the icon and `aria-label`; `IconButton` in 4a adopts it later), IssuedLink. Specs and tests affected by `role="alert"`: `web-client` R12 (the login page's "backend cannot be reached" warning is a danger banner; `pages.test.tsx` finds it by role `status`: R12-S2, R12-S4 and the other tests that look for the warning); `web-client` R17 (the "no form message" guards look for `role="status"` and would stay green with an error banner on screen, so each must also check `role="alert"`: R17-S1 in `AdminTeachersPage.test.tsx`; R17-S2 in `App.test.tsx` (two), `SetPasswordPage.test.tsx` and `ProfilePage.test.tsx` (three); R17-S5 to R17-S7 in `fieldErrors.spec.ts`); `web-profile` R2-S6 and R3-S8 (`ProfilePage.test.tsx` finds the error by role `status`).
4. New components:
   - 4a (#199, depends on 3a #197 and 3b #198): Tooltip, IconButton, Menu;
   - 4b (#200, depends on 5a #201): page header and data table components, after 5a (which sets `container: main`) and before 6a.
   The lesson tree comes with #116/#117.
5. App frame:
   - 5a (#201): the desktop rail and the Profile menu (name and role from `/api/me`, no backend change; on desktop Log out and the language select move into the menu and there is no top bar; below `64rem` the current header stays, with its Log out and language select, until 5b). It also builds the rail wordmark and the inset focus ring for rail items, and it owns the decision on the language choice in the Menu and `container: main`; 4b (#200) and 6a (#203) depend on it. Specs and tests affected, which 5a breaks at `64rem` and wider: `web-auth` R3 (the header shows the name, a "Profile" button and a "Log out" button; R3-S1, R3-S2), R4 (R4-S1 to R4-S3, the "Log out" button) and R5-S10 (the teacher's name in the header); `web-profile` R1 (the cabinet header and "Back to cabinet"; R1-S1, and R1-S4 which presses "Back to cabinet") and R2 (its header clauses; R2-S2, R2-S6); `web-client` R17-S6 and R17-S7 (`fieldErrors.spec.ts` presses the header's "Profile"); the e2e `auth.spec.ts`, `profile.spec.ts`, `fieldErrors.spec.ts` and `selectArrow.spec.ts` (they click the header's "Profile" or "Log out"), and the unit tests `App.test.tsx` and `ProfilePage.test.tsx` (header buttons, `headerHas`, R1-S4). On desktop "Back to cabinet" is replaced by the rail's first item (Teachers for admin, the cabinet item for teacher and student), so 5a removes "Back to cabinet" at `64rem` and wider; below `64rem` it stays until 5b;
   - 5b (#202): the phone tab bar and the title bar with the back button; it removes the old header, so Log out and the language select reach phones through the Profile menu, and the title bar's back button replaces "Back to cabinet". Specs and tests affected, which 5b breaks below `64rem`: `web-auth` R3 (the header shows the name, a "Profile" button and a "Log out" button; R3-S1, R3-S2), R4 (R4-S1 to R4-S3, the "Log out" button) and R5-S10 (the teacher's name in the header); `web-profile` R1 (the cabinet header and "Back to cabinet"; R1-S1, and R1-S4 which presses "Back to cabinet") and R2 (its header clauses; R2-S2, R2-S6); `web-client` R17-S6 and R17-S7 (`fieldErrors.spec.ts` presses the header's "Profile"); the e2e `auth.spec.ts`, `profile.spec.ts`, `fieldErrors.spec.ts` and `selectArrow.spec.ts` (they click the header's "Profile" or "Log out"), and the unit tests `App.test.tsx` and `ProfilePage.test.tsx` (header buttons, `headerHas`, R1-S4). Partly #57.
6. Admin teachers (`AdminTeachersPage.tsx`):
   - 6a (#203, depends on 5a #201): restyle with the current data. `/admin` has one h1, "Teachers" with the count beside it; "Admin cabinet" is no longer shown, so `auth.spec.ts:112-114` (R5-S10 looks for the admin cabinet heading) and `App.test.tsx:74` (R2-S2 looks for the admin cabinet title) change; no spec scenario names that heading. The issued-link row and the "Just added" badge change `web-admin-teachers` R2-S1 and R3 ("inside that row"), R2-S8 (the "just added" label) and R3-S6, with `AdminTeachersPage.test.tsx` and `adminTeachers.spec.ts` (R2-S1). The dialog buttons "Keep active" and "Deactivate" change `web-admin-teachers` R4-S2 and R4-S3, which say "Confirm" and "Cancel" (the tests read the buttons by i18n key, so they follow the translation file; the scenarios change);
   - 6b: search and paging, a new issue with the backend change;
   - optional folds: #73, #157.
7. Login, set-password, not found; #87 and #89 only when they touch the same lines.
8. Profile page.

Small review items go to the issue that touches the code:

- 3a and 3b: the space before a full stop after a control, the quiet button icon inset, the 2rem row focus ring meeting the border.
- 4a: a tooltip's text always matches its trigger's current label; a tooltip never covers the row below its trigger's row.
- 5a: the Russian Profile-menu role lowercase.
- 7: the login subtitle width and break.
- Builder, #116-#118: the "Item n of N" position, the part actions; the tree's part names wrap within the fixed `14rem` tree width without overlapping the next row; "Hide" sits on its own line above the course name; type labels fit the longest name in each language.
- #134 and #135: the To-do icon size and alignment, the 13px student text, the lesson and homework page title inconsistency, the checked result shown three times, the Russian 360 admin status badge position (the last one in 6a).
- #119 (builds the preview): the phone type name above the preview.
