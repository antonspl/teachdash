# lessons Specification

## Purpose
Defines lessons: the ordered units of teaching content inside a teacher's course, which
a teacher creates and lists.

## Requirements

### Requirement: R1 A teacher creates a lesson in a course
`POST /api/teacher/courses/{courseId}/lessons` with `{title}` SHALL add a lesson at the end of the caller's course and return 201 with `{id, title, position, createdAt, updatedAt}`. The title follows courses R1, not unique. A 101st lesson gives 409 `LESSON_LIMIT_REACHED`; a foreign, unknown or malformed course gives 404. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign course (404), limit (409).

#### Scenario: lessons/R1-S1 Create an English lesson
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en")
- **WHEN** teacher A creates a lesson in it with the title "Present Simple"
- **THEN** the response is 201 with a new `id`, the title "Present Simple", `position` 1, a `createdAt` time and an `updatedAt` equal to `createdAt`, exactly these properties and no `teacherId` or `courseId`, and the lesson is stored owned by teacher A and linked to the course at position 1

#### Scenario: lessons/R1-S2 Create a Russian lesson
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target)
- **WHEN** teacher A creates a lesson in it with the title "Падежи: именительный, родительный?"
- **THEN** the response is 201 with exactly that title and `position` 1, and the stored lesson has the same title

#### Scenario: lessons/R1-S3 Lessons are added at the end, per course
- **GIVEN** a logged-in teacher A with the courses "English B1" and "Maths", where "English B1" already has the lessons "One" and "Two", created at a time T, and the clock advanced by one hour
- **WHEN** teacher A creates the lesson "Three" in "English B1" and the lesson "First" in "Maths"
- **THEN** the responses are 201 with `position` 3 and `position` 1, "English B1" has the positions 1, 2, 3 without gaps, the lessons "One" and "Two" keep their positions 1 and 2 and their `updatedAt` T, and the `updatedAt` of both courses is unchanged

#### Scenario: lessons/R1-S4 The title is normalised
- **GIVEN** a logged-in teacher A with a course
- **WHEN** teacher A creates a lesson with the title "  Грамматика\u200B  " (ending in a zero-width space)
- **THEN** the response is 201 with the title "Грамматика", and the stored lesson has the same title

#### Scenario: lessons/R1-S5 A title must have 1-200 characters and a letter or digit
- **GIVEN** a logged-in teacher A with a course
- **WHEN** teacher A creates lessons with each title "   ", 201 Cyrillic letters ("Я"), "!!!", and 200 Cyrillic letters ("Я"), with the body `{}` and the body `{"title": null}`, and with the bodies `{"title":` and no body
- **THEN** the first, the fifth and the sixth responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the second is `[{field: "title", code: "TOO_LONG"}]`, the third is `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 201, the seventh and the eighth are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the course has exactly one lesson

#### Scenario: lessons/R1-S6 Wrong JSON types and a number or a boolean are rejected
- **GIVEN** a logged-in teacher A with a course
- **WHEN** teacher A creates a lesson with the title `["Grammar"]`, then with the title `123`, then with the title `true`
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]`, no body contains "Grammar", or "123" or "true" as a value, and the course has no lesson

#### Scenario: lessons/R1-S7 Titles are not unique
- **GIVEN** a logged-in teacher A with the courses "English B1" and "English B2", where "English B1" has the lesson "Grammar"
- **WHEN** teacher A creates the lessons "Grammar" and "GRAMMAR" in "English B1", and "Grammar" in "English B2"
- **THEN** all three responses are 201, and "English B1" has three lessons at the positions 1, 2, 3

#### Scenario: lessons/R1-S8 At most 100 lessons per course
- **GIVEN** a logged-in teacher A with a course "Full" with 100 lessons and a course "Other" with 99 lessons
- **WHEN** teacher A creates the lesson "Lesson 101" in "Full", then a lesson with a blank title in "Full", and then the lesson "Lesson 100" in "Other"
- **THEN** the first response is 409 with code `LESSON_LIMIT_REACHED` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the third is 201 with `position` 100, "Full" has 100 lessons and "Other" has 100 lessons

#### Scenario: lessons/R1-S9 Parallel requests keep positions distinct and the limit (the teacher and course row locks serialise them)
- **GIVEN** a logged-in teacher A with a course with 97 lessons
- **WHEN** teacher A sends 6 create requests at the same time with the titles "New 1" to "New 6"
- **THEN** exactly 3 responses are 201 with the distinct positions 98, 99 and 100, the other 3 are 409 `LESSON_LIMIT_REACHED`, and the course has exactly 100 lessons with the positions 1 to 100 without gaps

