# Spec Delta

## MODIFIED Requirements

### Requirement: R3 Role cabinets

Each role SHALL have a cabinet page: `/admin`, `/teacher` and `/student`. The teacher
and student cabinets are empty; the admin cabinet shows the teachers page
(web-admin-teachers). Every cabinet page SHALL have a header with the user's name and a
"Log out" button. All text SHALL come from the translation resources.

#### Scenario: web-auth/R3-S1 Cabinet shows the role and the user's name
- **GIVEN** the current user is a teacher named "Anna Smith"
- **WHEN** `/teacher` is rendered
- **THEN** the page shows the translated teacher cabinet title
- **AND** the header shows "Anna Smith" and a "Log out" button
