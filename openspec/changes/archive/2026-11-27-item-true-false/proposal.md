# Proposal

## Clarifications

Issue #222. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and packaging? | Type code `true-false`; its own package `items/domain/truefalse/` (`TrueFalseItemType`, `TrueFalsePayload`, `TrueFalseStatement`). Kept as its own type and NOT built on item-test's model: branch independence (item-test is a parallel unmerged branch). |
| 2 | Payload shape? | `{allowNotStated?: boolean, statements: [{text: string, answer: "true"\|"false"\|"not-stated"}]}`. `allowNotStated` is an optional `Boolean`; missing or null means false; stored as sent (the stored JSON drops a null). |
| 3 | How many statements? | 1 to 25 statements (missing, null or empty is `REQUIRED` at `payload.statements`, more than 25 is `TOO_LONG`); a null statement is `REQUIRED`. |
| 4 | Statement `text`? | Plain text, required non-blank, at most 500 UTF-16 characters; an exact-match duplicate text is `INVALID_FORMAT` at the later `.text`. |
| 5 | `answer`? | A string; missing, null or blank is `REQUIRED`; any value other than the three (case-sensitive: "True" is wrong) is `INVALID_FORMAT`; "not-stated" when `allowNotStated` is not true is `INVALID_FORMAT` at `.answer`. A wrong JSON type (for example the boolean `true`) is `INVALID_TYPE`. |
| 6 | Student projection? | `studentView` is an allowlist `{"allowNotStated": boolean (false when missing), "statements": [{"text"}]}`, in stored order, with no `answer` key. |
| 7 | Out of scope? | Answer checking (#126), the student endpoint (#125), the editor, explanations per statement, shuffling. |
| 8 | Boolean strictness of `allowNotStated`? | Strict booleans come from PR #242 (item-test), which adds a generic `StrictBooleanJsonConfig` so only JSON booleans are accepted. Today a `Boolean` also accepts `"true"`/`"false"`, `1` and `0`. This change does not pin those coerced cases, because the result differs before and after #242 merges. R2-S8 keeps `"yes"` and `[true]` (`INVALID_TYPE` both before and after). |
| 9 | Pointer in `items`? | None: items R2's pointer names only gap-fill. Each type's rules live in its own capability; this avoids conflicts across the parallel item branches. |
| 10 | Duplicate texts? | Kept: an exact duplicate text is rejected (Clarification 4). |

## Research angles

One researcher ran (a combined angle with the matching and test layout). Its conclusions are the payload shape and rules now in the Clarifications, and the plug-in point and test layout as in gap-fill:

- **Plug-in point.** A new `@Component` `items/domain/truefalse/TrueFalseItemType` implements `ItemType<TrueFalsePayload>` (code `true-false`), mirroring `GapFillItemType` and `TheoryItemType`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change. The `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (an unknown property is `UNKNOWN_FIELD` reported as `payload`; a wrong JSON type is `INVALID_TYPE` with the path), then runs Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `FieldErrorCodes`: `@NotBlank`, `@NotNull`, `@NotEmpty` give `REQUIRED`; `@Size` gives `TOO_SHORT` or `TOO_LONG`; any other constraint gives `INVALID_FORMAT`. Errors are sorted by field, list indexes numerically.
- **Generic rules and scoping.** The generic payload rules (items R2) and teacher scoping (items R1, R4) do not depend on the type and are already covered; one scenario checks another teacher's add and edit for this type.
- **Tests.** `ItemApiTest` helpers, mirroring `GapFillItemTest` and `GapFillItemTypeTest`. The stored JSON drops null fields.

## Why

A true / false statement set (optionally with "not stated", as in reading and listening exams) is a common exercise for teachers of English and Russian. The items core holds registered types; the change adds `true-false` with its limits and a student view that never carries the verdicts.

## What Changes

- New item type `true-false` with the payload of Clarification 2, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `TrueFalseItemType.studentView`: an allowlist projection without any answer. No endpoint uses it yet (#125).
- No `items` spec delta, no migration, no new error code, no change to shared classes (branch independence with the parallel item-type branches).

## Capabilities

### New Capabilities
- `item-true-false`: R1 (a teacher adds and edits a true / false item; the student view hides the answers) and R2 (limits and validation of the payload).

### Modified Capabilities
- None.

## Impact

- Backend: new `items/domain/truefalse/TrueFalseItemType`, `TrueFalsePayload` and `TrueFalseStatement`. No controller, service, repository or registry change.
- Tests: `items/api/TrueFalseItemTest` (REST scenarios R1-S1 to R1-S6), `items/domain/truefalse/TrueFalseItemTypeTest` (unit scenarios R1-S7 to R1-S9) and `items/api/TrueFalsePayloadTest` (REST scenarios R2-S1 to R2-S10).
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126), the student read endpoint (#125), the editor.
- Explanations per statement, shuffling of statements.
- Any change to the generic item rules, the item envelope or the other item types.
