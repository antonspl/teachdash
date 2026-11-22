## ADDED Requirements

### Requirement: R1 A teacher adds an item to a part
`POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` with `{type, payload}` SHALL add an item at the end of the caller's part and return 201 with `{id, type, payload, position, version, createdAt, updatedAt}`, `version` 1. A 51st item gives 409 `ITEM_LIMIT_REACHED`; a foreign, unknown, malformed or mismatched id gives 404. Checks run in order: CSRF and role (403), deactivated (401), malformed id (404), input (400, R2), unknown or foreign id (404), limit (409).

#### Scenario: items/R1-S1 Add an English item
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple" that has the part "Warm-up", the item type `test-note`, and another teacher B
- **WHEN** teacher A adds an item with the type `test-note` and the payload `{"text": "Read the story aloud."}` to the part, with the extra body fields `teacherId` (teacher B's id) and `title` ("Ignored")
- **THEN** the response is 201 with a new `id`, the type `test-note`, the payload `{"text": "Read the story aloud."}`, `position` 1, `version` 1, a `createdAt` time and an `updatedAt` equal to `createdAt`, exactly these properties and no `partId`, `lessonId`, `courseId`, `teacherId` or `title`, and the item is stored owned by teacher A (not teacher B) in that part at position 1

#### Scenario: items/R1-S2 Add a Russian item, Cyrillic text is kept exactly
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the part "Разминка"
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{"text": "Просклоняйте слово «мышь»: именительный, родительный?  Ёж & ёлка."}` to the part
- **THEN** the response is 201 with `position` 1 and a payload whose `text` is exactly that text, and the stored payload has the same `text`

#### Scenario: items/R1-S3 Items are added at the end, per part
- **GIVEN** a logged-in teacher A with a lesson with the parts "Reading" and "Writing", where "Reading" already has the items "One" and "Two" created at a time T, and the clock advanced by one hour
- **WHEN** teacher A adds the item "Three" to "Reading" and the item "First" to "Writing"
- **THEN** the responses are 201 with `position` 3 and `position` 1, "Reading" has the positions 1, 2, 3 without gaps, the items "One" and "Two" keep their positions 1 and 2, their `version` 1 and their `updatedAt` T, and the `updatedAt` of the course, of the lesson and of both parts is unchanged

#### Scenario: items/R1-S4 At most 50 items per part
- **GIVEN** a logged-in teacher A with a part "Full" with 50 items and a part "Other" with 49 items
- **WHEN** teacher A adds an item to "Full", then an item with the payload `{}` to "Full", and then an item to "Other"
- **THEN** the first response is 409 with code `ITEM_LIMIT_REACHED` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]` (input is checked before the limit), the third is 201 with `position` 50, "Full" has 50 items and "Other" has 50 items

#### Scenario: items/R1-S5 Parallel requests keep positions distinct and the limit (the lock chain serialises them)
- **GIVEN** a logged-in teacher A with a part with 47 items
- **WHEN** teacher A sends 6 add requests at the same time with the texts "New 1" to "New 6"
- **THEN** exactly 3 responses are 201 with the distinct positions 48, 49 and 50, the other 3 are 409 `ITEM_LIMIT_REACHED`, and the part has exactly 50 items with the positions 1 to 50 without gaps

#### Scenario: items/R1-S6 Another teacher's, unknown and malformed course, lesson and part ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" in "Maths" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" and the part "Разминка", which has 50 items
- **WHEN** teacher A adds a valid item of the type `test-note` to: (1) teacher B's part under teacher B's course and lesson, (2) teacher B's part under teacher A's own course "Maths" and lesson "Fractions", (3) teacher B's part under "English B1" and teacher B's lesson, (4) teacher A's own part "Halves" under teacher B's course and lesson, (5) a part with an unknown id under "Maths" and "Fractions", (6) the part id "not-a-uuid" under "Maths" and "Fractions", (7) "Halves" under "Maths" and an unknown lesson id, (8) "Halves" under "Maths" and the lesson "not-a-uuid", (9) "Halves" under an unknown course id, (10) "Halves" under the course "not-a-uuid"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property (so ownership is checked before the limit), none contains "Грамматика", "Падежи" or "Разминка", teacher B's part still has exactly 50 items, and "Halves" and "Tenths" have no item

#### Scenario: items/R1-S7 A part of another lesson or course of the same teacher is not found under the wrong parent
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a lesson "Tenses" in "English B1"
- **WHEN** teacher A adds a valid item to "Tenths" under "Maths" and "Fractions", to "Halves" under "Maths" and "Decimals", to "Halves" under "English B1" and "Fractions", and to "Halves" under "English B1" and "Tenses"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, and "Halves" and "Tenths" have no item

#### Scenario: items/R1-S8 Only active teachers add items
- **GIVEN** a part of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each add a valid item to the part, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the part has no item

#### Scenario: items/R1-S9 Checks run in a fixed order
- **GIVEN** teacher A logged in with a part, a part of teacher B with the lesson "Падежи", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A adds an item with the payload `{}` to the part "not-a-uuid", then to teacher B's part, then to the part under the course "not-a-uuid"; and the admin and the deactivated teacher A2 each add an item with the payload `{}` to the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]` and does not contain "Падежи", the third is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's part has no new item

#### Scenario: items/R1-S10 Adding items while the part is deleted
- **GIVEN** a logged-in teacher A with a part with 5 items
- **WHEN** 1 request deleting the part and 4 requests adding an item to it are sent at the same time (`support.Parallel.run`)
- **THEN** the delete response is 204, each add response is 201 or 404 and none is 500, the part is not stored, and no item row references it

### Requirement: R2 An item payload follows the generic rules and its type's rules
`type` SHALL be a registered code and `payload` a JSON object, else 400 `VALIDATION_FAILED` with `REQUIRED`, `UNKNOWN_TYPE` or `INVALID_TYPE`. The payload SHALL then be read into the type's record and checked, with errors under `payload.` (`INVALID_TYPE`, `INVALID_FORMAT` for a NUL or a lone surrogate, `UNKNOWN_FIELD`, the record's constraints, the type's cross-field checks). The stored payload is the checked record serialised; the response `payload` is read back from jsonb.

#### Scenario: items/R2-S1 A missing, null, blank, wrongly typed or unknown type is rejected
- **GIVEN** a logged-in teacher A with a part, and the item type `test-note` (no type `poll` is registered)
- **WHEN** teacher A adds items with the body `{"payload": {"text": "Hi"}}`, with `{"type": null, "payload": {"text": "Hi"}}`, with the types "" and "  ", with the type `123`, with the type "poll", with the body `{}`, and with the type "poll" and the payload `[]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED`, `errors` are exactly `[{field: "type", code: "REQUIRED"}]` for the first four, `[{field: "type", code: "INVALID_TYPE"}]` for `123`, `[{field: "type", code: "UNKNOWN_TYPE"}]` for "poll", `[{field: "payload", code: "REQUIRED"}, {field: "type", code: "REQUIRED"}]` for `{}`, and `[{field: "payload", code: "INVALID_TYPE"}, {field: "type", code: "UNKNOWN_TYPE"}]` for the last (sorted by field), and the part has no item

#### Scenario: items/R2-S2 A missing or null payload is rejected, a broken body is malformed
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items with the bodies `{"type": "test-note"}`, `{"type": "test-note", "payload": null}`, `{"type": "test-note", "payload":` and no body
- **THEN** the first two responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "REQUIRED"}]`, the last two are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the part has no item

#### Scenario: items/R2-S3 A payload that is not an object is rejected before the type's rules run
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test-note` with the payload `[]`, `["Hi"]`, `"Hi"`, `123` and `true`
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_TYPE"}]` (no `payload.text` entry), no body contains "Hi" as a value, and the part has no item

