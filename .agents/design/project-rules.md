# Project rules

These are teachdash's own frontend design rules. Read this file right after
`README.md`. It wins over the vendored sources in `.agents/design/`: where a source
says something else, follow this file. The full order is in C0.

## Base rules

- All colours, fonts, sizes, spacing, radii and shadows are tokens in
  `frontend/src/styles/tokens.css`. The `npm run lint` rules
  (`frontend/scripts/check-design-tokens.mjs`) apply:
  - CSS has no raw colours and no raw px or rem lengths (a bare `0` is fine;
    `@media` conditions are not checked);
  - a font is exactly one `var(...)`;
  - custom properties are defined only in `tokens.css`;
  - `.ts` and `.tsx` files have no inline styles;
  - pages in `frontend/src/pages/` import no CSS and set no `className`: they use
    `src/ui/` components.
- Nothing loads from third-party CDNs: no web fonts, scripts, styles or images from
  another domain (constitution §10).
- UI copy goes through i18n (`frontend/src/i18n/`): sentence case, informal and
  friendly, like talking to a friend.
- Buttons never touch fields, cards or each other. Native parts such as the select
  arrow are inset like text. Controls go exactly where the spec says.
- New UI built in a change meets these rules and WCAG 2.2 AA.

## Resolved conflicts

