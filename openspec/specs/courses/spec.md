# courses Specification

## Purpose
Defines courses: the teacher-owned containers of teaching content, each taught in a
course language from the language list and optionally teaching another.

## Requirements

### Requirement: R1 A teacher creates a course
`POST /api/teacher/courses` SHALL create a course owned by the caller and return 201 with
`{id, title, instructionLanguage, targetLanguage?, createdAt, updatedAt}`. The trimmed
title MUST have 1–200 characters with a letter or digit, unique per teacher ignoring
case. Trimmed codes MUST be listed; a listed code with `content` false gets
`UNSUPPORTED_COURSE_LANGUAGE`. At most 20 courses per teacher, checked after input
validation, before the duplicate-title check.

#### Scenario: courses/R1-S1 Create an English course taught in English
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "English B1", the instruction language "en" and the target language "en"
- **THEN** the response is 201 with a new `id`, the title "English B1", `instructionLanguage` "en", `targetLanguage` "en", a `createdAt` time and an `updatedAt` equal to `createdAt`, and the course is stored owned by teacher A

#### Scenario: courses/R1-S2 Create a Russian course without a target language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Литература 9 класс", the instruction language "ru" and no target language, and another with the title "Maths", the instruction language "en" and the target language `null`
- **THEN** both responses are 201, the first with the title "Литература 9 класс" and `targetLanguage` `null`, the second with `targetLanguage` `null`

#### Scenario: courses/R1-S3 A Russian course taught in Russian or in English
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Русский A1", the instruction language "ru" and the target language "ru", and another with the title "Russian A1", the instruction language "en" and the target language "ru"
- **THEN** both responses are 201, the first with `instructionLanguage` "ru" and `targetLanguage` "ru", the second with `instructionLanguage` "en" and `targetLanguage` "ru"

#### Scenario: courses/R1-S4 A title may contain punctuation
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Business Result: Upper-Intermediate. Part 1?!", the instruction language "en" and the target language "en", and another with the title "Говорим по-русски, часть 2!", the instruction language "ru" and the target language "ru"
- **THEN** both responses are 201 with exactly those titles

#### Scenario: courses/R1-S5 The title and language codes are normalised
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "  Грамматика\u200B  " (ending in a zero-width space), the instruction language " en " and the target language " ru " (spaces only)
- **THEN** the response is 201 with the title "Грамматика", `instructionLanguage` "en" and `targetLanguage` "ru", and the stored course has the same values

#### Scenario: courses/R1-S6 A title must have 1–200 characters and a letter or digit
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates courses with the instruction language "en" and each title "   ", 201 Cyrillic letters ("Я"), "!!!", and 200 Cyrillic letters ("Я")
- **THEN** the first three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, `[{field: "title", code: "TOO_LONG"}]` and `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 201, and teacher A has exactly one course

#### Scenario: courses/R1-S7 The instruction language is a listed code
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates courses with the title "Course" and, in turn: no instruction language; `null`; ""; "  "; "xx"; "EN"; "eng"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, `errors` is exactly `[{field: "instructionLanguage", code: "REQUIRED"}]` for the first four and `[{field: "instructionLanguage", code: "INVALID_FORMAT"}]` for the others, no body contains "xx" or "eng", and teacher A has no course

#### Scenario: courses/R1-S8 The target language is a listed code when given
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates courses with the title "Course", the instruction language "en" and, in turn, the target language "xx"; "RU"; "rus"; ""; "  "
- **THEN** each response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "targetLanguage", code: "INVALID_FORMAT"}]`, no body contains "xx" or "rus", and teacher A has no course

#### Scenario: courses/R1-S9 A course cannot be created in an unsupported language
- **GIVEN** a logged-in teacher A, and the language list with "de" and "uk" marked `content` false
- **WHEN** teacher A creates courses with the title "Course" and, in turn: the instruction language "de"; the instruction language "en" and the target language "uk"; the instruction language "de" and the target language "uk"
- **THEN** each response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "instructionLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}]`, `[{field: "targetLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}]` and `[{field: "instructionLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}, {field: "targetLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}]`, and teacher A has no course

#### Scenario: courses/R1-S10 All failing fields are reported together
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with a blank title, the instruction language "xx" and the target language "de"
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "instructionLanguage", code: "INVALID_FORMAT"}, {field: "targetLanguage", code: "UNSUPPORTED_COURSE_LANGUAGE"}, {field: "title", code: "REQUIRED"}]`

#### Scenario: courses/R1-S11 A title is unique per teacher, ignoring case
- **GIVEN** teacher A with courses "Grammar" and "Грамматика", and teacher B with no courses, both logged in
- **WHEN** teacher A creates courses titled "GRAMMAR" and "грамматика" with the instruction language "en", and teacher B creates a course titled "Grammar" with the instruction language "en"
- **THEN** teacher A's responses are 409 with code `TITLE_TAKEN` and `errors` exactly `[{field: "title", code: "TITLE_TAKEN"}]`, teacher B's response is 201, and teacher A still has exactly two courses

