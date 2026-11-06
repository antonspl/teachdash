# Tasks

Frontend-only change. Each group lands its own tests: Vitest or Playwright, one per
scenario, with `[<scenario ID>]` at the start of the test title.

## 1. Shared UI and strings

- [ ] 1.1 Add `TextInput`, `PasswordInput` (with show toggle), `Button` and `AppHeader` to `src/ui/` using design tokens; verify `npm run lint` passes
- [ ] 1.2 Add the new strings to `src/i18n/en.json` (login, cabinets, header, set-password, session ended, link messages); verify `npm run build` passes

## 2. Session state and API client

- [ ] 2.1 Add the `onUnauthorized` hook to `apiRequest`, skipping the auth endpoints and `/api/me`; verify with a Vitest unit test
- [ ] 2.2 Add `AuthProvider` (load `/api/me`, `login`, `logout`, `setPasswordWithLink`, session-ended reason); verify with Vitest unit tests

## 3. Routing and cabinets

- [ ] 3.1 Add `RequireRole`, `AnonymousOnly` and `HomeRedirect`, and the `/admin`, `/teacher`, `/student` routes in `App.tsx`; verify `[web-auth/R2-S1]` to `[web-auth/R2-S6]` in Vitest
- [ ] 3.2 Add the three empty cabinet pages with the header; verify `[web-auth/R3-S1]` in Vitest

## 4. Login and logout

- [ ] 4.1 Add the login page with the reachability warning and the "ask for a link" text; remove the placeholder page; verify `[web-auth/R1-S2]` to `[web-auth/R1-S5]`, `[web-client/R2-S1]`, `[web-client/R12-S2]` and `[web-client/R12-S4]` in Vitest (move or rename the old placeholder-page tests)
- [ ] 4.2 Wire "Log out" in the header; verify `[web-auth/R4-S2]` in Vitest
- [ ] 4.3 Handle the session ending during use and the return to the page; verify `[web-auth/R6-S1]` to `[web-auth/R6-S3]` in Vitest

## 5. Set-password page

- [ ] 5.1 Add the `/set-password` page (token from the fragment, two fields, client checks, 410 and deactivated messages); verify `[web-auth/R5-S2]` to `[web-auth/R5-S7]` in Vitest

## 6. End-to-end

- [ ] 6.1 Add an E2E helper that logs in as the admin from `.env` and creates a teacher with a unique email to get a set-password link
- [ ] 6.2 Update `e2e/skeleton.spec.ts` for the replaced `web-client` scenarios (old R1 and R3 tests renamed to R11 and R12); verify `[web-client/R11-S1]` to `[web-client/R11-S3]` and `[web-client/R12-S1]`, `[web-client/R12-S3]` pass against the local stack
- [ ] 6.3 Add `e2e/auth.spec.ts`; verify `[web-auth/R5-S1]`, `[web-auth/R1-S1]` and `[web-auth/R4-S1]` pass against the local stack
- [ ] 6.4 Verify `npm run lint`, `npm test`, `npm run test:e2e` and `scripts/check-scenario-coverage.sh` all pass

## Workflow follow-up

- Archive the change in the same PR, so `openspec/specs/` matches the code.
- Mark the PR ready for the owner to merge.
