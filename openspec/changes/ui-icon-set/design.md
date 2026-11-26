# Design

## Context

`frontend/src/ui/PlusIcon.tsx` is the only icon. It draws on a `0 0 16 16` grid with a 3px stroke and `PlusIcon.module.css` sizes it `1em`. `Button` (`icon?: 'plus'`) is its only importer and `AdminTeachersPage.tsx:461` ("Add teacher") its only caller. The design code (`.agents/design/design-code.md`, "Icons") defines one icon table on a 20-unit grid with a 1.75 stroke and three size tokens. Redesign step 1 builds that set now because steps 2, 3b and 4a use it.

## Goals / Non-Goals

- Goals: the whole table as components, the three tokens, `PlusIcon` on the new geometry.
- Non-goals: using any icon but the plus, restyling "Add teacher" (step 2), a tooltip or `IconButton` (step 4a), a lint rule.

## Decisions

### D1 One file, one base component

`frontend/src/ui/icons.tsx` holds a `SvgIcon` base (root `<svg>`: viewBox `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round caps and joins, `aria-hidden="true"`, `focusable="false"`, the size class) and one thin component per table icon, named PascalCase of the table name + `Icon` (`PlusIcon`, `ChevDownIcon`, `TTheoryIcon`, `UserOffIcon`, `LogOutIcon` and so on). The file is split only if it passes about 400 lines. Filled `play` and `more` set `fill="currentColor"` and `stroke="none"` on their own elements; `todo` sets `stroke-dasharray="2.5 2.5"` on its circle. No `title`, no `aria-label` prop: the accessible name belongs on the button.

### D2 `PlusIcon` moves

`PlusIcon` lives in `icons.tsx` with `M10 4v12M4 10h12`. `PlusIcon.tsx` and `PlusIcon.module.css` are deleted. `Button.tsx` changes only its import path.

### D3 Size prop and tokens

`size?: 'default' | 'small' | 'rail'` (default `'default'`) selects one of three classes in `icons.module.css`; each sets `width` and `height` from `--icon-size`, `--icon-size-small` or `--icon-size-rail` and `flex: none`. Never `em`. The tokens go in `tokens.css`: `--icon-size` 1.25rem, `--icon-size-small` 1rem, `--icon-size-rail` 1.5rem. No existing token expresses an icon box (checked), so a new one is justified (C3). `--icon-size-placeholder` (3rem) is not added: it belongs to the image placeholder, which this change does not build.

### D4 Tests

One table-driven Vitest checks that every icon of the table exists and none is extra, the shared root attributes, the size class per size value, and the path and shape data against the design-code table. The geometry check is accepted as a change detector: it is the only check of about 40 hand-typed shapes before their first use. A unit check keeps "Create teacher" and other buttons icon-free and the plus first, `aria-hidden` and `focusable="false"` in "Add teacher". One Playwright check measures the plus on "Add teacher" (20px by 20px, computed style, like `frontend/e2e/selectArrow.spec.ts:20-31`); it is the enforcement of "sized only by the tokens". There is no new lint rule: the design-token lint flags raw px and rem and custom properties outside `tokens.css`, so a per-component size property is already rejected, but it does not check SVG attributes or `em`.

### D5 Unused icons

Nothing flags unused exports (no knip, no `noUnusedExports`). The icons other than the plus are built and unused on purpose (Clarification 1); the answer to a reviewer is redesign plan step 1.

## UI design

Scope: shared icon set in `frontend/src/ui`, three new tokens, `PlusIcon` moved to the 20-unit geometry. The only screen touched is `/admin`, the "Add teacher" button. This is a transitional look; design-code plan step 2 turns the button into the accent primary.

### Screens touched

`/admin`, the "Add teacher" button. No other screen changes.

### Layout (desktop and 360 px)

The plus is a 12px glyph in a 20px box, 1.75px stroke (11px glyph at 3px before), vertically centred on the label. The button is about 0.25rem wider; height and position are unchanged. At 360 the English label stays on one line in the toolbar. The gap between icon and label stays `--space-1`.

### States per screen

Default, hover and keyboard focus are drawn. Disabled and loading "Add teacher" do not exist (the button is never set to either). No empty, error or success state.

### Copy

No new i18n keys. The label "Add teacher" is unchanged; the icon has no text.

### A11y

The icon is decorative: `aria-hidden="true"`, `focusable="false"`, no title. The button's name is its label. The plus stays the first child. Focus order and keyboard behaviour are unchanged. Contrast: the icon uses `--color-action-success-text` `#14532d` on `#dcfce7` (8.3:1; 7.5:1 on hover), above the 3:1 non-text floor.

### Components and tokens

- Reused: `Button` (`success` variant, `icon="plus"`), `--color-action-success-*`, `--space-1`, `--control-height`.
- New: the `SvgIcon` base and the named icon components in `icons.tsx`, `icons.module.css`.
- New tokens: `--icon-size` (1.25rem, the default icon size), `--icon-size-small` (1rem, for badges, chevrons and field errors), `--icon-size-rail` (1.5rem, for the rail). Reuse checked: none of the existing tokens expresses an icon box.
- Not added: `--icon-size-placeholder`.

### Visual plan per screen and state

Scope: shared icon set in `frontend/src/ui`, three new tokens, `PlusIcon` moved to the 20-unit geometry. The only screen touched is `/admin`, the "Add teacher" button. This is a transitional look; design-code plan step 2 turns the button into the accent primary.

*Icon set (no screen).*
- Every icon of the design-code icon table, as inline SVG: viewBox `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round caps and joins, `aria-hidden="true"`, `focusable="false"`, `flex: none`.
- Filled `play` and `more` set `fill="currentColor"`, `stroke="none"` per element; `todo` uses `stroke-dasharray="2.5 2.5"`.
- Sizes: `default` reads `--icon-size`, `small` reads `--icon-size-small`, `rail` reads `--icon-size-rail`. No `em`; no `title`.
- Colour is inherited (`currentColor`); the set adds no colour. No emoji (C16).

*New tokens (tokens.css; reuse checked, none of the existing tokens expresses an icon box).*
- `--icon-size`: 1.25rem, the default icon size. `--icon-size-small`: 1rem, for badges, chevrons and field errors. `--icon-size-rail`: 1.5rem, for the rail.
- `--icon-size-placeholder` (3rem) is not added; it belongs to the image placeholder, which this change does not build.

*"Add teacher", default (desktop and 360).*
- Hierarchy: the only action in the page header, so it reads primary through its green fill and 600-weight label. The icon is tertiary: same colour as the label, no weight of its own.
- Tokens: `--color-action-success-*` fill and text (unchanged), `--icon-size` icon, `--space-1` gap, `--control-height` unchanged.
- Layout: the plus is a 12px glyph in a 20px box, 1.75px stroke (11px glyph at 3px before), vertically centred on the label. The button is about 0.25rem wider; height and position are unchanged.
- Density: unchanged toolbar. At 360 the English label stays on one line in the toolbar.

*"Add teacher", hover and keyboard focus.* Hover uses `--color-action-success-hover`, and the icon follows `currentColor`. Focus shows the existing `:focus-visible` ring with the new icon inside; the ring is not clipped.

*Not changed.* "Create teacher" and every other button stay icon-free (design-code "Icons are never on primary, form or dialog buttons"). No other component switches to icons.

*Screenshots.*
- Before (from `main`) and after, at desktop and 360, for `/admin`: default, hover and keyboard focus on "Add teacher", in en. Russian: `main` has only a stub `ru.json` (full Russian is PR #211, not merged), so the Russian label falls back to English. If #211 is merged before the code gate, the branch is rebased and a Russian 360 shot checks that "Add teacher" stays on one line; otherwise the PR states that the Russian fit was not checked (Clarification 9).
- Unreachable: a disabled or loading "Add teacher" (the button is never set to either).

## Risks / Trade-offs

- [About 40 hand-typed shapes with no use yet] -> the table-driven geometry test is a change detector (D4); the first real use in a later step is visually checked there.
- [Unused icons are not flagged by any tool] -> expected, Clarification 1 (D5).
- [The button is about 0.25rem wider] -> accepted; height is unchanged and the label stays on one line at 360 in English.
- [Russian fit not checked while #211 is unmerged] -> stated in the PR if it stays so (Clarification 9).
- [The transitional green button] -> step 2 turns it into the accent primary (Clarification 8).

## Migration Plan

None. No backend or data changes, no new dependency, no Flyway migration.
