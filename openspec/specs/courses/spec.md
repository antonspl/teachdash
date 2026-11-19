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
