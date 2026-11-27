# Proposal

## Clarifications

Issue #236. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and where does the code live? | Type code `numeric`; new self-contained package `items/domain/numeric/` (`NumericItemType` `@Component`, `NumericPayload`, `NumericQuestion`). |
| 2 | Payload shape? | `{questions: [{text: string, answer: string, tolerance: string, unit?: string}]}`. `answer` and `tolerance` are exact decimals written as JSON strings: the payload arrives as `Map<String,Object>`, so a JSON number would already be a double before validation; strings keep exactness (the issue's "exact decimals" decision). A JSON number for them gives `INVALID_TYPE` (strict string config). |
| 3 | Format of `answer` and `tolerance`? | `^-?\d{1,15}(\.\d{1,10})?$`: dot only, no exponent, no `+`, no comma, no fraction, no spaces. Missing, `null` or blank is `REQUIRED`, else a non-matching value is `INVALID_FORMAT` at the field. `tolerance` is required, absolute and must not be negative (`-...` is `INVALID_FORMAT`); `"0"` is allowed (exact match). Stored raw: "1.50" stays "1.50"; "-0" is allowed as written (for `answer`). |
| 4 | `text` and `unit`? | `text`: plain, required non-blank, at most 500 UTF-16 code units. `unit`: optional display text (not converted); when present, non-blank and at most 20. |
| 5 | How many questions? | 1 to 25 (missing, `null` or empty is `REQUIRED`, more is `TOO_LONG`); a `null` question is `REQUIRED`. |
| 6 | Language? | Stored numbers always use ".". The course's instruction language does not affect stored numbers. Parsing the student's answer (`0,5` in `ru`, `0.5` in `en`, fractions) belongs to #126 and #127. Recorded recommendation for #127: accept both separators in every language, no fractions. Scenarios use `en` and `ru` question and unit text ("km/h", "км/ч", "кг"). |
| 7 | Student view? | `studentView` is an allowlist `{"questions": [{"text", "unit"?}]}` (no `unit` key when absent), in stored order; no `answer`, no `tolerance`. It throws `IllegalStateException` on corrupt data, with no stored values in the message. |
| 8 | Cross-references and infrastructure? | No cross-reference in the `items` spec (each type in its own capability; avoids parallel-branch conflicts). No migration, no new error code. |
| 9 | Out of scope? | Answer checking (#126, #127), relative tolerance, unit conversion, the student endpoint (#125) and the editor. |

Derived from the engineer's decisions, for the spec gate to confirm: a `tolerance` with any leading "-" (also "-0") is `INVALID_FORMAT` (decision 3: `-...`); a blank `unit` (present but empty or whitespace-only) is `REQUIRED` at `.unit`, as other blank texts; a JSON integer outside the `Integer` range is a generic matter (PR #238, items R2-S14), no scenario here.

## Research angles

Conclusions from the code on `main` (as for `item-gap-fill` and `item-test`):

- **Plug-in point.** A `@Component` implementing `ItemType<P>` is collected by `ItemTypeRegistry`; no controller, service or registry change; the `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator`: strict Jackson mapping, then Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `FieldErrorCodes` maps `@NotBlank` and `@NotEmpty` to `REQUIRED`, `@Size` to `TOO_SHORT` or `TOO_LONG`, `@Pattern` to `INVALID_FORMAT`.

## Why

A numeric question ("How fast is the train?" - 60 km/h) cannot be a gap-fill word: the student's `0,5` and `0.50` are the same number, and a teacher accepts a small error. The items core can hold a registered type; the change adds `numeric`, with a correct number, an absolute tolerance and an optional unit per question, stored as exact decimals, and a student view that shows the question and the unit but never the number or the tolerance.

## What Changes

- New item type `numeric` with the payload `{questions: [{text, answer, tolerance, unit?}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `NumericItemType.studentView`: an allowlist projection without `answer` and `tolerance`. No endpoint uses it yet (#125).
- No migration, no new error code, no change to existing endpoints and no change to the `items` spec (Clarification 8).

## Capabilities

### New Capabilities
- `item-numeric`: R1 (a teacher adds and edits a numeric item; the student view hides the number and the tolerance) and R2 (limits and validation of the payload).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/numeric/NumericItemType`, `NumericPayload` and `NumericQuestion`. No controller, service, repository, registry or shared-class change.
- Tests: `items/api/NumericItemTest` (REST scenarios R1-S1 to R1-S5), `items/api/NumericPayloadTest` (REST scenarios R2-S1 to R2-S13), `items/domain/numeric/NumericItemTypeTest` (unit scenarios R1-S6 to R1-S9) and the helper `items/support/NumericPayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer, parsing the student's number and separators (#126, #127), the student read endpoint (#125) and the editor.
- Relative tolerance, unit conversion, a range of accepted values.
- Any change to the generic item rules, the item envelope or the other item types.
