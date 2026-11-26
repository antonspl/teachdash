# Proposal

Issues: #103 (closes it) and #209 (closes it). Background: #102 (`ui-i18n-locale`, PR #208, archived), which shipped detection, the stored choice and the visible language switch, and left `ru.json` with 4 leaves.

## Clarifications

Decisions by the engineer:

1. Scope: every key currently in `en.json` (119 leaves; #103's "76" is stale) gets a Russian value. #103's line "the language switch becomes visible" no longer applies (#102 Clarification 3).
2. Russian plural forms are supported (`_one/_few/_many/_other`, i18next JSON v4) and tested on synthetic test bundles only. No real count key is added to the UI just for the test (no `{{count}}` exists in `en.json` today).
3. The key-parity test is strict: every `en` leaf has a non-empty `ru` leaf; no `ru`-only keys except plural suffixes; each plural base has exactly the categories of `Intl.PluralRules(lang).resolvedOptions().pluralCategories` for its language (en one/other, ru one/few/many/other); the same set of `{{vars}}` per base key (`count` may be absent in `_one`/`_few`/`_many`); failures list the key names.
4. #209 "language list from resources" (`UiLanguageSelect.tsx` uses `Object.keys(resources)` and `FALLBACK_LANGUAGE` instead of the `['en','ru']` and `'en'` literals) is a task with proof only, not a new scenario. R20-S4 already fixes the option order.
5. The rest of #209 is in scope as tasks: `Select` gets a no-margin prop or class instead of `.wrap > div { margin: 0 }` in `UiLanguageSelect.module.css`; `initI18n`'s test seam is typed as i18next `Resource` (drops `as never` in the R2-S2 test); `initI18n` stops calling `.use(initReactI18next)` on every instance (the app instance is wired in `src/i18n/index.ts`, import order checked, and rendering tests use `I18nextProvider`); the redundant `toContainText('Русский')` in `frontend/e2e/uiLanguage.spec.ts:34` is dropped (`toHaveValue('ru')` stays). Changed existing tests go through the tests gate.
6. Copy rules, confirmed from the sources: formal "вы", neutral tone, second person, sentence case (project rules C1, C10; #102 Clarification 10); "Учителя" / "учитель" everywhere, never "преподаватель" (`design-code.md` "Copy and languages"); «» quotes (C11); `…` for the ellipsis, "Загрузка…"; a non-breaking space between a number and its unit («8 символов», «72 байта», «7 дней»); text wraps and is never truncated (`design-code.md:364`); `app.name` stays "teachdash" in Russian; language endonyms are the same in both languages (R20-S4); no new tokens and no CSS or layout change for the Russian text.
7. Verb forms: buttons in the infinitive («Сохранить», «Войти»), messages in the imperative («Обновите список»), "вы" in lower case.
8. No «пожалуйста» in Russian, even where `en` keeps "Please".
9. Glossary: учитель, ученик, администратор, учётная запись, деактивировать, ссылка для сброса пароля, часовой пояс, родной язык, кабинет, профиль; Вход (page title) / Войти (button) / Выйти; email = «Электронная почта»; session = «сеанс»; Reactivate = «Активировать снова».
10. The admin row button for an active teacher without a password ("New invite link", `adminTeachers.actions.inviteLink`) is «Новое приглашение»; the one for a teacher with a password ("Password reset link") is «Ссылка для сброса пароля». «Новая ссылка-приглашение» was rejected as sounding odd.
11. Teacher status badges agree with «учётная запись» (feminine): «Активна», «Ожидает пароля», «Деактивирована», «Только что добавлена».
12. Names stay in the base case via colon forms: «Деактивировать: {{name}}?», «Ссылка для входа: {{name}}»; «Ученики этого учителя…» instead of a possessive pronoun. «Показать {{label}}» keeps the label as is (WCAG 2.5.3).
13. The role in the app header is lower case in Russian: «Анна (учитель)».
14. Always write ё. Also fix `en` "Loading..." to "Loading…" in this change.
15. At 360 px the button pairs «Отмена» + «Создать учителя» (create dialog) and «Скопировать» + «Закрыть» (issued link) may go onto two lines; labels are not shortened. Admin action labels stay at about 24 characters or less.
16. Out of scope, follow-up issues to file: (a) native-language options keep backend code order; sorting by the name in the current UI language is a separate issue; (b) time zone names stay IANA ids; translating them is a separate issue. The browser's native `required` bubble is already covered by #165 (no new issue).
17. The full key to Russian value table goes in `design.md`, so the engineer reviews the Russian copy at the spec gate.
18. The Russian copy rules (decisions 6 to 15: glossary, verb forms, typography, gender and case patterns) are added to `.agents/design/design-code.md` "Copy and languages" in this PR, so later changes write Russian the same way.

## Research angles and conclusions

Angles agreed with the engineer: i18n plumbing and plurals (technical), and the UI copy and layout in Russian (`frontend-researcher`).

- Plumbing: `initI18n.ts` takes `resources {en, ru}` (one namespace), `supported = Object.keys(bundles)` and `fallbackLng: 'en'`. A full `ru.json` plugs in with no init change; `detectLanguage.ts` is unchanged.
- Plurals: with i18next 26.6.0 on Node, `Intl.PluralRules('ru')` gives one/few/many/other. If `ru` lacks `_few` or `_many`, i18next falls back to the `en` `_other` text silently, so only the parity test catches it.
- Tests: `translations.test.tsx` has `leaves()` and `setPath()` helpers that the parity test reuses through a shared helper. Tests that read the real `ru` today: `src/api/client.test.ts:578-586`, `e2e/uiLanguage.spec.ts`, and two that assume `ru` lacks `login.title`: `detectLanguage.test.ts:157` (R2-S3) and `languageSwitch.test.tsx:182-183` (R20-S5). Most page tests run in `en`.
- UI: 115 of 119 leaves are missing in `ru` (adminTeachers 23, profile 18, errors 14, languages 13, login 7, ui 6, issuedLink 6, fieldErrors 5, setPassword 5, header 4, notFound/cabinet/roles/password 3 each, app 1, startup 1). Text outside i18n: IANA time zone ids (decision 16b), the browser `required` bubble (#165), `<title>` (the brand). From the CSS, nothing overflows; at 360 px two button rows wrap (decision 15). No CSS or token change, no conflict with `design-code.md`.
- Risks: removing `.use(initReactI18next)` from `initI18n` needs the app instance wired before init; a `Select` no-margin prop must not fall back to the default margin, or the header select drops by 1rem.

## What changes

- `web-client` R2 modified (the `ru` resources are complete; a missing key still falls back to `en`; R2-S3 uses a test bundle). R20 modified (R20-S5 no longer relies on `ru.json` lacking `login.title`). New R15 (complete Russian strings, shown in Russian, key parity, copy rules, 360 px) and R16 (Russian plural forms).
- `ru.json` gets all 119 leaves; `en.json` "Loading..." becomes "Loading…".
- A strict parity test and a plural test on synthetic bundles.
- The #209 tidy-ups (language list from resources, `Select` no-margin prop, `Resource` typing and no global `initReactI18next` swap, one redundant e2e line).
- The Russian copy rules go into `.agents/design/design-code.md`.
- Final screenshot set in `ru` and `en`, at 1280 and 360 px.

## Out of scope

- Sorting the native-language options by the name in the current UI language (follow-up issue, decision 16a).
- Translating time zone names (follow-up issue, decision 16b).
- The browser's native `required` bubble (#165).
- The width shift of the password field when the toggle changes «Показать» to «Скрыть» (about 15 px); fields belong to redesign step 3a (#197).
- A real count string in the UI, the content language of lessons (#114), new dependencies, backend changes, CSS, layout and token changes.
