## MODIFIED Requirements

### Requirement: R1 A teacher creates a lesson in a course
`POST /api/teacher/courses/{courseId}/lessons` with `{title}` SHALL add a lesson at the end of the caller's course and return 201 with `{id, title, position, createdAt, updatedAt}`. The title follows courses R1, not unique. A 101st lesson gives 409 `LESSON_LIMIT_REACHED`; a foreign, unknown or malformed course gives 404. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign course (404), limit (409).

#### Scenario: lessons/R1-S1 Create an English lesson
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en")
- **WHEN** teacher A creates a lesson in it with the title "Present Simple"
- **THEN** the response is 201 with a new `id`, the title "Present Simple", `position` 1, a `createdAt` time and an `updatedAt` equal to `createdAt`, exactly these properties and no `teacherId` or `courseId`, and the lesson is stored owned by teacher A in the course at position 1

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

## ADDED Requirements

### Requirement: R3 A teacher renames a lesson
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}` with `{title}` SHALL rename a lesson of the caller's course and return 200 with `{id, title, position, createdAt, updatedAt}`. The title follows lessons R1. Only that lesson's title and `updatedAt` change. Another teacher's lesson, an unknown or malformed id and a lesson under another course give 404 `NOT_FOUND`. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign (404).

#### Scenario: lessons/R3-S1 Rename an English lesson
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lessons "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A renames the lesson "Two" to "Present Simple"
- **THEN** the response is 200 with the same `id`, the title "Present Simple", `position` 2, `createdAt` T and `updatedAt` T plus one hour, exactly the properties `id`, `title`, `position`, `createdAt` and `updatedAt`, the stored title is "Present Simple", the lessons "One" and "Three" keep their titles, positions and `updatedAt` T, and the `updatedAt` of the course is unchanged

#### Scenario: lessons/R3-S2 Rename a Russian lesson, the title is normalised
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи"
- **WHEN** teacher A renames it to "  Склонение имён​  " (ending in a zero-width space)
- **THEN** the response is 200 with the title "Склонение имён", and the stored lesson has the same title

#### Scenario: lessons/R3-S3 A title must have 1-200 characters and a letter or digit
- **GIVEN** a logged-in teacher A with a course with the lesson "Падежи"
- **WHEN** teacher A renames it to each title "   ", 201 Cyrillic letters ("Я"), "!!!" and 200 Cyrillic letters ("Я"), with the body `{}` and the body `{"title": null}`, and with the bodies `{"title":` and no body
- **THEN** the first, the fifth and the sixth responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the second is `[{field: "title", code: "TOO_LONG"}]`, the third is `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 200, the seventh and the eighth are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the stored title is the 200-letter title

#### Scenario: lessons/R3-S4 Wrong JSON types are rejected, titles are not unique
- **GIVEN** a logged-in teacher A with a course with the lessons "Grammar" and "Verbs"
- **WHEN** teacher A renames "Verbs" with the title `["Grammar"]`, then with the title `123`, then with the title `true`, and then with the title "Grammar"
- **THEN** the first three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]`, the fourth is 200, and the course has two lessons titled "Grammar" at the positions 1 and 2

#### Scenario: lessons/R3-S5 Another teacher's, unknown, malformed and wrong-course ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with the lesson "Падежи"
- **WHEN** teacher A renames teacher B's lesson (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", the lesson "Fractions" under "English B1", and the lesson "Fractions" under the course "not-a-uuid" and under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика" or "Падежи", and the titles and `updatedAt` of "Падежи" and "Fractions" are unchanged

#### Scenario: lessons/R3-S6 Only active teachers rename lessons
- **GIVEN** a course of teacher A with the lesson "Lesson", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each rename the lesson to "Changed", and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the lesson is still titled "Lesson"

#### Scenario: lessons/R3-S7 Checks run in a fixed order
- **GIVEN** teacher A logged in with a course with a lesson, and a course "Грамматика" of teacher B with the lesson "Падежи"
- **WHEN** teacher A renames the lesson "not-a-uuid" with a blank title, and then teacher B's lesson "Падежи" with a blank title
- **THEN** the first response is 404 with code `NOT_FOUND` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` and does not contain "Грамматика" or "Падежи", and teacher B's lesson is unchanged

#### Scenario: lessons/R3-S8 Renaming to the same title still updates
- **GIVEN** a logged-in teacher A with a course with the lesson "Падежи" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A renames it to "Падежи"
- **THEN** the response is 200 with the title "Падежи" and `updatedAt` T plus one hour

