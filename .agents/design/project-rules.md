# Project rules

These are teachdash's own frontend design rules. Read this file right after
`README.md`. It wins over every other file in `.agents/design/`: where a source says
something else, follow this file.

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
  code (#171), then these project rules, then the vendored sources.
- **C1** Sentence case, not Title Case, for headings, buttons, labels and messages.
- **C2** No all-caps or uppercase-small labels.
- **C3** Tokens only. A new value becomes a new role-named token added to
  `tokens.css` (named for its role, such as `--space-button-gap`, not for its
  value).
- **C4** Text uses `--font-body`, the system stack. No web fonts and no "distinctive
  typeface".
- **C5** The one `--radius` and `--shadow-card` are ours. Changing, renaming or
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
  `--control-height`.
- **C10** Copy is friendly, in the second person, with no apologies. A message says
  what happened and how to fix it.
- **C11** Quotes follow each language's typography in the i18n files (ru uses «»).
- **C12** Focus on error follows the specs: `openspec/specs/web-client/spec.md` R14
  (R14-S12) and R17 (R17-S3).
- **C13** No scores and no P0-P3 priorities. Findings at review are triaged as
  constitution §1 step 10 says: security problems, data loss, spec violations, and
  design-rule violations (the design rules in `.agents/design/`, in the precedence
  order of its README) and WCAG 2.2 AA violations in UI the change itself adds or
  edits are fixed in the PR.
  - "UI the change adds or edits" means markup, CSS, copy (i18n strings) or behaviour
    (handlers, focus, states) that the change adds or edits. Touching an existing
    component for an unrelated reason does not pull in its existing problems (#168,
    field border contrast, stays out unless the change edits that border).
  - Every other finding becomes an issue, as step 10 says.
- **C14** Empty states are text plus a `src/ui/` action. No image assets.
