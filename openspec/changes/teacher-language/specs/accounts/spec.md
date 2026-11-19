# Spec Delta

## ADDED Requirements

### Requirement: R13 A teacher's native language is enforced by the database
The database SHALL enforce who has a native language: none for an admin, one from the
language list (languages R1) for every student and every teacher. The migration SHALL give
existing teachers `en` and leave students' and admins' values as they are.

#### Scenario: accounts/R13-S1 The database enforces who has a language
- **GIVEN** the migrated database
- **WHEN** rows are inserted directly for a teacher with no language, a student with no language, an admin with the language "en", a teacher with the language "xx", and then for a teacher with "en", a student with "en" and an admin with no language
- **THEN** the first four inserts are rejected and the last three succeed

## MODIFIED Requirements

### Requirement: R5 Admin creates teacher accounts
An admin SHALL be able to create a teacher account with name, email, time zone and a
native language. The native language is required: a code from the language list
(languages R1) with whitespace around it ignored. Missing, `null` or blank is `REQUIRED`,
an array, object, number or boolean `INVALID_TYPE`, any other value not in the list
`INVALID_FORMAT`; it is checked together with the other fields, so all failing validation fields are
reported at once (a JSON type error stops at the first field, as in api-errors). The account has no password yet; the response SHALL contain a single-use
set-password link and does not return the language. Emails MUST be unique across all
accounts, case-insensitive.

#### Scenario: accounts/R5-S1 Admin creates a teacher
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a new email and the native language " pl ", and then lists teachers
- **THEN** the response is 201 with the teacher's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, and has no `nativeLanguage` property, the teacher's list item has `nativeLanguage` "pl", and a login before the password is set gets 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R5-S2 Duplicate email is rejected
- **GIVEN** an account with email "anna@example.com"
- **WHEN** the admin creates a teacher with email "ANNA@example.com"
- **THEN** the response is 409 and no account is created

#### Scenario: accounts/R5-S3 Link from teacher creation sets the password
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher and the returned set-password link is used to set the password "new-password-1"
- **THEN** the creation returns 201, setting the password returns 200, and the teacher logs in with "new-password-1"

#### Scenario: accounts/R5-S4 Creation needs a listed native language
- **GIVEN** a logged-in admin
- **WHEN** the admin creates teachers with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn: no native language; `null`; ""; "  "; "xx"; "SR"; "srp"
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first four and `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]` for the others, no body contains "xx" or "srp", and no account is created

#### Scenario: accounts/R5-S5 All failing fields are reported together
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a blank name, a new email, the time zone "Europe/Berlin" and the native language "xx"
- **THEN** the response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "nativeLanguage", code: "INVALID_FORMAT"}]`

#### Scenario: accounts/R5-S6 A wrong JSON type is reported
- **GIVEN** a logged-in admin
- **WHEN** the admin creates teachers with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn, the native language `["sr"]`, `{"code": "sr"}`, `5` and `true`
- **THEN** all four responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "INVALID_TYPE"}]`, and no account is created

### Requirement: R9 Profile
A logged-in user SHALL be able to change their name and time zone, and change their
password by giving the current one. The time zone MUST be a valid IANA zone name.
A teacher MAY also change their native language through `PATCH /api/me`. The field is
optional: a request without it keeps the stored value. When present for a teacher it has
the rules of creation (R5): whitespace around it is ignored; `null`, "" or a blank string
is `REQUIRED`, an array, object, number or boolean `INVALID_TYPE`, a code not in the list
`INVALID_FORMAT`; it is checked with name and time zone and all failing validation fields are
reported at once (a JSON type error stops at the first field, as in api-errors), and a failed request changes nothing. For a student or an admin the field
is ignored whatever its value or type: no error, the stored value does not change. A
teacher's change never touches another account's language.
A successful password change MUST end the account's other sessions and invalidate its
unused password links; the session that made the change stays valid under a new session
id, so a copy of the old id stops working. A password change
that races a revocation MUST lose: it changes nothing and the session gets 401.

#### Scenario: accounts/R9-S1 Update name and time zone
- **GIVEN** a logged-in student
- **WHEN** the student sets name "Ivan P." and time zone "Asia/Dubai"
- **THEN** the current-user endpoint returns the new name and time zone

#### Scenario: accounts/R9-S2 Invalid time zone is rejected
- **GIVEN** a logged-in student
- **WHEN** the student sets time zone "Mars/Olympus"
- **THEN** the response is 400 and the time zone does not change

#### Scenario: accounts/R9-S3 Password change needs the current password
- **GIVEN** a logged-in teacher
- **WHEN** the teacher changes the password giving a wrong current password
- **THEN** the response is 400 and the old password still works

#### Scenario: accounts/R9-S4 Password change ends other sessions
- **GIVEN** a teacher logged in on two devices
- **WHEN** the teacher changes the password on the first device, giving the correct current password
- **THEN** the response is 204, the second device's session gets 401, the first device's session still works, and the teacher logs in with the new password

#### Scenario: accounts/R9-S5 Password change invalidates unused links
- **GIVEN** a logged-in teacher with an unused reset link
- **WHEN** the teacher changes the password, and the link is then used
- **THEN** using the link returns 410 and the new password still works

#### Scenario: accounts/R9-S6 Password change from an outdated session changes nothing
- **GIVEN** a teacher whose session version was raised after the session's check, so the change holds an older version
- **WHEN** the password change runs with that older version
- **THEN** it is rejected with 401, the old password still works, and the session version does not change

#### Scenario: accounts/R9-S7 Password change replaces the session id
- **GIVEN** a logged-in teacher and a copy of the same session cookie
- **WHEN** the teacher changes the password
- **THEN** the copy gets 401 and the teacher's client, with the cookie from the response, gets 200

