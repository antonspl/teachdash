# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. `theory` is the only product type. This change adds `gap-fill` in the same way.

## Goals / Non-Goals

**Goals:**
- A structured gap-fill payload with accepted answers per gap, with limits.
- A student projection that cannot leak an answer.

**Non-Goals:**
- Answer checking and normalisation (#126, #127), the student endpoint (#125), the editor and markup (#120), and the deferred features listed in the proposal.

## Decisions

**D1. Plug-in.** `items/domain/gapfill/GapFillItemType` (`@Component`, code `gap-fill`, `payloadClass` `GapFillPayload`) and the records `GapFillPayload(List<GapFillSegment> segments)` and `GapFillSegment(String type, String text, List<String> answers)`. No controller, service, repository or registry change; the registry picks the bean up by injection, and the `items.type` column has no per-type check. No Flyway migration, no new error code (Clarification 10).

**D2. One segment record with a discriminator.** A single `GapFillSegment` has the nullable `text` and `answers` and `type` as a string, not a sealed interface with polymorphic Jackson. Reason: the strict mapper still turns an unknown property into `UNKNOWN_FIELD` (reported as `payload`) and a wrong JSON type into `INVALID_TYPE` with the path (`payload.segments[0].text`), as for every record, and no custom deserializer or type-id error mapping is needed. The per-type fields (a gap with `text`, a text with `answers`) are then enforced in `extraChecks` with `UNKNOWN_FIELD`; a `null` property is the same as an absent one, which is the same as the stored form (`NON_NULL`). A known property in the wrong segment type (a gap with `text`) is reported at its path (`segments[i].text` `UNKNOWN_FIELD`) because the name is a known property and safe to echo; a truly unknown property is reported as `payload` `UNKNOWN_FIELD`, with no name. The implementer checks with a unit test that an unknown property still gives `UNKNOWN_FIELD` on the segment record.

**D3. Where each rule lives (and why the order matters).**
- Bean Validation on the records: `segments` is `@NotEmpty @Size(max = 51)` with `@Valid` on the elements (`List<@Valid GapFillSegment>`), `text` is `@Size(max = 500)`, `answers` is `@Size(max = 5)`. This gives `REQUIRED` and `TOO_LONG` with the paths `payload.segments`, `payload.segments[i].text` and `payload.segments[i].answers`. No `Map` field (a key would be user text).
- `extraChecks` (runs only when Bean Validation passed): a null segment (`REQUIRED`), `type`, `text` and `answers` rules per segment type, adjacent texts, the text rule (`text` missing, `null` or the empty string "" is `REQUIRED`; whitespace-only text is allowed, so no `@NotBlank`), the answer rules (non-blank by `Character.isWhitespace` only, the same rule as the other fields, items R5, so an answer of only U+00A0 no-break space or U+200B zero-width space is accepted: a known limit, kept so one rule applies to every field; at most 100 characters, no exact duplicate in the gap), more than 25 gaps (`TOO_LONG` at `payload.segments`) and no gap (`REQUIRED` at `payload.segments`). The gap count and the no-gap rule count every segment of type `gap`, also one with missing or empty `answers` (that gap gets its own `answers` `REQUIRED`); a null, typeless or unknown-type segment is not a gap. The answer rules are here and not as container-element constraints (`List<@Size(max = 200) String>`), because Hibernate Validator appends a `<list element>` node to the path of such constraints (`segments[0].answers[1].<list element>`), and the contract paths are `payload.segments[i].answers[j]`; a null segment is here for the same reason (a `@NotNull` on the element would add the node). `FieldErrorCodes.reduce` keeps one code per field, sorted by field with list indexes compared as numbers (`payload.segments[2]` before `payload.segments[10]`), not as plain strings. `ItemPayloadValidator` adds the `payload.` prefix, so `extraChecks` returns `segments[i].text` and so on.
- Consequence, stated in R2: a payload that breaks a limit checked by Bean Validation (text length, answer count, segment count) shows only those errors; the structure errors, including a too-long answer, appear after they are fixed (scenario R2-S11). Because Bean Validation runs first, a gap carrying an over-long `text` reports `TOO_LONG` at `segments[i].text`, not `UNKNOWN_FIELD` (R2-S11). Blank (for answers) means `Character.isWhitespace` only, as items R5; length is `String.length` (UTF-16 code units), also as items R5.
- The rule from the research stays: every list is bounded with `@Size`, there is no `Map` field, and the elements are `List<@Valid X>`. The 25-gap limit has no annotation form, so it is an `extraChecks` rule.

**D4. Gap identity is the index among the gap segments.** No id. A gap's index is the count of gap segments before it. #126 keys answers by (student, item) with a snapshot of the item, and an edit keeps old results (#126, #128), so renumbering gaps by an edit does not corrupt stored results (R1-S5 shows the edit).

**D5. Student projection.** `studentView` builds a new envelope `{"segments": [...]}`, the same shape as the stored payload (Clarification 13), from the allowlist, with a fixed key order (`type` first): it matches `text` and `gap` explicitly and throws on any other type or a null text (stored payloads are always validated, so this only catches corrupt data); for a text segment `{type, text}`, for a gap `{type: "gap"}`; it never copies a field from the stored record, so a future field in `GapFillSegment` does not leak by default. No answer count or length is in the view, since either would help a guess. It is not wired to any endpoint (#125). Tests: a unit test on the projection and its `segments` wrapper (R1-S8) and a serialisation test with the application's JSON mapper asserting that no accepted answer and no `answers` name appears in the JSON (R1-S9), in English and Russian. There is no REST scenario until #125.

**D6. Limits and the body size.** The limits are in the proposal (Clarification 7, amended by 12) and are the spec's own, not config properties. Worst case of one request: 25 gaps with 5 answers of 100 characters (12500 characters) plus 26 texts of 500 characters (13000) is 25500 UTF-16 code units. In UTF-8 that is about 51 KB for Cyrillic (2 bytes) and about 77 KB at 3 bytes per character (an emoji is 2 code units and 4 bytes, so also 2 bytes per unit). If every code unit is written as a `\uXXXX` escape (6 bytes), it is 153 KB. The JSON structure (51 segments, 125 answers, quotes, keys and commas) adds about 3 KB, so the worst request is about 156 KB, well under the 1 MB `request_body max_size` of Caddy on `/api/*` (infra/Caddyfile): no request of this type meets that cap before the validator, even fully escaped. The payload is a small jsonb value.

**D7. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1-S6, R1-S7 and the edit equivalents), and R1-S7 here checks the gap-fill add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No answer text is logged (constitution section 5).

## Risks / Trade-offs

- [A gap has no stable id, so a client cannot tell that a gap moved] -> Accepted (Clarification 2); the consumers key by index and snapshot.
- [Answers are raw, so "Go" and "go" are different accepted answers] -> Accepted (Clarification 3, 4); checking and normalisation are #126 and #127.
- [A future field on `GapFillSegment` leaks to students] -> The projection is an allowlist built field by field (D5), with a serialisation test.
- [The unpaginated part list returns every item with its full payload: a part holds at most 50 items (items R1), and a gap-fill item can be about 51 KB (Cyrillic, 2 bytes per character) to 77 KB (3 bytes) of text, so the worst list is about 2.6 MB to 3.9 MB (50 x 51-77 KB, plus structure)] -> Accepted for now; pagination or a lighter list is a separate change.
- [A rollback after use leaves `gap-fill` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert, and cleaning them is a manual database task until such tooling exists.
- [The single segment record lets a gap carry `text`] -> `extraChecks` rejects it with `UNKNOWN_FIELD` (D2, R2-S8).

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version. A rollback after teachers have used the type leaves `gap-fill` rows with an unknown type, and there is no tooling yet to find or clean them (see Risks).
