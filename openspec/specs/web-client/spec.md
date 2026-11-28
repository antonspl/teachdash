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
selected text SHALL be cut off before the arrow when it does not fit. A select that
leads with an icon (the UI language select, with the globe) SHALL instead put the arrow
and the icon at the same gap, `--space-group`, from the inner edges of the right and
left borders, and its text SHALL start after the icon, so the selected language name
fits with room to spare. A click or tap on
the arrow SHALL reach the field, as a click on its text does.

#### Scenario: web-client/R13-S1 Arrow is inset like the text
- **GIVEN** a logged-in admin, on `/profile` and then in the "Add teacher" dialog
- **WHEN** the time zone select is shown
- **THEN** each time the browser's own arrow is switched off and the app's arrow is visible inside the field
- **AND** the distance from the arrow's right edge to the inner edge of the right border equals the select's left padding, within 1 pixel

#### Scenario: web-client/R13-S2 Long option does not run under the arrow
- **GIVEN** a logged-in teacher with time zone "America/Argentina/ComodRivadavia" and a 360-pixel-wide screen
- **WHEN** `/profile` is opened
- **THEN** the select is as wide as the name field, its text area ends before the arrow's left edge, and text that does not fit is cut off with an ellipsis
- **AND** the page has no horizontal scroll

#### Scenario: web-client/R13-S3 Click on the arrow reaches the select
- **GIVEN** a logged-in teacher on `/profile`
- **WHEN** the teacher clicks in the middle of the arrow
- **THEN** the click lands on the time zone select, not on the arrow, and the select has the focus

#### Scenario: web-client/R13-S4 Disabled select keeps the arrow
- **GIVEN** a logged-in teacher on `/profile`, and the arrow's position is noted
- **WHEN** a profile save is still running, so the time zone select is disabled
- **THEN** the arrow is visible at the same position

#### Scenario: web-client/R13-S5 The icon-led select has equal gaps for the icon and the arrow
- **GIVEN** the UI language select on the login page (in `<main>`, above the title) and in the header of a logged-in teacher, in `en` and in `ru`, on the desktop and the 360-pixel phone projects
- **WHEN** the select is shown
- **THEN** it has a globe icon that is `aria-hidden="true"`, is not inside the `[data-select-arrow]` element, and is muted like the arrow; the `[data-select-arrow]` element still holds only the chevron
- **AND** the distance from the icon's left edge to the inner edge of the left border equals the distance from the arrow's right edge to the inner edge of the right border, within 1 pixel, and both are `--space-group`
- **AND** the select's left padding equals `--space-group` plus `--icon-size` plus `--space-inline`, and the selected language name is not cut off (its text width, measured with `canvas.measureText` using the select's computed font, is at most the width of the text area)
- **AND** with the select disabled, and in a run where it has an error (the test passes one), the icon is dimmed with the select, and the icon and the text start do not move by more than 1 pixel

#### Scenario: web-client/R13-S6 Only a select with an icon gets the icon rules
- **GIVEN** `Select.tsx` and `Field.module.css`, read as text, and rendered selects with and without the `icon` prop
- **WHEN** the select, icon and arrow rules are read, and the markup is read
- **THEN** a select given an icon renders the icon with its own data attribute, `aria-hidden="true"`, outside `[data-select-arrow]`, at `left: calc(var(--border-width) + var(--space-group))`, and a select class with `padding-inline-start: calc(var(--space-group) + var(--icon-size) + var(--space-inline))`
- **AND** the arrow stays the element right after the select (`.select:disabled + .arrow` still matches) and a disabled icon-led select dims the icon too, and an invalid one has a rule that keeps the icon-led start padding minus one `--border-width`
- **AND** a select without the prop renders no icon and gets none of those rules, and the arrow and select rules of web-client R28-S2 are unchanged

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

### Requirement: R21 Shared icon set

The web client SHALL have one shared set of inline SVG icons: one component for each icon
of the design-code icon table and none beyond it. Each icon SHALL carry the shared
attributes on its root `svg`, SHALL have the table's shapes (scenarios), SHALL be hidden
from assistive technology, and has no title and no accessible name of its own. A change to
the design-code icon table needs a spec change.

#### Scenario: web-client/R21-S1 Every icon of the table exists and none is extra
- **GIVEN** the icon table of the design code
- **WHEN** the icon components are listed
- **THEN** there is one component for each icon of the table, named in PascalCase from the table name with the suffix `Icon`
- **AND** there is no other icon component

