# items Specification

## Purpose
A teacher adds items (lesson tasks and content) to a part of a lesson, moves them to another position in the part and deletes them. Each item has a registered type and a JSON payload that the type validates.

## Requirements

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
- **WHEN** teacher A adds an item of the type `test-note` with the text "Fifty-one" to "Full", then an item of the type `test-note` with the payload `{}` to "Full", and then an item of the type `test-note` with the text "Fiftieth" to "Other"
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
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{}` to the part "not-a-uuid", then to teacher B's part, then to the part under the course "not-a-uuid"; and the admin and the deactivated teacher A2 each add an item of the type `test-note` with the payload `{}` to the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]` and does not contain "Падежи", the third is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's part has no new item

#### Scenario: items/R1-S10 Adding items while the part is deleted
- **GIVEN** a logged-in teacher A with a part with 5 items
- **WHEN** 1 request deleting the part and 4 requests adding an item to it are sent at the same time (`support.Parallel.run`)
- **THEN** the delete response is 204, each add response is 201 or 404 and none is 500, the part is not stored, and no item row references it

### Requirement: R2 An item payload follows the generic rules and its type's rules
`type` SHALL be a registered code and `payload` a JSON object, else 400 `VALIDATION_FAILED` with `REQUIRED`, `UNKNOWN_TYPE` or `INVALID_TYPE` (the first binding error wins). A NUL or lone surrogate in any payload string, a known field or not, gives `payload` `INVALID_FORMAT`, and an unknown property `payload` `UNKNOWN_FIELD`: no key is echoed. Other errors of the record or its constraints keep the path `payload.<path>`. The stored payload is the record serialised without null fields.

#### Scenario: items/R2-S1 A missing, null, blank, wrongly typed or unknown type is rejected
- **GIVEN** a logged-in teacher A with a part, and the item type `test-note` (no type `poll` is registered)
- **WHEN** teacher A adds items with the body `{"payload": {"text": "Hi"}}`, with `{"type": null, "payload": {"text": "Hi"}}`, with the types "" and "  ", with the type `123`, with the type "poll", and with the body `{}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED`, `errors` are exactly `[{field: "type", code: "REQUIRED"}]` for the first four, `[{field: "type", code: "INVALID_TYPE"}]` for `123`, `[{field: "type", code: "UNKNOWN_TYPE"}]` for "poll", `[{field: "payload", code: "REQUIRED"}, {field: "type", code: "REQUIRED"}]` for `{}` (sorted by field), and the part has no item

#### Scenario: items/R2-S2 A missing or null payload is rejected, a broken body is malformed
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items with the bodies `{"type": "test-note"}`, `{"type": "test-note", "payload": null}`, `{"type": "test-note", "payload":` and no body
- **THEN** the first two responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "REQUIRED"}]`, the last two are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the part has no item

#### Scenario: items/R2-S3 A payload that is not an object is rejected before the type is checked, the first binding error wins
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test-note` with the payload `[]`, `["Hi"]`, `"Hi"`, `""`, `123` and `true`, and an item of the type "poll" (not registered) with the payload `[]`, one with a blank type and the payload `[]`, and the body `{"type":123,"payload":[]}` (the `type` key first)
- **THEN** every response is 400 with code `VALIDATION_FAILED`; all but the last have `errors` exactly `[{field: "payload", code: "INVALID_TYPE"}]` (no `payload.text` entry and no `type` entry: the type is not checked), the last has `errors` exactly `[{field: "type", code: "INVALID_TYPE"}]` (binding stops at the first error, `type`, and `payload` is not reported), no body contains "Hi" as a value, and the part has no item

#### Scenario: items/R2-S5 A NUL character or a lone surrogate in a string value is rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test-note` with the payloads `{"text": "a\u0000b"}`, `{"text": "ok", "hint": "a\u0000"}` and `{"text": "a\ud800b"}` (JSON escapes: the NUL and the lone high surrogate are characters of the string)
- **THEN** every response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (not 500; the field is cut at the `Map` key, also for the known fields `text` and `hint`), no body contains the rejected value, and the part has no item

