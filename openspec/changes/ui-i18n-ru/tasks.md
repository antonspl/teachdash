# Tasks

Frontend-only change: tests and code in one pass, reviewed together at the code gate
(`openspec/config.yaml`). The title of every Vitest or Playwright test for a scenario
starts with its `[<scenario ID>]`. Existing tests that change: R2-S3, R20-S5, the R2-S2
cast, `uiLanguage.spec.ts:34`, and the page tests that move to `I18nextProvider` (task 1.3).

## 1. i18n core and tidy-ups

- [ ] 1.1 Move `leaves()` and `setPath()` from `translations.test.tsx` into a shared helper; add the pure parity helper and `i18n/parity.test.ts` (design D1); Vitest for web-client/R15-S1, R15-S2, R16-S1, R16-S2 and R16-S3, the failures listing the key names; Vitest for web-client/R16-S4 on a synthetic `ru` instance (design D2); verify `npm test`
- [ ] 1.2 Type `initI18n`'s `resources` seam as i18next `Resource` and drop `as never` in the R2-S2 test; remove `.use(initReactI18next)` from `initI18n`, wire the app instance in `src/i18n/index.ts` before `initI18n`, check the import order in `main.tsx`, and move rendering tests of other instances to `I18nextProvider`; proof: `grep -rn "as never\|initReactI18next" frontend/src` shows `initReactI18next` only in `i18n/index.ts` and no `as never` in the R2-S2 test; `npm test` and `npm run build` pass; recorded in the PR
- [ ] 1.3 Change the R2-S3 test (`detectLanguage.test.ts`) to use a test `ru` bundle with a missing key, and the R20-S5 test (`languageSwitch.test.tsx`) to use a test `ru` bundle with `uiLanguage.*` and no `login.title` (design D3); re-check R20-S7, `api/client.test.ts:578-586` and `e2e/uiLanguage.spec.ts` against the new `ru`; verify `npm test`

## 2. Russian copy

- [ ] 2.1 Fill `ru.json` with all 119 leaves from the table in design "UI design / Copy", keeping the existing four values; change `en.json` `ui.loading` to "Loading…"; check each value against the rules of Clarifications 6 to 14; Vitest for web-client/R15-S5 (copy rules on both bundles) and R15-S1; verify `npm test`
- [ ] 2.2 Vitest for web-client/R15-S3 (every page and dialog with `ru` active, no `en` text left) and R15-S4 (header role in lower case, colon forms with the name, the two admin row buttons); verify `npm test`
- [ ] 2.3 Playwright for web-client/R15-S6 (Russian pages at 360 px, no horizontal scroll, nothing cut off) against `docker compose up --build --wait`, phone projects; drop the redundant `toContainText('Русский')` in `frontend/e2e/uiLanguage.spec.ts:34`; verify `npm run test:e2e`

## 3. The select

- [ ] 3.1 `UiLanguageSelect` builds its options from `Object.keys(resources)` and uses `FALLBACK_LANGUAGE` instead of `['en','ru']` and `'en'`; `Select` gets an optional no-margin prop with its own class (the default margin stays without the prop) and `.wrap > div { margin: 0 }` is deleted; proof: R20-S4 and the `Select` tests pass unchanged, `grep` shows no `'en'` or `['en', 'ru']` literal in `UiLanguageSelect.tsx` and no `.wrap > div` rule, and the header screenshots at 1280 and 360 px show the select where it was; recorded in the PR

## 4. Design docs and follow-ups

- [ ] 4.1 Add the Russian copy rules of Clarifications 6 to 15 (glossary, verb forms, typography, gender and case patterns) to `.agents/design/design-code.md` "Copy and languages"; proof: the section lists each rule and agrees with `project-rules.md`, recorded in the PR
- [ ] 4.2 The main agent files two follow-up issues: native-language options sorted by the name in the current UI language (Clarification 16a), and translated time zone names (16b); link them in the PR description

## 5. Final checks and screenshots

- [ ] 5.1 Take the final screenshot set in `frontend/screenshots/ui-i18n-ru/`: every screen and state of the visual plan in design, in `ru` and `en`, at 1280 and 360 px; check by eye that nothing overflows or is cut off and that the two button pairs of Clarification 15 wrap as accepted; list unreachable states; record the result in the PR
- [ ] 5.2 Run `npm run lint`, `npm run format:check`, `npm test`, `npm run build`, `scripts/check-scenario-coverage.sh` and `npm run test:e2e` (desktop and phone projects); all pass, and every `web-client` R2, R15, R16 and R20 scenario has a test

## Workflow follow-up

- `code-gate-reviewer` and `visual-reviewer` on the whole PR (no slice review for a change touching `frontend/`), then ask for the code gate in the PR with the commit range and the screenshot folder path `frontend/screenshots/ui-i18n-ru/`.
- Archive the change in the same PR (`openspec/specs/web-client` gets R2 and R20 modified, R15 and R16 added).
- The PR description says it closes #103 and #209.
- Mark the PR ready for the final review by a code owner other than the engineer.
