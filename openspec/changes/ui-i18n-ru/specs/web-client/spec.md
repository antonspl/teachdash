# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Translated UI strings

Every string the web client shows to a user SHALL come from the translation resources.
The UI languages are English (`en`) and Russian (`ru`). The `en` and `ru` resources SHALL
each hold every string (R15), and a key missing in the active language SHALL still show
its `en` text. The `TARGET_ACCOUNT_DEACTIVATED` problem
code SHALL have its own translated message in both languages.

#### Scenario: web-client/R2-S1 Page text comes from the translation resources
- **GIVEN** the translation resources are replaced with test values
- **WHEN** the login page and the not-found page are rendered
- **THEN** each page shows the test values
- **AND** no English text from the real resources is shown

#### Scenario: web-client/R2-S2 A Russian string is shown when the UI language is Russian
- **GIVEN** the UI language is `ru` and the `ru` resources hold a value for a key
- **WHEN** a page that shows that key is rendered
- **THEN** the page shows the `ru` value, not the `en` one

#### Scenario: web-client/R2-S3 A key missing in Russian shows the English text
- **GIVEN** the UI language is `ru` and a test `ru` bundle lacks a key that `en` has
- **WHEN** a page that shows that key is rendered
- **THEN** the page shows the `en` text for it

#### Scenario: web-client/R2-S4 Deactivated target account has its own message
- **GIVEN** the backend answers with a problem response with code `TARGET_ACCOUNT_DEACTIVATED`
- **WHEN** the API client handles the response
- **THEN** the caller gets the message "This account has been deactivated, so a reset link cannot be created. Reload the list to see the current status."
- **AND** it is not the generic error message

#### Scenario: web-client/R2-S5 Deactivated target account has its Russian message
- **GIVEN** the UI language is `ru` and the backend answers with a problem response with code `TARGET_ACCOUNT_DEACTIVATED`
- **WHEN** the API client handles the response
- **THEN** the caller gets the message from `ru.json` key `errors.TARGET_ACCOUNT_DEACTIVATED`, which is not the `en` text and not the generic message

### Requirement: R20 UI language switch

The login, set-password and header pages (cabinet, Profile, admin) SHALL show a native
language select with a visually hidden label and the options "English" and "Русский", each
with its own `lang`; the not-found and loading pages SHALL NOT. Choosing a language SHALL
re-render at once, keep the focus on the select, store the choice and set `<html lang>`,
with no reload and no toast. An error already on screen SHALL NOT change language by
itself (R20-S7). The select SHALL fit a 360-pixel screen.

#### Scenario: web-client/R20-S1 The switch is on the pages that should have it
- **GIVEN** an anonymous user, a logged-in teacher, student and admin
- **WHEN** the routes `/login`, `/set-password`, `/admin`, `/teacher`, `/student` and `/profile` are rendered for the users that may open them
- **THEN** each page shows the language select

#### Scenario: web-client/R20-S2 The switch is absent where it should not be
- **GIVEN** the app is rendered
- **WHEN** the not-found route (`*`), the loading and guard states and the `HomeRedirect` state are shown
- **THEN** none of them shows the language select

#### Scenario: web-client/R20-S3 The switch sits at its place on the page
- **GIVEN** a logged-in admin on `/admin`, and an anonymous user on `/login` and on `/set-password`
- **WHEN** the pages are rendered
- **THEN** in the header the select comes after "Log out", as the last control
- **AND** on the login and set-password pages the select comes before the page title in the DOM and is the first tab stop, and it is outside the form

#### Scenario: web-client/R20-S4 The select shows endonyms with their own language
- **GIVEN** the UI language is `en`
- **WHEN** the login page is rendered
- **THEN** the select has the accessible name "Language" and the options "English" and "Русский"
- **AND** the option "English" has `lang` `en` and the option "Русский" has `lang` `ru`
- **AND** the option texts are the same when the UI language is `ru`, where the accessible name is "Язык"

#### Scenario: web-client/R20-S5 Choosing Russian changes the page at once
- **GIVEN** the UI language is `en`, the login page is shown, and the `ru` resources are a test bundle that holds `uiLanguage.*` and lacks `login.title`
- **WHEN** the user picks "Русский" in the select
- **THEN** the select's accessible name is "Язык" and the focus stays on the select
- **AND** the page title still reads the `en` value of `login.title`, because the test bundle has no such key (per-key fallback)
- **AND** `teachdash.lang` holds `ru` and `document.documentElement.lang` is `ru`

#### Scenario: web-client/R20-S6 Choosing English changes the page back
- **GIVEN** the UI language is `ru`
- **WHEN** the user picks "English" in the select
- **THEN** the select's accessible name is "Language"
- **AND** `teachdash.lang` holds `en` and `document.documentElement.lang` is `en`

