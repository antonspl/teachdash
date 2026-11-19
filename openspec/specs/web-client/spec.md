# web-client Specification

## Purpose

The browser application shell and how it talks to the backend: routing, translated UI
strings, the API client and same-origin serving. Product screens build on it.

## Requirements

### Requirement: R2 Translated UI strings

Every string the web client shows to a user SHALL come from the translation resources.
English is the only UI language for now.

#### Scenario: web-client/R2-S1 Page text comes from the translation resources
- **GIVEN** the translation resources are replaced with test values
- **WHEN** the login page and the not-found page are rendered
- **THEN** each page shows the test values
- **AND** no English text from the real resources is shown

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
- **AND** the login page it redirects to renders with no Content Security Policy violations

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
