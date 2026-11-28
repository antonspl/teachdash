# item-gap-fill-word-box Specification

## Purpose
A teacher adds and edits items of the type `gap-fill-word-box` ("fill in the blanks from a word box"): a text with gaps, one answer per gap, and optional extra words, stored as structured JSON. The student view carries the texts, the gap positions and the shuffled word box but never which word goes where.

## Requirements

### Requirement: R1 A teacher adds and edits a word-box item, and the student view hides which word goes where
The type `gap-fill-word-box` SHALL have the payload `{segments, extraWords}` of `{type: "text", text}` and `{type: "gap", answer}`, plus `extraWords`, added (items R1), edited (items R4), stored as sent. `studentView` SHALL return `{"segments": [...], "wordBox": [...]}`: gaps as `{type: "gap"}`, `wordBox` the shuffled answers and extras, never `answer` or `extraWords`; corrupt data throws `IllegalStateException`, no stored value in its message (R1-S11). Unit only (#125). `gap "go"`: answer "go".

#### Scenario: item-gap-fill-word-box/R1-S1 Add an English word-box item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `gap-fill-word-box` with the segments `[text "She ", gap "is", text " a teacher and they ", gap "are", text " doctors."]` and `extraWords` `["was", "be"]`, and then lists the items of the part
- **THEN** the add response is 201 with the type `gap-fill-word-box`, `position` 1, `version` 1 and exactly the payload that was sent (the segments and the extra words in the same order), no segment has a property it was not sent with (no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-gap-fill-word-box/R1-S2 Add a Russian item without extra words, Cyrillic is kept exactly, "ё" and "е" stay distinct
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `gap-fill-word-box` with the segments `[text "  Выберите слово: «Ёж» и ", gap "ёлка", text ".  "]` and no `extraWords`, then another with the same segments and `extraWords` `null`, and then another with `extraWords` `["елка", "Ёлка"]`
- **THEN** the three responses are 201 and each payload equals the sent one character by character (the leading and trailing spaces of the texts, the guillemets; "елка" and "Ёлка" accepted as extra words, since they differ from the answer "ёлка"), the first two payloads have no `extraWords` property at all (a `null` is dropped), and the stored payloads equal the responses

#### Scenario: item-gap-fill-word-box/R1-S3 Words are stored raw, and the same answer may fill two gaps
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `gap-fill-word-box` with the segments `[gap " go ", text " and ", gap " go ", text ", ", gap "GO"]` and `extraWords` `["go", "é", "é"]` (the last is "e" followed by U+0301, the decomposed letter)
- **THEN** the response is 201 and the answers are exactly `" go "`, `" go "` and `"GO"` (the spaces kept, the same answer in two gaps kept, "GO" and "go" different) and the extra words exactly `"go"`, the composed "é" and the decomposed "é" as three entries in this order, and the stored payload equals the response

#### Scenario: item-gap-fill-word-box/R1-S4 A gap may be first, last, next to another gap or the only segment, and a whitespace-only text may separate gaps
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[gap "a", text " b ", gap "c"]`, `[gap "a", gap "b", text "c"]` (two adjacent gaps), `[text "a", gap "b", gap "c"]`, `[gap "a"]`, `[text "I ", gap "go", text " ", gap "walk", text " to school."]` and `[gap "go", text "\n", gap "walk", text "  \n\t"]`, each without `extraWords`
- **THEN** all six responses are 201 with the segments exactly as sent (the whitespace-only texts kept), in the positions 1 to 6

#### Scenario: item-gap-fill-word-box/R1-S5 Edit a word-box item: the payload is replaced, the segments and the extra words together
- **GIVEN** a logged-in teacher A with an item of the type `gap-fill-word-box` with the segments `[text "She ", gap "is", text " a teacher."]`, `extraWords` `["are"]` and `version` 1
- **WHEN** teacher A edits it with `version` 1, the segments `[gap "She", text " ", gap "is", text " a teacher."]` and no `extraWords`
- **THEN** the response is 200 with the type `gap-fill-word-box`, the new payload exactly as sent (replaced, not merged, so there are no extra words any more), `version` 2 and the same `position`, and the stored payload has the new segments and no `extraWords`

#### Scenario: item-gap-fill-word-box/R1-S6 Editing a word-box item follows the word-box rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `gap-fill-word-box` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the segments `[text "No gap here."]`, and with the segments `[text "I ", gap "go"]` and `extraWords` `["go"]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "REQUIRED"}]` and `[{field: "payload.extraWords[0]", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-gap-fill-word-box/R1-S7 Another teacher cannot add or edit a word-box item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a word-box item in it
- **WHEN** teacher B adds a word-box item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-gap-fill-word-box/R1-S8 The student view keeps the text, shows gaps without an answer and lists answers and extra words as one box
- **GIVEN** the `gap-fill-word-box` type and two payloads with the same segments `[text "I ", gap "go", text " to school and ", gap "ran", text " home, ", gap "go", text " again."]`, the first with `extraWords` `["went", "run"]` and the second with `extraWords` `["swam", "jump"]` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with each payload
- **THEN** each result has exactly the properties `segments` and `wordBox`; `segments` has seven entries in the same order: `{type: "text", text: "I "}`, `{type: "gap"}` (no other property), `{type: "text", text: " to school and "}`, `{type: "gap"}`, `{type: "text", text: " home, "}`, `{type: "gap"}` and `{type: "text", text: " again."}`; the `segments` of the two views are equal; `wordBox` is a list of strings that holds exactly "go" twice, "ran" and the two extra words of that payload (five entries); the two `wordBox` lists have the same size, and no property of either view marks a word as extra

#### Scenario: item-gap-fill-word-box/R1-S9 The serialised student view has no `answer` and no `extraWords` key, in English and Russian
- **GIVEN** the `gap-fill-word-box` type and two payloads: an English one with the segments `[text "He ", gap "walks", text " and she ", gap "runs", text "."]` and `extraWords` `["swims"]`, and a Russian one with the segments `[text "Мой ", gap "Ёжик", text " любит ", gap "ежевику", text "."]` and `extraWords` `["ёжик", "ежик"]`, with no text containing the words "answer" or "extraWords"
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object whose keys are exactly `segments` and `wordBox`, in this order, each gap is exactly `{"type":"gap"}`, it contains no `answer` and no `extraWords` property name, every text segment appears exactly with `type` before `text`, and `wordBox` holds exactly the answers and extra words of the payload (three entries for the English one, four for the Russian one, "Ёжик" and "ёжик" as different entries)

#### Scenario: item-gap-fill-word-box/R1-S10 The box order is the seeded shuffle of the sorted words and does not depend on the gap order
- **GIVEN** the `gap-fill-word-box` type, the payload `[text "A ", gap "c", text " B ", gap "a", text " C ", gap "b"]` with `extraWords` `["d", "e"]`, and a second payload with the same texts but the gaps reordered to the answers "b", "c", "a" and `extraWords` `["e", "d"]`
- **WHEN** `studentView` is called twice with the first payload and once with the second
- **THEN** the `wordBox` of all three views is exactly `["c", "e", "b", "d", "a"]` (the words sorted by `String.compareTo` give `["a", "b", "c", "d", "e"]`; `Collections.shuffle(list, new Random(seed))` with the seed `String.join("\u0000", sortedWords).hashCode()` gives this order, which differs from the sorted one)

#### Scenario: item-gap-fill-word-box/R1-S11 The student view of corrupt data throws
- **GIVEN** the `gap-fill-word-box` type and payload records built directly (not validated): (a) a segment of an unknown type `image`; (b) a text segment with a `null` `text`; (c) a `null` segment; (d) a gap segment with a `null` `answer`; (e) a valid gap with `extraWords` `["x", null]`; (f) `segments` `null`; (g) `segments` an empty list
- **WHEN** `studentView` is called with each payload
- **THEN** each call throws `IllegalStateException` and returns no view, and the message of each is a fixed text that does not contain the stored value (for case (a), not the word `image`)

### Requirement: R2 A word-box payload follows the limits and structure rules
A `gap-fill-word-box` payload SHALL follow items R2. Limits (UTF-16): 51 segments, 25 gaps (`TOO_LONG` at `segments`), 500 per text, 100 per `answer` (`TOO_LONG`), 10 `extraWords` (`TOO_LONG`), 100 per extra word. A gap SHALL have a non-blank `answer`, extra words non-blank (`REQUIRED`). A repeated extra word, or one equal to an answer, is `INVALID_FORMAT` (exact match). Text is `REQUIRED` only if missing, `null` or "". Bean Validation runs first, then `extraChecks`.

#### Scenario: item-gap-fill-word-box/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with: (a) 26 text segments of 500 English letters ("a") alternating with 25 gaps (51 segments) with the answers "w1" to "w25" and `extraWords` "x1" to "x10" (a box of 35 words); (b) one gap with an answer of 100 Cyrillic letters ("Я") and `extraWords` with one word of 100 Cyrillic letters ("Ж"); (c) a text segment of 500 Cyrillic letters ("Я"), a gap with an answer of 50 emoji "😀" (100 UTF-16 code units) and `extraWords` with one word of 50 emoji "😁"; (d) a text segment of 250 emoji "😀" (500 UTF-16 code units), a gap "go" and `extraWords` of ten different words of 100 English letters each (99 times "b", then one distinguishing digit "0" to "9")
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-gap-fill-word-box/R2-S2 A text, an answer, an extra word, an extra-word list, a segment list or a gap count over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with: (a) a text segment of 501 Cyrillic letters ("Я") and the gap "go"; (b) a text segment of 251 emoji "😀" (502 UTF-16 code units) and the gap "go"; (c) the gap with an answer of 101 English letters ("a"); (d) the gap with an answer of 51 emoji "😀" (102 UTF-16 code units); (e) the gap "go" and 11 different `extraWords`; (f) the gap "go" and `extraWords` with one word of 101 English letters ("a"); (g) the gap "go" and `extraWords` with one word of 51 emoji "😀" (102 UTF-16 code units); (h) 52 segments (26 gaps alternating with 26 texts); (i) 26 adjacent gaps (26 segments, within the segment limit), each with a different answer
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]` for (a) and (b) (the text is the first segment), `[{field: "payload.segments[0].answer", code: "TOO_LONG"}]` for (c) and (d) (the gap is the first segment), `[{field: "payload.extraWords", code: "TOO_LONG"}]` for (e), `[{field: "payload.extraWords[0]", code: "TOO_LONG"}]` for (f) and (g) and `[{field: "payload.segments", code: "TOO_LONG"}]` for (h) and (i) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S3 A missing or empty text, and a payload without a gap, are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments: (a) `[gap "go", {"type": "text"}]`; (b) `[gap "go", {"type": "text", "text": null}]`; (c) `[gap "go", text ""]`; (d) only `[text "No gap here."]`; and with the payloads (e) `{"segments": []}`, (f) `{"segments": null}` and (g) `{}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "REQUIRED"}]` for (a), (b) and (c), and `[{field: "payload.segments", code: "REQUIRED"}]` for (d), (e), (f) and (g), and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S4 A gap needs an answer that is not blank
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the gap `{"type": "gap"}` (no `answer`), `{"type": "gap", "answer": null}`, `{"type": "gap", "answer": ""}` and `{"type": "gap", "answer": "  \n"}`, each as the second segment after a text segment "Hi"
- **THEN** all four responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].answer", code: "REQUIRED"}]` (the gap still counts as a gap, so there is no `payload.segments` error), and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S5 Every extra word must be non-blank, and a missing or empty list is accepted
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[text "Hi ", gap "go"]` and `extraWords` `["x", ""]`, `["  ", "x"]`, `["x", null]` and `[null]`, and then with the same segments and `extraWords` `[]` and `null`
- **THEN** the first four responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.extraWords[1]", code: "REQUIRED"}]`, `[{field: "payload.extraWords[0]", code: "REQUIRED"}]`, `[{field: "payload.extraWords[1]", code: "REQUIRED"}]` and `[{field: "payload.extraWords[0]", code: "REQUIRED"}]`, the last two are 201 (an empty list is stored as `[]`, a `null` is dropped), and the part has exactly two items