#### Scenario: items/R2-S6 A NUL character in an object key is an unknown property reported as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{"text": "ok", "ke\u0000y": 1}`
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (not 500; the key is not echoed), and the part has no item

#### Scenario: items/R2-S7 The record's constraints report errors under `payload.`
- **GIVEN** a logged-in teacher A with a part, and the type `test-note` whose `text` is required, a string of at most 1000 characters, and whose optional `hint` is at most 200 characters
- **WHEN** teacher A adds items of the type `test-note` with the payloads `{}`, `{"text": null}`, `{"text": 42}`, `{"text": ["Привет"]}`, `{"text": "<1001 Cyrillic letters>"}` and `{"text": "ok", "hint": "<201 Cyrillic letters>"}`
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]`, `[{field: "payload.text", code: "REQUIRED"}]`, `[{field: "payload.text", code: "INVALID_TYPE"}]`, `[{field: "payload.text", code: "INVALID_TYPE"}]`, `[{field: "payload.text", code: "TOO_LONG"}]` and `[{field: "payload.hint", code: "TOO_LONG"}]`, no body contains a `rejectedValue` or "Привет", and the part has no item

#### Scenario: items/R2-S8 A payload at the type's limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with a `text` of 1000 Cyrillic letters ("Я"), then one with a `text` of 1000 English letters ("a")
- **THEN** both responses are 201 with the `text` exactly as sent and `position` 1 and 2

#### Scenario: items/R2-S9 The stored payload is the serialised record, without null fields, in its jsonb form
- **GIVEN** a logged-in teacher A with a part, and the type `test-note`
- **WHEN** teacher A adds an item of the type `test-note` with the body text `{"type": "test-note", "payload": {"hint":  "Ответ: «мышь»", "text":  "Привет", "text": "Мир"}}` (extra spaces, the key `text` twice), and an item with the payload `{"text": "Hi"}` (no `hint`)
- **THEN** the first response is 201 with the payload equal, as a JSON object, to `{"text": "Мир", "hint": "Ответ: «мышь»"}` (the last duplicate wins, the key order is not specified), `version` 1, and the stored row has the same payload read from jsonb and the type `test-note`; the second response and its stored payload are exactly `{"text": "Hi"}` (no `"hint": null`)

#### Scenario: items/R2-S10 An unknown payload property is rejected and stops further checks
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test-note` with the payload `{"hint": "Ok", "extra": {"deep": [1]}}` (no `text`)
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (the property name is not echoed; no `payload.text` entry: reading the payload stops at the first Jackson error), the body contains neither "extra" nor "deep", and the part has no item

#### Scenario: items/R2-S11 A property of the request body outside the payload is still ignored
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item with the body `{"type": "test-note", "payload": {"text": "Hi"}, "extra": 1}`
- **THEN** the response is 201 and the payload is `{"text": "Hi"}`

#### Scenario: items/R2-S12 Decimals keep their precision, out-of-range decimals and a non-integer rating are rejected
- **GIVEN** a logged-in teacher A with a part, and the type `test-note` whose optional `rating` is an `Integer` and optional `weight` a `BigDecimal` with `@Digits(integer = 10, fraction = 20)`
- **WHEN** teacher A adds items of the type `test-note` with the payloads `{"text": "ok", "weight": 0.12345678901234567890, "rating": 3}`, then `{"text": "ok", "weight": 1e400}`, `{"text": "ok", "weight": 1e200000}` and `{"text": "ok", "weight": 1e-200000}`, then `{"text": "ok", "rating": 1.5}`, `{"text": "ok", "rating": 1e400}` and `{"text": "ok", "rating": "5"}`
- **THEN** the first response is 201 with `weight` numerically equal to 0.12345678901234567890 (all 20 digits), the stored payload has the same value, and `rating` 3, the next three are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.weight", code: "INVALID_FORMAT"}]` (not 500), the last three are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.rating", code: "INVALID_TYPE"}]`, and the part has exactly 1 item

#### Scenario: items/R2-S13 A key is never echoed, even when its value is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items with the bodies `{"type": "test-note", "payload": {"text": "ok", "secretKey": "a\u0000"}}` and `{"type": "test-note", "payload": {"text": "ok", "secretKey": "a\ud800"}}` (JSON escapes: a NUL and a lone high surrogate in the value)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (not `payload.secretKey`, not 500), no body contains "secretKey", and the part has no item

### Requirement: R6 A teacher moves an item to a position
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}/position` with `{position}` SHALL move the item and return 200 with the item as R1. `position` is a required JSON integer, clamped to 1..count (any integer within the JSON number-length limit); floats and strings are rejected. Shifted items are unchanged; the moved item gets a new `updatedAt`, also on a same-position move, and keeps `version`. Checks: 403, 401, malformed id 404, input 400, unknown id 404.

