# Spec Delta

## Purpose

Links students to teachers: a teacher registers new students and sees only their own,
and admins see all assignments.

## ADDED Requirements

### Requirement: R1 Teacher registers a new student
A teacher SHALL be able to register a student by name, email and time zone. Emails are
compared case-insensitively. A new email creates a student account without a password,
assigned to the teacher at once, with a single-use set-password link in the response.
Any email that already has an account MUST return 409 and change nothing.

#### Scenario: student-enrolment/R1-S1 New email creates and assigns a student
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a new email and the returned set-password link is used to set the password "new-password-1"
- **THEN** the response to registering is 201 with the student's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the student appears in the teacher's student list, and the student logs in with "new-password-1" and has the role student

#### Scenario: student-enrolment/R1-S2 Existing student is not assigned
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link
- **WHEN** teacher A registers a student with that email and a different name and time zone
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in teacher A's list, the student's name and time zone are unchanged, and the unused link still sets the password

#### Scenario: student-enrolment/R1-S3 Email of a teacher or admin is rejected
- **GIVEN** a teacher account with email "t@example.com" and an admin account with email "a@example.com"
- **WHEN** another teacher registers a student with each of these emails
- **THEN** both responses are 409 with code `EMAIL_TAKEN`, and nothing is assigned

#### Scenario: student-enrolment/R1-S4 Already assigned student is rejected
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A registers a student with that email again
- **THEN** the response is 409 with code `ALREADY_ENROLLED` and there is still exactly one assignment between them

#### Scenario: student-enrolment/R1-S5 Email case is ignored
- **GIVEN** a student account with email "student@example.com"
- **WHEN** a teacher registers a student with email "STUDENT@example.com"
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in the teacher's list, and no account is created

#### Scenario: student-enrolment/R1-S6 Invalid input is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a blank name, then with an invalid email, then with an unknown time zone
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S7 Email with invisible characters is rejected
- **GIVEN** a logged-in teacher and a student account with email "nina@example.com"
- **WHEN** the teacher registers a student with email "nina\u00A0@example.com", then with email "\u2003@example.com"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S8 Field limits apply
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a name of 201 characters, an email of 321 characters, a blank email, a blank time zone, no time zone, and a name of only "\u00A0"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S9 Surrounding whitespace is ignored
- **GIVEN** a logged-in teacher and a student account with email "nina@example.com"
- **WHEN** the teacher registers a student with a 199-character name followed by two spaces and email " anna@example.com ", then with email " nina@example.com "
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

### Requirement: R2 Teacher sees only own students
A teacher's student list SHALL contain exactly the students assigned to that teacher,
ordered by name, each with id, name, email, time zone and active flag. Any teacher request about an
id that is not a student assigned to them, including a malformed id, MUST return 404. Teacher endpoints SHALL be
allowed only for teachers.

#### Scenario: student-enrolment/R2-S1 List shows only own students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** teacher A lists students
- **THEN** the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R2-S2 Another teacher's student is not found
- **GIVEN** a student assigned only to teacher B
- **WHEN** teacher A requests that student's details, and then the details of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R2-S3 Student cannot call teacher endpoints
- **GIVEN** a logged-in student
- **WHEN** the student calls the teacher endpoint that lists students
- **THEN** the response is 403

#### Scenario: student-enrolment/R2-S4 Teacher reads own student's details
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A requests that student's details
- **THEN** the response is 200 with the student's id, name, email and time zone

#### Scenario: student-enrolment/R2-S5 Admin cannot call teacher endpoints
- **GIVEN** a logged-in admin
- **WHEN** the admin calls the teacher endpoint that lists students
- **THEN** the response is 403

#### Scenario: student-enrolment/R2-S6 Malformed id is not found
- **GIVEN** a logged-in teacher
- **WHEN** the teacher requests the details of the id "not-a-uuid"
- **THEN** the response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R2-S7 Students and admins cannot register or read details
- **GIVEN** a logged-in student and a logged-in admin
- **WHEN** each registers a student and requests a student's details
- **THEN** all responses are 403, and no account is created

#### Scenario: student-enrolment/R2-S8 Teacher endpoints require a session
- **GIVEN** no session
- **WHEN** a client calls the teacher endpoints that list students and read details
- **THEN** both responses are 401 with code `UNAUTHENTICATED`

### Requirement: R3 Admin sees assignments
An admin SHALL be able to list the students assigned to any teacher and the teachers of
any student, ordered by name, each with id, name, email, time zone and active flag. An id that is not
a teacher or a student, respectively, including a malformed id, MUST return 404.

#### Scenario: student-enrolment/R3-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** the admin lists teacher A's students
- **THEN** the response is 200 and the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R3-S2 Id that is not a teacher is not found
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin lists the students of the student's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S3 Admin lists a student's teachers
- **GIVEN** a student assigned to teachers A and B, and teacher C without that student
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 and the list contains A and B only, ordered by name, each with id, name, email and time zone

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
- **GIVEN** a student assigned to an active teacher and a deactivated teacher
- **WHEN** the admin lists the student's teachers
- **THEN** both teachers are listed, the active one with active true and the deactivated one with active false