#### Scenario: courses/R1-S12 At most 20 courses per teacher
- **GIVEN** teacher A with 20 courses, one of them titled "Course 1", and teacher B with 19 courses, both logged in
- **WHEN** teacher A creates a course titled "Course 21", another titled "Course 1" and another with a blank title, all with the instruction language "en", and teacher B creates a course titled "Course 20" with the instruction language "en"
- **THEN** teacher A's first two responses are 409 with code `COURSE_LIMIT_REACHED` and no `errors` property, the third is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, teacher B's response is 201, teacher A has 20 courses and teacher B has 20 courses

#### Scenario: courses/R1-S13 Parallel requests respect the limit and uniqueness
- **GIVEN** teacher A with 19 courses and teacher B with no courses, both logged in
- **WHEN** teacher A sends 5 create requests at the same time with the titles "New 1" to "New 5", and teacher B sends 5 create requests at the same time, all titled "Same"
- **THEN** exactly one of teacher A's responses is 201 and the others are 409 `COURSE_LIMIT_REACHED`, exactly one of teacher B's responses is 201 and the others are 409 `TITLE_TAKEN`, teacher A has 20 courses and teacher B has one course

#### Scenario: courses/R1-S14 The course belongs to the caller
- **GIVEN** teachers A and B, with teacher A logged in
- **WHEN** teacher A creates a course with the title "Course", the instruction language "en" and the extra body field `teacherId` set to teacher B's id
- **THEN** the response is 201, the course is owned by teacher A, and teacher B has no course

#### Scenario: courses/R1-S15 Only active teachers create courses
- **GIVEN** a logged-in admin, a logged-in student, a logged-in teacher who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them creates a course with the title "Course" and the instruction language "en"
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, and no course exists

#### Scenario: courses/R1-S16 Wrong JSON types are rejected
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title `["Grammar"]` and the instruction language "en", and another with the title "Grammar" and the instruction language `{"code": "en"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]` and `[{field: "instructionLanguage", code: "INVALID_TYPE"}]`, neither body contains "Grammar", and teacher A has no course

### Requirement: R3 A teacher lists their courses
`GET /api/teacher/courses` SHALL return the caller's courses, and only theirs, as a JSON array
without pagination, newest first (`createdAt` descending, then `id` ascending), each item with the
fields R1 returns: `{id, title, instructionLanguage, targetLanguage, createdAt, updatedAt}`. Admin and student get 403 `FORBIDDEN`; a client with no session and a
teacher deactivated after login get 401 `UNAUTHENTICATED`.

#### Scenario: courses/R3-S1 Courses are listed newest first
- **GIVEN** a logged-in teacher A with the courses "Литература 9 класс" (instruction "ru", no target) created at a middle time, "English B1" (instruction "en", target "en") created at the oldest time, and "Maths" (instruction "en", no target) created at the newest time, inserted in that order
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with the titles "Maths", "Литература 9 класс", "English B1" in that order

#### Scenario: courses/R3-S2 Renaming does not move a course in the list
- **GIVEN** a logged-in teacher A with the courses "Old", "Middle" and "New" created at increasing times, and the clock advanced
- **WHEN** teacher A renames "Old" to "Oldest" and then lists the courses
- **THEN** the list has the titles "New", "Middle", "Oldest" in that order

#### Scenario: courses/R3-S3 Equal creation times are ordered by id
- **GIVEN** a logged-in teacher A with three courses created at the same time, with the fixed ids ending in 0001, 0002 and 0003, inserted in the order 0002, 0003, 0001
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with the three courses in the order of ids 0001, 0002, 0003

#### Scenario: courses/R3-S4 A teacher without courses gets an empty list
- **GIVEN** a logged-in teacher A with no courses and a teacher B with one course
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with an empty array

#### Scenario: courses/R3-S5 All twenty courses are listed
- **GIVEN** a logged-in teacher A with 20 courses
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with an array of 20 items, all different

#### Scenario: courses/R3-S6 The list never shows another teacher's courses
- **GIVEN** a logged-in teacher A with the course "Grammar" and a teacher B with the courses "Grammar" and "Грамматика"
- **WHEN** teacher A lists the courses
- **THEN** the response is 200 with exactly one item, the course "Grammar" owned by teacher A

#### Scenario: courses/R3-S7 A list item has the same fields as a created course
- **GIVEN** a logged-in teacher A with a course with the title "Литература 9 класс", instruction language "ru" and no target language, created at one time
- **WHEN** teacher A lists the courses
- **THEN** the item has exactly the properties `id`, `title`, `instructionLanguage`, `targetLanguage`, `createdAt` and `updatedAt`, with the title "Литература 9 класс", `instructionLanguage` "ru", `targetLanguage` `null`, and `updatedAt` equal to `createdAt`

