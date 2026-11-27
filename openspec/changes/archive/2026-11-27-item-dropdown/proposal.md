# Proposal

## Clarifications

Issue #218. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Package and type code? | Type code `dropdown`. A new self-contained package `items/domain/dropdown/` with `DropdownItemType` (`@Component`), `DropdownPayload` and `DropdownSegment`, copying the structure of gap-fill. No shared code with gap-fill and no change to any shared class, so the branch stays independent of the parallel item-type branches. |
| 2 | Payload shape? | `{segments: [{type: "text", text} \| {type: "gap", options: [string], correct: int}]}`. `correct` is the 0-based index into `options`, so exactly one option is correct by construction. Markup such as `[is/was*/are]` is an editor concern. |
| 3 | Structure rules? | As gap-fill: at least one gap (`REQUIRED` at `payload.segments`); a null segment is `REQUIRED`; a missing or blank segment `type` is `REQUIRED`, an unknown type is `INVALID_FORMAT` at `.type`; text missing, `null` or "" is `REQUIRED`, whitespace-only text is allowed; two adjacent texts are `INVALID_FORMAT` on the second; `options` or `correct` on a text segment and `text` on a gap give `UNKNOWN_FIELD` at that path (as gap-fill does for `answers` on a text); the generic rules of items R2 (unknown property, NUL, lone surrogate, wrong JSON types) apply. |
| 4 | Limits? | At most 51 segments and at most 25 gaps (`TOO_LONG` at `payload.segments`); text at most 500 characters; 2 to 5 options per gap (`TOO_SHORT` or `TOO_LONG` at `.options`; missing or `null` options is `REQUIRED`); option at most 100 characters; lengths in UTF-16 units as items R5. |
| 5 | Option rules? | A null or blank option (`Character.isWhitespace` rule, as gap-fill Clarification 16) is `REQUIRED` at `options[j]`; an exact-match duplicate in one gap is `INVALID_FORMAT` at the later index; no trim and no case folding ("Go" and "go" differ; "ё" and "е" differ); the same option in two gaps is allowed; options are stored raw. |
| 6 | `correct`? | Missing or `null` is `REQUIRED` at `.correct`; out of range (below 0 or at least the options size) is `INVALID_FORMAT` at `.correct`; a wrong JSON type is `INVALID_TYPE` (binding). The range check is skipped when `options` itself has an error. |
| 7 | Migration, error codes, items spec? | No Flyway migration, no new error code, no change to `openspec/specs/items/spec.md` (amended by Clarification 12) (no `items` delta, so parallel item-type branches do not conflict; the generic rules already apply). |
| 8 | Student view? | `studentView` is an allowlist returning `{"segments": [...]}`: text segments as `{type, text}`, gaps as `{type: "gap", options: [...]}` with the options in stored order, no `correct` and no index; a fixed key order (`type` first); it throws on an unknown type (corrupt data only). Unit tests, including a serialisation test that the JSON contains no `correct` key. No REST scenario until #125. |
| 9 | Error order? | Errors are sorted by field with list indexes compared as numbers (already in the generic code). |
| 10 | Out of scope? | Answer checking (#126), the student endpoint (#125), the editor (a frontend issue like #120), shuffling of options, per-gap points and hints. |
| 11 | Amends 4: the `@Size(min)` fallback? | Dropped: `FieldErrorCodes` maps `@Size(min)` to `TOO_SHORT`, so the minimum is `@Size(min = 2)` in Bean Validation with no fallback. A unit test keeps the mapping checked. |
| 12 | A JSON integer out of the `Integer` range? | It gives 500 on `main` today: Jackson's `InputCoercionException` is not a `DatabindException`, so `JacksonErrors.codeOf` and `ApiException.fromJackson` rethrow it (verified by a probe). It is fixed in this PR as a generic rule: such a value (for example `correct` or test-note `rating` 2147483648, -2147483649 or 99999999999999999999) gives 400 `VALIDATION_FAILED` with `INVALID_TYPE` at the field path, the same as a float in an integer field, never 500. |

## Research angles

The engineer agreed on these conclusions from the code on `main`:

- **Plug-in point.** A `@Component` implementing `ItemType<P>` is collected by `ItemTypeRegistry` by injection; no controller, service or registry change, and the `items.type` column has no per-type check. `items/domain/dropdown/DropdownItemType` mirrors `items/domain/gapfill/GapFillItemType`.
- **Validation pipeline.** `ItemPayloadValidator` does a strict Jackson mapping, then Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. Rules that must depend on the segment type (a gap with `text`, a text with `options`, a missing `options` or `correct` on a gap) go in `extraChecks`, because a missing value on a shared record cannot be told from a field of the other segment type in Bean Validation.
- **Tests.** `ItemApiTest` helpers; a payload helper `items/support/DropdownPayloads` copied from `GapFillPayloads`; test files `items/api/DropdownItemTest`, `items/api/DropdownPayloadTest` and `items/domain/dropdown/DropdownItemTypeTest`.

## Why

Gap-fill asks the student to type a word. A dropdown item ("choose the option") is the easier, auto-checkable exercise in which each gap offers 2 to 5 options and one is correct. The items core can hold a registered type; the change adds `dropdown`, with the options and the correct index stored as structured JSON and a student view that never carries the correct mark.

## What Changes

- New item type `dropdown` with the payload `{segments: [{type: "text", text} | {type: "gap", options: [...], correct}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `DropdownItemType.studentView`: an allowlist projection with the options and without `correct`. No endpoint uses it yet (#125).
- No migration, no new error code, no change to existing endpoints and, except the generic integer-overflow rule (Clarification 12), no change to the `items` spec.

## Capabilities

### New Capabilities
- `item-dropdown`: R1 (a teacher adds and edits a dropdown item; the student view hides the correct mark) and R2 (limits and validation of the payload).

### Modified Capabilities
- `items`: R2 gets one sentence and a scenario (R2-S14): a JSON integer outside the `Integer` range gives `INVALID_TYPE`, not 500 (Clarification 12). Other item types with `Integer` fields rely on it. This is the only `items` change.

## Impact

- Backend: new `items/domain/dropdown/DropdownItemType`, `DropdownPayload` and `DropdownSegment`. No controller, service, repository or registry change; the only shared-class change is the integer-overflow fix (Clarification 12).
- Tests: `items/api/DropdownItemTest` (REST scenarios R1-S1 to R1-S7), `items/api/DropdownPayloadTest` (REST scenarios R2-S1 to R2-S14), `items/domain/dropdown/DropdownItemTypeTest` (unit scenarios R1-S8, R1-S9) and the helper `items/support/DropdownPayloads`.
- Shared code, by Clarification 12: `JacksonErrors` and `ApiException.fromJackson` map `InputCoercionException` to `INVALID_TYPE`. Test: items R2-S14 in the items tests.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126), the student read endpoint (#125) and the editor and its markup (a frontend issue like #120).
- Shuffling of options, per-gap points and hints.
- Any change to the generic item rules, the item envelope, gap-fill or the other item types.
