# Design

## Context

- `frontend/src/i18n/initI18n.ts` builds `resources = {en, ru}` (one namespace), computes `supported = Object.keys(bundles)` and starts i18next with `fallbackLng: 'en'`. `ru.json` has 4 leaves; `en.json` has 119. `detectLanguage.ts` needs no change.
- With i18next 26.6.0, `Intl.PluralRules('ru')` gives one/few/many/other, and a Russian bundle without `_few` or `_many` silently shows the `en` `_other` text. Only a test can catch it.
- `i18n/translations.test.tsx` already has `leaves()` and `setPath()` helpers.
- `ui/UiLanguageSelect.tsx` hard-codes `['en','ru']` and `'en'`; its CSS removes the `Select` margin with `.wrap > div { margin: 0 }`; `initI18n` calls `.use(initReactI18next)` on every instance (#209).

## Goals / Non-Goals

**Goals:** a complete, reviewed Russian UI; a strict parity test that keeps `ru` complete from now on; plural support proved on synthetic bundles; the #209 tidy-ups; the Russian copy rules written into the design code.

**Non-Goals:** sorting native-language options by name (follow-up issue), translating time zone names (follow-up issue), the browser `required` bubble (#165), a real count string, content language (#114), password field width shift (see Risks). No new dependency, so there is nothing to justify. No Flyway migration. No new token.

## Decisions

### D1 Strict key-parity test (R15-S1, R15-S2, R16-S1 to R16-S3)

A pure helper in `frontend/src/i18n/` takes two bundles and returns a list of problems, each with the key name. `leaves()` and `setPath()` move from `translations.test.tsx` into a shared helper that both tests import. A new `i18n/parity.test.ts` runs it on the real `en.json` and `ru.json` (R15-S1) and on synthetic bundles for the failure cases. Rules:

- every `en` leaf has a non-empty `ru` leaf; a `ru` leaf without an `en` counterpart is a problem, except plural suffixes of a base that `en` has (R16);
- a plural base is a key whose last segment ends in `_zero`, `_one`, `_two`, `_few`, `_many` or `_other`. Per language, the set of suffixes of each base equals `Intl.PluralRules(lang).resolvedOptions().pluralCategories` (`en`: one, other; `ru`: one, few, many, other);
- the set of `{{variables}}` of every leaf, plain and plural, is the same in every language; for a plural base the baseline is the variable set of `en`'s `_other` text, and `ru` `_one`, `_few` and `_many` may drop only `count` from it (an extra variable fails);
- every `ru` value contains a Cyrillic letter, except an explicit allow-list (`app.name` and the language endonyms `uiLanguage.name.*`); no `ru` value contains a deny-listed form («учетн», «пожалуйста», and the whole word «еще»), compared case-insensitively; this is part of the copy-rules check (R15-S5);
- the failure message lists the key names, with the language and the problem.

The real bundles have no plural base today, so the plural rules run only on synthetic bundles.

### D2 Plural forms (R16-S4)

i18next JSON v4 already selects `_one/_few/_many/_other` from `Intl.PluralRules`. The code needs no change. The test creates a synthetic `ru` instance with one count key in four categories and checks the counts 0, 1, 1.5, 2, 5, 11, 21 and 22 with four distinct texts: 1 and 21 give `_one`, 2 and 22 `_few`, 0, 5 and 11 `_many`, and 1.5 `_other`. No count key is added to `en.json` or `ru.json`.

### D3 `ru.json` and the existing tests (R2, R20-S5)

`ru.json` gets all 119 leaves (table in "UI design / Copy"), the existing four keep their values. The tests that assumed an incomplete `ru` change:

- R2-S3 (`detectLanguage.test.ts:157`) uses a test `ru` bundle with a missing key through `initI18n`'s `resources` seam, instead of the real `ru`.
- R20-S5 (`languageSwitch.test.tsx:182-183`) uses a test `ru` bundle with `uiLanguage.*` and no `login.title`, so the per-key fallback is still proved while the title stays English. `UiLanguageSelect` imports the global `setUiLanguage` (`UiLanguageSelect.tsx:2`), so a test `ru` bundle on a separate instance cannot drive it. The test swaps the bundle on the global instance: `removeResourceBundle('ru', 'translation')` then `addResourceBundle('ru', 'translation', {uiLanguage: …})`, and restores the real bundle in `afterEach`.
- R20-S7 (`languageSwitch.test.tsx`) is re-checked: the next failed login shows the error in Russian, which the scenario already allows; the wording stays.
- `src/api/client.test.ts:578-586` and `e2e/uiLanguage.spec.ts` read the real `ru`; they are re-checked against the new values.

### D4 #209 tidy-ups (tasks with proof, no new scenario except where noted)

- Language list: `UiLanguageSelect` builds its options from the instance's own supported languages (`Object.keys(i18n.options.resources ?? {})`, or a `supported` list that `initI18n` returns next to `setUiLanguage`), not from the static `resources` export, so the select never lists a language that `setUiLanguage` rejects. `FALLBACK_LANGUAGE` replaces the `'en'` literal. R20-S4 still fixes the order (English, Русский) and passes unchanged. Proof: a Vitest where the select lists exactly the instance's languages, including a third synthetic one.
- `Select` gets an optional boolean prop `flush` that adds a CSS class with no bottom margin; without the prop the default margin stays. `.wrap > div { margin: 0 }` is deleted. A unit test in `Select.test.tsx`: with `flush` the root has the class, without it the default class stays and the flush class is absent. The header select must not move: the final screenshots show the header at 1280 and 360 px.
- `initI18n`'s `resources` seam is typed as i18next's `Resource`; the `as never` in the R2-S2 test goes. `initI18n` no longer calls `.use(initReactI18next)`. The app instance is wired once in `src/i18n/index.ts` (`i18next.use(initReactI18next)` before `initI18n`, checked against the import order in `main.tsx`: `i18n` is imported before the first render), and the two test files that render other instances (`i18n/detectLanguage.test.ts`, `i18n/translations.test.tsx`) already use `I18nextProvider`, and no other test calls `initI18n`, so no rendering test moves.
- `frontend/e2e/uiLanguage.spec.ts:34`: the line `toContainText('Русский')` is dropped; `toHaveValue('ru')` is the real check.

### D5 Russian copy rules (decisions 6 to 15)

The rules are written into `.agents/design/design-code.md` "Copy and languages" in this PR: glossary, verb forms (buttons in the infinitive, messages in the imperative), «вы» in lower case, no «пожалуйста», colon forms for names and labels (a name stays in the base case; the password toggle name is «Показать: {{label}}»), status badges in the feminine to agree with «учётная запись», lower-case role in the header, ё, «» quotes, `…`, a non-breaking space between a number and its unit, text wraps and is never truncated. R15-S5 checks the mechanical rules on the bundles. The rest is reviewed by the engineer at the spec gate through the table below.

### D6 Tests and screenshots

- Vitest: parity and copy-rules tests (R15-S1, R15-S2, R15-S5, R16-S1 to R16-S4); a bundle test that every `adminTeachers.actions.*` `ru` value is at most 24 characters; page tests with `ru` active for R15-S3 and R15-S4 (pages in a shared render helper with the real `ru`). R15-S3 computes «no text equals an `en` value» as follows: collect the text content of every rendered element and the `aria-label`, `title`, `placeholder` and `alt` attributes of every element, trim them, and fail when one equals an `en` leaf value, comparing against every `en` leaf value minus the allow-list of three values (`teachdash`, `English`, `Русский`; `Русский` is both `languages.ru` in `ru` and the `en` value of `uiLanguage.name.ru`). The page list and the message-state sweep are the explicit list in R15-S3; check it against `en.json` once more at implementation.
- Playwright: R15-S6 at 360 px, and at 768 px for the admin list, in `ru`, for the pages and dialogs of the scenario.
- Final screenshot set: every screen and state of the visual plan in `ru` and `en`, at 1280 and 360 px, in `frontend/screenshots/ui-i18n-ru/`.

## UI design

### Screens touched

Every existing screen, for text only: login, set-password, admin teachers (list, create dialog, deactivate dialog, issued-link dialog), Profile, the app header (cabinet, Profile, admin), not-found, teacher and student cabinet stubs, startup and loading states. No layout, CSS or token change.

### Layout (desktop and 360 px)

- At 1024 px and wider, pages stay one 40rem column. At 360 px the column is 328 px wide.
- Nothing overflows in Russian (estimated from the CSS; the screenshots and R15-S6 confirm). At 360 px two button rows wrap onto two lines: «Отмена» + «Создать учителя» (create dialog) and «Скопировать» + «Закрыть» (issued link). Labels are not shortened. Admin row action labels stay at most 24 characters (bundle test).
- The admin page title «Кабинет администратора» may wrap to two lines at 360.
- Text wraps and is never truncated (`design-code.md:364`).

### States per screen

No state is added or removed. Every existing state gets Russian text: login (default, unreachable banner, session-ended banner, failed login), set-password (form with hints, incomplete link, error), admin teachers (loading, empty, load error, list, dialogs), Profile (default, saved, errors, password changed), startup (loading, error with retry), not-found.

### Copy

The table lists every key of `en.json` (119 leaves) with its current `en` text and its `ru` value. The `ru` values are the proposal for the engineer's review at the spec gate. Three values hold a non-breaking space (U+00A0) between a number and its unit: `password.tooShort` («8 символов»), `password.tooLong` («72 байт») and `issuedLink.note` («7 дней»). In the table below `␣` marks each U+00A0 so the engineer can see it; in `ru.json` it is the real character U+00A0, not the symbol, and R15-S5 asserts the character code. `roles.*` are lower case in Russian because the header shows «Анна (учитель)». Only `ui.loading` changes in `en`.

| Key | en | ru |
|---|---|---|
| `app.name` | teachdash | teachdash |
| `notFound.title` | Page not found | Страница не найдена |
| `notFound.message` | There is nothing at this address. | По этому адресу ничего нет. |
| `notFound.home` | Go to the start page | Перейти на стартовую страницу |
| `errors.generic` | Something went wrong. Please try again. | Что-то пошло не так. Попробуйте ещё раз. |
| `errors.UNAUTHENTICATED` | You are not signed in. Please sign in and try again. | Вы не вошли в систему. Войдите и попробуйте ещё раз. |
| `errors.FORBIDDEN` | You do not have permission to do this. | У вас нет прав на это действие. |
| `errors.CSRF_INVALID` | Your session check failed. Please reload the page and try again. | Не удалось проверить ваш сеанс. Перезагрузите страницу и попробуйте ещё раз. |
| `errors.VALIDATION_FAILED` | Some of the entered values are not valid. | Некоторые введённые значения недопустимы. |
| `errors.MALFORMED_REQUEST` | The request could not be understood. | Не удалось понять запрос. |
| `errors.INTERNAL_ERROR` | Something went wrong on our side. Please try again later. | Что-то пошло не так на нашей стороне. Попробуйте позже. |
| `errors.EMAIL_TAKEN` | An account with this email already exists. | Учётная запись с этой электронной почтой уже существует. |
| `errors.NOT_FOUND` | We could not find what you asked for. | Не удалось найти то, что вы запросили. |
| `errors.LINK_CONFLICT` | This link cannot be used right now. Please try again or ask for a new link. | Эту ссылку сейчас нельзя использовать. Попробуйте ещё раз или запросите новую ссылку. |
| `errors.LINK_INVALID` | This link is not valid or has expired. Please ask for a new one. | Эта ссылка недействительна или устарела. Запросите новую. |
| `errors.ACCOUNT_DEACTIVATED` | This account is deactivated. | Эта учётная запись деактивирована. |
| `errors.WRONG_PASSWORD` | The password is incorrect. | Пароль неверный. |
| `errors.INVALID_CREDENTIALS` | The email and password do not match. | Электронная почта и пароль не совпадают. |
| `errors.TARGET_ACCOUNT_DEACTIVATED` | This account has been deactivated, so a reset link cannot be created. Reload the list to see the current status. | Эта учётная запись деактивирована, поэтому ссылку для сброса пароля создать нельзя. Обновите список, чтобы увидеть актуальный статус. |
| `fieldErrors.REQUIRED` | Please fill this in. | Заполните это поле. |
| `fieldErrors.TOO_SHORT` | This value is too short. | Значение слишком короткое. |
| `fieldErrors.TOO_LONG` | This value is too long. | Значение слишком длинное. |
| `fieldErrors.INVALID_FORMAT` | This value is not valid. Please check it. | Значение недопустимо. Проверьте его. |
| `fieldErrors.INVALID_TYPE` | This value is not valid. Please check it. | Значение недопустимо. Проверьте его. |
| `ui.show` | Show | Показать |
| `ui.hide` | Hide | Скрыть |
| `ui.showPassword` | Show {{label}} | Показать: {{label}} |
| `ui.hidePassword` | Hide {{label}} | Скрыть: {{label}} |
| `ui.tryAgain` | Try again | Повторить попытку |
| `ui.loading` | Loading… (was "Loading...") | Загрузка… |
| `languages.choose` | Choose a language | Выберите язык |
| `languages.required` | Please choose a native language. | Выберите родной язык. |
| `languages.loadFailed` | The list of languages could not be loaded. | Не удалось загрузить список языков. |
| `languages.de` | German | Немецкий |
| `languages.en` | English | Английский |
| `languages.es` | Spanish | Испанский |
| `languages.fr` | French | Французский |
| `languages.it` | Italian | Итальянский |
| `languages.pl` | Polish | Польский |
| `languages.pt` | Portuguese | Португальский |
| `languages.ru` | Russian | Русский |
| `languages.sr` | Serbian | Сербский |
| `languages.uk` | Ukrainian | Украинский |
| `login.title` | Sign in | Вход |
| `login.email` | Email | Электронная почта |
| `login.password` | Password | Пароль |
| `login.submit` | Sign in | Войти |
| `login.linkHint` | No password, or forgot it? Ask your teacher or administrator for a new link. | Нет пароля или вы его забыли? Попросите у учителя или администратора новую ссылку. |
| `login.sessionEnded` | Your session has ended. Please sign in again. | Ваш сеанс завершён. Войдите снова. |
| `login.unreachable` | The server cannot be reached. | Не удаётся связаться с сервером. |
| `cabinet.admin` | Admin cabinet | Кабинет администратора |
| `cabinet.teacher` | Teacher cabinet | Кабинет учителя |
| `cabinet.student` | Student cabinet | Кабинет ученика |
| `roles.ADMIN` | Admin | администратор |
| `roles.TEACHER` | Teacher | учитель |
| `roles.STUDENT` | Student | ученик |
| `header.logout` | Log out | Выйти |
| `header.logoutError` | Could not log out. Please try again. | Не удалось выйти. Попробуйте ещё раз. |
| `header.profile` | Profile | Профиль |
| `header.backToCabinet` | Back to cabinet | Назад в кабинет |
| `password.mismatch` | The passwords do not match. | Пароли не совпадают. |
| `password.tooShort` | The password must be at least 8 characters. | Пароль должен быть не короче 8␣символов. |
| `password.tooLong` | The password is too long (at most 72 bytes). | Пароль слишком длинный (не более 72␣байт). |
| `setPassword.title` | Set your password | Задайте пароль |
| `setPassword.password` | New password | Новый пароль |
| `setPassword.confirm` | Repeat the password | Повторите пароль |
| `setPassword.submit` | Set password | Задать пароль |
| `setPassword.incompleteLink` | This link is incomplete. Please use the full link you were given or ask for a new one. | Эта ссылка неполная. Воспользуйтесь полной ссылкой, которую вам дали, или запросите новую. |
| `startup.error` | We could not load your session. Check the connection and try again. | Не удалось загрузить ваш сеанс. Проверьте подключение и повторите попытку. |
| `issuedLink.label` | Link for {{name}} | Ссылка для входа: {{name}} |
| `issuedLink.note` | You can see this link only now. It works once, within 7 days, and replaces any earlier unused link. Do not open it in this browser, or your admin session will end. | Вы увидите эту ссылку только сейчас. Она действует один раз, в течение 7␣дней, и заменяет любую предыдущую неиспользованную ссылку. Не открывайте её в этом браузере, иначе ваш сеанс администратора завершится. |
| `issuedLink.copy` | Copy | Скопировать |
| `issuedLink.close` | Close | Закрыть |
| `issuedLink.copied` | Copied. | Скопировано. |
| `issuedLink.copyByHand` | The link could not be copied. It is selected, so you can copy it by hand. | Не удалось скопировать ссылку. Она выделена, скопируйте её вручную. |
| `adminTeachers.loadError` | Could not load the teachers. | Не удалось загрузить учителей. |
| `adminTeachers.empty` | No teachers yet. Use the button above to add the first one. | Учителей пока нет. Добавьте первого с помощью кнопки выше. |
| `adminTeachers.status.ACTIVE` | Active | Активна |
| `adminTeachers.status.WAITING` | Waiting for password | Ожидает пароля |
| `adminTeachers.status.DEACTIVATED` | Deactivated | Деактивирована |
| `adminTeachers.add` | Add teacher | Добавить учителя |
| `adminTeachers.justAdded` | Just added | Только что добавлена |
| `adminTeachers.create.title` | Add a teacher | Добавление учителя |
| `adminTeachers.create.cancel` | Cancel | Отмена |
| `adminTeachers.create.name` | Name | Имя |
| `adminTeachers.create.email` | Email | Электронная почта |
| `adminTeachers.create.timeZone` | Time zone | Часовой пояс |
| `adminTeachers.create.nativeLanguage` | Native language | Родной язык |
| `adminTeachers.create.submit` | Create teacher | Создать учителя |
| `adminTeachers.create.validationFailed` | Some values were not accepted. Please check the name, the email, the time zone and the native language. | Некоторые значения не приняты. Проверьте имя, электронную почту, часовой пояс и родной язык. |
| `adminTeachers.actions.deactivate` | Deactivate | Деактивировать |
| `adminTeachers.actions.reactivate` | Reactivate | Активировать снова |
| `adminTeachers.actions.inviteLink` | New invite link | Новое приглашение |
| `adminTeachers.actions.resetLink` | Password reset link | Ссылка для сброса пароля |
| `adminTeachers.deactivate.title` | Deactivate {{name}}? | Деактивировать: {{name}}? |
| `adminTeachers.deactivate.body` | {{name}} will no longer be able to sign in, and their students will be released. The students are not restored if you reactivate the account later. | {{name}} больше не сможет войти в систему, а ученики будут откреплены от учителя. Если позже активировать учётную запись снова, ученики не вернутся. |
| `adminTeachers.deactivate.confirm` | Confirm | Подтвердить |
| `adminTeachers.deactivate.cancel` | Cancel | Отмена |
| `profile.title` | Your profile | Ваш профиль |
| `profile.email` | Your email: {{email}} | Ваша электронная почта: {{email}} |
| `profile.profileHeading` | About you | О вас |
| `profile.passwordHeading` | Password | Пароль |
| `profile.name` | Name | Имя |
| `profile.timeZone` | Time zone | Часовой пояс |
| `profile.nativeLanguage` | Native language | Родной язык |
| `profile.save` | Save | Сохранить |
| `profile.saved` | Saved. | Сохранено. |
| `profile.validationFailed` | Some values were not accepted. Please check the name and the time zone. | Некоторые значения не приняты. Проверьте имя и часовой пояс. |
| `profile.validationFailedTeacher` | Some values were not accepted. Please check the name, the time zone and the native language. | Некоторые значения не приняты. Проверьте имя, часовой пояс и родной язык. |
| `profile.currentPassword` | Current password | Текущий пароль |
| `profile.newPassword` | New password | Новый пароль |
| `profile.repeatPassword` | Repeat the new password | Повторите новый пароль |
| `profile.changePassword` | Change password | Изменить пароль |
| `profile.passwordChanged` | Your password has been changed. You were signed out on your other devices. | Ваш пароль изменён. На других устройствах сеансы завершены. |
| `profile.wrongPassword` | The current password is incorrect. Please try again. | Текущий пароль неверный. Попробуйте ещё раз. |
| `profile.passwordValidationFailed` | The passwords were not accepted. Please check them and try again. | Пароли не приняты. Проверьте их и попробуйте ещё раз. |
| `uiLanguage.label` | Language | Язык |
| `uiLanguage.name.en` | English | English |
| `uiLanguage.name.ru` | Русский | Русский |

### A11y

- Focus order and keyboard are unchanged; only the text changes.
- Focus on a language switch: the select keeps focus (R20). Dialogs are modal (`frontend/src/ui/Dialog.tsx` calls `showModal()`), so the header select cannot be reached while a dialog is open; no focus rule for a language switch inside a dialog is needed (Clarification 22).
- Accessible names follow the Russian visible text: «Показать: {{label}}» and «Скрыть: {{label}}» keep the label as is and start with the visible word (WCAG 2.5.3 Label in Name). The hidden label of the language select is «Язык».
- `<html lang>` follows the UI language already (R19-S6). Option `lang` attributes stay.
- Error banners keep `role="alert"`, success banners `role="status"`.

### Components and tokens

- Reused: every existing component and token; there are no new tokens.
- Changed: `Select` gets an optional boolean `flush` prop (D4) with its own class in the existing CSS module; `UiLanguageSelect` uses it.

### Visual plan per screen and state

All screens: no layout, CSS or token changes; the existing tokens are reused and there are no new tokens. Only the text changes. Russian copy follows the glossary, «вы», infinitive buttons, «», `…`, non-breaking space between number and unit, and ё. Text wraps and is never truncated (design-code:364). Screenshots in `ru` and `en` at 1280 and 360 px.
- Login (default, unreachable banner, session-ended banner, failed login):
  - Primary: the title «Вход» and the «Войти» button. Secondary: labels «Электронная почта» and «Пароль», the toggle «Показать»/«Скрыть». Tertiary: the muted link hint.
  - Language row unchanged (select 9rem, "Русский"). Banners wrap within the column.
  - Density as today. At 360, one column 328 px wide, nothing on two lines except the hint.
- Set password (form with hints, incomplete link, error):
  - Primary: title «Задайте пароль» and the submit button. Hints «…не короче 8␣символов» and «…не более 72␣байт» use a non-breaking space (`␣` marks U+00A0).
  - The incomplete-link danger banner wraps. Layout unchanged.
- Admin teachers, list:
  - Primary: the «Добавить учителя» toolbar button and the title «Кабинет администратора» (it may wrap to two lines at 360; accepted).
  - Each row: name, then status badges («Активна», «Ожидает пароля», «Деактивирована», «Только что добавлена»). Badges wrap under the name when narrow and stay on one line each.
  - Actions «Ссылка для сброса пароля» / «Новое приглашение» + «Деактивировать» fit on one line at 1280. At 360 they stack. Each label is at most 24 characters. Between 360 px and 72rem: the current CSS has no `72rem` container condition (it comes with the app frame, #201), so the existing wrapping and stacking rule applies; R15-S6 checks the list at 768 px.
- Admin teachers, loading / empty / load error: «Загрузка…»; the empty-list text pointing to the button above; error text + «Повторить попытку». Same spacing as today.
- Create dialog: title, four labelled fields, error banner. Buttons «Отмена» (secondary) and «Создать учителя» (primary). At 360 they may sit on two lines, with `--space-button-gap` between them.
- Deactivate dialog: title in colon form with the name. The phrase «Ученики будут откреплены от учителя» replaces «ученики этого учителя будут освобождены» inside the full body of the table (`adminTeachers.deactivate.body`: «{{name}} больше не сможет войти в систему, а ученики будут откреплены от учителя. …»), with no demonstrative or possessive pronoun. Buttons «Отмена» / «Подтвердить», as web-admin-teachers R4 requires until 6a.
- Issued link: label «Ссылка для входа: {{name}}»; muted note («в течение 7 дней», non-breaking space). Buttons «Скопировать» / «Закрыть» may sit on two lines at 360. Banners «Скопировано.» and the copy-by-hand text.
- Profile (default, saved, errors, password changed):
  - Primary: the title and each section's button («Сохранить», «Изменить пароль»).
  - Section headings at `--text-large`. Labels «Имя», «Часовой пояс», «Родной язык», «Текущий пароль», «Новый пароль», «Повторите новый пароль».
  - Success and danger banners wrap.
- App header (cabinet, profile, admin pages):
  - "teachdash", then «{name} ({role in lower case})», «Профиль» or «Назад в кабинет», «Выйти», then the language select last.
  - Wraps at 360 as it already does in English. #209 keeps the select's zero margin and the option order (English, Русский).
- Not found / cabinet stubs / startup:
  - Not found: «Страница не найдена» + text + link.
  - Cabinets: «Кабинет учителя» / «Кабинет ученика».
  - Startup: «Загрузка…» and the error with retry. Layout unchanged.

## Risks / Trade-offs

- [A Russian plural form missing in `ru` shows the `en` text silently] -> the strict parity test (D1) fails on it.
- [A `Select` no-margin prop that falls back to the default margin moves the header select by 1rem] -> the prop is opt-in with its own class; the header screenshots at 1280 and 360 px are compared with the current ones (D4).
- [Removing `.use(initReactI18next)` from `initI18n` leaves the app without react bindings if the app instance is not wired first] -> `src/i18n/index.ts` wires it before `initI18n`; the full test suite and the e2e run prove it.
- [Russian text is longer than English] -> estimated from the CSS: nothing overflows; two button pairs may wrap at 360 px (accepted, decision 15); the screenshots check it.
- [«Показать» is wider than «Скрыть», so the password input shifts by about 15 px when the toggle changes] -> known; fields belong to redesign step 3a (#197), out of scope.
- [The Russian copy is the main review item] -> the full table is in "Copy" for the spec gate (decision 17).

## Migration Plan

None. No backend or data changes. Users who stored `ru` see the full Russian UI after the deploy.
