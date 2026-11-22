## ADDED Requirements

### Requirement: R1 A teacher assigns an own course to an own student
`PUT /api/teacher/courses/{courseId}/students/{studentId}` SHALL assign the caller's course to the caller's student and return 204 with no body, also when already assigned (the original assignment time is kept). No request body, no limit beyond the teacher's own students. A foreign, unknown or malformed course, and an unknown, foreign, malformed or non-student id, give 404 `NOT_FOUND`. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), course (404), student (404).

#### Scenario: course-assignment/R1-S1 Assign an English course to a student
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") and the student "Anna Smith", and the clock fixed
- **WHEN** teacher A assigns the course to the student
- **THEN** the response is 204 with an empty body, and exactly one assignment is stored, for that course and that student, owned by teacher A, with a creation time equal to the fixed clock value

#### Scenario: course-assignment/R1-S2 Assign a Russian course to a student with a Cyrillic name
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) and the student "Анна Иванова"
- **WHEN** teacher A assigns the course to the student
- **THEN** the response is 204 with an empty body, and the student is stored as assigned to the course

#### Scenario: course-assignment/R1-S3 Assigning again is a no-op
- **GIVEN** a logged-in teacher A with a course and a student, assigned at a time T, and the clock advanced by one hour
- **WHEN** teacher A assigns the course to the student again
- **THEN** the response is 204 with an empty body, exactly one assignment is stored, and its creation time is still T

#### Scenario: course-assignment/R1-S4 Many courses and many students, assignments are independent
- **GIVEN** a logged-in teacher A with the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Борис", where "Anna" is already assigned to "English B1"
- **WHEN** teacher A assigns "English B1" to "Борис", and "Литература 9 класс" to "Anna" and to "Борис"
- **THEN** all responses are 204, and exactly four assignments are stored: both students in both courses, with the assignment of "Anna" to "English B1" unchanged

#### Scenario: course-assignment/R1-S5 Another teacher's, unknown and malformed ids are not found
- **GIVEN** teacher A logged in with a course "English B1", a student "Anna" of teacher A and a student "Борис" of teacher B, a student "Вера" with no teacher, an admin, and teacher B's course "Грамматика"
- **WHEN** teacher A assigns "English B1" to "Борис", to "Вера", to the admin's id, to teacher B's id, to an unknown id and to the id "not-a-uuid", and assigns teacher B's course, a course with an unknown id and the course "not-a-uuid" to "Anna"
- **THEN** all nine responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Борис", "Вера" or "Грамматика", and no assignment is stored

#### Scenario: course-assignment/R1-S6 Only active teachers assign
- **GIVEN** a course and a student of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each assign the course to the student, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and no assignment is stored

#### Scenario: course-assignment/R1-S7 Checks run in a fixed order
- **GIVEN** a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher B with a course of teacher A's and a student of teacher A's
- **WHEN** the admin and teacher A each assign the course "not-a-uuid" to the student id "not-a-uuid", and teacher B assigns the course of teacher A to a student of teacher B and then a course of teacher B to the student of teacher A
- **THEN** the admin's response is 403 with code `FORBIDDEN`, teacher A's is 401 with code `UNAUTHENTICATED`, and teacher B's two responses are 404 with code `NOT_FOUND`, none of which is a 400, and no assignment is stored

#### Scenario: course-assignment/R1-S8 Assigning in parallel with the student's removal leaves no assignment of a removed student
- **GIVEN** a logged-in teacher A with 5 courses and a student "Анна", and the clock fixed
- **WHEN** teacher A removes the student and assigns each of the 5 courses to the student at the same time
- **THEN** the removal response is 204, each assignment response is 204 or 404 `NOT_FOUND` and none is a 500, the student is no longer teacher A's student, and no assignment of the student is stored

### Requirement: R2 A teacher reads the students assigned to an own course
`GET /api/teacher/courses/{courseId}/students` SHALL return 200 with the students assigned to the caller's course, as an unpaginated array of `{id, name, assignedAt}` and no other property, ordered by name as student-enrolment R2, then by id. No students gives an empty array; a foreign, unknown or malformed course gives 404 `NOT_FOUND`. Checks run in order: role (403), deactivated (401), malformed id (404), course (404); a GET has no CSRF check.

#### Scenario: course-assignment/R2-S1 Students are listed by name, in English and Russian
- **GIVEN** a logged-in teacher A with the course "English B1" and the students "Boris", "anna", "Ёлка", "Анна", "яна" and two students both named "Zed" assigned to it, inserted in that order
- **WHEN** teacher A lists the students of the course
- **THEN** the response is 200 with the names "anna", "Boris", "Zed", "Zed", "Анна", "Ёлка", "яна" in that order, with the two "Zed" items ordered by id

#### Scenario: course-assignment/R2-S2 A list item has only the student's id, name and assignment time
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" and the student "Анна Иванова" with an email, assigned at a time T
- **WHEN** teacher A lists the students of the course
- **THEN** the item has exactly the properties `id`, `name` and `assignedAt`, with the student's id, the name "Анна Иванова" and `assignedAt` T, and the response does not contain the email

