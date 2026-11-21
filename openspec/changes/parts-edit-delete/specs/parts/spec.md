## ADDED Requirements

### Requirement: R3 A teacher renames a part
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}` with `{title}` SHALL rename a part of the caller's lesson and return 200 with `{id, title, position, createdAt, updatedAt}`. The title follows R1. Only that part's title and `updatedAt` change. A foreign, unknown or misplaced id gives 404 `NOT_FOUND`. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404).

#### Scenario: parts/R3-S1 Rename an English part, also to the same title, titles are not unique
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple" that has the parts "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A renames the part "Two" to "Warm-up", then advances the clock by one more hour and renames it to "Warm-up" again, and then renames the part "Three" to "Warm-up"
- **THEN** the first response is 200 with the same `id`, the title "Warm-up", `position` 2, `createdAt` T and `updatedAt` T plus one hour, exactly the properties `id`, `title`, `position`, `createdAt` and `updatedAt`, the second is 200 with `updatedAt` T plus two hours, the third is 200 with the title "Warm-up" and `position` 3, the lesson has two parts titled "Warm-up" at the positions 2 and 3, the part "One" keeps its title, position and `updatedAt` T, and the `updatedAt` of the lesson and of the course is unchanged

#### Scenario: parts/R3-S2 Rename a Russian part, the title is normalised
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the part "Разминка"
- **WHEN** teacher A renames the part to "  Склонение имён&#8203;  " (the text `&#8203;` stands for a zero-width space, U+200B, written before the two trailing spaces)
- **THEN** the response is 200 with the title "Склонение имён", and the stored part has the same title

#### Scenario: parts/R3-S3 A title must have 1-200 characters and a letter or digit, wrong types are rejected
- **GIVEN** a logged-in teacher A with a lesson with the part "Разминка"
- **WHEN** teacher A renames it to each title "   ", 201 Cyrillic letters ("Я"), "!!!" and 200 Cyrillic letters ("Я"), with the body `{}` and the body `{"title": null}`, with the bodies `{"title":` and no body, and with the title `["Grammar"]`, then `123`, then `true`
- **THEN** the first, the fifth and the sixth responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the second is `[{field: "title", code: "TOO_LONG"}]`, the third is `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 200, the seventh and the eighth are 400 with code `MALFORMED_REQUEST` and no `errors` property, the ninth to the eleventh are 400 with `errors` exactly `[{field: "title", code: "INVALID_TYPE"}]` and no `rejectedValue`, and the stored title is the 200-letter title

#### Scenario: parts/R3-S4 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
- **WHEN** teacher A renames, each time to "Changed": (1) teacher B's part under teacher B's course and lesson, (2) teacher B's part under "English B1" and teacher B's lesson, (3) teacher B's part under her own course "Maths" and lesson "Fractions", (4) her own part "Halves" under teacher B's course and lesson, (5) a part with an unknown id under "Maths" and "Fractions", (6) the part id "not-a-uuid" under "Maths" and "Fractions", (7) "Tenths" under "Maths" and "Fractions", (8) "Halves" under "English B1" and "Fractions", (9) "Halves" under "Maths" and the lesson "not-a-uuid", (10) "Halves" under "Maths" and an unknown lesson id, (11) "Halves" under the course "not-a-uuid", (12) "Halves" under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи" or "Разминка", and the titles and `updatedAt` of "Halves", "Tenths" and "Разминка" are unchanged

#### Scenario: parts/R3-S5 Only active teachers rename parts
- **GIVEN** a lesson of teacher A with the part "Part", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each rename the part to "Changed", and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the part is still titled "Part"

#### Scenario: parts/R3-S6 Checks run in a fixed order
- **GIVEN** teacher A logged in with a lesson with a part, a lesson "Падежи" of teacher B with the part "Разминка", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A renames the part "not-a-uuid" with a blank title, then teacher B's part with a blank title, then a part with an unknown id in her own lesson with a blank title, and then her own part in the lesson "not-a-uuid" with a blank title; and the admin and the deactivated teacher A2 each rename the part "not-a-uuid" with a blank title in the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second and the third are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` and do not contain "Падежи" or "Разминка", the fourth is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's part is unchanged

