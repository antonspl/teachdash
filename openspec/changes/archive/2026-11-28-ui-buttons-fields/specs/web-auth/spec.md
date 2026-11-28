# Spec Delta

## MODIFIED Requirements

### Requirement: R1 Login page

The web client SHALL show a login page at `/login` with email and password fields. On
success it SHALL open the user's cabinet. On failure it SHALL show the translated
message for the problem code, or the generic message, and keep the entered email. The
page SHALL always show a short text telling users without a working password to ask
their teacher or administrator for a link.

#### Scenario: web-auth/R1-S1 Valid credentials open the cabinet
- **GIVEN** an active teacher with password "teacher-pass-1"
- **WHEN** the teacher logs in on `/login` with that email and password
- **THEN** the teacher's cabinet `/teacher` is shown

#### Scenario: web-auth/R1-S2 Wrong credentials show the translated message
- **GIVEN** the backend answers the login with code `INVALID_CREDENTIALS`
- **WHEN** the user submits the login form
- **THEN** the translated message for `INVALID_CREDENTIALS` is shown
- **AND** the email field keeps its value and the password field is empty

#### Scenario: web-auth/R1-S3 Deactivated account shows its own message
- **GIVEN** the backend answers the login with code `ACCOUNT_DEACTIVATED`
- **WHEN** the user submits the login form
- **THEN** the translated message for `ACCOUNT_DEACTIVATED` is shown

#### Scenario: web-auth/R1-S4 Form is not sent twice
- **GIVEN** a login request is in progress
- **WHEN** the user presses the same submit button again (found, while the request runs, by its busy label, `login.submitting`, web-client R29)
- **THEN** no second login request is sent
- **AND** that button is disabled and has `aria-busy="true"`

#### Scenario: web-auth/R1-S5 Page explains how to get a link
- **WHEN** the login page is rendered, before any login attempt
- **THEN** it shows the translated text telling the user to ask their teacher or administrator for a link

#### Scenario: web-auth/R1-S6 Network error or rejected CSRF check
- **GIVEN** the login request fails with a network error, or the backend answers with code `CSRF_INVALID`
- **WHEN** the user submits the login form
- **THEN** the generic message or the translated message for `CSRF_INVALID` is shown
- **AND** the form can be submitted again
