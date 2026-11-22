## ADDED Requirements

### Requirement: R3 A teacher lists the items of a part
`GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` SHALL return 200 with the items of the caller's part only, as an unpaginated JSON array ordered by `position`, each with the fields R1 returns; no items gives `[]`. A foreign, unknown, malformed or mismatched id gives 404 `NOT_FOUND`. Checks run in order: role (403, no CSRF on a GET), deactivated (401), malformed id (404), unknown or foreign id (404). The read takes no write locks.

#### Scenario: items/R3-S1 Items are listed by position with the item fields
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple" that has the part "Warm-up" with the items of the type `test-note` "Hi" at position 1, "Read the story aloud." at position 2 and "Прочитайте текст вслух: ёж & ёлка." at position 3 (the last with `version` 3), inserted in the order 3, 1, 2 at a time T
- **WHEN** teacher A lists the items of the part
- **THEN** the response is 200 with the texts in the order "Hi", "Read the story aloud.", "Прочитайте текст вслух: ёж & ёлка." with the positions 1, 2, 3, every element has exactly the properties `id`, `type`, `payload`, `position`, `version`, `createdAt` and `updatedAt` (no `partId`, `lessonId`, `courseId` or `teacherId`), the type `test-note`, the versions 1, 1 and 3, `updatedAt` equal to `createdAt`, and every payload is exactly the stored payload with the Cyrillic text unchanged

#### Scenario: items/R3-S2 A part without items gives an empty list, and only that part's items are listed
- **GIVEN** a logged-in teacher A with the lesson "Падежи" with the part "Пусто" with no items, the part "Разминка" with the item "Один", and a teacher B with a part with the item "Один"
- **WHEN** teacher A lists the items of "Пусто", then of "Разминка"
- **THEN** the first response is 200 with an empty array, the second is 200 with exactly one element, the item "Один" of "Разминка"

#### Scenario: items/R3-S3 Another teacher's, unknown, malformed and wrong-parent ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" (with an item) and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" and the part "Разминка" with an item "Секрет"
- **WHEN** teacher A lists the items of: (1) teacher B's part under teacher B's course and lesson, (2) teacher B's part under teacher A's own course "Maths" and lesson "Fractions", (3) teacher B's part under "English B1" and teacher B's lesson, (4) teacher A's own part "Halves" under teacher B's course and lesson, (5) a part with an unknown id under "Maths" and "Fractions", (6) the part id "not-a-uuid" under "Maths" and "Fractions", (7) "Halves" under "Maths" and an unknown lesson id, (8) "Halves" under "Maths" and the lesson "not-a-uuid", (9) "Tenths" under "Maths" and "Fractions", (10) "Halves" under "English B1" and "Fractions", (11) "Halves" under an unknown course id, (12) "Halves" under the course "not-a-uuid"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, and none contains "Грамматика", "Падежи", "Разминка" or "Секрет"

