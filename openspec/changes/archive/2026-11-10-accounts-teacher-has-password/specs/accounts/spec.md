# Spec Delta

## ADDED Requirements

### Requirement: R12 Teacher list shows whether a password is set
Each item of the admin's teacher list SHALL have a boolean `hasPassword`: `false` until
the teacher sets a password, `true` afterwards. It does not depend on whether the teacher
is active. The list MUST NOT return the password hash.

#### Scenario: accounts/R12-S1 Teacher who has not set a password
- **GIVEN** a logged-in admin and a teacher who has not set a password
- **WHEN** the admin lists teachers
- **THEN** the teacher's list item has `hasPassword` equal to `false`

#### Scenario: accounts/R12-S2 Teacher who set a password with a link
- **GIVEN** a logged-in admin and a teacher who set a password with a set-password link
- **WHEN** the admin lists teachers
- **THEN** the teacher's list item has `hasPassword` equal to `true`, and the response does not contain the teacher's stored password hash

#### Scenario: accounts/R12-S3 Deactivated teachers keep their password status
- **GIVEN** a logged-in admin, a deactivated teacher with a password and a deactivated teacher who never set one
- **WHEN** the admin lists teachers
- **THEN** `hasPassword` is `true` for the first teacher and `false` for the second
