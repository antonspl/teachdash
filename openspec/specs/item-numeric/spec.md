# item-numeric Specification

## Purpose
A teacher adds and edits items of the type `numeric`: questions with a numeric answer, a tolerance and an optional unit, stored as structured JSON. The student view of the payload carries the texts and units but never the answer or the tolerance.

## Requirements

### Requirement: R1 A teacher adds and edits a numeric item, and the student view hides the number and the tolerance
The type `numeric` SHALL have the payload `{questions: [{text, answer, tolerance, unit?}]}`, added with items R1 and edited with items R4 (rules in R2). Stored as sent, numbers with "." in every course language. Another teacher's add and edit give 404. `studentView` SHALL return `{"questions": [{"text", "unit"?}]}` in stored order, no `unit` key when absent, no `answer` or `tolerance`; corrupt data throws `IllegalStateException`. Unit scenarios only (#125).

#### Scenario: item-numeric/R1-S1 Add an English numeric item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "Physics B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `numeric` with the payload `{"questions": [{"text": "A train covers 120 km in 2 hours. How fast is it?", "answer": "60", "tolerance": "0.5", "unit": "km/h"}, {"text": "What is 1/4 as a decimal?", "answer": "0.25", "tolerance": "0"}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `numeric`, `position` 1, `version` 1 and exactly the payload that was sent (the questions in the same order, the numbers as strings, the second question with no `unit` property and no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-numeric/R1-S2 Add a Russian numeric item, Cyrillic, spaces and number text are kept exactly
- **GIVEN** a logged-in teacher A with the course "Физика 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `numeric` with the payload `{"questions": [{"text": "  Масса тела?  ", "answer": "-0", "tolerance": "0.10", "unit": "кг"}, {"text": "Скорость автобуса?", "answer": "0012.50", "tolerance": "007", "unit": "км/ч"}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the spaces around the first text, "-0", "0.10", "0012.50" and "007" not normalised, the decimal point "." kept in a `ru` course), and the stored payload has the same content

