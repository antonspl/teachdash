# Spec Delta

## MODIFIED Requirements

### Requirement: R1 A teacher creates a part in a lesson
`POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts` with `{title}` SHALL add a part at the end of the caller's lesson and return 201 with `{id, title, position, createdAt, updatedAt}`. The title follows courses R1, not unique. A 21st part gives 409 `PART_LIMIT_REACHED`; a foreign, unknown or malformed course or lesson gives 404. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404), limit (409).

#### Scenario: parts/R1-S1 Create an English part
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple"
- **WHEN** teacher A creates a part in it with the title "Warm-up"
- **THEN** the response is 201 with a new `id`, the title "Warm-up", `position` 1, a `createdAt` time and an `updatedAt` equal to `createdAt`, exactly these properties and no `lessonId`, `courseId` or `teacherId`, and the part is stored owned by teacher A in that lesson at position 1

#### Scenario: parts/R1-S2 Create a Russian part
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи"
- **WHEN** teacher A creates a part in it with the title "Разминка: именительный, родительный?"
- **THEN** the response is 201 with exactly that title and `position` 1, and the stored part has the same title

#### Scenario: parts/R1-S3 Parts are added at the end, per lesson
- **GIVEN** a logged-in teacher A with a course with the lessons "Grammar" and "Reading", where "Grammar" already has the parts "One" and "Two" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A creates the part "Three" in "Grammar" and the part "First" in "Reading"
- **THEN** the responses are 201 with `position` 3 and `position` 1, "Grammar" has the positions 1, 2, 3 without gaps, the parts "One" and "Two" keep their positions 1 and 2 and their `updatedAt` T, and the `updatedAt` of the course and of both lessons is unchanged

#### Scenario: parts/R1-S4 The title is normalised
- **GIVEN** a logged-in teacher A with a lesson
- **WHEN** teacher A creates a part with the title "  Грамматика&#8203;  " (the text `&#8203;` stands for a zero-width space, U+200B, written before the two trailing spaces)
- **THEN** the response is 201 with the title "Грамматика", and the stored part has the same title

#### Scenario: parts/R1-S5 A title must have 1-200 characters and a letter or digit
- **GIVEN** a logged-in teacher A with a lesson
- **WHEN** teacher A creates parts with each title "   ", 201 Cyrillic letters ("Я"), "!!!", and 200 Cyrillic letters ("Я"), with the body `{}` and the body `{"title": null}`, and with the bodies `{"title":` and no body
- **THEN** the first, the fifth and the sixth responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the second is `[{field: "title", code: "TOO_LONG"}]`, the third is `[{field: "title", code: "INVALID_FORMAT"}]`, the fourth is 201, the seventh and the eighth are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the lesson has exactly one part

#### Scenario: parts/R1-S6 Wrong JSON types and a number or a boolean are rejected
- **GIVEN** a logged-in teacher A with a lesson
- **WHEN** teacher A creates a part with the title `["Grammar"]`, then with the title `123`, then with the title `true`
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly the array `[{field: "title", code: "INVALID_TYPE"}]`, no body has a `rejectedValue`, and the lesson has no part

#### Scenario: parts/R1-S7 Titles are not unique
- **GIVEN** a logged-in teacher A with the lessons "Grammar" and "Reading", where "Grammar" has the part "Warm-up"
- **WHEN** teacher A creates the parts "Warm-up" and "WARM-UP" in "Grammar", and "Warm-up" in "Reading"
- **THEN** all three responses are 201, and "Grammar" has three parts at the positions 1, 2, 3

#### Scenario: parts/R1-S8 At most 20 parts per lesson
- **GIVEN** a logged-in teacher A with a lesson "Full" with 20 parts and a lesson "Other" with 19 parts
- **WHEN** teacher A creates the part "Part 21" in "Full", then a part with a blank title in "Full", and then the part "Part 20" in "Other"
- **THEN** the first response is 409 with code `PART_LIMIT_REACHED` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]`, the third is 201 with `position` 20, "Full" has 20 parts and "Other" has 20 parts

#### Scenario: parts/R1-S9 Parallel requests keep positions distinct and the limit (the teacher, course and lesson row locks serialise them)
- **GIVEN** a logged-in teacher A with a lesson with 17 parts
- **WHEN** teacher A sends 6 create requests at the same time with the titles "New 1" to "New 6"
- **THEN** exactly 3 responses are 201 with the distinct positions 18, 19 and 20, the other 3 are 409 `PART_LIMIT_REACHED`, and the lesson has exactly 20 parts with the positions 1 to 20 without gaps

#### Scenario: parts/R1-S10 Another teacher's, unknown, malformed and wrong-course ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with a lesson "Падежи" that has 20 parts
- **WHEN** teacher A creates a part with the title "Intruder" in teacher B's lesson (under teacher B's course, and under "English B1"), in a lesson with an unknown id, in the lesson "not-a-uuid", in "Fractions" under "English B1", in "Fractions" under teacher B's course, and in "Fractions" under the course "not-a-uuid" and under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property (so ownership is checked before the limit), none contains "Грамматика" or "Падежи", teacher B's lesson still has exactly 20 parts, and "Fractions" has no part

#### Scenario: parts/R1-S11 Only active teachers create parts
- **GIVEN** a lesson of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each create a part in the lesson with the title "Part", and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the lesson has no part

#### Scenario: parts/R1-S12 Checks run in a fixed order
- **GIVEN** teacher A logged in with a lesson, a lesson "Падежи" of teacher B, a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A creates a part with a blank title in the lesson "not-a-uuid", then a part with a blank title in teacher B's lesson, then a part with a blank title in the lesson of a course "not-a-uuid"; and the admin and the deactivated teacher A2 each create a part with a blank title in the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "REQUIRED"}]` and does not contain "Падежи", the third is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's lesson has no new part

#### Scenario: parts/R1-S13 Creating parts while the lesson is deleted
- **GIVEN** a logged-in teacher A with a lesson with 5 parts
- **WHEN** 1 request deleting the lesson and 4 requests creating a part in it are sent at the same time (`support.Parallel.run`)
- **THEN** the delete response is 204, each create response is 201 or 404 and none is 500, the lesson is not stored, and no part row references it


#### Scenario: parts/R1-S14 A title with an unpaired surrogate is rejected
- **GIVEN** a logged-in teacher A with a lesson
- **WHEN** teacher A creates a part with the title "a\uD800b" (an unpaired high surrogate between two letters, sent as the JSON escape), then a part with the title "a\uDC00b" (an unpaired low surrogate)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "title", code: "INVALID_FORMAT"}]`, no body has a `rejectedValue`, and the lesson has no part (rejected at JSON parsing by the existing NUL/surrogate rule, so items R1-S18 and the course and lesson titles follow it)
