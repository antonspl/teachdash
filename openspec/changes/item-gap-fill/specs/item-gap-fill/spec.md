## ADDED Requirements

### Requirement: R1 A teacher adds and edits a gap-fill item, and the student view hides the answers
The item type `gap-fill` SHALL have the payload `{segments}`: segments `{type: "text", text}` and `{type: "gap", answers: [string]}`, added with items R1 and edited with items R4. A gap is its 0-based position among the gaps. Text and answers are stored exactly as sent. `GapFillItemType.studentView` SHALL be an allowlist: text segments with `text`, gaps as `{type: "gap"}` only. No student endpoint exists yet (#125), so only unit scenarios cover the view.


#### Scenario: item-gap-fill/R1-S1 Add an English gap-fill item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with the payload `{"segments": [{"type": "text", "text": "I "}, {"type": "gap", "answers": ["go", "walk"]}, {"type": "text", "text": " to school every day."}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `gap-fill`, `position` 1, `version` 1 and exactly the payload that was sent (the segments in the same order, `go` before `walk`), no segment has a property it was not sent with (no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-gap-fill/R1-S2 Add a Russian gap-fill item, Cyrillic is kept exactly, "ё" and "е" stay distinct
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with the payload `{"segments": [{"type": "text", "text": "  Вставьте слово: «Ёж» и "}, {"type": "gap", "answers": ["ёлка", "елка", "Ёлка"]}, {"type": "text", "text": ".  "}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the leading and trailing spaces of the text segments, the guillemets and the three answers "ёлка", "елка" and "Ёлка" as three distinct answers in this order), and the stored payload has the same segments

#### Scenario: item-gap-fill/R1-S3 Answers are stored raw, with no trim, case change or normalisation
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with one gap whose answers are `" go "`, `"GO"`, `"go"`, `"é"` (U+00E9) and `"é"` written as "e" followed by U+0301 (the same letter, decomposed)
- **THEN** the response is 201 and the answers are exactly these five strings in this order (the spaces kept, "GO" and "go" both kept, the composed and the decomposed "é" both kept as different strings), and the stored payload has the same five answers

#### Scenario: item-gap-fill/R1-S4 A gap may be first, last, next to another gap or the only segment
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the segments `[gap, text, gap]`, `[gap, gap, text]` (two adjacent gaps), `[text, gap, gap]` and `[gap]`
- **THEN** all four responses are 201 with the segments exactly as sent, in the positions 1 to 4

#### Scenario: item-gap-fill/R1-S5 Edit a gap-fill item: the payload is replaced, the gaps are numbered anew
- **GIVEN** a logged-in teacher A with an item of the type `gap-fill` with the segments `[text "She ", gap ["is"], text " a teacher and he ", gap ["is"], text " a doctor."]` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `[gap ["She"], text " is a teacher and he ", gap ["is", "'s"], text " a doctor."]` (the first gap is now a different one, and one gap has two answers)
- **THEN** the response is 200 with the type `gap-fill`, the new payload exactly as sent (replaced, not merged), `version` 2 and the same `position`, and the stored payload has the new segments, so the gap at index 1 now has the answers "is" and "'s"

#### Scenario: item-gap-fill/R1-S6 Editing a gap-fill item follows the gap-fill rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `gap-fill` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"segments": [{"type": "text", "text": "No gap here."}]}` and `{"segments": [{"type": "gap", "answers": []}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "REQUIRED"}]` and `[{field: "payload.segments[0].answers", code: "REQUIRED"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-gap-fill/R1-S7 Another teacher cannot add or edit a gap-fill item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a gap-fill item in it
- **WHEN** teacher B adds a gap-fill item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-gap-fill/R1-S8 The student view keeps the text and shows each gap as a bare gap
- **GIVEN** the `gap-fill` type and a payload with the segments `[text "I ", gap ["go", "walk"], text " to school.", gap ["x"]]` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with the payload
- **THEN** the result is four segments in the same order: `{type: "text", text: "I "}`, `{type: "gap"}`, `{type: "text", text: " to school."}` and `{type: "gap"}`, a gap has no other property (no answers, no count, no length, no index), and the two gaps with different answer lists give identical views

#### Scenario: item-gap-fill/R1-S9 The serialised student view contains no accepted answer, in English and Russian
- **GIVEN** the `gap-fill` type and two payloads: an English one with the gaps `["goes", "walks"]` and `["brings"]`, and a Russian one with the gaps `["Ёжик", "ёжик", "ежик"]` and `["мышь"]`, each with text segments that do not contain any answer
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of neither view contains any accepted answer string as a substring, nor the property name `answers`, and it contains every text segment exactly

### Requirement: R2 A gap-fill payload follows the limits and structure rules
A `gap-fill` payload SHALL follow items R2, with paths `payload.segments[i]...`. Limits (`TOO_LONG`): 101 segments, 50 gaps, 1000 characters per text, 10 answers per gap, 200 per answer (UTF-16). JSON types and list or text limits are checked first; then, together: segment `type`, blank text, gaps without answers or with blank answers, fields of the other segment type, adjacent texts, exact duplicate answers in a gap, no gap. `null` counts as absent.


#### Scenario: item-gap-fill/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with: (a) 51 text segments of 1000 English letters ("a") alternating with 50 gaps (101 segments), each gap with the answer "go"; (b) one gap with 10 different answers of 200 Cyrillic letters each ("Я" repeated, then a distinguishing letter at the end of each); (c) a text segment of 1000 Cyrillic letters ("Я") and a gap with one answer of 100 emoji "😀" (200 UTF-16 code units)
- **THEN** all three responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-gap-fill/R2-S2 A text, an answer, an answer list or a segment list over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with: (a) a text segment of 1001 Cyrillic letters ("Я") and a valid gap; (b) a gap with an answer of 201 English letters ("a"); (c) a gap with an answer of 101 emoji "😀" (202 UTF-16 code units); (d) a gap with 11 different answers; (e) 102 segments (51 gaps alternating with 51 texts)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]`, `[{field: "payload.segments[0].answers[0]", code: "TOO_LONG"}]`, `[{field: "payload.segments[0].answers[0]", code: "TOO_LONG"}]`, `[{field: "payload.segments[0].answers", code: "TOO_LONG"}]` and `[{field: "payload.segments", code: "TOO_LONG"}]` (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-gap-fill/R2-S3 More than 50 gaps are too many
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with 51 adjacent gaps, each with the answer "go" (51 segments, within the segment limit)
- **THEN** the response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "TOO_LONG"}]`, and the part has no item; the same payload with 50 gaps is accepted with 201

#### Scenario: item-gap-fill/R2-S4 A blank or missing text, and a payload without a gap, are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the segments: (a) `[gap ["go"], {"type": "text"}]`; (b) `[gap ["go"], text ""]`; (c) `[gap ["go"], text "  \n\t"]`; (d) only `[text "No gap here."]`; and with the payloads (e) `{"segments": []}`, (f) `{"segments": null}` and (g) `{}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "REQUIRED"}]` for (a), (b) and (c), and `[{field: "payload.segments", code: "REQUIRED"}]` for (d), (e), (f) and (g), and the part has no item

#### Scenario: item-gap-fill/R2-S5 A gap needs answers, and every answer must be non-blank
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the gap `{"type": "gap"}` (no `answers`), `{"type": "gap", "answers": null}`, `{"type": "gap", "answers": []}`, `{"type": "gap", "answers": ["go", ""]}`, `{"type": "gap", "answers": ["  ", "go"]}` and `{"type": "gap", "answers": ["go", null]}`, each as the second segment after a text segment
- **THEN** the first three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].answers", code: "REQUIRED"}]`, the next three have `errors` exactly `[{field: "payload.segments[1].answers[1]", code: "REQUIRED"}]`, `[{field: "payload.segments[1].answers[0]", code: "REQUIRED"}]` and `[{field: "payload.segments[1].answers[1]", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-gap-fill/R2-S6 A repeated answer in a gap is rejected only on an exact match
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with the gap answers `["go", "walk", "go"]`, then one with `["Ёлка", "ёлка", "елка", "Ёлка"]`, and then one with `["go", "Go", " go", "go "]` and, in another gap of the same item, `["go"]`
- **THEN** the first response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[0].answers[2]", code: "INVALID_FORMAT"}]` (the later answer), the second is 400 with `errors` exactly `[{field: "payload.segments[0].answers[3]", code: "INVALID_FORMAT"}]`, the third is 201 with all answers exactly as sent (different case, a leading and a trailing space make different answers; the same answer in two gaps is allowed), and the part has exactly one item

