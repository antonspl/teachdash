# Spec Delta

## Purpose

The admin's page in the web client for managing teacher accounts: the teacher list,
creating teachers, deactivating and reactivating them, and handing out set-password
links.

## ADDED Requirements

### Requirement: R1 Teacher list

The admin cabinet SHALL list every teacher in the order the backend returns them, with
name, email, time zone and a translated status: active, waiting for password (active,
no password set yet) or deactivated. All text SHALL come from the translation
resources.

#### Scenario: web-admin-teachers/R1-S1 Each teacher is shown with their status
- **GIVEN** the backend returns an active teacher with a password, an active teacher without a password and a deactivated teacher
- **WHEN** the admin opens `/admin`
- **THEN** each teacher's name, email and time zone are shown, in the backend's order
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

### Requirement: R2 Create a teacher

The admin SHALL be able to create a teacher with name, email and time zone. The time
zone SHALL be chosen from a list of the browser's time zones plus `UTC`, with the
browser's own zone preselected. On success the page SHALL show the set-password link,
clear the form and load the list again. On failure the entered values SHALL stay.

#### Scenario: web-admin-teachers/R2-S1 Admin creates a teacher
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the admin enters a name, a new email and a time zone, and submits
- **THEN** the set-password link is shown
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
- **THEN** the translated `VALIDATION_FAILED` message is shown and the entered values stay

#### Scenario: web-admin-teachers/R2-S5 The form is not sent twice
- **GIVEN** a create request is in progress
- **WHEN** the admin presses the submit button again
- **THEN** no second request is sent

### Requirement: R3 Issued link

A set-password link returned by the backend SHALL be shown once on the page as plain
text, not as a clickable link, with a "Copy" button and a translated note that it is
shown only now, works once within 7 days, replaces any earlier unused link and must
not be opened in this browser. A newly issued link SHALL replace the one shown.

#### Scenario: web-admin-teachers/R3-S1 Link is shown as text with a note
- **GIVEN** the backend returned a set-password link for "Anna Smith"
- **WHEN** the link is shown
- **THEN** the full link text and the teacher's name are visible, the link is not a clickable element, and the note is shown

#### Scenario: web-admin-teachers/R3-S2 Copy puts the link on the clipboard
- **GIVEN** a link is shown
- **WHEN** the admin presses "Copy"
- **THEN** the clipboard holds the full link and a translated "Copied" confirmation is shown

#### Scenario: web-admin-teachers/R3-S3 Copy fails
- **GIVEN** a link is shown and the browser refuses clipboard access
- **WHEN** the admin presses "Copy"
- **THEN** a translated message asks the admin to copy the link by hand, and the link text is selected

#### Scenario: web-admin-teachers/R3-S4 Closing the link box
- **GIVEN** a link is shown
- **WHEN** the admin presses "Close"
- **THEN** the link is no longer on the page

### Requirement: R4 Deactivate and reactivate

Deactivating SHALL first open a modal dialog naming the teacher and saying their
students will be released and will not come back on reactivation. Only confirming SHALL
send the request. Reactivating SHALL need no confirmation; if the backend returns a
link, the page SHALL show it (R3). After either action the list SHALL load again.

#### Scenario: web-admin-teachers/R4-S1 Deactivate asks first
- **GIVEN** an active teacher "Anna Smith" in the list
- **WHEN** the admin presses "Deactivate" on her row
- **THEN** a modal dialog names "Anna Smith" and says her students will be released
- **AND** no request has been sent

#### Scenario: web-admin-teachers/R4-S2 Confirming deactivates the teacher
- **GIVEN** the deactivate dialog for an active teacher is open
- **WHEN** the admin confirms
- **THEN** the dialog closes and the teacher's status reads "Deactivated"

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

#### Scenario: web-admin-teachers/R4-S6 A failed action is reported
- **GIVEN** the backend answers a deactivate or reactivate request with 404 `NOT_FOUND`
- **WHEN** the admin does that action
- **THEN** the translated `NOT_FOUND` message is shown and the list loads again

### Requirement: R5 New link for an active teacher

Each active teacher's row SHALL offer a new link: "New invite link" when the teacher has
no password yet, "Password reset link" otherwise. Pressing it SHALL request a reset link
and show it (R3). A deactivated teacher's row SHALL NOT offer a link. While a row's
request is in progress, its buttons SHALL NOT send another.

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

#### Scenario: web-admin-teachers/R5-S4 A row's request is not sent twice
- **GIVEN** a link request for a teacher is in progress
- **WHEN** the admin presses that row's link button again
- **THEN** no second request is sent
