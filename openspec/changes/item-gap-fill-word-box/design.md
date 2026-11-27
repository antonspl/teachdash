# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `gap-fill-word-box` in the same way as `gap-fill`, in its own package and with no shared code.

## Goals / Non-Goals

**Goals:**
- A structured payload with one answer per gap and optional extra words, with limits.
- A student projection with a shuffled word box that cannot leak which word goes in which gap.

**Non-Goals:**
- Answer checking (#126, #127), the student endpoint (#125), the editor, reusable words and per-gap points.

## Decisions

**D1. Plug-in.** `items/domain/wordbox/WordBoxItemType` (`@Component`, code `gap-fill-word-box`, `payloadClass` `WordBoxPayload`) and the records `WordBoxPayload(List<WordBoxSegment> segments, List<String> extraWords)` and `WordBoxSegment(String type, String text, String answer)`. No controller, service, repository or registry change; no shared class is touched and nothing is shared with gap-fill (Clarification 1). No Flyway migration, no new error code.

**D2. One segment record with a discriminator.** As in gap-fill, a single `WordBoxSegment` has nullable `text` and `answer`, and `type` as a string; no polymorphic Jackson. The strict mapper turns an unknown property into `UNKNOWN_FIELD` (reported as `payload`) and a wrong JSON type into `INVALID_TYPE` with the path (`payload.segments[0].answer`). The per-type fields (a gap with `text`, a text with `answer`) are enforced in `extraChecks` with `UNKNOWN_FIELD` at their path (the name is a known property, safe to echo); a `null` property equals an absent one, which is the stored form (`NON_NULL`, so a `null` `extraWords` is dropped, Clarification 6). `extraWords` null and missing are the same: no extra words.

**D3. Where each rule lives.**
- Bean Validation on the records: `segments` is `@NotEmpty @Size(max = 51)` with `List<@Valid WordBoxSegment>`, `text` is `@Size(max = 500)`, `extraWords` is `@Size(max = 10)` (a missing list passes). No `Map` field.
- `extraChecks` (runs only when Bean Validation passed): a null segment (`REQUIRED`); `type` rules; per type, the text rule ("" and missing are `REQUIRED`, whitespace-only is allowed, so no `@NotBlank`), the fields of the other type (`UNKNOWN_FIELD`), a gap's `answer` (missing or blank `REQUIRED`, over 100 `TOO_LONG`); adjacent texts; each extra word (null or blank `REQUIRED`, over 100 `TOO_LONG` at `extraWords[i]`); duplicates (D4); more than 25 gaps (`TOO_LONG` at `payload.segments`); no gap (`REQUIRED` at `payload.segments`). Blank means `Character.isWhitespace` only, as items R5, so U+00A0 and U+200B are not blank: a known limit kept so one rule applies to every field. Length is `String.length` (UTF-16 units), as items R5. The gap count counts every segment of type `gap`, also one with a missing `answer` (that gap gets its own `REQUIRED`); a null, typeless or unknown-type segment is not a gap.
- Answer and extra-word rules are here and not container-element constraints, because Hibernate Validator appends a `<list element>` node to such paths and the contract paths are `payload.segments[i].answer` and `payload.extraWords[i]`. `FieldErrorCodes.reduce` keeps one code per field, sorted by field with list indexes compared as numbers. `ItemPayloadValidator` adds the `payload.` prefix.
- Consequence, stated in R2: a payload that breaks a limit checked by Bean Validation (text length, extra-word count, segment count) shows only those errors; the structure errors, including a too-long answer or extra word, appear after they are fixed (R2-S11). Because Bean Validation runs first, a gap carrying an over-long `text` reports `TOO_LONG` at `segments[i].text`, not `UNKNOWN_FIELD`.

**D4. Duplicates and the box limit.** All comparisons are exact string equality (Clarification 5). The same answer in two gaps is not an error: the box then holds the word twice, and the student fills each gap from it. An extra word that equals any gap's answer is rejected, because that word would be right and wrong at once and #126 could not check by "is this word an extra". A repeated extra word is rejected at the later index. Blank extra words are skipped by the duplicate checks (they already have `REQUIRED`). The box holds at most 25 answers plus 10 extra words, 35 words.

**D5. Student projection.** `studentView` builds a new envelope `{"segments": [...], "wordBox": [...]}` from an allowlist with a fixed key order (`type` first): `text` and `gap` are matched explicitly and any other type or a null text or answer throws (stored payloads are validated, so this only catches corrupt data); a text segment becomes `{type, text}` and a gap `{type: "gap"}`. It never copies a field from the stored record, so a future field does not leak by default. `wordBox` is every gap's answer plus the extra words in one list, with nothing that marks an extra word. The order is deterministic (Clarification 7): sort by `String.compareTo`, then `Collections.shuffle(list, new java.util.Random(seed))` where seed = `String.join("\u0000", sortedWords).hashCode()`. Sorting first makes the box independent of the gap order, so the box does not reveal the answers' order. The seed is derived from the words, so the same item always gets the same box; the student endpoint (#125) may re-shuffle per request. The number of words in the box equals gaps plus extras, so a student can tell that extras exist only by comparing with the gap count, which is shown anyway. Tests: unit tests on the projection (R1-S8), a serialisation test with the application's JSON mapper (R1-S9) and the order tests with a pinned expected list (R1-S10) and the throw tests (R1-S11).

**D6. Limits and the body size.** Worst case of one request: 25 answers and 10 extra words of 100 characters (3500 UTF-16 units) plus 26 texts of 500 (13000) is 16500 units; fully escaped as `\uXXXX` (6 bytes each) that is about 99 KB, plus about 2 KB of structure: about 101 KB, well under the 1 MB `request_body max_size` of Caddy on `/api/*`. The payload is a small jsonb value.

**D7. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1-S6, R1-S7 and the edit equivalents); R1-S7 here checks the word-box add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No answer or word is logged (constitution section 5).

## Risks / Trade-offs

- [A student can read the answers from the box] -> By design: the box holds the answers; only which gap takes which word is hidden. Extras are not marked.
- [The box order is derived from the words, so the same item always shows the same order] -> Accepted; #125 may re-shuffle per request.
- [A future field on `WordBoxSegment` leaks to students] -> The projection is an allowlist built field by field (D5), with a serialisation test.
- [Exact match hides differences a teacher cannot see: "go" and "go " (trailing space), or "é" composed and decomposed, look equal but are different words, so an extra word may pass the equal-to-answer check, or two gaps may seem to share an answer] -> Accepted (Clarification 9), consistent with gap-fill's raw rule; an editor may warn.
- [Words are raw, so "Go" and "go" differ] -> Accepted (Clarification 5, 6).
- [The unpaginated part list returns every item with its full payload: a part holds at most 50 items (items R1)] -> Accepted for now, as for gap-fill; the word-box payload is smaller than gap-fill's worst case.
- [A rollback after use leaves `gap-fill-word-box` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.
- [The single segment record lets a gap carry `text`] -> `extraChecks` rejects it with `UNKNOWN_FIELD` (R2-S8).

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version.
