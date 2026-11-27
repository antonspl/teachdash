## ADDED Requirements

### Requirement: R1 A teacher adds and edits a true / false item, and the student view hides the answers
The type `true-false` SHALL have the payload `{allowNotStated?, statements}` of `{text, answer}` with `answer` `true`, `false` or `not-stated`, added with items R1 and edited with items R4, and stored as sent (a null `allowNotStated` is dropped). `studentView` SHALL return `{"allowNotStated": boolean, "statements": [{"text"}]}` as an allowlist, in stored order, without `answer`; `allowNotStated` is false when missing. No student endpoint exists yet (#125): only unit scenarios cover it.

#### Scenario: item-true-false/R1-S1 Add an English true / false item with "not stated", it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `true-false` with the payload `{"allowNotStated": true, "statements": [{"text": "London is the capital of France.", "answer": "false"}, {"text": "Water boils at 100 degrees Celsius at sea level.", "answer": "true"}, {"text": "The teacher has a brother.", "answer": "not-stated"}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `true-false`, `position` 1, `version` 1 and exactly the payload that was sent (the statements in the same order), no property it was not sent with (no `null` field), and the stored payload and the listed payload equal the sent one

#### Scenario: item-true-false/R1-S2 Add a Russian item without allowNotStated, Cyrillic text is kept exactly
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `true-false` with the payload `{"statements": [{"text": "  «Ёж» — это животное. ", "answer": "true"}, {"text": "Ёлка и елка — одно и то же слово.", "answer": "false"}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the spaces of the first text and the guillemets kept, "Ёлка" and "елка" untouched) with no `allowNotStated` property, and the stored payload has the same statements and no `allowNotStated` key

#### Scenario: item-true-false/R1-S3 allowNotStated false is stored as sent, null is dropped
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the payloads `{"allowNotStated": false, "statements": [{"text": "A cat is an animal.", "answer": "true"}]}` and `{"allowNotStated": null, "statements": [{"text": "A cat is an animal.", "answer": "true"}]}`
- **THEN** both responses are 201 in the positions 1 and 2, the first payload has `"allowNotStated": false`, the second has no `allowNotStated` property (the null is dropped, not stored as `null`), and the stored payloads equal the response payloads

#### Scenario: item-true-false/R1-S4 Edit a true / false item: the payload is replaced, not merged
- **GIVEN** a logged-in teacher A with an item of the type `true-false` with the payload `{"allowNotStated": true, "statements": [{"text": "Sofia is in Bulgaria.", "answer": "true"}, {"text": "Sofia has a metro of ten lines.", "answer": "not-stated"}]}` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"statements": [{"text": "Sofia has a metro of ten lines.", "answer": "false"}, {"text": "Sofia is in Bulgaria.", "answer": "true"}, {"text": "Sofia is a port.", "answer": "false"}]}`
- **THEN** the response is 200 with the type `true-false`, the new payload exactly as sent (the new order, three statements, no `allowNotStated` property), `version` 2 and the same `position`, and the stored payload equals it

#### Scenario: item-true-false/R1-S5 Editing follows the true-false rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `true-false` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"statements": [{"text": "The sky is blue.", "answer": "not-stated"}]}` and `{"allowNotStated": true, "statements": []}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements[0].answer", code: "INVALID_FORMAT"}]` and `[{field: "payload.statements", code: "REQUIRED"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-true-false/R1-S6 Another teacher cannot add or edit a true / false item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a `true-false` item in it
- **WHEN** teacher B adds a `true-false` item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-true-false/R1-S7 The student view keeps the texts in order and has no answer, allowNotStated is false when missing
- **GIVEN** the `true-false` type and two payloads: `{"allowNotStated": true, "statements": [{"text": "Rome is in Italy.", "answer": "true"}, {"text": "Rome has a river called Seine.", "answer": "false"}, {"text": "The guide speaks Italian.", "answer": "not-stated"}]}` and `{"allowNotStated": null, "statements": [{"text": "Rome is in Italy.", "answer": "false"}]}` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with each payload
- **THEN** the first result has exactly the properties `allowNotStated` (true) and `statements`, with three statements in the stored order, each with exactly the property `text` (no `answer`, no index); the second has `allowNotStated` false (the property exists) and one statement with only `text`; and two payloads that differ only in their answers give identical views

#### Scenario: item-true-false/R1-S8 The serialised student view contains no answer, in English and Russian
- **GIVEN** the `true-false` type and two payloads whose texts contain none of the strings "true", "false", "not-stated" and "answer": an English one with `allowNotStated` true and the statements "Paris is in France." (`true`), "Paris is in Spain." (`false`), "The guide is tall." (`not-stated`), and a Russian one without `allowNotStated` with the statements "Москва стоит на Волге." (`false`), "Волга впадает в Каспийское море." (`true`)
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with exactly the properties `allowNotStated` and `statements`, every statement is an object with exactly the property `text`, no JSON string value equals "true", "false" or "not-stated", the JSON contains no property named `answer`, and every text appears exactly

#### Scenario: item-true-false/R1-S9 The student view of corrupt data fails without echoing stored values
- **GIVEN** the `true-false` type and three payloads built in memory, not validated: `statements` null; `statements` with the statements "Berlin is in Germany." and a `null` element; and `statements` with "Berlin is in Germany." and a statement whose `text` is null
- **WHEN** `studentView` is called with each payload
- **THEN** each call throws `IllegalStateException`, and no message contains "Berlin is in Germany."

### Requirement: R2 A true / false payload follows the limits and validation rules
A `true-false` payload SHALL follow items R2: 1 to 25 statements (`REQUIRED` if missing, null or empty, a null statement too; `TOO_LONG` over 25); `text` non-blank (`REQUIRED`), at most 500 UTF-16 characters (`TOO_LONG`), no exact duplicate (`INVALID_FORMAT` at the later one); `answer` non-blank (`REQUIRED`), only `true`, `false` or `not-stated`, case-sensitive (`INVALID_FORMAT`), `not-stated` only if `allowNotStated` is true (`INVALID_FORMAT`). A wrong JSON type is `INVALID_TYPE`.

#### Scenario: item-true-false/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with: (a) `allowNotStated` true and 25 statements, each text a two-digit number "01" to "25" followed by English letters "a" up to 500 characters, the answers cycling `true`, `false`, `not-stated`; (b) one statement with a text of 500 Cyrillic letters "Я" and the answer `true`; (c) one statement with a text of 250 emoji "😀" (500 UTF-16 code units) and the answer `false`
- **THEN** all three responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-true-false/R2-S2 More than 25 statements, or a text over 500 characters, is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with: (a) 26 statements "Statement 1" to "Statement 26", each with the answer `true`; (b) one statement with a text of 501 Cyrillic letters "Я"; (c) one statement with a text of 251 emoji "😀" (502 UTF-16 code units), the answer `false` in (b) and (c)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements", code: "TOO_LONG"}]` for (a) and `[{field: "payload.statements[0].text", code: "TOO_LONG"}]` for (b) and (c) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-true-false/R2-S3 A missing, null or empty statements list and a null statement are required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the payloads (a) `{}`, (b) `{"statements": null}`, (c) `{"allowNotStated": true, "statements": []}` and (d) `{"statements": [{"text": "A cat is an animal.", "answer": "true"}, null]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements", code: "REQUIRED"}]` for (a), (b) and (c), and `[{field: "payload.statements[1]", code: "REQUIRED"}]` for (d), and the part has no item

#### Scenario: item-true-false/R2-S4 A missing, null, empty or blank text is required, a no-break space is not blank
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the statements `[{"text": "A cat is an animal.", "answer": "true"}, S]` where S is: (a) `{"answer": "true"}`; (b) `{"text": null, "answer": "true"}`; (c) `{"text": "", "answer": "true"}`; (d) `{"text": "  \n\t", "answer": "true"}`; (e) `{"text": " ", "answer": "true"}`
- **THEN** (a) to (d) are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements[1].text", code: "REQUIRED"}]`, (e) is 201 with the text exactly as sent (U+00A0 is not whitespace for `Character.isWhitespace`, as items R5), and the part has exactly one item