#### Scenario: items/R3-S4 Only active teachers list items
- **GIVEN** a part of teacher A with an item, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
- **WHEN** each of them lists the items of the part
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, and the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`

#### Scenario: items/R3-S5 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** the admin and teacher A2 each list the items of the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A2's is 401 with code `UNAUTHENTICATED`, neither is a 404

### Requirement: R4 A teacher edits an item's payload with its version
`PATCH .../parts/{partId}/items/{itemId}` with `{version, payload}` SHALL replace the payload of the caller's item and return 200 with the item as in R1. A `version` (required integer) other than the stored one gives 409 `VERSION_CONFLICT`, no `errors`. The payload follows the stored type and R2; `type` is ignored. A success adds 1 to `version`. Checks: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404), payload (400), version (409).

#### Scenario: items/R4-S1 Edit an English item: the payload is replaced, the type and position are kept
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part with the items "One" and "Two" and a third item of the type `test-note` at position 3 with the payload `{"text": "Read the story aloud.", "hint": "Page 3"}`, all created at a time T with `version` 1, a teacher B, and the clock advanced by one hour
- **WHEN** teacher A edits the third item with `{"version": 1, "payload": {"text": "Read it twice."}, "type": "poll", "position": 1, "teacherId": <teacher B's id>}`, then advances the clock by one more hour and edits it with `{"version": 2, "payload": {"text": "Read it twice."}}`
- **THEN** the first response is 200 with the same `id`, the type `test-note`, the payload exactly `{"text": "Read it twice."}` (replaced, not merged: no `hint`, no `"hint": null`), `position` 3, `version` 2, `createdAt` T and `updatedAt` T plus one hour, exactly the properties `id`, `type`, `payload`, `position`, `version`, `createdAt` and `updatedAt`, the second is 200 with `version` 3 and `updatedAt` T plus two hours (the payload is unchanged and the version still increases), the stored item has the same type, payload, `version` and times, the items "One" and "Two" keep their positions, `version` 1 and `updatedAt` T, and the `updatedAt` of the part, the lesson and the course is unchanged

#### Scenario: items/R4-S2 Edit a Russian item: Cyrillic text is kept exactly, the version grows
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part with an item of the type `test-note` with the payload `{"text": "Просклоняйте слово «мышь»."}`, created at a time T with `version` 1, and the clock advanced by one hour
- **WHEN** teacher A edits it with `{"version": 1, "payload": {"text": "  Ёж & ёлка: именительный, родительный?  ", "hint": "Ответ: «ёж»"}}`
- **THEN** the response is 200 with a payload whose `text` is exactly "  Ёж & ёлка: именительный, родительный?  " (no trimming) and `hint` "Ответ: «ёж»", `version` 2 and `updatedAt` T plus one hour, and the stored payload has the same values

#### Scenario: items/R4-S3 The version and the payload must be present and of the right JSON type
- **GIVEN** a logged-in teacher A with an item of the type `test-note` with the payload `{"text": "Hi"}` and `version` 1
- **WHEN** teacher A edits it with the bodies `{"payload": {"text": "New"}}`, `{"version": null, "payload": {"text": "New"}}`, `{"version": "1", "payload": {"text": "New"}}`, `{"version": 1.0, "payload": {"text": "New"}}`, `{"version": true, "payload": {"text": "New"}}`, `{"version": 1}`, `{"version": 1, "payload": null}`, `{"version": 1, "payload": []}`, `{"version": 1, "payload": "New"}`, `{}`, `{"version": "1", "payload": []}` (the `version` key first), `{"version": 1, "payload":` and no body
- **THEN** the first two responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "version", code: "REQUIRED"}]`, the next three are 400 with `errors` exactly `[{field: "version", code: "INVALID_TYPE"}]`, the sixth and the seventh are 400 with `errors` exactly `[{field: "payload", code: "REQUIRED"}]`, the eighth and the ninth are 400 with `errors` exactly `[{field: "payload", code: "INVALID_TYPE"}]`, the tenth is 400 with `errors` exactly `[{field: "payload", code: "REQUIRED"}, {field: "version", code: "REQUIRED"}]` (sorted by field), the eleventh is 400 with `errors` exactly `[{field: "version", code: "INVALID_TYPE"}]` (binding stops at the first error), the last two are 400 with code `MALFORMED_REQUEST` and no `errors` property, no body contains "New" as a value, and the item is unchanged with `version` 1

#### Scenario: items/R4-S4 The payload is validated by the stored type, and a bad payload changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `test-note` (a `text` of at most 1000 characters, an optional `hint`) with the payload `{"text": "Hi"}`, `version` 1 and `updatedAt` T
- **WHEN** teacher A edits it with `version` 1 and the payloads `{}`, `{"text": 42}`, `{"text": "<1001 Cyrillic letters>"}`, `{"text": "ok", "extra": {"deep": [1]}}` and `{"text": "a\u0000b"}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]`, `[{field: "payload.text", code: "INVALID_TYPE"}]`, `[{field: "payload.text", code: "TOO_LONG"}]`, `[{field: "payload", code: "UNKNOWN_FIELD"}]` and `[{field: "payload", code: "INVALID_FORMAT"}]`, no body contains "extra", "deep" or a `rejectedValue`, and the item still has the payload `{"text": "Hi"}`, `version` 1 and `updatedAt` T

#### Scenario: items/R4-S5 A stale version is a conflict and changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `test-note` with the payload `{"text": "Привет"}`, `version` 3 and `updatedAt` T, and the clock advanced by one hour
- **WHEN** teacher A edits it with the payload `{"text": "Мир"}` and `version` 2, then 4, then 0, then -1, and then 3
- **THEN** the first four responses are 409 with code `VERSION_CONFLICT` and no `errors` property, and none contains "Привет" or "Мир" (the conflict does not return the stored payload), after them the item still has the payload `{"text": "Привет"}`, `version` 3 and `updatedAt` T, and the last response is 200 with the text "Мир", `version` 4 and `updatedAt` T plus one hour

#### Scenario: items/R4-S6 Another teacher's, unknown, malformed and wrong-parent ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", each with one item (`version` 1), and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Секрет"
- **WHEN** teacher A edits each time with `{"version": 1, "payload": {"text": "Changed"}}`: (1) teacher B's item under teacher B's course, lesson and part, (2) teacher B's item under teacher A's own course "Maths", lesson "Fractions" and part "Halves", (3) teacher A's item of "Halves" under teacher B's course, lesson and part, (4) the item of "Halves" under "Maths", "Fractions" and teacher B's part, (5) an item with an unknown id under "Maths", "Fractions" and "Halves", (6) the item id "not-a-uuid" there, (7) the item of "Quarters" under "Halves", (8) the item of "Tenths" under "Maths", "Fractions" and "Halves", (9) the item of "Halves" under "Maths", "Fractions" and the part "not-a-uuid", (10) under an unknown part id, (11) under the lesson "not-a-uuid", (12) under an unknown lesson id, (13) under "English B1" with "Fractions", (14) under the course "not-a-uuid", (15) under an unknown course id
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи", "Разминка" or "Секрет", and the payload, `version` and `updatedAt` of every item are unchanged

#### Scenario: items/R4-S7 Only active teachers edit items
- **GIVEN** an item of teacher A with the payload `{"text": "Hi"}`, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each edit it with `{"version": 1, "payload": {"text": "Changed"}}`, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the item still has the payload `{"text": "Hi"}` and `version` 1

#### Scenario: items/R4-S8 Checks run in a fixed order
- **GIVEN** teacher A logged in with an item I of type `test-note` with the payload `{"text": "Hi"}` and `version` 1, a course "Грамматика" of teacher B with a part with the item "Секрет", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A sends `{}` for (1) the item "not-a-uuid" under A's own course, lesson and part, (2) teacher B's item, (3) an unknown item id; then `{"version": 1, "payload": {}}` for (4) teacher B's item; then `{"version": 9, "payload": {}}` for (5) I; then `{"version": 9, "payload": {"text": "New"}}` for (6) I; and the admin and the deactivated teacher A2 each send `{}` for the item "not-a-uuid" of the part, lesson and course "not-a-uuid"
- **THEN** (1) is 404 with code `NOT_FOUND` and no `errors` property, (2) and (3) are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "REQUIRED"}, {field: "version", code: "REQUIRED"}]` and do not contain "Секрет", (4) is 404 with code `NOT_FOUND` (not 400 and not 409), (5) is 400 with `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]` (the payload is checked before the version), (6) is 409 with code `VERSION_CONFLICT`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and I and teacher B's item are unchanged

