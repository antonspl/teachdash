# accounts Specification

## Purpose

Lets admins, teachers and students have accounts with one role each, sign in with
email and password, and manage passwords through single-use links sent by hand.

## Requirements

### Requirement: R1 First admin from environment
On startup, if no admin account exists and `ADMIN_EMAIL`, `ADMIN_PASSWORD` and
`ADMIN_NAME` are set, the system SHALL create an active admin account with these
values. If an admin already exists, the system MUST NOT create or change any account.
If no admin exists and a variable breaks the rules the API applies to the same field,
startup MUST fail with a message naming the variable, without its value. Values are taken
literally, with no placeholder or expression handling. A blank value counts as invalid,
not as missing.

#### Scenario: accounts/R1-S1 First start creates the admin
- **GIVEN** an empty database and the admin variables are set
- **WHEN** the application starts
- **THEN** an admin account with that email exists and can log in with that password

#### Scenario: accounts/R1-S2 Existing admin is not changed
- **GIVEN** an admin account exists with the email from `ADMIN_EMAIL` and password "old-password"
- **WHEN** the application starts with `ADMIN_PASSWORD` set to a different value
- **THEN** no new account is created, the admin account is unchanged (email, name, password hash, role, active), and the admin still logs in with "old-password"

#### Scenario: accounts/R1-S3 Admin with another email blocks bootstrap
- **GIVEN** an admin account exists with an email different from `ADMIN_EMAIL`
- **WHEN** the application starts
- **THEN** no account is created and no account with the `ADMIN_EMAIL` email exists

#### Scenario: accounts/R1-S4 Invalid admin variables stop startup
- **GIVEN** no admin account exists and `ADMIN_PASSWORD` is "abc"
- **WHEN** the application starts
- **THEN** startup fails with a message naming `ADMIN_PASSWORD` and not containing "abc", and no account is created

#### Scenario: accounts/R1-S5 An email the API would reject stops startup
- **GIVEN** no admin account exists and `ADMIN_EMAIL` is "a,b@x"
- **WHEN** the application starts
- **THEN** startup fails with a message naming `ADMIN_EMAIL`, and no account is created

#### Scenario: accounts/R1-S6 Admin variables are taken literally
- **GIVEN** no admin account exists and `ADMIN_PASSWORD` is "#{1+1}${x}aaaaaaa"
- **WHEN** the application starts
- **THEN** the admin account exists and logs in with exactly that password

#### Scenario: accounts/R1-S7 A blank admin password stops startup
- **GIVEN** no admin account exists and `ADMIN_PASSWORD` is eight spaces
- **WHEN** the application starts
- **THEN** startup fails with a message naming `ADMIN_PASSWORD`, and no account is created

#### Scenario: accounts/R1-S8 An invalid variable stops startup even if another is missing
- **GIVEN** no admin account exists, `ADMIN_EMAIL` is "a,b@x" and `ADMIN_NAME` is not set
- **WHEN** the application starts
- **THEN** startup fails with a message naming `ADMIN_EMAIL`, and no account is created

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

### Requirement: R3 Logout
The system SHALL end the current session on logout. Logout without a session gets 401.

#### Scenario: accounts/R3-S1 Logout ends the session
- **GIVEN** a logged-in student
- **WHEN** the student logs out
- **THEN** the current-user endpoint returns 401 for that session

#### Scenario: accounts/R3-S2 Logout without a session is rejected
- **WHEN** a client with a valid CSRF token and no session logs out
- **THEN** the response is 401 with code `UNAUTHENTICATED`

### Requirement: R4 Role-based access
Every endpoint except login, set-password-by-link, join-link preview and join
(student-enrolment R9) and `/actuator/health` SHALL require a session. Admin endpoints
SHALL be allowed only for admins; later capabilities do the same for teachers and students.
A POST, PUT, PATCH or DELETE without a valid CSRF token MUST get 403. Login,
set-password-by-link and a visitor's join SHALL replace the CSRF token and return the new
one.

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

#### Scenario: accounts/R4-S4 Request without CSRF token is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher changes their profile without sending the CSRF token, and then sends the same request with the token
- **THEN** the first response is 403 and leaves the profile unchanged, and the second response is 200

#### Scenario: accounts/R4-S5 Non-admins cannot call admin endpoints that change data
- **GIVEN** a logged-in teacher, a logged-in student, the admin account and another teacher
- **WHEN** the teacher and the student each call every admin endpoint that changes data: create a teacher, deactivate and reactivate the other teacher, issue a reset link for the admin
- **THEN** every response is 403, no account is created or changed, and no link is issued