#### Scenario: item-gap-fill-word-box/R2-S6 A repeated extra word, or an extra word equal to an answer, is rejected only on an exact match
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[text "I ", gap "go"]` and `extraWords` `["walk", "run", "walk"]`, then `[text "I ", gap "go"]` and `["go"]`, then `[text "I ", gap "go", text " and ", gap "ran"]` and `["x", "ran"]`, then `[text "Мой ", gap "Ёж"]` and `["ёж", "еж", "Ёж"]`, and then `[text "I ", gap "go", text " and ", gap "go", text " ", gap "Go"]` and `["go ", " go", "GO", "Ёж", "ёж", "еж"]`
- **THEN** the first response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.extraWords[2]", code: "INVALID_FORMAT"}]` (the later word), the second `[{field: "payload.extraWords[0]", code: "INVALID_FORMAT"}]`, the third `[{field: "payload.extraWords[1]", code: "INVALID_FORMAT"}]` (an answer of any gap counts), the fourth `[{field: "payload.extraWords[2]", code: "INVALID_FORMAT"}]` (the Russian "Ёж" equals the answer, "ёж" and "еж" do not), the fifth is 201 with all words exactly as sent (a different case, a leading or trailing space and "ё" against "е" make different words; the same answer "go" in two gaps is allowed), and the part has exactly one item