#### Scenario: items/R6-S1 Move an item down and up, a Russian payload is unchanged
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lesson "Падежи" that has the parts "Разминка" and "Итоги", where "Разминка" has four items of the type `test-note` with the texts "Один", "Два", "Три" and "Четыре" at the positions 1 to 4, all created at a time T with `version` 1, "Итоги" has four items at the positions 1 to 4 created at T, a teacher B with a part with four items created at T, and the clock advanced by one hour
- **WHEN** teacher A moves "Один" to position 3, then advances the clock by one more hour and moves "Четыре" to position 1
- **THEN** the first response is 200 with the text "Один", the type `test-note`, `position` 3, `version` 1 and `updatedAt` T plus one hour, exactly the properties `id`, `type`, `payload`, `position`, `version`, `createdAt` and `updatedAt`, `createdAt` T; the second is 200 with the text "Четыре", `position` 1 and `updatedAt` T plus two hours; the final order of "Разминка" is "Четыре", "Два", "Три", "Один" at the positions 1, 2, 3, 4; the shifted items "Два" and "Три" have `updatedAt` T and `version` 1; "Один" keeps `updatedAt` T plus one hour and its payload and `version` 1; the order and the `updatedAt` of the items of "Итоги" and of teacher B's part are unchanged; and the `updatedAt` of the parts, the lesson and the course is unchanged

#### Scenario: items/R6-S2 Moving to the same position bumps updatedAt, not version
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part with the items of the type `test-note` with the texts "One", "Two" and "Three" created at a time T with `version` 1, and the clock advanced by one hour
- **WHEN** teacher A moves "Two" to position 2
- **THEN** the response is 200 with `position` 2, `version` 1 and `updatedAt` T plus one hour, the order is unchanged, "One" and "Three" have `updatedAt` T and `version` 1, and the `updatedAt` of the part, the lesson and the course is unchanged

#### Scenario: items/R6-S3 Any integer is clamped to the part range
- **GIVEN** a logged-in teacher A with a part "Main" with the items "A", "B", "C" at the positions 1, 2, 3, a part "Single" with the one item "Only" at position 1, a part "Sibling" with four items at the positions 1 to 4, and a teacher B with a part with four items at the positions 1 to 4, all created at a time T, and the clock advanced by one hour
- **WHEN** teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4, and moves "Only" to position 7, then to 0, then to -3
- **THEN** the first four responses are 200 with the positions 3, 1, 1 and 3, the final order of "Main" is "A", "B", "C" without gaps, the last three responses are 200 with `position` 1, "Single" has exactly one item at position 1, and the order and `updatedAt` T of the items of "Sibling" and of teacher B's part are unchanged

