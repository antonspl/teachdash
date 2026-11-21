# api-errors Specification

## Purpose
Defines the field-level details that API problem responses carry, so that a client can
show each error next to the field it belongs to, and how a malformed path id is answered.

## Requirements

### Requirement: R1 Validation errors name each failing field
A 400 `VALIDATION_FAILED` response SHALL carry `errors`: one `{field, code}` per failing
field, sorted as strings by field, then code. `field` is the dotted body path (`[i]` for
list items) or the query parameter name. Several broken rules give the first of
`REQUIRED`, `TOO_SHORT`/`TOO_LONG`, `INVALID_FORMAT`/`UNSUPPORTED_COURSE_LANGUAGE`/`UNKNOWN_TYPE`.
NUL is `INVALID_FORMAT`, a wrong type `INVALID_TYPE`; in a body these stop reading, so
only that field is listed. Rejected values MUST NOT be in the response.

#### Scenario: api-errors/R1-S1 Missing and blank fields are required
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name "   ", no email and the time zone "Europe/Berlin", and once more with the name "Anna", the email `null` and a blank time zone
- **THEN** both responses are 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "email", code: "REQUIRED"}, {field: "name", code: "REQUIRED"}]` and `[{field: "email", code: "REQUIRED"}, {field: "timeZone", code: "REQUIRED"}]`, and no account is created

#### Scenario: api-errors/R1-S2 Too-long values in Cyrillic and Latin
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin creates a teacher with a name of 201 Cyrillic letters ("Я") and an email of 321 characters, the teacher updates the profile with a name of 201 Latin letters ("a"), and someone logs in with an email of 321 characters
- **THEN** the first response is 400 with `errors` exactly `[{field: "email", code: "TOO_LONG"}, {field: "name", code: "TOO_LONG"}]`, the second and third are 400 with `errors` exactly `[{field: "name", code: "TOO_LONG"}]` and `[{field: "email", code: "TOO_LONG"}]`, and nothing is created or changed and no session is started

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
- **WHEN** the client previews with a blank token, joins through the link with the name "", the email "anna@example.com", the time zone "Europe/Moscow" and the password "short", and joins with a blank token, the name "Анна", the email "anna@", no time zone and the password "password-1"
- **THEN** the preview response is 400 with `errors` exactly `[{field: "token", code: "REQUIRED"}]`, the first join response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "password", code: "TOO_SHORT"}]`, the second is 400 with `errors` exactly `[{field: "email", code: "INVALID_FORMAT"}, {field: "timeZone", code: "REQUIRED"}, {field: "token", code: "REQUIRED"}]`, and no account is created

#### Scenario: api-errors/R1-S6 A NUL character names its field
- **GIVEN** a logged-in teacher
- **WHEN** someone logs in with an email containing a NUL character, and the teacher registers a student with the name "Ан\u0000на", a blank email and the time zone "Europe/Moscow"
- **THEN** both responses are 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "email", code: "INVALID_FORMAT"}]` and `[{field: "name", code: "INVALID_FORMAT"}]`, the second body does not contain "Ан", no session is started and no student is created

#### Scenario: api-errors/R1-S7 A wrong JSON type is a validation error
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name `["Anna"]`, the email "anna@example.com" and the time zone "Europe/Berlin", and once more with the name "Анна", the email `{"value": "anna@example.com"}` and the time zone "Europe/Berlin"
- **THEN** both responses are 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "name", code: "INVALID_TYPE"}]` and `[{field: "email", code: "INVALID_TYPE"}]`, neither body contains "Anna" or "anna@example.com", and no account is created

#### Scenario: api-errors/R1-S8 Broken JSON stays a malformed request
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the body `{"name":`, with the body `[]`, and with no body
- **THEN** each response is 400 with code `MALFORMED_REQUEST` and no `errors` property, and no account is created

#### Scenario: api-errors/R1-S9 Query parameters are named
- **GIVEN** an endpoint with a required numeric query parameter `page` and an optional text query parameter `q` of at most 10 characters, and a logged-in teacher
- **WHEN** the teacher calls it with no `page`, with `page=abc`, and with `page=1` and `q` of 11 Cyrillic letters ("Ж")
- **THEN** each response is 400 with code `VALIDATION_FAILED` and `errors` is exactly `[{field: "page", code: "REQUIRED"}]`, `[{field: "page", code: "INVALID_TYPE"}]` and `[{field: "q", code: "TOO_LONG"}]`, and no body contains "abc" or "Ж"

#### Scenario: api-errors/R1-S10 A course in an unsupported language is rejected with its own code
- **GIVEN** a logged-in teacher, and the language list with "de" marked `content` false
- **WHEN** the teacher creates a course with the title "Course", the instruction language "de" and the target language "xx"
- **THEN** the response is 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "instructionLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}, {field: "targetLanguage", code: "INVALID_FORMAT"}]`, the body does not contain "xx", and no course is created

#### Scenario: api-errors/R1-S11 An unregistered item type is rejected with its own code
- **GIVEN** a logged-in teacher with a part, and no item type `poll` registered
- **WHEN** the teacher adds an item with the type "poll" and the payload `{"text": "Hi"}`
- **THEN** the response is 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "type", code: "UNKNOWN_TYPE"}]`, the body does not contain "poll" as a value, and no item is created

### Requirement: R2 Single-field rejections name the field
An `EMAIL_TAKEN`, `WRONG_PASSWORD` or `TITLE_TAKEN` response SHALL keep its status and
code and carry `errors` with one entry whose code is the response code: field `email`
for `EMAIL_TAKEN`, `currentPassword` for `WRONG_PASSWORD`, `title` for `TITLE_TAKEN`.
Responses with any code other than `VALIDATION_FAILED`, `EMAIL_TAKEN`, `WRONG_PASSWORD`
and `TITLE_TAKEN` MUST NOT carry `errors`.

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

#### Scenario: api-errors/R2-S6 Taken course title
- **GIVEN** a logged-in teacher with a course titled "Грамматика"
- **WHEN** the teacher creates a course titled " ГРАММАТИКА " with the instruction language "ru"
- **THEN** the response is 409 with code `TITLE_TAKEN` and `errors` exactly `[{field: "title", code: "TITLE_TAKEN"}]`

### Requirement: R3 A malformed path id is not found
On every endpoint the caller is allowed to use, a path id that cannot be read as an id
SHALL return 404 with code `NOT_FOUND` and no `errors`, the same as an id that does not
exist, and MUST change nothing. A caller without access to the endpoint still gets 401
or 403 first.

#### Scenario: api-errors/R3-S1 Malformed id on admin endpoints
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin deactivates, reactivates and issues a reset link for the id "not-a-uuid", and the teacher deactivates the id "not-a-uuid"
- **THEN** each of the admin's responses is 404 with code `NOT_FOUND` and no `errors` property, and the teacher's response is 403

#### Scenario: api-errors/R3-S2 Malformed id on enrolment endpoints stays not found
- **GIVEN** a logged-in teacher and a logged-in admin
- **WHEN** the teacher requests the details of the id "not-a-uuid", and the admin lists the students of the id "not-a-uuid"
- **THEN** each response is 404 with code `NOT_FOUND` and no `errors` property
