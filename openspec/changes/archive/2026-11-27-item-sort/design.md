# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. `theory` and `gap-fill` are existing types. This change adds `sort` in the same way.

## Goals / Non-Goals

**Goals:**
- A structured payload of titled columns with their words, with limits and a one-column-per-word rule.
- A student projection that shows the titles and the mixed words but not which word is in which column.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, a word in several columns, images and partial credit.

## Decisions

**D1. Plug-in.** `items/domain/sort/SortItemType` (`@Component`, code `sort`, `payloadClass` `SortPayload`) and the records `SortPayload(List<SortColumn> columns)` and `SortColumn(String title, List<String> words)`. No controller, service, repository or registry change. No Flyway migration, no new error code, no change to the `items` spec or to a shared class (Clarifications 1, 8).

**D2. Where each rule lives.**
- Bean Validation: `SortPayload.columns` is `@NotEmpty @Size(min = 2, max = 10)` with `List<@Valid SortColumn>`; `SortColumn.words` is `@NotEmpty @Size(max = 25)`. An empty list breaks both `@NotEmpty` and `@Size(min)`; the field-error reduction keeps `REQUIRED`. This gives `REQUIRED`, `TOO_SHORT` and `TOO_LONG` at `payload.columns` and `payload.columns[i].words`. `title` and the word elements have no annotation.
- `extraChecks` (runs only when Bean Validation passed), in reading order (column by column; title before words): a `null` column (`REQUIRED` at `columns[i]`); the title (missing, `null`, empty or whitespace-only is `REQUIRED`; over 100 UTF-16 code units, `String.length`, is `TOO_LONG`; a repeat of an earlier title by `String.equals` is `INVALID_FORMAT` at the later `.title`); each word (the same rules; a repeat of an earlier word of any column by `String.equals` is `INVALID_FORMAT` at the later `columns[j].words[k]`); the total of words over all columns above 50 is `TOO_LONG` at `columns`. A title or word with its own error is not added to its seen set and not compared. Blank is `String.isBlank` (`Character.isWhitespace`; U+00A0 and U+200B count as non-blank), the same rule as the other fields. The titles and the words have separate seen sets: a title may equal a word. The total counts every element of every `words` list of a non-null column, also a blank or `null` one, so a payload cannot hide words behind errors. The per-value rules are not container-element constraints, because Hibernate Validator would append a `<list element>` node to the path.
- Consequence, stated in R2: a payload that breaks a limit checked by Bean Validation (column count, words per column or an empty `words`) shows only those errors; the structure errors appear once they are fixed (R2-S12). The total of 50 is an `extraChecks` rule, so it is reported together with the other structure errors (R2-S3).
- Every list is bounded with `@Size`, there is no `Map` field, and the column elements are `List<@Valid SortColumn>`.

**D3. A word belongs to one column.** The duplicate-word rule spans all columns (Clarification 6), so a word cannot be in two columns and the answer for each word is unique. The comparison is exact: "Go" and "go", "ё" and "е", a composed and a decomposed "é" are different values.

**D4. Student projection.** `studentView` returns a new envelope `{"titles": [...], "words": [...]}`, built field by field (an allowlist, fixed key order `titles` then `words`); there is no column field per word and no nesting of words under a title. It throws `IllegalStateException` on corrupt data; it only checks for `null` (`columns` `null`, a `null` column, a `null` title, `words` `null`, a `null` word), without a stored value in the message; stored payloads are always validated, so this only catches corrupt data.
- `titles`: the titles in stored order.
- `words`: all words of all columns in stored reading order, copied into a list, sorted with `String.compareTo` (natural order), then `Collections.shuffle(list, new Random(seed))` with `seed = String.join("\u0000", all words in stored reading order).hashCode()` (an `int` widened to `long`). The sort only makes the shuffle input canonical. The seed depends on the stored order, so reordering words or columns changes the seed and so the order; every student and every call sees the same order for the same stored payload.
- The shuffle may leave the words in an order that follows the columns (for few words the chance is high). This is accepted (Risks); #125 may re-shuffle per student or per view.
- Not wired to any endpoint (#125). Tests: unit tests on the projection (R1-S7, R1-S8, R1-S10) and a serialisation test with the application's JSON mapper (R1-S9).

**D5. Limits and the body size.** The limits are the spec's own, not config properties: 50 words of at most 100 characters and 10 titles of at most 100 characters is 5500 UTF-16 code units. Fully escaped as `\uXXXX` (6 bytes) that is 33 KB, plus about 2 KB of structure: far under the 1 MB `request_body max_size` of Caddy on `/api/*` (infra/Caddyfile). The payload is a small jsonb value. A part holds at most 50 items (items R1), so the unpaginated part list adds at most about 1.8 MB for sort items in the worst case; accepted as for the other types.

**D6. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1 and R4 scenarios on other teachers), and R1-S6 checks sort add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No title or word is logged (constitution section 5).

## Risks / Trade-offs

- [The shuffle may leave the words in column order, so a student sees the answer key] -> Accepted (Clarification 7); #125 may re-shuffle, and the same order for every student is a known limit.
- [The seed comes from the content, so the shuffle is reproducible: anyone holding the view can brute-force it and recover the column of each word] -> Accepted (Clarification 10); the view is not a secret boundary, and #125 must re-shuffle with server-side randomness before a student endpoint exists.
- [Invisible differences pass the duplicate checks: a trailing space, a composed against a decomposed letter] -> Accepted (Clarification 6); values are raw, as in the other types. Two words may then differ only invisibly, so a student sees two identical-looking words.
- [U+00A0 and U+200B are non-blank, so a value of only those is accepted] -> Known limit, kept so one blank rule applies to every field (items R5).
- [A rollback after use leaves `sort` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version (see the last risk).
