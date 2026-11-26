# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate.
The title of every Vitest or Playwright test for a scenario starts with its
`[<scenario ID>]`. Unit tests of helper modules have no scenario and no ID. Existing
tests that change: the literals found in task 2.1.

## 1. Language core

- [ ] 1.1 Add `i18n/detectLanguage.ts` (pure detector), the storage wrapper, `initI18n(instance, env)`, `setUiLanguage` and `ru.json`; change `i18n/index.ts` to call `initI18n` before render (design D1, D2); Vitest for web-client/R19-S1 to R19-S7, R2-S2 and R2-S3, resetting language, storage and `lang` in `afterEach`; verify `npm test` and `i18n/translations.test.tsx`

## 2. Copy

- [ ] 2.1 Rewrite the `en.json` strings of design D6 after checking each current text; add `errors.TARGET_ACCOUNT_DEACTIVATED` to `en.json` and `ru.json`; grep the old strings of D6 in `frontend/e2e/*.spec.ts` and `frontend/src/**/*.test.tsx` and switch every match (at least `useFieldErrors.test.tsx:103` and `api/client.test.ts:76`) to the `en.<key>` value or the new text; Vitest for web-client/R2-S4 and R2-S5 in `client.test.ts`; verify `npm test`

## 3. The select

- [ ] 3.1 Add `--lang-select-width`, `--size-visually-hidden` and `--space-block` to `tokens.css`, the `.visuallyHidden` class, the `hideLabel` and `optionLang` props of `Select`, and `ui/UiLanguageSelect.tsx` with the `uiLanguage.*` keys in both resource files (design D3 to D5); Vitest for web-client/R20-S4 and R20-S6 in `UiLanguageSelect.test.tsx`, and the `Select` tests unchanged; verify `npm run lint` (token check) and `npm test`

## 4. Placement

- [ ] 4.1 Put the select last in `AppHeader`'s user group, and as a right-aligned first row inside `<main>` of `LoginPage` and `SetPasswordPage`, outside the form, `--space-block` between it and the title (first in the DOM and the first Tab stop on those two pages; left-aligned when it wraps in the header at 360); none on the not-found, loading, guard and `HomeRedirect` states; Vitest for web-client/R20-S1 to R20-S3, R20-S5 and R20-S7; verify `npm test`, with the existing header tests unchanged

## 5. End to end

- [ ] 5.1 Set `locale: 'en-US'` in `use` of `playwright.config.ts`; Playwright tests for web-client/R20-S8 and R20-S9 against `docker compose up --build --wait`, desktop and phone projects; verify `npm run test:e2e`
- [ ] 5.2 Take the final screenshot set in `frontend/screenshots/ui-i18n-locale/`: the header (cabinet, Profile, admin) and the login and set-password pages, at desktop and 360 px, in `en` and `ru`; check by eye that the select does not touch buttons or the title and that nothing overflows; list unreachable states; record the result in the PR

## 6. Design docs

- [ ] 6.1 Replace "informal and friendly, like talking to a friend" with a neutral tone in `.agents/design/design-code.md:367`, `.agents/design/project-rules.md:21-22` and C10, `.agents/design/frontend-design-copy.md:34`, `.agents/design/checklist.md:136`, `.agents/design/product-brief.md:45` and `:60` ("Friendly words, calm personality"), and `.agents/design/refactoring-ui-patterns.md:22`, keeping the other rules; proof: `grep -rn "informal\|friend" .agents/design/` shows no remaining requirement of informal copy in those files, recorded in the PR
- [ ] 6.2 In `.agents/design/design-code.md` (login and header sections) add one sentence that this change's placement (header, and inside `<main>` on login and set-password, not the login band of line 354) is temporary until #201, #202 and #205

## 7. Final checks

- [ ] 7.1 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every `web-client` R2, R19 and R20 scenario has a test

## Workflow follow-up

- `code-gate-reviewer` and `visual-reviewer` on the whole PR (no slice review for a change touching `frontend/`), then ask for the code gate in the PR with the commit range and the screenshot folder path.
- Archive the change in the same PR (`openspec/specs/web-client` gets R2 modified, R19 and R20). Closes #102, #92 and #172 (its content-language part goes to #114). #103's note that the switch becomes visible is superseded.
- When closing #172, note on the issue and in the PR description that #114 owns its second "Done when" bullet (content `lang`).
- Mark the PR ready for the final review by a code owner other than the engineer.
