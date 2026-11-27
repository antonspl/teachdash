# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Groups 1 to 5 touch `frontend/` (each is one slice for the
`frontend-implementer`); group 6 touches `.agents/design` (the `implementer`).

## 1. Before screenshots

- [ ] 1.1 From `main`, before any other change, take the before set into `frontend/screenshots/ui-buttons-fields/before/`: the design-audit state list (login x5, set-password x6, admin x11 including dialogs, profile x9, cabinet x2, not found, startup error), at desktop and 360; list unreachable states (design D12)

## 2. Buttons, token and focus ring (design D1 to D4)

- [ ] 2.1 `tokens.css`: add `--button-icon-inset: 0.1875rem` and set `--lang-select-width` to `10.5rem`; `tokens.test.ts` has 90 names and the two values (web-client/R24-S1, R24-S2)
- [ ] 2.2 `Button.tsx` and `Button.module.css`: the variants `quiet`, `dangerText` and `muted`; `icon` takes any shared icon (a name union and one name-to-component map in `icons.tsx`); the icon-led class with the inset; the state rules and transitions; secondary text `--color-text`; the `ariaLabel`, `ariaPressed` and `fullWidth` props (web-client/R27-S1, R27-S2, R27-S5, R27-S6, R27-S7, R27-S8, R27-S9; R23 stays green)
- [ ] 2.3 `base.css`: the one global `:focus-visible` rule; no `outline: none` or `outline: 0` anywhere in `src` (web-client/R30-S1, Vitest part and a Playwright check of the computed ring)
- [ ] 2.4 `LoginPage` and `SetPasswordPage` submit use `fullWidth` (web-client/R27-S9)

## 3. Fields and the icon-led select (design D5 to D8)

- [ ] 3.1 `FieldError` component; `TextInput` and `Select` use it; `.invalid` and the message rules in `Field.module.css`; hover, disabled and read-only rules; `cssRules.test.ts` R26-S2 for the 2px border (web-client/R26-S2, R30-S3, R30-S4, R30-S6, R30-S7)
- [ ] 3.2 `PasswordInput`: the toggle is a `Button` (secondary, `aria-pressed`, passed-through aria-label, native `disabled`); delete `.toggle` (web-client/R30-S8)
- [ ] 3.3 `Select` `icon` prop and the icon-led select rules; `UiLanguageSelect` passes the globe; the wrapper uses `--lang-select-width` (web-client/R13-S6; R28-S2 and R28-S3 stay green)
- [ ] 3.4 Playwright: the focused invalid field shows border and ring (web-client/R30-S2), the long message wraps and nothing moves (R30-S5), the icon-led select gaps in en and ru, desktop and 360, in `selectArrow.spec.ts` with its own measure (R13-S5; R13-S1 to R13-S4 unchanged on the default select)

## 4. Busy labels (design D2, D9)

- [ ] 4.1 `Button` `busy` and `busyLabel` (two labels in one grid cell, `disabled`, `aria-busy`); the nine keys of design D9 in `en.json` and `ru.json`; `parity.test.ts` and `copyRules.test.ts` cover them (web-client/R29-S1, R29-S2 CSS half, R29-S5)
- [ ] 4.2 `LoginPage`, `SetPasswordPage`, `ProfileForm`, `PasswordForm` and the create dialog pass `busy` and `busyLabel` to their submit; update the tests that read the button by its idle name mid-request (design D10 list, held element reference where a test re-queries): `App.test.tsx` (web-auth/R1-S4), `ProfilePage.test.tsx` (web-profile/R2-S8, R3-S9) and `AdminTeachersPage.test.tsx` `submitButton()` (web-admin-teachers/R2-S5) (web-client/R29-S3, R29-S7)
- [ ] 4.3 `AdminTeachersPage`: `runRowAction` takes the pressed action, the per-row busy state records it, the pressed row button and the deactivate Confirm show their busy labels; update `deactivateButton(row)` use in R5-S4 and `confirm()` (web-admin-teachers/R4-S2, R5-S4; web-client/R29-S4)
- [ ] 4.4 Playwright: the button keeps its width while busy, in en and ru at desktop and 360 (web-client/R29-S2); the Russian busy labels fit at 360 in `e2e/russianFit.spec.ts` (web-client/R29-S6)

## 5. After screenshots

- [ ] 5.1 Take the final set in `frontend/screenshots/ui-buttons-fields/`: same states and widths as the before set, plus the busy label of each button (en and ru at 360), a focused invalid field and the icon-led language select (header and login, en and ru); check by eye the icon inset of "Add teacher", the field error, the focus ring, the icon-led select margins, 360 and Russian wrapping; list unreachable states (design D12)
- [ ] 5.2 Verify `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (re-run at 360 in en and ru: R7-S1, R13-S2, `skeleton.spec.ts`, `russianFit.spec.ts`)

## 6. Design docs (design D11)

- [ ] 6.1 `.agents/design/design-code.md`: the busy-label noun rule in "Copy and languages", the `--button-icon-inset` row and the `--lang-select-width` value in the token tables, the small-items line moved to 4a; `refactoring-ui-patterns.md:91` and `accessibility-patterns.md:29`, each with the exact before and after lines of design D11

## Workflow follow-up

- `code-gate-reviewer` and `visual-reviewer` on the whole PR (no slice review for a change touching `frontend/`), then ask for the code gate in the PR with the commit range and the screenshot folder path.
- Archive the change in the same PR (`openspec/specs/web-client` gets R29 and R30 and the edits of R13, R24, R26 and R27; `web-auth`, `web-profile` and `web-admin-teachers` get their edited scenarios). Closes #197.
- The main agent files the Log out busy-state follow-up issue and comments on #199 (the two review items moved to 4a).
- Mark the PR ready for the final review by a code owner other than the engineer.
