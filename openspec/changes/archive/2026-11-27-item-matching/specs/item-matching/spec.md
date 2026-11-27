## ADDED Requirements

### Requirement: R1 A teacher adds and edits a matching item, and the student view hides the pairing
The type `matching` SHALL have the payload `{pairs: [{left, right}]}`, as items R1 and R4, stored as sent. `studentView` SHALL return `{"left": [...], "right": [...]}`: `left` in stored order, `right` the sorted rights shuffled with `new Random(seed)`, seed = `String.join("\u0000", sortedLefts + sortedRights).hashCode()` (so it does not depend on the pairing); no pair index. No endpoint yet (#125).

#### Scenario: item-matching/R1-S1 Add an English matching item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `matching` with the payload `{"pairs": [{"left": "apple", "right": "яблоко"}, {"left": "dog", "right": "собака"}, {"left": "house", "right": "дом"}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `matching`, `position` 1, `version` 1 and exactly the payload that was sent (the pairs in the same order), no pair has a property it was not sent with, and the stored payload and the listed payload equal the sent one

#### Scenario: item-matching/R1-S2 Add a Russian matching item, Cyrillic is kept exactly, "ё" and "е" stay distinct
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `matching` with the payload `{"pairs": [{"left": "Ёж", "right": "животное"}, {"left": "Еж", "right": "колючка"}, {"left": "ёлка", "right": "дерево"}, {"left": "елка", "right": "хвоя"}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the four lefts "Ёж", "Еж", "ёлка" and "елка" as four distinct values in this order), and the stored payload has the same pairs

#### Scenario: item-matching/R1-S3 Values are stored raw, with no trim, case change or normalisation
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `matching` with the pairs `{" Go ", "идти"}`, `{"GO", "ехать"}`, `{"go", "ходить"}`, `{"é", "e1"}` (U+00E9) and `{"é", "e2"}` where the left is "e" followed by U+0301 (the same letter, decomposed)
- **THEN** the response is 201 and the five lefts are exactly these strings in this order (the spaces kept, "GO" and "go" both kept, the composed and the decomposed "é" both kept as different values), and the stored payload has the same five pairs

#### Scenario: item-matching/R1-S4 A left may equal a right, and a pair may have the same text on both sides
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs `[{"left": "go", "right": "went"}, {"left": "went", "right": "go"}]` and `[{"left": "no", "right": "no"}, {"left": "ok", "right": "ok"}]`
- **THEN** both responses are 201 with the pairs exactly as sent, in the positions 1 and 2

#### Scenario: item-matching/R1-S5 Edit a matching item: the payload is replaced
- **GIVEN** a logged-in teacher A with an item of the type `matching` with the pairs `[{"left": "cat", "right": "кошка"}, {"left": "dog", "right": "собака"}]` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"pairs": [{"left": "dog", "right": "пёс"}, {"left": "bird", "right": "птица"}, {"left": "fish", "right": "рыба"}]}`
- **THEN** the response is 200 with the type `matching`, the new payload exactly as sent (replaced, not merged), `version` 2 and the same `position`, and the stored payload has the new three pairs

#### Scenario: item-matching/R1-S6 Editing a matching item follows the matching rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `matching` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"pairs": [{"left": "cat", "right": "кошка"}]}` and `{"pairs": [{"left": "cat", "right": "кошка"}, {"left": "cat", "right": "кот"}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs", code: "TOO_SHORT"}]` and `[{field: "payload.pairs[1].left", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-matching/R1-S7 Another teacher cannot add or edit a matching item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a matching item in it
- **WHEN** teacher B adds a matching item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-matching/R1-S8 The student view lists the lefts in order and the rights in the seeded shuffled order
- **GIVEN** the `matching` type and a payload with the pairs `[{"left": "apple", "right": "яблоко"}, {"left": "dog", "right": "собака"}, {"left": "house", "right": "дом"}, {"left": "tree", "right": "дерево"}]` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with the payload
- **THEN** the result has exactly the properties `left` and `right`, in this order, with `left` equal to `["apple", "dog", "house", "tree"]` and `right` equal to `["собака", "яблоко", "дом", "дерево"]` (the rights sorted by `String.compareTo` are `["дерево", "дом", "собака", "яблоко"]`; then shuffled with the seed `783176451`, computed from the sorted lefts `apple`, `dog`, `house`, `tree` and the sorted rights; a sort-only implementation fails), there is no pair index and no other property

#### Scenario: item-matching/R1-S9 The shuffled order is the same on every call and does not depend on the pairing, in Russian too
- **GIVEN** the `matching` type and a payload with the pairs `[{"left": "Москва", "right": "Russia"}, {"left": "Париж", "right": "France"}, {"left": "Лондон", "right": "England"}, {"left": "Берлин", "right": "Germany"}]` (unit level)
- **WHEN** `studentView` is called three times with the same payload, and once with a payload with the same pairs in the reverse order (`Берлин`, `Лондон`, `Париж`, `Москва` with `Germany`, `England`, `France`, `Russia`)
- **THEN** the three results are equal, each with `left` equal to `["Москва", "Париж", "Лондон", "Берлин"]` and `right` equal to `["Germany", "France", "Russia", "England"]` (the sorted rights `["England", "France", "Germany", "Russia"]` shuffled with the seed `525389863`, computed from the sorted lefts `Берлин`, `Лондон`, `Москва`, `Париж` and the sorted rights), and the payload with the same pairs in the reverse order gives `left` equal to `["Берлин", "Лондон", "Париж", "Москва"]` and `right` equal to `["Germany", "France", "Russia", "England"]`, exactly the same right order as the original (the view does not depend on the pairing)

#### Scenario: item-matching/R1-S10 The serialised student view has no pairing, in English and Russian
- **GIVEN** the `matching` type and two payloads: an English one with the pairs `{"cat", "кошка"}`, `{"dog", "собака"}` and a Russian one with the pairs `{"Ёжик", "hedgehog"}`, `{"мышь", "mouse"}` (unit level)
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with exactly the properties `left` and `right`, each an array of the same length as the number of pairs, and it contains no property name `pairs`, no index and no other property, and every left and every right appears exactly once as an array element

#### Scenario: item-matching/R1-S11 The student view of corrupt data fails without echoing the stored values
- **GIVEN** the `matching` type and corrupt payloads built in the test: `pairs` `null`, a `null` pair, a pair with a `null` left and a pair with a `null` right, each (except the first) next to a valid pair with the text "secret-pair" (unit level)
- **WHEN** `studentView` is called with each payload
- **THEN** each of the four calls throws `IllegalStateException`, and no message contains "secret-pair" or any stored value

### Requirement: R2 A matching payload follows the limits and structure rules
A `matching` payload SHALL follow items R2. `pairs`: missing, `null` or empty is `REQUIRED`, 1 is `TOO_SHORT`, over 25 is `TOO_LONG` (checked first); a `null` pair is `REQUIRED`. `left` and `right`: missing, `null` or blank is `REQUIRED`, over 100 UTF-16 code units `TOO_LONG`, an exact repeat on the same side `INVALID_FORMAT` at the later one; a value with its own error is not compared; a left may equal a right.

#### Scenario: item-matching/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with: (a) 2 pairs `{"a", "b"}` and `{"c", "d"}`; (b) 25 pairs `{"w1", "t1"}` to `{"w25", "t25"}`; (c) one pair whose left and right are 100 Cyrillic letters "Я" and another pair `{"x", "y"}`; (d) one pair whose left and right are 50 emoji "😀" each (100 UTF-16 code units) and another pair `{"x", "y"}`
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-matching/R2-S2 A left, a right or a list of pairs over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with: (a) 26 valid pairs `{"w1", "t1"}` to `{"w26", "t26"}`; (b) a first pair with a left of 101 English letters ("a") and a right "t", and a valid second pair; (c) a first pair with a right of 101 Cyrillic letters ("Я") and a valid second pair; (d) a left of 51 emoji "😀" (102 UTF-16 code units) in the first pair; (e) a right of 51 emoji in the first pair
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs", code: "TOO_LONG"}]` for (a), `[{field: "payload.pairs[0].left", code: "TOO_LONG"}]` for (b) and (d), and `[{field: "payload.pairs[0].right", code: "TOO_LONG"}]` for (c) and (e) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-matching/R2-S3 Missing, null or empty pairs are required, and one pair is too short
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the payloads `{}`, `{"pairs": null}`, `{"pairs": []}` and `{"pairs": [{"left": "a", "right": "b"}]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs", code: "REQUIRED"}]` for the first three and `[{field: "payload.pairs", code: "TOO_SHORT"}]` for the last, and the part has no item

#### Scenario: item-matching/R2-S4 A null pair is required at its index
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs `[{"left": "a", "right": "b"}, null]` and `[null, null, {"left": "a", "right": "b"}]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs[1]", code: "REQUIRED"}]` and `[{field: "payload.pairs[0]", code: "REQUIRED"}, {field: "payload.pairs[1]", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-matching/R2-S5 A missing, null, empty or blank left or right is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs (each after the valid first pair `{"left": "a", "right": "b"}`): `{"right": "d"}`, `{"left": null, "right": "d"}`, `{"left": "", "right": "d"}`, `{"left": "c", "right": "  "}`, `{"left": "c", "right": "\n\t"}`, `{"left": "c"}` and `{"left": " ", "right": ""}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs[1].left", code: "REQUIRED"}]` for the first three, `[{field: "payload.pairs[1].right", code: "REQUIRED"}]` for the next three, and `[{field: "payload.pairs[1].left", code: "REQUIRED"}, {field: "payload.pairs[1].right", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-matching/R2-S6 A repeated left or right is rejected on the later one
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs: (a) `[{go, идти}, {walk, ходить}, {go, ехать}]`; (b) `[{go, идти}, {walk, идти}]`; (c) `[{go, идти}, {go, идти}]`; (d) `[{a, x}, {a, y}, {a, z}]` (each pair written as {left, right})
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs[2].left", code: "INVALID_FORMAT"}]` for (a), `[{field: "payload.pairs[1].right", code: "INVALID_FORMAT"}]` for (b), `[{field: "payload.pairs[1].left", code: "INVALID_FORMAT"}, {field: "payload.pairs[1].right", code: "INVALID_FORMAT"}]` for (c), and `[{field: "payload.pairs[1].left", code: "INVALID_FORMAT"}, {field: "payload.pairs[2].left", code: "INVALID_FORMAT"}]` for (d), and the part has no item

#### Scenario: item-matching/R2-S7 Only an exact repeat is a duplicate, and a left may equal a right
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `matching` with the pairs `[{"Go", "ёлка"}, {"go", "елка"}, {" go", "Ёлка"}, {"go ", "Ёлка "}]` (each written as {left, right}; different case, a leading space, a trailing space, "ё" against "е")
- **THEN** the response is 201 with all four pairs exactly as sent, and the part has exactly one item

#### Scenario: item-matching/R2-S8 A value with its own error is not compared, the other side of its pair still is
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs: (a) `[{"a", "x"}, {"", "y"}, {"", "z"}]`; (b) two pairs whose lefts are the same 101 English letters ("a") and whose rights are "x" and "y"; (c) `[{"go", "a"}, {"go", ""}]` (each written as {left, right})
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs[1].left", code: "REQUIRED"}, {field: "payload.pairs[2].left", code: "REQUIRED"}]` for (a), `[{field: "payload.pairs[0].left", code: "TOO_LONG"}, {field: "payload.pairs[1].left", code: "TOO_LONG"}]` for (b) (no `INVALID_FORMAT`), and `[{field: "payload.pairs[1].left", code: "INVALID_FORMAT"}, {field: "payload.pairs[1].right", code: "REQUIRED"}]` for (c), and the part has no item

#### Scenario: item-matching/R2-S9 A value of the wrong JSON type or an unknown property is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the payloads: `{"pairs": "text"}`, `{"pairs": {}}`, `{"pairs": ["text"]}`, `{"pairs": [{"left": 5, "right": "b"}]}`, `{"pairs": [{"left": "a", "right": ["Привет"]}]}`, and with a valid payload plus (a) `"caption": "Intro"` in the payload, (b) `"id": "p-7731"` in the first pair
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.pairs", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.pairs[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.pairs[0].left", code: "INVALID_TYPE"}]`, `[{field: "payload.pairs[0].right", code: "INVALID_TYPE"}]`, and `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (a) and (b) (the name is not echoed), no body contains "Привет", and the part has no item

#### Scenario: item-matching/R2-S10 The pair count is checked first, the pair rules after, and all pair errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with: (a) 26 pairs `{"w1", "t1"}` to `{"w26", "t26"}` where the first pair's left is ""; (b) the single pair `{"left": "", "right": "b"}`; (c) the pairs `[{"go", "a"}, {"go", " "}, null]` (each written as {left, right})
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.pairs", code: "TOO_LONG"}]`, (b) is 400 with `errors` exactly `[{field: "payload.pairs", code: "TOO_SHORT"}]`, and (c) is 400 with `errors` exactly `[{field: "payload.pairs[1].left", code: "INVALID_FORMAT"}, {field: "payload.pairs[1].right", code: "REQUIRED"}, {field: "payload.pairs[2]", code: "REQUIRED"}]` (sorted by field, indexes as numbers), and the part has no item

#### Scenario: item-matching/R2-S11 A NUL and a lone surrogate in a value are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `matching` with the pairs `[{"left": "a\u0000b", "right": "x"}, {"left": "c", "right": "y"}]` and `[{"left": "a", "right": "x"}, {"left": "b", "right": "y\ud800z"}]` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item