#### Scenario: items/R6-S4 The position must be an integer
- **GIVEN** a logged-in teacher A with a part with the items "A" and "B"
- **WHEN** teacher A moves "A" with the body `{}`, with `{"position": null}`, with the position `"2"`, with the position `""`, with `{"position": {"n": 2}}`, with the positions `1.5`, `2.0` and `1e2`, with the position `true`, and with the bodies `{"position":` and no body
- **THEN** the first and the second responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]`, the third to the ninth are 400 with `errors` exactly `[{field: "position", code: "INVALID_TYPE"}]`, the tenth and the eleventh are 400 with code `MALFORMED_REQUEST` and no `errors` property, and the order is still "A", "B" with `updatedAt` and `version` unchanged

#### Scenario: items/R6-S5 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", the items "Half" in "Halves" and "Tenth" in "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос"
- **WHEN** teacher A moves each to position 1: (1) teacher B's item under teacher B's course, lesson and part, (2) teacher B's item under teacher A's own course "Maths", lesson "Fractions" and part "Halves", (3) teacher B's item under "Maths", "Fractions" and teacher B's part, (4) teacher B's item under "Maths", teacher B's lesson and teacher B's part, (5) teacher A's own item "Half" under teacher B's course, lesson and part, (6) an item with an unknown id under "Maths", "Fractions" and "Halves", (7) the item id "not-a-uuid" under the same, (8) "Half" under "Maths", "Fractions" and the part "Quarters" (another part of the same lesson), (9) "Tenth" under "Maths", "Fractions" and "Halves" (a part of another lesson), (10) "Half" under "English B1", "Fractions" and "Halves" (a lesson of another course), (11) "Half" under "Maths", "Fractions" and an unknown part id, (12) "Half" under "Maths", "Fractions" and the part "not-a-uuid", (13) "Half" under "Maths", an unknown lesson id and "Halves", (14) "Half" under "Maths", the lesson "not-a-uuid" and "Halves", (15) "Half" under an unknown course id, (16) "Half" under the course "not-a-uuid"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи", "Разминка" or "Вопрос", and the order, `updatedAt` and `version` of all items are unchanged

#### Scenario: items/R6-S6 Only active teachers move items
- **GIVEN** a part of teacher A with the items "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the order is still "One", "Two"

#### Scenario: items/R6-S7 Checks run in a fixed order
- **GIVEN** teacher A logged in with a course C with a lesson L that has the part P with the item I, a logged-in teacher B with a course "Грамматика" with the lesson "Падежи", the part "Разминка" and its item "Вопрос", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A sends the body `{}` for (1) the item "not-a-uuid" under C, L and P, (1a) the body `{"position":` and (1b) no body under the item "not-a-uuid" under C, L and P, (2) teacher B's item "Вопрос" under teacher B's course, lesson and part, (3) an item with an unknown id under C, L and P, and (4) I under C, L and the part "not-a-uuid"; and the admin and the deactivated teacher A2 each move the item "not-a-uuid" to position 1 under the part "not-a-uuid", the lesson "not-a-uuid" and the course "not-a-uuid"
- **THEN** teacher A's first, (1a) and (1b) responses are 404 with code `NOT_FOUND` and no `errors` property, the second and the third are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]` and do not contain "Падежи", "Разминка" or "Вопрос", the fourth is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), teacher B's item is unchanged, and teacher B without a CSRF token moving the item "not-a-uuid" under the part "not-a-uuid", the lesson "not-a-uuid" and the course "not-a-uuid" gets 403 with code `CSRF_INVALID`

#### Scenario: items/R6-S8 Parallel moves keep positions distinct (the lock chain serialises them)
- **GIVEN** a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends 10 move requests at the same time (`support.Parallel.run`), each moving a different item, addressed by its id, to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
- **THEN** every response is 200, and the part has exactly 10 items with the positions 1 to 10 without gaps and without duplicates, and the final order is a permutation of the original ids

### Requirement: R7 A teacher deletes an item
`DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}` SHALL delete the item of the caller's part and return 204 with no body. Items after it move up by one and keep their `updatedAt` and `version`, so positions stay 1..count; the `updatedAt` of the part, the lesson and the course does not change. A second delete gives 404, and a deletion frees a slot under the R1 limit. Not found cases and the order of checks are as R6, without the input step.

#### Scenario: items/R7-S1 Delete a Russian item in the middle
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the parts "Разминка" and "Итоги", where "Разминка" has four items of the type `test-note` with the texts "Один", "Два", "Три" and "Четыре" at the positions 1 to 4, all created at a time T with `version` 1, "Итоги" has four items at the positions 1 to 4, a teacher B with a part with four items at the positions 1 to 4, all created at T, and the clock advanced by one hour
- **WHEN** teacher A deletes "Два"
- **THEN** the response is 204 with an empty body, "Два" is no longer stored, "Разминка" has "Один", "Три", "Четыре" at the positions 1, 2, 3, all with `updatedAt` T and `version` 1, the order and `updatedAt` of the items of "Итоги" and of teacher B's part are unchanged, and the `updatedAt` of the parts, the lesson and the course is unchanged