#### Scenario: items/R4-S9 Parallel edits with the same version: one wins
- **GIVEN** a logged-in teacher A with an item of the type `test-note` with `version` 1
- **WHEN** teacher A sends 6 edit requests at the same time (`support.Parallel.run`), all with `version` 1, with the texts "Edit 1" to "Edit 6"
- **THEN** exactly one response is 200 with `version` 2, the other 5 are 409 `VERSION_CONFLICT`, none is 500, and the item has `version` 2 and the text of the successful response

#### Scenario: items/R4-S10 Editing items while the part is deleted
- **GIVEN** a logged-in teacher A with a part with 5 items
- **WHEN** 1 request deleting the part and 4 requests editing one of its items with the current version are sent at the same time (`support.Parallel.run`)
- **THEN** the delete response is 204, each edit response is 200 or 404 and none is 500, the part is not stored, and no item row references it

### Requirement: R5 The theory item type holds Markdown text
The first registered type, `theory`, SHALL have the payload `{text}`, follow R2, and be listed by R3 and edited by R4. `text` is required and not blank (`REQUIRED` otherwise) and has at most 20000 characters (`TOO_LONG`, Java `String` length). It is stored and returned exactly as sent: not trimmed, raw HTML and `$...$` untouched, as showing it is the client's job. Its student view is the payload unchanged.

