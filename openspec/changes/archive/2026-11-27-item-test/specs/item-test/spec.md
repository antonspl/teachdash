## ADDED Requirements

### Requirement: R1 A teacher adds and edits a test item
The type `test` SHALL have the payload `{questions: [{text, options: [{text, correct}]}]}`, added with items R1 and edited with items R4. Texts are plain text and `correct` is a boolean; everything is stored as sent (no trim, case change or normalisation; `false` is kept). Another teacher's add and edit give 404. The payload rules are in R2, the student view in R3.

#### Scenario: item-test/R1-S1 Add an English test item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `test` with the payload `{"questions": [{"text": "Which word is a verb?", "options": [{"text": "run", "correct": true}, {"text": "table", "correct": false}, {"text": "green", "correct": false}]}, {"text": "Pick the past forms.", "options": [{"text": "went", "correct": true}, {"text": "go", "correct": false}, {"text": "saw", "correct": true}]}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `test`, `position` 1, `version` 1 and exactly the payload that was sent (the questions and options in the same order, `"correct": false` kept), no object has a property it was not sent with (no `multiple`, no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-test/R1-S2 Add a Russian test item, Cyrillic and spaces are kept exactly
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `test` with the payload `{"questions": [{"text": "  Кто написал «Ёж»?  ", "options": [{"text": " Ёлка ", "correct": true}, {"text": "елка", "correct": false}, {"text": "ЁЛКА", "correct": false}, {"text": "<b>ёлка</b>", "correct": false}]}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the leading and trailing spaces of the question and of the first option, the guillemets, "Ёлка", "елка", "ЁЛКА" and "ёлка" in markup as four different options in this order, the markup not interpreted or removed), and the stored payload has the same content

#### Scenario: item-test/R1-S3 Edit a test item: the payload is replaced
- **GIVEN** a logged-in teacher A with an item of the type `test` with the payload `{"questions": [{"text": "2 + 2?", "options": [{"text": "3", "correct": false}, {"text": "4", "correct": true}]}]}` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"questions": [{"text": "2 + 3?", "options": [{"text": "5", "correct": true}, {"text": "6", "correct": false}]}, {"text": "3 + 3?", "options": [{"text": "6", "correct": true}, {"text": "7", "correct": false}]}]}`
- **THEN** the response is 200 with the type `test`, the new payload exactly as sent (replaced, not merged, two questions), `version` 2 and the same `position`, and the stored payload has the two new questions

#### Scenario: item-test/R1-S4 Editing a test item follows the test rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `test` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"questions": []}` and `{"questions": [{"text": "Q", "options": [{"text": "A", "correct": true}]}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "REQUIRED"}]` and `[{field: "payload.questions[0].options", code: "TOO_SHORT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-test/R1-S5 Another teacher cannot add or edit a test item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a test item in it
- **WHEN** teacher B adds a test item to teacher A's part and edits teacher A's item, each with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

### Requirement: R2 A test payload follows the limits and structure rules
A `test` payload SHALL follow items R2. Questions: 1 to 25, text non-blank, at most 500 (UTF-16). Options: 2 to 10, text non-blank, at most 100, `correct` a JSON `true` or `false`. Missing, `null` or empty lists and blank texts: `REQUIRED`, as a `null` question or option or `correct`; 1 option `TOO_SHORT`; over a maximum `TOO_LONG`; wrong JSON type `INVALID_TYPE`; an exact repeat of an option text `INVALID_FORMAT` at the later `.text`.