#### Scenario: item-true-false/R2-S5 A repeated text is rejected only on an exact match
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the statements (a) `[{"text": "A", "answer": "true"}, {"text": "B", "answer": "false"}, {"text": "A", "answer": "false"}]`; (b) `[{"text": "A", "answer": "true"}, {"text": "A", "answer": "true"}, {"text": "A", "answer": "true"}]`; (c) `[{"text": "Go home.", "answer": "true"}, {"text": "go home.", "answer": "true"}, {"text": "Go home. ", "answer": "true"}, {"text": " Go home.", "answer": "true"}]`
- **THEN** (a) is 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements[2].text", code: "INVALID_FORMAT"}]` (the later one, although the answers differ), (b) is 400 with `errors` exactly `[{field: "payload.statements[1].text", code: "INVALID_FORMAT"}, {field: "payload.statements[2].text", code: "INVALID_FORMAT"}]`, (c) is 201 with all four texts exactly as sent (a different case, a trailing and a leading space make different texts), and the part has exactly one item

#### Scenario: item-true-false/R2-S6 A missing or blank answer is required, any other value than the three is a wrong format
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with `allowNotStated` true and the statements `[{"text": "A cat is an animal.", "answer": "true"}, {"text": "A dog is a bird.", "answer": V}]` where V is: (a) missing; (b) `null`; (c) `""`; (d) `"  "`; (e) `"True"`; (f) `"TRUE"`; (g) `"yes"`; (h) `" true"`; (i) `"true "`; (j) `"Not-stated"`; (k) `"not stated"`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements[1].answer", code: "REQUIRED"}]` for (a) to (d) and `[{field: "payload.statements[1].answer", code: "INVALID_FORMAT"}]` for (e) to (k), and the part has no item

