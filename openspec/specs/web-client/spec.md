# web-client Specification

## Purpose

The browser application shell and how it talks to the backend: routing, translated UI
strings, the API client and same-origin serving. Product screens build on it.

## Requirements

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

### Requirement: R4 CSRF header

The API client SHALL send requests to the same-origin `/api` path. On a state-changing
request it SHALL send the value of the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header.

#### Scenario: web-client/R4-S1 State-changing request carries the CSRF token
- **GIVEN** the browser has an `XSRF-TOKEN` cookie with value `abc`
- **WHEN** the API client sends a POST to `auth/login`
- **THEN** the request goes to the same-origin path `/api/auth/login`
- **AND** the request has the header `X-XSRF-TOKEN: abc`

### Requirement: R5 Translated API errors

When a request fails, the API client SHALL give the caller a translated message for the
problem `code`; a code without a translation, or a response that is not a problem
response, gives a generic one. The `INVALID_CREDENTIALS` message SHALL say only that
the email and password do not match (`web-auth` R1 shows the link hint). The client
SHALL also give the caller the `{field, code}` entries of `errors` (`api-errors` R1-R2)
in order, without entries whose `field` or `code` is not a string.

#### Scenario: web-client/R5-S1 Known problem code gives its translated message
- **GIVEN** the backend answers with a problem response with code `WRONG_PASSWORD`
- **WHEN** the API client handles the response
- **THEN** the caller gets the translated message for `WRONG_PASSWORD`

#### Scenario: web-client/R5-S2 Unknown code or non-problem response gives the generic message
- **GIVEN** the response has a code without a translation, or is not a problem response
- **WHEN** the API client handles the response
- **THEN** the caller gets the generic translated error message

#### Scenario: web-client/R5-S3 Real backend error is recognised as a problem response
- **GIVEN** the local stack is running
- **WHEN** a client sends a login with an empty body and the headers the API client uses
- **THEN** the response has content type `application/problem+json` and code `VALIDATION_FAILED`

#### Scenario: web-client/R5-S4 Failed login message does not repeat the link hint
- **GIVEN** the backend answers with a problem response with code `INVALID_CREDENTIALS`
- **WHEN** the API client handles the response
- **THEN** the caller gets a message saying the email and password do not match
- **AND** the message says nothing about a link or setting a password

#### Scenario: web-client/R5-S5 Field errors reach the caller
- **GIVEN** the backend answers with status 400, code `VALIDATION_FAILED` and `errors` `[{field: "email", code: "INVALID_FORMAT"}, {field: "name", code: "TOO_LONG"}]`
- **WHEN** the API client handles the response
- **THEN** the caller gets the translated message for `VALIDATION_FAILED` and the field errors `email` `INVALID_FORMAT` and `name` `TOO_LONG`, in that order

#### Scenario: web-client/R5-S6 Broken field errors are left out
- **GIVEN** the backend answers with code `VALIDATION_FAILED` and, one response at a time, no `errors`, `errors` that is the string "x", `errors` `[]`, and `errors` `[{field: 1, code: "REQUIRED"}, {field: "name"}, {field: "email", code: "REQUIRED"}]`
- **WHEN** the API client handles each response
- **THEN** the caller gets the translated message for `VALIDATION_FAILED` each time, no field errors for the first three, and only `email` `REQUIRED` for the last

### Requirement: R6 Same-origin serving

The web container SHALL proxy `/api/*` to the backend on the same origin as the web
application, with no CORS, and SHALL NOT expose other backend paths.

#### Scenario: web-client/R6-S1 API request reaches the backend on the same origin
- **GIVEN** the local stack is running
- **WHEN** a client sends `GET /api/me` with an `Origin` header to the web container, anonymously
- **THEN** the response is the backend's 401 problem with code `UNAUTHENTICATED`
- **AND** the response has no `Access-Control-Allow-Origin` header

#### Scenario: web-client/R6-S2 Actuator is not exposed through the web container
- **GIVEN** the local stack is running
- **WHEN** a client sends `GET /actuator/health` to the web container
- **THEN** the response does not contain the backend's health data