#### Scenario: accounts/R4-S6 Anonymous response sets the CSRF cookie
- **WHEN** a client with no cookies calls the current-user endpoint
- **THEN** the response is 401 and sets a non-empty `XSRF-TOKEN` cookie

#### Scenario: accounts/R4-S7 Login response carries a new CSRF token
- **GIVEN** a client with a CSRF cookie and an active account
- **WHEN** the client logs in, and then sends a state-changing request with the token from the login response
- **THEN** the login response sets a new non-empty `XSRF-TOKEN` cookie with `SameSite=Lax`, different from the old one, and the next request is not rejected for CSRF

#### Scenario: accounts/R4-S8 A failure in the session check returns a coded error
- **GIVEN** a request with a session
- **WHEN** the database fails during the session check
- **THEN** the response is 500 with code `INTERNAL_ERROR` and no internal details in the body

#### Scenario: accounts/R4-S9 The coded error survives a failure saving the session
- **GIVEN** a request with a session
- **WHEN** the database fails during the session check, and saving the session then fails too
- **THEN** the response is 500 with code `INTERNAL_ERROR` and no internal details in the body

#### Scenario: accounts/R4-S10 A failure loading the session returns a coded error
- **GIVEN** a request with a session cookie
- **WHEN** the database fails while the session is loaded
- **THEN** the response is 500 with code `INTERNAL_ERROR` and no internal details in the body

#### Scenario: accounts/R4-S11 The coded error replaces a large response
- **GIVEN** a logged-in admin and a teacher list too large for the response buffer
- **WHEN** the admin requests the list and saving the session fails

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

### Requirement: R6 Admin deactivates and reactivates teachers
An admin SHALL be able to deactivate and reactivate a teacher account. Deactivation
MUST end the teacher's sessions: every request re-checks that the account is active.
Deactivation also releases the teacher's students (student-enrolment R7).
The teacher list SHALL show whether each teacher is active. An id that is not a teacher
MUST get 404 and change nothing. Reactivating a teacher who has no password SHALL return
a new set-password link.

#### Scenario: accounts/R6-S1 Deactivation ends sessions and blocks login
- **GIVEN** a logged-in teacher
- **WHEN** the admin deactivates the teacher
- **THEN** the teacher's existing session gets 401 and a new login gets 401 with code `ACCOUNT_DEACTIVATED`

#### Scenario: accounts/R6-S2 Reactivated teacher can log in
- **GIVEN** a deactivated teacher
- **WHEN** the admin reactivates the teacher
- **THEN** the teacher can log in with their password

#### Scenario: accounts/R6-S3 Only teachers can be deactivated
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin tries to deactivate their own account and the student's account
- **THEN** both responses are 404, both accounts stay active, and the admin's session still works

#### Scenario: accounts/R6-S4 Every request re-checks that the account is active
- **GIVEN** a logged-in teacher whose session still exists
- **WHEN** the account becomes inactive in the database without the session being touched
- **THEN** the teacher's next request gets 401

#### Scenario: accounts/R6-S5 Teacher list shows who is active
- **GIVEN** a logged-in admin, an active teacher and a deactivated teacher
- **WHEN** the admin lists teachers
- **THEN** each list item has an `active` field with the right value

#### Scenario: accounts/R6-S6 Reactivation does not revive an old session
- **GIVEN** a logged-in teacher
- **WHEN** the admin deactivates and then reactivates the teacher
- **THEN** the teacher's old session gets 401 and a new login returns 200

#### Scenario: accounts/R6-S7 Reactivating a teacher with no password returns a new link
- **GIVEN** a logged-in admin and a deactivated teacher who never set a password
- **WHEN** the admin reactivates the teacher, and the returned link is used to set a password
- **THEN** reactivation returns 200 with a set-password link that starts with the configured base URL, and the link sets the password

#### Scenario: accounts/R6-S8 Reactivating an active teacher keeps their link
- **GIVEN** a logged-in admin and an active teacher with an unused set-password link
- **WHEN** the admin reactivates the teacher
- **THEN** the response is 204 and the existing link still sets the password

### Requirement: R7 Single-use password links
A set-password or reset-password link SHALL let its holder set a new password once.
A link MUST be rejected after it is used, after 7 days, or after a newer link for the
same account is issued. A successful use SHALL start a session for that account.
A link for a deactivated account MUST be rejected with 401 `ACCOUNT_DEACTIVATED`
and change nothing.

