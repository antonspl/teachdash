# Spec Delta

## MODIFIED Requirements

### Requirement: R4 Role-based access
Every endpoint except login, set-password-by-link, the join-link preview and join
(student-enrolment R9) and the health check (`/actuator/health`) SHALL require a session. Admin
endpoints SHALL be allowed only for admins. Later capabilities apply the same rule to
teacher and student endpoints. A state-changing request (POST, PUT, PATCH, DELETE)
without a valid CSRF token MUST be rejected with 403. Login SHALL replace the CSRF token,
and its response SHALL carry the new one.

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