#### Scenario: item-test/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with: (a) 25 questions, each with the text "Q" and two options "A" (correct) and "B"; (b) one question with a text of 500 Cyrillic letters ("Я") and 10 options "o1" to "o10", the last one correct; (c) one question with a text of 250 emoji "😀" (500 UTF-16 code units) and two options, "A" (correct) and an option of 50 emoji "😀" (100 UTF-16 code units); (d) one question "Q" with two options of 100 English letters ("a" and "b" repeated, so they differ), both correct, and a second question "Вопрос" with the options "да" (correct) and "нет"
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-test/R2-S2 A question, an option, a question list or an option list over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with: (a) one question with a text of 501 Cyrillic letters ("Я") and two valid options; (b) a question text of 251 emoji "😀" (502 UTF-16 code units); (c) a valid question whose second option text is 101 English letters ("a"); (d) a second option text of 51 emoji "😀" (102 UTF-16 code units); (e) a valid question with 11 different options "o1" to "o11"; (f) 26 valid questions "Q" with the options "A" and "B"
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].text", code: "TOO_LONG"}]` for (a) and (b), `[{field: "payload.questions[0].options[1].text", code: "TOO_LONG"}]` for (c) and (d), `[{field: "payload.questions[0].options", code: "TOO_LONG"}]` for (e) and `[{field: "payload.questions", code: "TOO_LONG"}]` for (f) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-test/R2-S3 Missing, null or empty questions and a null question are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with the payloads `{}`, `{"questions": null}`, `{"questions": []}` and `{"questions": [{"text": "Q", "options": [{"text": "A", "correct": true}, {"text": "B", "correct": false}]}, null]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "REQUIRED"}]` for the first three and `[{field: "payload.questions[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-test/R2-S4 A missing, null, empty or blank question text is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with one question that has the two valid options "A" (correct) and "B" and the question `text` missing, `null`, `""`, `"   "` and `"\n\t"`
- **THEN** all five responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].text", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-test/R2-S5 Options need at least two entries, and a null option is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with one question "Q" whose `options` are missing, `null`, `[]`, one option `[{"text": "A", "correct": true}]`, and `[{"text": "A", "correct": true}, null, {"text": "B", "correct": false}]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].options", code: "REQUIRED"}]` for the first three, `[{field: "payload.questions[0].options", code: "TOO_SHORT"}]` for the one option and `[{field: "payload.questions[0].options[1]", code: "REQUIRED"}]` for the null option, and the part has no item

#### Scenario: item-test/R2-S6 An option needs a non-blank text and a `correct` value
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with one question "Q" whose second option is `{"correct": false}`, `{"text": null, "correct": false}`, `{"text": "", "correct": false}`, `{"text": "  ", "correct": false}`, `{"text": "B"}` and `{"text": "B", "correct": null}` (the first option is `{"text": "A", "correct": true}`)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].options[1].text", code: "REQUIRED"}]` for the first four and `[{field: "payload.questions[0].options[1].correct", code: "REQUIRED"}]` for the last two, and the part has no item

#### Scenario: item-test/R2-S7 A repeated option text in a question is rejected only on an exact match
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with the options `["go", "walk", "go"]` of one question, then `["Ёлка", "ёлка", "елка", "Ёлка", "Ёлка"]`, and then the two questions "Q1" with the options `["go", "Go", " go", "go "]` and "Q2" with the options `["go", "walk"]` (each option has `"correct": false` except the first one of the question)
- **THEN** the first response is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].options[2].text", code: "INVALID_FORMAT"}]` (the later option), the second is 400 with `errors` exactly `[{field: "payload.questions[0].options[3].text", code: "INVALID_FORMAT"}, {field: "payload.questions[0].options[4].text", code: "INVALID_FORMAT"}]`, the third is 201 with all options exactly as sent (different case, a leading and a trailing space make different options; the same text in two questions is allowed), and the part has exactly one item

#### Scenario: item-test/R2-S8 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with the payloads: `{"questions": "Q"}`, `{"questions": {}}`, `{"questions": ["Q"]}`, a question with `"text": 5`, a question with `"text": ["Привет"]`, a question with `"options": "A"`, a question with `"options": ["A", "B"]`, and two valid options where the second has `"correct": "yes"` and `"correct": []`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.questions[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.questions[0].text", code: "INVALID_TYPE"}]` for both text cases, `[{field: "payload.questions[0].options", code: "INVALID_TYPE"}]`, `[{field: "payload.questions[0].options[0]", code: "INVALID_TYPE"}]` and `[{field: "payload.questions[0].options[1].correct", code: "INVALID_TYPE"}]` for both `correct` cases, no body contains "Привет", and the part has no item

#### Scenario: item-test/R2-S9 An unknown property is rejected without echoing its name
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with a valid payload that also has: (a) the payload property `"timer": 60`; (b) a question with `"multiple": true`; (c) an option with `"id": "opt-7731"`; (d) a question with `"points": 2`
- **THEN** all four responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (the name is not echoed), and the part has no item

#### Scenario: item-test/R2-S10 A NUL in an option and a lone surrogate in a question are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with a question "Q" whose options are `"a\u0000b"` (correct) and "B", and with a question text `"a\ud800b"` and the options "A" (correct) and "B" (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item

#### Scenario: item-test/R2-S11 Limits are checked first, the structure rules after, and all structure errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with: (a) a question with a text of 501 English letters ("a") and the options "A", "A"; (b) the questions `[{"text": "Q1", "options": [A, A, null]}, null]` (A is `{"text": "A", "correct": true}`); (c) a question "Q" with the options "A" and "A" (both correct); (d) a question "Q" with the options "A" (correct) and an option with a text of 101 English letters ("a") and `"correct": null`
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.questions[0].text", code: "TOO_LONG"}]` (the duplicate is not reported yet), (b) has `errors` exactly `[{field: "payload.questions[0].options[1].text", code: "INVALID_FORMAT"}, {field: "payload.questions[0].options[2]", code: "REQUIRED"}, {field: "payload.questions[1]", code: "REQUIRED"}]` (sorted by field), (c) has `errors` exactly `[{field: "payload.questions[0].options[1].text", code: "INVALID_FORMAT"}]`, (d) has `errors` exactly `[{field: "payload.questions[0].options[1].correct", code: "REQUIRED"}, {field: "payload.questions[0].options[1].text", code: "TOO_LONG"}]` (both are Bean Validation errors, sorted by field), and the part has no item

