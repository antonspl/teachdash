## ADDED Requirements

### Requirement: R6 A teacher moves an item to a position
`PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}/position` with `{position}` SHALL move the item and return 200 with the item as in R1. `position` is a required JSON integer, clamped to 1..count; floats and strings are rejected. Shifted items keep `updatedAt` and `version`; the moved item gets a new `updatedAt`, also on a same-position move, but keeps `version` and payload. Checks in order: 403, 401, malformed id 404, input 400, unknown or foreign id 404.

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
- **GIVEN** teacher A logged in with a course C with a lesson L that has the part P with the item I, a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос", a logged-in admin, and a logged-in teacher A2 who is then deactivated
- **WHEN** teacher A sends the body `{}` for (1) the item "not-a-uuid" under C, L and P, (2) teacher B's item "Вопрос" under teacher B's course, lesson and part, (3) an item with an unknown id under C, L and P, and (4) I under C, L and the part "not-a-uuid"; and the admin and the deactivated teacher A2 each move the item "not-a-uuid" to position 1 under the part "not-a-uuid", the lesson "not-a-uuid" and the course "not-a-uuid"
- **THEN** teacher A's first response is 404 with code `NOT_FOUND` and no `errors` property, the second and the third are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "position", code: "REQUIRED"}]` and do not contain "Падежи", "Разминка" or "Вопрос", the fourth is 404 with code `NOT_FOUND`, the admin's response is 403 with code `FORBIDDEN` and the deactivated teacher's is 401 with code `UNAUTHENTICATED` (neither is 404 or 400), and teacher B's item is unchanged

#### Scenario: items/R6-S8 Parallel moves keep positions distinct (the lock chain serialises them)
- **GIVEN** a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends 10 move requests at the same time (`support.Parallel.run`), each moving a different item, addressed by its id, to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
- **THEN** every response is 200, and the part has exactly 10 items with the positions 1 to 10 without gaps and without duplicates

### Requirement: R7 A teacher deletes an item
`DELETE /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}` SHALL delete the item of the caller's part and return 204 with no body. Items after it move up by one and keep their `updatedAt` and `version`, so positions stay 1..count; the `updatedAt` of the part, the lesson and the course does not change. A second delete gives 404, and a deletion frees a slot under the R1 limit. Not found cases and the order of checks are as R6, without the input step.

#### Scenario: items/R7-S1 Delete a Russian item in the middle
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the parts "Разминка" and "Итоги", where "Разминка" has four items of the type `test-note` with the texts "Один", "Два", "Три" and "Четыре" at the positions 1 to 4, all created at a time T with `version` 1, "Итоги" has four items at the positions 1 to 4, a teacher B with a part with four items at the positions 1 to 4, all created at T, and the clock advanced by one hour
- **WHEN** teacher A deletes "Два"
- **THEN** the response is 204 with an empty body, "Два" is no longer stored, "Разминка" has "Один", "Три", "Четыре" at the positions 1, 2, 3, all with `updatedAt` T and `version` 1, the order and `updatedAt` of the items of "Итоги" and of teacher B's part are unchanged, and the `updatedAt` of the parts, the lesson and the course is unchanged

#### Scenario: items/R7-S2 A second delete is not found, and a slot is freed
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part with 50 items, where the id of the item at position 10 is read in GIVEN, and the item type `test-note`
- **WHEN** teacher A deletes that item by its id, deletes it again by the same id, and then adds an item of the type `test-note` with the text "Item 51" to the part
- **THEN** the first response is 204, the second is 404 with code `NOT_FOUND` and no `errors` property, the third is 201 with `position` 50, and the part has 50 items with the positions 1 to 50 without gaps

#### Scenario: items/R7-S3 Another teacher's, unknown, malformed and misplaced ids are not found
- **GIVEN** teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", the items "Half" in "Halves" and "Tenth" in "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос"
- **WHEN** teacher A deletes: (1) teacher B's item under teacher B's course, lesson and part, (2) teacher B's item under teacher A's own course "Maths", lesson "Fractions" and part "Halves", (3) teacher B's item under "Maths", "Fractions" and teacher B's part, (4) teacher B's item under "Maths", teacher B's lesson and teacher B's part, (5) teacher A's own item "Half" under teacher B's course, lesson and part, (6) an item with an unknown id under "Maths", "Fractions" and "Halves", (7) the item id "not-a-uuid" under the same, (8) "Half" under "Maths", "Fractions" and the part "Quarters", (9) "Tenth" under "Maths", "Fractions" and "Halves", (10) "Half" under "English B1", "Fractions" and "Halves", (11) "Half" under "Maths", "Fractions" and an unknown part id, (12) "Half" under "Maths", "Fractions" and the part "not-a-uuid", (13) "Half" under "Maths", an unknown lesson id and "Halves", (14) "Half" under "Maths", the lesson "not-a-uuid" and "Halves", (15) "Half" under an unknown course id, (16) "Half" under the course "not-a-uuid"
- **THEN** all responses are 404 with code `NOT_FOUND` and no `errors` property, none contains "Грамматика", "Падежи", "Разминка" or "Вопрос", and all three items are still stored

#### Scenario: items/R7-S4 Only active teachers delete items
- **GIVEN** a part of teacher A with an item, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
- **WHEN** the admin, the student, teacher A and the client each delete the item, and teacher B does so without a CSRF token
- **THEN** the admin's and the student's responses are 403 with code `FORBIDDEN`, the deactivated teacher's and the client's responses are 401 with code `UNAUTHENTICATED`, teacher B's is 403 with code `CSRF_INVALID`, and the item is still stored

#### Scenario: items/R7-S5 Checks run in a fixed order
- **GIVEN** a logged-in admin, and a logged-in teacher A who is then deactivated
- **WHEN** the admin and teacher A each delete the item "not-a-uuid" in the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
- **THEN** the admin's response is 403 with code `FORBIDDEN` and teacher A's is 401 with code `UNAUTHENTICATED`, neither is a 404

#### Scenario: items/R7-S6 Parallel deletes and moves end with positions dense and no error
- **GIVEN** a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
- **WHEN** teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 5 and 8, and 4 move requests for the items that were at the positions 1, 3, 6 and 10, each to position 1, all addressed by id
- **THEN** none of the responses is 500, the delete responses are 204 and the move responses are 200, and the part ends with exactly 7 items with the positions 1 to 7 without gaps and without duplicates

#### Scenario: items/R7-S7 Parallel deletes and adds end within the limit, with positions dense and no error
- **GIVEN** a logged-in teacher A with a part with 50 items, whose ids are read in GIVEN, and the item type `test-note`
- **WHEN** teacher A sends at the same time 3 delete requests for the items that were at the positions 2, 9 and 15, addressed by id, and 5 add requests of the type `test-note` with the texts "New 1" to "New 5"
- **THEN** none of the responses is 500, the delete responses are 204, each add response is 201 or 409 `ITEM_LIMIT_REACHED`, the part ends with at most 50 items, and its positions are 1 to the number of its items without gaps and without duplicates

#### Scenario: items/R7-S8 Deleting items while the part is deleted
- **GIVEN** a logged-in teacher A with a part with 5 items, whose ids are read in GIVEN
- **WHEN** 1 request deleting the part and 4 requests deleting different items of it are sent at the same time (`support.Parallel.run`)
- **THEN** the part delete response is 204, each item delete response is 204 or 404 and none is 500, the part is not stored, and no item row references it