#### Scenario: accounts/R9-S8 An over-long current password counts as wrong
- **GIVEN** a logged-in teacher
- **WHEN** the teacher changes the password giving a current password of 40 Cyrillic characters (80 bytes)
- **THEN** the response is 400 with code `WRONG_PASSWORD` and the old password still works

#### Scenario: accounts/R9-S9 A parallel request cannot undo the session replacement
- **GIVEN** a logged-in teacher and a parallel request that loaded the session before the password change
- **WHEN** the teacher changes the password, and the parallel request then saves its copy of the session
- **THEN** the teacher's client, with the cookie from the response, gets 200 and the old session cookie gets 401

#### Scenario: accounts/R9-S10 An over-long ASCII current password counts as wrong
- **GIVEN** a logged-in teacher
- **WHEN** the teacher changes the password giving a current password of 73 ASCII characters
- **THEN** the response is 400 with code `WRONG_PASSWORD` and the old password still works

#### Scenario: accounts/R9-S11 A name of only Unicode spaces is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher sets the name to a single U+2003 space
- **THEN** the response is 400 and the name does not change

#### Scenario: accounts/R9-S12 A teacher changes their native language
- **GIVEN** a logged-in teacher with native language "en"
- **WHEN** the teacher updates the profile with the name "Anna S.", the time zone "Asia/Dubai" and the native language " ru ", and then reads the current user
- **THEN** the update returns 200 with `nativeLanguage` "ru", and the current user shows "ru", "Anna S." and "Asia/Dubai"

#### Scenario: accounts/R9-S13 Omitting the native language keeps it
- **GIVEN** a logged-in teacher with native language "pl"
- **WHEN** the teacher updates the profile with a new name and time zone and no native language
- **THEN** the response is 200 with `nativeLanguage` "pl", and the new name and time zone are saved

#### Scenario: accounts/R9-S14 A teacher's invalid language changes nothing
- **GIVEN** a logged-in teacher with the name "Anna" and native language "en"
- **WHEN** the teacher updates the profile with a valid new name and time zone and, in turn, the native language: `null`; ""; "  "; "xx"; "SR"; `["sr"]`; `{"code": "sr"}`; `5`; `true`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first three, `INVALID_FORMAT` for "xx" and "SR", and `INVALID_TYPE` for the last four, and the name is still "Anna" and the native language "en"

#### Scenario: accounts/R9-S15 All failing fields are reported together
- **GIVEN** a logged-in teacher
- **WHEN** the teacher updates the profile with a blank name, a valid time zone and the native language "xx"
- **THEN** the response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "nativeLanguage", code: "INVALID_FORMAT"}]`

#### Scenario: accounts/R9-S16 A student's or an admin's value is ignored
- **GIVEN** a logged-in student with native language "ru" and a logged-in admin
- **WHEN** each updates the profile with a valid new name and time zone and, in turn, the native language: none; `null`; "xx"; "de"; `["de"]`; `{"code": "de"}`
- **THEN** all twelve responses are 200, the student's show `nativeLanguage` "ru", the admin's have no `nativeLanguage` property, and the new names are saved

#### Scenario: accounts/R9-S17 A teacher's change does not touch other accounts
- **GIVEN** a logged-in teacher with native language "en", their student with native language "sr", another teacher with native language "fr" and an admin
- **WHEN** the teacher updates the profile with the native language "de"
- **THEN** the response is 200 with the native language "de", the teacher has "de", and the student, the other teacher and the admin still have "sr", "fr" and no language

### Requirement: R11 Account fields have one rule
Name, email, time zone and a teacher's native language SHALL follow the same rules wherever an account is created or
changed. Whitespace around a name or email SHALL be ignored before they are checked and
stored, including invisible characters around a name. The native language has the value rule of R5 in
creation and in the profile (R9); the only difference is that the profile may omit it.
A name MUST contain a letter or
digit and MUST NOT contain line breaks, control or text-direction characters.

#### Scenario: accounts/R11-S1 Limits apply to teacher creation
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a name of 201 characters, an email of 321 characters, a blank email, a blank time zone, no time zone, and a name of only "\u00A0"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: accounts/R11-S2 Limits apply to the profile
- **GIVEN** a logged-in teacher
- **WHEN** the teacher updates the profile with a name of 201 characters, a blank time zone, no time zone, and a name of only "\u00A0"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and the profile is unchanged

#### Scenario: accounts/R11-S3 Whitespace around values is ignored
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin creates a teacher with a 199-character name followed by two spaces and email " anna@example.com ", and the teacher updates the profile with a 199-character name followed by two spaces
- **THEN** both responses are successful, and the stored names and email have no surrounding whitespace

#### Scenario: accounts/R11-S4 Invisible characters around a name are removed
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name "\u00A0Анна\u200B\u202F"
- **THEN** the response is 201 and the stored name is "Анна"

#### Scenario: accounts/R11-S5 Names that only look filled or hide characters are rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with each of the names "\u3164", "\u2800", "\u034F", "Anna\u202Exyz" and "A\nB"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: accounts/R11-S6 The native language has one rule in creation and in the profile
- **GIVEN** a logged-in admin and a logged-in teacher with native language "en"
- **WHEN** the admin creates teachers and the teacher updates the profile, each with the native language "xx", then "SR", then "  ", then `["sr"]`, and otherwise valid fields
- **THEN** the creation and the profile responses are 400 with the same `errors` for each value (`INVALID_FORMAT`, `INVALID_FORMAT`, `REQUIRED`, `INVALID_TYPE`), and nothing is created or changed

### Requirement: R12 Teacher list shows whether a password is set
Each item of the admin's teacher list SHALL have the teacher's `nativeLanguage` and a boolean `hasPassword`: `false` until
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