### Requirement: R7 Mobile browsers

Pages SHALL fit the screen in mobile browsers, down to a width of 360 CSS pixels, without
horizontal scrolling and without the browser shrinking the page to fit.

#### Scenario: web-client/R7-S1 Pages fit a phone screen
- **GIVEN** the local stack is running and the browser has a phone-sized screen
- **WHEN** a user opens the login page, the set-password page, a cabinet page and the not-found page
- **THEN** each page has no horizontal scrolling
- **AND** the page is laid out at the phone's own width, not zoomed out

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

### Requirement: R9 Caching and compression

Hashed assets SHALL be cached for a long time and served compressed. The HTML page SHALL be
revalidated on every load, so a new deploy reaches users.

#### Scenario: web-client/R9-S1 Hashed asset is long-lived and compressed
- **GIVEN** the local stack is running
- **WHEN** a client that accepts compression requests a script file under `/assets/`
- **THEN** the response has `Cache-Control` with `immutable` and a one-year `max-age`
- **AND** the response is compressed

#### Scenario: web-client/R9-S2 HTML page is revalidated
- **GIVEN** the local stack is running
- **WHEN** a client requests `/`
- **THEN** the response has `Cache-Control: no-cache`

### Requirement: R10 Container health endpoint

The web container SHALL answer `/healthz` with a 200 without depending on the backend, so
orchestration can check the container itself.

#### Scenario: web-client/R10-S1 Health endpoint answers without the application
- **GIVEN** the local stack is running
- **WHEN** a client requests `/healthz`
- **THEN** the response status is 200
- **AND** the response is not the web application's HTML

### Requirement: R11 Application pages

The web client SHALL redirect the root path by login state and role (see `web-auth`
R2) and SHALL show a not-found page for any path it does not know, including when that
path is opened directly. A missing file under `/assets/` SHALL be a real 404, so a stale
page fails visibly instead of loading HTML as a script. `/robots.txt` and `/favicon.ico`
SHALL also be real 404s.

#### Scenario: web-client/R11-S1 Root path opens the login page for an anonymous user
- **GIVEN** the local stack is running and the user is not logged in
- **WHEN** a user opens `/`
- **THEN** the login page is shown with the application name

#### Scenario: web-client/R11-S2 Unknown path opened directly shows the not-found page
- **GIVEN** the local stack is running
- **WHEN** a user opens `/no-such-page` directly
- **THEN** the response is the web application, not a server error page
- **AND** the not-found page is shown

#### Scenario: web-client/R11-S3 Missing asset is a real 404
- **GIVEN** the local stack is running
- **WHEN** a client requests `/assets/no-such-file.js`, `/robots.txt` or `/favicon.ico`
- **THEN** the response status is 404
- **AND** the response is not the web application's HTML

### Requirement: R12 Backend reachability warning

The login page SHALL warn when the backend cannot be reached and SHALL show no warning
when it can. A successful JSON response or a complete problem response means reachable.
A network failure, a dropped or hung response or any other response means unreachable.
The page SHALL check once on load and again when the tab becomes visible or the browser
comes back online, and SHALL NOT poll on a timer. The checks SHALL NOT send the session
cookie, so they never extend a session.

#### Scenario: web-client/R12-S1 Reachable backend shows no warning
- **GIVEN** the local stack is running and the user is not logged in
- **WHEN** a user opens `/login`
- **THEN** the page shows no "unreachable" warning

#### Scenario: web-client/R12-S2 Backend cannot be reached
- **GIVEN** the status request fails with a network error, or returns a 502 that is not a problem response, or returns a 200 with HTML, or returns a 200 with JSON whose body drops or hangs
- **WHEN** the login page is rendered
- **THEN** the page shows the translated "unreachable" warning

#### Scenario: web-client/R12-S3 Status check does not send the session cookie
- **GIVEN** the local stack is running and the browser holds a `SESSION` cookie
- **WHEN** the login page sends a status request
- **THEN** the request has no `Cookie` header with that session