#### Scenario: courses/R3-S8 Only active teachers list courses
- **GIVEN** a logged-in admin, a logged-in student, a logged-in teacher A with a course who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them lists the courses
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and teacher A's (deactivated) and the client's responses are 401 with code `UNAUTHENTICATED`

### Requirement: R4 A teacher views one of their courses
`GET /api/teacher/courses/{id}` SHALL return 200 with the caller's course with the
same fields as R3. Another teacher's course, an unknown id and a malformed id MUST give 404 `NOT_FOUND` with
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
- **GIVEN** a course of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who owns it and is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them views the course
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and teacher A's and the client's responses are 401 with code `UNAUTHENTICATED`

### Requirement: R5 A teacher renames a course
`PATCH /api/teacher/courses/{id}` with `{title}` SHALL rename the caller's course and return 200
with the same fields as R3. The title follows the R1 rules (409 `TITLE_TAKEN` on a clash); the
course's own title and a case-only change are allowed. Only the title changes; other fields are
ignored. Every success sets `updatedAt` to now; `createdAt` never changes. Checks run in order:
CSRF and role (403), deactivated (401), malformed id (404), input (400), foreign or unknown id (404).

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
- **WHEN** teacher A renames it to "", to "   ", to a title of 201 characters, and to "!?"; sends the body `{}` and the body `{"title": null}`, and sends the bodies `{"title":` and no body
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` for "", "   ", `{}` and `null`, 400 with code `MALFORMED_REQUEST` and no `errors` property for the broken body and for no body, `[{field: "title", code: "TOO_LONG"}]` for 201 characters, and `[{field: "title", code: "INVALID_FORMAT"}]` for "!?", and the title is still "Grammar"

#### Scenario: courses/R5-S5 Wrong JSON types are rejected
- **GIVEN** a logged-in teacher A with the course "Grammar"
- **WHEN** teacher A sends the title `["Grammar"]`, and another request with the title `{"text": "Grammar"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]`, neither body contains "Grammar", and the title is still "Grammar"

#### Scenario: courses/R5-S6 A title is unique per teacher, ignoring case
- **GIVEN** a logged-in teacher A with the courses "Grammar", "Грамматика" and "Vocabulary", and teacher B with the course "Reading", and the clock advanced
- **WHEN** teacher A renames "Vocabulary" to "GRAMMAR", and to "грамматика", and teacher A renames "Grammar" to "Reading"
- **THEN** the first two responses are 409 with code `TITLE_TAKEN` and `errors` exactly `[{field: "title", code: "TITLE_TAKEN"}]`, the third is 200 and the course "Grammar" is now stored as "Reading", and "Vocabulary" keeps its title and its `updatedAt`

#### Scenario: courses/R5-S7 Only the title changes
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en"), teacher B, and the clock advanced
- **WHEN** teacher A renames the course to "English B2" with the extra body fields `instructionLanguage` "ru", `targetLanguage` "ru" and `teacherId` set to teacher B's id
- **THEN** the response is 200 with the title "English B2", `instructionLanguage` "en" and `targetLanguage` "en", the course is still owned by teacher A, and teacher B has no course

#### Scenario: courses/R5-S8 Another teacher's, unknown and malformed ids are not found
- **GIVEN** teacher A logged in, a course "Грамматика" of teacher B, and the clock advanced
- **WHEN** teacher A renames teacher B's course to "Grammar", renames an unknown id to "Grammar", renames the id "not-a-uuid" to "Grammar", and renames the id "not-a-uuid" to ""
- **THEN** all four responses are 404 with code `NOT_FOUND` and no `errors` property, and teacher B's course keeps the title "Грамматика" and its `updatedAt`

#### Scenario: courses/R5-S9 Only active teachers rename a course
- **GIVEN** a course of teacher A, a logged-in admin, a logged-in student, teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B, and the clock advanced
- **WHEN** the admin, the student, teacher A and the client each rename the course to "Renamed", and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, teacher A's and the client's are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the course keeps its title and its `updatedAt`

#### Scenario: courses/R5-S10 Parallel renames to one title give one winner
- **GIVEN** a logged-in teacher A with the courses "One", "Two", "Three" and "Four", and the clock advanced
- **WHEN** teacher A renames all four to "Same" at the same time
- **THEN** exactly one response is 200 and the other three are 409 `TITLE_TAKEN`, exactly one of the courses has the title "Same", and the other three keep their titles and their `updatedAt`

#### Scenario: courses/R5-S11 A number or a boolean is not a title
- **GIVEN** a logged-in teacher A with the course "Grammar"
- **WHEN** teacher A renames it with the title `123`, then with `true`, and creates a course with the title `123` and the instruction language "en", then with `true`
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]`, no body contains "123" or "true" as a value, the title is still "Grammar" and teacher A has one course
