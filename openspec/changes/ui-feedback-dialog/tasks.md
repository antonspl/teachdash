# Tasks

Frontend-only change. Each scenario has a Vitest or Playwright test whose title starts with its `[<scenario ID>]`. Tests and code are reviewed together at the code gate.

## 1. Before screenshots

- [ ] 1.1 Capture the baseline at `d598859` in `frontend/screenshots/ui-feedback-dialog/before/`. Use the screens and states in the visual plan at desktop and 360 CSS pixels, with Russian at 360. Keep test data and response fixtures suitable for the after set. List unreachable states by name.

## 2. Shared banners and loading

- [ ] 2.1 Apply the three tone roles and shared icons to `StatusMessage`. Use the existing visual tokens and preserve the surrounding feedback spacing. Keep retry outside the alert. Cover the tone mapping and the retry cases (web-client/R31-S1, R31-S4).
- [ ] 2.2 Use plain localized text in the auth guards' startup loading state. Cover it with a held session request and English and Russian copy. Assert page-error placement, session-ended status, form-level alert placement and Profile success statuses (web-client/R31-S2, R31-S3, R31-S5 and R31-S6). Use rendered test cases for login, set-password, admin, profile and the cabinet.
- [ ] 2.3 Adjust reachability error and absence locators in `pages.test.tsx` and `skeleton.spec.ts` (web-client/R12-S2, R12-S4 and related tests). Use alert locators for error cases in `ProfilePage.test.tsx` (web-profile/R2-S6 and R3-S8). For R17-S1, R17-S2 and R17-S5 through R17-S7, check both status and alert absence in the affected form in the admin, login, set-password, Profile and Playwright field-error tests. Retain R29-S7 and R30 coverage. Use Playwright to check first-line alignment and uncut wrapping in a long Russian banner at 360.

## 3. Admin badges, list and IssuedLink

- [ ] 3.1 Apply the shared badge icons, tones and padding to the existing teacher statuses and Just added. Verify list loading, empty and retry states and the localized status badges. Update the 360 px admin e2e check to cover badge and action fit, and exercise the group with two badges and a long Russian label (web-admin-teachers/R1-S1 through R1-S4, R1-S6 and R1-S7). Keep the session-ended behavior covered by R1-S5.
- [ ] 3.2 Cover the link's readonly field, note, action hierarchy, successful copy and neutral manual-copy fallback. Preserve its current row or standalone placement and internal focus during remount. Connect Close to the saved issuing action, with Add teacher when needed. Verify the still-available row action and the replaced Reactivate action, and keep the failed reload and replacement cases covered (web-admin-teachers/R3-S1 through R3-S7).

## 4. Admin modal dialogs

- [ ] 4.1 Restyle the native Dialog and its footer for desktop and narrow screens. Add the localized header button and explicit initial focus targets. Keep existing opener restoration, busy dismissal rules and native-close recovery. Update the dialog icon assertions in `AdminTeachersPage.test.tsx` and the dialog border assertion in `cssRules.test.ts` (web-client/R23-S2 and R26-S2). Keep R23-S1, R26-S1 and R26-S3 green.
- [ ] 4.2 Cover opening focus and accessible names, header Close, footer Cancel, Escape, busy dismissal and native-close recovery, and post-deactivation focus in Playwright (web-client/R32-S1 through R32-S4). Check the long dialog title, Close target and footer at 360. Keep existing request and error cases green.

## 5. Design sources

- [ ] 5.1 Update the description of `StatusMessage` roles in the two accessibility guides and add the Dialog header icon exception to the design code's Button roles paragraph as described in design decision 5.

## 6. After screenshots and verification

- [ ] 6.1 Capture the final set in `frontend/screenshots/ui-feedback-dialog/` with the same desktop and 360 CSS px Russian states as the baseline. Compare banner alignment, spacing and wraps, dialog focus and action order, badge grouping and the link box. Report any unreachable states.
- [ ] 6.2 Run frontend lint, format check, unit tests, build and Playwright tests, along with `scripts/check-scenario-coverage.sh --strict`. Check the focused Playwright cases in the desktop and phone projects.

## Workflow follow-up

- Run the PR-level code and visual reviews. Request the code gate with its commit range and the final screenshot path.
- Archive the change in the same PR after approval, updating `web-client` and `web-admin-teachers`, and check scenario coverage.
- Mark the PR ready for final review by a code owner other than the engineer.
