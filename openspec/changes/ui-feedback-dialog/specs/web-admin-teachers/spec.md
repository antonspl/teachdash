# Spec Delta

## MODIFIED Requirements

### Requirement: R1 Teacher list

The admin cabinet SHALL list every teacher with name, email, time zone and a status in
words: active, waiting for password (active, no password set yet) or deactivated. All
text SHALL come from the translation resources. The page SHALL fit a phone screen
without horizontal scrolling. A 401 on any of its requests SHALL go through the
session-ended flow (web-auth R6). The load SHALL show plain localized text and the status badges SHALL pair text with decorative icons.

#### Scenario: web-admin-teachers/R1-S1 Each teacher is shown with their status
- **GIVEN** the backend returns an active teacher with a password, an active teacher without a password and a deactivated teacher
- **WHEN** the admin opens `/admin`
- **THEN** each teacher's name, email and time zone are shown
- **AND** their statuses read "Active", "Waiting for password" and "Deactivated"
- **AND** Active has the success tone and right icon, Waiting for password has the neutral tone and clock icon, and Deactivated has the neutral tone and ban icon

#### Scenario: web-admin-teachers/R1-S2 No teachers yet
- **GIVEN** the backend returns an empty list
- **WHEN** the admin opens `/admin`
- **THEN** a translated "no teachers yet" message is shown together with the primary "Add teacher" button

#### Scenario: web-admin-teachers/R1-S3 List fails to load
- **GIVEN** loading the list fails with a server error
- **WHEN** the admin opens `/admin`
- **THEN** a translated error alert is shown in the list area with a separate "Try again" button
- **AND** pressing it loads the list again and shows the teachers

#### Scenario: web-admin-teachers/R1-S4 The page fits a phone screen
- **GIVEN** a teacher with a long email exists and a link has just been issued for them
- **WHEN** `/admin` is shown 360 pixels wide, and then the "Add teacher" dialog is opened
- **THEN** the page has no horizontal scroll and every row's buttons are visible, with each badge's icon and label together even when the badges wrap
- **AND** the dialog's fields and buttons fit the screen without horizontal scroll

#### Scenario: web-admin-teachers/R1-S5 Session ends during an action
- **GIVEN** a link is shown on `/admin`
- **WHEN** the admin presses "Deactivate" on a row, confirms, and the request gets 401
- **THEN** the login page is shown with the translated "your session has ended" message
- **AND** after logging in again `/admin` is shown without the old link

#### Scenario: web-admin-teachers/R1-S6 Loading the teacher list
- **GIVEN** the first teacher list response is held
- **WHEN** the admin opens `/admin`
- **THEN** the list area shows plain localized loading text and the Add teacher action remains available
- **AND** the empty message appears only if the response is an empty list

#### Scenario: web-admin-teachers/R1-S7 The new teacher's badge is neutral
- **GIVEN** the admin has just created a teacher and the refreshed list includes them
- **WHEN** the teacher's row is shown
- **THEN** the translated Just added badge uses the neutral tone and decorative check icon in the row's existing badge group

### Requirement: R3 Issued link

A link SHALL be shown once inside its teacher's row, as plain text and not as a link,
with "Copy" and "Close" and a note: shown only now, works once within 7 days, replaces
any earlier unused link, not to be opened in this browser. Only one link SHALL be
shown: a new one closes the old one. Only "Close" or a new link SHALL remove it. If
the teacher's row is not in the list, the link SHALL be shown above the list. Close SHALL restore focus to the available issuing action or Add teacher.

#### Scenario: web-admin-teachers/R3-S1 Link is shown as text with a note
- **GIVEN** the backend returned a set-password link for "Anna Smith"
- **WHEN** the link is shown
- **THEN** the full link text is shown in a labelled, selectable read-only field for "Anna Smith", no element with role `link` holds it, and the note is shown
- **AND** Copy is the primary action and Close is secondary

#### Scenario: web-admin-teachers/R3-S2 Copy puts the link on the clipboard
- **GIVEN** a link is shown
- **WHEN** the admin presses "Copy"
- **THEN** the clipboard holds the full link and a translated "Copied" confirmation is shown below the actions as a success status message

#### Scenario: web-admin-teachers/R3-S3 Copy fails
- **GIVEN** a link is shown and the browser refuses clipboard access
- **WHEN** the admin presses "Copy"
- **THEN** a translated neutral status message below the actions asks the admin to copy the link by hand, and the field has focus with the whole link text selected

#### Scenario: web-admin-teachers/R3-S4 Closing the link box
- **GIVEN** a link was issued from a row action and that action is still available
- **WHEN** the admin presses "Close"
- **THEN** the link is no longer on the page and keyboard focus returns to the issuing action

#### Scenario: web-admin-teachers/R3-S5 Link stays when the list fails to reload
- **GIVEN** the create request succeeds with a link
- **WHEN** loading the list again fails
- **THEN** the link is still shown with the teacher's name above the list, together with the list's error and "Try again"

#### Scenario: web-admin-teachers/R3-S6 A new link moves to its teacher's row
- **GIVEN** a link for "Anna Smith" is shown in her row
- **WHEN** the admin presses the link button in the row of "Bob Brown" and gets a link
- **THEN** the link for "Bob Brown" is shown in his row
- **AND** no link box is left in the row of "Anna Smith"

#### Scenario: web-admin-teachers/R3-S7 Closing the link when its issuing action is unavailable
- **GIVEN** a link is shown and its issuing action is absent or disabled, for example a Reactivate button replaced by the teacher's active row actions
- **WHEN** the admin presses "Close"
- **THEN** the link is removed and keyboard focus goes to Add teacher
