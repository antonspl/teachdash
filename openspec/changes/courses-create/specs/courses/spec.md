# Spec Delta

## Purpose

Defines courses: the teacher-owned containers of teaching content, each taught in a
supported language and optionally teaching another, and the list of supported languages.

## ADDED Requirements

### Requirement: R1 Teachers and admins read the supported languages
`GET /api/languages` SHALL return 200 with the supported languages as a list of
`{code}` objects, where `code` is a lowercase ISO 639-1 code, sorted by code. The list
is configured data, initially `en` and `ru`; a language added to it appears without a
code change. Teachers and admins MAY read it; a student gets 403 and a caller without a
session gets 401.

#### Scenario: courses/R1-S1 A teacher reads the initial list
- **GIVEN** a logged-in teacher
- **WHEN** the teacher reads the supported languages
- **THEN** the response is 200 with the body exactly `[{code: "en"}, {code: "ru"}]`

#### Scenario: courses/R1-S2 An admin reads the list
- **GIVEN** a logged-in admin
- **WHEN** the admin reads the supported languages
- **THEN** the response is 200 with the body exactly `[{code: "en"}, {code: "ru"}]`

#### Scenario: courses/R1-S3 A configured language appears in the list
- **GIVEN** the language "de" added to the supported languages, and a logged-in teacher
- **WHEN** the teacher reads the supported languages
- **THEN** the response is 200 with the body exactly `[{code: "de"}, {code: "en"}, {code: "ru"}]`

#### Scenario: courses/R1-S4 Students and anonymous callers cannot read the list
- **GIVEN** a logged-in student, and a client with no session
- **WHEN** each of them reads the supported languages
- **THEN** the student's response is 403 with code `FORBIDDEN` and the client's response is 401 with code `UNAUTHENTICATED`

### Requirement: R2 A teacher creates a course
`POST /api/teacher/courses` SHALL create a course owned by the caller and return 201
with `{id, title, instructionLanguage, targetLanguage, createdAt}`; the target is optional. The title is normalised
like a name, MUST have 1–200 characters with a letter or digit, and MUST be unique per
teacher ignoring case. Both languages MUST be supported codes and may be equal. A
teacher has at most 20 courses, checked after input, before title. Languages are fixed.

#### Scenario: courses/R2-S1 Create an English course for Russian speakers
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "English A1", the instruction language "ru" and the target language "en"
- **THEN** the response is 201 with a new `id`, the title "English A1", `instructionLanguage` "ru", `targetLanguage` "en" and a `createdAt` time, and the course is stored owned by teacher A

#### Scenario: courses/R2-S2 Create a Russian course without a target language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Литература 9 класс", the instruction language "ru" and no target language, and another with the title "Maths", the instruction language "en" and the target language `null`
- **THEN** both responses are 201, the first with the title "Литература 9 класс" and `targetLanguage` `null`, the second with `targetLanguage` `null`

#### Scenario: courses/R2-S3 The target language may equal the instruction language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Advanced English", the instruction language "en" and the target language "en"
- **THEN** the response is 201 with `instructionLanguage` "en" and `targetLanguage` "en"

#### Scenario: courses/R2-S4 A configured language is accepted
- **GIVEN** the language "de" added to the supported languages, and a logged-in teacher A
- **WHEN** teacher A creates a course with the title "Deutsch", the instruction language "en" and the target language "de"
- **THEN** the response is 201 with `targetLanguage` "de"

#### Scenario: courses/R2-S5 The title is normalised
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title "  Грамматика​  ", the instruction language "ru" and no target language
- **THEN** the response is 201 with the title "Грамматика", and the stored title is "Грамматика"

#### Scenario: courses/R2-S6 Title length and format
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates courses with the instruction language "en" and each title "   ", 201 Cyrillic letters ("Я"), "!!!", and 200 Cyrillic letters ("Я")
- **THEN** the first three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, `[{field: "title", code: "TOO_LONG"}]` and `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 201, and teacher A has exactly one course

#### Scenario: courses/R2-S7 Languages must be supported codes
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates courses with the title "Course" and, in turn: no instruction language; the instruction language "xx"; the instruction language "EN"; the instruction language "en" and the target language "eng"; the instruction language "en" and the target language ""
- **THEN** each response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "instructionLanguage", code: "REQUIRED"}]`, `[{field: "instructionLanguage", code: "INVALID_FORMAT"}]`, `[{field: "instructionLanguage", code: "INVALID_FORMAT"}]`, `[{field: "targetLanguage", code: "INVALID_FORMAT"}]` and `[{field: "targetLanguage", code: "INVALID_FORMAT"}]`, no body contains "xx" or "eng", and teacher A has no course

#### Scenario: courses/R2-S8 All failing fields are reported together
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with a blank title, the instruction language "xx" and the target language "yy"
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "instructionLanguage", code: "INVALID_FORMAT"}, {field: "targetLanguage", code: "INVALID_FORMAT"}, {field: "title", code: "REQUIRED"}]`

#### Scenario: courses/R2-S9 A title is unique per teacher, ignoring case
- **GIVEN** teacher A with courses "Grammar" and "Грамматика", and teacher B with no courses, both logged in
- **WHEN** teacher A creates courses titled "GRAMMAR" and "грамматика" with the instruction language "en", and teacher B creates a course titled "Grammar" with the instruction language "en"
- **THEN** teacher A's responses are 409 with code `TITLE_TAKEN` and `errors` exactly `[{field: "title", code: "TITLE_TAKEN"}]`, teacher B's response is 201, and teacher A still has exactly two courses

#### Scenario: courses/R2-S10 At most 20 courses per teacher
- **GIVEN** teacher A with 20 courses, one of them titled "Course 1", and teacher B with 19 courses, both logged in
- **WHEN** teacher A creates a course titled "Course 21", another titled "Course 1" and another with a blank title, all with the instruction language "en", and teacher B creates a course titled "Course 20" with the instruction language "en"
- **THEN** teacher A's first two responses are 409 with code `COURSE_LIMIT_REACHED` and no `errors` property, the third is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, teacher B's response is 201, teacher A has 20 courses and teacher B has 20 courses

#### Scenario: courses/R2-S11 Parallel requests respect the limit and uniqueness
- **GIVEN** teacher A with 19 courses and teacher B with no courses, both logged in
- **WHEN** teacher A sends 5 create requests at the same time with the titles "New 1" to "New 5", and teacher B sends 5 create requests at the same time, all titled "Same"
- **THEN** exactly one of teacher A's responses is 201 and the others are 409 `COURSE_LIMIT_REACHED`, exactly one of teacher B's responses is 201 and the others are 409 `TITLE_TAKEN`, teacher A has 20 courses and teacher B has one course

#### Scenario: courses/R2-S12 The course belongs to the caller
- **GIVEN** teachers A and B, with teacher A logged in
- **WHEN** teacher A creates a course with the title "Course", the instruction language "en" and the extra body field `teacherId` set to teacher B's id
- **THEN** the response is 201, the course is owned by teacher A, and teacher B has no course

#### Scenario: courses/R2-S13 Only active teachers create courses
- **GIVEN** a logged-in admin, a logged-in student, a logged-in teacher who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them creates a course with the title "Course" and the instruction language "en"
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, and no course exists

#### Scenario: courses/R2-S14 Wrong JSON types are rejected
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A creates a course with the title `["Grammar"]` and the instruction language "en", and another with the title "Grammar" and the instruction language `{"code": "en"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]` and `[{field: "instructionLanguage", code: "INVALID_TYPE"}]`, neither body contains "Grammar", and teacher A has no course
