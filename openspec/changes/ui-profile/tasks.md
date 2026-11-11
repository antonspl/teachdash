# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Unit tests of helper modules have no scenario and no ID.

## 1. Groundwork

- [x] 1.1 Add `web-profile` to `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`; verify the script lists the new scenarios as missing
- [x] 1.2 Add `src/api/profile.ts` with `updateProfile` and `changePassword` (design D3), the second parsing no body on 204; add `updateProfile` to `AuthProvider`, which stores the returned user; unit test in `AuthProvider.test.tsx` that the stored user changes; verify `npm test`, `npm run lint` and `npm run build`
- [x] 1.3 Narrow the `me` entry of the 401 exemption in `api/client.ts` to GET requests by passing the method to the exemption check (design D4), with a unit test in `client.test.ts` for a 401 on `PATCH me` (handler called) and on `GET me` (not called); verify `npm test`, including the existing web-auth R2 and R6 tests
- [x] 1.4 Move the password rule to `src/passwordRules.ts` and its three messages to shared `password.*` keys (design D6), used by `SetPasswordPage`; unit tests for mismatch, 7 characters, 80 bytes and a valid password; verify `npm test` with the web-auth/R5-S3, R5-S4 and R5-S8 tests unchanged
- [x] 1.5 Let `timeZoneOptions` take an extra zone (design D7), with unit tests for an extra zone that is missing from the browser's list and one that is already in it; verify `npm test`

## 2. Route, header and page shell

- [ ] 2.1 Add `RequireUser` (design D1), the `/profile` route and `ProfilePage` with the header, the email as text and "Back to cabinet" (design D2); i18n strings in an informal tone; Vitest for web-profile/R1-S1, R1-S2, R1-S4, R1-S6 and R1-S7; add the page to `i18n/translations.test.tsx`; verify `npm test`
- [ ] 2.2 Add the "Profile" button to the cabinet header and the wrapping rule for the header's name (design D2); update the web-auth/R3-S1 test and add web-auth/R3-S2; verify `npm test`, including the admin teachers tests

## 3. Profile form

- [ ] 3.1 Add the profile form (name, time zone select with the stored zone, "Save"), with the success message, the `VALIDATION_FAILED` message, the general error, the in-flight guard and fields disabled while sending (design D8); Vitest for web-profile/R2-S2 to R2-S8; verify `npm test`
- [ ] 3.2 Restore `/profile` after logging in again, ignoring a trailing slash (design D5); Vitest for web-profile/R4-S1 and R4-S3; verify `npm test`, including web-auth/R6-S2 to R6-S4
- [ ] 3.3 Add an e2e helper in `e2e/accounts.ts` that creates a teacher with a given name and a password and logs in through the UI; Playwright test for web-profile/R2-S1; verify `npm run test:e2e` against `docker compose up --build --wait`

## 4. Password form

- [ ] 4.1 Add the password form (current password, new password twice, show/hide toggles, "Change password") using `passwordRules`, with the success message that empties the fields, the `WRONG_PASSWORD` message, the neutral `VALIDATION_FAILED` message, the general error, its own in-flight guard, and each form's button disabled while the other is sending (design D8); Vitest for web-profile/R1-S3, R3-S2 to R3-S11 and R4-S2; verify `npm test`
- [ ] 4.2 Playwright test for web-profile/R3-S1 (change the password, save the profile right after, log out, log in with the new password, the old one is refused); verify `npm run test:e2e`
- [ ] 4.3 Playwright test for web-profile/R1-S5 on the phone projects (200-character name without spaces, no horizontal scroll on `/profile` and on `/teacher`); verify `npm run test:e2e`

## 5. Final checks

- [ ] 5.1 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every web-profile scenario and web-auth/R3-S2 has a test

## Workflow follow-up

- Opus review of the code, then ask for the code gate in the PR with the commit range.
- Archive the change in the same PR (`openspec/specs/web-profile` created, `web-auth`
  R3 and R6 updated).
- Mark the PR ready for the final review by a code owner other than the engineer.