#### Scenario: items/R7-S2 A second delete is not found, and a slot is freed
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part with 50 items, where the id of the item at position 10 is read in GIVEN, and the item type `test-note`
- **WHEN** teacher A deletes that item by its id, deletes it again by the same id, and then adds an item of the type `test-note` with the text "Item 51" to the part
- **THEN** the first response is 204, after it the 49 remaining items are at the positions 1 to 49 before the add, the second is 404 with code `NOT_FOUND` and no `errors` property, the third is 201 with `position` 50, and the part has 50 items with the positions 1 to 50 without gaps

#### Scenario: items/R7-S3 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", the items "Half" in "Halves" and "Tenth" in "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос"
- **WHEN** teacher A deletes: (1) teacher B's item under teacher B's course, lesson and part, (2) teacher B's item under teacher A's own course "Maths", lesson "Fractions" and part "Halves", (3) teacher B's item under "Maths", "Fractions" and teacher B's part, (4) teacher B's item under "Maths", teacher B's lesson and teacher B's part, (5) teacher A's own item "Half" under teacher B's course, lesson and part, (6) an item with an unknown id under "Maths", "Fractions" and "Halves", (7) the item id "not-a-uuid" under the same, (8) "Half" under "Maths", "Fractions" and the part "Quarters", (9) "Tenth" under "Maths", "Fractions" and "Halves", (10) "Half" under "English B1", "Fractions" and "Halves", (11) "Half" under "Maths", "Fractions" and an unknown part id, (12) "Half" under "Maths", "Fractions" and the part "not-a-uuid", (13) "Half" under "Maths", an unknown lesson id and "Halves", (14) "Half" under "Maths", the lesson "not-a-uuid" and "Halves", (15) "Half" under an unknown course id, (16) "Half" under the course "not-a-uuid"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи", "Разминка" or "Вопрос", and all three items are still stored

#### Scenario: items/R7-S4 Only active teachers delete items
- **GIVEN** a part of teacher A with an item, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each delete the item, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the item is still stored

#### Scenario: items/R7-S5 Checks run in a fixed order
- **GIVEN** a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher B
- **WHEN** the admin and teacher A each delete the item "not-a-uuid" in the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid", and teacher B does so without a CSRF token
- **THEN** the admin's response is 403 with code `FORBIDDEN`, teacher A's is 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and none is a 404

#### Scenario: items/R7-S6 Parallel deletes and moves end with positions dense and no error
- **GIVEN** a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 5 and 8, and 4 move requests for the items that were at the positions 1, 3, 6 and 10, each to position 1, all addressed by id
- **THEN** none of the responses is 500, the delete responses are 204 and the move responses are 200, and the part ends with exactly 7 items with the positions 1 to 7 without gaps and without duplicates, the three deleted ids are gone, and the 7 remaining ids are a permutation of the 7 survivors

#### Scenario: items/R7-S7 Parallel deletes and adds end within the limit, with positions dense and no error
- **GIVEN** a logged-in teacher A with a part with 50 items, whose ids are read in GIVEN, and the item type `test-note`
- **WHEN** teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 9 and 15, addressed by id, and 5 add requests of the type `test-note` with the texts "New 1" to "New 5"
- **THEN** none of the responses is 500, the delete responses are 204, each add response is 201 or 409 `ITEM_LIMIT_REACHED`, the final count is 47 plus the number of 201 responses and at most 50, every text of a 201 response is in the part, and its positions are 1 to the number of its items without gaps and without duplicates

#### Scenario: items/R7-S8 Deleting items while the part is deleted
- **GIVEN** a logged-in teacher A with a part with 5 items, whose ids are read in GIVEN
- **WHEN** 1 request deleting the part and 4 requests deleting different items of it are sent at the same time (`support.Parallel.run`)
- **THEN** the part delete response is 204, each item delete response is 204 or 404 and none is 500, the part is not stored, and no item row references it
