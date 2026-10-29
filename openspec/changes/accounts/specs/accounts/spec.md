# Spec Delta

## Purpose

Lets admins, teachers and students have accounts with one role each, sign in with
email and password, and manage passwords through single-use links sent by hand.

## ADDED Requirements

### Requirement: R1 First admin from environment
On startup, if no admin account exists and `ADMIN_EMAIL`, `ADMIN_PASSWORD` and
`ADMIN_NAME` are set, the system SHALL create an active admin account with these
values. If an admin already exists, the system MUST NOT create or change any account.

#### Scenario: accounts/R1-S1 First start creates the admin
- **GIVEN** an empty database and the admin variables are set
- **WHEN** the application starts
- **THEN** an admin account with that email exists and can log in with that password

#### Scenario: accounts/R1-S2 Existing admin is not changed
- **GIVEN** an admin account exists with password "old-password"
- **WHEN** the application starts with `ADMIN_PASSWORD` set to a different value
- **THEN** no new account is created and the admin still logs in with "old-password"

### Requirement: R2 Login with email and password
The system SHALL sign in an active account with a matching email and password and
start a session. Email matching MUST be case-insensitive. A failed login MUST return
401 with an error code naming the reason: `UNKNOWN_EMAIL`, `WRONG_PASSWORD`,
`ACCOUNT_DEACTIVATED` or `PASSWORD_NOT_SET`.

#### Scenario: accounts/R2-S1 Valid credentials start a session
- **GIVEN** an active teacher account
- **WHEN** the teacher logs in with the correct email and password
- **THEN** the response is 200 and the current-user endpoint returns the teacher's id, name, email, role and time zone

#### Scenario: accounts/R2-S2 Wrong password is rejected
- **GIVEN** an active teacher account
- **WHEN** someone logs in with the teacher's email and a wrong password
- **THEN** the response is 401 with code `WRONG_PASSWORD` and no session is started

#### Scenario: accounts/R2-S3 Unknown email is reported
- **WHEN** someone logs in with an email that has no account
- **THEN** the response is 401 with code `UNKNOWN_EMAIL`

#### Scenario: accounts/R2-S4 Email is case-insensitive
- **GIVEN** an account with email "anna@example.com"
- **WHEN** the user logs in with "Anna@Example.COM" and the correct password
- **THEN** the response is 200

#### Scenario: accounts/R2-S5 Deactivated account cannot log in
- **GIVEN** a deactivated teacher account
- **WHEN** the teacher logs in with the correct email and password
- **THEN** the response is 401 with code `ACCOUNT_DEACTIVATED`

### Requirement: R3 Logout
The system SHALL end the current session on logout.

#### Scenario: accounts/R3-S1 Logout ends the session
- **GIVEN** a logged-in student
- **WHEN** the student logs out
- **THEN** the current-user endpoint returns 401 for that session

### Requirement: R4 Role-based access
Every endpoint except login and set-password-by-link SHALL require a session. Admin
endpoints SHALL be allowed only for admins. Later capabilities apply the same rule to
teacher and student endpoints.

#### Scenario: accounts/R4-S1 No session is rejected
- **WHEN** a request without a session calls the current-user endpoint
- **THEN** the response is 401

#### Scenario: accounts/R4-S2 Teacher cannot call admin endpoints
- **GIVEN** a logged-in teacher
- **WHEN** the teacher calls the admin endpoint that lists teachers
- **THEN** the response is 403

#### Scenario: accounts/R4-S3 Student cannot call admin endpoints
- **GIVEN** a logged-in student
- **WHEN** the student calls the admin endpoint that lists teachers
- **THEN** the response is 403

### Requirement: R5 Admin creates teacher accounts
An admin SHALL be able to create a teacher account with name, email and time zone.
The account has no password yet; the response SHALL contain a single-use set-password
link. Emails MUST be unique across all accounts, case-insensitive.

#### Scenario: accounts/R5-S1 Admin creates a teacher
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a new email
- **THEN** the response is 201 with the teacher's id and a set-password link, and a login before the password is set gets 401 with code `PASSWORD_NOT_SET`

#### Scenario: accounts/R5-S2 Duplicate email is rejected
- **GIVEN** an account with email "anna@example.com"
- **WHEN** the admin creates a teacher with email "ANNA@example.com"
- **THEN** the response is 409 and no account is created

### Requirement: R6 Admin deactivates and reactivates teachers
An admin SHALL be able to deactivate and reactivate a teacher account. Deactivation
MUST end the teacher's active sessions.

#### Scenario: accounts/R6-S1 Deactivation ends sessions and blocks login
- **GIVEN** a logged-in teacher
- **WHEN** the admin deactivates the teacher
- **THEN** the teacher's existing session gets 401 and a new login gets 401

#### Scenario: accounts/R6-S2 Reactivated teacher can log in
- **GIVEN** a deactivated teacher
- **WHEN** the admin reactivates the teacher
- **THEN** the teacher can log in with their password

### Requirement: R7 Single-use password links
A set-password or reset-password link SHALL let its holder set a new password once.
A link MUST be rejected after it is used, after 7 days, or after a newer link for the
same account is issued. A successful use SHALL start a session for that account.

#### Scenario: accounts/R7-S1 Valid link sets the password
- **GIVEN** an unused set-password link issued 1 day ago
- **WHEN** its holder sets the password "new-password-1"
- **THEN** the response is 200, a session is started, and the account logs in with "new-password-1"

#### Scenario: accounts/R7-S2 Used link is rejected
- **GIVEN** a set-password link that was already used
- **WHEN** someone uses it again
- **THEN** the response is 410 and the password does not change

#### Scenario: accounts/R7-S3 Expired link is rejected
- **GIVEN** an unused link issued 8 days ago
- **WHEN** someone uses it
- **THEN** the response is 410

#### Scenario: accounts/R7-S4 Newer link replaces older one
- **GIVEN** two links issued for the same account, one after the other
- **WHEN** someone uses the older link
- **THEN** the response is 410 and the newer link still works

#### Scenario: accounts/R7-S5 Short password is rejected and link stays valid
- **GIVEN** an unused link
- **WHEN** its holder sets a 7-character password
- **THEN** the response is 400 and the link can still be used with a valid password

### Requirement: R8 Admin issues password reset links
An admin SHALL be able to issue a reset link for any account. After a reset, the old
password MUST stop working and the account's other sessions MUST end.

#### Scenario: accounts/R8-S1 Admin issues a reset link
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin requests a reset link for the teacher
- **THEN** the response is 201 with a link

#### Scenario: accounts/R8-S2 Reset replaces the old password and ends sessions
- **GIVEN** a logged-in student and a reset link issued for them
- **WHEN** the link is used to set a new password
- **THEN** the old password gets 401 on login and the student's earlier session gets 401

### Requirement: R9 Profile
A logged-in user SHALL be able to change their name and time zone, and change their
password by giving the current one. The time zone MUST be a valid IANA zone name.

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
