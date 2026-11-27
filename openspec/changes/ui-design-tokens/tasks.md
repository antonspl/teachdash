# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Every task group touches `frontend/` (or `.agents/design` for group 6), so each is one slice for the
`frontend-implementer` (group 6 for the `implementer`).

## 1. Before screenshots

- [ ] 1.1 From `main`, before any other change, take the before set into `frontend/screenshots/ui-design-tokens/before/`: the design-audit state list (login x5, set-password x6, admin x11 including dialogs, profile x9, cabinet x2, not found, startup error), at desktop and 360; list unreachable states

## 2. Tokens and rename (one slice, design D1)

- [ ] 2.1 Rewrite `frontend/src/styles/tokens.css` to exactly the design-code tables (design D2); remove `--space-1` to `--space-4`, `--color-border`, `--color-action-success-*`, `--shadow-card` and `--arrow-width`; check `base.css`
- [ ] 2.2 Apply the colour, type, shadow and space maps of design D3 to every `src/ui` CSS file; `AppHeader` bottom border; IssuedLink input fill; remove the `success` variant from `Button.tsx` and `Button.module.css`; `AdminTeachersPage.tsx:299,460` use the default variant
- [ ] 2.3 Select chevron (design D5): `Select.tsx` uses `ChevDownIcon`; `Field.module.css` drops `.arrowIcon` and `--arrow-width` and takes the Clarification 5 insets
- [ ] 2.4 Extend `frontend/scripts/check-design-tokens.mjs` to fail on an undefined `var(--name)` and to accept an optional folder argument (design D4); verify `npm run lint` and `npm run build`

## 3. Tests

- [ ] 3.1 Vitest in `frontend/src/styles/tokens.test.ts` for web-client/R24-S1, R24-S2 and R26-S1 (hand-copied lists; contrast computed from the hex values)
- [ ] 3.2 Vitest in `frontend/src/styles/cssRules.test.ts` for web-client/R24-S3, R24-S4, R25-S1, R25-S2, R25-S3, R26-S2, R27-S2 and R28-S1
- [ ] 3.3 Vitest in `AdminTeachersPage.test.tsx` and the select tests for web-client/R27-S1, R27-S3 and R28-S2 (buttons by role and name)
- [ ] 3.4 Playwright test for web-client/R28-S3 in `frontend/e2e/` (desktop and phone projects); re-run at 360 in en and ru: R7-S1, R13-S2, `skeleton.spec.ts`, `russianFit.spec.ts`; no approved test changes
- [ ] 3.5 Verify `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e`

## 4. After screenshots

- [ ] 4.1 Take the final set in `frontend/screenshots/ui-design-tokens/`: same states and widths as the before set; check by eye the tightened spacing, header edge, chevron inset, 360 and Russian wrapping, control borders; list unreachable states

## 5. Decorative borders note

- [ ] 5.1 Put the decorative-border list of design D7 in the PR description (#168)

## 6. Vendored design docs

- [ ] 6.1 In a separate commit, update `.agents/design` files (not `design-code.md`) that name `--shadow-card`, `--color-border`, `--space-1` to `--space-4`, `--color-action-success-*`, the success variant or `--arrow-width` to the new token names (design D8)

## Workflow follow-up

- `code-gate-reviewer` and `visual-reviewer` on the whole PR (no slice review for a change touching `frontend/`), then ask for the code gate in the PR with the commit range and the screenshot folder path.
- Archive the change in the same PR (`openspec/specs/web-client` gets R24 to R28). Closes #196 and #168.
- Mark the PR ready for the final review by a code owner other than the engineer.
