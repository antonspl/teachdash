## ADDED Requirements

### Requirement: R1 A teacher adds and edits a dropdown item, and the student view hides the correct mark
The type `dropdown` SHALL have the payload `{segments}` of `{type: "text", text}` and `{type: "gap", options: [string], correct: int}` (0-based index), added with items R1 and edited with items R4, stored as sent. `studentView` SHALL return `{"segments": [...]}` as an allowlist: gaps as `{type: "gap", options}`, never `correct`; it throws on corrupt data (no scenario). Unit scenarios only (#125). `gap ["a", "b"] -> 1` is a gap with `correct` 1.

#### Scenario: item-dropdown/R1-S1 Add an English dropdown item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `dropdown` with the segments `[text "She ", gap ["is", "was", "are"] -> 1, text " a teacher yesterday."]` and then lists the items of the part
- **THEN** the add response is 201 with the type `dropdown`, `position` 1, `version` 1 and exactly the payload that was sent (the options in the same order, `correct` 1), no segment has a property it was not sent with (no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-dropdown/R1-S2 Add a Russian dropdown item, Cyrillic is kept exactly, "ё" and "е" stay distinct
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `dropdown` with the segments `[text "  Выберите слово: «Ёж» и ", gap ["ёлка", "елка", "Ёлка"] -> 2, text ".  "]`
- **THEN** the response is 201 and the payload equals the sent one character by character (the leading and trailing spaces of the texts, the guillemets, the three options "ёлка", "елка" and "Ёлка" as three distinct options in this order, `correct` 2), and the stored payload has the same segments

#### Scenario: item-dropdown/R1-S3 Options are stored raw, with no trim, case change or normalisation
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `dropdown` with one gap whose options are `" go "`, `"GO"`, `"go"`, `"é"` (U+00E9) and `"é"` written as "e" followed by U+0301 (the same letter, decomposed), with `correct` 2
- **THEN** the response is 201 and the options are exactly these five strings in this order (the spaces kept, "GO" and "go" both kept, the composed and the decomposed "é" both kept as different strings) with `correct` 2, and the stored payload has the same five options and `correct`

#### Scenario: item-dropdown/R1-S4 A gap may be first, last, next to another gap or the only segment
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[gap, text, gap]`, `[gap, gap, text]` (two adjacent gaps), `[text, gap, gap]` and `[gap]`, each gap with two options
- **THEN** all four responses are 201 with the segments exactly as sent, in the positions 1 to 4

#### Scenario: item-dropdown/R1-S5 Edit a dropdown item: the payload is replaced, the options and the correct index are replaced together
- **GIVEN** a logged-in teacher A with an item of the type `dropdown` with the segments `[text "She ", gap ["is", "are"] -> 0, text " a teacher and he ", gap ["is", "are"] -> 0, text " a doctor."]` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the segments `[gap ["She", "He"] -> 0, text " is a teacher and he ", gap ["is", "'s", "are"] -> 1, text " a doctor."]`
- **THEN** the response is 200 with the type `dropdown`, the new payload exactly as sent (replaced, not merged), `version` 2 and the same `position`, and the stored payload has the new segments, so the gap at index 1 now has three options and `correct` 1

#### Scenario: item-dropdown/R1-S6 Editing a dropdown item follows the dropdown rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `dropdown` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the segments `[text "No gap here."]` and `[gap ["a", "b"] -> 2]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "REQUIRED"}]` and `[{field: "payload.segments[0].correct", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-dropdown/R1-S7 Another teacher cannot add or edit a dropdown item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a dropdown item in it
- **WHEN** teacher B adds a dropdown item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-dropdown/R1-S8 The student view keeps the text and shows each gap with its options and no correct mark
- **GIVEN** the `dropdown` type and a payload with the segments `[text "I ", gap ["go", "goes", "went"] -> 1, text " to school.", gap ["a", "b"] -> 0]` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with the payload
- **THEN** the result is `{"segments": [...]}` with exactly the property `segments` and four segments in the same order: `{type: "text", text: "I "}`, `{type: "gap", options: ["go", "goes", "went"]}`, `{type: "text", text: " to school."}` and `{type: "gap", options: ["a", "b"]}`, a gap has no other property (no `correct`, no index), the options keep the stored order, and two payloads that differ only in `correct` give identical views

#### Scenario: item-dropdown/R1-S9 The serialised student view contains no `correct` key, in English and Russian
- **GIVEN** the `dropdown` type and two payloads: an English one with the gaps `["goes", "walks"] -> 1` and `["brings", "bring", "bringing"] -> 0`, and a Russian one with the gaps `["Ёжик", "ёжик", "ежик"] -> 2` and `["мышь", "мыши"] -> 0`, each with text segments that do not contain the word "correct"
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with the property `segments`, it contains no `correct` property name, each gap's `options` appear exactly and in stored order, and every text segment appears exactly

### Requirement: R2 A dropdown payload follows the limits and structure rules
A `dropdown` payload SHALL follow items R2. Limits (UTF-16): 51 segments, 25 gaps (`TOO_LONG` at `segments`), 500 characters per text, 2 to 5 options (`TOO_SHORT`, `TOO_LONG`), 100 per option. A gap SHALL have `options` and `correct` (`REQUIRED`), blank options `REQUIRED`, duplicates and `correct` out of `0..size-1` `INVALID_FORMAT` (range skipped if `options` has an error). Text is `REQUIRED` only if missing, `null` or "". Bean Validation, then `extraChecks` (limits first).

#### Scenario: item-dropdown/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with: (a) 26 text segments of 500 English letters ("a") alternating with 25 gaps (51 segments), each gap `["go", "went"] -> 1`; (b) one gap with 5 different options of 100 Cyrillic letters each ("Я" repeated, 99 times, then one distinguishing letter: 100 characters in total) and `correct` 4; (c) a text segment of 500 Cyrillic letters ("Я") and a gap with the options `["x", <50 emoji "😀" (100 UTF-16 code units)>]` and `correct` 0; (d) a text segment of 250 emoji "😀" (500 UTF-16 code units) and a gap `["go", "went"] -> 0`
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-dropdown/R2-S2 A text, an option, an option list, a segment list or a gap count over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with: (a) a text segment of 501 Cyrillic letters ("Я") and a valid gap; (b) a text segment of 251 emoji "😀" (502 UTF-16 code units) and a valid gap; (c) a gap with an option of 101 English letters ("a") and another option "b"; (d) a gap with an option of 51 emoji "😀" (102 UTF-16 code units) and another option "b"; (e) a gap with 6 different options; (f) 52 segments (26 gaps alternating with 26 texts); (g) 26 adjacent gaps, each `["go", "went"] -> 0` (26 segments, within the segment limit)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]` for (a) and (b), `[{field: "payload.segments[0].options[0]", code: "TOO_LONG"}]` for (c) and (d), `[{field: "payload.segments[0].options", code: "TOO_LONG"}]` for (e) and `[{field: "payload.segments", code: "TOO_LONG"}]` for (f) and (g) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-dropdown/R2-S3 A missing or empty text, and a payload without a gap, are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments: (a) `[gap ["a", "b"] -> 0, {"type": "text"}]`; (b) `[gap ["a", "b"] -> 0, {"type": "text", "text": null}]`; (c) `[gap ["a", "b"] -> 0, text ""]`; (d) only `[text "No gap here."]`; and with the payloads (e) `{"segments": []}`, (f) `{"segments": null}` and (g) `{}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "REQUIRED"}]` for (a), (b) and (c), and `[{field: "payload.segments", code: "REQUIRED"}]` for (d), (e), (f) and (g), and the part has no item

#### Scenario: item-dropdown/R2-S4 A gap needs 2 to 5 options, and every option must be non-blank
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the gap `{"type": "gap", "correct": 0}` (no `options`), `{"type": "gap", "options": null, "correct": 0}`, `{"type": "gap", "options": [], "correct": 0}`, `{"type": "gap", "options": ["a"], "correct": 0}`, `["go", ""] -> 0`, `["  ", "go"] -> 0` and `["go", null] -> 0`, each as the second segment after a text segment
- **THEN** the first two responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].options", code: "REQUIRED"}]` (the gap with no options still counts as a gap, so there is no `payload.segments` error), the next two have `errors` exactly `[{field: "payload.segments[1].options", code: "TOO_SHORT"}]`, the last three have `errors` exactly `[{field: "payload.segments[1].options[1]", code: "REQUIRED"}]`, `[{field: "payload.segments[1].options[0]", code: "REQUIRED"}]` and `[{field: "payload.segments[1].options[1]", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-dropdown/R2-S5 A repeated option in a gap is rejected only on an exact match
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `dropdown` with the gap `["go", "walk", "go"] -> 0`, then one with `["Ёлка", "ёлка", "елка", "Ёлка"] -> 0` (5 options are the limit, 4 are used), and then one with `["go", "Go", " go", "go "] -> 3` and, in another gap of the same item, `["go", "stop"] -> 0`
- **THEN** the first response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[0].options[2]", code: "INVALID_FORMAT"}]` (the later option), the second is 400 with `errors` exactly `[{field: "payload.segments[0].options[3]", code: "INVALID_FORMAT"}]`, the third is 201 with all options and `correct` values exactly as sent (different case, a leading and a trailing space make different options; the same option in two gaps is allowed), and the part has exactly one item

