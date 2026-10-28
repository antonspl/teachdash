# Spec Delta

## Purpose

Links students to teachers: teachers add students directly or through a join link,
see only their own students, and a student can study with several teachers.

## ADDED Requirements

### Requirement: R1 Teacher creates a student
A teacher SHALL be able to add a student by name, email and time zone. If no account
has that email, a student account is created, linked to the teacher, and the response
contains a single-use set-password link. If a student account has that email, it is
linked to the teacher and no link is returned.

#### Scenario: student-enrolment/R1-S1 New email creates and links a student
- **GIVEN** a logged-in teacher
- **WHEN** the teacher adds a student with a new email
- **THEN** the response is 201 with the student's id and a set-password link, and the student appears in the teacher's student list

#### Scenario: student-enrolment/R1-S2 Existing student is linked without a new account
- **GIVEN** a student account linked to teacher B
- **WHEN** teacher A adds a student with that email
- **THEN** the response is 200 without a link, and the student is linked to both A and B

#### Scenario: student-enrolment/R1-S3 Email of a teacher or admin is rejected
- **GIVEN** a teacher account with email "t@example.com"
- **WHEN** another teacher adds a student with email "t@example.com"
- **THEN** the response is 409 and nothing is linked

#### Scenario: student-enrolment/R1-S4 Already linked student is rejected
- **GIVEN** a student linked to teacher A
- **WHEN** teacher A adds a student with that email again
- **THEN** the response is 409

### Requirement: R2 Teacher join link
A teacher SHALL have at most one active join link. The teacher SHALL be able to see
it, generate a new one (the old one stops working) and disable it.

#### Scenario: student-enrolment/R2-S1 Regenerating invalidates the old link
- **GIVEN** a teacher with an active join link
- **WHEN** the teacher generates a new join link
- **THEN** joining with the old link returns 404 and joining with the new link works

#### Scenario: student-enrolment/R2-S2 Disabled link does not work
- **GIVEN** a teacher who disabled their join link
- **WHEN** someone joins with that link
- **THEN** the response is 404

### Requirement: R3 Joining with a join link
A visitor without a session SHALL be able to register as a student through an active
join link with name, email, password and time zone; the new student is linked to the
teacher and logged in. A logged-in student using the link SHALL be linked to the
teacher. Teachers and admins MUST NOT join.

#### Scenario: student-enrolment/R3-S1 New visitor registers and is linked
- **GIVEN** a teacher with an active join link
- **WHEN** a visitor joins with a new email and a valid password
- **THEN** the response is 201, a session is started, and the student appears in the teacher's student list

#### Scenario: student-enrolment/R3-S2 Logged-in student is linked to another teacher
- **GIVEN** a logged-in student linked to teacher B and teacher A's active join link
- **WHEN** the student joins with teacher A's link
- **THEN** the response is 200 and the student is linked to both A and B

#### Scenario: student-enrolment/R3-S3 Registered email must log in first
- **GIVEN** an existing account with email "s@example.com"
- **WHEN** a visitor without a session joins with email "s@example.com"
- **THEN** the response is 409 and no account is created or linked

#### Scenario: student-enrolment/R3-S4 Joining twice is harmless
- **GIVEN** a logged-in student already linked to teacher A
- **WHEN** the student joins with teacher A's link
- **THEN** the response is 200 and there is still exactly one link between them

#### Scenario: student-enrolment/R3-S5 Teacher cannot join
- **GIVEN** a logged-in teacher and another teacher's active join link
- **WHEN** the teacher joins with the link
- **THEN** the response is 403

### Requirement: R4 Teacher sees only own students
A teacher's student list SHALL contain exactly the students linked to that teacher.
Any teacher request about a student not linked to them MUST return 404.

#### Scenario: student-enrolment/R4-S1 List shows only own students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** teacher A lists students
- **THEN** the list contains S1 and S2 only

#### Scenario: student-enrolment/R4-S2 Another teacher's student is not found
- **GIVEN** a student linked only to teacher B
- **WHEN** teacher A requests that student's details
- **THEN** the response is 404

### Requirement: R5 Teacher removes a student
A teacher SHALL be able to remove a student from their list. This removes only the
link to that teacher; the student account and links to other teachers stay.

#### Scenario: student-enrolment/R5-S1 Removed student disappears from the list
- **GIVEN** a student linked to teacher A
- **WHEN** teacher A removes the student
- **THEN** the student is not in A's list, A is not in the student's teacher list, and the student can still log in

#### Scenario: student-enrolment/R5-S2 Links to other teachers stay
- **GIVEN** a student linked to teachers A and B
- **WHEN** teacher A removes the student
- **THEN** the student is still in teacher B's list

### Requirement: R6 Student sees their teachers
A student SHALL be able to list the teachers they are linked to.

#### Scenario: student-enrolment/R6-S1 Student with two teachers sees both
- **GIVEN** a student linked to teachers A and B
- **WHEN** the student lists their teachers
- **THEN** the list contains A and B with their names

### Requirement: R7 Admin sees all teachers and students
An admin SHALL be able to list all teachers and, for any teacher, the students linked
to them.

#### Scenario: student-enrolment/R7-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2
- **WHEN** the admin lists teacher A's students
- **THEN** the list contains S1 and S2
