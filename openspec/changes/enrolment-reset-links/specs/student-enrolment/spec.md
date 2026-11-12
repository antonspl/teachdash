## ADDED Requirements

### Requirement: R12 Teacher issues password reset links for own students
A teacher SHALL be able to issue a password reset link for a student assigned to them,
with or without a password. The link follows the rules and effects of an admin's reset
link (accounts R7 and R8): it voids the student's older unused link, and using it
replaces the password and ends the student's other sessions. An id that is not a student
assigned to the teacher MUST return 404, as in R2, and change nothing.

#### Scenario: student-enrolment/R12-S1 Teacher issues a reset link for own student
- **GIVEN** a student with the password "old-password-1", assigned to teacher A, and logged in on another client
- **WHEN** teacher A requests a reset link for the student and the link is used to set the password "new-password-1"
- **THEN** the response to the request is 201 with a link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the link response is 200, logging in with "old-password-1" returns 401, the student's earlier session gets 401, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R12-S2 Another teacher's student is not found
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link issued by teacher B
- **WHEN** teacher A requests a reset link for that student
- **THEN** the response is 404 with code `NOT_FOUND`, and the set-password link still sets the password

#### Scenario: student-enrolment/R12-S3 Ids that are not own students are not found
- **GIVEN** a logged-in teacher A, teacher B with an unused link, a student with no teacher and an unused link, and an id with no account
- **WHEN** teacher A requests a reset link for teacher B's id, the student's id, the id with no account and the id "not-a-uuid"
- **THEN** each of the four responses is 404 with code `NOT_FOUND`, the existing links of teacher B and the student are still unused, and no other link is stored for either

#### Scenario: student-enrolment/R12-S4 Student without a password gets a working link
- **GIVEN** a student without a password, assigned to teacher A, with an unused set-password link issued by teacher A
- **WHEN** teacher A requests a reset link for the student, the set-password link is then used to set the password "old-link-1", and the reset link to set the password "new-password-1"
- **THEN** the response to the request is 201 with a link, the set-password link response is 410 with code `LINK_INVALID`, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R12-S5 Teacher's link voids an admin's unused link
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin
- **WHEN** teacher A requests a reset link for the student, the admin's link is then used to set the password "admin-password-1", and the teacher's link to set the password "new-password-1"
- **THEN** the admin's link response is 410 with code `LINK_INVALID`, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R12-S6 Students and admins cannot issue through the teacher endpoint
- **GIVEN** a student assigned to teacher A, a logged-in student and a logged-in admin
- **WHEN** the logged-in student and the admin each request a reset link for that student through the teacher endpoint
- **THEN** both responses are 403

#### Scenario: student-enrolment/R12-S7 Issuing requires a session
- **GIVEN** a student assigned to teacher A and a client with a valid CSRF token and no session
- **WHEN** the client requests a reset link for that student
- **THEN** the response is 401 with code `UNAUTHENTICATED`

#### Scenario: student-enrolment/R12-S8 Request without a CSRF token is rejected
- **GIVEN** a student with the password "old-password-1", assigned to teacher A
- **WHEN** teacher A requests a reset link for the student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and the student still logs in with "old-password-1"

#### Scenario: student-enrolment/R12-S9 Only the newest link works
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A requests a reset link twice, the first link is then used to set the password "first-password-1", and the second to set the password "new-password-1"
- **THEN** both requests return 201, the first link response is 410 with code `LINK_INVALID`, and the student logs in with "new-password-1"

## MODIFIED Requirements

### Requirement: R4 Teacher removes their student
A teacher SHALL be able to remove a student assigned to them. Removal deletes the
assignment and keeps the student's account, which then has no teacher and can be registered
again as in R1. Removal MUST also invalidate the student's unused link if this teacher
issued it, whether a set-password link (R1) or a reset link (R12); a link an admin issued
stays usable. An id that is not a student assigned to the teacher MUST return 404, as in R2.

#### Scenario: student-enrolment/R4-S1 Removed student disappears from the lists
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A removes the student
- **THEN** the response is 204, the student is not in teacher A's list, teacher A is not in the student's list of teachers, and the student can still log in

#### Scenario: student-enrolment/R4-S3 Removal keeps the account and invalidates the unused link
- **GIVEN** a student without a password, assigned to teacher A, with an unused set-password link issued by teacher A, and a logged-in admin
- **WHEN** teacher A removes the student, the link is then used to set the password "new-password-1", and the admin looks up the student's email
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the lookup response is 200

