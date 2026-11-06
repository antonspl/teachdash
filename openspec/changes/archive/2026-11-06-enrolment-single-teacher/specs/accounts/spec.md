# Spec Delta

## MODIFIED Requirements

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