#### Scenario: web-client/R12-S4 Page checks again when the tab is shown, and not on a timer
- **GIVEN** the login page shows the "unreachable" warning and the tab is hidden
- **WHEN** a long time passes, and then the tab becomes visible and the backend now answers
- **THEN** no status request is sent before the tab is visible
- **AND** the warning disappears without a reload

### Requirement: R13 Select arrow

Every select field of the web client SHALL show the app's own dropdown arrow in place of
the browser's. The gap between the arrow and the inner edge of the field's right border
SHALL equal the gap between the text and the inner edge of the left border, and the
selected text SHALL be cut off before the arrow when it does not fit. A click or tap on
the arrow SHALL reach the field, as a click on its text does.

#### Scenario: web-client/R13-S1 Arrow is inset like the text
- **GIVEN** a logged-in admin, on `/profile` and then in the "Add teacher" dialog
- **WHEN** the time zone select is shown
- **THEN** each time the browser's own arrow is switched off and the app's arrow is visible inside the field
- **AND** the distance from the arrow's right edge to the inner edge of the right border equals the select's left padding, within 1 pixel

#### Scenario: web-client/R13-S2 Long option does not run under the arrow
- **GIVEN** a logged-in teacher with time zone "America/Argentina/ComodRivadavia" and a 360-pixel-wide screen
- **WHEN** `/profile` is opened
- **THEN** the select is as wide as the name field, its text area ends before the arrow's left edge, the whole time zone name fits in the text area, and text that does not fit would be cut off with an ellipsis
- **AND** the page has no horizontal scroll

#### Scenario: web-client/R13-S3 Click on the arrow reaches the select
- **GIVEN** a logged-in teacher on `/profile`
- **WHEN** the teacher clicks in the middle of the arrow
- **THEN** the click lands on the time zone select, not on the arrow, and the select has the focus

#### Scenario: web-client/R13-S4 Disabled select keeps the arrow
- **GIVEN** a logged-in teacher on `/profile`, and the arrow's position is noted
- **WHEN** a profile save is still running, so the time zone select is disabled
- **THEN** the arrow is visible at the same position

### Requirement: R14 New-password hints while typing

Every form that asks for a new password twice (the profile password form and the
set-password page) SHALL show a translated hint under the field while the user types:
"at least 8 characters" or "password is too long" under the new password, "passwords do
not match" under the repeat. A hint SHALL go away once its rule is met; an empty field
SHALL show no hint. The button SHALL stay enabled; a submit with a broken rule sends
nothing and focuses the first field with a hint.

#### Scenario: web-client/R14-S1 Short new password
- **GIVEN** the profile password form, and in a second run the set-password page, with empty fields
- **WHEN** the user types "abc" in the new password field
- **THEN** in both runs the translated "at least 8 characters" hint is shown under that field, without the button being pressed
- **AND** after the user types on to "abcdefgh" the hint is gone

#### Scenario: web-client/R14-S2 New password over 72 bytes
- **GIVEN** the profile password form, and in a second run the set-password page
- **WHEN** the user enters 40 Cyrillic letters (80 bytes) in the new password field
- **THEN** in both runs the translated "password is too long" hint is shown under that field
- **AND** after the user shortens it to 36 Cyrillic letters (72 bytes) the hint is gone

#### Scenario: web-client/R14-S3 Repeat that differs
- **GIVEN** the profile password form, and in a second run the set-password page, with "teacher-pass-2" in the new password field
- **WHEN** the user types "teacher" in the repeat field
- **THEN** in both runs the translated "passwords do not match" hint is shown under the repeat field
- **AND** after the user types on to "teacher-pass-2" the hint is gone

#### Scenario: web-client/R14-S4 New password edited after the repeat matched
- **GIVEN** the profile password form, and in a second run the set-password page, with "teacher-pass-2" in both the new password and the repeat field, and no hint shown
- **WHEN** the user adds "3" to the new password field
- **THEN** in both runs the "passwords do not match" hint is shown under the repeat field

