# Proposal

## Clarifications

Issue #221. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and package? | Type code `test`; package `items/domain/test/`. If a package named `test` is awkward in Java/Gradle, use `items/domain/choice/` with the classes `TestItemType`, `TestPayload`, `TestQuestion` and `TestOption` (the writer decides and records it). Recorded as decided by the writer (row 9). |
| 2 | Payload shape? | `{questions: [{text: string, options: [{text: string, correct: boolean}]}]}`. |
| 3 | Number of questions? | 1 to 25. A missing, `null` or empty `questions` is `REQUIRED` at `payload.questions`; more than 25 is `TOO_LONG`. A `null` question is `REQUIRED` at `payload.questions[i]`. |
| 4 | Question text? | Plain text (rendering is a frontend concern), required, non-blank, at most 500 UTF-16 characters. |
| 5 | Options? | 2 to 10 options per question: missing, `null` or empty `options` is `REQUIRED`, exactly 1 option is `TOO_SHORT`, more than 10 is `TOO_LONG`. A `null` option is `REQUIRED`. Option `text` is required, non-blank, at most 100. `correct` is required (`Boolean`: missing or `null` is `REQUIRED`, any other value than JSON `true` or `false` is `INVALID_TYPE`, see row 10). A duplicate option text inside one question (exact match) is `INVALID_FORMAT` at the later `.text`; the same text in two questions is allowed. Stored raw. |
| 6 | Questions with no correct option, or all correct? | A question with no correct option is accepted: it is not auto-checked (the answers work, #126, decides whether it is teacher-checked or a poll). A question whose options are all correct is accepted. For one or more correct options there is no stored `multiple` flag: the number of correct options defines it. |
| 7 | Student view? | `studentView` is an allowlist `{"questions": [{"text", "multiple": boolean, "options": [{"text"}]}]}`. `multiple` is true when more than one option is correct, so the UI can pick radio buttons or checkboxes (an accepted leak, design Risks). Options keep their stored order and are not shuffled (a teacher may want "all of the above" last). No `correct` key and no index. A question with zero correct options gives `multiple` false. |
| 8 | Out of scope? | A timer, points per answer (the issue), answer checking (#126), the student endpoint (#125), the editor, shuffling options and images in options. |
| 9 | Package and class names (the writer's record, as row 1 allows)? | A package named `test` would sit beside the test helper `TestNoteType` and the `src/test` tree and confuse readers, so the package is `items/domain/choice/` with `TestItemType` (`@Component`, code `test`), `TestPayload`, `TestQuestion` and `TestOption`. Test classes: `items/api/ChoiceItemTest`, `items/api/ChoicePayloadTest`, `items/domain/choice/ChoiceItemTypeTest`, `items/domain/choice/ChoiceMapperTest` and the helper `items/support/ChoicePayloads`. No change to any shared class, no `openspec/specs/items/spec.md` delta, no migration and no new error code (branch independence with parallel item-type branches). |
| 10 | Boolean coercion? | Make `Boolean` strict like strings and integers: a new `common/StrictBooleanJsonConfig` (mirroring `StrictStringJsonConfig` and `StrictIntegerJsonConfig`) so a `Boolean` or `boolean` field accepts only JSON `true` and `false`; strings, numbers and the empty string give `INVALID_TYPE`. A shared-class change, accepted under the same policy as the existing strict configs. The rule is written in this capability (R2-S12), not as an `items` delta, because PR #238 already modifies items R2. |

## Research angles

The engineer agreed on these conclusions from the code on `main` (as for `gap-fill`):

- **Plug-in point.** A `@Component` implementing `ItemType<P>` is collected by `ItemTypeRegistry` by injection; no controller, service or registry change, and the `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator` does a strict Jackson mapping, then Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `FieldErrorCodes` maps `@NotBlank`, `@NotNull` and `@NotEmpty` to `REQUIRED` (the highest priority when two codes meet at one field) and `@Size` to `TOO_SHORT` or `TOO_LONG`. A string field fails with `INVALID_TYPE` for a number or boolean (`StrictStringJsonConfig`); there is no strict config for `Boolean`.
- **Tests.** `ItemApiTest` helpers; a payload helper like `GapFillPayloads`.

## Why

A teacher needs a multiple-choice question: a question with options of which one or more are right. The items core can hold a registered type; the change adds `test`, with its questions stored as structured JSON, and a student view that never says which options are right.

## What Changes

- New item type `test` with the payload `{questions: [{text, options: [{text, correct}]}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `TestItemType.studentView`: an allowlist projection with `multiple` and no correct marks. No endpoint uses it yet (#125).
- New `common/StrictBooleanJsonConfig`: a `Boolean` accepts only JSON `true` and `false` (Clarification 10).
- No migration, no new error code, no change to existing endpoints and no change to the `items` spec (Clarification 9).

## Capabilities

### New Capabilities
- `item-test`: R1 (a teacher adds and edits a test item), R2 (limits and validation of the payload) and R3 (the student view hides the correct options; a question may have no correct option).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/choice/TestItemType`, `TestPayload`, `TestQuestion` and `TestOption`. No controller, service, repository or registry change; one shared class is added, `common/StrictBooleanJsonConfig`.
- Tests: `items/api/ChoiceItemTest` (REST scenarios R1-S1 to R1-S5 and R3-S1), `items/api/ChoicePayloadTest` (REST scenarios R2-S1 to R2-S12), `items/domain/choice/ChoiceItemTypeTest` (unit scenarios R3-S2 to R3-S5), `ChoiceMapperTest`, a unit test of `StrictBooleanJsonConfig` and the helper `items/support/ChoicePayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- A timer, points per answer, checking a student's answer (#126), the student read endpoint (#125), the editor, shuffling options and images in options.
- Any change to the generic item rules, the item envelope or the other item types.