### Requirement: R4 A teacher moves a lesson to a position
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/position` with `{position}` SHALL move the lesson there and return 200 with the moved lesson as in R3. `position` is a required JSON integer; any integer within the JSON number-length limit is clamped to 1..count; floats and strings are rejected. Lessons in between shift by one; only the moved lesson's `updatedAt` is set, also on a same-position move. Errors and checks as R3.

#### Scenario: lessons/R4-S1 Move a lesson down and up
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lessons "Один", "Два", "Три" and "Четыре" at the positions 1 to 4 created at a time T, and the clock advanced by one hour
- **WHEN** teacher A moves "Один" to position 3, and then moves "Четыре" to position 1
- **THEN** the first response is 200 with the title "Один", `position` 3 and `updatedAt` T plus one hour, the second is 200 with the title "Четыре", `position` 1 and `updatedAt` T plus one hour, the final order is "Четыре", "Два", "Один", "Три" at the positions 1, 2, 3, 4, the lessons "Два" and "Три" have `updatedAt` T, and the `updatedAt` of the course is unchanged

#### Scenario: lessons/R4-S2 Moving to the same position bumps updatedAt
- **GIVEN** a logged-in teacher A with a course "English B1" with the lessons "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A moves "Two" to position 2
- **THEN** the response is 200 with `position` 2 and `updatedAt` T plus one hour, the order is unchanged, and "One" and "Three" have `updatedAt` T

#### Scenario: lessons/R4-S3 Any integer is clamped to the course range
- **GIVEN** a logged-in teacher A with a course with the lessons "A", "B", "C" at the positions 1, 2, 3
- **WHEN** teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4
- **THEN** all four responses are 200 with the positions 3, 1, 1 and 3, and the final order is "A", "B", "C" without gaps

#### Scenario: lessons/R4-S4 The position must be an integer
- **GIVEN** a logged-in teacher A with a course with the lessons "A" and "B"
- **WHEN** teacher A moves "A" with the body `{}`, with `{"position": null}`, with the position `"2"`, with `{"position": {"n": 2}}`, with the positions `1.5`, `2.0` and `1e2`, with the position `true`, and with the bodies `{"position":` and no body
- **THEN** the first and the second responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]`, the third to the eighth are 400 with `errors` exactly `[{field: "position", code: "INVALID_TYPE"}]`, the ninth and the tenth are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the order is still "A", "B"

#### Scenario: lessons/R4-S5 Another teacher's, unknown, malformed and wrong-course ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths" and the lessons "Fractions" and "Decimals" in "Maths", and a course "Грамматика" of teacher B with the lessons "Падежи" and "Роды"
- **WHEN** teacher A moves teacher B's lesson "Роды" to position 1 (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", "Decimals" under "English B1", and "Decimals" under the course "not-a-uuid" and under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи" or "Роды", and the orders and `updatedAt` of both courses are unchanged

#### Scenario: lessons/R4-S6 Only active teachers move lessons
- **GIVEN** a course of teacher A with the lessons "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the order is still "One", "Two"

#### Scenario: lessons/R4-S7 Checks run in a fixed order
- **GIVEN** a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher C with a course
- **WHEN** the admin moves the lesson "not-a-uuid" to position 1, teacher A sends the body `{}` for it, and teacher C sends the body `{}` for the lesson "not-a-uuid" and then for an unknown lesson id, both in her own course
- **THEN** the admin's response is 403 with code `FORBIDDEN`, teacher A's is 401 with code `UNAUTHENTICATED`, teacher C's first is 404 with code `NOT_FOUND` and no `errors` property, and the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]`

#### Scenario: lessons/R4-S8 Parallel moves keep positions dense (the teacher and course row locks serialise them)
- **GIVEN** a logged-in teacher A with a course with 10 lessons at the positions 1 to 10
- **WHEN** teacher A sends 10 move requests at the same time, each moving a different lesson to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
- **THEN** every response is 200, and the course has exactly 10 lessons with the positions 1 to 10 without gaps and without duplicates

#### Scenario: lessons/R4-S9 A single lesson stays at position 1
- **GIVEN** a logged-in teacher A with a course with the one lesson "Only" at position 1
- **WHEN** teacher A moves it to position 7, then to 0, then to -3
- **THEN** all three responses are 200 with `position` 1, and the course has exactly one lesson at position 1

#### Scenario: lessons/R4-S10 Input is validated before a foreign or unknown lesson is looked up
- **GIVEN** teacher A logged in, and a course "Грамматика" of teacher B with the lesson "Падежи"
- **WHEN** teacher A moves teacher B's lesson with the body `{}`, and moves a lesson with an unknown id in teacher B's course with the position `"2"`
- **THEN** the first response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]`, the second is 400 with `errors` exactly `[{field: "position", code: "INVALID_TYPE"}]`, neither contains "Грамматика" or "Падежи", and teacher B's course is unchanged

### Requirement: R5 A teacher deletes a lesson
`DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}` SHALL delete the lesson of the caller's course and return 204 with no body. Lessons above it move down by one and keep their `updatedAt`, so positions stay 1..count. A second delete gives 404, and a deletion frees a slot under the R1 limit. Not found cases and the order of checks are as R3, without the input step.

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