#### Scenario: web-client/R14-S5 Empty fields show no hint
- **GIVEN** the profile password form, and in a second run the set-password page, just opened
- **WHEN** the user types "teacher-pass-2" in the new password field and leaves the repeat field empty, and then clears the new password field
- **THEN** in both runs no hint is shown at any moment

#### Scenario: web-client/R14-S6 Two hints at once
- **GIVEN** the profile password form, and in a second run the set-password page
- **WHEN** the user types "abc" in the new password field and "abd" in the repeat field
- **THEN** in both runs "at least 8 characters" is shown under the new password and "passwords do not match" under the repeat

#### Scenario: web-client/R14-S7 Submitting with a hint sends nothing
- **GIVEN** the profile password form with a current password entered, and in a second run the set-password page, each with "teacher-pass-2" as the new password, "teacher-pass-3" as the repeat and the "passwords do not match" hint shown
- **WHEN** the user looks at the submit button and then presses it
- **THEN** in both runs the button is enabled, no request is sent and the hint stays

#### Scenario: web-client/R14-S8 Hint belongs to its field
- **GIVEN** the profile password form, and in a second run the set-password page, with the "at least 8 characters" hint shown
- **WHEN** assistive technology reads the new password field
- **THEN** in both runs the field is marked invalid and the hint is its description, and the repeat field is not marked invalid

#### Scenario: web-client/R14-S9 Current password is not checked while typing
- **GIVEN** the profile password form
- **WHEN** the user types "x" in the current password field
- **THEN** no hint is shown under any field and the backend has received no request

#### Scenario: web-client/R14-S10 No hints after a successful change
- **GIVEN** the profile password form with a current password and a valid new password entered twice, and the backend answers the password change with status 204
- **WHEN** the form is submitted
- **THEN** the three fields are empty and no hint is shown

#### Scenario: web-client/R14-S11 A hint replaces an old backend error
- **GIVEN** the set-password page shows the translated message for `LINK_CONFLICT` after a submit
- **WHEN** the user changes the repeat field so that it differs and presses the button
- **THEN** the "passwords do not match" hint is shown, the `LINK_CONFLICT` message is gone and no second request is sent

#### Scenario: web-client/R14-S12 Submitting with a hint moves the focus to it
- **GIVEN** the profile password form with a current password entered, and in a second run the set-password page, each with "abc" as the new password and "abd" as the repeat, and the focus on the submit button
- **WHEN** the user presses the button
- **THEN** in both runs no request is sent and the focus is on the new password field, the first field that shows a hint, so assistive technology reads its hint
- **AND** after the user fixes the new password to "teacher-pass-2" and presses the button again, the focus is on the repeat field, the only field that still shows a hint

### Requirement: R17 Backend errors under their fields

The login, set-password, "Add teacher", profile and password forms SHALL show a failed
request's field errors (R5) under their fields when every entry names a shown field and
a code with a field message: the first entry per field counts, each field is marked
invalid with its message as the description, the focus moves to the topmost such field,
and no form-level message is shown. Otherwise the form SHALL show its form-level
message as before; other specs' form-level messages describe this case.

#### Scenario: web-client/R17-S1 Errors are shown under their fields
- **GIVEN** the backend answers the "Add teacher" request with status 400, code `VALIDATION_FAILED` and `errors` `[{field: "email", code: "INVALID_FORMAT"}, {field: "name", code: "TOO_LONG"}, {field: "name", code: "INVALID_FORMAT"}]`
- **WHEN** the admin submits the dialog with a name, an email and a time zone
- **THEN** the translated "too long" message, and not the "check the format" one, is shown under the name, and the translated "check the format" message under the email; both fields are marked invalid with their message as the description, and the time zone is not marked invalid
- **AND** no form-level message is shown, the dialog stays open and the entered values stay

#### Scenario: web-client/R17-S2 Every form places its own fields
- **GIVEN** one run per form, the backend answering the request with status 400, code `VALIDATION_FAILED` and the `errors` given: the login page with `email` `TOO_LONG`, and in another run `password` `REQUIRED`; the set-password page with `password` `TOO_SHORT`; the profile form with `timeZone` `INVALID_FORMAT`; the password form with `currentPassword` `REQUIRED`, and in another run `newPassword` `TOO_LONG`
- **WHEN** the form is submitted with values that pass its own checks
- **THEN** in every run the translated message for the code is shown under the named field (the email, the password, the new password, the time zone select, the current password, the new password), that field is marked invalid, and no form-level message is shown
- **AND** in the login password run the password field is empty, as after every failed login

