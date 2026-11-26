# Proposal

Issue: #195 (closes it). Redesign plan step 1 (`.agents/design/design-code.md`, "Redesign plan").

## Clarifications

Decisions by the engineer:

1. Only `PlusIcon` is used in this change. All other icons are built but nothing imports them yet; no other component (select chevron, badges, Dialog close) switches to icons here. Later redesign steps do that.
2. Component shape: a shared `SvgIcon` base component (root `<svg>` with viewBox `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round caps and joins, `aria-hidden="true"`, `focusable="false"`, the size class) plus thin named components per icon of the design-code table (`PlusIcon`, `CloseIcon`, `ChevDownIcon`, `ChevRightIcon`, `ChevLeftIcon`, `ArrowUpIcon`, `TTheoryIcon`, `UserOffIcon`, `LogOutIcon`, `RightIcon`, `WrongIcon` and so on: PascalCase of the table name + `Icon`), all in one file `frontend/src/ui/icons.tsx` (split only if it passes about 400 lines). Filled `play` and `more` set `fill="currentColor"` and `stroke="none"` on their own elements; `todo` sets `stroke-dasharray="2.5 2.5"` on its circle. Icons have no `title` and no `aria-label` prop; the accessible name belongs on the button.
3. `PlusIcon` moves into `icons.tsx` with the table geometry (`M10 4v12M4 10h12`); `PlusIcon.tsx` and `PlusIcon.module.css` are deleted; `Button.tsx` changes only its import path.
4. Sizes: a `size?: 'default' | 'small' | 'rail'` prop (default `'default'`) mapping to three CSS module classes, each setting width and height from one token (`--icon-size`, `--icon-size-small`, `--icon-size-rail`) and `flex: none`. Never `em`. Tokens added to `frontend/src/styles/tokens.css`: `--icon-size` 1.25rem, `--icon-size-small` 1rem, `--icon-size-rail` 1.5rem. `--icon-size-placeholder` is NOT added.
5. Unit test: one table-driven Vitest that checks every icon of the design-code table exists and none is extra, the shared root attributes, the size class per size value, and the path and shape data equal to the design-code table (accepted as a change detector: it is the only check of about 40 hand-typed shapes before their first use). Plus a unit check that "Create teacher" and other buttons stay icon-free (design-code: icons never on primary, form or dialog buttons except a create action's plus) and that the plus stays first in "Add teacher", `aria-hidden`, `focusable="false"`.
6. "Sized only by the tokens" is enforced by one Playwright e2e check that the plus on "Add teacher" renders 20px wide and high (computed style, like `frontend/e2e/selectArrow.spec.ts:20-31`). No new lint rule.
7. Spec: capability `web-client`, two ADDED requirements: R21 the shared icon set (including the plus geometry and its use in "Add teacher"), R22 the icon size tokens. The next free number is R21 (R15 and R16 are taken by the open PR #211; do not use them). Every scenario gets a test titled `[web-client/R21-S1] ...` (scenario-coverage script `scripts/check-scenario-coverage.sh`). At most 6 requirements; each has at least one Given/When/Then.
8. Look: "Add teacher" stays the green `success` Button; only its icon changes (a 12px glyph in a 20px box, 1.75px stroke, against 11px at 3px before; about 0.25rem wider, height unchanged). The gap stays `--space-1` (0.5rem, equal to design-code's later `--space-inline`). The accent-primary move and the token renames stay in redesign step 2.
9. Screenshots: before (taken by the `frontend-implementer` from `main` before the first slice, under its role's before-set exception, into `frontend/screenshots/ui-icon-set/before/`) and after, in en, at desktop and 360, for `/admin`: default, hover and keyboard focus on "Add teacher"; optional 200% zoom on desktop. ru: `main` has only a stub `ru.json` (full Russian is PR #211, not merged), so if #211 is merged before the code gate, the branch is rebased and an ru 360 shot checks that "Add teacher" stays on one line; otherwise the PR states that the ru fit was not checked. Unreachable: disabled and loading "Add teacher".

## Research angles and conclusions

Angles: the icon code and its users, the design-token lint, contrast and the visual plan (`frontend-researcher`).

- The only importer of `PlusIcon` is `frontend/src/ui/Button.tsx` (`icon?: 'plus'`); the only caller is `AdminTeachersPage.tsx:461`. No existing test asserts the old geometry or `1em`; `Button.test.tsx:7-15` (aria-hidden, focusable, first child) stays green.
- The design-token lint (`npm run lint` = eslint + `frontend/scripts/check-design-tokens.mjs`) flags raw px and rem values and custom-property definitions outside `tokens.css`, so a per-component custom property for size is not allowed. It does not check SVG attributes, `em` or unused exports. Nothing (no knip, no `noUnusedExports`) flags the unused icons; the code-gate reviewer may ask, and the answer is redesign plan step 1.
- Contrast: the icon colour `--color-action-success-text` `#14532d` on `#dcfce7` is 8.3:1 (7.5:1 on hover), above the 3:1 non-text floor; the icon is decorative. The old 3px stroke departed from the visual-passes Polish rule "keep icon family, stroke weight, size coherent".
- Optical alignment is judged on the rendered screenshot (visual-type "Optical alignment"), not computed.

## What changes

- New `frontend/src/ui/icons.tsx` (shared `SvgIcon` and one named component per icon of the design-code table) and `icons.module.css` (three size classes).
- Tokens `--icon-size`, `--icon-size-small` and `--icon-size-rail` in `frontend/src/styles/tokens.css`.
- `PlusIcon` moves to the 20-unit geometry; `PlusIcon.tsx` and `PlusIcon.module.css` are deleted; `Button.tsx` imports from `icons.tsx`.
- `web-client` gets R21 (the shared icon set) and R22 (the icon size tokens).
- Tests: one table-driven Vitest, the button checks, and one Playwright check of the plus size. Before and after screenshots of "Add teacher".

## Out of scope

- Using any icon other than the plus: the select chevron (step 2), badges and banners, the Dialog close button (3b), the rail, row actions.
- The accent-primary move, token renames and removal of the `success` Button variant (redesign step 2).
- `--icon-size-placeholder` (the image placeholder is not built here).
- A lint rule for icon sizes, a tooltip or `IconButton` (step 4a), emoji, dark mode, backend changes, new dependencies.