#### Scenario: item-dropdown/R2-S6 Two adjacent text segments are rejected on the second
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[text "A", text "B", gap ["a", "b"] -> 0]` and `[gap ["a", "b"] -> 0, text "A", text "B", text "C"]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}]` and `[{field: "payload.segments[2]", code: "INVALID_FORMAT"}, {field: "payload.segments[3]", code: "INVALID_FORMAT"}]`, and the part has no item

#### Scenario: item-dropdown/R2-S7 A field of the other segment type, or an unknown property, is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the payloads: (a) `[text "Hi", {"type": "gap", "text": "go", "options": ["a", "b"], "correct": 0}]`; (b) `[{"type": "text", "text": "Hi", "options": ["a", "b"]}, gap ["a", "b"] -> 0]`; (c) `[{"type": "text", "text": "Hi", "correct": 0}, gap ["a", "b"] -> 0]`; (d) `[{"type": "text", "text": "Hi", "hint": "x"}, gap ["a", "b"] -> 0]`; (e) `{"segments": [text "Hi", gap ["a", "b"] -> 0], "caption": "Intro"}`; (f) `[text "Hi", {"type": "gap", "id": "seg-7731", "options": ["a", "b"], "correct": 0}]`; (g) `[{"type": "text", "text": "Hi", "options": ["a"]}, gap ["a", "b"] -> 0]`; (h) `[{"type": "text", "text": "Hi", "options": null, "correct": null}, {"type": "gap", "text": null, "options": ["a", "b"], "correct": 0}]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "UNKNOWN_FIELD"}]` for (a), `[{field: "payload.segments[0].options", code: "UNKNOWN_FIELD"}]` for (b), `[{field: "payload.segments[0].correct", code: "UNKNOWN_FIELD"}]` for (c) (a known property in the wrong segment type, reported at its path), `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (d), (e) and (f) (a truly unknown property; the name is not echoed) and `[{field: "payload.segments[0].options", code: "TOO_SHORT"}]` for (g) (Bean Validation runs first, so not `UNKNOWN_FIELD`); the responses for (a) to (g) leave the part with no item, and (h) is 201 (an explicit `null` of a field of the other type is treated as absent) with a stored payload that has no `options`, `correct` or `text` key on those segments