#### Scenario: web-client/R17-S3 Focus moves to the first field with an error
- **GIVEN** the backend answers the "Add teacher" request with `errors` `[{field: "email", code: "INVALID_FORMAT"}, {field: "name", code: "REQUIRED"}]`, and the focus is on the submit button
- **WHEN** the admin presses it
- **THEN** the focus is on the name field, the topmost field of the dialog with an error, although the backend lists the email first

#### Scenario: web-client/R17-S4 Errors the form cannot place keep the form-level message
- **GIVEN** one run per case: the set-password page and the backend answering with `errors` `[{field: "password", code: "TOO_SHORT"}, {field: "token", code: "TOO_LONG"}]`; the profile form and the backend answering with `errors` `[{field: "name", code: "SOMETHING_NEW"}]`; the "Add teacher" dialog and the backend answering with code `VALIDATION_FAILED` and no `errors`
- **WHEN** the form is submitted
- **THEN** in every run the form shows the form-level message it shows today for that response, and no field shows a backend error

#### Scenario: web-client/R17-S5 Taken email against the real backend
- **GIVEN** the local stack is running, a logged-in admin, and a teacher with the email "taken@example.com"
- **WHEN** the admin adds a teacher named "Анна" with the email "taken@example.com"
- **THEN** the translated `EMAIL_TAKEN` message is shown under the email field, the focus is on it, no form-level message is shown and the dialog stays open

#### Scenario: web-client/R17-S6 Wrong current password against the real backend
- **GIVEN** the local stack is running and a logged-in teacher with the password "teacher-pass-1" on `/profile`
- **WHEN** the teacher enters "wrong-pass-1" as the current password and "teacher-pass-2" twice as the new one, and presses "Change password"
- **THEN** the password form's `WRONG_PASSWORD` message is shown under the current password field, the focus is on it, no form-level message is shown, and the new password fields keep their values

#### Scenario: web-client/R17-S7 Too-long name against the real backend
- **GIVEN** the local stack is running and a logged-in teacher on `/profile`
- **WHEN** the teacher enters a name of 201 Cyrillic letters ("Я") and presses "Save"
- **THEN** the translated "too long" message is shown under the name field, no form-level message is shown, the field keeps the typed name and the header keeps the old name

#### Scenario: web-client/R17-S8 Focus in the password form
- **GIVEN** the backend answers the password change with status 400, code `VALIDATION_FAILED` and `errors` `[{field: "newPassword", code: "TOO_LONG"}, {field: "currentPassword", code: "REQUIRED"}]`
- **WHEN** the form is submitted with a current password and a valid new password entered twice
- **THEN** both messages are shown under their fields and the focus is on the current password field, the topmost one

### Requirement: R18 Backend field errors go away on edit

In the forms of R17, editing a field SHALL remove that field's backend error and keep
the others, and a new submit SHALL remove them all before the request is sent. On the
set-password page, editing a field SHALL also remove the form-level message.

#### Scenario: web-client/R18-S1 Editing a field removes only its error
- **GIVEN** the "Add teacher" dialog shows backend errors under the name and the email, and the backend holds its answer to the next request
- **WHEN** the admin changes the value of the email field
- **THEN** the email's error is gone and the email is no longer marked invalid, while the name still shows its error
- **AND** when the admin submits again, the name's error is gone while the request is still waiting for its answer

#### Scenario: web-client/R18-S2 Editing on the set-password page removes the form-level message
- **GIVEN** the set-password page shows the translated message for `LINK_CONFLICT` after a submit
- **WHEN** the user changes the value of the new password field, and in a second run of the repeat field
- **THEN** in both runs the `LINK_CONFLICT` message is gone and the fields keep the typed values

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