#### Scenario: item-numeric/R1-S3 Edit a numeric item: the payload is replaced
- **GIVEN** a logged-in teacher A with an item of the type `numeric` with the payload `{"questions": [{"text": "2 + 2?", "answer": "4", "tolerance": "0"}]}` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"questions": [{"text": "Pi to two places?", "answer": "3.14", "tolerance": "0.01"}, {"text": "Вес яблока?", "answer": "150", "tolerance": "10", "unit": "г"}]}`
- **THEN** the response is 200 with the type `numeric`, the new payload exactly as sent (replaced, not merged, two questions), `version` 2 and the same `position`, and the stored payload has the two new questions

#### Scenario: item-numeric/R1-S4 Editing a numeric item follows the numeric rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `numeric` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"questions": []}` and `{"questions": [{"text": "Q", "answer": "1", "tolerance": "-1"}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "REQUIRED"}]` and `[{field: "payload.questions[0].tolerance", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-numeric/R1-S5 Another teacher cannot add or edit a numeric item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a numeric item in it
- **WHEN** teacher B adds a numeric item to teacher A's part and edits teacher A's item, each with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-numeric/R1-S6 The student view has the text and the unit, never the number or the tolerance
- **GIVEN** the `numeric` type and a payload with three questions (unit level: there is no student endpoint yet): "Speed?" with the answer "60", the tolerance "0.5" and the unit "km/h"; "Масса?" with "75.5", "0" and the unit "кг"; "Count the sides." with "4", "0" and no unit
- **WHEN** `studentView` is called with the payload
- **THEN** the result is `{"questions": [...]}` with exactly the property `questions` and three entries in the same order, the first two with exactly the properties `text` and `unit` (in this order) holding the stored values, the third with exactly the property `text` (no `unit` key, not `null`), and no entry has `answer` or `tolerance`

#### Scenario: item-numeric/R1-S7 The questions keep their stored order
- **GIVEN** the `numeric` type and a payload with the questions "Q3", "Q1", "Q2" (not in alphabetical order) with any valid numbers
- **WHEN** `studentView` is called twice with the payload
- **THEN** both views are equal and their question texts are exactly `["Q3", "Q1", "Q2"]` (not sorted, not shuffled)

#### Scenario: item-numeric/R1-S8 The serialised student view contains no number or tolerance, in English and Russian
- **GIVEN** the `numeric` type and two payloads: an English one with the questions "Boiling point of water?" (answer "100", tolerance "0.125", unit "°C") and "Speed of sound?" (answer "343.5", tolerance "1.25"), and a Russian one with "Температура кипения воды?" (answer "100", tolerance "0.125", unit "°C") and "Сколько ног у паука?" (answer "8", tolerance "0"), with no question text or unit containing the digits of an answer or a tolerance
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with the property `questions`, each question has the key `text` and, only when it had a unit, the key `unit` after it, the JSON contains no `answer` or `tolerance` property name and none of the strings "100", "0.125", "343.5", "1.25" and "8" as values, and every text and unit appears exactly

#### Scenario: item-numeric/R1-S9 The student view of corrupt data throws
- **GIVEN** the `numeric` type and payload records built directly (not validated): (a) a `null` `questions` list; (b) a `null` question; (c) a question with a `null` `text`
- **WHEN** `studentView` is called with each payload
- **THEN** each call throws `IllegalStateException` and returns no view, and no message contains a stored text, number or unit

### Requirement: R2 A numeric payload follows the limits and format rules
A `numeric` payload SHALL follow items R2. Questions: 1 to 25; `text` non-blank, at most 500 (UTF-16); `unit` optional, when present non-blank (checked first), at most 20. `answer` and `tolerance`: digits with an optional "." part, 1-15 and 1-10 digits; `answer` may have a minus, `tolerance` not ("-0" is rejected). Missing, `null` or blank values, a `null` question, a blank unit: `REQUIRED`; over a maximum `TOO_LONG`; bad number `INVALID_FORMAT`; wrong JSON type `INVALID_TYPE`.

#### Scenario: item-numeric/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with: (a) 25 questions, each `{"text": "Q", "answer": "1", "tolerance": "0"}`; (b) one question with a text of 500 Cyrillic letters ("Я"), the answer "-999999999999999", the tolerance "999999999999999.9999999999" and a unit of 20 Cyrillic letters ("к"); (c) one question with a text of 250 emoji "😀" (500 UTF-16 code units), the answer "0.0000000001", the tolerance "0" and a unit of 10 emoji "😀" (20 UTF-16 code units); (d) the questions `{"text": "Скорость?", "answer": "-0.5", "tolerance": "0.01", "unit": "км/ч"}` and `{"text": "Speed?", "answer": "12", "tolerance": "1"}`
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-numeric/R2-S2 A text, a unit or the question list over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with a valid question `{"text": "Q", "answer": "1", "tolerance": "0"}` changed so that: (a) the text is 501 Cyrillic letters ("Я"); (b) the text is 251 emoji "😀" (502 UTF-16 code units); (c) it has the unit of 21 English letters ("a"); (d) it has the unit of 11 emoji "😀" (22 UTF-16 code units); and (e) 26 such valid questions
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].text", code: "TOO_LONG"}]` for (a) and (b), `[{field: "payload.questions[0].unit", code: "TOO_LONG"}]` for (c) and (d) and `[{field: "payload.questions", code: "TOO_LONG"}]` for (e) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-numeric/R2-S3 Missing, null or empty questions and a null question are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with the payloads `{}`, `{"questions": null}`, `{"questions": []}` and `{"questions": [{"text": "Q", "answer": "1", "tolerance": "0"}, null]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "REQUIRED"}]` for the first three and `[{field: "payload.questions[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-numeric/R2-S4 A missing, null, empty or blank question text is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with one question with `"answer": "1"`, `"tolerance": "0"` and the `text` missing, `null`, `""`, `"   "` and `"\n\t"`
- **THEN** all five responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].text", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-numeric/R2-S5 A unit may be absent or null, but a present unit must not be blank
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with one question `{"text": "Q", "answer": "1", "tolerance": "0"}` that has no `unit`, `"unit": null`, `"unit": ""`, `"unit": "   "`, `"unit": "\t"` and a `unit` of 21 spaces
- **THEN** the first two responses are 201 and the stored payload has no `unit` property (the `null` is dropped), the last four are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].unit", code: "REQUIRED"}]`, and the part has exactly two items (21 spaces are blank, not too long)

#### Scenario: item-numeric/R2-S6 A missing, null, empty or blank answer or tolerance is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with one question `{"text": "Q", "answer": "1", "tolerance": "0"}` in which the `answer` is missing, `null`, `""`, `"   "`, and then the `tolerance` is missing, `null`, `""`, `"   "`
- **THEN** all eight responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].answer", code: "REQUIRED"}]` for the first four and `[{field: "payload.questions[0].tolerance", code: "REQUIRED"}]` for the last four, and the part has no item

