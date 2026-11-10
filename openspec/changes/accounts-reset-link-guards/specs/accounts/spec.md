# Spec Delta

## MODIFIED Requirements

### Requirement: R8 Admin issues password reset links
An admin SHALL be able to issue a reset link for any active account except their own.
A deactivated account MUST get 409 `ACCOUNT_DEACTIVATED` and the admin's own account
409 `OWN_ACCOUNT`; neither stores a link. After a reset, the old password MUST stop
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
- **GIVEN** a logged-in admin and a deactivated teacher with a password
- **WHEN** the admin requests a reset link for the teacher
- **THEN** the response is 409 with code `ACCOUNT_DEACTIVATED`, and no link is stored for the teacher

#### Scenario: accounts/R8-S5 Reset link for the admin's own account is rejected
- **GIVEN** a logged-in admin and a second admin account
- **WHEN** the admin requests a reset link for their own account and then for the second admin's account
- **THEN** the first response is 409 with code `OWN_ACCOUNT`, no link is stored for the admin, and the admin's session still works; the second response is 201 with a link
