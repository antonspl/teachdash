# item-reorder Specification

## Purpose
A teacher adds and edits items of the type `reorder`: entries of units (words, sentences or letters) stored in the correct order, with an optional hint, as structured JSON. The student view of the payload shows the units in a shuffled order that does not depend on the stored order.

## Requirements

### Requirement: R1 A teacher adds and edits a reorder item
The type `reorder` SHALL have the payload `{mode, entries: [{units, hint?}]}` (`mode` is `words`, `sentences` or `letters`; `units` are stored in the correct order), added with items R1 and edited with items R4, values stored as sent; another teacher gets 404.

#### Scenario: item-reorder/R1-S1 Add an English words item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `reorder` with the payload `{"mode": "words", "entries": [{"units": ["I", "like", "green", "apples"], "hint": "Я люблю зелёные яблоки"}, {"units": ["She", "reads", "books"]}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `reorder`, `position` 1, `version` 1 and exactly the payload that was sent (the units in the same order, the first entry with its `hint`, the second without a `hint` property, no property that was not sent), and the stored payload and the listed payload equal the sent one

#### Scenario: item-reorder/R1-S2 Add Russian sentences and letters items, Cyrillic is kept exactly
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `reorder` with the payload `{"mode": "sentences", "entries": [{"units": ["Сначала мы позавтракали.", "Потом пошли в школу.", "После уроков вернулись домой."], "hint": "Расскажи, как прошёл день"}]}` and then an item with the payload `{"mode": "letters", "entries": [{"units": ["Ё", "ж"]}, {"units": ["е", "л", "к", "а"]}]}`
- **THEN** both responses are 201 with the payloads equal to the sent ones character by character (the units in the same order, "Ё" and "е" kept as they are), in the positions 1 and 2, and the stored payloads equal the sent ones

#### Scenario: item-reorder/R1-S3 Values are stored raw, with no trim, case change or normalisation
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `reorder` with the payload `{"mode": "words", "entries": [{"units": [" Go ", "GO", "go", "\u00e9", "e\u0301"], "hint": " Order! "}]}` (the fourth unit is "é" composed, the fifth is "e" followed by U+0301, decomposed)
- **THEN** the response is 201 and the five units are exactly these strings in this order (the spaces kept, "GO" and "go" both kept, the composed and the decomposed "é" both kept as different units), the `hint` is " Order! ", and the stored payload is the same