#### Scenario: web-client/R21-S2 Every icon has the shared attributes on its root svg
- **GIVEN** each icon is rendered
- **WHEN** its root element is read
- **THEN** it is an `svg` with `viewBox` `0 0 20 20`, `fill="none"`, `stroke="currentColor"`, `stroke-width` 1.75, round line caps and joins, `aria-hidden="true"` and `focusable="false"`
- **AND** it has no `title` element and no `aria-label`
- **AND** these shared attributes are not repeated on its shapes

#### Scenario: web-client/R21-S3 Every icon's shapes equal the table
- **GIVEN** each icon is rendered and the test holds one literal list per icon, copied by hand from the design-code table in the table's element order, each element as a tag and its attributes (`d` for paths; `cx`, `cy`, `r` for circles; `x`, `y`, `width`, `height`, `rx` for rects)
- **WHEN** the icon's elements are read
- **THEN** they equal its list in number, order, tags and attributes, with `d` compared as an exact string
- **AND** the elements carry no attributes beyond those listed, except the `fill` and `stroke` overrides of `play` and `more` (`fill="currentColor"`, `stroke="none"`) and the `stroke-dasharray="2.5 2.5"` of the `todo` circle

### Requirement: R22 Icon size tokens

The web client SHALL define the icon sizes as the tokens `--icon-size` (1.25rem),
`--icon-size-small` (1rem) and `--icon-size-rail` (1.5rem) in `tokens.css`. An icon
component SHALL take a size of `default`, `small` or `rail`, `default` when none is given,
and its width and height SHALL come only from the token of that size, never from the
font size.

#### Scenario: web-client/R22-S1 The three tokens have the specified values
- **GIVEN** `frontend/src/styles/tokens.css`, read as text
- **WHEN** the icon size tokens are read
- **THEN** `--icon-size` is `1.25rem`, `--icon-size-small` is `1rem` and `--icon-size-rail` is `1.5rem`

#### Scenario: web-client/R22-S2 The size prop selects the size class
- **GIVEN** an icon is rendered with no size, with `default`, with `small` and with `rail`, and the icon CSS module is read as text
- **WHEN** its root element's class is read
- **THEN** no size and `default` give the same class, and `small` and `rail` each give a different one
- **AND** each size class (`default`, `small`, `rail`) sets its width and height from its own token (`--icon-size`, `--icon-size-small`, `--icon-size-rail`) and contains no `em`

#### Scenario: web-client/R22-S3 The plus on Add teacher is 20 pixels
- **GIVEN** a logged-in admin on `/admin`, on the desktop and phone projects
- **WHEN** the plus on "Add teacher" is measured by its computed style
- **THEN** its width and its height are 20 pixels
- **AND** on the phone project at 360 pixels `/admin` has no horizontal scroll

### Requirement: R23 Icons on buttons

The plus of "Add teacher" SHALL use the set's plus (`M10 4v12M4 10h12`), SHALL be the first
child of the button before the label, and SHALL be `aria-hidden="true"` and
`focusable="false"`. Icons SHALL NOT appear on primary, form or dialog buttons, except a
create action's plus; "Create teacher" (the dialog's submit) SHALL have no icon.

#### Scenario: web-client/R23-S1 The plus of Add teacher uses the set's plus
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button is read by role and name
- **THEN** its first child is an `svg` that is `aria-hidden="true"` and `focusable="false"`, followed by the label
- **AND** its path is `M10 4v12M4 10h12`

#### Scenario: web-client/R23-S2 Create teacher and the other dialog buttons have no icon
- **GIVEN** a logged-in admin with the "Add teacher" dialog open
- **WHEN** the "Create teacher" button and the other buttons of the dialog are read by role and name
- **THEN** none of them contains an `svg`

### Requirement: R24 Design tokens

`frontend/src/styles/tokens.css` SHALL declare exactly the design-code token tables, with
their names and values, and no other custom property. Titles and headings SHALL use the
type tokens and `--weight-strong`, not raw sizes or weights, and the Page padding SHALL
follow the space roles (scenarios).

