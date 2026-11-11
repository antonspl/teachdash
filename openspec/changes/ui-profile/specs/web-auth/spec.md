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
