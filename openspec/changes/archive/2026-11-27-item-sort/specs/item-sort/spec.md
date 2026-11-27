## ADDED Requirements

### Requirement: R1 A teacher adds and edits a sort item, and the student view does not show which word is in which column
The type `sort` SHALL have the payload `{columns: [{title, words: [...]}]}`, added with items R1 and edited with items R4, values stored as sent. `studentView` SHALL return `{"titles": [...], "words": [...]}`: `titles` in stored order, `words` all words sorted by `String.compareTo` then `Collections.shuffle(list, new Random(seed))`, seed = `String.join("\u0000", all words in stored reading order).hashCode()`; no column per word. No student endpoint yet (#125): unit scenarios only.

#### Scenario: item-sort/R1-S1 Add an English sort item, it is stored and listed exactly
- **GIVEN** a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
- **WHEN** teacher A adds an item of the type `sort` with the payload `{"columns": [{"title": "Animals", "words": ["cat", "dog", "horse"]}, {"title": "Fruits", "words": ["apple", "pear", "plum"]}]}` and then lists the items of the part
- **THEN** the add response is 201 with the type `sort`, `position` 1, `version` 1 and exactly the payload that was sent (the columns and the words in the same order), no column has a property it was not sent with, and the stored payload and the listed payload equal the sent one

#### Scenario: item-sort/R1-S2 Add a Russian sort item, Cyrillic is kept exactly, "ё" and "е" stay distinct
- **GIVEN** a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
- **WHEN** teacher A adds an item of the type `sort` with the payload `{"columns": [{"title": "Деревья", "words": ["ёлка", "дуб"]}, {"title": "Хвоя", "words": ["елка", "Ёж", "Еж"]}]}`
- **THEN** the response is 201 and the payload equals the sent one character by character (the words "ёлка" and "елка", "Ёж" and "Еж" as distinct values in this order), and the stored payload has the same columns

#### Scenario: item-sort/R1-S3 Edit a sort item: the payload is replaced; a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `sort` with the columns `[{"title": "Cats", "words": ["cat"]}, {"title": "Dogs", "words": ["dog"]}]` and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payload `{"columns": [{"title": "Pets", "words": ["dog", "cat"]}, {"title": "Wild", "words": ["wolf", "fox"]}, {"title": "Birds", "words": ["owl"]}]}`
- **THEN** the response is 200 with the type `sort`, the new payload exactly as sent (replaced, not merged), `version` 2 and the same `position`, and the stored payload has the new three columns

#### Scenario: item-sort/R1-S4 Values are stored raw, with no trim, case change or normalisation
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `sort` with the columns `{" Verbs ", [" Go ", "GO", "go"]}` and `{"Accents", ["é", "é"]}` (written as {title, words}; the first "é" is U+00E9, the second is "e" followed by U+0301, the same letter decomposed)
- **THEN** the response is 201 and the titles and the five words are exactly these strings in this order (the spaces kept, "GO" and "go" both kept, the composed and the decomposed "é" both kept as different values), and the stored payload has the same columns

#### Scenario: item-sort/R1-S5 Editing a sort item follows the sort rules and a bad edit changes nothing
- **GIVEN** a logged-in teacher A with an item of the type `sort` with a valid payload and `version` 1
- **WHEN** teacher A edits it with `version` 1 and the payloads `{"columns": [{"title": "A", "words": ["a"]}]}` and `{"columns": [{"title": "A", "words": ["a"]}, {"title": "B", "words": ["a"]}]}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns", code: "TOO_SHORT"}]` and `[{field: "payload.columns[1].words[0]", code: "INVALID_FORMAT"}]`, and the item keeps its payload and `version` 1

#### Scenario: item-sort/R1-S6 Another teacher cannot add or edit a sort item
- **GIVEN** a logged-in teacher B, and teacher A with a part and a sort item in it
- **WHEN** teacher B adds a sort item to teacher A's part and edits teacher A's item with a valid payload
- **THEN** both responses are 404, and teacher A's part still has exactly the one item with its payload and `version` unchanged

#### Scenario: item-sort/R1-S7 The student view lists the titles in order and the words in the seeded shuffled order
- **GIVEN** the `sort` type and a payload with the columns `[{"title": "Animals", "words": ["cat", "dog", "horse"]}, {"title": "Fruits", "words": ["apple", "pear", "plum"]}]` (unit level: there is no student endpoint yet)
- **WHEN** `studentView` is called with the payload
- **THEN** the result has exactly the properties `titles` and `words`, in this order, with `titles` equal to `["Animals", "Fruits"]` and `words` equal to `["apple", "dog", "pear", "plum", "horse", "cat"]` (the words sorted by `String.compareTo` are `["apple", "cat", "dog", "horse", "pear", "plum"]`; then shuffled with the seed `310127031`, taken from the words in the stored order `cat`, `dog`, `horse`, `apple`, `pear`, `plum`; a sort-only implementation fails), there is no column of a word and no other property

#### Scenario: item-sort/R1-S8 The shuffled order is the same on every call, in Russian too, and depends on the stored order
- **GIVEN** the `sort` type and a payload with the columns `[{"title": "Мужской род", "words": ["стол", "дом"]}, {"title": "Женский род", "words": ["книга", "лампа"]}, {"title": "Средний род", "words": ["окно", "море"]}]` (unit level)
- **WHEN** `studentView` is called three times with the same payload, and once with a payload where only the order of the columns differs (`Женский род`, `Средний род`, `Мужской род`)
- **THEN** the three results are equal, each with `titles` equal to `["Мужской род", "Женский род", "Средний род"]` and `words` equal to `["лампа", "окно", "стол", "книга", "дом", "море"]` (the sorted words `["дом", "книга", "лампа", "море", "окно", "стол"]` shuffled with the seed `165575252`), and the payload with the reordered columns gives `titles` equal to `["Женский род", "Средний род", "Мужской род"]` and `words` equal to `["стол", "книга", "окно", "дом", "море", "лампа"]` (seed `-358281908`), a different order from the first

#### Scenario: item-sort/R1-S9 The serialised student view has no column of a word, in English and Russian
- **GIVEN** the `sort` type and two payloads: an English one with the columns `{"Animals", ["cat", "dog"]}` and `{"Fruits", ["apple", "pear"]}` and a Russian one with the columns `{"Глаголы", ["бежать", "читать"]}` and `{"Существительные", ["ёжик", "мышь"]}` (unit level)
- **WHEN** each payload's student view is serialised to JSON with the application's JSON mapper
- **THEN** the JSON of each view is an object with exactly the properties `titles` and `words`, both arrays of strings, `titles` with the 2 titles and `words` with the 4 words, and it contains no property name `columns`, no nested array, no index and no other property, and every title and every word appears exactly once as an array element

#### Scenario: item-sort/R1-S10 The student view of corrupt data fails without echoing the stored values
- **GIVEN** the `sort` type and corrupt payloads built in the test: `columns` `null`, a `null` column, a column with a `null` title, a column with `words` `null` and a column with a `null` word, each (except the first) next to a valid column with the title "secret-title" and the word "secret-word" (unit level)
- **WHEN** `studentView` is called with each payload
- **THEN** each of the five calls throws `IllegalStateException`, and no message contains "secret-title", "secret-word" or any stored value

### Requirement: R2 A sort payload follows the limits and structure rules
A `sort` payload SHALL follow items R2. `columns`: missing, `null` or empty is `REQUIRED`, 1 is `TOO_SHORT`, over 10 `TOO_LONG`; a `null` column is `REQUIRED`. `words`: missing, `null` or empty `REQUIRED`, over 25 `TOO_LONG` (both checked first). `title` and each word: blank `REQUIRED`, over 100 UTF-16 code units `TOO_LONG`; a repeated title, and a repeated word in any column, `INVALID_FORMAT` at the later one; over 50 words in all `TOO_LONG` at `columns`.

#### Scenario: item-sort/R2-S1 A payload at every limit is accepted in both languages
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with: (a) 2 columns `{"A", ["a"]}` and `{"B", ["b"]}`; (b) 10 columns titled "T1" to "T10" with 5 words each, "w1" to "w50" in order; (c) 2 columns titled "A" and "B" with 25 words each, "w1" to "w25" and "w26" to "w50"; (d) a column whose title is 100 Cyrillic letters "Я" and whose only word is 50 emoji "😀" (100 UTF-16 code units), and a column `{"B", ["x"]}` (each column written as {title, words})
- **THEN** all four responses are 201 with the payload exactly as sent, and the stored payloads equal the sent ones

#### Scenario: item-sort/R2-S2 A title, a word or a count over its limit is too long
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with: (a) 11 columns titled "T1" to "T11" with one word each, "w1" to "w11"; (b) a first column with a title of 101 English letters ("a") and the word "x", and the valid second column `{"B", ["y"]}`; (c) a first column `{"A", [...]}` whose only word is 101 Cyrillic letters ("Я") and the valid second column; (d) a first column with a title of 51 emoji "😀" (102 UTF-16 code units); (e) a first column whose only word is 51 emoji; (f) a first column `{"A", ["w1", ..., "w26"]}` with 26 words, and the valid second column
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns", code: "TOO_LONG"}]` for (a), `[{field: "payload.columns[0].title", code: "TOO_LONG"}]` for (b) and (d), `[{field: "payload.columns[0].words[0]", code: "TOO_LONG"}]` for (c) and (e), and `[{field: "payload.columns[0].words", code: "TOO_LONG"}]` for (f) (no body contains a `rejectedValue` or the sent text), and the part has no item

#### Scenario: item-sort/R2-S3 More than 50 words in all is too long at the columns, together with the other structure errors
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with: (a) 3 columns titled "A", "B", "C" with 20, 20 and 11 valid words "w1" to "w51"; (b) 10 columns titled "T1" to "T10" with 6 words each, "w1" to "w60"; (c) 3 columns titled "A", "B", "C" whose words are "w1" to "w25", "w26" to "w50" and the single word ""
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns", code: "TOO_LONG"}]` for (a) and (b), and `[{field: "payload.columns", code: "TOO_LONG"}, {field: "payload.columns[2].words[0]", code: "REQUIRED"}]` for (c) (the empty word counts in the total of 51), and the part has no item

#### Scenario: item-sort/R2-S4 Missing, null or empty columns are required, one column is too short, a null column is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the payloads `{}`, `{"columns": null}`, `{"columns": []}`, `{"columns": [{"title": "A", "words": ["a"]}]}`, and with the columns `[{"title": "A", "words": ["a"]}, null]` and `[null, null, {"title": "A", "words": ["a"]}]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns", code: "REQUIRED"}]` for the first three, `[{field: "payload.columns", code: "TOO_SHORT"}]` for the fourth, `[{field: "payload.columns[1]", code: "REQUIRED"}]` for the fifth and `[{field: "payload.columns[0]", code: "REQUIRED"}, {field: "payload.columns[1]", code: "REQUIRED"}]` for the last, and the part has no item

#### Scenario: item-sort/R2-S5 A missing, null, empty or blank title is required
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns `[{"title": "A", "words": ["a"]}, X]`, where X is `{"words": ["b"]}`, `{"title": null, "words": ["b"]}`, `{"title": "", "words": ["b"]}`, `{"title": "  ", "words": ["b"]}` and `{"title": "\n\t", "words": ["b"]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns[1].title", code: "REQUIRED"}]`, and the part has no item

#### Scenario: item-sort/R2-S6 Missing, null or empty words are required, and a blank or null word is required at its index
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns `[{"title": "A", "words": ["a"]}, X]`, where (a) X is `{"title": "B"}`, `{"title": "B", "words": null}` and `{"title": "B", "words": []}`; (b) X is `{"title": "B", "words": [""]}`, `{"title": "B", "words": ["  "]}`, `{"title": "B", "words": ["\n\t"]}` and `{"title": "B", "words": [null]}`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns[1].words", code: "REQUIRED"}]` for the three of (a) and `[{field: "payload.columns[1].words[0]", code: "REQUIRED"}]` for the four of (b), and the part has no item

#### Scenario: item-sort/R2-S7 A repeated title is rejected on the later one, and a value with its own error is not compared
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns (written as {title, words}): (a) `[{A, [a]}, {B, [b]}, {A, [c]}]`; (b) `[{A, [a]}, {A, [b]}, {A, [c]}]`; (c) `[{"", [a]}, {"", [b]}]`; (d) two columns with the same title of 101 English letters ("a") and the words `[a]` and `[b]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns[2].title", code: "INVALID_FORMAT"}]` for (a), `[{field: "payload.columns[1].title", code: "INVALID_FORMAT"}, {field: "payload.columns[2].title", code: "INVALID_FORMAT"}]` for (b), `[{field: "payload.columns[0].title", code: "REQUIRED"}, {field: "payload.columns[1].title", code: "REQUIRED"}]` for (c), and `[{field: "payload.columns[0].title", code: "TOO_LONG"}, {field: "payload.columns[1].title", code: "TOO_LONG"}]` for (d) (no `INVALID_FORMAT`), and the part has no item

#### Scenario: item-sort/R2-S8 A word may be in one column only: a repeat is rejected at the later occurrence
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns (written as {title, words}): (a) `[{A, [go, walk, go]}, {B, [ride]}]`; (b) `[{A, [go, walk]}, {B, [ride, go]}]`; (c) `[{A, [x]}, {B, [x]}, {C, [x]}]`; (d) `[{A, [a, b]}, {B, [b, a]}]`
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns[0].words[2]", code: "INVALID_FORMAT"}]` for (a), `[{field: "payload.columns[1].words[1]", code: "INVALID_FORMAT"}]` for (b), `[{field: "payload.columns[1].words[0]", code: "INVALID_FORMAT"}, {field: "payload.columns[2].words[0]", code: "INVALID_FORMAT"}]` for (c), and `[{field: "payload.columns[1].words[0]", code: "INVALID_FORMAT"}, {field: "payload.columns[1].words[1]", code: "INVALID_FORMAT"}]` for (d) (the first occurrence in reading order is kept), and the part has no item

#### Scenario: item-sort/R2-S9 Only an exact repeat is a duplicate, and a title may equal a word
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds an item of the type `sort` with the columns `[{"Go", ["go", "Go", " go", "go ", "ёлка", "елка"]}, {"go", ["GO"]}]` (each written as {title, words}; different case, a leading space, a trailing space, "ё" against "е"; the title "go" of the second column equals a word of the first)
- **THEN** the response is 201 with both columns exactly as sent, and the part has exactly one item

#### Scenario: item-sort/R2-S10 A word with its own error is not compared, the other words still are
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns (written as {title, words}): (a) `[{A, [go, ""]}, {B, ["", go]}]`; (b) `[{A, [w, w]}, {B, [x]}]` where `w` is the same 101 English letters ("a")
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns[0].words[1]", code: "REQUIRED"}, {field: "payload.columns[1].words[0]", code: "REQUIRED"}, {field: "payload.columns[1].words[1]", code: "INVALID_FORMAT"}]` for (a) and `[{field: "payload.columns[0].words[0]", code: "TOO_LONG"}, {field: "payload.columns[0].words[1]", code: "TOO_LONG"}]` for (b) (no `INVALID_FORMAT`), and the part has no item

#### Scenario: item-sort/R2-S11 A value of the wrong JSON type or an unknown property is rejected
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the payloads: `{"columns": "text"}`, `{"columns": {}}`, `{"columns": ["text"]}`, `{"columns": [{"title": 5, "words": ["a"]}]}`, `{"columns": [{"title": "A", "words": "text"}]}`, `{"columns": [{"title": "A", "words": [5]}]}`, `{"columns": [{"title": "A", "words": ["a", ["Привет"]]}]}`, and with a valid payload plus (a) `"caption": "Intro"` in the payload, (b) `"id": "c-7731"` in the first column
- **THEN** all responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload.columns", code: "INVALID_TYPE"}]` for the first two, `[{field: "payload.columns[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.columns[0].title", code: "INVALID_TYPE"}]`, `[{field: "payload.columns[0].words", code: "INVALID_TYPE"}]`, `[{field: "payload.columns[0].words[0]", code: "INVALID_TYPE"}]`, `[{field: "payload.columns[0].words[1]", code: "INVALID_TYPE"}]`, and `[{field: "payload", code: "UNKNOWN_FIELD"}]` for (a) and (b) (the name is not echoed), no body contains "Привет", and the part has no item

#### Scenario: item-sort/R2-S12 The count limits are checked first, the structure rules after, and all structure errors come together
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with (written as {title, words}): (a) 11 columns "T1" to "T11" with one word "w1" to "w11" each, where the first title is ""; (b) the single column `{"", [a]}`; (c) the columns `[{"", [a]}, {B, []}]`; (d) the columns `[{A, [go, go, " "]}, {A, [x]}, null]`; (e) 11 columns "T1" to "T11" where the first has 26 words "w1" to "w26" and each other has one word
- **THEN** (a) is 400 with `errors` exactly `[{field: "payload.columns", code: "TOO_LONG"}]`, (b) is 400 with `errors` exactly `[{field: "payload.columns", code: "TOO_SHORT"}]`, (c) is 400 with `errors` exactly `[{field: "payload.columns[1].words", code: "REQUIRED"}]` (the empty title is not reported until the words are fixed), and (d) is 400 with `errors` exactly `[{field: "payload.columns[0].words[1]", code: "INVALID_FORMAT"}, {field: "payload.columns[0].words[2]", code: "REQUIRED"}, {field: "payload.columns[1].title", code: "INVALID_FORMAT"}, {field: "payload.columns[2]", code: "REQUIRED"}]` (sorted by field, indexes as numbers), and (e) is 400 with `errors` exactly `[{field: "payload.columns", code: "TOO_LONG"}, {field: "payload.columns[0].words", code: "TOO_LONG"}]` (both count limits are reported together), and the part has no item

#### Scenario: item-sort/R2-S13 A NUL and a lone surrogate in a value are rejected as `payload`
- **GIVEN** a logged-in teacher A with a part
- **WHEN** teacher A adds items of the type `sort` with the columns `[{"title": "A", "words": ["a\u0000b"]}, {"title": "B", "words": ["c"]}]` and `[{"title": "A\ud800z", "words": ["a"]}, {"title": "B", "words": ["b"]}]` (JSON escapes: a NUL and a lone high surrogate are characters of the string)
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "payload", code: "INVALID_FORMAT"}]` (no nested path, not 500), no body contains the rejected value, and the part has no item
