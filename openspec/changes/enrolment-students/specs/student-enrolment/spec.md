# Spec Delta

## Purpose

Links students to teachers: teachers add students and see only their own, and a
student can study with several teachers.

## ADDED Requirements

### Requirement: R1 Teacher adds a new student
A teacher SHALL be able to add a student by name, email and time zone. If no account
has that email, a student account without a password is created and linked to the
teacher, and the response SHALL contain a single-use set-password link. If any account
has that email, compared case-insensitively, the response MUST be 409 with code
`EMAIL_TAKEN`, identical for every kind of account, and nothing changes.

#### Scenario: student-enrolment/R1-S1 New email creates and links a student
- **GIVEN** a logged-in teacher
- **WHEN** the teacher adds a student with a new email and the returned set-password link is used to set the password "new-password-1"
- **THEN** the response to adding is 201 with the student's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the student appears in the teacher's student list, and the student logs in with "new-password-1" and has the role student

#### Scenario: student-enrolment/R1-S2 Email of another teacher's student is rejected
- **GIVEN** a student without a password, linked to teacher B, with an unused set-password link
- **WHEN** teacher A adds a student with that email
- **THEN** the response is 409 with code `EMAIL_TAKEN` and no link, the student is not linked to teacher A, the student's name and time zone are unchanged, and the unused link still sets the password

#### Scenario: student-enrolment/R1-S3 Email of a teacher or admin is rejected
- **GIVEN** a teacher account with email "t@example.com" and an admin account with email "a@example.com"
- **WHEN** another teacher adds a student with each of these emails
- **THEN** both responses are 409 with code `EMAIL_TAKEN`, and nothing is linked

#### Scenario: student-enrolment/R1-S4 Already linked student is rejected
- **GIVEN** a student linked to teacher A
- **WHEN** teacher A adds a student with that email again
- **THEN** the response is 409 with code `EMAIL_TAKEN` and there is still exactly one link between them

#### Scenario: student-enrolment/R1-S5 Email case is ignored
- **GIVEN** a student account with email "student@example.com"
- **WHEN** a teacher adds a student with email "STUDENT@example.com"
- **THEN** the response is 409 with code `EMAIL_TAKEN` and no account is created

#### Scenario: student-enrolment/R1-S6 Invalid input is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher adds a student with a blank name, then with an invalid email, then with an unknown time zone
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

### Requirement: R4 Teacher sees only own students
A teacher's student list SHALL contain exactly the students linked to that teacher,
ordered by name, each with id, name, email and time zone. Any teacher request about an
id that is not a student linked to them MUST return 404. Teacher endpoints SHALL be
allowed only for teachers.

#### Scenario: student-enrolment/R4-S1 List shows only own students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** teacher A lists students
- **THEN** the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R4-S2 Another teacher's student is not found
- **GIVEN** a student linked only to teacher B
- **WHEN** teacher A requests that student's details, and then the details of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S3 Student cannot call teacher endpoints
- **GIVEN** a logged-in student
- **WHEN** the student calls the teacher endpoint that lists students
- **THEN** the response is 403

#### Scenario: student-enrolment/R4-S4 Teacher reads own student's details
- **GIVEN** a student linked to teacher A
- **WHEN** teacher A requests that student's details
- **THEN** the response is 200 with the student's id, name, email and time zone

#### Scenario: student-enrolment/R4-S5 Admin cannot call teacher endpoints
- **GIVEN** a logged-in admin
- **WHEN** the admin calls the teacher endpoint that lists students
- **THEN** the response is 403