#### Scenario: web-client/R20-S7 A message on screen keeps its language until the next action
- **GIVEN** the login page shows the error for a failed login in `en`
- **WHEN** the user picks "Русский" in the select
- **THEN** the error text is unchanged
- **AND** the next failed login shows the error in the language then active
- **AND** on a page that reloads its data when the language changes (the admin teacher list), an error on screen may instead be cleared by that reload; it is never shown in the new language without a new action

#### Scenario: web-client/R20-S8 The stored choice is used after a reload
- **GIVEN** the local stack is running and `teachdash.lang` holds `ru`
- **WHEN** a user opens the login page
- **THEN** the select shows "Русский"
- **AND** `document.documentElement.lang` is `ru`

#### Scenario: web-client/R20-S9 The switch fits a phone screen
- **GIVEN** the local stack is running, the browser has a 360-pixel-wide screen and the UI language is `ru`
- **WHEN** a user opens the login page, and a logged-in user opens a page with the header
- **THEN** the language select is fully visible, inside the viewport, and the page has no horizontal scrolling

## ADDED Requirements

### Requirement: R15 Russian strings

The `ru` resources SHALL hold a Russian value, with the same `{{variables}}`, for every
leaf of `en`, and no key `en` lacks except the plural suffixes of R16. With the UI language
Russian, every page, dialog and message state SHALL show Russian text that follows
`design-code.md` "Copy and languages" (R15-S5), wraps and is never truncated, and fits 360
pixels.

#### Scenario: web-client/R15-S1 Every English string has a Russian one
- **GIVEN** the bundled `en` and `ru` resources
- **WHEN** the key-parity check runs
- **THEN** every `en` leaf has a non-empty `ru` leaf and `ru` has no leaf that `en` lacks, except plural suffixes (R16)
- **AND** every leaf has the same set of `{{variables}}` in `en` and `ru`
- **AND** the check reports no problem

#### Scenario: web-client/R15-S2 The parity check names the keys that break it
- **GIVEN** synthetic `en` and `ru` bundles where one `ru` key is missing, one is an empty string, one has no `en` counterpart, one plain key drops a `{{variable}}` that `en` has, and one plain key renames it (`{{name}}` against `{{title}}`)
- **WHEN** the key-parity check runs
- **THEN** it fails
- **AND** the failure lists each of the five key names with its problem

#### Scenario: web-client/R15-S3 Russian pages show Russian text
- **GIVEN** the UI language is `ru`
- **WHEN** the login page, the set-password page (with a token, and with an incomplete link), the not-found page, the teacher cabinet, the student cabinet («Кабинет ученика»), the Profile page and the admin teacher list with the create dialog, the deactivate dialog and the issued-link box open are rendered
- **AND** the message states are rendered: the login banners `login.unreachable` and `login.sessionEnded`; `startup.error`; `header.logoutError`; the admin list load error (an `errors.*` message with `ui.tryAgain`); the status badges `ACTIVE`, `WAITING`, `DEACTIVATED` and «Только что добавлена»; one message of every `errors.*` and `fieldErrors.*` key; the Profile error banners `profile.validationFailed`, `profile.validationFailedTeacher`, `profile.wrongPassword` and `profile.passwordValidationFailed`, and the success states `profile.saved` and `profile.passwordChanged`; `issuedLink.copyByHand` and `issuedLink.copied`; `setPassword.incompleteLink`; `languages.loadFailed`, `languages.required` and the `languages.*` options; `adminTeachers.empty` and `adminTeachers.create.validationFailed`; `password.mismatch`, `password.tooShort` and `password.tooLong`; `ui.tryAgain`, `ui.loading`, `ui.show`, `ui.hide`, `ui.showPassword` and `ui.hidePassword`; `header.backToCabinet`
- **THEN** no text on them equals an `en` leaf value, except the values of an allow-list of three: `teachdash`, `English` and `Русский` (`app.name`, `uiLanguage.name.en`, and `languages.ru` together with `uiLanguage.name.ru`, which have the same text in `ru` and `en`); the text compared is the text content of every element and the `aria-label`, `title`, `placeholder` and `alt` attributes, trimmed, against every `en` leaf value minus that set
- **AND** the page titles are «Вход», «Задайте пароль», «Страница не найдена», «Кабинет учителя», «Ваш профиль» and «Кабинет администратора»

