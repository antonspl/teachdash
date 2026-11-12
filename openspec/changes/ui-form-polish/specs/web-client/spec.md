# Spec Delta

## ADDED Requirements

### Requirement: R13 Select arrow

Every select field of the web client SHALL show the app's own dropdown arrow in place of
the browser's. The gap between the arrow and the field's right border SHALL equal the
gap between the text and the left border, and the selected text SHALL end before the
arrow. The field SHALL still open the browser's own list of options, by mouse, touch and
keyboard.

#### Scenario: web-client/R13-S1 Arrow is inset like the text
- **GIVEN** a logged-in admin, on `/profile` and then in the "Add teacher" dialog
- **WHEN** the time zone select is shown
- **THEN** each time the browser's own arrow is not drawn and the app's arrow is
- **AND** the gap between the arrow and the right border equals the gap between the selected text and the left border

#### Scenario: web-client/R13-S2 Long option does not run under the arrow
- **GIVEN** a logged-in teacher with time zone "America/Argentina/ComodRivadavia" and a 360-pixel-wide screen
- **WHEN** `/profile` is opened
- **THEN** the selected text ends before the arrow, cut off if it does not fit
- **AND** the page has no horizontal scroll

#### Scenario: web-client/R13-S3 Select still works from the keyboard
- **GIVEN** a logged-in teacher on `/profile` with time zone "UTC"
- **WHEN** the teacher focuses the time zone select with the keyboard and types "Europe/Moscow"
- **THEN** the select shows "Europe/Moscow"

#### Scenario: web-client/R13-S4 Disabled select keeps the arrow
- **GIVEN** a profile save is still running, so the time zone select is disabled
- **WHEN** the teacher looks at the select
- **THEN** the arrow is shown in the same place

### Requirement: R14 New-password hints while typing

Every form that asks for a new password twice (the profile password form and the
set-password page) SHALL show a translated hint under the field while the user types: "at
least 8 characters" or "password is too long" under the new password, "passwords do not
match" under the repeat. A hint SHALL go away as soon as its rule is met. An empty field
SHALL show no hint until the form is submitted. The submit button SHALL stay enabled;
submitting with a broken rule sends nothing.

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
- **GIVEN** the profile password form, and in a second run the set-password page, with the "passwords do not match" hint shown
- **WHEN** the user looks at the submit button and then presses it
- **THEN** in both runs the button is enabled, no request is sent and the hint stays

#### Scenario: web-client/R14-S8 Submitting with an empty repeat
- **GIVEN** the profile password form with a current password, and in a second run the set-password page, with "teacher-pass-2" in the new password field and an empty repeat field
- **WHEN** the form is submitted
- **THEN** in both runs no request is sent and the "passwords do not match" hint is shown under the repeat field
- **AND** after the user types "teacher-pass-2" in the repeat field the hint is gone

#### Scenario: web-client/R14-S9 Hint belongs to its field
- **GIVEN** the profile password form, and in a second run the set-password page, with the "at least 8 characters" hint shown
- **WHEN** assistive technology reads the new password field
- **THEN** in both runs the field is marked invalid and the hint is its description, and the repeat field is not marked invalid

#### Scenario: web-client/R14-S10 Current password is not checked while typing
- **GIVEN** the profile password form
- **WHEN** the user types "x" in the current password field
- **THEN** no hint is shown and no request is sent

#### Scenario: web-client/R14-S11 No hints after a successful change
- **GIVEN** the profile password form, submitted once with an empty repeat field, then corrected, and the backend answers the password change with status 204
- **WHEN** the form is submitted
- **THEN** the three fields are empty and no hint is shown
