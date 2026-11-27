# Proposal

## Clarifications

Issue #113. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Payload shape? | `{ "segments": [ ... ] }`: one flat ordered list. Each segment is `{type: "text", text}` or `{type: "gap", answers: [string]}`. Markup is an editor concern (#120); the backend stores structured JSON only. |
| 2 | Gap identity? | Index-based: a gap is identified by its 0-based position among the gap segments. No id field. Reason: #126 keys answers by (student, item) with a snapshot, and item edits keep old results (#126, #128), so shifting gaps does not break stored results. |
| 3 | Answer storage? | Raw: no trim, no case change, no Unicode normalisation (consistent with items R5 "kept exactly"). Answer checking and normalisation are out of scope (#126, #127; constitution section 9: language-specific normalisation comes from per-language settings). |
| 4 | Duplicate accepted answers? | Rejected only on an exact string match within one gap. "go" and "Go" are both allowed. |
| 5 | Structure rules? | Every gap has at least one answer; an item has at least one gap; every answer is non-blank; a text segment is rejected (`REQUIRED`) only when its `text` is missing, `null` or the empty string "" (amended by Clarification 11: whitespace-only text is allowed). A text segment carries no `answers`; a gap carries no `text` (unknown or wrong fields per segment type are rejected). |
| 6 | Adjacent segments? | Two adjacent text segments are rejected (the error is on the second). Adjacent gaps, and a gap at the start or end, are allowed. |
| 7 | Limits? | Text segment at most 500 characters; answer at most 100 characters (UTF-16 length, as items R5); at most 25 gaps; at most 51 segments; at most 5 answers per gap (amended by Clarification 12). The worst case is about 156 KB even with every character written as a `\uXXXX` escape, under the 1 MB Caddy cap on `/api/*` (design D6). |
| 8 | Student projection? | `GapFillItemType.studentView` is an allowlist and returns `{ "segments": [...] }`, the same envelope as the stored payload (Clarification 13): segments with `text` for text segments and only `{type: "gap"}` for gaps; no answer count or length. No endpoint (the student read endpoint is #125). Tested at unit level, including a serialisation test asserting that no accepted answer string appears anywhere in the JSON. The spec states that no REST scenario is possible yet. |
| 9 | Deferred? | See Out of scope: per-gap case flag, hints, wildcards, partial credit, per-gap points, ё/е handling. |
| 10 | Migration and error codes? | No Flyway migration and no new `api-errors` codes: reuse `REQUIRED`, `TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_TYPE`, `UNKNOWN_FIELD`. |
| 11 | Amends 5: which text is rejected? | Only the empty string "" (and a missing or `null` text, `REQUIRED`). Whitespace-only text (" ", "\n") is allowed anywhere: it separates gaps, e.g. "I ___ ___ to school" is `[text "I ", gap, text " ", gap, text " to school"]`, and a line break may sit between gaps. `@NotBlank` would be wrong; answers stay non-blank as before. |
| 12 | Amends 7: new limits? | Text segment at most 500 characters; answer at most 100 characters (UTF-16 length); at most 25 gaps; at most 51 segments; at most 5 answers per gap. |
| 13 | Student view envelope? | `studentView` returns `{ "segments": [...] }`, the same envelope as the stored payload; R1 states it and R1-S8 and R1-S9 assert the wrapper. |
| 14 | Pointer in `items`? | A one-line pointer in items R2 (a delta) names the `item-gap-fill` capability for the gap-fill type's rules. |
| 15 | Error sort order? | Validation `errors` are sorted by field with list indexes compared as numbers (`payload.segments[2]` before `payload.segments[10]`), not as plain strings. Stated in the items R2 delta. |
| 16 | Blank answer? | `Character.isWhitespace` only, the same rule as the other fields (items R5). U+00A0 and U+200B are not blank, so an answer of only those is accepted. Known limit, kept so one rule applies to every field. |
| 17 | `studentView` strictness? | It matches `text` and `gap` explicitly and throws on any other type or a null text (stored payloads are always validated, so this only catches corrupt data); the view has a fixed key order (`type` first). |

## Research angles

The engineer agreed on these conclusions from the code on `main`:

- **Plug-in point.** A new `@Component` `items/domain/gapfill/GapFillItemType` implements `ItemType<GapFillPayload>` (code `gap-fill`), with a record `GapFillPayload`, mirroring `items/domain/theory/TheoryItemType` and `TheoryPayload`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change. The `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (unknown property gives `UNKNOWN_FIELD` reported as `payload`; wrong JSON type gives `INVALID_TYPE` with the path, for example `payload.segments[0].text`; a lone surrogate or NUL gives `INVALID_FORMAT`), then runs Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. Errors are prefixed `payload.`. `FieldErrorCodes`: `@NotBlank`, `@NotNull`, `@NotEmpty` give `REQUIRED`; `@Size` gives `TOO_SHORT` or `TOO_LONG` (also on lists); any other constraint gives `INVALID_FORMAT`. The per-type rules (a gap with `text`, a text with `answers`, adjacent texts, duplicates, no gap) go in `extraChecks` with the existing codes.
- **Generic rules and scoping.** The generic payload rules (items R2) and teacher scoping (`ItemService.lockForWrite`, items R1-S6, R1-S7 and the matching edit scenarios) do not depend on the type and are already covered. The issue still requires testing another teacher's access at each level the change touches, so one light scenario covers it for gap-fill add and edit.
- **Tests.** `ItemApiTest` helpers (`teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`), mirroring `TheoryItemTest` and `TheoryItemTypeTest`. The stored JSON drops null fields.

## Why

Gap-fill ("fill in the blanks") is the first exercise type of the product, and teachers of English and Russian use it daily. The items core can hold a registered type, but only `theory` exists. The change adds `gap-fill`: a text with gaps and the accepted answers of each gap, stored as structured JSON, and a student view that never carries the answers.

## What Changes

- New item type `gap-fill` with the payload `{segments: [{type: "text", text} | {type: "gap", answers: [...]}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `GapFillItemType.studentView`: an allowlist projection without any answer. No endpoint uses it yet (#125).
- A one-line pointer in the `items` spec (items R2, delta) names the `item-gap-fill` capability. No migration, no new error code, no change to the existing endpoints.

## Capabilities

### New Capabilities
- `item-gap-fill`: R1 (a teacher adds and edits a gap-fill item; the student view hides the answers) and R2 (limits and validation of the payload).

### Modified Capabilities
- `items`: R2 gets one sentence pointing to the `item-gap-fill` capability for the gap-fill type's rules (Clarification 14). The generic rules stay in `items`; no scenario of items changes.

## Impact

- Backend: new `items/domain/gapfill/GapFillItemType` and `GapFillPayload` (and the segment record). No controller, service, repository or registry change.
- Tests: `items/api/GapFillItemTest` (REST scenarios R1-S1 to R1-S7, with `ItemApiTest` helpers), `items/api/GapFillPayloadTest` (REST scenarios R2-S1 to R2-S15) and `items/domain/gapfill/GapFillItemTypeTest` (unit scenarios).
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer, normalisation of answers and language-specific rules (#126, #127; constitution section 9).
- The student read endpoint (#125) and the editor and its markup for gaps (#120).
- Deferred: a per-gap case flag, hints, wildcards, partial credit, per-gap points and special handling of "ё" and "е" (both are plain, distinct strings here).
- Any change to the generic item rules, the item envelope or the other item types.
