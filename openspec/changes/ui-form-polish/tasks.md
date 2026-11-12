# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Unit tests of helper modules have no scenario and no ID.

## 1. Select arrow

- [ ] 1.1 Give `ui/Select.tsx` a wrapper with the app's own arrow and style the select as in design D1 (real `aria-hidden` arrow element, `appearance: none`, full width, inset from `--space-1`, right padding, text cut off); sizes from `styles/tokens.css`; verify `npm run lint` (token check) and `npm test`, including the profile and admin teachers tests
- [ ] 1.2 Playwright tests for web-client/R13-S1 (profile and "Add teacher" dialog), R13-S2 (phone width, long time zone), R13-S3 (click on the arrow) and R13-S4 (disabled while saving), measured as in design D5; verify `npm run test:e2e` against `docker compose up --build --wait`, desktop and phone projects
- [ ] 1.3 Take screenshots of `/profile` and the "Add teacher" dialog at desktop and 360-pixel width in Chromium, Firefox and WebKit, and check by eye that the arrow is inset like the text and nothing is cramped; record the result in the PR

## 2. New-password hints

- [ ] 2.1 Add `passwordHints` to `src/passwordRules.ts` (design D2), sharing the constants with `passwordProblem`; unit tests for the table of cases (empty, short, 72 and 80 bytes, repeat empty, differs, equal); verify `npm test`
- [ ] 2.2 Wire the hints into `PasswordForm` and `SetPasswordPage` through the `error` prop of `PasswordInput`, stop copying rule problems into the form message, and clear the form message before the rule check on both forms (design D3); Vitest for web-client/R14-S1 to R14-S11, one parameterised test per scenario over the two forms where the scenario names both, pressing the real button with the required fields filled (design D5); verify `npm test`
- [ ] 2.3 Adjust the tests of web-profile/R3-S3 to R3-S5 and web-auth/R5-S3, R5-S4 and R5-S8 (design D4): keep their helpers and checks, and add a check that the hint is still shown after the press; verify `npm test` and that `i18n/translations.test.tsx` still passes

## 3. Final checks

- [ ] 3.1 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every web-client R13 and R14 scenario has a test

## Workflow follow-up

- Sonnet review after each slice, Opus review of the whole PR, then ask for the code gate
  in the PR with the commit range.
- Archive the change in the same PR (`openspec/specs/web-client` gets R13 and R14).
- After #84 is merged, change the PR's base to `main`.
- Mark the PR ready for the final review by a code owner other than the engineer.
