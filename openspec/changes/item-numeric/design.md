# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `numeric` in the same way as `gap-fill`, in its own package and with no shared code.

## Goals / Non-Goals

**Goals:**
- A structured payload with an exact decimal answer, an absolute tolerance and an optional unit per question, with limits.
- A student projection that cannot leak the answer or the tolerance.

**Non-Goals:**
- Answer checking and parsing the student's number (#126, #127), the student endpoint (#125), the editor, relative tolerance, unit conversion.

## Decisions

**D1. Plug-in.** `items/domain/numeric/NumericItemType` (`@Component`, code `numeric`, `payloadClass` `NumericPayload`) and the records `NumericPayload(List<NumericQuestion> questions)` and `NumericQuestion(String text, String answer, String tolerance, String unit)`. No controller, service, repository or registry change; no shared class is touched (Clarification 1, 8). No Flyway migration, no new error code.

**D2. Decimals are strings.** The payload arrives as `Map<String,Object>`; a JSON number would already be a `Double` (or `Integer`) before validation and `0.1` could not be told from `0.10`. So `answer` and `tolerance` are `String` fields read with the strict string config: a JSON number, boolean, array or object gives `INVALID_TYPE` with the path (Clarification 2). The text is stored raw ("1.50" stays "1.50", "007" stays "007", "-0" stays "-0" for `answer`). The class later used for checking (#126) parses with `BigDecimal`: the format keeps at most 15 integer and 10 fraction digits, so values are exact.

**D3. Where each rule lives.**
- Bean Validation on the records (all reported together, sorted by field): `questions` is `@NotEmpty @Size(max = 25)` with `List<@Valid NumericQuestion>`; `text` is `@NotBlank @Size(max = 500)`; `answer` is `@NotBlank @Pattern(^-?\d{1,15}(\.\d{1,10})?$)`; `tolerance` is `@NotBlank @Pattern(^\d{1,15}(\.\d{1,10})?$)` (a leading "-" does not match, so "-0" is rejected too); `unit` has no Bean Validation rule. A blank value gets `REQUIRED` only: `FieldErrorCodes.reduce` keeps one code per field and `REQUIRED` ranks above `INVALID_FORMAT`. Blank means `Character.isWhitespace` only, as items R5. Length is `String.length` (UTF-16 units). The `\d` in the patterns is ASCII-only (the pattern is compiled without the Unicode flag), so Arabic-Indic digits are `INVALID_FORMAT`.
- `extraChecks` (runs only when Bean Validation passed): a `null` question is `REQUIRED` at `payload.questions[i]`; a `unit` that is present is checked blank first (`REQUIRED` at `.unit`, whatever its length, so 21 spaces are `REQUIRED`), then length over 20 (`TOO_LONG`); `null` and missing mean no unit.
- Consequence, stated in R2: a payload that breaks a Bean Validation rule shows only those errors; a `null` question and a bad `unit` (blank or too long) appear after they are fixed (R2-S13).
- `ItemPayloadValidator` adds the `payload.` prefix.

**D4. Tolerance semantics are for #126.** The stored tolerance is absolute: the student's number is right when it differs from `answer` by at most `tolerance`. This change only stores and validates; it states no check. `"0"` means an exact match. Tolerance has no upper bound beyond the format; a tolerance larger than the answer is the teacher's choice.

**D5. Student projection.** `studentView` builds a new envelope `{"questions": [...]}` from an allowlist with a fixed key order (`text`, then `unit`): each question becomes `{text}` or `{text, unit}`; the `unit` key is absent, not `null`, when the stored unit is absent. It never copies a field from the stored record, so a future field does not leak by default. Order is the stored order (no shuffle). A `null` question list, `null` question or `null` text throws `IllegalStateException` (stored payloads are validated, so this only catches corrupt data); messages hold no stored value. Tests: unit tests on the projection (R1-S6, R1-S7), a serialisation test with the application's JSON mapper (R1-S8) and the throw test (R1-S9).

**D6. Limits and the body size.** Worst case of one request: 25 questions of a 500-unit text, two numbers of at most 27 characters (the longest answer is "-", 15 digits, ".", 10 digits) and a 20-unit unit is about 13000 units; escaped as `\uXXXX` (6 bytes) about 80 KB plus structure, well under the 1 MB `request_body max_size` of Caddy on `/api/*`. The payload is a small jsonb value.

**D7. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; R1-S5 checks the numeric add and edit with another teacher). Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. Numbers always use "." whatever the course's instruction language (Clarification 6). No question, number or unit is logged (constitution section 5).

## Risks / Trade-offs

- [The teacher of a `ru` course types "0,5" in the editor and gets `INVALID_FORMAT`] -> By design: stored numbers use "." only; the editor may convert, and #127 decides what the student may type (recommendation: both separators in every language, no fractions).
- [A leading zero or trailing zeros ("007", "1.50") are kept raw] -> Accepted; the check parses to `BigDecimal`, so they are the same number.
- [A very small tolerance relative to the answer, or a huge one, is not rejected] -> Accepted (D4).
- [A future field on `NumericQuestion` leaks to students] -> The projection is an allowlist built field by field (D5), with a serialisation test.
- [A rollback after use leaves `numeric` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.
- [The unpaginated part list returns every item with its full payload: a part holds at most 50 items (items R1)] -> Accepted, as for the other types.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version.
