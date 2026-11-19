## ADDED Requirements

### Requirement: R3 A teacher lists their courses
`GET /api/teacher/courses` SHALL return the caller's courses, and only theirs, as a JSON array
without pagination, newest first (`createdAt` descending, then `id` ascending), each item in the
create response shape. Admin and student get 403 `FORBIDDEN`; a client with no session and a
teacher deactivated after login get 401 `UNAUTHENTICATED`.

#### Scenario: courses/R3-S1 Courses are listed newest first
- **GIVEN** a logged-in teacher A with the courses "English B1" (instruction "en", target "en"), "Литература 9 класс" (instruction "ru", no target) and "Maths" (instruction "en", no target), created at three increasing times
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with the titles "Maths", "Литература 9 класс", "English B1" in that order

#### Scenario: courses/R3-S2 Equal creation times are ordered by id
- **GIVEN** a logged-in teacher A with three courses created at the same time
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with the three courses in ascending order of `id`

#### Scenario: courses/R3-S3 A teacher without courses gets an empty list
- **GIVEN** a logged-in teacher A with no courses and a teacher B with one course
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with an empty array

#### Scenario: courses/R3-S4 The list never shows another teacher's courses
- **GIVEN** a logged-in teacher A with the course "Grammar" and a teacher B with the courses "Grammar" and "Грамматика"
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with exactly one item, the course "Grammar" owned by teacher A

#### Scenario: courses/R3-S5 A list item has the create response shape
- **GIVEN** a logged-in teacher A with a course with the title "Литература 9 класс", instruction language "ru" and no target language, created at one time
- **WHEN** teacher A lists the courses
- **THEN** the item has exactly the properties `id`, `title`, `instructionLanguage`, `targetLanguage`, `createdAt` and `updatedAt`, with the title "Литература 9 класс", `instructionLanguage` "ru", `targetLanguage` `null`, and `updatedAt` equal to `createdAt`