#### Scenario: web-client/R24-S1 tokens.css declares exactly the token names
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of 90 design-code token names copied by hand from the "Tokens" tables (83 table rows, with `--button-icon-inset` in "Controls and icons"; a row such as `--color-neutral-background` / `-text`, `--color-danger-action` / `-hover`, `--weight-regular` / `--weight-strong`, `--border-width` / `--border-width-strong` or `--side-panel-min` / `--side-panel-max` gives two names)
- **WHEN** its custom property definitions are listed
- **THEN** every name of the list is defined
- **AND** no other name is defined

#### Scenario: web-client/R24-S2 tokens.css has the design-code values
- **GIVEN** `frontend/src/styles/tokens.css`, read as text, and a literal list of the design-code values copied by hand, including `--control-height` and `--row-action-size` under `@media (pointer: fine)` and `--type-label-width` under `:root:lang(ru)`
- **WHEN** each token's value is read
- **THEN** it equals the list's value, with `--radius` `0.25rem`, `--text-large` `1.25rem`, `--text-title` `1.5rem`, `--line-height` `1.45`, `--line-height-tight` `1.2`, `--control-height` `2.75rem` (`2.5rem` under `@media (pointer: fine)`), `--button-icon-inset` `0.1875rem` and `--lang-select-width` `10.5rem`

#### Scenario: web-client/R24-S3 Headings use the type tokens
- **GIVEN** the CSS of Title, Section and Dialog, read as text
- **WHEN** the heading rules are read
- **THEN** the Title `font-size` is `--text-title` with no `@media (min-width: 40rem)` size step, its line height is `--line-height-tight` and its weight is `--weight-strong`
- **AND** the Section heading and the Dialog title use `--text-large`, `--line-height-large` and `--weight-strong`
- **AND** no CSS file under `src/` contains a raw `font-weight` number

#### Scenario: web-client/R24-S4 Page padding follows the space roles
- **GIVEN** the CSS of Page, read as text
- **WHEN** its padding rules are read
- **THEN** the padding is `var(--space-block) var(--space-group)`
- **AND** from `@media (min-width: 40rem)` it is `var(--space-page)`

### Requirement: R25 No removed or undefined token

No file under `frontend/src` SHALL name a removed token: `--color-border`,
`--color-action-success-*`, `--shadow-card`, `--space-1` to `--space-4` or `--arrow-width`.
Every `var(--name)` in `src/**/*.css` and in the TSX files SHALL name a token that
`tokens.css` defines. `npm run lint` SHALL fail on a `var(--name)` that `tokens.css` does
not define.

#### Scenario: web-client/R25-S1 No removed token name is left
- **GIVEN** every `.css`, `.ts` and `.tsx` file under `frontend/src` except the test files (`*.test.ts` and `*.test.tsx`), read as text
- **WHEN** they are searched for the removed names
- **THEN** none of `--color-border`, `--color-action-success`, `--shadow-card`, `--space-1`, `--space-2`, `--space-3`, `--space-4` or `--arrow-width` occurs

#### Scenario: web-client/R25-S2 Every referenced token is defined
- **GIVEN** every `var(--name)` in `frontend/src/**/*.css` and the non-test TSX files (test files are excluded), and the names defined in `tokens.css`
- **WHEN** each referenced name is looked up
- **THEN** it is defined in `tokens.css`

#### Scenario: web-client/R25-S3 Lint fails on an undefined token
- **GIVEN** a temporary folder that the test writes at run time, laid out like `src` (`styles/tokens.css` and a CSS file that reads an undefined token, the name built in the test so no source file under `src` contains the literal `var(--...)`)
- **WHEN** `frontend/scripts/check-design-tokens.mjs` runs with that folder as its argument
- **THEN** it exits with a non-zero code and its stderr names the file (relative to the folder), the line and the token
- **AND** with the name defined in the folder's `tokens.css` it exits with code 0

### Requirement: R26 Control borders have 3:1 contrast

`--color-control-border` SHALL have a contrast of at least 3:1 against every background it
can sit on, and the text pairs of the design code SHALL meet their ratios. Edges a person
operates SHALL use the control border; decorative edges SHALL use `--color-divider`, below
3:1 by design (scenarios).

#### Scenario: web-client/R26-S1 The control border passes 3:1 on every background
- **GIVEN** the colour values of `tokens.css`, read as text
- **WHEN** the contrast ratio of `--color-control-border` against each of `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail`, `--color-accent-subtle`, `--color-neutral-background`, `--color-success-background` and `--color-danger-background` is computed
- **THEN** every ratio is at least 3

