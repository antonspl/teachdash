# Spec Delta

## MODIFIED Requirements

### Requirement: R5 Translated API errors

When a request fails, the API client SHALL give the caller a translated message chosen
by the problem `code`. A code without a translation, or a response that is not a problem
response, SHALL give a generic translated message. The message for `INVALID_CREDENTIALS`
SHALL say only that the email and password do not match; the login page shows the hint
about getting a link separately (`web-auth` R1). When a problem response carries
`errors` (`api-errors` R1-R2), the client SHALL also give the caller its `{field, code}`
entries in the order received, leaving out every entry whose `field` or `code` is not a
string; a response with no such entry gives the caller no field errors.

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

## ADDED Requirements

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
- **GIVEN** one run per form, the backend answering the request with status 400, code `VALIDATION_FAILED` and the `errors` given: the login page with `email` `TOO_LONG`; the set-password page with `password` `TOO_SHORT`; the profile form with `timeZone` `INVALID_FORMAT`; the password form with `currentPassword` `REQUIRED`, and in another run `newPassword` `TOO_LONG`
- **WHEN** the form is submitted with values that pass its own checks
- **THEN** in every run the translated message for the code is shown under the named field (the email, the new password, the time zone select, the current password, the new password), that field is marked invalid, and no form-level message is shown

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
the others, and a new submit SHALL remove them all before the request is sent. A field
that shows a client-side hint (R14) SHALL show the hint instead of a backend error. On
the set-password page, editing a field SHALL also remove the form-level message.

#### Scenario: web-client/R18-S1 Editing a field removes only its error
- **GIVEN** the "Add teacher" dialog shows backend errors under the name and the email, and the backend holds its answer to the next request
- **WHEN** the admin changes the value of the email field
- **THEN** the email's error is gone and the email is no longer marked invalid, while the name still shows its error
- **AND** when the admin submits again, the name's error is gone while the request is still waiting for its answer

#### Scenario: web-client/R18-S2 Editing on the set-password page removes the form-level message
- **GIVEN** the set-password page shows the translated message for `LINK_CONFLICT` after a submit
- **WHEN** the user changes the value of the new password field, and in a second run of the repeat field
- **THEN** in both runs the `LINK_CONFLICT` message is gone and the fields keep the typed values
