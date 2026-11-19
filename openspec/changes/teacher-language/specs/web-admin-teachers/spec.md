# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Create a teacher

An "Add teacher" button at the top of the page SHALL open a modal dialog with name,
email, time zone and native language. The time zone SHALL be chosen from the browser's
time zones plus `UTC`, with the browser's own zone preselected. The native language SHALL
be a required select with no language preselected, whose options are the codes returned
by `GET /api/languages`, shown with names from the translation resources. Nothing SHALL
be sent until a language is chosen; if the list cannot be loaded, the dialog SHALL show a
translated error with a "Try again" button. On success the dialog SHALL close, the
link SHALL be shown (R3) and the list SHALL load again with the new teacher first until
the page is reloaded. On failure the dialog SHALL stay open with the entered values.

#### Scenario: web-admin-teachers/R2-S1 Admin creates a teacher
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the admin presses "Add teacher", enters a name, a new email, a time zone and a native language, and submits
- **THEN** the dialog closes and the new teacher is the first row of the list
- **AND** the set-password link is shown inside that row

#### Scenario: web-admin-teachers/R2-S2 The browser's time zone is preselected
- **GIVEN** the browser's time zone is "Europe/Moscow"
- **WHEN** the admin opens the "Add teacher" dialog
- **THEN** the time zone select shows "Europe/Moscow" and also offers "UTC"

#### Scenario: web-admin-teachers/R2-S3 Email already in use
- **GIVEN** the backend answers the create request with 409 `EMAIL_TAKEN`
- **WHEN** the admin submits the dialog
- **THEN** the dialog stays open with the translated `EMAIL_TAKEN` message
- **AND** the entered name, email, time zone and language stay in the form

#### Scenario: web-admin-teachers/R2-S4 Backend rejects the fields
- **GIVEN** the backend answers the create request with 400 `VALIDATION_FAILED`
- **WHEN** the admin submits the dialog
- **THEN** the dialog stays open with a translated message asking the admin to check the fields or try another time zone, and the entered values stay

#### Scenario: web-admin-teachers/R2-S5 The form is not sent twice
- **GIVEN** a create request is in progress
- **WHEN** the admin presses the submit button again
- **THEN** no second request is sent

#### Scenario: web-admin-teachers/R2-S6 Network failure on create
- **GIVEN** the create request fails without reaching the backend
- **WHEN** the admin submits the dialog
- **THEN** the dialog stays open with the translated generic error, the entered values stay and the submit button works again

#### Scenario: web-admin-teachers/R2-S7 Cancelling the dialog
- **GIVEN** the admin opened the "Add teacher" dialog and typed a name
- **WHEN** the admin presses "Cancel", or presses Escape
- **THEN** the dialog closes, no request is sent and focus returns to the "Add teacher" button
- **AND** opening the dialog again shows an empty form

#### Scenario: web-admin-teachers/R2-S8 The new teacher stays first until a reload
- **GIVEN** the backend returns the list sorted by name, with the new teacher "Zoe Young" last
- **WHEN** the admin creates "Zoe Young"
- **THEN** "Zoe Young" is the first row and is marked with a translated "just added" label
- **AND** after the page is reloaded the list is in the backend's order without the label

#### Scenario: web-admin-teachers/R2-S9 The language select starts empty
- **GIVEN** the backend lists the languages "de", "en" and "ru"
- **WHEN** the admin opens the "Add teacher" dialog
- **THEN** the language select has no language selected and offers exactly "German", "English" and "Russian"

#### Scenario: web-admin-teachers/R2-S10 No language, no request
- **GIVEN** the "Add teacher" dialog with a name, an email and a time zone filled in and no language chosen
- **WHEN** the admin submits the dialog
- **THEN** no request is sent, and a translated message at the language select asks for a language

#### Scenario: web-admin-teachers/R2-S11 The request carries the language
- **GIVEN** the "Add teacher" dialog filled in with the language "German"
- **WHEN** the admin submits the dialog
- **THEN** the create request carried the native language "de"

#### Scenario: web-admin-teachers/R2-S12 A language the backend rejects
- **GIVEN** the backend answers the create request with 400 `VALIDATION_FAILED` and `errors` `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`
- **WHEN** the admin submits the dialog
- **THEN** the dialog stays open with the translated `INVALID_FORMAT` message at the language select, and the entered values stay

#### Scenario: web-admin-teachers/R2-S13 The language list does not load
- **GIVEN** loading the languages fails with a server error, and then succeeds
- **WHEN** the admin opens the "Add teacher" dialog, and presses "Try again"
- **THEN** a translated error with a "Try again" button is shown, no request is sent on submit, and after "Try again" the select offers the languages