#### Scenario: item-gap-fill/R2-S7 Two adjacent text segments are rejected on the second
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the segments `[text "A", text "B", gap ["go"]]` and `[gap ["go"], text "A", text "B", text "C"]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}]` and `[{field: "payload.segments[2]", code: "INVALID_FORMAT"}, {field: "payload.segments[3]", code: "INVALID_FORMAT"}]`, and the part has no item

#### Scenario: item-gap-fill/R2-S8 A field of the other segment type, or an unknown property, is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with: (a) a gap with a `text` ("go"); (b) a text segment with `answers: ["go"]`; (c) a text segment with `answers: []`; (d) a segment `{"type": "text", "text": "Hi", "hint": "x"}`; (e) the payload `{"segments": [...valid...], "title": "Intro"}`; (f) a gap with `"id": 1`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "UNKNOWN_FIELD"}]` for (a), `[{field: "payload.segments[0].answers", code: "UNKNOWN_FIELD"}]` for (b) and (c), and `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (d), (e) and (f) (the property name is not echoed), and the part has no item

#### Scenario: item-gap-fill/R2-S9 A missing, blank or unknown segment type and a null segment are rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the segments (each after a valid gap): `[{"text": "Hi"}]`, `[{"type": null, "text": "Hi"}]`, `[{"type": " ", "text": "Hi"}]`, `[{"type": "image", "text": "Hi"}]`, `[{"type": "Gap", "answers": ["go"]}]` and `[null]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].type", code: "REQUIRED"}]` for the first three, `[{field: "payload.segments[1].type", code: "INVALID_FORMAT"}]` for the next two, and `[{field: "payload.segments[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-gap-fill/R2-S10 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill` with the payloads: `{"segments": "text"}`, `{"segments": {}}`, `{"segments": ["text"]}`, `{"segments": [{"type": 5, "text": "Hi"}]}`, `{"segments": [{"type": "text", "text": ["Привет"]}]}`, `{"segments": [{"type": "gap", "answers": "go"}]}` and `{"segments": [{"type": "gap", "answers": ["go", 5]}]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.segments[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].type", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].text", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].answers", code: "INVALID_TYPE"}]` and `[{field: "payload.segments[0].answers[1]", code: "INVALID_TYPE"}]`, no body contains "Привет", and the part has no item

#### Scenario: item-gap-fill/R2-S11 Limits are checked first, the structure rules after, and all structure errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `gap-fill` with the segments `[text <1001 English letters>, gap ["go", "go"]]`, then one with `[text "A", text "B", gap ["go", "go"], gap ["", "x"]]`, then one with the first payload's text shortened to "A"
- **THEN** the first response is 400 with `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]` (the duplicate is not reported yet), the second is 400 with `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[2].answers[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[3].answers[0]", code: "REQUIRED"}]` (sorted by field), the third is 400 with `errors` exactly `[{field: "payload.segments[1].answers[1]", code: "INVALID_FORMAT"}]`, and the part has no item
