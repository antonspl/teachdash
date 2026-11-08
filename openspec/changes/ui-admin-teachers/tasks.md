# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
Every Vitest or Playwright test title starts with its `[<scenario ID>]`.

## 1. Groundwork

- [x] 1.1 Add `web-admin-teachers` to `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`; verify the script lists the new scenarios as missing
- [x] 1.2 Add `src/api/adminTeachers.ts` with the five typed calls (design D2), mapping both link keys to one shape and parsing no body on 204; verify with `npm run lint` and `npm run build`
- [x] 1.3 Add the time zone list module (design D5) with unit tests for: `UTC` included, browser zone added and preselected, fallback when `supportedValuesOf` is missing; verify `npm test`
- [x] 1.4 Add the `showModal`/`close` stub for jsdom to the Vitest setup; verify `npm test` still passes

## 2. UI components

- [x] 2.1 Add `ui/Select`, `ui/StatusBadge`, `ui/List`/`ListItem` and the `danger` variant of `ui/Button`, using design tokens only (design D7); verify `npm run lint` passes the design-token check
- [x] 2.2 Add `ui/Dialog` on native `<dialog>` (design D4) with Escape mapped to cancel; verify `npm run lint` and `npm run build`
- [x] 2.3 Add `ui/IssuedLink` (design D6) with tests for web-admin-teachers/R3-S1 to R3-S4 (clipboard stubbed; the selection checked through `selectionStart`/`selectionEnd`); verify `npm test`

## 3. Teacher list

- [x] 3.1 Render `AdminTeachersPage` on `/admin` inside the cabinet layout (design D1), with the list, statuses (missing `hasPassword` read as `true`), empty state and load error with retry; add i18n strings in an informal tone; tests for web-admin-teachers/R1-S1 to R1-S3; verify `npm test` and that web-auth/R3-S1 and the routing tests still pass

## 4. Create a teacher

- [x] 4.1 Add the create form (name, email, time zone select) with link display, form reset and list reload, the link kept when the reload fails; Vitest for web-admin-teachers/R2-S2 to R2-S6 and R3-S5; verify `npm test`
- [x] 4.2 Add `loginAsAdminInUi(page)` to `e2e/accounts.ts` and the Playwright test for web-admin-teachers/R2-S1 (no status check, so it does not need #70); verify `npm run test:e2e` against `docker compose up --build --wait`

## 5. Deactivate and reactivate

- [x] 5.1 Add "Deactivate" with the confirm dialog (focus in and back) and "Reactivate" with link display, reload after each and the error message on failure; Vitest for web-admin-teachers/R4-S1 to R4-S8 and R1-S5; verify `npm test`

## 6. New links

- [x] 6.1 Add the per-row link button ("New invite link" / "Password reset link", none for deactivated rows) with one busy flag per row for all its buttons; Vitest for web-admin-teachers/R5-S1 to R5-S5; verify `npm test`
- [x] 6.2 Playwright test for web-admin-teachers/R1-S4 on the phone projects (no horizontal scroll with a long email and a shown link); verify `npm run test:e2e`

## 7. Final checks

- [x] 7.1 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every web-admin-teachers scenario has a test

## 8. Create form in a dialog (asked while trying the page by hand)

- [ ] 8.1 Add the green `success` variant to `ui/Button` (design tokens only); verify `npm run lint`
- [ ] 8.2 Move the create form into a `ui/Dialog` opened by "Add teacher" at the top of the page; on success close it, show the link and pin the new teacher first with a "just added" badge (design D9); Vitest for web-admin-teachers/R1-S2, R2-S2 to R2-S8 and R3-S5 updated to the dialog; verify `npm test`
- [ ] 8.3 Update the Playwright tests for web-admin-teachers/R2-S1 (opens the dialog, new teacher is the first row) and R1-S4 (dialog fits 360 pixels); verify `npm run test:e2e` on all projects
- [ ] 8.4 Run the full checks of 7.1 again; all pass and every scenario has a test

## Workflow follow-up

- Opus review of the code, then ask for the code gate in the PR with the commit range.
- Archive the change in the same PR (`openspec/specs/web-admin-teachers` created,
  `web-auth` R3 updated).
- Rebase onto `main` once #67 (ui-auth) is merged.
- Mark the PR ready for the final review by a code owner other than the engineer.
