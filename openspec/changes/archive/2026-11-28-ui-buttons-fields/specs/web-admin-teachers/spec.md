# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Create a teacher

An "Add teacher" button at the top of the page SHALL open a modal dialog with name,
email, time zone and native language. The time zone SHALL be chosen from the browser's
time zones plus `UTC`, with the browser's own zone preselected. The native language SHALL
be a required select with no language preselected, whose options are the codes returned
by `GET /api/languages`, shown with names from the translation resources (a code with no name is shown as the code itself). Nothing SHALL
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
- **WHEN** the admin presses the same submit button again (found, while the request runs, by its busy label, `adminTeachers.create.submitting`, web-client R29)
- **THEN** no second request is sent
- **AND** that button and "Cancel" are disabled, and the submit button has `aria-busy="true"`

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
- **WHEN** the admin opens the "Add teacher" dialog, fills in the other fields and submits, and then presses "Try again"
- **THEN** a translated error with a "Try again" button is shown and no request is sent on submit, and after "Try again" the error is gone, the other values stay and the select offers the languages

#### Scenario: web-admin-teachers/R2-S14 A language with no name shows its code
- **GIVEN** the backend lists the languages "de" and "sv", and the translation resources have no name for "sv"
- **WHEN** the admin opens the "Add teacher" dialog
- **THEN** the language select offers "German" and "sv"

### Requirement: R4 Deactivate and reactivate

Deactivating SHALL first open a modal dialog naming the teacher and saying their
students will be released and will not come back on reactivation. Only confirming SHALL
send the request, once. Reactivating SHALL need no confirmation; a returned link SHALL
be shown (R3). After either action, successful or not, the list SHALL load again.

#### Scenario: web-admin-teachers/R4-S1 Deactivate asks first
- **GIVEN** an active teacher "Anna Smith" in the list
- **WHEN** the admin presses "Deactivate" on her row
- **THEN** a modal dialog names "Anna Smith" and says her students will be released
- **AND** no request has been sent

#### Scenario: web-admin-teachers/R4-S2 Confirming deactivates the teacher once
- **GIVEN** the deactivate dialog for an active teacher is open
- **WHEN** the admin presses "Confirm" twice quickly
- **THEN** exactly one deactivate request is sent for that teacher
- **AND** while it runs, the same button shows its busy label (`adminTeachers.deactivate.confirming`, web-client R29), is disabled and has `aria-busy="true"`, and "Cancel" is disabled
- **AND** the dialog closes and the teacher's status reads "Deactivated"

#### Scenario: web-admin-teachers/R4-S3 Cancelling changes nothing
- **GIVEN** the deactivate dialog is open
- **WHEN** the admin presses "Cancel", or presses Escape
- **THEN** the dialog closes, no request is sent and the teacher stays active

#### Scenario: web-admin-teachers/R4-S4 Reactivating a teacher who has a password
- **GIVEN** a deactivated teacher who has set a password
- **WHEN** the admin presses "Reactivate" and the backend answers 204
- **THEN** the teacher's status reads "Active" and no link is shown

#### Scenario: web-admin-teachers/R4-S5 Reactivating a teacher who never set a password
- **GIVEN** a deactivated teacher who never set a password
- **WHEN** the admin presses "Reactivate" and the backend answers 200 with a link
- **THEN** the link is shown for that teacher and their status reads "Waiting for password"

#### Scenario: web-admin-teachers/R4-S6 A failed deactivation is reported
- **GIVEN** the backend answers the deactivate request with 404 `NOT_FOUND`
- **WHEN** the admin confirms the dialog
- **THEN** the dialog closes, the translated `NOT_FOUND` message is shown and the list loads again

#### Scenario: web-admin-teachers/R4-S7 A failed reactivation is reported
- **GIVEN** the backend answers the reactivate request with 500 `INTERNAL_ERROR`
- **WHEN** the admin presses "Reactivate"
- **THEN** the translated `INTERNAL_ERROR` message is shown, the list loads again and the row's buttons work again

#### Scenario: web-admin-teachers/R4-S8 Focus moves into the dialog and back
- **GIVEN** an active teacher in the list
- **WHEN** the admin opens the deactivate dialog and then cancels it
- **THEN** focus is inside the dialog while it is open, and returns to that row's "Deactivate" button after it closes

### Requirement: R5 New link for an active teacher

Each active teacher's row SHALL offer a new link: "New invite link" when the teacher has
no password yet, "Password reset link" otherwise. Pressing it SHALL request a reset link
and show it (R3). A deactivated teacher's row SHALL NOT offer a link. While any request
for a row is in progress, none of that row's buttons SHALL send another.

#### Scenario: web-admin-teachers/R5-S1 New invite link for a teacher without a password
- **GIVEN** an active teacher "Anna Smith" who has no password yet
- **WHEN** the admin presses "New invite link" on her row
- **THEN** a reset link is requested for her and the returned link is shown for "Anna Smith"

#### Scenario: web-admin-teachers/R5-S2 Password reset link for a teacher with a password
- **GIVEN** an active teacher who has a password
- **WHEN** the admin looks at the row
- **THEN** the row offers "Password reset link" and not "New invite link"

#### Scenario: web-admin-teachers/R5-S3 No link for a deactivated teacher
- **GIVEN** a deactivated teacher
- **WHEN** the admin looks at the row
- **THEN** the row offers "Reactivate" and no link button

#### Scenario: web-admin-teachers/R5-S4 A row's buttons wait for its request
- **GIVEN** a link request for a teacher is in progress
- **WHEN** the admin presses that row's link button again (the same button, now showing its busy label, `adminTeachers.busy.inviteLink` or `adminTeachers.busy.resetLink`, web-client R29), or its "Deactivate"
- **THEN** no second request is sent and no dialog opens
- **AND** the pressed button is disabled and has `aria-busy="true"`, and the row's other buttons are disabled and keep their own labels

#### Scenario: web-admin-teachers/R5-S5 A failed link request is reported
- **GIVEN** the backend answers the reset link request with 403 `CSRF_INVALID`
- **WHEN** the admin presses the row's link button
- **THEN** the translated `CSRF_INVALID` message is shown, no link is shown and the row's buttons work again
