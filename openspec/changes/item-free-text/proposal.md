# Proposal

## Clarifications

Issue #219. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Plug-in and scope of the change? | Type code `free-text`; new self-contained package `items/domain/freetext/` (`FreeTextItemType` as a `@Component`, `FreeTextPayload`). No shared-class change; no delta of `openspec/specs/items/spec.md` (branch independence with the parallel item-type branches); no migration; no new error code. |
| 2 | Payload and checking? | `{prompt: string, minLength?: int, maxLength?: int}`. The answer is checked by the teacher (the answers work, #126 and #129, adds the "waiting for teacher" state and the verdict). This type stores no accepted answer, so there is nothing to hide. |
| 3 | `prompt`? | Markdown, `@NotBlank`, at most 20000 characters (the same as the theory text: one rule per field kind), stored raw, no trim; lengths are UTF-16 code units. |
| 4 | `minLength` and `maxLength`? | Optional integers (null or absent means no limit), each 1..10000 (`@Min(1) @Max(10000)`, reported as `INVALID_FORMAT`; checked against `FieldErrorCodes`), counted in UTF-16 characters of the answer. A non-integer JSON value (string, float, boolean) gives `INVALID_TYPE` (strict integer config). `minLength > maxLength` gives `INVALID_FORMAT` at `payload.maxLength`, checked in `extraChecks` only when both are present and valid; equal is allowed (an exact length). |
| 5 | Student projection? | `studentView` is an allowlist record `{prompt, minLength, maxLength}`; a null limit is absent from the JSON. The application's JSON mapper does not drop nulls (only the stored-payload mapper does), so the view record carries `@JsonInclude(NON_NULL)`. A unit test checks the serialised JSON keys. |
| 6 | Out of scope? | Answer submission, the teacher verdict and the result states (#126, #129), the student endpoint (#125), the editor, word-count limits and a rubric. |

## Research angles

The engineer agreed on these conclusions from the code on `main`:

- **Plug-in point.** A new `@Component` `items/domain/freetext/FreeTextItemType` implements `ItemType<FreeTextPayload>` (code `free-text`), mirroring `items/domain/theory/TheoryItemType`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (unknown property gives `UNKNOWN_FIELD` at `payload`; a wrong JSON type gives `INVALID_TYPE` with the path), runs Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `FieldErrorCodes`: `@NotBlank` gives `REQUIRED`, `@Size` gives `TOO_LONG`, any other constraint (`@Min`, `@Max`) gives `INVALID_FORMAT`. `StrictIntegerJsonConfig` makes a string, float or boolean in an integer field `INVALID_TYPE`.
- **Tests.** `items/api/FreeTextItemTest`, `items/api/FreeTextPayloadTest` and `items/domain/freetext/FreeTextItemTypeTest`, with the `ItemApiTest` helpers, modelled on `TheoryItemTest`.

## Why

Free writing ("write a few sentences") is a basic exercise type; the teacher reads the answer and judges it. The items core holds registered types, but a text answer that only a teacher can check does not exist yet. The change adds `free-text`: a Markdown prompt with optional answer length limits, and a student view with the prompt and the limits.

## What Changes

- New item type `free-text` with the payload `{prompt, minLength?, maxLength?}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and validation (design D2, D3), reported with the existing error codes.
- `FreeTextItemType.studentView`: an allowlist projection of the prompt and the limits. No endpoint uses it yet (#125).
- No migration, no new error code, no change to the generic item rules or to the `items` spec (Clarification 1).

## Capabilities

### New Capabilities
- `item-free-text`: R1 (a teacher adds and edits a free-text item; the student view) and R2 (limits and validation of the payload).

### Modified Capabilities
- None.

## Impact

- Backend: new `items/domain/freetext/FreeTextItemType` and `FreeTextPayload` (and the view record). No controller, service, repository or registry change.
- Tests: `items/api/FreeTextItemTest` (REST scenarios R1-S1 to R1-S6), `items/domain/freetext/FreeTextItemTypeTest` (unit scenarios R1-S7, R1-S8) and `items/api/FreeTextPayloadTest` (REST scenarios R2-S1 to R2-S7).
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Submitting an answer, the teacher verdict and the "waiting for teacher" state (#126, #129).
- The student read endpoint (#125) and the editor.
- Word-count limits and a rubric.
- Any change to the generic item rules, the item envelope or the other item types.
