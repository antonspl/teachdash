# Spec Delta

## ADDED Requirements

### Requirement: R13 A teacher has a native language
Every teacher SHALL have a native language from the language list (languages R1), the
same field as a student's. Creating a teacher (R5) and a teacher's own `PATCH /api/me`
(R9) SHALL require `nativeLanguage`, with the validation rules of student-enrolment R13.
For a student or an admin, `PATCH /api/me` SHALL ignore the field. An admin has none.
The database SHALL enforce this, and the migration SHALL give existing teachers `en`.

#### Scenario: accounts/R13-S1 Admin creates a teacher with a native language
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a name, a new email, a time zone and the native language " pl ", and then lists teachers
- **THEN** the creation returns 201, and the list item for the teacher has `nativeLanguage` "pl"

#### Scenario: accounts/R13-S2 Creation needs a listed native language
- **GIVEN** a logged-in admin
- **WHEN** the admin creates teachers with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn: no native language; `null`; ""; "  "; "xx"; "SR"; "srp"
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first four and `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]` for the others, no body contains "xx" or "srp", and no account is created

#### Scenario: accounts/R13-S3 All failing fields are reported together
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a blank name, a new email, the time zone "Europe/Berlin" and the native language "xx"
- **THEN** the response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "nativeLanguage", code: "INVALID_FORMAT"}]`

#### Scenario: accounts/R13-S4 A wrong JSON type is reported
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin creates a teacher with the name "Anna", a new email, the time zone "Europe/Berlin" and the native language `["sr"]`, and the teacher updates the profile with a valid name and time zone and the native language `{"code": "sr"}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "INVALID_TYPE"}]`, and no account is created

#### Scenario: accounts/R13-S5 A teacher changes their native language
- **GIVEN** a logged-in teacher with native language "en"
- **WHEN** the teacher updates the profile with the name "Anna S.", the time zone "Asia/Dubai" and the native language " ru ", and then reads the current user
- **THEN** the update returns 200 with `nativeLanguage` "ru", and the current user shows "ru", "Anna S." and "Asia/Dubai"

#### Scenario: accounts/R13-S6 Invalid values change nothing
- **GIVEN** a logged-in teacher with the name "Anna" and native language "en"
- **WHEN** the teacher updates the profile with a valid name and time zone and, in turn: no native language; `null`; ""; "xx"; "SR"
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first three and `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]` for the others, and the native language is still "en"

#### Scenario: accounts/R13-S7 Students and admins are not affected by the field
- **GIVEN** a logged-in student with native language "ru" and a logged-in admin
- **WHEN** each updates the profile with a valid name and time zone and no native language, and then with the native language "xx" and a new name
- **THEN** all four responses are 200, the student's responses show `nativeLanguage` "ru", the admin's responses have no `nativeLanguage` property, and the new names are saved

#### Scenario: accounts/R13-S8 The database enforces who has a language
- **GIVEN** the migrated database
- **WHEN** rows are inserted directly for a teacher with no language, a student with no language, an admin with the language "en", a teacher with the language "xx", and then for a teacher with "en", a student with "en" and an admin with no language
- **THEN** the first four inserts are rejected and the last three succeed

#### Scenario: accounts/R13-S9 The migration gives existing teachers English
- **GIVEN** a database migrated to the version before the one that requires the language, holding a teacher and an admin, both with no language, and a student with the language "sr"
- **WHEN** the last migration runs
- **THEN** the teacher has the language "en", the student still has "sr", and the admin has none

## MODIFIED Requirements

### Requirement: R12 Teacher list shows whether a password is set
Each item of the admin's teacher list SHALL have the teacher's `nativeLanguage` (R13) and a boolean `hasPassword`: `false` until
the teacher sets a password, `true` afterwards. It does not depend on whether the teacher
is active. The list MUST NOT return the password hash.

#### Scenario: accounts/R12-S1 Admin sees no password set
- **GIVEN** a logged-in admin and a teacher the admin created, who has not set a password
- **WHEN** the admin lists teachers
- **THEN** the teacher's list item has `hasPassword` equal to `false`

#### Scenario: accounts/R12-S2 Admin sees a password has been set
- **GIVEN** a logged-in admin and a teacher the admin created, who set a password with the set-password link from the creation
- **WHEN** the admin lists teachers
- **THEN** the teacher's list item has `hasPassword` equal to `true`, and its fields are exactly `id`, `name`, `email`, `role`, `timeZone`, `nativeLanguage`, `active` and `hasPassword`

#### Scenario: accounts/R12-S3 Deactivated teachers keep their password status
- **GIVEN** a logged-in admin, a deactivated teacher with a password and a deactivated teacher who never set one
- **WHEN** the admin lists teachers
- **THEN** `hasPassword` is `true` for the first teacher and `false` for the second