#### Scenario: item-dropdown/R2-S8 A missing, blank or unknown segment type and a null segment are rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments (each after a valid gap): `[{"text": "Hi"}]`, `[{"type": null, "text": "Hi"}]`, `[{"type": " ", "text": "Hi"}]`, `[{"type": "image", "text": "Hi"}]`, `[{"type": "Gap", "options": ["a", "b"], "correct": 0}]` and `[null]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].type", code: "REQUIRED"}]` for the first three, `[{field: "payload.segments[1].type", code: "INVALID_FORMAT"}]` for the next two, and `[{field: "payload.segments[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-dropdown/R2-S9 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the payloads: `{"segments": "text"}`, `{"segments": {}}`, `{"segments": ["text"]}`, `{"segments": [{"type": 5, "text": "Hi"}]}`, `{"segments": [{"type": "text", "text": ["Привет"]}]}`, `{"segments": [{"type": "gap", "options": "go", "correct": 0}]}`, `{"segments": [{"type": "gap", "options": ["go", 5], "correct": 0}]}`, `{"segments": [{"type": "gap", "options": ["a", "b"], "correct": "1"}]}` `{"segments": [{"type": "gap", "options": ["a", "b"], "correct": true}]}`, `{"segments": [{"type": "gap", "options": ["a", "b"], "correct": 1.5}]}` and `{"segments": [{"type": "gap", "options": ["a", "b"], "correct": 2147483648}]}` (outside the `Integer` range)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.segments[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].type", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].text", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].options", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].options[1]", code: "INVALID_TYPE"}]` and `[{field: "payload.segments[0].correct", code: "INVALID_TYPE"}]` for the last four, no body contains "Привет", and the part has no item

