# Proposal

## Clarifications

Issue #225. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | New type or a mode of gap-fill? Where does the code live? | A new type `gap-fill-word-box`, not a mode of `gap-fill`: the change only adds files; gap-fill's payload and spec stay untouched. New self-contained package `items/domain/wordbox/` (`WordBoxItemType` `@Component`, `WordBoxPayload`, `WordBoxSegment`); no shared code with gap-fill and no change to any shared class; no `openspec/specs/items/spec.md` delta (branch independence with parallel item-type branches). No migration, no new error code. |
| 2 | Payload shape? | `{segments: [{type: "text", text} \| {type: "gap", answer: string}], extraWords: [string]}`. One accepted answer per gap is structural (`answer` is a string). `extraWords` is optional; missing or `null` means none. |
| 3 | Structure rules? | As gap-fill: at least one gap (`REQUIRED` at `payload.segments`); a null segment is `REQUIRED`; a missing or blank segment type is `REQUIRED`, an unknown type `INVALID_FORMAT` at `.type`; a text that is missing, `null` or "" is `REQUIRED`, whitespace-only text is allowed; two adjacent texts are `INVALID_FORMAT` on the second; `answer` on a text and `text` on a gap are `UNKNOWN_FIELD` at that path; the generic rules (items R2) apply. |
| 4 | Limits? | At most 51 segments and 25 gaps (`TOO_LONG` at `payload.segments`); a text at most 500 characters. `answer` is required, non-blank (`Character.isWhitespace` rule) and at most 100 characters. `extraWords` has at most 10 entries (`TOO_LONG` at `payload.extraWords`); each entry is non-blank (`REQUIRED`) and at most 100 characters (`TOO_LONG` at `payload.extraWords[i]`). Lengths are UTF-16 units. The word box holds at most 35 words. |
| 5 | Duplicates? | Compared by exact match, no trim and no case folding ("Go" and "go", "ё" and "е" differ). The same answer in two gaps is allowed and the box shows it twice. A duplicate inside `extraWords` is `INVALID_FORMAT` at the later index. An extra word equal to any gap's answer is `INVALID_FORMAT` at `extraWords[i]` (a wrong word that is also right would make checking ambiguous). |
| 6 | Storage? | Stored raw, as sent; the stored JSON drops a `null` `extraWords`. |
| 7 | Student view? | `studentView` is an allowlist `{"segments": [text {type, text} \| {type: "gap"}], "wordBox": [string]}`. Gaps carry nothing. `wordBox` holds every gap's answer (one per gap) plus the extra words, as one list, never marked which is extra. Order: sort by `String.compareTo`, then shuffle with `new java.util.Random(seed)`, where seed = `String.join("\u0000", sortedWords).hashCode()`: deterministic for the same words and independent of the gap order. The student endpoint (#125) may re-shuffle per request. Fixed key order (`type` first). Unit tests: the JSON has no `answer` and no `extraWords` key; the box is the multiset of answers and extras; the same payload gives the same order twice; a payload with the gaps reordered gives the same box. |
| 8 | Out of scope? | Answer checking (#126, #127), the student endpoint (#125), the editor, a reusable-word box mode and per-gap points. |

## Research angles

The engineer agreed on these conclusions from the code on `main`:

- **Plug-in point.** A `@Component` implementing `ItemType<P>` is collected by `ItemTypeRegistry` by injection; no controller, service or registry change, and the `items.type` column has no per-type check. `items/domain/wordbox/WordBoxItemType` mirrors `items/domain/gapfill/GapFillItemType`.
- **Validation pipeline.** `ItemPayloadValidator` does a strict Jackson mapping, then Bean Validation, then `ItemType.extraChecks` only if Bean Validation passed. `FieldErrorCodes` maps `@NotEmpty` to `REQUIRED` and `@Size` to `TOO_SHORT` or `TOO_LONG`.
- **Tests.** `ItemApiTest` helpers; a payload helper `items/support/WordBoxPayloads` (like `GapFillPayloads`); test files `items/api/WordBoxItemTest`, `items/api/WordBoxPayloadTest` and `items/domain/wordbox/WordBoxItemTypeTest`.

## Why

In plain gap-fill the student types a word. With a word box the student picks each gap's word from one shared list that also holds a few wrong words, which is easier for beginners and can be checked automatically. The items core can hold a registered type; the change adds `gap-fill-word-box`, with one answer per gap and optional extra words stored as structured JSON, and a student view that shows the shuffled box and never says which gap takes which word.

## What Changes

- New item type `gap-fill-word-box` with the payload `{segments: [{type: "text", text} | {type: "gap", answer}], extraWords: [...]}`, added and edited through the existing item endpoints (items R1, R4).
- Limits and per-type validation (design D2 to D4), reported with the existing error codes.
- `WordBoxItemType.studentView`: an allowlist projection with a deterministic shuffled word box and no answers. No endpoint uses it yet (#125).
- No migration, no new error code, no change to existing endpoints and no change to the `items` or `item-gap-fill` specs (Clarification 1).

## Capabilities

### New Capabilities
- `item-gap-fill-word-box`: R1 (a teacher adds and edits a word-box item; the student view hides which word goes where) and R2 (limits and validation of the payload).

### Modified Capabilities
None.

## Impact

- Backend: new `items/domain/wordbox/WordBoxItemType`, `WordBoxPayload` and `WordBoxSegment`. No controller, service, repository, registry or shared-class change.
- Tests: `items/api/WordBoxItemTest` (REST scenarios R1-S1 to R1-S7), `items/api/WordBoxPayloadTest` (REST scenarios R2-S1 to R2-S13), `items/domain/wordbox/WordBoxItemTypeTest` (unit scenarios R1-S8 to R1-S10) and the helper `items/support/WordBoxPayloads`.
- No new dependency, no migration, no new error code, no change to security configuration or Caddy.

## Out of scope

- Checking a student's answer (#126, #127), the student read endpoint (#125) and the editor.
- A box of reusable words (a word that may fill several gaps), and per-gap points.
- Any change to the generic item rules, the item envelope, gap-fill or the other item types.
