# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Change name and time zone

The profile form SHALL have a name field and a time zone select, filled with the user's
current values, and a "Save" button. Both fields are required. For a teacher the form
SHALL also have a required native language select, filled with the stored language. Its
options SHALL be the codes returned by `GET /api/languages`, shown with names from the
translation resources (a code with no name is shown as the code itself), and the select
SHALL always include the stored language. If the list cannot be loaded, a translated error
with a "Try again" button SHALL be shown next to the select, and the rest of the form SHALL
keep working. The request SHALL carry the language for a teacher and SHALL NOT for a
student or an admin, whose form has no such select. On success the form, the header and
the app's current-user state SHALL show the values the backend returned, and a translated success message
SHALL stay until the next edit of the form. If the save fails, a translated message
SHALL say so and the header SHALL not change. The time zone select SHALL always include the
user's stored time zone.

#### Scenario: web-profile/R2-S1 Saved profile survives a reload
- **GIVEN** a logged-in teacher "Anna" on `/profile`
- **WHEN** the teacher sets name "Anna S." and time zone "Asia/Dubai", presses "Save" and reloads the page
- **THEN** the header shows "Anna S.", the name field holds "Anna S." and the select has "Asia/Dubai" selected

#### Scenario: web-profile/R2-S2 Form and header show what the backend returned
- **GIVEN** a logged-in teacher on `/profile`, and the backend answers a save with name "Anna S." and time zone "Asia/Dubai"
- **WHEN** the teacher types "  Anna S.  " as the name and presses "Save"
- **THEN** the name field and the header show "Anna S." without a reload, and a translated success message is shown
- **AND** the request carried the name, the time zone and the native language

#### Scenario: web-profile/R2-S3 Success message goes away on the next edit
- **GIVEN** the success message is shown after a save
- **WHEN** the teacher changes the name field, and in a second run the time zone select
- **THEN** in both runs the success message is no longer shown

#### Scenario: web-profile/R2-S4 An empty name is not sent
- **WHEN** the name field is emptied and "Save" is pressed
- **THEN** no request is sent

#### Scenario: web-profile/R2-S5 Values the backend rejects
- **GIVEN** a logged-in teacher on `/profile`, and in a second run a logged-in student, and the backend answers a save with status 400 and code `VALIDATION_FAILED`
- **WHEN** the user presses "Save"
- **THEN** for the teacher a translated message asks to check the name, the time zone and the native language, and for the student it asks to check the name and the time zone
- **AND** the fields keep what the user typed, the header keeps the old name, and the form can be submitted again

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

#### Scenario: web-profile/R2-S9 A teacher changes the native language
- **GIVEN** a logged-in teacher with native language "ru" on `/profile`, and the backend lists the languages "de", "en" and "ru"
- **WHEN** the page is rendered, the teacher selects the language named "German" and presses "Save", and the page is reloaded
- **THEN** before the change the select has the language named "Russian" selected and offers exactly "German", "English" and "Russian"
- **AND** the request carried the native language "de", and after the reload the select has "German" selected

#### Scenario: web-profile/R2-S10 Students and admins have no language select
- **GIVEN** a logged-in student with native language "ru", and a logged-in admin, one at a time
- **WHEN** each opens `/profile`, changes the name and presses "Save"
- **THEN** neither page has a native language select, and each request carried the name and the time zone and no native language

#### Scenario: web-profile/R2-S11 A language the backend rejects
- **GIVEN** a logged-in teacher on `/profile`, and the backend answers a save with status 400, code `VALIDATION_FAILED` and `errors` `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`
- **WHEN** the teacher presses "Save"
- **THEN** the translated message for `INVALID_FORMAT` is shown at the language select (web-client R17), the select keeps the value the teacher chose, and the form can be submitted again

#### Scenario: web-profile/R2-S12 The language list does not load
- **GIVEN** a logged-in teacher with native language "ru" on `/profile`, and loading the languages fails
- **WHEN** the page is rendered, and the teacher changes the name and presses "Save"
- **THEN** a translated error with a "Try again" button is shown, the select offers only the stored language "ru", the rest of the form works, and the request carries the native language "ru"

#### Scenario: web-profile/R2-S13 Try again loads the list
- **GIVEN** a logged-in teacher with native language "ru" on `/profile`, loading the languages fails once and then lists "de", "en" and "ru"
- **WHEN** the page is rendered and the teacher presses "Try again"
- **THEN** the error is gone and the select offers exactly "German", "English" and "Russian", with "Russian" selected

#### Scenario: web-profile/R2-S14 A language with no name shows its code
- **GIVEN** a logged-in teacher with native language "sv" on `/profile`, the backend lists the languages "de" and "sv", and the translation resources have no name for "sv"
- **WHEN** the page is rendered
- **THEN** the select offers "German" and "sv", with "sv" selected

#### Scenario: web-profile/R2-S15 The current-user state gets the new language
- **GIVEN** a logged-in teacher with native language "ru" on `/profile`, the backend lists "de", "en" and "ru" and answers a save with native language "de"
- **WHEN** the teacher selects "German", presses "Save", leaves `/profile` by a link inside the app and comes back, without a page reload
- **THEN** the select has "German" selected, and no new current-user request was made

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
- **GIVEN** the backend answers with status 400, code `WRONG_PASSWORD` and no `errors` (web-client R17)
- **WHEN** the form is submitted
- **THEN** the translated message for `WRONG_PASSWORD` is shown, the fields keep their values, and the form can be submitted again

#### Scenario: web-profile/R3-S7 New password the backend rejects
- **GIVEN** the backend answers with status 400, code `VALIDATION_FAILED` and no `errors` (web-client R17)
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
- **GIVEN** the backend answered the password change with status 400, code `WRONG_PASSWORD` and `errors` `[{field: "currentPassword", code: "WRONG_PASSWORD"}]`, so the password form shows the `WRONG_PASSWORD` message under the current password field, and the form holds typed passwords
- **WHEN** the teacher changes the name and presses "Save"
- **THEN** the save request carries only the name, the time zone and the native language, the profile form shows its success message, and the password form keeps its message and its values
- **AND** editing the new password field afterwards keeps the `WRONG_PASSWORD` message, editing the current password field removes it, and both leave the profile form's success message

#### Scenario: web-profile/R3-S11 One form waits while the other is sending
- **GIVEN** a password change request is still running
- **WHEN** the teacher looks at the profile form, and in a second run a save is running and the teacher looks at the password form
- **THEN** in both runs the other form's button is disabled until the request ends, and enabled after it