#### Scenario: items/R5-S1 Add an English theory item, Markdown and a formula are kept exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `theory` with the payload `{"text": "## Present Simple\n\nUse **do/does** with *I, you, we*.\n\n- I work\n- She works\n\nThe formula is $x^2 + y^2 = z^2$ and $$E = mc^2$$.\n"}` (with a trailing newline)
- **THEN** the response is 201 with the type `theory`, `position` 1, `version` 1 and a payload with exactly the property `text`, equal to that text character by character (the newlines, the `$` signs and the trailing newline included), and the stored payload and the listed payload have the same `text`

#### Scenario: items/R5-S2 Add a Russian theory item, Cyrillic Markdown is kept exactly
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `theory` with the payload `{"text": "  ## Падежи\n\nСклоняйте слово **«мышь»**: `ёж` & ёлка.\n\nФормула: $a^2 + b^2 = c^2$.  "}` (with spaces at both ends)
- **THEN** the response is 201 with a `text` exactly equal to the sent text, the leading and trailing spaces included, and the stored payload has the same `text`

#### Scenario: items/R5-S3 Raw HTML is stored as text, not removed
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `theory` with the text `<script>alert(1)</script><b>Hi</b> <img src=x onerror=alert(1)>`
- **THEN** the response is 201 with a `text` exactly equal to the sent text, the stored payload has the same `text`, and the list of the part returns it unchanged

#### Scenario: items/R5-S4 A theory text of 20000 characters is accepted, 20001 is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `theory` with a `text` of 20000 Cyrillic letters ("Я"), then one of 20001 Cyrillic letters ("Я"), then one of 20000 English letters ("a")
- **THEN** the first response is 201 with the `text` exactly as sent and `position` 1, the second is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "TOO_LONG"}]` and no `rejectedValue`, the third is 201 with `position` 2, and the part has exactly 2 items

#### Scenario: items/R5-S5 A blank, wrongly typed or unknown payload is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `theory` with the payloads `{}`, `{"text": null}`, `{"text": ""}`, `{"text": "   "}`, `{"text": "\n\t  "}`, `{"text": 5}`, `{"text": ["Привет"]}` and `{"text": "ok", "title": "Intro"}`
- **THEN** the first five responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]`, the next two are 400 with `errors` exactly `[{field: "payload.text", code: "INVALID_TYPE"}]`, the last is 400 with `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (the property name is not echoed), no body contains "Привет" or "Intro", and the part has no item

#### Scenario: items/R5-S6 Editing a theory item follows the theory rules
- **GIVEN** a logged-in teacher A with an item of the type `theory` with the payload `{"text": "Old"}` and `version` 1, created at a time T, and the clock advanced by one hour
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"text": "   "}`, `{"text": "<20001 Cyrillic letters>"}` and `{"text": "ok", "hint": "x"}` (`hint` is a property of another type), and then with `{"text": "<b>Привет</b> $x^2$"}`
- **THEN** the first response is 400 with `errors` exactly `[{field: "payload.text", code: "REQUIRED"}]`, the second `[{field: "payload.text", code: "TOO_LONG"}]`, the third `[{field: "payload", code: "UNKNOWN_FIELD"}]`, the item is unchanged after them, and the last response is 200 with the type `theory`, the `text` exactly as sent, `version` 2 and `updatedAt` T plus one hour
