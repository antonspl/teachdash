## MODIFIED Requirements

### Requirement: R5 A teacher deletes a lesson
`DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}` SHALL delete the lesson of the caller's course and return 204 with no body. Lessons above it move down by one and keep their `updatedAt`, so positions stay 1..count. A second delete gives 404, and a deletion frees a slot under the R1 limit. Not found cases and the order of checks are as R3, without the input step. Deleting a lesson also deletes its parts.

#### Scenario: lessons/R5-S1 Delete a lesson in the middle
- **GIVEN** a logged-in teacher A with the course "English B1" with the lessons "One", "Two", "Three" and "Four" at the positions 1 to 4 created at a time T, and the clock advanced by one hour
- **WHEN** teacher A deletes "Two"
- **THEN** the response is 204 with an empty body, "Two" is no longer stored, the list shows "One", "Three", "Four" at the positions 1, 2, 3, all with `updatedAt` T, and the `updatedAt` of the course is unchanged

#### Scenario: lessons/R5-S2 Delete the first Russian lesson
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lessons "Падежи" and "Роды" at the positions 1 and 2
- **WHEN** teacher A deletes "Падежи"
- **THEN** the response is 204, and the course has exactly the lesson "Роды" at position 1

#### Scenario: lessons/R5-S3 A second delete is not found, and a slot is freed
- **GIVEN** a logged-in teacher A with a course with 100 lessons
- **WHEN** teacher A deletes the lesson at position 50, deletes it again, and then creates the lesson "Lesson 101" in the course
- **THEN** the first response is 204, the second is 404 with code `NOT_FOUND` and no `errors` property, the third is 201 with `position` 100, and the course has 100 lessons with the positions 1 to 100 without gaps

#### Scenario: lessons/R5-S4 Another teacher's, unknown, malformed and wrong-course ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with the lesson "Падежи"
- **WHEN** teacher A deletes teacher B's lesson (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", "Fractions" under "English B1", and "Fractions" under the course "not-a-uuid" and under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика" or "Падежи", and both lessons are still stored

#### Scenario: lessons/R5-S5 Only active teachers delete lessons
- **GIVEN** a course of teacher A with a lesson, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each delete the lesson, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the lesson is still stored

#### Scenario: lessons/R5-S6 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A who is then deactivated
- **WHEN** the admin and teacher A each delete the lesson "not-a-uuid" in the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A's is 401 with code `UNAUTHENTICATED`, neither is a 404

#### Scenario: lessons/R5-S7 Parallel deletes and moves keep positions dense (the teacher and course row locks serialise them)
- **GIVEN** a logged-in teacher A with a course with 10 lessons at the positions 1 to 10
- **WHEN** teacher A sends at the same time 3 delete requests for the lessons at the positions 2, 5 and 8, and 4 move requests for the lessons at the positions 1, 3, 6 and 10, each to position 1
- **THEN** the 3 delete responses are 204, the 4 move responses are 200, and the course has exactly 7 lessons with the positions 1 to 7 without gaps and without duplicates

#### Scenario: lessons/R5-S8 Deleting a lesson deletes its parts
- **GIVEN** a logged-in teacher A with a course with the lessons "Present Simple" and "Падежи", where "Present Simple" has the parts "Warm-up" and "Grammar" and "Падежи" has the part "Разминка"
- **WHEN** teacher A deletes "Present Simple"
- **THEN** the response is 204, no part of "Present Simple" is stored, and the lesson "Падежи" still has its part "Разминка" at position 1
