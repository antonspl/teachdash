## ADDED Requirements

### Requirement: R1 A teacher adds and edits a free-text item, and the student view shows the prompt and the limits
The type `free-text` SHALL have the payload `{prompt, minLength, maxLength}` (the limits optional), added with items R1, edited with items R4, listed with items R3 and stored as sent (a `null` limit is dropped). The answer is checked by the teacher, so no accepted answer is stored. `studentView` SHALL return the allowlist `{prompt, minLength, maxLength}`, a limit that is absent staying absent from the JSON. Unit only (#125).

#### Scenario: item-free-text/R1-S1 Add an English free-text item with both limits, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `free-text` with the payload `{"prompt": "## Write\n\nDescribe **your day** in 3-5 sentences.\n", "minLength": 50, "maxLength": 500}`, and then lists the items of the part
- **THEN** the add response is 201 with the type `free-text`, `position` 1, `version` 1 and a payload with exactly the properties `prompt`, `minLength` and `maxLength` equal to the sent ones (the newlines kept), and the stored payload and the listed payload equal the sent one

#### Scenario: item-free-text/R1-S2 Add a Russian item, Cyrillic is kept exactly, and a missing or null limit is not stored
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds items of the type `free-text` with the payloads `{"prompt": "  Напишите сочинение о «Ёжике в тумане».  "}`, `{"prompt": "Опишите ёлку.", "minLength": null, "maxLength": null}`, `{"prompt": "Опишите ёлку.", "minLength": 20}` and `{"prompt": "Опишите ёлку.", "maxLength": 300}`
- **THEN** all four responses are 201, in the positions 1 to 4, each prompt equals the sent one character by character (the first keeps its leading and trailing spaces and the guillemets), the first and second payloads have exactly the property `prompt` (a `null` is dropped), the third exactly `prompt` and `minLength` 20, the fourth exactly `prompt` and `maxLength` 300, and the stored payloads equal the responses

#### Scenario: item-free-text/R1-S3 The prompt is stored raw, and the limits may be 1, 10000 or equal
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the payloads `{"prompt": "<b>Hi</b> $x^2$ <img src=x onerror=alert(1)>", "minLength": 1, "maxLength": 10000}` and `{"prompt": "Write one word.", "minLength": 5, "maxLength": 5}`
- **THEN** both responses are 201 with the payload exactly as sent (the HTML and the `$` signs untouched, the limits 1 and 10000 in the first, both 5 in the second), and the stored payloads equal the responses

#### Scenario: item-free-text/R1-S4 Edit a free-text item: the payload is replaced, the limits with it
- **GIVEN** a logged-in teacher A with an item of the type `free-text` with the payload `{"prompt": "Describe your day.", "minLength": 50, "maxLength": 500}` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"prompt": "Опишите свой день.", "maxLength": 200}`
- **THEN** the response is 200 with the type `free-text`, the new payload exactly as sent (replaced, not merged, so there is no `minLength` any more), `version` 2 and the same `position`, and the stored payload has exactly the properties `prompt` and `maxLength`

#### Scenario: item-free-text/R1-S5 Editing a free-text item follows the free-text rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `free-text` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"prompt": "   "}`, `{"prompt": "Ok", "minLength": 10, "maxLength": 9}` and `{"prompt": "Ok", "answer": "x"}` (`answer` is a property of no free-text payload)
- **THEN** the three responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.prompt", code: "REQUIRED"}]`, `[{field: "payload.maxLength", code: "INVALID_FORMAT"}]` and `[{field: "payload", code: "UNKNOWN_FIELD"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-free-text/R1-S6 Another teacher cannot add or edit a free-text item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a free-text item in it
- **WHEN** teacher B adds a free-text item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-free-text/R1-S7 The student view keeps the prompt and the limits and shows no limit that is absent
- **GIVEN** the `free-text` type and three payloads (unit level: there is no student endpoint yet): `{prompt: "Describe your day.", minLength: 50, maxLength: 500}`, `{prompt: "Опишите ёлку.", minLength: null, maxLength: 300}` and `{prompt: "Write.", minLength: null, maxLength: null}`
- **WHEN** `studentView` is called with each payload
- **THEN** each view is an instance of the allowlist record `FreeTextStudentView` with exactly the fields `prompt`, `minLength` and `maxLength` (a field added to the payload and not to the view is not in it), holding the prompt exactly as stored and the limits 50 and 500, no `minLength` and 300, and no limit, in this order

#### Scenario: item-free-text/R1-S8 The serialised student view has exactly the allowed keys, in English and Russian
- **GIVEN** the `free-text` type and two payloads: an English one `{prompt: "Describe your day.", minLength: 50, maxLength: 500}` and a Russian one `{prompt: "Опишите «ёлку».", minLength: null, maxLength: 300}`
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the English JSON is an object whose keys are exactly `prompt`, `minLength` and `maxLength`, in this order, with the sent values; the Russian JSON is an object whose keys are exactly `prompt` and `maxLength`, with no `minLength` key and no `null` value anywhere

