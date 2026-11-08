# Spec Delta

## Purpose

How a person gets into the web client and out of it: login, logout, setting a password
from a link, the current user, and routing each role to its own cabinet.

## ADDED Requirements

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
- **WHEN** the user presses the submit button again
- **THEN** no second login request is sent

#### Scenario: web-auth/R1-S5 Page explains how to get a link
- **WHEN** the login page is rendered, before any login attempt
- **THEN** it shows the translated text telling the user to ask their teacher or administrator for a link

#### Scenario: web-auth/R1-S6 Network error or rejected CSRF check
- **GIVEN** the login request fails with a network error, or the backend answers with code `CSRF_INVALID`
- **WHEN** the user submits the login form
- **THEN** the generic message or the translated message for `CSRF_INVALID` is shown
- **AND** the form can be submitted again

### Requirement: R2 Routing by login state and role

On start the web client SHALL load the current user. While loading it SHALL show a
loading state. If loading fails other than with 401, it SHALL show an error with a
retry button and not treat the user as anonymous. An anonymous user SHALL be sent to `/login` from any cabinet path and
from `/`. A logged-in user SHALL be sent from `/` and `/login` to their own cabinet,
and from another role's cabinet to their own.

#### Scenario: web-auth/R2-S1 Anonymous user opening a cabinet goes to login
- **GIVEN** the current-user request returns 401
- **WHEN** the user opens `/teacher`
- **THEN** the login page is shown

#### Scenario: web-auth/R2-S2 Root path sends each role to its cabinet
- **GIVEN** the current user is an admin, a teacher or a student
- **WHEN** the user opens `/`
- **THEN** `/admin`, `/teacher` or `/student` is shown, respectively

#### Scenario: web-auth/R2-S3 Root path sends an anonymous user to login
- **GIVEN** the current-user request returns 401
- **WHEN** the user opens `/`
- **THEN** the login page is shown

#### Scenario: web-auth/R2-S4 Another role's cabinet sends the user to their own
- **GIVEN** the current user is a teacher
- **WHEN** the user opens `/admin`
- **THEN** `/teacher` is shown

#### Scenario: web-auth/R2-S5 Logged-in user opening login goes to the cabinet
- **GIVEN** the current user is a student
- **WHEN** the user opens `/login`
- **THEN** `/student` is shown

#### Scenario: web-auth/R2-S6 Nothing is shown before the user is known
- **GIVEN** the current-user request has not answered yet
- **WHEN** the user opens `/teacher`
- **THEN** a loading state is shown, and neither the cabinet nor the login page

#### Scenario: web-auth/R2-S7 Failure to load the user is not a logout
- **GIVEN** the current-user request fails with a network error or a 500 with code `INTERNAL_ERROR`
- **WHEN** the user opens `/teacher`
- **THEN** a translated error with a "Try again" button is shown, and not the login page
- **AND** after the backend recovers, pressing "Try again" shows `/teacher` for a logged-in teacher

### Requirement: R3 Role cabinets

Each role SHALL have an empty cabinet page: `/admin`, `/teacher` and `/student`. Every
cabinet page SHALL have a header with the user's name and a "Log out" button. All text
SHALL come from the translation resources.

#### Scenario: web-auth/R3-S1 Cabinet shows the role and the user's name
- **GIVEN** the current user is a teacher named "Anna Smith"
- **WHEN** `/teacher` is rendered
- **THEN** the page shows the translated teacher cabinet title
- **AND** the header shows "Anna Smith" and a "Log out" button

### Requirement: R4 Logout

The "Log out" button SHALL end the session and show the login page. If the logout
request fails because the session has already ended, the result SHALL be the same. If
it fails for another reason, the page SHALL stay and show a translated error.

#### Scenario: web-auth/R4-S1 Logout ends the session
- **GIVEN** a logged-in student on `/student`
- **WHEN** the student presses "Log out"
- **THEN** the login page is shown
- **AND** opening `/student` afterwards shows the login page

#### Scenario: web-auth/R4-S2 Logout with an already ended session
- **GIVEN** the logout request returns 401 with code `UNAUTHENTICATED`
- **WHEN** the user presses "Log out"
- **THEN** the login page is shown without an error message

#### Scenario: web-auth/R4-S3 Logout that fails for another reason
- **GIVEN** the logout request fails with a network error or a 500
- **WHEN** the user presses "Log out"
- **THEN** the cabinet stays and a translated error is shown

