# Spec Delta

## MODIFIED Requirements

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
- **GIVEN** the password form shows the `WRONG_PASSWORD` message, under the current password field or as its form-level message (web-client R17), and holds typed passwords
- **WHEN** the teacher changes the name and presses "Save"
- **THEN** the save request carries only the name and the time zone, the profile form shows its success message, and the password form keeps its message and its values
- **AND** editing the current password field afterwards removes the `WRONG_PASSWORD` message, wherever it is shown, and leaves the profile form's success message

#### Scenario: web-profile/R3-S11 One form waits while the other is sending
- **GIVEN** a password change request is still running
- **WHEN** the teacher looks at the profile form, and in a second run a save is running and the teacher looks at the password form
- **THEN** in both runs the other form's button is disabled until the request ends, and enabled after it
