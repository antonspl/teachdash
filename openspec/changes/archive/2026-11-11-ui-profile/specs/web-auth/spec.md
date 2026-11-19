# Spec Delta

## MODIFIED Requirements

### Requirement: R3 Role cabinets

Each role SHALL have a cabinet page: `/admin`, `/teacher` and `/student`. The teacher
and student cabinets are empty; the admin cabinet shows the teachers page
(web-admin-teachers). Every cabinet page SHALL have a header with the user's name, a
"Profile" button that opens the profile page (web-profile) and a "Log out" button. All
text SHALL come from the translation resources.

#### Scenario: web-auth/R3-S1 Cabinet shows the role and the user's name
- **GIVEN** the current user is a teacher named "Anna Smith"
- **WHEN** `/teacher` is rendered
- **THEN** the page shows the translated teacher cabinet title
- **AND** the header shows "Anna Smith", a "Profile" button and a "Log out" button

#### Scenario: web-auth/R3-S2 Profile button opens the profile
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the admin presses "Profile"
- **THEN** `/profile` is shown

### Requirement: R6 Session ended during use

When any API request made by a logged-in page gets 401, the web client SHALL show the
login page with a translated "your session has ended" message. After the user logs in
again without reloading, it SHALL return to the page they were on if that page belongs
to their role or is the profile page (web-profile R4); otherwise, and after a reload,
it SHALL open their own cabinet.

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
