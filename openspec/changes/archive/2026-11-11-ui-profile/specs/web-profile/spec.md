# Spec Delta

## Purpose

The profile page of the web client, where a logged-in user of any role changes their
own name, time zone and password.

## ADDED Requirements

### Requirement: R1 Profile page for every role

The web client SHALL show a profile page at `/profile` to a logged-in user of any role;
an anonymous visitor gets the login page. The page SHALL show the user's email as plain
text, a profile form and a password form, and the cabinet header with a "Back to
cabinet" button, which opens the user's own cabinet, in place of "Profile". All text
SHALL come from the translation resources.

#### Scenario: web-profile/R1-S1 Each role opens the profile
- **GIVEN** a logged-in admin, a logged-in teacher and a logged-in student, one at a time
- **WHEN** each opens `/profile`
- **THEN** each sees the profile page with their own name in the header, and nobody is sent to a cabinet

#### Scenario: web-profile/R1-S2 Anonymous visitor gets the login page
- **GIVEN** nobody is logged in
- **WHEN** `/profile` is opened
- **THEN** the login page is shown

#### Scenario: web-profile/R1-S3 Page shows the current values
- **GIVEN** a logged-in teacher "Anna Smith" with email "anna@example.com" and time zone "Europe/Moscow"
- **WHEN** `/profile` is rendered
- **THEN** "anna@example.com" is shown as text and not in a field
- **AND** the name field holds "Anna Smith", the time zone select has "Europe/Moscow" selected, and the three password fields are empty

#### Scenario: web-profile/R1-S4 Back to cabinet opens the user's own cabinet
- **GIVEN** a logged-in student on `/profile`
- **WHEN** the student presses "Back to cabinet"
- **THEN** `/student` is shown

#### Scenario: web-profile/R1-S5 Page fits a phone
- **GIVEN** a logged-in teacher with a 200-character name and a 360-pixel-wide screen
- **WHEN** `/profile` is opened, and then `/teacher`
- **THEN** neither page has a horizontal scroll

#### Scenario: web-profile/R1-S6 Profile waits for the startup check
- **GIVEN** the request for the current user is still running
- **WHEN** `/profile` is opened
- **THEN** the loading state is shown, and neither the login page nor the profile

#### Scenario: web-profile/R1-S7 Startup error on the profile page
- **GIVEN** the request for the current user fails with a network error
- **WHEN** `/profile` is opened
- **THEN** the translated startup error with a "Try again" button is shown, as on a cabinet page

### Requirement: R2 Change name and time zone

The profile form SHALL have a name field and a time zone select, filled with the user's
current values, and a "Save" button. Both fields are required. On success the form and
the header SHALL show the values the backend returned, and a translated success message
SHALL stay until the next edit of the form. If the save fails, a translated message
SHALL say so and the header SHALL not change. The select SHALL always include the
user's stored time zone.

#### Scenario: web-profile/R2-S1 Saved profile survives a reload
- **GIVEN** a logged-in teacher "Anna" on `/profile`
- **WHEN** the teacher sets name "Anna S." and time zone "Asia/Dubai", presses "Save" and reloads the page
- **THEN** the header shows "Anna S.", the name field holds "Anna S." and the select has "Asia/Dubai" selected

#### Scenario: web-profile/R2-S2 Form and header show what the backend returned
- **GIVEN** a logged-in teacher on `/profile`, and the backend answers a save with name "Anna S." and time zone "Asia/Dubai"
- **WHEN** the teacher types "  Anna S.  " as the name and presses "Save"
- **THEN** the name field and the header show "Anna S." without a reload, and a translated success message is shown
- **AND** the request carried both the name and the time zone

#### Scenario: web-profile/R2-S3 Success message goes away on the next edit
- **GIVEN** the success message is shown after a save
- **WHEN** the teacher changes the name field, and in a second run the time zone select
- **THEN** in both runs the success message is no longer shown

#### Scenario: web-profile/R2-S4 An empty name is not sent
- **WHEN** the name field is emptied and "Save" is pressed
- **THEN** no request is sent

#### Scenario: web-profile/R2-S5 Values the backend rejects
- **GIVEN** the backend answers a save with status 400 and code `VALIDATION_FAILED`
- **WHEN** the teacher presses "Save"
- **THEN** a translated message asks to check the name and the time zone
- **AND** the fields keep what the teacher typed, the header keeps the old name, and the form can be submitted again

#### Scenario: web-profile/R2-S6 Save that fails for another reason
- **GIVEN** a save fails with a network error, a 500, or a 403 with code `CSRF_INVALID`, one at a time
- **WHEN** the teacher presses "Save"
- **THEN** each time a translated error is shown, the login page is not shown, the header keeps the old name, and the form can be submitted again

#### Scenario: web-profile/R2-S7 Stored time zone the browser does not list
- **GIVEN** a logged-in teacher with stored time zone "Europe/Kiev", and a browser whose list of time zones (stubbed in the test) does not contain it
- **WHEN** `/profile` is rendered and the teacher changes only the name and presses "Save"
- **THEN** the select has "Europe/Kiev" selected, and the request carries time zone "Europe/Kiev"

