# Spec Delta

## MODIFIED Requirements

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
