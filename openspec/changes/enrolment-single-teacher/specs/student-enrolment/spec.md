# Spec Delta

## MODIFIED Requirements

### Requirement: R1 Teacher registers a new student
A teacher SHALL be able to register a student by name, email and time zone; emails are
compared case-insensitively. A new email creates a student account without a password,
assigned to the teacher, with a single-use set-password link in the response. The email of
a student with no teacher assigns that account and keeps its details; the response carries
a new set-password link only if the student has no password. Any other existing email MUST
return 409 and change nothing.

#### Scenario: student-enrolment/R1-S1 New email creates and assigns a student
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a new email and the returned set-password link is used to set the password "new-password-1"
- **THEN** the response to registering is 201 with the student's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the student appears in the teacher's student list, and the student logs in with "new-password-1" and has the role student

#### Scenario: student-enrolment/R1-S2 Existing student is not assigned
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link
- **WHEN** teacher A registers a student with that email and a different name and time zone
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in teacher A's list, the student is still in teacher B's list, the student's name and time zone are unchanged, and the unused link still sets the password

#### Scenario: student-enrolment/R1-S3 Email of a teacher or admin is rejected
- **GIVEN** a teacher account with email "t@example.com" and an admin account with email "a@example.com"
- **WHEN** another teacher registers a student with each of these emails
- **THEN** both responses are 409 with code `EMAIL_TAKEN`, and nothing is assigned

#### Scenario: student-enrolment/R1-S4 Already assigned student is rejected
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A registers a student with that email again
- **THEN** the response is 409 with code `ALREADY_ENROLLED` and there is still exactly one assignment between them

#### Scenario: student-enrolment/R1-S5 Email case is ignored
- **GIVEN** a student with email "student@example.com", assigned to teacher B
- **WHEN** teacher A registers a student with email "STUDENT@example.com"
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in teacher A's list, and no account is created

#### Scenario: student-enrolment/R1-S6 Invalid input is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a blank name, then with an invalid email, then with an unknown time zone
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S7 Email with invisible characters is rejected
- **GIVEN** a logged-in teacher and a student account with email "nina@example.com" and no teacher
- **WHEN** the teacher registers a student with email "nina @example.com", then with email " @example.com"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, no account is created, and the student is not in the teacher's list

#### Scenario: student-enrolment/R1-S8 Field limits apply
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a name of 201 characters, an email of 321 characters, a blank email, a blank time zone, no time zone, and a name of only " "
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S9 Surrounding whitespace is ignored
- **GIVEN** a logged-in teacher A and a student with email "nina@example.com", assigned to teacher B
- **WHEN** teacher A registers a student with a 199-character name followed by two spaces and email " anna@example.com ", then with email " nina@example.com "
- **THEN** the first response is 201 and the account stores the name and email without the whitespace, and the second response is 409 with code `STUDENT_EXISTS`

#### Scenario: student-enrolment/R1-S10 Submitted values are stored
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with name "Анна", email "anna@example.com" and time zone "Europe/Moscow"
- **THEN** the student's details return that name, email and time zone

#### Scenario: student-enrolment/R1-S11 Request without a CSRF token is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and no account is created

#### Scenario: student-enrolment/R1-S12 Failed registration leaves nothing behind
- **GIVEN** a logged-in teacher and a failure while the set-password link is issued
- **WHEN** the teacher registers a student with a new email
- **THEN** the response is 500, and neither an account nor an assignment is created

#### Scenario: student-enrolment/R1-S13 Parallel requests create one account
- **GIVEN** two logged-in teachers
- **WHEN** both send several parallel requests to register the same new email
- **THEN** exactly one response is 201, all others are 409, and there is exactly one account, one assignment and one unused link

#### Scenario: student-enrolment/R1-S14 Student with no teacher and a password is assigned
- **GIVEN** a student with no teacher, with name "Анна", time zone "Europe/Moscow" and the password "old-password-1"
- **WHEN** teacher A registers a student with that email, name "Anna" and time zone "Europe/Berlin"
- **THEN** the response is 201 with the student's id and no set-password link, the student is in teacher A's list with name "Анна" and time zone "Europe/Moscow", teacher A is in the student's list of teachers, and the student logs in with "old-password-1"

#### Scenario: student-enrolment/R1-S15 Student with no teacher and no password gets a new link
- **GIVEN** a student without a password and with no teacher, whose set-password link was invalidated
- **WHEN** teacher A registers a student with that email and the returned set-password link is used to set the password "new-password-1"
- **THEN** the response to registering is 201 with the student's id and a set-password link, the student is in teacher A's list, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R1-S16 Parallel requests assign a student with no teacher once
- **GIVEN** a student with no teacher and two logged-in teachers
- **WHEN** both teachers send several parallel requests to register that student's email
- **THEN** exactly one response is 201, all others are 409, and the student has exactly one assignment

#### Scenario: student-enrolment/R1-S17 Failed assignment leaves nothing behind
- **GIVEN** a logged-in teacher, a student without a password and with no teacher, and a failure while the set-password link is issued
- **WHEN** the teacher registers a student with that email
- **THEN** the response is 500, and the student is not in the teacher's list

### Requirement: R3 Admin sees assignments
An admin SHALL be able to list the students assigned to any teacher, in name order as in R2,
and the teachers of any student, a list with at most one entry. Each entry has id, name,
email, time zone and active flag. An id that is not a teacher or a student, respectively,
including a malformed id, MUST return 404.