#### Scenario: item-test/R2-S12 `correct` accepts only a JSON boolean
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `test` with one question "Q" whose options are "A" with `"correct": true` and "B" with `"correct"` set to `"true"`, `"TRUE"`, `"false"`, `1`, `0`, `2`, `1.0` and `""`, and one more with `"correct": null`
- **THEN** the responses for `"true"`, `"TRUE"`, `"false"`, `1`, `0`, `2`, `1.0` and `""` are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].options[1].correct", code: "INVALID_TYPE"}]`, the response for `null` is 400 with `errors` exactly `[{field: "payload.questions[0].options[1].correct", code: "REQUIRED"}]`, and the part has no item

### Requirement: R3 The student view hides the correct options, and a question may have none
`studentView` SHALL return `{"questions": [{"text", "multiple", "options": [{"text"}]}]}` as an allowlist in this key order, with options in stored order (not shuffled) and no `correct` key or index; `multiple` is true when more than one option is correct, else false. A question with no correct option, or with all options correct, SHALL be accepted; none is auto-checked (#126). No student endpoint yet (#125): unit scenarios only.

#### Scenario: item-test/R3-S1 A question with no correct option and one with all options correct are accepted, with no stored `multiple`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `test` with the payload `{"questions": [{"text": "What do you think?", "options": [{"text": "Yes", "correct": false}, {"text": "No", "correct": false}]}, {"text": "Pick all.", "options": [{"text": "A", "correct": true}, {"text": "B", "correct": true}, {"text": "C", "correct": true}]}, {"text": "Pick one.", "options": [{"text": "A", "correct": true}, {"text": "B", "correct": false}]}]}` and then edits it with `version` 1 so that the first question's option "Yes" is correct
- **THEN** the add response is 201 with the payload exactly as sent and the edit response is 200 with `version` 2 and the edited payload exactly as sent, and neither payload, stored or listed, has a `multiple` property in any question

#### Scenario: item-test/R3-S2 The student view marks a question with more than one correct option as multiple
- **GIVEN** the `test` type and a payload with four questions (unit level: there is no student endpoint yet): "Q1" with the options "A" (correct), "B", "C"; "Q2" with "A" (correct), "B" (correct), "C"; "Q3" with "A", "B" (both not correct); "Q4" with "A" (correct), "B" (correct)
- **WHEN** `studentView` is called with the payload
- **THEN** the result is `{"questions": [...]}` with exactly the property `questions` and four entries in the same order, each with exactly the properties `text`, `multiple` and `options`, `multiple` is `false`, `true`, `false` and `true` (one correct, two of three, none, all two), each option has exactly the property `text`, and the options of each question have the stored texts in the stored order

#### Scenario: item-test/R3-S3 The options keep their stored order, "all of the above" stays last
- **GIVEN** the `test` type and the payload with one question "Which are fruits?" and the options "apple" (correct), "pear" (correct), "bread", "all of the above" (in this order) and a second question with the options "c", "a", "b" (all different, "a" correct)
- **WHEN** `studentView` is called twice with the payload
- **THEN** both views are equal, the first question's option texts are exactly `["apple", "pear", "bread", "all of the above"]` and the second's are exactly `["c", "a", "b"]` (not sorted, not shuffled)

#### Scenario: item-test/R3-S4 The serialised student view contains no correct mark, in English and Russian
- **GIVEN** the `test` type and two payloads: an English one with the questions "Past of go?" (options "went" correct, "goed") and "Pick verbs." (options "run" correct, "walk" correct, "table"), and a Russian one with "Столица России?" (options "Москва" correct, "Ёж") and "Выберите глаголы." (options "бежать" correct, "идти" correct, "стол"), with no text containing the word "correct" or "multiple"
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with the property `questions`, each question has the keys `text`, `multiple`, `options` in this order, each option is exactly `{"text": ...}`, the JSON contains no `correct` property name, no `true` or `false` other than the values of `multiple`, and every text appears exactly

#### Scenario: item-test/R3-S5 The student view of corrupt data throws
- **GIVEN** the `test` type and payload records built directly (not validated): (a) a `null` question; (b) a question with a `null` `text`; (c) a question with `null` `options`; (d) a `null` option; (e) an option with a `null` `text`; (f) an option with a `null` `correct`
- **WHEN** `studentView` is called with each payload
- **THEN** each call throws `IllegalStateException` and returns no view, and no message contains a stored text