#### Scenario: web-client/R26-S2 Control edges and decorative edges use their own tokens
- **GIVEN** the CSS of Field, Button, IssuedLink, List, Dialog and AppHeader, read as text
- **WHEN** their border rules are read
- **THEN** the text input, the select, the password toggle, the secondary button and the issued-link input use `--color-control-border`
- **AND** the List item, the IssuedLink box and the Dialog use `--color-divider`, and the AppHeader has a bottom border of `--border-width` solid `--color-divider`
- **AND** the field error border uses `--border-width-strong` and `--color-danger-text` (web-client R30)

#### Scenario: web-client/R26-S3 Text pairs meet their ratios
- **GIVEN** the colour values of `tokens.css`, read as text, and the contrast helper of R26-S1
- **WHEN** the ratios of the pairs listed in the design-code "Colour" section are computed
- **THEN** `--color-text` and `--color-text-muted` on `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail` and `--color-accent-subtle` are at least 4.5
- **AND** `--color-accent` (link text) on `--color-surface` and on `--color-rail`, `--color-on-accent` on `--color-accent` and on `--color-danger-action`, and the neutral, success and danger text on their own backgrounds are at least 4.5

### Requirement: R27 Button roles and states

`Button` SHALL have the variants `primary`, `secondary`, `quiet`, `dangerText`, `muted`
and `danger`, and no green variant. "Add teacher" (with its plus icon) and "Create
teacher" SHALL be the primary variant (accent fill with `--color-on-accent` text). The
danger variant SHALL fill with `--color-danger-action`. The row "Deactivate" button SHALL
stay a filled danger button until the admin row actions are restyled. The secondary
variant SHALL have `--color-text` text. Every variant SHALL have hover (only under `@media (hover: hover)`), disabled and focus states, and an active state except the danger fill, made only of design tokens. The `icon` prop SHALL accept any icon of the
shared set, and a button that leads with an icon SHALL start its padding
`--button-icon-inset` closer to the edge than `--space-group`. A full-width variant for
phones SHALL be used only by the lone submit button of the login and set-password pages.

#### Scenario: web-client/R27-S1 Add teacher and Create teacher are the accent primary
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button and, in the open dialog, the "Create teacher" button are read by role and name
- **THEN** each has the primary class of `Button`, and not any other variant class
- **AND** the primary rule fills with `--color-accent` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S2 There is no success variant
- **GIVEN** `Button.tsx` and `Button.module.css`, read as text
- **WHEN** the variants are read
- **THEN** the variant type is `'primary' | 'secondary' | 'quiet' | 'dangerText' | 'muted' | 'danger'`
- **AND** the CSS has no `.success` rule and no token starting `--color-action-success`

#### Scenario: web-client/R27-S3 The danger rule fills with the danger action colour
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the `.danger` rule is read
- **THEN** it fills and edges with `--color-danger-action` and sets `--color-on-accent` text

#### Scenario: web-client/R27-S4 The row Deactivate button stays a danger button
- **GIVEN** an admin page with an active teacher
- **WHEN** the row "Deactivate" button is read by role and name
- **THEN** it has the danger class of `Button`

#### Scenario: web-client/R27-S5 The new variants have their own look
- **GIVEN** `Button.tsx` and `Button.module.css`, read as text, and a `Button` rendered with each of `quiet`, `dangerText` and `muted` in a unit test (no screen uses them yet)
- **WHEN** each button's classes and the CSS rules are read
- **THEN** each carries its own variant class and no other, and the label is shown
- **AND** `quiet` is transparent with `--color-text` text and the `--color-surface-sunken` hover fill, `dangerText` has `--color-danger-text` text and a `--color-danger-background` hover fill, and `muted` has `--color-text-muted` text that goes to `--color-text` on hover; both have an active fill of `--color-neutral-background`

#### Scenario: web-client/R27-S6 The secondary button has text colour
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the `.secondary` rule is read
- **THEN** its text colour is `--color-text`, its fill `--color-surface` and its border `--color-control-border`
- **AND** no rule of a variant other than `primary` uses `--color-accent` as a text colour