#### Scenario: courses/R3-S6 Only active teachers list courses
- **GIVEN** a logged-in admin, a logged-in student, a logged-in teacher who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them lists the courses
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`

### Requirement: R4 A teacher views one of their courses
`GET /api/teacher/courses/{id}` SHALL return 200 with the caller's course in the create response
shape. Another teacher's course, an unknown id and a malformed id MUST give 404 `NOT_FOUND` with
no `errors` property, so existence does not leak. Access is as in R3.

#### Scenario: courses/R4-S1 A teacher views their course
- **GIVEN** a logged-in teacher A with the courses "English B1" (instruction "en", target "en") and "Литература 9 класс" (instruction "ru", no target)
- **WHEN** teacher A views each of them by id
- **THEN** both responses are 200 with that course's `id`, title, `instructionLanguage`, `targetLanguage` (`null` for the second), `createdAt` and `updatedAt`

#### Scenario: courses/R4-S2 Another teacher's, unknown and malformed ids are not found
- **GIVEN** teacher A logged in, and a course "Грамматика" of teacher B
- **WHEN** teacher A views teacher B's course, an unknown id and the id "not-a-uuid"
- **THEN** all three responses are 404 with code `NOT_FOUND` and no `errors` property, and none contains "Грамматика"

#### Scenario: courses/R4-S3 Only active teachers view a course
- **GIVEN** a course of teacher A, a logged-in admin, a logged-in student, a logged-in teacher B who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them views the course of teacher A
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and teacher B's and the client's responses are 401 with code `UNAUTHENTICATED`

### Requirement: R5 A teacher renames a course
`PATCH /api/teacher/courses/{id}` with `{title}` SHALL rename the caller's course and return 200
in the create response shape. The title follows the R1 rules (409 `TITLE_TAKEN` on a clash), but
the course's own title and a case-only change of it are allowed. Only the title changes: other
fields are ignored. Every success sets `updatedAt` to the current time; `createdAt` never
changes. 404 and access are as in R4; a malformed id is rejected before body validation.

#### Scenario: courses/R5-S1 A teacher renames a course
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en"), created at a time T, and the clock advanced by one hour
- **WHEN** teacher A renames the course to "English B2"
- **THEN** the response is 200 with the same `id`, the title "English B2", `instructionLanguage` "en", `targetLanguage` "en", `createdAt` T and `updatedAt` one hour after T, and the stored course has the same values

#### Scenario: courses/R5-S2 A course is renamed to a Cyrillic title
- **GIVEN** a logged-in teacher A with the course "Maths" (instruction "ru", no target), and the clock advanced
- **WHEN** teacher A renames it to "  Литература 9 класс  "
- **THEN** the response is 200 with the title "Литература 9 класс", `instructionLanguage` "ru" and `targetLanguage` `null`, and the stored title is "Литература 9 класс"

#### Scenario: courses/R5-S3 The own title and a case-only change are allowed
- **GIVEN** a logged-in teacher A with the courses "Grammar" and "Грамматика", created at a time T, and the clock advanced by one hour
- **WHEN** teacher A renames "Grammar" to "Grammar", and "Грамматика" to "ГРАММАТИКА"
- **THEN** both responses are 200, the titles are "Grammar" and "ГРАММАТИКА", both `updatedAt` are one hour after T and both `createdAt` are T

#### Scenario: courses/R5-S4 Invalid titles are rejected
- **GIVEN** a logged-in teacher A with the course "Grammar"
- **WHEN** teacher A renames it to "", to "   ", to a title of 201 characters, and to "!?"; and sends the body `{}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` for "", "   " and `{}`, `[{field: "title", code: "TOO_LONG"}]` for 201 characters, and `[{field: "title", code: "INVALID_FORMAT"}]` for "!?", and the title is still "Grammar"

#### Scenario: courses/R5-S5 Wrong JSON types are rejected
- **GIVEN** a logged-in teacher A with the course "Grammar"
- **WHEN** teacher A sends the title `["Grammar"]`, and another request with the title `{"text": "Grammar"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]`, neither body contains "Grammar", and the title is still "Grammar"

#### Scenario: courses/R5-S6 A title is unique per teacher, ignoring case
- **GIVEN** a logged-in teacher A with the courses "Grammar", "Грамматика" and "Vocabulary", and teacher B with the course "Reading"
- **WHEN** teacher A renames "Vocabulary" to "GRAMMAR", and to "грамматика", and teacher A renames "Grammar" to "Reading"
- **THEN** the first two responses are 409 with code `TITLE_TAKEN` and `errors` exactly `[{field: "title", code: "TITLE_TAKEN"}]`, the third is 200, the title "Vocabulary" is unchanged and its `updatedAt` too

#### Scenario: courses/R5-S7 Only the title changes
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en"), teacher B, and the clock advanced
- **WHEN** teacher A renames the course to "English B2" with the extra body fields `instructionLanguage` "ru", `targetLanguage` "ru" and `teacherId` set to teacher B's id
- **THEN** the response is 200 with the title "English B2", `instructionLanguage` "en" and `targetLanguage` "en", the course is still owned by teacher A, and teacher B has no course

#### Scenario: courses/R5-S8 Another teacher's, unknown and malformed ids are not found
- **GIVEN** teacher A logged in, a course "Грамматика" of teacher B, and the clock advanced
- **WHEN** teacher A renames teacher B's course to "Grammar", renames an unknown id to "Grammar", renames the id "not-a-uuid" to "Grammar", and renames the id "not-a-uuid" to ""
- **THEN** all four responses are 404 with code `NOT_FOUND` and no `errors` property, and teacher B's course keeps the title "Грамматика" and its `updatedAt`

#### Scenario: courses/R5-S9 Only active teachers rename a course
- **GIVEN** a course of teacher A, a logged-in admin, a logged-in student, a logged-in teacher B who is then deactivated, a client with a CSRF cookie and no session, and teacher A logged in
- **WHEN** the admin, the student, teacher B and the client each rename the course to "Renamed", and teacher A does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, teacher B's and the client's are 401 with code `UNAUTHENTICATED`, teacher A's is 403 with code `CSRF_INVALID`, and the course keeps its title and its `updatedAt`

#### Scenario: courses/R5-S10 Parallel renames to one title give one winner
- **GIVEN** a logged-in teacher A with the courses "One", "Two", "Three" and "Four"
- **WHEN** teacher A renames all four to "Same" at the same time
- **THEN** exactly one response is 200 and the other three are 409 `TITLE_TAKEN`, and exactly one of the courses has the title "Same"
