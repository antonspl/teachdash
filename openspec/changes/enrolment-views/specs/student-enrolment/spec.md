# Spec Delta

## ADDED Requirements

### Requirement: R4 Teacher removes a student
A teacher SHALL be able to remove a student assigned to them. Removal deletes that
assignment: the student's account and assignments to other teachers stay. Removing a
student's last teacher MUST also invalidate the student's unused password links. An id that
is not a student assigned to the teacher MUST return 404, as in R2.

#### Scenario: student-enrolment/R4-S1 Removed student disappears from the lists
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A removes the student
- **THEN** the response is 204, the student is not in teacher A's list, teacher A is not in the student's list of teachers, and the student can still log in

#### Scenario: student-enrolment/R4-S2 Assignments to other teachers stay
- **GIVEN** a student assigned to teachers A and B
- **WHEN** teacher A removes the student
- **THEN** the response is 204, the student is still in teacher B's list, and teacher B is still in the student's list of teachers

#### Scenario: student-enrolment/R4-S3 Last removal keeps the account and invalidates the unused link
- **GIVEN** a student without a password, assigned only to teacher A, with an unused set-password link, and a logged-in admin
- **WHEN** teacher A removes the student, the link is then used to set the password "new-password-1", and the admin looks up the student's email
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the lookup response is 200

#### Scenario: student-enrolment/R4-S4 Another teacher's student is not found
- **GIVEN** a student assigned only to teacher B
- **WHEN** teacher A removes that student, and then an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`, and the student is still in teacher B's list

#### Scenario: student-enrolment/R4-S5 Removing twice is not found
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student twice
- **THEN** the first response is 204 and the second is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S6 Malformed id is not found
- **GIVEN** a logged-in teacher
- **WHEN** the teacher removes the id "not-a-uuid"
- **THEN** the response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S7 Removed student cannot be registered again
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student and then registers a student with the same email
- **THEN** the removal response is 204, the registration response is 409 with code `STUDENT_EXISTS`, and the student is not in teacher A's list

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

#### Scenario: student-enrolment/R4-S12 Removal with another teacher left keeps the unused link
- **GIVEN** a student without a password, assigned to teachers A and B, with an unused set-password link
- **WHEN** teacher A removes the student and the link is then used to set the password "new-password-1"
- **THEN** the removal and link responses are successful, the student logs in with "new-password-1", and teacher B is in the student's list of teachers

### Requirement: R5 Student sees their teachers
A student SHALL be able to list the active teachers assigned to them, in name order as in
R2, each with id and name only. The student endpoint SHALL be allowed only for students.

#### Scenario: student-enrolment/R5-S1 Student sees only their own teachers
- **GIVEN** a student assigned to teachers A and B, and teacher C with another student
- **WHEN** the student lists their teachers
- **THEN** the response is 200, the list contains A and B only, and each entry has only the fields id and name

#### Scenario: student-enrolment/R5-S2 Student without teachers gets an empty list
- **GIVEN** a logged-in student with no teachers
- **WHEN** the student lists their teachers
- **THEN** the response is 200 with an empty list

#### Scenario: student-enrolment/R5-S3 Deactivated teacher is hidden
- **GIVEN** a student assigned to an active teacher and a deactivated teacher
- **WHEN** the student lists their teachers
- **THEN** the list contains the active teacher only

#### Scenario: student-enrolment/R5-S4 Reactivated teacher appears again
- **GIVEN** a student assigned to a deactivated teacher who has a password
- **WHEN** the admin reactivates the teacher and the student lists their teachers
- **THEN** the list contains that teacher

#### Scenario: student-enrolment/R5-S5 Name order ignores case and follows the alphabet
- **GIVEN** a student with teachers "Яков", "борис" and "Anna"
- **WHEN** the student lists their teachers
- **THEN** the order is "Anna", "борис", "Яков"

#### Scenario: student-enrolment/R5-S6 Teachers and admins cannot call the student endpoint
- **GIVEN** a logged-in teacher and a logged-in admin
- **WHEN** each calls the student endpoint that lists teachers
- **THEN** both responses are 403

#### Scenario: student-enrolment/R5-S7 Student endpoint requires a session
- **GIVEN** no session
- **WHEN** a client calls the student endpoint that lists teachers
- **THEN** the response is 401 with code `UNAUTHENTICATED`

### Requirement: R6 Admin finds a student by email
An admin SHALL be able to find a student account by email, using the same email rule as
R1, and get the student's id, name, email, time zone and active flag. An email with no
student account MUST return 404.

#### Scenario: student-enrolment/R6-S1 Admin finds a student
- **GIVEN** a logged-in admin and a student account with email "student@example.com"
- **WHEN** the admin looks up "student@example.com"
- **THEN** the response is 200 with the student's id, name, email, time zone and active flag

#### Scenario: student-enrolment/R6-S2 Removed student without a password can be recovered
- **GIVEN** a logged-in admin and a student without a password, with no teachers and an expired set-password link
- **WHEN** the admin looks up the student's email, issues a reset link for the returned id, and the link is used to set the password "new-password-1"
- **THEN** the lookup response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R6-S3 Email case and surrounding whitespace are ignored
- **GIVEN** a logged-in admin and a student account with email "student@example.com"
- **WHEN** the admin looks up " STUDENT@example.com "
- **THEN** the response is 200 with that student's id

#### Scenario: student-enrolment/R6-S4 Email without a student account is not found
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin looks up an email with no account, the teacher's email, and the admin's own email
- **THEN** each response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R6-S5 Invalid email is rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin looks up a blank email, then a malformed email, then sends no email
- **THEN** each response is 400 with code `VALIDATION_FAILED`

#### Scenario: student-enrolment/R6-S6 Teachers and students cannot look up
- **GIVEN** a student account, a logged-in teacher and a logged-in student
- **WHEN** the teacher and the student each look up that student's email
- **THEN** both responses are 403

#### Scenario: student-enrolment/R6-S7 Lookup requires a session
- **GIVEN** a client with a valid CSRF token and no session
- **WHEN** the client looks up an email
- **THEN** the response is 401 with code `UNAUTHENTICATED`

#### Scenario: student-enrolment/R6-S8 Request without a CSRF token is rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin looks up an email without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`