#### Scenario: student-enrolment/R4-S4 Another teacher's student is not found
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link issued by teacher B
- **WHEN** teacher A removes that student, and then an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`, the student is still in teacher B's list, and the link still sets the password

#### Scenario: student-enrolment/R4-S5 Removing twice is not found
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student twice
- **THEN** the first response is 204 and the second is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S6 Malformed id is not found
- **GIVEN** a logged-in teacher
- **WHEN** the teacher removes the id "not-a-uuid"
- **THEN** the response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S7 Removed student can be registered again
- **GIVEN** a student with a password, assigned to teacher A, and teacher B
- **WHEN** teacher A removes the student, teacher B then registers a student with that email, and teacher A then registers it too
- **THEN** the removal response is 204, teacher B's response is 201, teacher A's response is 409 with code `STUDENT_EXISTS`, the student is in teacher B's list and not in teacher A's, teacher B's request for the student's details returns 200 and teacher A's returns 404, and teacher B is the only entry in the student's list of teachers

#### Scenario: student-enrolment/R4-S8 Students and admins cannot remove
- **GIVEN** a student assigned to teacher A, a logged-in student and a logged-in admin
- **WHEN** the logged-in student and the admin each remove that student through the teacher endpoint
- **THEN** both responses are 403, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S9 Removing requires a session
- **GIVEN** a student assigned to teacher A and a client with a valid CSRF token and no session
- **WHEN** the client removes that student
- **THEN** the response is 401 with code `UNAUTHENTICATED`, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S10 Request without a CSRF token is rejected
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S11 Teacher's other students stay
- **GIVEN** teacher A with students S1 and S2
- **WHEN** teacher A removes S1
- **THEN** the response is 204, and teacher A's list contains S2 only

#### Scenario: student-enrolment/R4-S13 Removal keeps an admin's reset link
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin
- **WHEN** teacher A removes the student and the link is then used to set the password "new-password-1"
- **THEN** the removal response is 204, the link response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R4-S14 Same teacher can register a removed student again
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A removes the student and then registers a student with that email
- **THEN** the removal response is 204, the registration response is 201 with no set-password link, and the student is in teacher A's list

#### Scenario: student-enrolment/R4-S15 Removal invalidates the teacher's reset link
- **GIVEN** a student with the password "old-password-1", assigned to teacher A, with an unused reset link issued by teacher A
- **WHEN** teacher A removes the student and the link is then used to set the password "new-password-1"
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the student logs in with "old-password-1"

### Requirement: R7 Deactivating a teacher releases their students
Deactivating a teacher SHALL remove all of that teacher's assignments in the same
transaction, as if the teacher removed each student (R4): each account stays, has no
teacher, can be registered again, and its unused link is invalidated if that teacher issued
it; a link an admin issued stays usable.
Reactivating the teacher SHALL NOT restore the assignments.

#### Scenario: student-enrolment/R7-S1 Deactivation releases every student
- **GIVEN** teacher A with a student S1 who has a password and a student S2 without a password with an unused set-password link issued by teacher A, and teacher B with a student S3 without a password with an unused set-password link issued by teacher B
- **WHEN** the admin deactivates teacher A, the link of S2 is then used to set the password "new-password-1", and the link of S3 to set the password "new-password-3"
- **THEN** the deactivation response is 204, the admin's list of teacher A's students is empty, S1's list of teachers is empty, S1 can still log in, the response for S2's link is 410 with code `LINK_INVALID`, S3 is still in teacher B's list, and S3 logs in with "new-password-3"

#### Scenario: student-enrolment/R7-S2 Released student can be registered by another teacher
- **GIVEN** teacher A with a student who has a password, and teacher B
- **WHEN** the admin deactivates teacher A, and teacher B then registers a student with that email
- **THEN** the registration response is 201, the student is in teacher B's list, and teacher B is the only entry in the student's list of teachers

#### Scenario: student-enrolment/R7-S3 Reactivation does not restore students
- **GIVEN** teacher A with a password and a student
- **WHEN** the admin deactivates and then reactivates teacher A
- **THEN** the admin's list of teacher A's students is empty, and the student's list of teachers is empty

#### Scenario: student-enrolment/R7-S4 Failed release keeps the teacher active
- **GIVEN** teacher A with a password and a student, and a failure while the students are released
- **WHEN** the admin deactivates teacher A
- **THEN** the response is 500, teacher A can log in, and the student is still in teacher A's list

#### Scenario: student-enrolment/R7-S5 Release keeps an admin's reset link
- **GIVEN** teacher A with a student who has a password and an unused reset link issued by an admin
- **WHEN** the admin deactivates teacher A and the link is then used to set the password "new-password-1"
- **THEN** the deactivation response is 204, the link response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R7-S6 Teacher without students and repeated deactivation
- **GIVEN** teacher A with no students, and teacher B with a student
- **WHEN** the admin deactivates teacher A twice
- **THEN** both responses are 204, and the student is still in teacher B's list

#### Scenario: student-enrolment/R7-S7 Release invalidates the teacher's reset link
- **GIVEN** teacher A with a student who has the password "old-password-1" and an unused reset link issued by teacher A
- **WHEN** the admin deactivates teacher A and the link is then used to set the password "new-password-1"
- **THEN** the deactivation response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the student logs in with "old-password-1"