#### Scenario: web-client/R27-S7 Every button state is a token
- **GIVEN** `Button.module.css`, read as text
- **WHEN** the base, hover, active and disabled rules are read
- **THEN** the base has `min-height: var(--control-height)`, `padding: 0 var(--space-group)`, `font-weight: var(--weight-strong)` and `line-height: var(--line-height-tight)`
- **AND** hover (on an enabled button only) is `--color-accent-hover` for primary, `--color-surface-sunken` for secondary and quiet, and `--color-danger-action-hover` for danger; active is `--color-accent-active` for primary and `--color-neutral-background` for secondary, quiet, `dangerText` and `muted`, and the danger fill has no active step; primary and danger change fill and border together on hover and active, and every hover rule is inside `@media (hover: hover)`
- **AND** disabled is `opacity: var(--opacity-disabled)` with `cursor: not-allowed`, and the transitions use `--duration-fast` on `background-color`, `border-color`, `color` and `opacity` only

#### Scenario: web-client/R27-S8 The icon prop takes any icon and insets an icon-led button
- **GIVEN** `Button` rendered with `icon` set to the plus and to other icons of the shared set (a unit test), and `Button.module.css` read as text
- **WHEN** the markup and the rules are read
- **THEN** each icon is the first child, `aria-hidden="true"` and `focusable="false"`, with its own path from the icon table, and the button has the icon-led class; a button without an icon has no such class
- **AND** the icon-led rule has `padding-inline-start: calc(var(--space-group) - var(--button-icon-inset))`, written with logical properties and without `:has`, and the gap stays `--space-inline`
- **AND** "Add teacher" has the icon-led class and "Create teacher" does not

#### Scenario: web-client/R27-S9 The full-width variant is for the lone submit on phones
- **GIVEN** `Button.module.css` and the TSX files under `frontend/src`, read as text
- **WHEN** the full-width rule and its uses are read
- **THEN** the rule is inside `@media (max-width: 63.999rem)` and makes the button fill its row, and above that width the button keeps its natural width
- **AND** only the submit buttons of `LoginPage` and `SetPasswordPage` use it, and the `ButtonRow` rules are unchanged

### Requirement: R28 No card shadows, shared select chevron

Only overlays SHALL use a shadow, and only `--shadow-overlay`; no CSS file under `src`
SHALL define a `box-shadow` for the List item, StatusMessage, AppHeader or IssuedLink. The
chevron of every `Select` SHALL be the shared `ChevDownIcon` at `--icon-size`, in
`--color-text-muted`, inside the existing `data-select-arrow` box, which stays inert to the
pointer.

#### Scenario: web-client/R28-S1 No card has a shadow
- **GIVEN** the CSS files under `frontend/src/ui`, read as text
- **WHEN** their `box-shadow` declarations are read
- **THEN** none exists, or each one is `var(--shadow-overlay)`
- **AND** the List item, the StatusMessage, the AppHeader and the IssuedLink rules have no `box-shadow`

#### Scenario: web-client/R28-S2 The select chevron rules
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the arrow and select rules are read
- **THEN** the arrow rule has `pointer-events: none`, colour `--color-text-muted` and `right: calc(var(--border-width) + var(--space-group))`
- **AND** the select rule has `appearance: none` and `padding-right: calc(var(--space-group) + var(--icon-size) + var(--space-inline))`
- **AND** the file has no `.arrowIcon` rule

