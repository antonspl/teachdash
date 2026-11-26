# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Translated UI strings

Every string the web client shows to a user SHALL come from the translation resources.
The UI languages are English (`en`) and Russian (`ru`). The `en` resources SHALL hold
every string; the `ru` resources hold the strings translated so far, and a key missing
in the active language SHALL show its `en` text. The `TARGET_ACCOUNT_DEACTIVATED` problem
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
- **GIVEN** the UI language is `ru` and the `ru` resources lack a key that `en` has
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

### Requirement: R8 Security headers

Web application responses SHALL carry a Content Security Policy, `X-Content-Type-Options`
and `Referrer-Policy` headers, and SHALL NOT allow other sites to frame the application.
The application SHALL work under that policy.

#### Scenario: web-client/R8-S1 Page is served with security headers and works under them
- **GIVEN** the local stack is running
- **WHEN** a user opens `/`
- **THEN** the response has a Content Security Policy with `default-src 'self'` and `frame-ancestors 'none'`
- **AND** the response has `X-Content-Type-Options: nosniff` and a `Referrer-Policy` header
- **AND** the login page it redirects to renders with no Content Security Policy violations, except that in WebKit, when the page has a native `<select>`, the two console messages WebKit logs for its own internal inline style ("Refused to apply a stylesheet because its hash, its nonce, or 'unsafe-inline' appears in neither the style-src directive nor the default-src directive of the Content Security Policy." and "CSP violation: style-src-attr inline") are ignored; any other console error or violation still fails the scenario

## ADDED Requirements

### Requirement: R19 UI language detection and stored choice

The web client SHALL choose the UI language before the first render: the stored choice
(`teachdash.lang`) if supported, else the first supported browser language by primary
subtag, else `en`. An unsupported stored value SHALL be ignored, not overwritten. The app
SHALL start even when localStorage fails. `<html lang>` SHALL equal the UI language at
start and after every change.

#### Scenario: web-client/R19-S1 The stored choice wins over the browser languages
- **GIVEN** `teachdash.lang` holds `ru` and the browser languages are `en-US`
- **WHEN** `detectLanguage` runs with these values
- **THEN** the UI language is `ru`

#### Scenario: web-client/R19-S2 The first supported browser language is used, without its region
- **GIVEN** nothing is stored and the browser languages are `de-DE`, `RU-ru`, `en-US`
- **WHEN** `detectLanguage` runs with these values
- **THEN** the UI language is `ru`

#### Scenario: web-client/R19-S3 Unsupported browser languages give English
- **GIVEN** nothing is stored and the browser languages are `de-DE` and `fr`
- **WHEN** `detectLanguage` runs with these values
- **THEN** the UI language is `en`

#### Scenario: web-client/R19-S4 An unsupported stored value is ignored and kept
- **GIVEN** a fake storage passed to `initI18n` holds `xx` under `teachdash.lang` and the browser languages are `ru-RU`
- **WHEN** `initI18n` starts a fresh i18next instance
- **THEN** the UI language is `ru`
- **AND** the fake storage still holds `xx` and `setItem` was not called

#### Scenario: web-client/R19-S5 Unavailable storage does not stop the app
- **GIVEN** the storage passed to `initI18n` throws on the `localStorage` property access, in a second run on `getItem` and `setItem`
- **WHEN** `initI18n` starts a fresh instance and `setUiLanguage('ru')` is then called
- **THEN** `initI18n` returns without throwing and the instance has the detected language
- **AND** the instance language is `ru` after the call, for the rest of the session

#### Scenario: web-client/R19-S6 The page language equals the UI language at start
- **GIVEN** a fake document element is passed to `initI18n`, and the detected language is `en` in one run and `ru` in another
- **WHEN** `initI18n` has finished
- **THEN** the fake element's `lang` equals the UI language in each run

#### Scenario: web-client/R19-S7 Missing browser languages and a stored value in another case
- **GIVEN** `navigator.languages` is empty or undefined and `navigator.language` is `ru-RU`, in one run; and in another run `teachdash.lang` holds `RU`
- **WHEN** `detectLanguage` runs with these values
- **THEN** the UI language is `ru` in the first run, and in the second the stored `RU` counts as `ru`
- **AND** with neither `navigator.languages` nor `navigator.language`, and nothing stored, it is `en`

### Requirement: R20 UI language switch

The login, set-password and header pages (cabinet, Profile, admin) SHALL show a native
language select with a visually hidden label and the options "English" and "Русский", each
with its own `lang`; the not-found and loading pages SHALL NOT. Choosing a language SHALL
re-render at once, keep the focus on the select, store the choice and set `<html lang>`,
with no reload and no toast; an error already on screen keeps its language until the next
action. The select SHALL fit a 360-pixel screen.

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
- **GIVEN** the UI language is `en` and the login page is shown
- **WHEN** the user picks "Русский" in the select
- **THEN** the select's accessible name is "Язык" and the focus stays on the select
- **AND** the page title still reads the `en` value of `login.title`, because `ru.json` has no such key (per-key fallback)
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

#### Scenario: web-client/R20-S8 The stored choice is used after a reload
- **GIVEN** the local stack is running and `teachdash.lang` holds `ru`
- **WHEN** a user opens the login page
- **THEN** the select shows "Русский"
- **AND** `document.documentElement.lang` is `ru`

#### Scenario: web-client/R20-S9 The switch fits a phone screen
- **GIVEN** the local stack is running, the browser has a 360-pixel-wide screen and the UI language is `ru`
- **WHEN** a user opens the login page, and a logged-in user opens a page with the header
- **THEN** the language select is fully visible, inside the viewport, and the page has no horizontal scrolling