#### Scenario: accounts/R7-S1 Valid link sets the password
- **GIVEN** an unused set-password link issued 1 day ago
- **WHEN** its holder sets the password "new-password-1"
- **THEN** the response is 200, a session is started, and the account logs in with "new-password-1"

#### Scenario: accounts/R7-S2 Used link is rejected
- **GIVEN** a set-password link that was already used
- **WHEN** someone uses it again
- **THEN** the response is 410 and the password does not change

#### Scenario: accounts/R7-S3 Expired link is rejected
- **GIVEN** an unused link issued 8 days ago for an account with a password
- **WHEN** someone uses it to set a new password
- **THEN** the response is 410 and the old password still works

#### Scenario: accounts/R7-S4 Newer link replaces older one
- **GIVEN** two links issued for the same account, one after the other
- **WHEN** someone uses the older link
- **THEN** the response is 410 and the newer link still works

#### Scenario: accounts/R7-S5 Short password is rejected and link stays valid
- **GIVEN** an unused link
- **WHEN** its holder sets a 7-character password
- **THEN** the response is 400 and the link can still be used with a valid password

#### Scenario: accounts/R7-S6 Link for a deactivated account is rejected
- **GIVEN** an admin created a teacher and got a set-password link
- **WHEN** the admin deactivates the teacher and the link is then used to set a password
- **THEN** the response is 401 with code `ACCOUNT_DEACTIVATED`, the teacher still has no password, and no session is started

### Requirement: R8 Admin issues password reset links
An admin SHALL be able to issue a reset link for any active account except their own.
A deactivated account MUST get 409 `ACCOUNT_DEACTIVATED` and the admin's own account
409 `OWN_ACCOUNT`; neither stores a link or invalidates an existing one. After a reset, the old password MUST stop
working and the account's other sessions MUST end. Each account has a session version;
a session started under an older version MUST NOT work.

#### Scenario: accounts/R8-S1 Admin issues a reset link
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin requests a reset link for the teacher
- **THEN** the response is 201 with a link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment

#### Scenario: accounts/R8-S2 Reset replaces the old password and ends sessions
- **GIVEN** a logged-in student and a reset link issued for them
- **WHEN** the link is used to set a new password
- **THEN** the old password gets 401 on login, the student's earlier session gets 401, and the session started by the link stays valid

#### Scenario: accounts/R8-S3 A session from an older session version is rejected
- **GIVEN** a logged-in teacher whose session still exists
- **WHEN** the account's session version is raised in the database without the session being touched
- **THEN** the teacher's next request gets 401

#### Scenario: accounts/R8-S4 Reset link for a deactivated account is rejected
- **GIVEN** a logged-in admin and a deactivated teacher with a password and an unused link
- **WHEN** the admin requests a reset link for the teacher
- **THEN** the response is 409 with code `ACCOUNT_DEACTIVATED`, the teacher's existing link is still unused, and no other link is stored for the teacher

#### Scenario: accounts/R8-S5 Reset link for the admin's own account is rejected
- **GIVEN** a logged-in admin with an unused reset link for their own account, and a second admin account
- **WHEN** the admin requests a reset link for their own account and then for the second admin's account
- **THEN** the first response is 409 with code `OWN_ACCOUNT`, the admin's existing link is still unused, and no other link is stored for the admin; the second response is 201 with a link

### Requirement: R9 Profile
A logged-in user SHALL be able to change their name and time zone, and change their
password by giving the current one. The time zone MUST be a valid IANA zone name.
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

### Requirement: R10 Login timing does not reveal accounts
Response time MUST NOT reveal whether an email has an account or whether the account has
a password. Every login request that passes validation SHALL run exactly one password hash
check, at the cost used for new passwords, even when there is no stored hash to check.

#### Scenario: accounts/R10-S1 A login with no real hash to check still runs one hash check
- **GIVEN** an active teacher with a password, an active teacher with no password and a deactivated teacher with no password
- **WHEN** someone logs in with an email that has no account, with each no-password teacher's email and any password, and with the first teacher's email and a password of 73 ASCII characters
- **THEN** each login runs exactly one password hash check, at the cost used for new passwords

### Requirement: R11 Account fields have one rule
Name, email and time zone SHALL follow the same rules wherever an account is created or
changed. Whitespace around a name or email SHALL be ignored before they are checked and
stored, including invisible characters around a name. A name MUST contain a letter or
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