#### Scenario: item-reorder/R1-S4 Edit a reorder item: the payload is replaced, and the mode may change
- **GIVEN** a logged-in teacher A with an item of the type `reorder` with the payload `{"mode": "words", "entries": [{"units": ["I", "like", "tea"], "hint": "old"}]}` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"mode": "letters", "entries": [{"units": ["c", "a", "t"]}, {"units": ["d", "o", "g"]}]}`
- **THEN** the response is 200 with the type `reorder`, the new payload exactly as sent (replaced, not merged: the old hint is gone), `version` 2 and the same `position`, and the stored payload is the new one

#### Scenario: item-reorder/R1-S5 Editing a reorder item follows the reorder rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `reorder` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"mode": "words", "entries": [{"units": ["only"]}]}` and `{"mode": "words", "entries": [{"units": ["go", "go"]}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries[0].units", code: "TOO_SHORT"}]` and `[{field: "payload.entries[0].units", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-reorder/R1-S6 Another teacher cannot add or edit a reorder item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a reorder item in it
- **WHEN** teacher B adds a reorder item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

### Requirement: R2 A reorder payload follows the limits and structure rules
A `reorder` payload SHALL follow items R2. `mode` missing, `null` or blank `REQUIRED`, not `words`, `sentences` or `letters` `INVALID_FORMAT`. `entries` (1-25, 200 units in all) and `units` (2-25): missing, `null`, empty `REQUIRED`; too many `TOO_LONG`; one unit `TOO_SHORT`. A `null` entry or blank unit `REQUIRED`; a unit over 100 (words), 500 (sentences), 4 (letters) UTF-16 code units `TOO_LONG`. Under 2 distinct units `INVALID_FORMAT`. `hint` optional: blank `REQUIRED`, over 100 `TOO_LONG`.

#### Scenario: item-reorder/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with: (a) mode `words`, one entry of 2 units "a" and "b"; (b) mode `words`, 25 entries `{"units": ["a", "b"]}`; (c) mode `letters`, one entry of 25 units "a" to "y" with a `hint` of 100 Cyrillic letters "Я"; (d) mode `words`, one entry of units "a" and 100 Cyrillic letters "Я"; (e) mode `sentences`, one entry of units "a" and 500 English letters "x"; (f) mode `letters`, one entry of units "ch", "a", and "😀😀" (4 UTF-16 code units), and one of "e\u0301" and "a"; (g) mode `letters` and 8 entries of 25 units "a" to "y" (200 units in all); (h) mode `words`, one entry `{"units": ["a", "b"], "hint": null}`; (i) mode `letters` and the units "a" and "\uD83D\uDE42" (a short emoji, 2 UTF-16 code units)
- **THEN** all nine responses are 201 with the payload exactly as sent (for (h) without a `hint` property), and the stored payloads equal the sent ones (for (h) no `hint` key)

#### Scenario: item-reorder/R2-S2 A unit, a hint or a list over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with: (a) mode `words` and 26 entries `{"units": ["a", "b"]}`; (b) mode `letters` and one entry of 26 units "a" to "z"; (c) mode `words` and the units "a" and 101 English letters "x"; (d) mode `sentences` and the units "a" and 501 Cyrillic letters "Я"; (e) mode `letters` and the units "a" and "abcde" (5 letters); (f) mode `letters` and the units "a" and "😀😀😀" (6 UTF-16 code units); (g) mode `words`, units "a" and "b" and a `hint` of 101 English letters "h"; (h) mode `letters`, 7 entries of 25 units "a" to "y" and 2 entries of 13 units "a" to "m" (201 units in all, each entry valid); (i) mode `letters` and the units "a" and "\uD83D\uDC69\u200D\uD83C\uDFEB" (a ZWJ emoji sequence, U+1F469 U+200D U+1F3EB, 5 UTF-16 code units, one grapheme)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries", code: "TOO_LONG"}]` for (a) and (h), `[{field: "payload.entries[0].units", code: "TOO_LONG"}]` for (b), `[{field: "payload.entries[0].units[1]", code: "TOO_LONG"}]` for (c) to (f) and (i), and `[{field: "payload.entries[0].hint", code: "TOO_LONG"}]` for (g) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-reorder/R2-S3 Missing, null or empty entries and units are required, and a single unit is too short
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the payloads `{"mode": "words"}`, `{"mode": "words", "entries": null}`, `{"mode": "words", "entries": []}`, `{"mode": "words", "entries": [{}]}`, `{"mode": "words", "entries": [{"units": null}]}`, `{"mode": "words", "entries": [{"units": []}]}` and `{"mode": "words", "entries": [{"units": ["only"]}]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries", code: "REQUIRED"}]` for the first three, `[{field: "payload.entries[0].units", code: "REQUIRED"}]` for the next three and `[{field: "payload.entries[0].units", code: "TOO_SHORT"}]` for the last, and the part has no item

#### Scenario: item-reorder/R2-S4 A null entry is required at its index
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the mode `words` and the entries `[{"units": ["a", "b"]}, null]` and `[null, null, {"units": ["a", "b"]}]`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries[1]", code: "REQUIRED"}]` and `[{field: "payload.entries[0]", code: "REQUIRED"}, {field: "payload.entries[1]", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-reorder/R2-S5 A missing, null, blank or unknown mode is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the entries `[{"units": ["a", "b"]}]` and the mode: missing, `null`, `""`, `"  "`, `"Words"`, `"word"`, `"paragraphs"` and `"letters "`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.mode", code: "REQUIRED"}]` for the first four and `[{field: "payload.mode", code: "INVALID_FORMAT"}]` for the last four, and the part has no item

#### Scenario: item-reorder/R2-S6 A blank or null unit and a blank hint are required at their index
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the mode `words` and: (a) the units `["a", "", "b"]`; (b) the units `["a", "  ", "b"]`; (c) the units `["a", null, "b"]`; (d) the units `["a", "\n\t"]` and `[" ", "b", ""]` in two entries; (e) the units `["a", "b"]` and the `hint` `""`; (f) the units `["a", "b"]` and the `hint` `"  "`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries[0].units[1]", code: "REQUIRED"}]` for (a), (b) and (c), `[{field: "payload.entries[0].units[1]", code: "REQUIRED"}, {field: "payload.entries[1].units[0]", code: "REQUIRED"}, {field: "payload.entries[1].units[2]", code: "REQUIRED"}]` for (d), and `[{field: "payload.entries[0].hint", code: "REQUIRED"}]` for (e) and (f), and the part has no item