### Requirement: R5 Set-password page

The web client SHALL show a set-password page at `/set-password` that reads the token
from the `token` parameter of the URL fragment and removes the fragment from the
address bar. It SHALL ask for the password twice and check that both match, have at
least 8 characters and at most 72 bytes before sending. On success it SHALL open the
cabinet of the account the link belongs to, replacing any current session.

#### Scenario: web-auth/R5-S1 Valid link sets the password and opens the cabinet
- **GIVEN** an admin created a teacher and got a set-password link
- **WHEN** the link is opened and the password "teacher-pass-1" is entered twice and submitted
- **THEN** `/teacher` is shown
- **AND** the teacher can later log in with "teacher-pass-1"

#### Scenario: web-auth/R5-S2 Token is removed from the address bar
- **WHEN** `/set-password#token=abc` is opened, with the page rendered in React strict mode
- **THEN** the address bar shows `/set-password` without the fragment
- **AND** the form is shown, and submitting it sends the token "abc"

#### Scenario: web-auth/R5-S3 Passwords that differ are not sent
- **WHEN** the two password fields differ and the form is submitted
- **THEN** a translated "passwords do not match" message is shown and no request is sent

#### Scenario: web-auth/R5-S4 A short password is not sent
- **WHEN** a 7-character password is entered twice and submitted
- **THEN** a translated "at least 8 characters" message is shown and no request is sent

#### Scenario: web-auth/R5-S5 A used, expired or replaced link shows a clear message
- **GIVEN** the backend answers with status 410 and code `LINK_INVALID`
- **WHEN** the form is submitted
- **THEN** the translated message for `LINK_INVALID` is shown, saying to ask for a new link

#### Scenario: web-auth/R5-S6 A link without a token
- **WHEN** `/set-password` is opened without a token in the fragment
- **THEN** a translated message says the link is incomplete, and no form is shown

#### Scenario: web-auth/R5-S7 Link for a deactivated account
- **GIVEN** the backend answers with code `ACCOUNT_DEACTIVATED`
- **WHEN** the form is submitted
- **THEN** the translated message for `ACCOUNT_DEACTIVATED` is shown

#### Scenario: web-auth/R5-S8 A password over 72 bytes is not sent
- **WHEN** a password of 40 Cyrillic letters (80 bytes) is entered twice and submitted
- **THEN** a translated "password is too long" message is shown and no request is sent

#### Scenario: web-auth/R5-S9 A link used at the same moment elsewhere
- **GIVEN** the backend answers with status 409 and code `LINK_CONFLICT`
- **WHEN** the form is submitted
- **THEN** the translated message for `LINK_CONFLICT` is shown and the form can be submitted again

#### Scenario: web-auth/R5-S10 A logged-in admin uses a teacher's link
- **GIVEN** an admin is logged in and holds a new teacher's set-password link
- **WHEN** the admin opens the link and sets a valid password
- **THEN** `/teacher` is shown with the teacher's name in the header
- **AND** the admin's session has ended

### Requirement: R6 Session ended during use

When any API request made by a logged-in page gets 401, the web client SHALL show the
login page with a translated "your session has ended" message. After the user logs in
again without reloading, it SHALL return to the page they were on if that page belongs
to their role; otherwise, and after a reload, it SHALL open their own cabinet.

#### Scenario: web-auth/R6-S1 Session end sends the user to login
- **GIVEN** a teacher on `/teacher`
- **WHEN** an API request made through the web client's API client gets 401 (in this change, a test request; the cabinets send none yet)
- **THEN** the login page is shown with the translated "your session has ended" message

#### Scenario: web-auth/R6-S2 After logging in again the user returns to the page
- **GIVEN** the session of a teacher on `/teacher` ended and the login page is shown
- **WHEN** the teacher logs in again
- **THEN** `/teacher` is shown

#### Scenario: web-auth/R6-S3 A page of another role is not restored
- **GIVEN** the session ended on `/teacher` and the login page is shown
- **WHEN** a student logs in on that page
- **THEN** `/student` is shown

#### Scenario: web-auth/R6-S4 After a reload the return is not kept
- **GIVEN** the session ended on `/teacher` and the login page is shown
- **WHEN** the page is reloaded and the teacher logs in
- **THEN** `/teacher` is shown and no "your session has ended" message is left on the login page after the reload
