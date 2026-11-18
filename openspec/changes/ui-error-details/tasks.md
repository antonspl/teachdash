# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Unit tests of helper modules have no scenario and no ID. The only
existing test that changes is web-profile R3-S10 (task 3.2).

## 1. API client

- [x] 1.1 Add `fieldErrors` to `ApiError` and replace `parseProblemCode` with `parseProblem` (design D1); Vitest for web-client/R5-S5 and R5-S6 in `client.test.ts`; verify `npm test`

## 2. Shared pieces

- [x] 2.1 Add the `error` prop to `ui/Select.tsx` with the markup and styles of `TextInput` (design D5); verify `npm run lint` (token check) and `npm test`
- [x] 2.2 Add `ui/useFieldErrors.ts` (design D2, D3) and the `fieldErrors.*` strings to `i18n/en.json`; unit tests of the hook (unknown field, unknown code, empty list, first entry per field wins, focus order, clear, clearAll); verify `npm test` and `i18n/translations.test.tsx`

## 3. Forms

- [x] 3.1 Wire the hook into the "Add teacher" dialog (`AdminTeachersPage`): name, email, time zone; form-level message only when `place` returns false; Vitest for web-client/R17-S1, R17-S3, R18-S1 and the dialog run of R17-S4; verify `npm test`
- [ ] 3.2 Wire the hook into `ProfileForm` (name, time zone) and `PasswordForm` (`currentPassword` → current, `newPassword` → new, hint first per design D4, `WRONG_PASSWORD` override); Vitest for the profile and password runs of web-client/R17-S2, R17-S8 and the profile run of R17-S4; adjust the web-profile/R3-S10 test to the modified scenario: the `WRONG_PASSWORD` response carries `errors` `[{field: "currentPassword", code: "WRONG_PASSWORD"}]` and the test edits the new password field (message stays) and then the current password field (message goes); verify `npm test`, with the other web-profile R2 and R3 tests unchanged
- [ ] 3.3 Wire the hook into `LoginPage` (email, password) and `SetPasswordPage` (password; `token` is not a field), and clear the set-password form-level message on edit (design D6); Vitest for the login and set-password runs of web-client/R17-S2, the set-password run of R17-S4, and R18-S2; verify `npm test`, including web-auth and web-client R14 tests unchanged

## 4. End to end

- [ ] 4.1 Playwright tests for web-client/R17-S5, R17-S6 and R17-S7 against `docker compose up --build --wait`, desktop and phone projects; verify `npm run test:e2e`
- [ ] 4.2 Take screenshots of the "Add teacher" dialog with errors under name, email and time zone, and of `/profile` with errors under the name and the current password, at desktop and 360-pixel width; check by eye that messages do not touch the next field or the buttons; record the result in the PR

## 5. Final checks

- [ ] 5.1 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every web-client R5, R17 and R18 scenario and web-profile R3 scenario has a test

## Workflow follow-up

- Sonnet review after each slice, Opus review of the whole PR, then ask for the code gate
  in the PR with the commit range.
- Archive the change in the same PR (`openspec/specs/web-client` gets R5 modified,
  R17 and R18; `openspec/specs/web-profile` gets R3 modified). Close #89 with it.
- After #84 and #88 are merged, change the PR's base to `main`.
- Mark the PR ready for the final review by a code owner other than the engineer.
