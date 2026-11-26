# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Every task group touches `frontend/`, so each is one slice for the
`frontend-implementer`.

## 1. Before screenshots

- [ ] 1.1 From `main`, before any other change, take the before set into `frontend/screenshots/ui-icon-set/before/`: `/admin` in en, at desktop and 360, "Add teacher" default, hover and keyboard focus and one 200% zoom shot of `/admin` on desktop; list unreachable states (disabled and loading "Add teacher")

## 2. Icons and tokens

- [ ] 2.1 Add `--icon-size`, `--icon-size-small` and `--icon-size-rail` to `frontend/src/styles/tokens.css` (design D3); no `--icon-size-placeholder`
- [ ] 2.2 Add `frontend/src/ui/icons.tsx` (`SvgIcon` and one named component per icon of the design-code table, geometry copied from it) and `icons.module.css` (three size classes, `flex: none`, no `em`); move `PlusIcon` into it; delete `PlusIcon.tsx` and `PlusIcon.module.css`; change only the import in `Button.tsx` (design D1, D2); verify `npm run lint` (token check) and `npm run build`
- [ ] 2.3 First confirm whether Vitest gives distinct CSS-module class names in jsdom; if not, set `css.modules` (for example `classNameStrategy`) in `frontend/vite.config.ts` (Clarification 16)
- [ ] 2.4 Vitest in `frontend/src/ui/icons.test.tsx` for web-client/R21-S1, R21-S2 and R21-S3 (table-driven; one hand-copied literal list per icon, Clarification 11)
- [ ] 2.5 Vitest in `frontend/src/ui/icons.test.tsx` for web-client/R22-S1 (reads `tokens.css` as text) and R22-S2 (class per size, and the icon CSS module read as text: each size class sets width and height from its own token, no `em`)
- [ ] 2.6 Vitest in `frontend/src/pages/AdminTeachersPage.test.tsx` for web-client/R23-S1 and R23-S2 (buttons read by role and name, including "Create teacher"); the existing `Button.test.tsx` stays green; verify `npm test`
- [ ] 2.7 Playwright test for web-client/R22-S3 in `frontend/e2e/` (computed width and height of the plus on "Add teacher" are 20px, as `selectArrow.spec.ts:20-31` reads styles; on the phone project also no horizontal scroll on `/admin` at 360); verify `npm run test:e2e` on desktop and phone projects

## 3. After screenshots

- [ ] 3.1 Take the final screenshot set in `frontend/screenshots/ui-icon-set/`: same states and widths as the before set, including the 200% zoom shot of `/admin` on desktop; check by eye that the plus is optically centred on the label, the button is about 0.25rem wider with the same height, the focus ring is not clipped and the label stays on one line at 360; list unreachable states; if #211 is merged before the code gate, rebase and add an ru 360 shot, otherwise state in the PR that the ru fit was not checked
- [ ] 3.2 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e`; all pass, and every `web-client` R21, R22 and R23 scenario has a test

## Workflow follow-up

- `code-gate-reviewer` and `visual-reviewer` on the whole PR (no slice review for a change touching `frontend/`), then ask for the code gate in the PR with the commit range and the screenshot folder path. Unused icons are expected (Clarification 1).
- Archive the change in the same PR (`openspec/specs/web-client` gets R21, R22 and R23). Closes #195.
- Mark the PR ready for the final review by a code owner other than the engineer.
