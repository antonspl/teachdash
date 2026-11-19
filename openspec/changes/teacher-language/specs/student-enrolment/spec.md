# Spec Delta

## MODIFIED Requirements

### Requirement: R3 Admin sees a teacher's students and a student's teacher
An admin SHALL be able to list the students assigned to any teacher, in name order as in R2,
and the teachers of any student, a list with at most one entry. Each entry has id, name,
email, time zone and active flag, and a student's or a teacher's entry also the native language (R15). An id that is not a teacher or a student, respectively,
including a malformed id, MUST return 404.

#### Scenario: student-enrolment/R3-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** the admin lists teacher A's students
- **THEN** the response is 200 and the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R3-S2 Id that is not a teacher is not found
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin lists the students of the student's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S3 Admin lists a student's teacher
- **GIVEN** a student assigned to teacher A, and teacher C with another student
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 and the list contains A only, with id, name, email, time zone, active flag and `nativeLanguage`

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

#### Scenario: student-enrolment/R3-S8 Admin list of students uses the same name order
- **GIVEN** a teacher with students "Boris", "anna", "Ёлка", "Анна" and "яна"
- **WHEN** the admin lists the teacher's students
- **THEN** the students are in the order "anna", "Boris", "Анна", "Ёлка", "яна"

#### Scenario: student-enrolment/R3-S9 Student with no teacher has an empty list
- **GIVEN** a logged-in admin and a student with no teacher
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 with an empty list

### Requirement: R15 Student data shows the native language
Every response that returns a student's details to their teacher (R2), to an admin
(R3, R6) or to the student (their current-user data, as from `GET /api/me`, login and setting a password)
SHALL include `nativeLanguage`. So SHALL a teacher's current-user data (the same
endpoints), the admin's teacher list (accounts R12) and every entry for a teacher in the
admin's lists (R3), which extends R3's entry fields. Responses about an admin, and the
response to creating a teacher (accounts R5), MUST NOT include it. The student MUST NOT be
able to change it through the profile (accounts R9: the field is ignored); a teacher
changes their own through it.

#### Scenario: student-enrolment/R15-S1 The teacher sees it
- **GIVEN** teacher A with students "Анна" (native language "ru") and "Ben" (native language "en")
- **WHEN** teacher A lists students and reads Анна's details
- **THEN** the list shows `nativeLanguage` "ru" for Анна and "en" for Ben, and the details show "ru"

#### Scenario: student-enrolment/R15-S2 The admin sees it on students only
- **GIVEN** a logged-in admin, and teacher A with native language "fr" and student S, email "s@example.com", native language "pt"
- **WHEN** the admin lists teacher A's students, looks up "s@example.com" and lists S's teachers
- **THEN** the first two responses show `nativeLanguage` "pt" for S, and the entry for teacher A in the third shows `nativeLanguage` "fr"

#### Scenario: student-enrolment/R15-S3 The student sees it and cannot change it
- **GIVEN** a logged-in student with the name "Анна" and native language "ru", and a logged-in teacher with native language "fr"
- **WHEN** the student logs in, reads the current user, sets the name "Anna" with the native language "de" in the same request, and reads the current user again; and the teacher reads the current user
- **THEN** the student's responses show `nativeLanguage` "ru", the second read shows the name "Anna", and the teacher's response shows `nativeLanguage` "fr"

#### Scenario: student-enrolment/R15-S4 Setting a password returns it
- **GIVEN** teacher A with a student with no password, native language "sr" and a valid set-password link
- **WHEN** the student sets the password "password-1" through the link
- **THEN** the response shows `nativeLanguage` "sr"

#### Scenario: student-enrolment/R15-S5 A teacher's login and password link return it, an admin's data does not
- **GIVEN** a teacher with native language "es", no password and a valid set-password link, and a logged-in admin
- **WHEN** the teacher sets the password "password-1" through the link and then logs in with it, and the admin logs in and reads the current user
- **THEN** both of the teacher's responses show `nativeLanguage` "es", and the admin's responses have no `nativeLanguage` property
