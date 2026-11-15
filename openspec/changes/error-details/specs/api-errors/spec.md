# Spec Delta

## Purpose

Defines the field-level details that API problem responses carry, so that a client can
show each error next to the field it belongs to.

## ADDED Requirements

### Requirement: R1 Validation errors name each failing field
A 400 `VALIDATION_FAILED` response SHALL carry `errors`: a non-empty list with one
`{field, code}` entry per failing field, sorted by field. `field` is the dotted path in
the request body (e.g. `lessons[0].title`). If a field breaks several rules, the code is
the first in this order: `REQUIRED`, `TOO_SHORT` or `TOO_LONG`, `INVALID_FORMAT`. A NUL
character is `INVALID_FORMAT`; a wrong JSON type is `INVALID_TYPE`. The response MUST NOT
contain the rejected values.

#### Scenario: api-errors/R1-S1 Missing and blank fields are required
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name "   ", no email and the time zone "Europe/Berlin"
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` is exactly `[{field: "email", code: "REQUIRED"}, {field: "name", code: "REQUIRED"}]`, and no account is created

#### Scenario: api-errors/R1-S2 Too-long values in Cyrillic and Latin
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin creates a teacher with a name of 201 Cyrillic letters ("Я") and an email of 321 characters, and the teacher updates the profile with a name of 201 Latin letters ("a")
- **THEN** the first response is 400 with `errors` exactly `[{field: "email", code: "TOO_LONG"}, {field: "name", code: "TOO_LONG"}]`, the second is 400 with `errors` exactly `[{field: "name", code: "TOO_LONG"}]`, and nothing is created or changed

#### Scenario: api-errors/R1-S3 Invalid formats are reported without the values
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with the name "ㅤ", the email "anna@" and the time zone "Mars/Olympus"
- **THEN** the response is 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "email", code: "INVALID_FORMAT"}, {field: "name", code: "INVALID_FORMAT"}, {field: "timeZone", code: "INVALID_FORMAT"}]`, the body contains none of "anna@" and "Mars/Olympus", and no student is created

#### Scenario: api-errors/R1-S4 Password rules have separate codes
- **GIVEN** a logged-in teacher with the password "old-password-1"
- **WHEN** the teacher changes the password with current password "old-password-1" and each new password "", "1234567", 73 ASCII characters and 37 Cyrillic characters (74 bytes), and once more with a blank current password and new password "new-password-1"
- **THEN** each response is 400 with code `VALIDATION_FAILED` and `errors` is exactly `[{field: "newPassword", code: "REQUIRED"}]`, `[{field: "newPassword", code: "TOO_SHORT"}]`, `[{field: "newPassword", code: "TOO_LONG"}]`, `[{field: "newPassword", code: "TOO_LONG"}]` and `[{field: "currentPassword", code: "REQUIRED"}]` in that order, and the teacher still logs in with "old-password-1"

#### Scenario: api-errors/R1-S5 Join-link input names the failing fields
- **GIVEN** teacher A with a join link, and a client with a CSRF cookie and no session
- **WHEN** the client previews with a blank token, and joins through the link with the name "", the email "anna@example.com", the time zone "Europe/Moscow" and the password "short"
- **THEN** the preview response is 400 with `errors` exactly `[{field: "token", code: "REQUIRED"}]`, the join response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "password", code: "TOO_SHORT"}]`, and no account is created

#### Scenario: api-errors/R1-S6 A NUL character names its field
- **GIVEN** a logged-in teacher
- **WHEN** someone logs in with an email containing a NUL character, and the teacher registers a student with the name "Ан\u0000на", the email "anna@example.com" and the time zone "Europe/Moscow"
- **THEN** both responses are 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "email", code: "INVALID_FORMAT"}]` and `[{field: "name", code: "INVALID_FORMAT"}]`, no session is started and no student is created

#### Scenario: api-errors/R1-S7 A wrong JSON type is a validation error
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name `["Anna"]`, the email "anna@example.com" and the time zone "Europe/Berlin"
- **THEN** the response is 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "name", code: "INVALID_TYPE"}]`, and no account is created

#### Scenario: api-errors/R1-S8 Broken JSON stays a malformed request
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the body `{"name":`, with the body `[]`, and with no body
- **THEN** each response is 400 with code `MALFORMED_REQUEST` and no `errors` property, and no account is created

### Requirement: R2 Single-field rejections name the field
An `EMAIL_TAKEN` or `WRONG_PASSWORD` response SHALL keep its status and code and carry
`errors` with one entry whose code is the response code: field `email` for
`EMAIL_TAKEN`, `currentPassword` for `WRONG_PASSWORD`. Responses with any other code
MUST NOT carry `errors`.

#### Scenario: api-errors/R2-S1 Taken email at teacher creation
- **GIVEN** a logged-in admin and a teacher with email "t@example.com"
- **WHEN** the admin creates another teacher with the email " T@example.com "
- **THEN** the response is 409 with code `EMAIL_TAKEN` and `errors` exactly `[{field: "email", code: "EMAIL_TAKEN"}]`

#### Scenario: api-errors/R2-S2 Taken email at student registration
- **GIVEN** a logged-in teacher B and an admin with email "a@example.com"
- **WHEN** teacher B registers a student with the name "Анна", the email "a@example.com" and the time zone "Europe/Moscow"
- **THEN** the response is 409 with code `EMAIL_TAKEN` and `errors` exactly `[{field: "email", code: "EMAIL_TAKEN"}]`, and nothing is assigned

#### Scenario: api-errors/R2-S3 Taken email when joining through a link
- **GIVEN** teacher A with a join link, a teacher with email "t@example.com", and a client with a CSRF cookie and no session
- **WHEN** the client joins through the link with the name "Anna", the email "t@example.com", the time zone "Europe/Berlin" and the password "password-1"
- **THEN** the response is 409 with code `EMAIL_TAKEN` and `errors` exactly `[{field: "email", code: "EMAIL_TAKEN"}]`, and no account is created

#### Scenario: api-errors/R2-S4 Wrong current password
- **GIVEN** a logged-in teacher with the password "пароль-старый-1"
- **WHEN** the teacher changes the password with current password "пароль-неверный-1" and new password "new-password-1"
- **THEN** the response is 400 with code `WRONG_PASSWORD` and `errors` exactly `[{field: "currentPassword", code: "WRONG_PASSWORD"}]`, and the old password still works

#### Scenario: api-errors/R2-S5 Other errors carry no field list
- **GIVEN** teacher A with a student assigned to them, and a logged-in teacher A
- **WHEN** someone logs in with a wrong password, teacher A registers the assigned student's email again, and teacher A opens `/api/teacher/students/not-a-uuid`
- **THEN** the responses are 401 `INVALID_CREDENTIALS`, 409 `ALREADY_ENROLLED` and 404 `NOT_FOUND`, and none has an `errors` property
