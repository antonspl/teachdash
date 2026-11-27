# Proposal

## Clarifications

Issue #224. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Type code and package? | Type code `sort`; package `items/domain/sort/` with `SortItemType` (`@Component`), `SortPayload` and `SortColumn`. |
| 2 | Payload shape? | `{columns: [{title: string, words: [string]}]}`. |
| 3 | Number of columns? | 2 to 10 columns: missing, `null` or empty `columns` is `REQUIRED` at `payload.columns`, 1 column is `TOO_SHORT`, more than 10 is `TOO_LONG`. A `null` column is `REQUIRED` at `payload.columns[i]`. |
| 4 | `title`? | Required and non-blank, at most 100 UTF-16 code units (`TOO_LONG`). An exact-match duplicate title gives `INVALID_FORMAT` at the later `.title`. |
| 5 | `words`? | 1 to 25 words per column: missing, `null` or empty `words` is `REQUIRED` at `columns[i].words`, more than 25 is `TOO_LONG`. Each word is required and non-blank (`REQUIRED`) and at most 100 UTF-16 code units (`TOO_LONG`). The total of words over all columns is at most 50 (`TOO_LONG` at `payload.columns`, checked in `extraChecks`). |
| 6 | A word in one column only? | An exact-match duplicate word, within one column or across columns, gives `INVALID_FORMAT` at the later occurrence (`columns[j].words[k]`), in reading order: column by column, word by word. A value with its own error is not compared. The match is exact ("Go" and "go", "ё" and "е" differ); values are stored raw. Accepted risk (Risks): invisible differences pass. |
| 7 | Student view? | `studentView` returns the allowlist `{"titles": [stored order], "words": [all words, sorted by String.compareTo then Collections.shuffle(list, new Random(seed)), seed = String.join("\u0000", all words in stored reading order).hashCode()]}`. No column per word. The concrete orders are pinned in scenarios (computed with a Java run). It throws `IllegalStateException` on corrupt data, with no stored value in the message. |
| 8 | Pointer in `items`, migration, error codes? | No cross-reference in `items`: items R2's pointer names only gap-fill and stays as is; each type has its own capability (this avoids conflicts between parallel item branches). No migration, no new error code. |
| 10 | Is the shuffled view a secret? | No. The seed comes from the content, so anyone with the view can brute-force the shuffle and recover the column of each word; the view is not a secret boundary. #125 must re-shuffle with server-side randomness. |
| 9 | Out of scope? | Answer checking (#126), the student endpoint (#125), the editor, a word in several columns, images, partial credit. |

## Research angles

The engineer agreed on this conclusion: the plug-in point and the test layout are the same as for gap-fill.

- **Plug-in point.** A new `@Component` `items/domain/sort/SortItemType` implements `ItemType<SortPayload>` (code `sort`), with the records `SortPayload` and `SortColumn`, mirroring `GapFillItemType`. `ItemTypeRegistry` collects it by injection; no controller, service or registry change. The `items.type` column has no per-type check.
- **Validation pipeline.** `ItemPayloadValidator` reads the payload with a strict Jackson mapper (an unknown property is `UNKNOWN_FIELD` reported as `payload`; a wrong JSON type is `INVALID_TYPE` with the path; a lone surrogate or NUL is `INVALID_FORMAT` at `payload`), then runs Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `@NotEmpty` gives `REQUIRED`, `@Size` gives `TOO_SHORT` or `TOO_LONG`. The title and word rules and the duplicate rules go in `extraChecks` with the existing codes.
- **Generic rules and scoping.** The generic payload rules (items R2) and teacher scoping do not depend on the type. The issue still requires testing another teacher's access at each level the change touches, so one scenario covers sort add and edit.
- **Tests.** `items/api/SortItemTest` (R1 REST scenarios, with the `ItemApiTest` helpers), `items/api/SortPayloadTest` (R2), `items/domain/sort/SortItemTypeTest` (unit scenarios) and the helper `items/support/SortPayloads`.

## Why

"Sort into columns" is a common exercise for vocabulary and grammar (words by part of speech, nouns by gender, words by topic). The items core holds registered types; this change adds `sort`: columns with a title and the words that belong in each, stored as structured JSON, and a student view that shows the column titles and all the words mixed, without showing which word belongs where.

## What Changes

- New item type `sort` with the payload `{columns: [{title, words}]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2), reported with the existing error codes.
- `SortItemType.studentView`: an allowlist projection with the titles in stored order and all words in one seeded shuffled list, with no column of a word. No endpoint uses it yet (#125).
- No migration, no new error code, no change to the `items` spec, no change to a shared class.

## Capabilities

### New Capabilities
- `item-sort`: R1 (a teacher adds and edits a sort item; the student view does not show which word is in which column) and R2 (limits and validation of the payload).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/sort/SortItemType`, `SortPayload` and `SortColumn`. No controller, service, repository or registry change.
- Tests: `items/api/SortItemTest` (R1-S1 to R1-S6), `items/api/SortPayloadTest` (R2-S1 to R2-S13), `items/domain/sort/SortItemTypeTest` (unit scenarios R1-S7 to R1-S10) and `items/support/SortPayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126) and the student read endpoint (#125).
- The editor for columns.
- A word that belongs to several columns, images and partial credit.
- Any change to the generic item rules, the item envelope or the other item types.
