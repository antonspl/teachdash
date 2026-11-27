# Proposal

## Clarifications

Issue #223. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and package? | Type code `reorder`; package `items/domain/reorder/` with `ReorderItemType` (`@Component`), `ReorderPayload` and `ReorderEntry`. One generic type for the three modes (the issue). |
| 2 | Payload shape? | `{mode: "words"\|"sentences"\|"letters", entries: [{units: [string], hint?: string}]}`. `sentences` also covers paragraphs (text order). The units are stored in the correct order. |
| 3 | `mode`? | A string: missing, `null` or blank is `REQUIRED`, any other value (case-sensitive) is `INVALID_FORMAT`, both at `payload.mode`; a wrong JSON type is `INVALID_TYPE`. |
| 4 | Number of entries? | 1 to 25: missing, `null` or empty `entries` is `REQUIRED`, more than 25 is `TOO_LONG`, at `payload.entries`. A `null` entry is `REQUIRED` at `payload.entries[i]`. |
| 5 | `units`? | 2 to 25 per entry: missing, `null` or empty is `REQUIRED`, 1 unit is `TOO_SHORT`, more than 25 is `TOO_LONG`, at `payload.entries[i].units`. Each unit is required and non-blank (`REQUIRED` at `units[j]`) with a maximum length by mode in UTF-16 code units: words 100, sentences 500, letters 4 (a letter or a short letter group: a combining mark, an emoji, a digraph like "ch"); over the limit is `TOO_LONG` at `units[j]`. As the limit depends on `mode`, it is checked in `extraChecks`, and only when the mode is valid; with an invalid mode the unit length check is skipped. |
| 6 | Duplicates? | An entry with fewer than 2 distinct units (for example all the same) is `INVALID_FORMAT` at `entries[i].units` (nothing to order). Other duplicates are allowed ("a", "b", "a" in letters). The comparison is exact; values are stored raw. |
| 7 | `hint`? | Optional; when present, non-blank (`REQUIRED`) and at most 100 (`TOO_LONG`). |
| 8 | Student view? | Allowlist `{"mode", "entries": [{"units": [shuffled], "hint"?}]}` (no `hint` key when absent). Per entry: sort the units by `String.compareTo`, `Collections.shuffle(list, new Random(seed))` with seed = `String.join("\u0000", units in stored order).hashCode()`; if the result equals the stored (correct) order, `Collections.rotate(list, -1)`; with at least 2 distinct units the result then never equals the correct order. Concrete orders are pinned in scenarios, including one that hits the rotate case. Throws `IllegalStateException` on corrupt data. |
| 9 | Out of scope? | Answer checking (#126) and what counts as correct with duplicate units, the student endpoint (#125), the editor, splitting sentences into words automatically, partial credit. |

Recorded from the style of the other item types (no engineer decision): a JSON integer outside the `Integer` range sent where a string is expected is not treated here (the generic fix is PR #238, items R2); `items` stays untouched as for the other item types, so parallel item branches do not conflict.

## Research angles

The plug-in point, the validation pipeline and the test layout are the same as for `matching` and `gap-fill`; the research conclusions are in the brief for this change:

- **Plug-in point.** A new `@Component` `items/domain/reorder/ReorderItemType` implements `ItemType<ReorderPayload>` (code `reorder`), with the records `ReorderPayload` and `ReorderEntry`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (unknown property `UNKNOWN_FIELD` at `payload`, wrong JSON type `INVALID_TYPE`, NUL or lone surrogate `INVALID_FORMAT` at `payload`), then Bean Validation, then `extraChecks` only if Bean Validation passed.
- **Tests.** `items/api/ReorderItemTest` (R1 REST scenarios), `items/api/ReorderPayloadTest` (R2), `items/domain/reorder/ReorderItemTypeTest` (unit scenarios) and the helper `items/support/ReorderPayloads`.

## Why

Putting words, sentences or letters in order is a common exercise (word order in a sentence, the order of a text, unscrambling a word). The items core holds registered types; this change adds `reorder`: lists of units stored in the correct order, with a student view that shows them shuffled and never the correct order.

## What Changes

- New item type `reorder` with the payload `{mode, entries: [{units, hint?}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2, D3), reported with the existing error codes.
- `ReorderItemType.studentView`: an allowlist projection with each entry's units in a seeded shuffled order that is never the correct order. No endpoint uses it yet (#125).
- No migration, no new error code, no change to the `items` spec, no change to a shared class.

## Capabilities

### New Capabilities
- `item-reorder`: R1 (a teacher adds and edits a reorder item), R2 (limits and validation of the payload) and R3 (the student view hides the correct order).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/reorder/ReorderItemType`, `ReorderPayload` and `ReorderEntry`. No controller, service, repository or registry change.
- Tests: `items/api/ReorderItemTest` (R1-S1 to R1-S6), `items/api/ReorderPayloadTest` (R2-S1 to R2-S11), `items/domain/reorder/ReorderItemTypeTest` (unit scenarios R3-S1 to R3-S5) and `items/support/ReorderPayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126) and what counts as correct when units repeat; the student read endpoint (#125).
- The editor for entries.
- Splitting a sentence into words automatically, and partial credit.
- Any change to the generic item rules, the item envelope or the other item types.