#### Scenario: course-assignment/R2-S3 A course without students gives an empty list
- **GIVEN** a logged-in teacher A with a course with no assigned students, and an own student who is assigned to another course only
- **WHEN** teacher A lists the students of the first course
- **THEN** the response is 200 with an empty array

#### Scenario: course-assignment/R2-S4 The list shows only that course's students
- **GIVEN** a logged-in teacher A with the courses "English B1" and "Maths", the students "Anna" (assigned to both), "Boris" (assigned to "Maths" only) and "Вера" (assigned to no course), and a teacher B with a course and the student "Глеб" assigned to it
- **WHEN** teacher A lists the students of "English B1"
- **THEN** the response is 200 with exactly one item, "Anna", and it does not contain "Boris", "Вера" or "Глеб"

#### Scenario: course-assignment/R2-S5 Another teacher's, unknown and malformed course ids are not found
- **GIVEN** teacher A logged in, and a course "Грамматика" of teacher B with the assigned student "Глеб"
- **WHEN** teacher A lists the students of teacher B's course, of a course with an unknown id and of the course "not-a-uuid"
- **THEN** all three responses are 404 with code `NOT_FOUND` and no `errors` property, and none contains "Грамматика" or "Глеб"

#### Scenario: course-assignment/R2-S6 Only active teachers read, and checks run in a fixed order
- **GIVEN** a course of teacher A with an assigned student, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them lists the students of the course, and the admin and teacher A then each list the students of the course "not-a-uuid"
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, and for "not-a-uuid" the admin's response is 403 and teacher A's is 401, neither a 404

### Requirement: R3 A teacher unassigns a course, and assignments end with the student's removal and the teacher's deactivation
`DELETE /api/teacher/courses/{courseId}/students/{studentId}` SHALL delete the assignment of the caller's student and course and return 204 with no body; other assignments stay. A foreign, unknown or malformed id, or a student not assigned, gives 404 `NOT_FOUND`; checks as R1. Removing a student (student-enrolment R4) SHALL delete all of the student's assignments, and deactivating a teacher (student-enrolment R7) all assignments of the teacher's courses, in the same transaction, for good.

#### Scenario: course-assignment/R3-S1 Unassign one course
- **GIVEN** a logged-in teacher A with the courses "English B1" and "Литература 9 класс" and the student "Анна", assigned to both, and the student "Boris", assigned to "English B1"
- **WHEN** teacher A unassigns "English B1" from "Анна"
- **THEN** the response is 204 with an empty body, "Анна" is assigned to "Литература 9 класс" only, "Boris" is still assigned to "English B1", and "Анна" is still teacher A's student

#### Scenario: course-assignment/R3-S2 A second unassign and an unassigned student are not found
- **GIVEN** a logged-in teacher A with a course and the students "Anna" (assigned to it) and "Борис" (assigned to no course)
- **WHEN** teacher A unassigns the course from "Anna", then again from "Anna", and then from "Борис"
- **THEN** the first response is 204, the second and the third are 404 with code `NOT_FOUND` and no `errors` property, and no assignment is stored

#### Scenario: course-assignment/R3-S3 Another teacher's, unknown and malformed ids are not found
- **GIVEN** teacher A logged in with a course "English B1" and a student "Anna" assigned to it, a student "Борис" of teacher B assigned to teacher B's course "Грамматика"
- **WHEN** teacher A unassigns "Борис" from "English B1" and from "Грамматика", unassigns "Anna" from "Грамматика", from a course with an unknown id and from the course "not-a-uuid", and unassigns an unknown student id and the student id "not-a-uuid" from "English B1"
- **THEN** all seven responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Борис" or "Грамматика", and both assignments are still stored

#### Scenario: course-assignment/R3-S4 Only active teachers unassign
- **GIVEN** a course and a student of teacher A with an assignment, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each unassign the course from the student, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the assignment is still stored

#### Scenario: course-assignment/R3-S5 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A who is then deactivated
- **WHEN** the admin and teacher A each unassign the student id "not-a-uuid" from the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A's is 401 with code `UNAUTHENTICATED`, neither is a 404

#### Scenario: course-assignment/R3-S6 Removing a student deletes all of the student's assignments
- **GIVEN** teacher A with the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Анна", both assigned to both courses, and teacher B with a course and the student "Глеб" assigned to it
- **WHEN** teacher A removes "Anna", and then registers "Anna" again as the student of teacher A
- **THEN** the removal response is 204, no assignment of "Anna" is stored, "Анна" is still assigned to both courses, "Глеб" is still assigned to teacher B's course, and teacher A's lists of the students of both courses do not contain "Anna"

#### Scenario: course-assignment/R3-S7 Deactivating a teacher deletes the assignments of the teacher's courses
- **GIVEN** teacher A with a password, the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Анна" assigned to them, and teacher B with a course and the student "Глеб" assigned to it
- **WHEN** the admin deactivates teacher A and then reactivates teacher A
- **THEN** the deactivation response is 204, no assignment of teacher A's courses is stored, teacher A's course list still has both courses, teacher B's student "Глеб" is still assigned to teacher B's course, and after the reactivation teacher A can log in again, has no student, and the lists of the students of both courses are empty
