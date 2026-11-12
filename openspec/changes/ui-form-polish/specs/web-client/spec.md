# Spec Delta

## ADDED Requirements

### Requirement: R13 Select arrow

Every select field of the web client SHALL show the app's own dropdown arrow in place of
the browser's. The gap between the arrow and the inner edge of the field's right border
SHALL equal the gap between the text and the inner edge of the left border, and the
selected text SHALL be cut off before the arrow when it does not fit. A click or tap on
the arrow SHALL reach the field, as a click on its text does.

#### Scenario: web-client/R13-S1 Arrow is inset like the text
- **GIVEN** a logged-in admin, on `/profile` and then in the "Add teacher" dialog
- **WHEN** the time zone select is shown
- **THEN** each time the browser's own arrow is switched off and the app's arrow is visible inside the field
- **AND** the distance from the arrow's right edge to the inner edge of the right border equals the select's left padding, within 1 pixel

#### Scenario: web-client/R13-S2 Long option does not run under the arrow
- **GIVEN** a logged-in teacher with time zone "America/Argentina/ComodRivadavia" and a 360-pixel-wide screen
- **WHEN** `/profile` is opened
- **THEN** the select is as wide as the name field, its text area ends before the arrow's left edge, and text that does not fit is cut off with an ellipsis
- **AND** the page has no horizontal scroll

#### Scenario: web-client/R13-S3 Click on the arrow reaches the select
- **GIVEN** a logged-in teacher on `/profile`
- **WHEN** the teacher clicks in the middle of the arrow
- **THEN** the time zone select has the focus

#### Scenario: web-client/R13-S4 Disabled select keeps the arrow
- **GIVEN** a logged-in teacher on `/profile`, and the arrow's position is noted
- **WHEN** a profile save is still running, so the time zone select is disabled
- **THEN** the arrow is visible at the same position

### Requirement: R14 New-password hints while typing

Every form that asks for a new password twice (the profile password form and the
set-password page) SHALL show a translated hint under the field while the user types: "at
least 8 characters" or "password is too long" under the new password, "passwords do not
match" under the repeat. A hint SHALL go away as soon as its rule is met. An empty field
SHALL show no hint. The submit button SHALL stay enabled; submitting with a broken rule
sends nothing.

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
- **GIVEN** the profile password form with a current password entered, and in a second run the set-password page, each with "teacher-pass-2" as the new password, "teacher-pass-3" as the repeat and the "passwords do not match" hint shown
- **WHEN** the user looks at the submit button and then presses it
- **THEN** in both runs the button is enabled, no request is sent and the hint stays

#### Scenario: web-client/R14-S8 Hint belongs to its field
- **GIVEN** the profile password form, and in a second run the set-password page, with the "at least 8 characters" hint shown
- **WHEN** assistive technology reads the new password field
- **THEN** in both runs the field is marked invalid and the hint is its description, and the repeat field is not marked invalid

#### Scenario: web-client/R14-S9 Current password is not checked while typing
- **GIVEN** the profile password form
- **WHEN** the user types "x" in the current password field
- **THEN** no hint is shown under any field and the backend has received no request

#### Scenario: web-client/R14-S10 No hints after a successful change
- **GIVEN** the profile password form with a current password and a valid new password entered twice, and the backend answers the password change with status 204
- **WHEN** the form is submitted
- **THEN** the three fields are empty and no hint is shown

#### Scenario: web-client/R14-S11 A hint replaces an old backend error
- **GIVEN** the set-password page shows the translated message for `LINK_CONFLICT` after a submit
- **WHEN** the user changes the repeat field so that it differs and presses the button
- **THEN** the "passwords do not match" hint is shown, the `LINK_CONFLICT` message is gone and no second request is sent