#### Scenario: item-reorder/R2-S7 An entry needs at least 2 distinct units, other duplicates are allowed
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the mode `letters` and: (a) the entries `[{"units": ["a", "b"]}, {"units": ["a", "a"]}]`; (b) the entry `{"units": ["x", "x", "x"]}`; then adds the payloads (c) mode `letters` with the entry `{"units": ["a", "b", "a"]}`; (d) mode `words` with the entry `{"units": ["Go", "go", " go", "go ", "e\u0301", "\u00e9"]}` (U+00E9 and U+0065 U+0301)
- **THEN** (a) and (b) are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.entries[1].units", code: "INVALID_FORMAT"}]` and `[{field: "payload.entries[0].units", code: "INVALID_FORMAT"}]`, and the part has no item after them; (c) and (d) are 201 with the payloads exactly as sent (the comparison is exact: no trim, no case folding, composed and decomposed "é" differ)

#### Scenario: item-reorder/R2-S8 A value of the wrong JSON type or an unknown property is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the payloads: `{"mode": 5, "entries": [{"units": ["a", "b"]}]}`, `{"mode": ["words"], "entries": [{"units": ["a", "b"]}]}`, `{"mode": "words", "entries": "text"}`, `{"mode": "words", "entries": {}}`, `{"mode": "words", "entries": ["text"]}`, `{"mode": "words", "entries": [{"units": "Привет"}]}`, `{"mode": "words", "entries": [{"units": [5, "b"]}]}`, `{"mode": "words", "entries": [{"units": ["a", "b"], "hint": 7}]}`, and a valid payload plus (a) `"caption": "Intro"` in the payload, (b) `"id": "e-7731"` in the first entry
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.mode", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.entries", code: "INVALID_TYPE"}]` for the next two, `[{field: "payload.entries[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.entries[0].units", code: "INVALID_TYPE"}]`, `[{field: "payload.entries[0].units[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.entries[0].hint", code: "INVALID_TYPE"}]`, and `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (a) and (b) (the name is not echoed), no body contains "Привет", and the part has no item

#### Scenario: item-reorder/R2-S9 The list sizes are checked first, the other rules after, and all other errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with: (a) mode "Words" and 26 entries `{"units": ["a", "b"]}` where the first entry's units are `["", "b"]`; (b) mode "Words" and one entry of the units `["a"]`; (c) mode `letters` and the entries `[{"units": ["a", "b"], "hint": " "}, {"units": ["c", ""]}, null, {"units": ["d", "d"]}]`
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.entries", code: "TOO_LONG"}]`, (b) is 400 with `errors` exactly `[{field: "payload.entries[0].units", code: "TOO_SHORT"}]`, and (c) is 400 with `errors` exactly `[{field: "payload.entries[0].hint", code: "REQUIRED"}, {field: "payload.entries[1].units[1]", code: "REQUIRED"}, {field: "payload.entries[2]", code: "REQUIRED"}, {field: "payload.entries[3].units", code: "INVALID_FORMAT"}]` (sorted by field, indexes as numbers; the entry with a unit error is not checked for distinct units), and the part has no item

#### Scenario: item-reorder/R2-S10 With an invalid mode the unit length is not checked, the other rules still are
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with: (a) mode "Letters" and the entry `{"units": ["a", "abcdefgh"]}`; (b) mode "" and the entry `{"units": ["a", "abcdefgh"]}`; (c) mode "Letters" and the entry `{"units": ["a", "", "b"]}`; (d) mode `letters` and the entry `{"units": ["a", "abcdefgh"]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.mode", code: "INVALID_FORMAT"}]` for (a), `[{field: "payload.mode", code: "REQUIRED"}]` for (b), `[{field: "payload.entries[0].units[1]", code: "REQUIRED"}, {field: "payload.mode", code: "INVALID_FORMAT"}]` for (c), and `[{field: "payload.entries[0].units[1]", code: "TOO_LONG"}]` for (d), and the part has no item

#### Scenario: item-reorder/R2-S11 A NUL and a lone surrogate in a value are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `reorder` with the payloads `{"mode": "words", "entries": [{"units": ["a\u0000b", "c"]}]}`, `{"mode": "words", "entries": [{"units": ["a", "b\ud800z"]}]}` and `{"mode": "words", "entries": [{"units": ["a", "b"], "hint": "h\ud800"}]}` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item