#### Scenario: web-client/R15-S4 Names and roles keep their case
- **GIVEN** the UI language is `ru`, a logged-in teacher named "Анна", and an admin teacher list with an active teacher with a password and one without
- **WHEN** the app header, the deactivate dialog and the issued-link box are shown for a teacher named "Анна"
- **THEN** the header reads «Анна (учитель)»
- **AND** the deactivate dialog title is «Деактивировать: Анна?» and the issued-link label is «Ссылка для входа: Анна»
- **AND** the row button for the teacher without a password is «Новое приглашение» and for the teacher with a password is «Ссылка для сброса пароля»

#### Scenario: web-client/R15-S5 The Russian copy follows the typographic rules
- **GIVEN** the bundled `ru` and `en` resources
- **WHEN** the copy-rules check runs
- **THEN** no `ru` value contains «пожалуйста», «преподавател», «учетн» or three dots in a row, or the whole word «еще», compared case-insensitively, and no `en` value contains three dots in a row
- **AND** every `ru` value contains a Cyrillic letter, except `app.name` and `uiLanguage.name.en`, which are on an explicit allow-list
- **AND** every `ru` value with a number followed by a unit («символов», «байт», «дней») has a non-breaking space (U+00A0, asserted by character code) between them, so «8 символов», «72 байт» and «7 дней» use it
- **AND** `app.name` has the same value in `en` and `ru`, and `uiLanguage.name.en` and `uiLanguage.name.ru` are the same in both
- **AND** every `adminTeachers.actions.*` `ru` value is at most 24 characters

#### Scenario: web-client/R15-S6 Russian pages fit a phone screen
- **GIVEN** the local stack is running with an admin, a teacher without a password and a teacher with a password, the browser has a 360-pixel-wide screen and the UI language is `ru`
- **WHEN** a user opens the login page, the set-password page, the not-found page, and a logged-in teacher and admin open the cabinet, Profile and admin pages
- **AND** on the admin page the create dialog, the deactivate dialog and the issued-link box are opened in turn, the two longest admin rows are shown (the just-added teacher: name, «Ожидает пароля», «Только что добавлена», «Новое приглашение», «Деактивировать»; and a teacher with a password: «Ссылка для сброса пароля», «Деактивировать»), and the banners are shown (login `login.unreachable` and `login.sessionEnded`, the Profile error banners, the admin load error)
- **THEN** on every page and dialog `document.documentElement.scrollWidth <= clientWidth` and no element is wider than the viewport
- **AND** the buttons of the create dialog and of the issued-link box may sit on two lines
- **AND** with the browser 768 pixels wide, the admin list with its two longest rows meets the same two checks (the current CSS has no `72rem` container condition, so the existing wrapping rule applies between 360 and 72rem)

### Requirement: R16 Russian plural forms

The UI SHALL support the plural forms of each language through i18next JSON v4 suffixes
(`_one`, `_few`, `_many`, `_other`). Each plural base key SHALL have exactly the categories
of `Intl.PluralRules(lang).resolvedOptions().pluralCategories` for its language, and the
variables of the `en` `_other` text in every language and category, except that `count` MAY
be absent in `_one`, `_few` and `_many`. Russian plurals are proved on synthetic bundles.

#### Scenario: web-client/R16-S1 The parity check accepts the Russian categories
- **GIVEN** synthetic bundles where `en` has `item_one` and `item_other`, and `ru` has `item_one`, `item_few`, `item_many` and `item_other`
- **WHEN** the key-parity check runs
- **THEN** it reports no problem

#### Scenario: web-client/R16-S2 The parity check names a missing or extra category
- **GIVEN** synthetic bundles where `ru` lacks `item_few`, and another base where `en` has an extra `thing_few`
- **WHEN** the key-parity check runs
- **THEN** it fails and the failure lists `item_few` and `thing_few` with the language and the expected categories

#### Scenario: web-client/R16-S3 The variables of a plural base match
- **GIVEN** synthetic bundles where `ru` `item_one` has no `{{count}}`, and where `ru` `item_other` uses `{{total}}` while `en` uses `{{count}}`, and where `ru` `item_few` has an extra `{{x}}`
- **WHEN** the key-parity check runs
- **THEN** the missing `count` in `item_one` is accepted
- **AND** the `{{total}}` against `{{count}}` mismatch fails and names the key
- **AND** the extra `{{x}}` in `ru` `item_few` fails and names the key

#### Scenario: web-client/R16-S4 A count picks the right Russian form
- **GIVEN** the UI language is `ru` on a synthetic bundle with all four categories for one key
- **WHEN** the key is translated with the counts 0, 1, 1.5, 2, 5, 11, 21 and 22, and the four texts are distinct
- **THEN** 1 and 21 give the `_one` text, 2 and 22 give the `_few` text, and 0, 5 and 11 give the `_many` text
- **AND** 1.5 gives the `_other` text
