# Spec Delta

## MODIFIED Requirements

### Requirement: R2 Login with email and password
The system SHALL sign in an active account with a matching email and password and
start a session. Email matching MUST be case-insensitive. A failed login MUST return
401 with code `INVALID_CREDENTIALS`, whether the email has no account, the password is
wrong, or the account has no password, and whether or not the account is active. The only
exception is a deactivated account given its correct password, which gets
`ACCOUNT_DEACTIVATED`. A password over 72 bytes is a wrong password, however it is encoded. A JSON string field containing a NUL character
MUST be rejected with 400 `VALIDATION_FAILED` on every endpoint.

#### Scenario: accounts/R2-S1 Valid credentials start a session
- **GIVEN** an active teacher account
- **WHEN** the teacher logs in with the correct email and password
- **THEN** the response is 200 and the current-user endpoint returns the teacher's id, name, email, role and time zone

#### Scenario: accounts/R2-S2 Wrong password is rejected
- **GIVEN** an active teacher account
- **WHEN** someone logs in with the teacher's email and a wrong password
- **THEN** the response is 401 with code `INVALID_CREDENTIALS` and no session is started

#### Scenario: accounts/R2-S3 Unknown email is reported
- **WHEN** someone logs in with an email that has no account
- **THEN** the response is 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R2-S4 Email is case-insensitive
- **GIVEN** an account with email "anna@example.com"
- **WHEN** the user logs in with "Anna@Example.COM" and the correct password
- **THEN** the response is 200

#### Scenario: accounts/R2-S5 Deactivated account cannot log in
- **GIVEN** a deactivated teacher account
- **WHEN** the teacher logs in with the correct email and password
- **THEN** the response is 401 with code `ACCOUNT_DEACTIVATED`

#### Scenario: accounts/R2-S6 NUL character in input is rejected
- **WHEN** someone logs in with an email containing a NUL character
- **THEN** the response is 400 with code `VALIDATION_FAILED`

#### Scenario: accounts/R2-S7 Deactivated account with no password
- **GIVEN** a deactivated teacher who never set a password
- **WHEN** someone logs in with the teacher's email and any password
- **THEN** the response is 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R2-S8 An over-long password is a wrong password
- **GIVEN** an active teacher
- **WHEN** someone logs in with the teacher's email and a password of 73 ASCII characters
- **THEN** the response is 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R2-S9 Active account with no password
- **GIVEN** an active teacher who has not set a password
- **WHEN** someone logs in with the teacher's email and any password
- **THEN** the response is 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R2-S10 Deactivated account with a wrong password
- **GIVEN** a deactivated teacher account with a password
- **WHEN** someone logs in with the teacher's email and a wrong password
- **THEN** the response is 401 with code `INVALID_CREDENTIALS`

### Requirement: R5 Admin creates teacher accounts
An admin SHALL be able to create a teacher account with name, email and time zone.
The account has no password yet; the response SHALL contain a single-use set-password
link. Emails MUST be unique across all accounts, case-insensitive.

#### Scenario: accounts/R5-S1 Admin creates a teacher
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a new email
- **THEN** the response is 201 with the teacher's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, and a login before the password is set gets 401 with code `INVALID_CREDENTIALS`

#### Scenario: accounts/R5-S2 Duplicate email is rejected
- **GIVEN** an account with email "anna@example.com"
- **WHEN** the admin creates a teacher with email "ANNA@example.com"
- **THEN** the response is 409 and no account is created

#### Scenario: accounts/R5-S3 Link from teacher creation sets the password
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher and the returned set-password link is used to set the password "new-password-1"
- **THEN** the creation returns 201, setting the password returns 200, and the teacher logs in with "new-password-1"

## ADDED Requirements

### Requirement: R10 Login timing does not reveal accounts
Response time MUST NOT reveal whether an email has an account or whether the account has
a password. Every login request that passes validation SHALL run exactly one password hash
check, at the cost used for new passwords, even when there is no stored hash to check.

#### Scenario: accounts/R10-S1 A login with no real hash to check still runs one hash check
- **GIVEN** an active teacher with a password, an active teacher with no password and a deactivated teacher with no password
- **WHEN** someone logs in with an email that has no account, with each no-password teacher's email and any password, and with the first teacher's email and a password of 73 ASCII characters
- **THEN** each login runs exactly one password hash check, at the cost used for new passwords