#### Scenario: web-profile/R2-S8 A second press while saving sends nothing
- **GIVEN** a save request is still running
- **WHEN** "Save" is pressed again
- **THEN** only one request is sent

### Requirement: R3 Change password

The password form SHALL ask for the current password and for the new password twice,
with a "Change password" button. Before sending it SHALL check that the new passwords
match and have 8 characters or more and 72 bytes or fewer. On success the user SHALL
stay logged in, the fields SHALL be emptied, and a translated message SHALL say that
other devices were logged out. The forms keep their own values and messages; while one
is sending, the other's button SHALL be disabled.

#### Scenario: web-profile/R3-S1 Password change keeps the user logged in
- **GIVEN** a logged-in teacher with password "teacher-pass-1" on `/profile`
- **WHEN** the teacher enters "teacher-pass-1" as the current password and "teacher-pass-2" twice as the new one, and presses "Change password"
- **THEN** the profile page stays, and a profile save made right after it succeeds
- **AND** after logging out the teacher can log in with "teacher-pass-2" and not with "teacher-pass-1"

#### Scenario: web-profile/R3-S2 Success empties the fields
- **GIVEN** the backend answers a password change with status 204
- **WHEN** the form is submitted with a current password and a valid new password entered twice
- **THEN** the request carried the current and the new password, the three fields are empty, and a translated success message says that the password was changed and other devices were logged out
- **AND** the message goes away when a password field is edited

#### Scenario: web-profile/R3-S3 New passwords that differ are not sent
- **WHEN** the two new password fields differ and the form is submitted
- **THEN** a translated "passwords do not match" message is shown and no request is sent

#### Scenario: web-profile/R3-S4 A short new password is not sent
- **WHEN** a 7-character new password is entered twice and the form is submitted
- **THEN** a translated "at least 8 characters" message is shown and no request is sent

#### Scenario: web-profile/R3-S5 A new password over 72 bytes is not sent
- **WHEN** a new password of 40 Cyrillic letters (80 bytes) is entered twice and the form is submitted
- **THEN** a translated "password is too long" message is shown and no request is sent

#### Scenario: web-profile/R3-S6 Wrong current password
- **GIVEN** the backend answers with status 400 and code `WRONG_PASSWORD`
- **WHEN** the form is submitted
- **THEN** the translated message for `WRONG_PASSWORD` is shown, the fields keep their values, and the form can be submitted again

#### Scenario: web-profile/R3-S7 New password the backend rejects
- **GIVEN** the backend answers with status 400 and code `VALIDATION_FAILED`
- **WHEN** the form is submitted
- **THEN** a translated message asks to check the passwords, without saying which one, and the form can be submitted again

#### Scenario: web-profile/R3-S8 Password change that fails for another reason
- **GIVEN** a password change fails with a network error, a 500, or a 403 with code `CSRF_INVALID`, one at a time
- **WHEN** the form is submitted
- **THEN** each time a translated error is shown, the login page is not shown, the fields keep their values, and the form can be submitted again

#### Scenario: web-profile/R3-S9 A second press while changing sends nothing
- **GIVEN** a password change request is still running
- **WHEN** "Change password" is pressed again
- **THEN** only one request is sent

#### Scenario: web-profile/R3-S10 The two forms do not affect each other
- **GIVEN** the password form shows the `WRONG_PASSWORD` message and holds typed passwords
- **WHEN** the teacher changes the name and presses "Save"
- **THEN** the save request carries only the name and the time zone, the profile form shows its success message, and the password form keeps its message and its values
- **AND** editing a password field afterwards removes the password form's message and leaves the profile form's success message

#### Scenario: web-profile/R3-S11 One form waits while the other is sending
- **GIVEN** a password change request is still running
- **WHEN** the teacher looks at the profile form, and in a second run a save is running and the teacher looks at the password form
- **THEN** in both runs the other form's button is disabled until the request ends, and enabled after it

### Requirement: R4 Session ended on the profile page

When a request from either form gets 401, the web client SHALL show the login page with
the translated "your session has ended" message (web-auth R6). After the user logs in
again without reloading, `/profile` SHALL be shown, whatever the user's role.

#### Scenario: web-profile/R4-S1 A profile save with an ended session
- **GIVEN** a logged-in teacher on `/profile`, and the backend answers a save with 401 and code `UNAUTHENTICATED`
- **WHEN** the teacher presses "Save"
- **THEN** the login page is shown with the translated "your session has ended" message

#### Scenario: web-profile/R4-S2 A password change with an ended session
- **GIVEN** a logged-in teacher on `/profile`, and the backend answers a password change with 401 and code `UNAUTHENTICATED`
- **WHEN** the teacher presses "Change password"
- **THEN** the login page is shown with the translated "your session has ended" message

#### Scenario: web-profile/R4-S3 After logging in again the profile is shown
- **GIVEN** the session ended on `/profile` and the login page is shown
- **WHEN** a student logs in on that page
- **THEN** `/profile` is shown with the student's values
