# Spec Delta

## Purpose

The admin's page in the web client for managing teacher accounts: the teacher list,
creating teachers, deactivating and reactivating them, and handing out set-password
links.

## ADDED Requirements

### Requirement: R1 Teacher list

The admin cabinet SHALL list every teacher with name, email, time zone and a status in
words: active, waiting for password (active, no password set yet) or deactivated. All
text SHALL come from the translation resources. The page SHALL fit a phone screen
without horizontal scrolling. A 401 on any of its requests SHALL go through the
session-ended flow (web-auth R6).

#### Scenario: web-admin-teachers/R1-S1 Each teacher is shown with their status
- **GIVEN** the backend returns an active teacher with a password, an active teacher without a password and a deactivated teacher
- **WHEN** the admin opens `/admin`
- **THEN** each teacher's name, email and time zone are shown
- **AND** their statuses read "Active", "Waiting for password" and "Deactivated"

#### Scenario: web-admin-teachers/R1-S2 No teachers yet
- **GIVEN** the backend returns an empty list
- **WHEN** the admin opens `/admin`
- **THEN** a translated "no teachers yet" message is shown together with the create form

#### Scenario: web-admin-teachers/R1-S3 List fails to load
- **GIVEN** loading the list fails with a server error
- **WHEN** the admin opens `/admin`
- **THEN** a translated error with a "Try again" button is shown
- **AND** pressing it loads the list again and shows the teachers

#### Scenario: web-admin-teachers/R1-S4 The page fits a phone screen
- **GIVEN** a teacher with a long email exists and a link has just been issued for them
- **WHEN** `/admin` is shown 360 pixels wide
- **THEN** the page has no horizontal scroll and every row's buttons are visible

#### Scenario: web-admin-teachers/R1-S5 Session ends during an action
- **GIVEN** a link is shown on `/admin`
- **WHEN** the admin presses "Deactivate" on a row, confirms, and the request gets 401
- **THEN** the login page is shown with the translated "your session has ended" message
- **AND** after logging in again `/admin` is shown without the old link

### Requirement: R2 Create a teacher

The admin SHALL be able to create a teacher with name, email and time zone. The time
zone SHALL be chosen from a list of the browser's time zones plus `UTC`, with the
browser's own zone preselected. On success the page SHALL show the set-password link,
clear the form and load the list again. On failure the entered values SHALL stay.

#### Scenario: web-admin-teachers/R2-S1 Admin creates a teacher
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the admin enters a name, a new email and a time zone, and submits
- **THEN** the set-password link is shown with the new teacher's name
- **AND** the new teacher appears in the list as "Waiting for password"
- **AND** the form is empty again

#### Scenario: web-admin-teachers/R2-S2 The browser's time zone is preselected
- **GIVEN** the browser's time zone is "Europe/Moscow"
- **WHEN** the admin opens `/admin`
- **THEN** the time zone select shows "Europe/Moscow" and also offers "UTC"

#### Scenario: web-admin-teachers/R2-S3 Email already in use
- **GIVEN** the backend answers the create request with 409 `EMAIL_TAKEN`
- **WHEN** the admin submits the form
- **THEN** the translated `EMAIL_TAKEN` message is shown
- **AND** the entered name, email and time zone stay in the form

#### Scenario: web-admin-teachers/R2-S4 Backend rejects the fields
- **GIVEN** the backend answers the create request with 400 `VALIDATION_FAILED`
- **WHEN** the admin submits the form
- **THEN** a translated message asks the admin to check the fields or try another time zone, and the entered values stay

#### Scenario: web-admin-teachers/R2-S5 The form is not sent twice
- **GIVEN** a create request is in progress
- **WHEN** the admin presses the submit button again
- **THEN** no second request is sent

#### Scenario: web-admin-teachers/R2-S6 Network failure on create
- **GIVEN** the create request fails without reaching the backend
- **WHEN** the admin submits the form
- **THEN** the translated generic error is shown, the entered values stay and the submit button works again

### Requirement: R3 Issued link

A link returned by the backend SHALL be shown once with the teacher's name, as plain
text and not as a link, with "Copy" and "Close" buttons and a note: it is shown only
now, works once within 7 days, replaces any earlier unused link and must not be opened
in this browser. A new link SHALL replace the one shown. Only "Close" or a new link
SHALL remove it.

#### Scenario: web-admin-teachers/R3-S1 Link is shown as text with a note
- **GIVEN** the backend returned a set-password link for "Anna Smith"
- **WHEN** the link is shown
- **THEN** the full link text is visible in a field labelled with "Anna Smith", no element with role `link` holds it, and the note is shown

#### Scenario: web-admin-teachers/R3-S2 Copy puts the link on the clipboard
- **GIVEN** a link is shown
- **WHEN** the admin presses "Copy"
- **THEN** the clipboard holds the full link and a translated "Copied" confirmation is shown as a status message

#### Scenario: web-admin-teachers/R3-S3 Copy fails
- **GIVEN** a link is shown and the browser refuses clipboard access
- **WHEN** the admin presses "Copy"
- **THEN** a translated status message asks the admin to copy the link by hand, and the whole link text is selected

#### Scenario: web-admin-teachers/R3-S4 Closing the link box
- **GIVEN** a link is shown
- **WHEN** the admin presses "Close"
- **THEN** the link is no longer on the page

#### Scenario: web-admin-teachers/R3-S5 Link stays when the list fails to reload
- **GIVEN** the create request succeeds with a link
- **WHEN** loading the list again fails
- **THEN** the link is still shown with the teacher's name, together with the list's error and "Try again"

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
- **WHEN** the admin presses that row's link button again, or its "Deactivate"
- **THEN** no second request is sent and no dialog opens

#### Scenario: web-admin-teachers/R5-S5 A failed link request is reported
- **GIVEN** the backend answers the reset link request with 403 `CSRF_INVALID`
- **WHEN** the admin presses the row's link button
- **THEN** the translated `CSRF_INVALID` message is shown, no link is shown and the row's buttons work again