- **C0** Precedence: as the README states it, the change's spec, then the design
  code (#171), then these project rules, then `visual-criteria.md`, then the vendored
  sources.
- **C1** Sentence case, not Title Case, for headings, buttons, labels and messages.
- **C2** No all-caps or uppercase-small labels.
- **C3** Tokens only. Reuse an existing token first; add a new token only when none
  fits, as a role-named token in `tokens.css` (named for its role, such as
  `--space-button-gap`, not for its value), and list each new token in the "UI
  design" section.
- **C4** Text uses `--font-body`, the system stack. No web fonts and no "distinctive
  typeface".
- **C5** The one `--radius` and `--shadow-overlay` (overlays only) are ours. Changing, renaming or
  deleting them, or any other existing token, is an engineer decision. The
  frontend-implementer only adds new role-named tokens; when a slice seems to need an
  existing token changed, it stops and reports.
- **C6** Reduced motion (`prefers-reduced-motion: reduce`) removes non-essential
  motion but keeps state feedback: a change of state stays visible, for example as an
  instant change or a fade instead of a slide. No global rule that sets every
  duration near zero.
- **C7** Native elements first (`<button>`, `<a>`, `<input>`, `<select>`,
  `<dialog>`). Never add key handlers to a `<button>`: it already handles Enter and
  Space.
- **C8** Personal-data fields get their autocomplete tokens (WCAG 1.3.5), such as
  `name`, `email`, `current-password` and `new-password`. Autocomplete is never
  turned off to avoid password managers.
- **C9** The touch target floor is 24 CSS px (WCAG 2.5.8, AA). Controls use
  `--control-height` (2.5rem with `pointer: fine`, 2.75rem on touch; row icon buttons 2rem with a mouse).
- **C10** Copy is friendly, in the second person, with no apologies. A message says
  what happened and how to fix it.
- **C11** Quotes follow each language's typography in the i18n files (ru uses «»).
- **C12** Focus on error follows the specs: `openspec/specs/web-client/spec.md` R14
  (R14-S12) and R17 (R17-S3).
- **C13** No scores and no P0-P3 priorities. Findings at review are triaged as
  constitution §1 step 10 says: security problems, data loss and spec violations are
  fixed in the PR; so are, in UI the change itself adds or edits, violations of the
  change's "UI design" section, the design code (#171), `project-rules.md` and WCAG
  2.2 AA, and `visual-criteria.md` findings where the screenshot departs from the
  "UI design" visual plan. Findings from the vendored sources become issues.
  - "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour
    (handlers, focus, states) that the change adds or edits. Touching an existing
    component for an unrelated reason does not pull in its existing problems (#168,
    field border contrast, stays out unless the change edits that border).
  - Every other finding becomes an issue, as step 10 says.
- **C14** Empty states are text plus a `src/ui/` action. No image assets.
- **C15** Web-font data and `preview.py` are not used. `ui-ux-pro-max/data/google-fonts.csv`,
  `typography.csv` and the font-pairing results, and the `fonts.googleapis.com` link
  that `scripts/preview.py` writes, give font names we do not load. `preview.py` is not
  reachable through the allowed `search.py` command. Text uses
  `--font-body` (C4); nothing loads from third-party CDNs (base rules, constitution
  §10). Font-pairing output is not a recommendation.
- **C16** Icon sets are not added. `ui-ux-pro-max/data/icons.csv` names the Phosphor
  npm package; a new dependency is an engineer decision (constitution §7). Icons are
  inline SVG components in `src/ui/` (`PlusIcon` is the model), one set with one
  stroke style, as `refactoring-ui.md` section 6 (Icons) says. `icons.csv` may
  help choose which meaning an icon needs, not where it comes from. No emoji as icons.
- **C17** Tailwind, shadcn and other stack data (`ui-ux-pro-max/data/stacks/*.csv`)
  is reference only, as `ui-ux-pro-max/SKILL.md` says. teachdash has no Tailwind or
  shadcn: styling follows C3 and the base rules (token-only CSS, no `className` in
  pages), and C5 keeps the existing tokens.
- **C18** 44x44 does not apply. `ui-ux-pro-max` asks for a 44x44 touch target
  ("Touch & Interaction" priority); our floor is 24 CSS px (C9, WCAG 2.5.8 AA, as
  `accessibility.md` "Target size" states), met by `--control-height`. A target
  between 24 and 44 CSS px is not a finding. A new control is not made smaller than
  `--control-height` (2.5rem `pointer: fine`, 2.75rem touch; row icon buttons 2rem) because of this item.
- **C19** Landing and hero patterns do not apply. `ui-ux-pro-max/data/landing.csv`
  and the landing-page parts of the vendored sources describe marketing pages. A
  teachdash screen is a working screen: the page type and layout come from the
  change's spec and the design code (C0), empty states follow C14 (no image assets).
  A landing pattern is used only when a spec asks for a public page, and then it
  still obeys C3, C4 and C14.
- **C20** The `search.py` options that write files (persistence, page, output
  directory, force, preview) or export tokens are removed in our copy, so the allowed
  command only prints. The tokens live in `frontend/src/styles/tokens.css` (C3, C5,
  base rules). Basis: `ui-ux-pro-max/NOTICE-teachdash.md`, G-38 and G-51.
- **C21** "One radius, shadow only on overlays" is not a violation. The anti-pattern list in
  `frontend-design-principles.md` (the "SaaS-card kit") and the radius and shadow
  lines of `visual-passes.md` warn against one radius and the same soft shadow on
  everything. teachdash has exactly one `--radius` and `--shadow-overlay` (overlays only) by design
  (C5, `refactoring-ui.md` section 5, `refactoring-ui-patterns.md` "Radius"). Flag
  only what the spec's "UI design" section does not support: a card around
  everything, or decoration with no meaning (`visual-criteria.md`).
- **C22** Design system step. `ui-ux-pro-max/SKILL.md` Step 2 says generating a
  design system is "REQUIRED for new pages/projects". In teachdash it is not: #171
  chooses the direction and the token plan in `design-code.md` (G-37), which ranks
  above the vendored sources (C0). `search.py --design-system` output is a reference
  read in the terminal (C20), not a decision; where it differs from the design code,
  the design code wins.
- **C23** A "distinctive typeface", web-font fallback metrics and "character" through
  type: `visual-type.md` (the `size-adjust` fallback advice for a web font),
  `visual-passes.md` ("Still distinctive") and the typography data suggest a typeface
  choice. C4 wins: one system stack in `--font-body`. Character comes from hierarchy,
  spacing and the tokens in the design code, and a finding that asks for a different
  typeface becomes an issue (C13).
- **C24** Other disagreements. The "Pre-Delivery Checklist" in
  `ui-ux-pro-max/references/pro-rules.md` is scoped to native and mobile app UI and
  does not apply to the web client; `accessibility.md` and `checklist.md` do. Where a
  vendored source and these rules disagree on anything not named above, the order of
  C0 decides, so these rules win over the vendored sources and the change's spec and
  the design code win over these rules. A finding that rests on a vendored source
  alone is triaged as C13 says.