#### Scenario: item-gap-fill-word-box/R2-S7 Two adjacent text segments are rejected on the second
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[text "A", text "B", gap "go"]` and `[gap "go", text "A", text "B", text "C"]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}]` and `[{field: "payload.segments[2]", code: "INVALID_FORMAT"}, {field: "payload.segments[3]", code: "INVALID_FORMAT"}]`, and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S8 A field of the other segment type, or an unknown property, is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the payloads: (a) `[text "Hi", {"type": "gap", "text": "go", "answer": "go"}]`; (b) `[{"type": "text", "text": "Hi", "answer": "go"}, gap "go"]`; (c) `[{"type": "text", "text": "Hi"}, {"type": "gap", "answers": ["go"]}]`; (d) `{"segments": [text "Hi", gap "go"], "options": ["a"]}`; (e) `[text "Hi", {"type": "gap", "id": "seg-7731", "answer": "go"}]`; (f) `[text "Hi", {"type": "gap", "text": <501 English letters "a">, "answer": "go"}]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].text", code: "UNKNOWN_FIELD"}]` for (a), `[{field: "payload.segments[0].answer", code: "UNKNOWN_FIELD"}]` for (b) (a known property in the wrong segment type, reported at its path), `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (c), (d) and (e) (a truly unknown property; the name is not echoed) and `[{field: "payload.segments[1].text", code: "TOO_LONG"}]` for (f) (Bean Validation runs first, so not `UNKNOWN_FIELD`), and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S9 A missing, blank or unknown segment type and a null segment are rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments (each after a valid gap "go"): `[{"text": "Hi"}]`, `[{"type": null, "text": "Hi"}]`, `[{"type": " ", "text": "Hi"}]`, `[{"type": "image", "text": "Hi"}]`, `[{"type": "Gap", "answer": "go"}]` and `[null]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments[1].type", code: "REQUIRED"}]` for the first three, `[{field: "payload.segments[1].type", code: "INVALID_FORMAT"}]` for the next two, and `[{field: "payload.segments[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S10 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the payloads: `{"segments": "text"}`, `{"segments": {}}`, `{"segments": ["text"]}`, `{"segments": [{"type": 5, "text": "Hi"}]}`, `{"segments": [{"type": "text", "text": ["Привет"]}]}`, `{"segments": [{"type": "gap", "answer": 5}]}`, `{"segments": [{"type": "gap", "answer": ["go"]}]}`, `{"segments": [gap "go"], "extraWords": "went"}`, `{"segments": [gap "go"], "extraWords": {}}` and `{"segments": [gap "go"], "extraWords": ["went", 5]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.segments[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].type", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].text", code: "INVALID_TYPE"}]`, `[{field: "payload.segments[0].answer", code: "INVALID_TYPE"}]` for the next two, `[{field: "payload.extraWords", code: "INVALID_TYPE"}]` for the next two and `[{field: "payload.extraWords[1]", code: "INVALID_TYPE"}]` for the last, no body contains "Привет", and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S11 Bean Validation limits are checked first, the structure rules after, and the structure errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with: (1) the segments `[text <501 English letters "a">, gap "go"]` and `extraWords` `["x", "x"]`; (2) the segments `[text "A", text "B", gap "go", gap "", gap "go"]` and `extraWords` `["go", "w", "w", ""]`; (3) the segments `[text "A", gap "go"]` and `extraWords` `["go"]`; (4) the segments `[text "A", {"type": "gap", "text": <501 English letters "a">, "answer": "go"}]`; (5) the segments `[text "A", gap <101 English letters "a">]` and 11 different `extraWords`, one of 101 English letters; (6) the segments `[text "A", text "B", gap <101 English letters "a">]`
- **THEN** the first response is 400 with `errors` exactly `[{field: "payload.segments[0].text", code: "TOO_LONG"}]` (the duplicate extra word is not reported yet), the second is 400 with `errors` exactly `[{field: "payload.extraWords[0]", code: "INVALID_FORMAT"}, {field: "payload.extraWords[2]", code: "INVALID_FORMAT"}, {field: "payload.extraWords[3]", code: "REQUIRED"}, {field: "payload.segments[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[3].answer", code: "REQUIRED"}]` (sorted by field), the third is 400 with `errors` exactly `[{field: "payload.extraWords[0]", code: "INVALID_FORMAT"}]`, the fourth is 400 with `errors` exactly `[{field: "payload.segments[1].text", code: "TOO_LONG"}]` (Bean Validation runs first, so not `UNKNOWN_FIELD`), and the fifth is 400 with `errors` exactly `[{field: "payload.extraWords", code: "TOO_LONG"}]` (the over-long answer and extra word are not reported yet), and the sixth is 400 with `errors` exactly `[{field: "payload.segments[1]", code: "INVALID_FORMAT"}, {field: "payload.segments[2].answer", code: "TOO_LONG"}]` (only the Bean Validation limits stop the later checks; the checks of `extraChecks` report together, design D3), and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S12 A NUL in a word and a lone surrogate in a text are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[text "A", gap "a\u0000b"]`, then `[text "A", gap "go"]` with `extraWords` `["x\u0000y"]`, and then `[text "a\ud800b", gap "go"]` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** all three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item

#### Scenario: item-gap-fill-word-box/R2-S13 A null, typeless or unknown-type segment does not count as a gap, a gap with a missing answer does
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `gap-fill-word-box` with the segments `[text "Hi", {"type": "image"}]`, `[text "Hi", null]`, `[text "Hi", {"answer": "go"}]`, `[text "Hi", {"type": "gap"}]`, and with 25 gaps (the answers "w1" to "w25") followed by one segment `{"type": "image"}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1].type", code: "INVALID_FORMAT"}]`, `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1]", code: "REQUIRED"}]`, `[{field: "payload.segments", code: "REQUIRED"}, {field: "payload.segments[1].type", code: "REQUIRED"}]`, `[{field: "payload.segments[1].answer", code: "REQUIRED"}]` (the gap with a missing answer still counts as a gap, so there is no `payload.segments` error) and `[{field: "payload.segments[25].type", code: "INVALID_FORMAT"}]` (the unknown-type segment is not the 26th gap, so there is no `TOO_LONG`), and the part has no item