### Requirement: R3 The student view shuffles the units and does not depend on the stored order
`studentView` SHALL return `{"mode", "entries": [{"units", "hint"?}]}`: per entry the units sorted by `String.compareTo`, then `Collections.shuffle(list, new Random(seed))`, seed = `String.join("\u0000", sorted units).hashCode()`; no rotate. The view is the same for every stored order of the same units, so it does not depend on the correct order; a chance match of the correct order is accepted. No student endpoint yet (#125): unit scenarios only.

#### Scenario: item-reorder/R3-S1 The student view shuffles each entry's units with the seeded algorithm, in English and Russian
- **GIVEN** the `reorder` type and a payload `{"mode": "words", "entries": [{"units": ["I", "like", "green", "apples"], "hint": "Я люблю зелёные яблоки"}, {"units": ["Я", "люблю", "читать", "книги"]}]}` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with the payload
- **THEN** the result has exactly the properties `mode` and `entries`, in this order, with `mode` equal to "words" and two entries; the first entry has exactly the properties `units` and `hint`, in this order, with `units` equal to `["apples", "green", "I", "like"]` (sorted `["I", "apples", "green", "like"]`, shuffled with the seed `-194232188`) and `hint` equal to the stored one; the second entry has exactly the property `units` (no `hint` key) equal to `["Я", "читать", "люблю", "книги"]` (sorted `["Я", "книги", "люблю", "читать"]`, seed `-375214001`); a sort-only implementation fails

#### Scenario: item-reorder/R3-S2 Sentences are shuffled with the seed of the sorted units, and a chance match is kept
- **GIVEN** the `reorder` type and a payload `{"mode": "sentences", "entries": [{"units": ["Сначала мы позавтракали.", "Потом пошли в школу.", "После уроков вернулись домой."]}, {"units": ["go", "went"]}]}` (unit level)
- **WHEN** `studentView` is called with the payload
- **THEN** the first entry's `units` equal `["После уроков вернулись домой.", "Сначала мы позавтракали.", "Потом пошли в школу."]` (sorted `["После уроков вернулись домой.", "Потом пошли в школу.", "Сначала мы позавтракали."]`, seed `656002372`) and the second entry's `units` equal `["go", "went"]` (seed `105080108`; this equals the stored order, a chance match that is accepted and not rotated)

#### Scenario: item-reorder/R3-S3 The shuffled order is the same on every call and for every stored order of the same units, and keeps repeated units
- **GIVEN** the `reorder` type and three payloads, each `{"mode": "letters", "entries": [{"units": [...]}]}`, with the units `["a", "b"]`, `["b", "a"]` and `["a", "b", "a"]` (unit level)
- **WHEN** `studentView` is called three times with the first payload, and once with each of the others
- **THEN** the three results for `["a", "b"]` are equal, with `units` equal to `["a", "b"]` (seed `93315`); `["b", "a"]` gives the same view, `["a", "b"]` (the same units in another stored order, same seed `93315`); `["a", "b", "a"]` gives `["b", "a", "a"]` (sorted `["a", "a", "b"]`, seed `89674852`), with both "a" kept; and the words payload `["apples", "like", "I", "green"]` gives the same view as R3-S1's `["I", "like", "green", "apples"]`: `["apples", "green", "I", "like"]` (seed `-194232188`)

#### Scenario: item-reorder/R3-S4 The serialised student view has only the allowed properties, in English and Russian
- **GIVEN** the `reorder` type and two payloads: an English one `{"mode": "words", "entries": [{"units": ["The", "cat", "sat"], "hint": "A short sentence"}, {"units": ["go", "went"], "hint": null}]}` and a Russian one `{"mode": "letters", "entries": [{"units": ["к", "о", "т"], "hint": "Животное"}]}` (unit level)
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with exactly the properties `mode` and `entries`; each entry is an object with exactly the property `units` and, only when the stored entry has a non-null hint, `hint` (the second English entry has a `null` hint and no `hint` key); each `units` array has the same elements as the stored units (each stored unit appears as often as it is stored) and there is no property name other than these

#### Scenario: item-reorder/R3-S5 The student view of corrupt data fails without echoing the stored values
- **GIVEN** the `reorder` type and corrupt payloads built in the test, each next to a valid entry with the unit "secret-unit": `mode` `null`, an unknown `mode` "secret-mode", `entries` `null`, a `null` entry, an entry with `units` `null`, an entry with a `null` unit, an entry with `units` `[]`, an entry with the units `["x", "x"]` (unit level)
- **WHEN** `studentView` is called with each payload
- **THEN** each of the eight calls throws `IllegalStateException`, and no message contains "secret-unit", "secret-mode" or any stored value
