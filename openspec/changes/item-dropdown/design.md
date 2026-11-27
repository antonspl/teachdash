# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `dropdown` in the same way as `gap-fill`, in its own package and with no shared code.

## Goals / Non-Goals

**Goals:**
- A structured dropdown payload: options and one correct index per gap, with limits.
- A student projection that cannot leak the correct mark.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, shuffling, per-gap points and hints.

## Decisions

**D1. Plug-in.** `items/domain/dropdown/DropdownItemType` (`@Component`, code `dropdown`, `payloadClass` `DropdownPayload`) and the records `DropdownPayload(List<DropdownSegment> segments)` and `DropdownSegment(String type, String text, List<String> options, Integer correct)`. No controller, service, repository or registry change; no shared class is touched and nothing is shared with gap-fill (Clarification 1). No Flyway migration, no new error code (Clarification 7).

**D2. One segment record with a discriminator.** As in gap-fill, a single `DropdownSegment` has nullable `text`, `options` and `correct`, and `type` as a string; no polymorphic Jackson. `correct` is an `Integer`, so a missing value is `null`. The strict mapper turns an unknown property into `UNKNOWN_FIELD` (reported as `payload`) and a wrong JSON type into `INVALID_TYPE` with the path (`payload.segments[0].correct`). The per-type fields (a gap with `text`, a text with `options` or `correct`) are enforced in `extraChecks` with `UNKNOWN_FIELD` at their path (the name is a known property, safe to echo); a `null` property equals an absent one, which is the stored form (`NON_NULL`). The implementer checks with a unit test that an unknown property still gives `UNKNOWN_FIELD` on the segment record, and that a JSON string or boolean for `correct` gives `INVALID_TYPE`.

**D3. Where each rule lives.**
- Bean Validation on the records: `segments` is `@NotEmpty @Size(max = 51)` with `List<@Valid DropdownSegment>`, `text` is `@Size(max = 500)`, `options` is `@Size(min = 2, max = 5)`. A missing `options` passes `@Size`, so its `REQUIRED` is an `extraChecks` rule (no `@NotNull`, which would also hit text segments). The implementer confirms that `@Size(min)` gives `TOO_SHORT` in `FieldErrorCodes`; otherwise the minimum is checked in `extraChecks` with the same code and path (Clarification 4). No `Map` field.
- `extraChecks` (runs only when Bean Validation passed): a null segment (`REQUIRED`); `type` rules; per type, the text rule ("" and missing are `REQUIRED`, whitespace-only is allowed, so no `@NotBlank`), the fields of the other type (`UNKNOWN_FIELD`), a gap's missing `options` (`REQUIRED`) and missing `correct` (`REQUIRED`); adjacent texts; each option (blank by `Character.isWhitespace` only, as items R5, so U+00A0 and U+200B are not blank: a known limit kept so one rule applies to every field; at most 100 characters; no exact duplicate in the gap); `correct` in range; more than 25 gaps (`TOO_LONG` at `payload.segments`); no gap (`REQUIRED` at `payload.segments`). The gap count counts every segment of type `gap`, also one with missing `options` or `correct`; a null, typeless or unknown-type segment is not a gap.
- The range check of `correct` runs only when the gap's `options` has no error (not null, no `options` or `options[j]` error, including a duplicate); otherwise the index could be checked against a list that is already reported as wrong (Clarification 6).
- Option rules are here and not container-element constraints (`List<@Size(max = 100) String>`), because Hibernate Validator appends a `<list element>` node to such paths, and the contract paths are `payload.segments[i].options[j]`; a null segment is here for the same reason. `FieldErrorCodes.reduce` keeps one code per field, sorted by field with list indexes compared as numbers (Clarification 9). `ItemPayloadValidator` adds the `payload.` prefix, so `extraChecks` returns `segments[i].correct` and so on.
- Consequence, stated in R2: a payload that breaks a limit checked by Bean Validation (text length, option count, segment count) shows only those errors; the structure errors, including a too-long option, appear after they are fixed (R2-S11). Because Bean Validation runs first, a gap carrying an over-long `text` reports `TOO_LONG` at `segments[i].text`, not `UNKNOWN_FIELD`; a text segment carrying `options` of a wrong size reports `TOO_SHORT` or `TOO_LONG` the same way. Length is `String.length` (UTF-16 code units), as items R5.

**D4. `correct` is an index, so "exactly one correct" holds by construction.** There is no per-option flag and no "none correct" or "two correct" state to reject. An edit that moves or removes options is a full replacement of the payload (items R4), so `correct` is always checked against the options sent with it. Gap identity is the 0-based position among the gap segments (as gap-fill); #126 will key answers by it with a snapshot of the item.

**D5. Student projection.** `studentView` builds a new envelope `{"segments": [...]}`, the same shape as the stored payload, from an allowlist with a fixed key order (`type` first): `text` and `gap` are matched explicitly and any other type, a null text or null options throws (stored payloads are validated, so this only catches corrupt data); a text segment becomes `{type, text}` and a gap `{type: "gap", options: [...]}` with the options copied in stored order. It never copies a field from the stored record, so `correct` and any future field do not leak by default. The index of the correct option is not in the view, and the options are not shuffled (out of scope). Tests: a unit test on the projection and its wrapper (R1-S8) and a serialisation test with the application's JSON mapper asserting that no `correct` key appears (R1-S9), in English and Russian. There is no REST scenario until #125.

**D6. Limits and the body size.** Worst case of one request: 25 gaps with 5 options of 100 characters (12500 UTF-16 units) plus 26 texts of 500 (13000) is 25500 units; fully escaped as `\uXXXX` (6 bytes each) that is about 153 KB, plus about 3 KB of structure and the `correct` numbers: about 156 KB, well under the 1 MB `request_body max_size` of Caddy on `/api/*`. The payload is a small jsonb value.

**D7. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1-S6, R1-S7 and the edit equivalents); R1-S7 here checks the dropdown add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No option text is logged (constitution section 5).

## Risks / Trade-offs

- [The correct option is only an index, so reordering options in an editor must update it] -> An editor concern; the backend validates the pair on every write (D4).
- [A future field on `DropdownSegment` leaks to students] -> The projection is an allowlist built field by field (D5), with a serialisation test.
- [Options are raw, so "Go" and "go" are different options] -> Accepted (Clarification 5).
- [The unpaginated part list returns every item with its full payload: a part holds at most 50 items (items R1) and a dropdown item can be about 51 to 77 KB of text, so the worst list is about 2.6 to 3.9 MB] -> Accepted for now, as for gap-fill; pagination or a lighter list is a separate change.
- [A rollback after use leaves `dropdown` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.
- [The single segment record lets a gap carry `text`] -> `extraChecks` rejects it with `UNKNOWN_FIELD` (R2-S7).

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version.