#### Scenario: student-enrolment/R3-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** the admin lists teacher A's students
- **THEN** the response is 200 and the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R3-S2 Id that is not a teacher is not found
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin lists the students of the student's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S3 Admin lists a student's teachers
- **GIVEN** a student assigned to teacher A, and teacher C with another student
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 and the list contains A only, with id, name, email and time zone

#### Scenario: student-enrolment/R3-S4 Id that is not a student is not found
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin lists the teachers of the teacher's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S5 Malformed id is not found
- **GIVEN** a logged-in admin
- **WHEN** the admin lists the students of the id "not-a-uuid", and then the teachers of the id "not-a-uuid"
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S6 Teachers and students cannot call admin endpoints
- **GIVEN** teacher A, teacher B with a student, and a logged-in student
- **WHEN** teacher A and the student list teacher B's students and the student's teachers through the admin endpoints
- **THEN** all responses are 403

#### Scenario: student-enrolment/R3-S7 Deactivated teacher is shown as inactive
- **GIVEN** a student assigned to a deactivated teacher, and another student assigned to an active teacher
- **WHEN** the admin lists each student's teachers
- **THEN** the first list contains the deactivated teacher with active false, and the second the active teacher with active true

#### Scenario: student-enrolment/R3-S8 Admin lists use the same name order
- **GIVEN** a teacher with students "Boris", "anna", "Ёлка", "Анна" and "яна"
- **WHEN** the admin lists the teacher's students
- **THEN** the students are in the order "anna", "Boris", "Анна", "Ёлка", "яна"

#### Scenario: student-enrolment/R3-S9 Student with no teacher has an empty list
- **GIVEN** a logged-in admin and a student with no teacher
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 with an empty list

## REMOVED Requirements

### Requirement: R4 Teacher removes a student
**Reason**: A student has at most one teacher, so the scenarios for a student with several teachers (R4-S2, R4-S12) no longer apply, and a removed student can now be registered again (R4-S7). OpenSpec cannot drop or rename scenarios inside a modified requirement, so R4 is replaced as a whole.
**Migration**: Replaced by "R4 Teacher removes their student" below, which keeps the scenario IDs that still apply.

### Requirement: R5 Student sees their teachers
**Reason**: A student has at most one teacher, so the name-order scenario (R5-S5) no longer applies. OpenSpec cannot drop scenarios inside a modified requirement, so R5 is replaced as a whole.
**Migration**: Replaced by "R5 Student sees their teacher" below, which keeps the scenario IDs that still apply.

## ADDED Requirements

### Requirement: R4 Teacher removes their student
A teacher SHALL be able to remove a student assigned to them. Removal deletes the
assignment and keeps the student's account, which then has no teacher and can be registered
again as in R1. Removal MUST also invalidate the student's unused set-password link. An id
that is not a student assigned to the teacher MUST return 404, as in R2.

#### Scenario: student-enrolment/R4-S1 Removed student disappears from the lists
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A removes the student
- **THEN** the response is 204, the student is not in teacher A's list, teacher A is not in the student's list of teachers, and the student can still log in

#### Scenario: student-enrolment/R4-S3 Removal keeps the account and invalidates the unused link
- **GIVEN** a student without a password, assigned to teacher A, with an unused set-password link, and a logged-in admin
- **WHEN** teacher A removes the student, the link is then used to set the password "new-password-1", and the admin looks up the student's email
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the lookup response is 200

#### Scenario: student-enrolment/R4-S4 Another teacher's student is not found
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link
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
- **THEN** the removal response is 204, teacher B's response is 201, teacher A's response is 409 with code `STUDENT_EXISTS`, the student is in teacher B's list and not in teacher A's, and teacher B is the only entry in the student's list of teachers

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

### Requirement: R5 Student sees their teacher
A student SHALL be able to list their teachers: a list with at most one entry, which holds
their teacher if that teacher is active, with id and name only. The student endpoint SHALL
be allowed only for students.

#### Scenario: student-enrolment/R5-S1 Student sees only their own teacher
- **GIVEN** a student assigned to teacher A, and teacher C with another student
- **WHEN** the student lists their teachers
- **THEN** the response is 200, the list contains A only, and the entry has only the fields id and name

#### Scenario: student-enrolment/R5-S2 Student without teachers gets an empty list
- **GIVEN** a logged-in student with no teachers
- **WHEN** the student lists their teachers
- **THEN** the response is 200 with an empty list

#### Scenario: student-enrolment/R5-S3 Deactivated teacher is hidden
- **GIVEN** a student assigned to a deactivated teacher
- **WHEN** the student lists their teachers
- **THEN** the response is 200 with an empty list

#### Scenario: student-enrolment/R5-S4 Reactivated teacher appears again
- **GIVEN** a student assigned to a deactivated teacher who has a password
- **WHEN** the admin reactivates the teacher and the student lists their teachers
- **THEN** the list contains that teacher

#### Scenario: student-enrolment/R5-S6 Teachers and admins cannot call the student endpoint
- **GIVEN** a logged-in teacher and a logged-in admin
- **WHEN** each calls the student endpoint that lists teachers
- **THEN** both responses are 403

#### Scenario: student-enrolment/R5-S7 Student endpoint requires a session
- **GIVEN** no session
- **WHEN** a client calls the student endpoint that lists teachers
- **THEN** the response is 401 with code `UNAUTHENTICATED`