#### Scenario: items/R2-S5 A NUL character or a lone surrogate in a string is rejected with its field
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test-note` with the payloads `{"text": "a\u0000b"}`, `{"text": "ok", "hint": "a\u0000"}` and `{"text": "a\ud800b"}` (JSON escapes: the NUL and the lone high surrogate are characters of the string)
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "INVALID_FORMAT"}]`, `[{field: "payload.hint", code: "INVALID_FORMAT"}]` and `[{field: "payload.text", code: "INVALID_FORMAT"}]` (not 500), and the part has no item

#### Scenario: items/R2-S6 A NUL character in an object key is an unknown property
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{"text": "ok", "ke\u0000y": 1}`
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.ke\u0000y", code: "UNKNOWN_FIELD"}]` (not 500), and the part has no item

#### Scenario: items/R2-S7 The record's constraints report errors under `payload.`
- **GIVEN** a logged-in teacher A with a part, and the type `test-note` whose `text` is required, a string of at most 1000 characters, and whose optional `hint` is at most 200 characters
- **WHEN** teacher A adds items of the type `test-note` with the payloads `{}`, `{"text": null}`, `{"text": 42}`, `{"text": ["Привет"]}`, `{"text": "<1001 Cyrillic letters>"}` and `{"text": "ok", "hint": "<201 Cyrillic letters>"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]`, `[{field: "payload.text", code: "REQUIRED"}]`, `[{field: "payload.text", code: "INVALID_TYPE"}]`, `[{field: "payload.text", code: "INVALID_TYPE"}]`, `[{field: "payload.text", code: "TOO_LONG"}]` and `[{field: "payload.hint", code: "TOO_LONG"}]`, no body contains a `rejectedValue` or "Привет", and the part has no item

#### Scenario: items/R2-S8 A payload at the type's limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with a `text` of 1000 Cyrillic letters ("Я"), then one with a `text` of 1000 English letters ("a")
- **THEN** both responses are 201 with the `text` exactly as sent and `position` 1 and 2

#### Scenario: items/R2-S9 The stored payload is the serialised record in its jsonb form
- **GIVEN** a logged-in teacher A with a part, and the type `test-note`
- **WHEN** teacher A adds an item of the type `test-note` with the body text `{"type": "test-note", "payload": {"hint":  "Ответ: «мышь»", "text":  "Привет", "text": "Мир"}}` (extra spaces, the key `text` twice)
- **THEN** the response is 201 with the payload equal, as a JSON object, to `{"text": "Мир", "hint": "Ответ: «мышь»"}` (the last duplicate wins, the key order is not specified), `version` 1, and the stored row has the same payload read from jsonb and the type `test-note`

#### Scenario: items/R2-S10 An unknown payload property is rejected and stops further checks
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{"hint": "Ok", "extra": {"deep": [1]}}` (no `text`)
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.extra", code: "UNKNOWN_FIELD"}]` (no `payload.text` entry: reading the payload stops at the first Jackson error), the body does not contain "deep", and the part has no item

#### Scenario: items/R2-S11 A property of the request body outside the payload is still ignored
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item with the body `{"type": "test-note", "payload": {"text": "Hi"}, "extra": 1}`
- **THEN** the response is 201 and the payload is `{"text": "Hi"}`