#### Scenario: item-true-false/R2-S7 "not-stated" needs allowNotStated true
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the statements `[{"text": "A cat is an animal.", "answer": "true"}, {"text": "The cat is black.", "answer": "not-stated"}]` and (a) no `allowNotStated`, (b) `"allowNotStated": null`, (c) `"allowNotStated": false`; then (d) `"allowNotStated": false` with `[{"text": "A", "answer": "not-stated"}, {"text": "B", "answer": "true"}, {"text": "C", "answer": "not-stated"}]`; then (e) `"allowNotStated": true` with `[{"text": "The cat is black.", "answer": "not-stated"}]`
- **THEN** (a), (b) and (c) are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.statements[1].answer", code: "INVALID_FORMAT"}]`, (d) is 400 with `errors` exactly `[{field: "payload.statements[0].answer", code: "INVALID_FORMAT"}, {field: "payload.statements[2].answer", code: "INVALID_FORMAT"}]`, (e) is 201 with the payload exactly as sent (every answer may be `not-stated`), and the part has exactly one item

#### Scenario: item-true-false/R2-S8 A value of the wrong JSON type is rejected with its path
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the payloads: `{"allowNotStated": "yes", "statements": [{"text": "A cat is an animal.", "answer": "true"}]}`, `{"allowNotStated": [true], "statements": [{"text": "A cat is an animal.", "answer": "true"}]}`, `{"statements": "text"}`, `{"statements": {}}`, `{"statements": ["text"]}`, `{"statements": [{"text": 5, "answer": "true"}]}`, `{"statements": [{"text": ["Привет"], "answer": "true"}]}`, `{"statements": [{"text": "A cat is an animal.", "answer": true}]}`, `{"statements": [{"text": "A cat is an animal.", "answer": 1}]}` and `{"statements": [{"text": "A cat is an animal.", "answer": ["true"]}]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.allowNotStated", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.statements", code: "INVALID_TYPE"}]` for the next two, `[{field: "payload.statements[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.statements[0].text", code: "INVALID_TYPE"}]` for the next two, and `[{field: "payload.statements[0].answer", code: "INVALID_TYPE"}]` for the last three, no body contains "Привет", and the part has no item

#### Scenario: item-true-false/R2-S9 An unknown property is rejected without echoing its name
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with the payloads `{"statements": [{"text": "A cat is an animal.", "answer": "true"}], "caption": "Intro"}`, `{"statements": [{"text": "A cat is an animal.", "answer": "true", "explanation": "Cats are mammals"}]}` and `{"statements": [{"text": "A cat is an animal.", "answer": "true", "id": "st-7731"}]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]`, no body contains "caption", "Intro", "explanation", "Cats are mammals" or "st-7731", and the part has no item

#### Scenario: item-true-false/R2-S10 Limits and required fields are checked first, the format rules after, and all format errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `true-false` with: (a) `{"allowNotStated": false, "statements": [{"text": "A", "answer": "maybe"}, {"text": "  ", "answer": "true"}]}`; (b) `{"allowNotStated": false, "statements": [{"text": "A", "answer": "maybe"}, {"text": "A", "answer": "not-stated"}, null]}`; (c) 26 statements "Statement 1" to "Statement 26" with the answer `true`, except that the first has the text "" and the last has the answer "maybe"
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.statements[1].text", code: "REQUIRED"}]` (the `maybe` is not reported yet), (b) is 400 with `errors` exactly `[{field: "payload.statements[0].answer", code: "INVALID_FORMAT"}, {field: "payload.statements[1].answer", code: "INVALID_FORMAT"}, {field: "payload.statements[1].text", code: "INVALID_FORMAT"}, {field: "payload.statements[2]", code: "REQUIRED"}]` (sorted by field), (c) is 400 with `errors` exactly `[{field: "payload.statements", code: "TOO_LONG"}, {field: "payload.statements[0].text", code: "REQUIRED"}]` (the answer `maybe` is not reported), and the part has no item