### Requirement: R4 A teacher moves a part to a position
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/position` with `{position}` SHALL move the part there and return 200 with the moved part as in R3. `position` is a required JSON integer, clamped to 1..count (any integer within the JSON number-length limit); floats and strings are rejected. Parts in between shift by one; only the moved part's `updatedAt` is set, also on a same-position move. Errors and checks as R3.

#### Scenario: parts/R4-S1 Move a part down and up
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lessons "Падежи" and "Роды", each with four parts at the positions 1 to 4, where "Падежи" has the parts "Один", "Два", "Три" and "Четыре", all created at a time T, a teacher B with a lesson with four parts at the positions 1 to 4 created at T, and the clock advanced by one hour
- **WHEN** teacher A moves "Один" to position 3, then advances the clock by one more hour and moves "Четыре" to position 1
- **THEN** the first response is 200 with the title "Один", `position` 3 and `updatedAt` T plus one hour, the second is 200 with the title "Четыре", `position` 1 and `updatedAt` T plus two hours, the final order of "Падежи" is "Четыре", "Два", "Три", "Один" at the positions 1, 2, 3, 4, the shifted parts "Два" and "Три" have `updatedAt` T, "Один" keeps `updatedAt` T plus one hour, the order and the `updatedAt` of the parts of "Роды" and of teacher B's lesson are unchanged, and the `updatedAt` of the lessons and of the course is unchanged

#### Scenario: parts/R4-S2 Moving to the same position bumps updatedAt
- **GIVEN** a logged-in teacher A with the course "English B1" with a lesson with the parts "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A moves "Two" to position 2
- **THEN** the response is 200 with `position` 2 and `updatedAt` T plus one hour, the order is unchanged, "One" and "Three" have `updatedAt` T, and the `updatedAt` of the lesson and of the course is unchanged

#### Scenario: parts/R4-S3 Any integer is clamped to the lesson range
- **GIVEN** a logged-in teacher A with a lesson "Main" with the parts "A", "B", "C" at the positions 1, 2, 3, a lesson "Single" with the one part "Only" at position 1, a lesson "Sibling" with four parts at the positions 1 to 4, and a teacher B with a lesson with four parts at the positions 1 to 4, all created at a time T, and the clock advanced by one hour
- **WHEN** teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4, and moves "Only" to position 7, then to 0, then to -3
- **THEN** the first four responses are 200 with the positions 3, 1, 1 and 3, the final order of "Main" is "A", "B", "C" without gaps, the last three responses are 200 with `position` 1, "Single" has exactly one part at position 1, and the order and `updatedAt` T of the parts of "Sibling" and of teacher B's lesson are unchanged

#### Scenario: parts/R4-S4 The position must be an integer
- **GIVEN** a logged-in teacher A with a lesson with the parts "A" and "B"
- **WHEN** teacher A moves "A" with the body `{}`, with `{"position": null}`, with the position `"2"`, with the position `""`, with `{"position": {"n": 2}}`, with the positions `1.5`, `2.0` and `1e2`, with the position `true`, and with the bodies `{"position":` and no body
- **THEN** the first and the second responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]`, the third to the ninth are 400 with `errors` exactly `[{field: "position", code: "INVALID_TYPE"}]`, the tenth and the eleventh are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the order is still "A", "B"

#### Scenario: parts/R4-S5 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves", "Quarters" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
- **WHEN** teacher A moves each to position 1: (1) teacher B's part under teacher B's course and lesson, (2) teacher B's part under "English B1" and teacher B's lesson, (3) teacher B's part under her own course "Maths" and lesson "Fractions", (4) her own part "Halves" under teacher B's course and lesson, (5) a part with an unknown id under "Maths" and "Fractions", (6) the part id "not-a-uuid" under "Maths" and "Fractions", (7) "Tenths" under "Maths" and "Fractions", (8) "Halves" under "English B1" and "Fractions", (9) "Halves" under "Maths" and the lesson "not-a-uuid", (10) "Halves" under "Maths" and an unknown lesson id, (11) "Halves" under the course "not-a-uuid", (12) "Halves" under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи" or "Разминка", and the orders and `updatedAt` of all three lessons are unchanged

#### Scenario: parts/R4-S6 Only active teachers move parts
- **GIVEN** a lesson of teacher A with the parts "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the order is still "One", "Two"

#### Scenario: parts/R4-S7 Checks run in a fixed order
- **GIVEN** teacher A logged in with a course C with a lesson L that has the part P, a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A sends the body `{}` for (1) the part "not-a-uuid" under C and L, (2) teacher B's part "Разминка" under teacher B's course and lesson, (3) a part with an unknown id under C and L, and (4) P under C and the lesson "not-a-uuid"; and the admin and the deactivated teacher A2 each move the part "not-a-uuid" to position 1 under the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second and the third are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]` and do not contain "Падежи" or "Разминка", the fourth is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's part is unchanged

#### Scenario: parts/R4-S8 Parallel moves keep positions distinct (the teacher, course and lesson row locks serialise them)
- **GIVEN** a logged-in teacher A with a lesson with 10 parts at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends 10 move requests at the same time, each moving a different part, addressed by its id, to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
- **THEN** every response is 200, and the lesson has exactly 10 parts with the positions 1 to 10 without gaps and without duplicates

### Requirement: R5 A teacher deletes a part
`DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}` SHALL delete the part of the caller's lesson and return 204 with no body. Parts after it move up by one and keep their `updatedAt`, so positions stay 1..count; the `updatedAt` of the lesson and of the course do not change. A second delete gives 404, and a deletion frees a slot under the R1 limit. Not found cases and the order of checks are as R3, without the input step.

#### Scenario: parts/R5-S1 Delete a Russian part in the middle
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lessons "Падежи" and "Роды", each with four parts at the positions 1 to 4, where "Падежи" has the parts "Один", "Два", "Три" and "Четыре", a teacher B with a lesson with four parts at the positions 1 to 4, all created at a time T, and the clock advanced by one hour
- **WHEN** teacher A deletes "Два"
- **THEN** the response is 204 with an empty body, "Два" is no longer stored, the list of "Падежи" shows "Один", "Три", "Четыре" at the positions 1, 2, 3, all with `updatedAt` T, the order and `updatedAt` of the parts of "Роды" and of teacher B's lesson are unchanged, and the `updatedAt` of the lessons and of the course is unchanged

#### Scenario: parts/R5-S2 A second delete is not found, and a slot is freed
- **GIVEN** a logged-in teacher A with the course "English B1" with a lesson with 20 parts, where the id of the part at position 10 is read in GIVEN
- **WHEN** teacher A deletes that part by its id, deletes it again by the same id, and then creates the part "Part 21" in the lesson
- **THEN** the first response is 204, the second is 404 with code `NOT_FOUND` and no `errors` property, the third is 201 with `position` 20, and the lesson has 20 parts with the positions 1 to 20 without gaps

#### Scenario: parts/R5-S3 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
- **WHEN** teacher A deletes: (1) teacher B's part under teacher B's course and lesson, (2) teacher B's part under "English B1" and teacher B's lesson, (3) teacher B's part under her own course "Maths" and lesson "Fractions", (4) her own part "Halves" under teacher B's course and lesson, (5) a part with an unknown id under "Maths" and "Fractions", (6) the part id "not-a-uuid" under "Maths" and "Fractions", (7) "Tenths" under "Maths" and "Fractions", (8) "Halves" under "English B1" and "Fractions", (9) "Halves" under "Maths" and the lesson "not-a-uuid", (10) "Halves" under "Maths" and an unknown lesson id, (11) "Halves" under the course "not-a-uuid", (12) "Halves" under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи" or "Разминка", and all three parts are still stored

#### Scenario: parts/R5-S4 Only active teachers delete parts
- **GIVEN** a lesson of teacher A with a part, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each delete the part, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the part is still stored

#### Scenario: parts/R5-S5 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A who is then deactivated
- **WHEN** the admin and teacher A each delete the part "not-a-uuid" in the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A's is 401 with code `UNAUTHENTICATED`, neither is a 404

#### Scenario: parts/R5-S6 Parallel deletes and moves end with positions dense and no error
- **GIVEN** a logged-in teacher A with a lesson with 10 parts at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends at the same time 3 delete requests for the parts that were at the positions 2, 5 and 8, and 4 move requests for the parts that were at the positions 1, 3, 6 and 10, each to position 1, all addressed by id
- **THEN** none of the responses is 500, the delete responses are 204 and the move responses are 200, and the lesson ends with exactly 7 parts with the positions 1 to 7 without gaps and without duplicates

#### Scenario: parts/R5-S7 Parallel deletes and creates end within the limit, with positions dense and no error
- **GIVEN** a logged-in teacher A with a lesson with 20 parts, whose ids are read in GIVEN
- **WHEN** teacher A sends at the same time 3 delete requests for the parts that were at the positions 2, 9 and 15, addressed by id, and 5 create requests with the titles "New 1" to "New 5"
- **THEN** none of the responses is 500, the delete responses are 204, each create response is 201 or 409 `PART_LIMIT_REACHED`, the lesson ends with at most 20 parts, and its positions are 1 to the number of its parts without gaps and without duplicates