#### Scenario: item-dropdown/R2-S10 `correct` is required and must point at an option
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the gap (as the second segment after a text segment): (a) `{"type": "gap", "options": ["a", "b"]}`; (b) `{"type": "gap", "options": ["a", "b"], "correct": null}`; (c) `["a", "b"] -> -1`; (d) `["a", "b"] -> 2`; (e) `["go", "go"] -> 7`; (f) no `options` and `correct` 7; (g) `["", "x"] -> 7`; (h) neither `options` nor `correct`; (i) `["go", "go"]` and no `correct`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].correct", code: "REQUIRED"}]` for (a) and (b), `[{field: "payload.segments[1].correct", code: "INVALID_FORMAT"}]` for (c) and (d), `[{field: "payload.segments[1].options[1]", code: "INVALID_FORMAT"}]` for (e) (the range check is skipped when `options` has an error), `[{field: "payload.segments[1].options", code: "REQUIRED"}]` for (f), `[{field: "payload.segments[1].options[0]", code: "REQUIRED"}]` for (g) and `[{field: "payload.segments[1].correct", code: "REQUIRED"}, {field: "payload.segments[1].options", code: "REQUIRED"}]` for (h) (sorted by field) and `[{field: "payload.segments[1].correct", code: "REQUIRED"}, {field: "payload.segments[1].options[1]", code: "INVALID_FORMAT"}]` for (i), and the part has no item

#### Scenario: item-dropdown/R2-S11 Limits are checked first, the structure rules after, and all structure errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[text <501 English letters>, gap ["go", "go"] -> 0]`, then `[text "A", text "B", gap ["go", "go"] -> 5, gap ["", "x"] -> 0, gap ["a", "b"] -> 9]`, then `[text "A", gap ["go", "go"] -> 0]` (the first payload's text shortened to "A"), and then `[text "A", gap ["a", "b"] -> 0]` where the gap also carries a `text` of 501 English letters, and then a gap with 6 options, one of 101 English letters, and `correct` 0, and then `[text "A", text "B", gap [<101 English letters "a">, "b"] -> 0]`
- **THEN** the first response is 400 with `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]` (the duplicate is not reported yet), the second is 400 with `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[2].options[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[3].options[0]", code: "REQUIRED"}, {field: "payload.segments[4].correct", code: "INVALID_FORMAT"}]` (sorted by field), the third is 400 with `errors` exactly `[{field: "payload.segments[1].options[1]", code: "INVALID_FORMAT"}]`, the fourth is 400 with `errors` exactly `[{field: "payload.segments[1].text", code: "TOO_LONG"}]` (Bean Validation runs first, so not `UNKNOWN_FIELD`), and the fifth is 400 with `errors` exactly `[{field: "payload.segments[0].options", code: "TOO_LONG"}]` (the over-long option is not reported yet), and the sixth is 400 with `errors` exactly `[{field: "payload.segments[2].options[0]", code: "TOO_LONG"}]` (a limit error in `extraChecks` stops the structure rules, so the adjacent texts at `segments[1]` are not reported yet), and the part has no item

#### Scenario: item-dropdown/R2-S12 Whitespace-only text between gaps is accepted
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[text "I ", gap ["go", "went"] -> 0, text " ", gap ["walk", "ran"] -> 1, text " to school."]` (the text " " separates two gaps) and `[gap ["go", "went"] -> 0, text "\n", gap ["walk", "ran"] -> 0, text "  \n\t"]` (a line break between gaps, a whitespace-only text last)
- **THEN** both responses are 201 with the segments exactly as sent (the whitespace-only texts kept), in the positions 1 and 2

#### Scenario: item-dropdown/R2-S13 A NUL in an option and a lone surrogate in a text are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[text "A", gap ["a\u0000b", "x"] -> 0]` and `[text "a\ud800b", gap ["go", "went"] -> 0]` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item

#### Scenario: item-dropdown/R2-S14 A null, typeless or unknown-type segment does not count as a gap, a gap with missing fields does
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `dropdown` with the segments `[text "Hi", {"type": "image"}]`, `[text "Hi", null]`, `[text "Hi", {"options": ["a", "b"], "correct": 0}]`, `[text "Hi", {"type": "gap"}]`, and with 25 gaps (each `["go", "went"] -> 0`) followed by one segment `{"type": "image"}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1].type", code: "INVALID_FORMAT"}]`, `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1]", code: "REQUIRED"}]`, `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1].type", code: "REQUIRED"}]`, `[{field: "payload.segments[1].correct", code: "REQUIRED"}, {field: "payload.segments[1].options", code: "REQUIRED"}]` (the gap with missing fields still counts as a gap, so there is no `payload.segments` error) and `[{field: "payload.segments[25].type", code: "INVALID_FORMAT"}]` (the unknown-type segment is not the 26th gap, so there is no `TOO_LONG`), and the part has no item