### Requirement: R2 A free-text payload follows the limits and validation rules
A `free-text` payload SHALL follow items R2 (also for NUL or a lone surrogate in `prompt`). `prompt` is required, not blank (`REQUIRED`), at most 20000 characters (`TOO_LONG`, UTF-16). `minLength` and `maxLength` are optional integers 1 to 10000 (outside the range but fits an `int`: `INVALID_FORMAT`; a non-integer or an `int` overflow: `INVALID_TYPE`, needs #238). `minLength` above `maxLength` is `INVALID_FORMAT` at `maxLength`, checked after Bean Validation; equal is allowed.

#### Scenario: item-free-text/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with: (a) a `prompt` of 20000 English letters ("a"), `minLength` 1 and `maxLength` 10000; (b) a `prompt` of 20000 Cyrillic letters ("Я") and no limits; (c) a `prompt` of 10000 emoji "😀" (20000 UTF-16 code units) and `minLength` 1 and `maxLength` 1; (d) a `prompt` of a no-break space U+00A0 followed by "x" and `minLength` 10000; (e) a `prompt` of only a no-break space U+00A0 (not blank: `Character.isWhitespace` is false for it)
- **THEN** all five responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-free-text/R2-S2 A prompt over 20000 characters is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with a `prompt` of 20001 Cyrillic letters ("Я") and of 10001 emoji "😀" (20002 UTF-16 code units)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.prompt", code: "TOO_LONG"}]`, no body contains a `rejectedValue` or the sent text, and the part has no item

#### Scenario: item-free-text/R2-S3 A missing or blank prompt is required, and a wrong type or an unknown property is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the payloads `{}`, `{"prompt": null}`, `{"prompt": ""}`, `{"prompt": "   "}`, `{"prompt": "\n\t  "}`, `{"prompt": 5}`, `{"prompt": ["Привет"]}` and `{"prompt": "ok", "hint": "Привет"}`
- **THEN** the first five responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.prompt", code: "REQUIRED"}]`, the next two are 400 with `errors` exactly `[{field: "payload.prompt", code: "INVALID_TYPE"}]`, the last is 400 with `errors` exactly `[{field: "payload", code: "UNKNOWN_FIELD"}]` (the property name is not echoed), no body contains "Привет", and the part has no item

#### Scenario: item-free-text/R2-S4 A limit outside 1 to 10000 is an invalid format
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the prompt "Write." and: (a) `minLength` 0; (b) `minLength` -1; (c) `minLength` 10001; (d) `maxLength` 0; (e) `maxLength` -5; (f) `maxLength` 10001; (g) `minLength` 0 and `maxLength` 10001
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.minLength", code: "INVALID_FORMAT"}]` for (a), (b) and (c), `[{field: "payload.maxLength", code: "INVALID_FORMAT"}]` for (d), (e) and (f), and both errors, `payload.maxLength` before `payload.minLength`, for (g), and the part has no item

#### Scenario: item-free-text/R2-S5 A limit that is not an integer is an invalid type
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the prompt "Write." and: (a) `minLength` "5"; (b) `minLength` 5.5; (c) `minLength` true; (d) `maxLength` ""; (e) `maxLength` "10"; (f) `maxLength` [10]
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.minLength", code: "INVALID_TYPE"}]` for (a), (b) and (c), and `[{field: "payload.maxLength", code: "INVALID_TYPE"}]` for (d), (e) and (f), and the part has no item

#### Scenario: item-free-text/R2-S6 A minimum above the maximum is rejected at the maximum, an equal or single limit is accepted
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the prompt "Write." and: (a) `minLength` 10 and `maxLength` 9; (b) `minLength` 10000 and `maxLength` 1; (c) `minLength` 7 and `maxLength` 7; (d) `minLength` 9999 only; (e) `maxLength` 1 only
- **THEN** (a) and (b) are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.maxLength", code: "INVALID_FORMAT"}]` (no value is echoed), (c), (d) and (e) are 201 with the payload exactly as sent, and the part has exactly three items

#### Scenario: item-free-text/R2-S7 The order of the limits is checked only after the other errors are fixed
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with: (a) a blank `prompt` "  ", `minLength` 10 and `maxLength` 5; (b) the prompt "Write.", `minLength` 10001 and `maxLength` 1
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.prompt", code: "REQUIRED"}]`, (b) is 400 with `errors` exactly `[{field: "payload.minLength", code: "INVALID_FORMAT"}]` (only the limit out of range: the order check does not run, so there is no `payload.maxLength` error), and the part has no item

#### Scenario: item-free-text/R2-S8 A NUL character or a lone surrogate in the prompt is rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `free-text` with the payloads `{"prompt": "a\u0000b"}` and `{"prompt": "a\ud800b"}` (JSON escapes: the NUL and the lone high surrogate are characters of the string)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (as items R2-S5), no body contains the rejected value, and the part has no item
