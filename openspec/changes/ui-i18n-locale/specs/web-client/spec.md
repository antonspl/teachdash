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

## ADDED Requirements

### Requirement: R19 UI language detection and stored choice

The web client SHALL choose the UI language before the first render: the stored choice
(`teachdash.lang`) if supported, else the first supported browser language by primary
subtag, else `en`. An unsupported stored value SHALL be ignored, not overwritten. The app
SHALL start even when localStorage fails. `<html lang>` SHALL equal the UI language at
start and after every change.

#### Scenario: web-client/R19-S1 The stored choice wins over the browser languages
- **GIVEN** `teachdash.lang` holds `ru` and the browser languages are `en-US`
- **WHEN** the web client starts
- **THEN** the UI language is `ru`

#### Scenario: web-client/R19-S2 The first supported browser language is used, without its region
- **GIVEN** nothing is stored and the browser languages are `de-DE`, `RU-ru`, `en-US`
- **WHEN** the web client starts
- **THEN** the UI language is `ru`

#### Scenario: web-client/R19-S3 Unsupported browser languages give English
- **GIVEN** nothing is stored and the browser languages are `de-DE` and `fr`
- **WHEN** the web client starts
- **THEN** the UI language is `en`

#### Scenario: web-client/R19-S4 An unsupported stored value is ignored and kept
- **GIVEN** `teachdash.lang` holds `xx` and the browser languages are `ru-RU`
- **WHEN** the web client starts
- **THEN** the UI language is `ru`
- **AND** `teachdash.lang` still holds `xx`

#### Scenario: web-client/R19-S5 Unavailable storage does not stop the app
- **GIVEN** reading and writing localStorage throws
- **WHEN** the web client starts and the user then picks a language
- **THEN** the app starts in the detected language, with no blank page
- **AND** the picked language is used for the rest of the session

#### Scenario: web-client/R19-S6 The page language equals the UI language at start
- **GIVEN** the UI language is `en`, and in a second run it is `ru`
- **WHEN** the web client starts
- **THEN** `document.documentElement.lang` equals the UI language in each run

### Requirement: R20 UI language switch

The login, set-password and header pages (cabinet, Profile, admin) SHALL show a native
language select with a visually hidden label and the options "English" and "Русский", each
with its own `lang`; the not-found and loading pages SHALL NOT. Choosing a language SHALL
re-render at once, keep the focus on the select, store the choice and set `<html lang>`,
with no reload and no toast. The select SHALL fit a 360-pixel screen.

#### Scenario: web-client/R20-S1 The switch is on the pages that should have it
- **GIVEN** a logged-in teacher, and in other runs a logged-in admin, and an anonymous user
- **WHEN** the cabinet, Profile and admin pages, the login page and the set-password page are rendered, and then the not-found page
- **THEN** each of the first five shows the language select
- **AND** the not-found page shows none
- **AND** in the header it comes after "Log out", and on the login and set-password pages it comes before the page title and outside the form

#### Scenario: web-client/R20-S2 The select shows endonyms with their own language
- **GIVEN** the UI language is `en`
- **WHEN** the login page is rendered
- **THEN** the select has the accessible name "Language" and the options "English" and "Русский"
- **AND** the option "English" has `lang` `en` and the option "Русский" has `lang` `ru`
- **AND** the option texts are the same when the UI language is `ru`, where the accessible name is "Язык"

#### Scenario: web-client/R20-S3 Choosing Russian changes the page at once
- **GIVEN** the UI language is `en` and the login page is shown
- **WHEN** the user picks "Русский" in the select
- **THEN** the page text that has a Russian string is shown in Russian and the rest stays English, with no reload
- **AND** the select's accessible name is "Язык" and the focus stays on the select
- **AND** `teachdash.lang` holds `ru` and `document.documentElement.lang` is `ru`

#### Scenario: web-client/R20-S4 Choosing English changes the page back
- **GIVEN** the UI language is `ru`
- **WHEN** the user picks "English" in the select
- **THEN** the page text is English and the select's accessible name is "Language"
- **AND** `teachdash.lang` holds `en` and `document.documentElement.lang` is `en`

#### Scenario: web-client/R20-S5 The stored choice is used after a reload
- **GIVEN** the local stack is running and `teachdash.lang` holds `ru`
- **WHEN** a user opens the login page
- **THEN** the select shows "Русский"
- **AND** `document.documentElement.lang` is `ru`

#### Scenario: web-client/R20-S6 The switch fits a phone screen
- **GIVEN** the local stack is running and the browser has a 360-pixel-wide screen
- **WHEN** a user opens the login page, and a logged-in user opens a page with the header, in `en` and in `ru`
- **THEN** the language select is fully visible and the page has no horizontal scrolling
