# Spec Delta

## Purpose

Links students to teachers: a teacher registers new students and sees only their own,
and admins see any teacher's students. A student can study with several teachers, and
an existing student assigns themselves through a link from the teacher.

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

### Requirement: R2 Teacher sees only own students
A teacher's student list SHALL contain exactly the students assigned to that teacher,
ordered by name, each with id, name, email and time zone. Any teacher request about an
id that is not a student assigned to them MUST return 404. Teacher endpoints SHALL be
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

### Requirement: R3 Admin sees any teacher's students
An admin SHALL be able to list the students assigned to any teacher, ordered by name,
each with id, name, email and time zone. An id that is not a teacher MUST return 404.

#### Scenario: student-enrolment/R3-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** the admin lists teacher A's students
- **THEN** the response is 200 and the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R3-S2 Id that is not a teacher is not found
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin lists the students of the student's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`
