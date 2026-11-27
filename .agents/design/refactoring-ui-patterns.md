Source: wondelai/skills, `refactoring-ui/references/advanced-patterns.md` @c526558, MIT, licence in `.agents/design/licenses/wondelai-skills.LICENSE`; modified by teachdash.

# Advanced design patterns

Extended reference for components beyond static layout: empty states, forms, icons, interaction states, colour roles, radius, truncation, responsive layout, dialogs, menus and navigation. Read it on demand, when the slice builds one of these patterns. `project-rules.md` wins over this file: values are tokens from `frontend/src/styles/tokens.css`, pages use `src/ui/` components, and a new value becomes a new role-named token (C3).

## Empty states

Empty states are an opportunity, not an afterthought. In teachdash an empty state is text plus a `src/ui/` action, with no image assets (C14).

**A good empty state has:**
- a clear explanation of what goes here;
- the primary action that fills it (a `src/ui/` Button or TextLink);
- optional secondary actions.

```text
Bad:  "No items"
Good: "No lessons yet. Add your first one and it shows up here."
      [+ Add lesson]
```

**Match the tone:** neutral, second person (C10). Copy goes through i18n.

## Form design

### Input sizing

Match input width to the expected content:
- email, URL, name, street address: the full width of the form (inside `--content-width`);
- short values (a code, a postcode, a number of minutes): narrower, through a new role-named token when no token fits;
- a select is as wide as its longest option needs, not wider.

### Placeholder and label

**Never use a placeholder as the only label.** Placeholders disappear on input. Always show a visible label (the `src/ui/` field components render one). Placeholders work for format hints ("DD.MM.YYYY"), examples ("e.g. anna@example.com…"; placeholders end with `…`, checklist) and optional clarification.

### Input states

Use the `src/ui/` field components (TextInput, PasswordInput, Select) and their states; don't restyle them in a page.

```text
Default:   --color-control-border border, --color-surface background
Focus:     visible outline in --color-accent (:focus-visible)
Error:     --color-danger-text border, plus the error text next to the field
Disabled:  --opacity-disabled
```

An error is never shown by colour alone: the error text says what happened and how to fix it (C10). Focus on error follows web-client R14-S12 and R17-S3 (C12).

### Button hierarchy

One primary action per view; everything else is secondary. Use the `src/ui/` Button variants:

```text
primary:    filled --color-accent, --color-on-accent text
secondary:  --color-surface with a --color-control-border border
danger:     the confirming button of a destructive action
```

Buttons sit in a `src/ui/` ButtonRow (`--space-button-gap`) and never touch fields, cards or each other (project rules).

### Form layout

- One column for simple forms.
- Two columns only when inputs are related (first and last name) and the width allows; at 360 px they stack.
- Labels above inputs.
- Group related fields with spacing (`--space-block` between fields, `--space-section` between groups) or a `src/ui/` Section.

## Icon usage

- **Sizing:** icons balance with the text next to them: about the height of a capital letter inline, a bit larger in icon-only buttons. Size them in `em` or through a role-named token, so they follow the text.
- **Pairing:** align the icon centre with the text centre, with a consistent gap (`--space-inline`), as the `src/ui/` Button does for its `plus` icon.
- **Use icons for:** navigation items; common actions (edit, delete, share); status, always with text (`src/ui/` StatusBadge); feature lists, with caution.
- **Don't use icons:** to fill space; on buttons with clear text ("Save", "Continue"); as decoration that adds no meaning. When a decorative icon stays, give it `aria-hidden="true"`.

## Interaction states

Every interactive element needs visible state changes.

- **Hover:** a subtle background change, or a shift towards a stronger shade (a hover token, such as `--color-accent-hover`).
- **Active (pressed):** stronger than hover, with no layout shift.
- **Focus:** an obvious outline, at least 3:1 against its background; not colour alone; `:focus-visible` for keyboard focus.
- **Loading:** keep the submit button enabled until the request starts, then disable it and show progress; keep the layout size; announce the result through a `role="status"` message (`src/ui/` StatusMessage).

```css
/* Example button states, all in tokens */
.button:hover:not(:disabled) {
  background: var(--color-accent-hover);
}
.button:focus-visible {
  outline: var(--border-width) solid var(--color-accent);
}
.button:disabled {
  opacity: var(--opacity-disabled);
  cursor: not-allowed;
}
```

A thicker outline or an offset is a new role-named token (C3).

## Colour roles

Use colour purposefully, through the role tokens:

| Role | Association | Tokens |
|-------|-------------|---------|
| Accent | Trust, calm | `--color-accent`, `--color-on-accent`: primary actions, links, focus |
| Success | Success, go | `--color-success-*` |
| Danger | Error, destructive | `--color-danger-*`: errors, destructive actions |
| Neutral | Neutral information | `--color-neutral-*`, `--color-text-muted` |
| Base | Text, surfaces, borders | `--color-text`, `--color-surface`, `--color-background`, `--color-control-border`, `--color-divider` |

Avoid: danger colours on non-destructive primary buttons; meaning by colour alone (colour-blind users); low-contrast colours for important actions; a new accent colour where a role token already exists.

## Radius

teachdash has one `--radius` for controls, cards and dialogs; changing it is an engineer decision (C5). For nested elements, the inner radius is the outer radius minus the padding (`calc()` on tokens). Content inside a card follows the card's radius, or the card uses `overflow: hidden`.

## Text wrapping and truncation

**Truncate:** navigation items; table cells with fixed widths; card titles when the full title is shown elsewhere (for example on the item's own page).

**Don't truncate:** body text, important information, search results, error messages.

```css
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

In a flex row the truncated child also needs `min-width: 0`.

## Responsive layout

Breakpoints live in `@media` conditions (where tokens do not work); keep their number small and reuse the ones already in the codebase.

1. Design for 360 px first, add complexity for larger screens.
2. Stack on mobile, side by side on desktop.
3. Full-width inputs on mobile, constrained (`--content-width`) on desktop.
4. Touch targets at least 24 CSS px (WCAG 2.5.8); controls use `--control-height` (C9).
5. Keep secondary navigation reachable on mobile; don't hide the only way to an action.

Screenshots at desktop and 360 px prove the layout (frontend-implementer role).

## Dialogs

Use the native `<dialog>` through `src/ui/Dialog`. It handles focus, Escape and returning focus to the control that opened it; don't build a focus trap by hand.

| Content type | Width | Height |
|--------------|-------|--------|
| Confirmation | Up to `--content-width` | Auto (minimal) |
| Form | Up to `--content-width` | Auto |
| Long content | `--content-width` | Scrolls inside, with `overscroll-behavior: contain` |

On a 360 px screen the dialog keeps a `--space-group` margin on each side.

```text
┌─────────────────────────────────────────────┐
│  Title                                      │
├─────────────────────────────────────────────┤
│  Dialog content here                        │
├─────────────────────────────────────────────┤
│  Cancel    Primary action                   │
└─────────────────────────────────────────────┘
```

- Put the safe choice first: focus moves to the first button. A destructive action is confirmed in a dialog or has an undo window (checklist).
- Style `::backdrop` with a token colour; a new backdrop colour is a new role-named token.
- Opening and closing may fade or scale (`opacity`, `transform` only; a duration is a new role-named token). Under `prefers-reduced-motion: reduce` the state change stays visible as an instant change or a fade (C6).

## Dropdowns and menus

Prefer a native `<select>` (`src/ui/` Select) for choosing one value. Build a custom menu only for actions, and give it full keyboard support (`accessibility-patterns.md`). Place it below the trigger, aligned to its start edge; flip it above when there is no space below; keep it inside the viewport.

```css
.menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: var(--space-inline);
  max-height: 50vh;
  overflow-y: auto;
  background: var(--color-surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow-overlay);
}

.menu-item {
  min-height: var(--control-height);
  padding: 0 var(--space-group);
}

.menu-item:hover {
  background: var(--color-neutral-background);
}
```

| Item type | Visual |
|------|--------|
| Standard | Text label |
| With icon | Icon + label |
| With description | Label + description in `--color-text-muted` |
| Destructive | `--color-danger-text`, plus a confirmation |
| Disabled | `--opacity-disabled`, and `aria-disabled` |

## Navigation

- **Top navigation:** the `src/ui/` AppHeader holds it. It wraps or collapses at 360 px, every item stays reachable by keyboard, and the active item has `aria-current="page"` and a visible state that is not colour alone.
- **Breadcrumbs** (`Home > Group > Lesson`): every item links except the current page; truncate middle items when too long; show them on nested pages.
- **Tabs:** an underline in `--color-accent` marks the selected tab (`aria-selected="true"`), on a `--color-divider` line; a thicker underline is a new role-named token. Many tabs scroll horizontally at 360 px. Tabs that change the view sync to the URL (checklist, Navigation and state).