#### Scenario: web-client/R28-S3 The select chevron is the shared icon
- **GIVEN** a rendered `Select` (the create-teacher dialog's time zone select, the profile time zone, the language select)
- **WHEN** the `[data-select-arrow]` element is read
- **THEN** it is `aria-hidden="true"` and contains the `svg` of `ChevDownIcon` (path `M5 8l5 5 5-5` as in the icon table) with the default size class, and no other `svg`

#### Scenario: web-client/R28-S4 The chevron renders at the icon size
- **GIVEN** a logged-in admin with the "Add teacher" dialog open, on the desktop and phone projects
- **WHEN** the chevron in the time zone select is measured by its computed style
- **THEN** its width and its height are 20 pixels

### Requirement: R29 Busy button

A button whose request is in flight SHALL be natively disabled, have `aria-busy="true"` and show its own busy label, with no spinner and a width that never shrinks while busy (it may grow). This covers the form submits, the deactivate "Confirm" and the pressed row button, and nothing else (not Log out, not "Try again"). A busy label ends with "…" and, in Russian, is a noun. Starting a request SHALL announce nothing.

#### Scenario: web-client/R29-S1 A busy button is disabled, marked busy and named by its busy label
- **GIVEN** a `Button` with an idle label and a busy label, rendered idle and then busy (a unit test)
- **WHEN** it is read by role and name
- **THEN** idle, it is found by the idle label, is enabled and has no `aria-busy="true"`, and only the idle label is in the DOM (its `textContent` is the idle label)
- **AND** busy, it is found by the busy label, is `disabled` and has `aria-busy="true"`, the idle label is not part of its accessible name (it is `aria-hidden`), no `svg` was added, and pressing it calls no handler

#### Scenario: web-client/R29-S2 The busy button never gets narrower
- **GIVEN** `Button.module.css`, read as text, and in `en` and in `ru` the login page on the desktop project and, on the 360-pixel phone project, a button that is not full width (profile Save, "Create teacher" or a row button), each with its request held
- **WHEN** the button is pressed
- **THEN** the button's width while the request runs is at least its width before, equal where the busy label is not longer than the idle one, and its height is the same
- **AND** the CSS puts the two labels in one grid cell only while busy, the idle one with `visibility: hidden`, and the button has no `style` attribute

#### Scenario: web-client/R29-S3 Each form's submit button shows its own busy label
- **GIVEN** the login page, the set-password page, the profile form, the profile password form and the "Add teacher" dialog, each with its request held
- **WHEN** each form is submitted
- **THEN** the submit button is found by the text of its own key, `login.submitting`, `setPassword.submitting`, `profile.saving`, `profile.changingPassword` and `adminTeachers.create.submitting` in turn, and is `disabled` with `aria-busy="true"`
- **AND** when the request fails, the button is found by its idle label again and has no `aria-busy="true"`; when it succeeds the button is gone (the route changed or the dialog closed) or, for Save and Change password, idle again

#### Scenario: web-client/R29-S4 The pressed row button and Confirm show their busy labels
- **GIVEN** an admin page with an active teacher without a password, one with a password and a deactivated one, and the deactivate dialog open, each request held
- **WHEN** "New invite link", "Password reset link" or "Reactivate" is pressed on its row, and in the dialog "Confirm" is pressed
- **THEN** the pressed row button shows its own busy label (`adminTeachers.busy.inviteLink`, `adminTeachers.busy.resetLink` or `adminTeachers.busy.reactivate`) with `aria-busy="true"`, and the other buttons of that row keep their idle labels, are disabled and are not marked busy
- **AND** "Confirm" shows `adminTeachers.deactivate.confirming` with `aria-busy="true"`, and "Cancel" keeps its label and is disabled

#### Scenario: web-client/R29-S5 Busy labels exist in both languages and follow the copy rule
- **GIVEN** `en.json` and `ru.json`, read by a bundle test, and the keys `login.submitting`, `setPassword.submitting`, `profile.saving`, `profile.changingPassword`, `adminTeachers.create.submitting`, `adminTeachers.deactivate.confirming`, `adminTeachers.busy.inviteLink`, `adminTeachers.busy.resetLink` and `adminTeachers.busy.reactivate`
- **WHEN** the keys are read in both bundles
- **THEN** each exists in both with a non-empty text that ends with the single character "…", never three dots, and the Russian text differs from the English one
- **AND** the existing parity and copy-rule tests pass with the new keys

#### Scenario: web-client/R29-S6 Russian busy labels fit a phone screen
- **GIVEN** the browser has a 360-pixel-wide screen, the UI language is `ru`, and each request of R29-S3 and R29-S4 is held
- **WHEN** each button is pressed and shows its busy label
- **THEN** on every such page and dialog `document.documentElement.scrollWidth <= clientWidth` and no element is wider than the viewport

#### Scenario: web-client/R29-S7 Starting a request announces nothing and moves no focus
- **GIVEN** the login page, with the request held, and in a second run a form whose request then fails with field errors (R17-S1)
- **WHEN** the submit button is pressed
- **THEN** no element with `role="status"` or `role="alert"` and no live region appears while the request runs, and `document.activeElement` is the same element before the press and while the request runs when the form was submitted from a field (not moved by the busy state)
- **AND** when the request fails with field errors, the focus goes to the topmost invalid field as web-client R17 says, and when it fails without them the button is enabled again with its idle label

### Requirement: R30 Field focus, error and states

Every interactive element SHALL show one global `:focus-visible` ring and never `outline: none`. A field with an error SHALL be `aria-invalid` and described by a message under it that has an alert icon and no `role="alert"`, with a `--border-width-strong` danger border that moves nothing; the live hints of web-client R14 take the same path. Fields SHALL have hover, disabled and read-only looks, and the password toggle SHALL be a `Button` (scenarios).

#### Scenario: web-client/R30-S1 One global focus rule, never removed
- **GIVEN** `base.css` and every `.css` file under `frontend/src`, and an unauthenticated visitor on the login page and on the not-found page
- **WHEN** the CSS rules are read and the visitor tabs through the language select, email input, password input, password visibility toggle and sign-in button on the login page, and to the link to the start page on the not-found page
- **THEN** `base.css` has one `:focus-visible` rule with `outline: var(--border-width-strong) solid var(--color-focus-ring)` and `outline-offset: var(--focus-offset)`, and no CSS file contains `outline: none` or `outline: 0`
- **AND** each named element receives keyboard focus and has a computed outline of 2 pixels, solid, at an offset of 2 pixels while focused

#### Scenario: web-client/R30-S2 A focused invalid field shows the red border and the ring
- **GIVEN** the set-password page with "abc" typed in the new password field, which is focused and shows its "at least 8 characters" hint
- **WHEN** the computed style of the field is read
- **THEN** its border is 2 pixels wide in `--color-danger-text` and its outline is 2 pixels wide in `--color-focus-ring`, so both are visible
- **AND** the same holds for the profile form's wrong current password after a failed change

#### Scenario: web-client/R30-S3 An erroring field is described by its message
- **GIVEN** `TextInput`, `PasswordInput` and `Select` rendered with an `error` text and without one (a unit test)
- **WHEN** each control is read by its label
- **THEN** with an error it has `aria-invalid="true"` and an `aria-describedby` that names the message element, its accessible description equals the message text exactly, and the message has no `role="alert"`
- **AND** the message's first child is an `svg` of the alert icon at the small icon size, `aria-hidden="true"` and `focusable="false"`; without an error none of this exists

#### Scenario: web-client/R30-S4 The error rules keep the field still
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the invalid and message rules are read
- **THEN** the invalid rule has a border of `--border-width-strong` in `--color-danger-text` and `padding-inline-start: calc(var(--space-group) - var(--border-width))`, and no `outline: none`
- **AND** the message rule is a flex row with `gap: var(--space-inline)`, `--text-small`, `--line-height-small` and `--color-danger-text`, has no `text-overflow`, `white-space: nowrap` or `overflow: hidden`, and the icon sits on the first line of the text; a Playwright check shows the icon's vertical centre within the first text line's box

#### Scenario: web-client/R30-S5 A long error message wraps under its text
- **GIVEN** a text input and the time zone select with a long error text (the longest Russian field error), at 360 pixels, and the same controls without an error
- **WHEN** the layout is measured
- **THEN** the message wraps onto more lines inside the field's width, every line's text starts at the same left edge to the right of the icon, and no text is cut
- **AND** the control's height, the text's start and the select arrow's position are the same with and without the error, within 1 pixel

#### Scenario: web-client/R30-S6 The live password hints look like errors
- **GIVEN** the set-password page and the profile password form
- **WHEN** a 3-character new password is typed, and then a repeat that differs
- **THEN** each hint field is `aria-invalid="true"`, has the 2-pixel danger border and the alert icon, and its hint is the field's accessible description, as in R30-S3
- **AND** the wording and timing of the hints stay as web-client R14 says

#### Scenario: web-client/R30-S7 Fields hover, disabled and read-only
- **GIVEN** `Field.module.css`, read as text
- **WHEN** the input and select rules are read
- **THEN** an enabled, writable, valid input or select has a hover border of `--color-text-muted`, in a rule inside `@media (hover: hover)` that does not use `:read-only` (a computed hover check on a select shows the border); an invalid, a disabled or a read-only (`[readonly]`) one has no hover rule; a field keeps `min-height: var(--control-height)`
- **AND** a disabled input or select (and its arrow) has `opacity: var(--opacity-disabled)` and `cursor: not-allowed`, and the read-only issued-link input keeps its `--color-surface-sunken` fill

#### Scenario: web-client/R30-S8 The password toggle is a Button
- **GIVEN** `PasswordInput` rendered, hidden and shown, enabled and disabled (a unit test), and `Field.module.css` read as text
- **WHEN** the toggle is read by role
- **THEN** it is a `Button` of the secondary variant with `aria-pressed` that follows the state, and its accessible name is the text of `ui.showPassword` or `ui.hidePassword` with the field's label
- **AND** it is natively `disabled` when the field is, and the CSS has no `.toggle` rule
