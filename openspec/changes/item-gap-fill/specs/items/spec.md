## MODIFIED Requirements

### Requirement: R2 An item payload follows the generic rules and its type's rules
`type` SHALL be a registered code and `payload` a JSON object, else 400 `VALIDATION_FAILED` with `REQUIRED`, `UNKNOWN_TYPE` or `INVALID_TYPE` (the first binding error wins). A NUL or lone surrogate in any payload string, a known field or not, gives `payload` `INVALID_FORMAT`, and an unknown property `payload` `UNKNOWN_FIELD`: no key is echoed. Other errors of the record or its constraints keep the path `payload.<path>`. The stored payload is the record serialised without null fields. The rules of the `gap-fill` type are in the `item-gap-fill` capability.

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