#### Scenario: lessons/R1-S10 The lesson belongs to the caller
- **GIVEN** teachers A and B, with teacher A logged in and owning a course
- **WHEN** teacher A creates a lesson in it with the extra body field `teacherId` set to teacher B's id
- **THEN** the response is 201, the lesson is owned by teacher A, and teacher B has no lesson

#### Scenario: lessons/R1-S11 Another teacher's, unknown and malformed course ids are not found
- **GIVEN** teacher A logged in, and a course "Грамматика" of teacher B with 100 lessons, one of them "Падежи"
- **WHEN** teacher A creates a lesson with the title "Intruder" in teacher B's course, in a course with an unknown id and in the course "not-a-uuid"
- **THEN** all three responses are 404 with code `NOT_FOUND` and no `errors` property (so ownership is checked before the limit), none contains "Грамматика" or "Падежи", and teacher B's course still has exactly 100 lessons

#### Scenario: lessons/R1-S12 Only active teachers create lessons
- **GIVEN** a course of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each create a lesson in the course with the title "Lesson", and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the course has no lesson

#### Scenario: lessons/R1-S13 Checks run in a fixed order
- **GIVEN** teacher A logged in with a course, and a course "Грамматика" of teacher B
- **WHEN** teacher A creates a lesson with a blank title in the course "not-a-uuid", and then a lesson with a blank title in teacher B's course
- **THEN** the first response is 404 with code `NOT_FOUND` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` and does not contain "Грамматика", and teacher B's course has no new lesson

### Requirement: R2 A teacher lists the lessons of a course
`GET /api/teacher/courses/{courseId}/lessons` SHALL return 200 with the lessons of the caller's course only, as an unpaginated JSON array ordered by `position`, each with the fields R1 returns. No lessons gives an empty array; another teacher's, unknown or malformed course gives 404 `NOT_FOUND`. Checks run in order as R1, without the input and limit steps.

#### Scenario: lessons/R2-S1 Lessons are listed by position
- **GIVEN** a logged-in teacher A with a course with the lessons "Падежи" at position 2, "Present Simple" at position 3 and "Alphabet" at position 1, inserted in that order
- **WHEN** teacher A lists the lessons of the course
- **THEN** the response is 200 with the titles "Alphabet", "Падежи", "Present Simple" in that order, with the positions 1, 2, 3

#### Scenario: lessons/R2-S2 A list item has the lesson fields
- **GIVEN** a logged-in teacher A with a course with the lesson "Падежи" created at one time
- **WHEN** teacher A lists the lessons of the course
- **THEN** the item has exactly the properties `id`, `title`, `position`, `createdAt` and `updatedAt`, with the title "Падежи", `position` 1, and `updatedAt` equal to `createdAt`

#### Scenario: lessons/R2-S3 A course without lessons gives an empty list
- **GIVEN** a logged-in teacher A with a course with no lessons and another course with one lesson
- **WHEN** teacher A lists the lessons of the first course
- **THEN** the response is 200 with an empty array

#### Scenario: lessons/R2-S4 The list shows only that course's lessons
- **GIVEN** a logged-in teacher A with the courses "English B1" with the lesson "Present Simple" and "Maths" with the lesson "Fractions", and a teacher B with a course with the lesson "Present Simple"
- **WHEN** teacher A lists the lessons of "English B1"
- **THEN** the response is 200 with exactly one item, the lesson "Present Simple" of "English B1"

#### Scenario: lessons/R2-S5 Another teacher's, unknown and malformed course ids are not found
- **GIVEN** teacher A logged in, and a course "Грамматика" of teacher B with the lesson "Падежи"
- **WHEN** teacher A lists the lessons of teacher B's course, of a course with an unknown id and of the course "not-a-uuid"
- **THEN** all three responses are 404 with code `NOT_FOUND` and no `errors` property, and none contains "Грамматика" or "Падежи"

#### Scenario: lessons/R2-S6 Only active teachers list lessons
- **GIVEN** a course of teacher A with a lesson, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them lists the lessons of the course
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`

#### Scenario: lessons/R2-S7 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A who is then deactivated
- **WHEN** the admin and teacher A each list the lessons of the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A's is 401 with code `UNAUTHENTICATED`, neither is a 404
