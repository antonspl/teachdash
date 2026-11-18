## MODIFIED Requirements

### Requirement: R7 Single-use password links
A set-password or reset-password link SHALL let its holder set a new password once.
A link MUST be rejected after it is used, after 7 days, or after a newer link for the same
account is issued by an admin or by the same issuer. Otherwise links from different
issuers stay usable side by side until one of them is used: a successful use MUST make the
account's other unused links unusable. A successful use SHALL start a session for that account.
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
- **GIVEN** two links issued for the same account by the same issuer, one after the other
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

#### Scenario: accounts/R7-S7 Links from different issuers work until one is used
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin and a newer unused reset link issued by teacher A
- **WHEN** the teacher's link is used with a 7-character password, the admin's link is then used to set the password "admin-password-1", and the teacher's link to set the password "teacher-password-1"
- **THEN** the first response is 400, the admin's link response is 200, the teacher's link response is 410 with code `LINK_INVALID`, and the student logs in with "admin-password-1"

### Requirement: R8 Admin issues password reset links
An admin SHALL be able to issue a reset link for any active account except their own.
A deactivated account MUST get 409 `TARGET_ACCOUNT_DEACTIVATED` and the admin's own account
409 `OWN_ACCOUNT`; neither stores a link or invalidates an existing one.
`TARGET_ACCOUNT_DEACTIVATED` means the account acted on is deactivated; `ACCOUNT_DEACTIVATED`
(R2, R7) is used only for the caller's own account. An admin's new link SHALL void every
older unused link of the account, whoever issued it (R7). After a reset, the old password
MUST stop working and the account's other sessions MUST end. Each account has a session
version; a session started under an older version MUST NOT work.

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
- **THEN** the response is 409 with code `TARGET_ACCOUNT_DEACTIVATED`, the teacher's existing link is still unused, and no other link is stored for the teacher

#### Scenario: accounts/R8-S5 Reset link for the admin's own account is rejected
- **GIVEN** a logged-in admin with an unused reset link for their own account, and a second admin account
- **WHEN** the admin requests a reset link for their own account and then for the second admin's account
- **THEN** the first response is 409 with code `OWN_ACCOUNT`, the admin's existing link is still unused, and no other link is stored for the admin; the second response is 201 with a link

#### Scenario: accounts/R8-S6 Admin's reset voids a teacher's link
- **GIVEN** a logged-in admin and a student with a password, assigned to teacher A, with an unused reset link issued by teacher A
- **WHEN** the admin requests a reset link for the student, the teacher's link is then used to set the password "teacher-password-1", and the admin's link to set the password "admin-password-1"
- **THEN** the admin's request returns 201, the teacher's link response is 410 with code `LINK_INVALID`, the admin's link response is 200, and the student logs in with "admin-password-1"
