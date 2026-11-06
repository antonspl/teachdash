# Tasks

Frontend-only change. Each group lands its own tests: Vitest or Playwright, exactly one
test per scenario ID, with `[<scenario ID>]` at the start of the test title.

## 1. Shared UI and strings

- [x] 1.1 Add `TextInput`, `PasswordInput` (with show toggle), `Button`, `ErrorWithRetry` and `AppHeader` to `src/ui/` using design tokens; verify `npm run lint` passes
- [x] 1.2 Add the new strings to `src/i18n/en.json` (login, link hint, cabinets, header, set-password, password checks, session ended, start-up error, logout error) and remove the placeholder strings; verify `npm run build` passes

## 2. Session state and API client

- [x] 2.1 Add the `onUnauthorized` hook to `apiRequest`, skipping login, logout, `/api/me` and password links; verify with a Vitest unit test
- [x] 2.2 Add `AuthProvider` (`loading`, `error`, `anonymous`, `user`; `login`, `logout`, `setPasswordWithLink`, `retry`; session-ended reason and path in memory); verify with Vitest unit tests

## 3. Routing and cabinets

- [x] 3.1 Add `RequireRole`, `AnonymousOnly` and `HomeRedirect`, and the `/admin`, `/teacher`, `/student` routes in `App.tsx`; verify `[web-auth/R2-S1]` to `[web-auth/R2-S7]` in Vitest
- [x] 3.2 Add the three empty cabinet pages with the header; verify `[web-auth/R3-S1]` in Vitest

## 4. Login and logout

- [x] 4.1 Add the login page with the reachability warning and the link hint; the page decides where to go after login; remove the placeholder page and move its tests; verify `[web-auth/R1-S2]` to `[web-auth/R1-S6]`, `[web-client/R2-S1]`, `[web-client/R12-S2]` and `[web-client/R12-S4]` in Vitest
- [x] 4.2 Wire "Log out" in the header; verify `[web-auth/R4-S2]` and `[web-auth/R4-S3]` in Vitest
- [x] 4.3 Handle the session ending during use and the return to the page, using a request sent through `apiRequest` from a test page; verify `[web-auth/R6-S1]` to `[web-auth/R6-S4]` in Vitest

## 5. Set-password page

- [x] 5.1 Add the `/set-password` page (token read in the first render, fragment removed, two fields, length and byte checks, link error codes); verify `[web-auth/R5-S2]` to `[web-auth/R5-S9]` in Vitest, with `R5-S2` rendered in strict mode

## 6. End-to-end

- [ ] 6.1 Add an E2E helper that logs in as the admin from `.env` and creates a teacher with a unique email to get a set-password link; fail fast if the admin variables are missing
- [ ] 6.2 Update `e2e/skeleton.spec.ts`: rename the old R1 and R3 tests to R11 and R12, and move the phone-width and security-header tests to the new pages; verify `[web-client/R7-S1]`, `[web-client/R8-S1]`, `[web-client/R11-S1]` to `[web-client/R11-S3]`, `[web-client/R12-S1]` and `[web-client/R12-S3]` pass against the local stack
- [ ] 6.3 Add `e2e/auth.spec.ts`; verify `[web-auth/R1-S1]`, `[web-auth/R4-S1]`, `[web-auth/R5-S1]` and `[web-auth/R5-S10]` pass against the local stack
- [ ] 6.4 Verify `npm run lint`, `npm test`, `npm run test:e2e` and `scripts/check-scenario-coverage.sh` all pass, and that no scenario ID has two tests

## Workflow follow-up

- Archive the change in the same PR, so `openspec/specs/` matches the code.
- Mark the PR ready for the owner to merge.