#### Scenario: item-numeric/R2-S7 Numbers in the allowed format are accepted and stored as written
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds one item of the type `numeric` with questions "Q1" to "Q8", with `answer` and `tolerance`: "0" and "0"; "-0" and "0.00"; "007" and "007"; "1.50" and "1.50"; "-12.5" and "3"; "999999999999999" and "999999999999999"; "0.0000000001" and "0.0000000001"; "-0.1" and "0.1"
- **THEN** the response is 201 and the payload equals the sent one character by character (no number is changed to another form, for example "1.50" is not "1.5" and "007" is not "7"), and the stored payload has the same content

#### Scenario: item-numeric/R2-S8 An answer off the number format is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with one question `{"text": "Q", "tolerance": "0", "answer": ...}` in which the `answer` is each of: "1,5", "1e3", "1E-3", "+1", " 1", "1 ", "1 000", ".5", "5.", "1/2", "0x10", "NaN", "Infinity", "--1", "1.5.2", "-", "١٢" (Arabic-Indic digits), "1234567890123456" (16 integer digits), "1.12345678901" (11 fraction digits), "1\n" and "abc"
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].answer", code: "INVALID_FORMAT"}]` (no body contains the sent value), and the part has no item

#### Scenario: item-numeric/R2-S9 A tolerance that is negative or off the number format is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with one question `{"text": "Q", "answer": "1", "tolerance": ...}` in which the `tolerance` is each of: "-1", "-0", "-0.5", "-0.0000000001", "0,5", "1e-2", "+1", " 1", ".5", "1/2", "1234567890123456" (16 integer digits) and "0.12345678901" (11 fraction digits)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions[0].tolerance", code: "INVALID_FORMAT"}]`, and the part has no item

#### Scenario: item-numeric/R2-S10 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with the payloads: `{"questions": "Q"}`, `{"questions": {}}`, `{"questions": ["Q"]}`, and a question otherwise valid (`{"text": "Q", "answer": "1", "tolerance": "0"}`) with `"text": 5`, `"text": ["Привет"]`, `"answer": 1.5`, `"answer": 1`, `"answer": true`, `"answer": ["1"]`, `"tolerance": 0.5`, `"tolerance": 0`, `"tolerance": {}`, `"unit": 5` and `"unit": ["км"]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.questions", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.questions[0]", code: "INVALID_TYPE"}]` for the third, and `INVALID_TYPE` at `payload.questions[0].text`, `.answer`, `.tolerance` and `.unit` for the question cases of that field (a JSON number is not accepted for a number field), no body contains "Привет", and the part has no item

#### Scenario: item-numeric/R2-S11 An unknown property is rejected without echoing its name
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with a valid payload that also has: (a) the payload property `"precision": 2`; (b) a question with `"points": 2`; (c) a question with `"relative": true`
- **THEN** all three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (the name is not echoed), and the part has no item

#### Scenario: item-numeric/R2-S12 A NUL in a text or unit and a lone surrogate are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with a question `{"text": "a\u0000b", "answer": "1", "tolerance": "0"}`, with a question whose unit is `"k\u0000m"`, and with a question text `"a\ud800b"` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** all three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item

#### Scenario: item-numeric/R2-S13 Bean Validation errors come together, a null question and a bad unit come after
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `numeric` with: (a) one question with a text of 501 English letters ("a"), the answer "abc" and the tolerance "-1"; (b) the questions `[{"text": "Q1", "answer": "1", "tolerance": "0", "unit": "  "}, null]`; (c) the questions `[{"text": "Q1", "answer": "1", "tolerance": "0", "unit": "  "}, null, {"text": "Q3", "answer": "x", "tolerance": "0"}]`; (d) 26 questions, the first 25 valid `{"text": "Q", "answer": "1", "tolerance": "0"}` and the 26th `null`
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.questions[0].answer", code: "INVALID_FORMAT"}, {field: "payload.questions[0].text", code: "TOO_LONG"}, {field: "payload.questions[0].tolerance", code: "INVALID_FORMAT"}]` (sorted by field), (b) has `errors` exactly `[{field: "payload.questions[0].unit", code: "REQUIRED"}, {field: "payload.questions[1]", code: "REQUIRED"}]`, (c) has `errors` exactly `[{field: "payload.questions[2].answer", code: "INVALID_FORMAT"}]` (the blank unit and the null question are not reported yet), (d) has `errors` exactly `[{field: "payload.questions", code: "TOO_LONG"}]` (the null question is not reported yet), and the part has no item
