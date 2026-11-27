# Proposal

## Clarifications

Issue #220. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and package? | Type code `matching`; package `items/domain/matching/` with `MatchingItemType` (`@Component`), `MatchingPayload` and `MatchingPair`. |
| 2 | Payload shape? | `{pairs: [{left: string, right: string}]}`. |
| 3 | Number of pairs? | 2 to 25 pairs: missing, `null` or empty `pairs` is `REQUIRED` at `payload.pairs`, 1 pair is `TOO_SHORT`, more than 25 is `TOO_LONG`. A `null` pair is `REQUIRED` at `payload.pairs[i]`. |
| 4 | `left` and `right`? | Both required and non-blank (`Character.isWhitespace` rule, as the gap-fill answers), at most 100 UTF-16 code units (`TOO_LONG`); stored raw. |
| 5 | Duplicates? | By exact match (no trim, no case folding: "Go" and "go", and "ё" and "е", differ). A duplicate `left` gives `INVALID_FORMAT` at the later `.left`; the same for `right`. A left equal to some right is allowed. A value with its own error is not compared. Accepted risk (Risks): invisible differences (a trailing space, NFC against NFD) pass. |
| 6 | Student view? | `studentView` returns `{"left": [...], "right": [...]}`. `left` is in stored order. `right` is sorted, then shuffled with the seeded algorithm; the seed is `String.join("\u0000", all lefts then all rights in stored order).hashCode()`. No pair index. The shuffle may return the left-aligned order (accepted, Risks); every student sees the same order; #125 may re-shuffle. |
| 7 | Out of scope? | Answer checking (#126), the student endpoint (#125), the editor, images in pairs, many-to-one matching, partial credit. |
| 8 | Pointer in `items`? | `items` stays untouched: items R2's pointer names only gap-fill and stays as is. Each type's rules live in its own capability; a pointer list in `items` would make every parallel item branch conflict. |

## Research angles

The engineer agreed on this conclusion: the plug-in point and the test layout are the same as for gap-fill.

- **Plug-in point.** A new `@Component` `items/domain/matching/MatchingItemType` implements `ItemType<MatchingPayload>` (code `matching`), with the records `MatchingPayload` and `MatchingPair`, mirroring `GapFillItemType`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change. The `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (an unknown property is `UNKNOWN_FIELD` reported as `payload`; a wrong JSON type is `INVALID_TYPE` with the path; a lone surrogate or NUL is `INVALID_FORMAT` at `payload`), then runs Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `@NotEmpty` gives `REQUIRED`, `@Size` gives `TOO_SHORT` or `TOO_LONG`; where one field has several errors, the highest-priority code stays (`REQUIRED`). The rules per pair and the duplicate rule go in `extraChecks` with the existing codes.
- **Generic rules and scoping.** The generic payload rules (items R2) and teacher scoping (items R1 and R4 scenarios on other teachers) do not depend on the type. The issue still requires testing another teacher's access at each level the change touches, so one scenario covers matching add and edit.
- **Tests.** `items/api/MatchingItemTest` (R1 REST scenarios, with the `ItemApiTest` helpers), `items/api/MatchingPayloadTest` (R2), `items/domain/matching/MatchingItemTypeTest` (unit scenarios) and the helper `items/support/MatchingPayloads`.

## Why

"Match the pairs" is a common exercise for vocabulary (a word and its translation, a country and its capital). The items core holds registered types; this change adds `matching`: pairs of left and right text, stored as structured JSON, and a student view that shows both columns without the pairing.

## What Changes

- New item type `matching` with the payload `{pairs: [{left, right}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `MatchingItemType.studentView`: an allowlist projection with the left items in stored order and the right items in a seeded shuffled order, with no pairing. No endpoint uses it yet (#125).
- No migration, no new error code, no change to the `items` spec, no change to a shared class.

## Capabilities

### New Capabilities
- `item-matching`: R1 (a teacher adds and edits a matching item; the student view hides the pairing) and R2 (limits and validation of the payload).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/matching/MatchingItemType`, `MatchingPayload` and `MatchingPair`. No controller, service, repository or registry change.
- Tests: `items/api/MatchingItemTest` (R1-S1 to R1-S7), `items/api/MatchingPayloadTest` (R2-S1 to R2-S11), `items/domain/matching/MatchingItemTypeTest` (unit scenarios R1-S8 to R1-S11) and `items/support/MatchingPayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126) and the student read endpoint (#125).
- The editor for pairs.
- Images in pairs, many-to-one matching (several left items for one right item) and partial credit.
- Any change to the generic item rules, the item envelope or the other item types.
