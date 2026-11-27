# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. `theory`, `gap-fill` and `matching` are existing types. This change adds `reorder` in the same way.

## Goals / Non-Goals

**Goals:**
- A structured payload of entries, each a list of units in the correct order, in one of three modes, with limits.
- A student projection that shuffles the units and never shows the correct order.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, automatic splitting of sentences and partial credit.

## Decisions

**D1. Plug-in.** `items/domain/reorder/ReorderItemType` (`@Component`, code `reorder`, `payloadClass` `ReorderPayload`) and the records `ReorderPayload(String mode, List<ReorderEntry> entries)` and `ReorderEntry(List<String> units, String hint)`. No controller, service, repository or registry change. No Flyway migration, no new error code, no change to the `items` spec or to a shared class (Clarifications 1, 9).

**D2. Where each rule lives.**
- Bean Validation: `entries` is `@NotEmpty @Size(max = 25)` with `List<@Valid ReorderEntry>`; `units` is `@NotEmpty @Size(min = 2, max = 25)` with `List<String>`. An empty list breaks both `@NotEmpty` and `@Size(min)`; the field-error reduction keeps `REQUIRED`. `ReorderEntry.hint` and `ReorderPayload.mode` have no annotations.
- `extraChecks` (runs only when Bean Validation passed): `mode` missing, `null` or blank is `REQUIRED`, a value other than `words`, `sentences`, `letters` (`String.equals`) is `INVALID_FORMAT`, at `mode`; a `null` entry is `REQUIRED` at `entries[i]`; per unit: `null` or blank (`String.isBlank`) is `REQUIRED` at `entries[i].units[j]`, and, only when the mode is valid, longer than the limit of the mode (`String.length`; words 100, sentences 500, letters 4) is `TOO_LONG`; per entry, when no unit of it has its own error, fewer than 2 distinct units (`HashSet` of the units) is `INVALID_FORMAT` at `entries[i].units`; `hint`, when not `null`: blank is `REQUIRED`, longer than 100 is `TOO_LONG`, at `entries[i].hint`. The per-value rules are not container-element constraints, because Hibernate Validator would append a `<list element>` node to the path.
- Consequence, stated in R2: a payload that breaks a list size shows only that error; the other errors appear once it is fixed (R2-S9). The mode and the other `extraChecks` errors come together (R2-S10).
- Every list is bounded with `@Size`, there is no `Map` field, and the elements are `List<@Valid X>`.

**D3. Limits by mode.** Words are at most 100 and sentences (or paragraphs) at most 500 UTF-16 code units; letters at most 4, so a unit is a letter or a short group (a letter with a combining mark, an emoji, a digraph such as "ch"). Repeated units are allowed ("a", "b", "a"); only an entry with fewer than 2 distinct units is rejected. The comparison is exact: no trim, no case folding, a composed and a decomposed "é" differ. Accepted risk: invisible differences pass.

**D4. Student projection.** `studentView` returns an allowlist envelope `{"mode", "entries": [{"units", "hint"?}]}` built field by field, in this key order; `hint` is omitted when absent (not `null`). It throws `IllegalStateException` on corrupt data (`mode` `null` or unknown, `entries` `null`, a `null` entry, `units` `null`, a `null` unit), without the stored value in the message; stored payloads are always validated, so this only catches corrupt data.
- Per entry: copy the units, sort with `String.compareTo` (the sort makes the shuffle input canonical), `Collections.shuffle(list, new Random(seed))` with `seed = String.join("\u0000", units in stored order).hashCode()` (an `int` widened to `long`). If the result equals the stored order, `Collections.rotate(list, -1)`. With at least 2 distinct units a left rotation of a list equal to the stored order differs from it, so the shown order is never the correct one.
- The seed depends on the stored order, so reordering the units changes the order shown; every student and every call sees the same order for the same stored payload; #125 may re-shuffle.
- Not wired to any endpoint (#125). Tests: unit tests on the projection (R3-S1, R3-S2, R3-S3, R3-S5) and a serialisation test with the application's JSON mapper (R3-S4).

**D5. Limits and the body size.** The limits are the spec's own, not config properties: at most 25 entries of 25 units of at most 500 code units is 312 500 code units, fully escaped as `\uXXXX` (6 bytes) about 1.9 MB, over the 1 MB `request_body max_size` of Caddy on `/api/*` (infra/Caddyfile): the proxy stops such a request before the application sees it, which is accepted (a realistic sentences item is far smaller; a reply of the proxy is not part of the scenarios). A payload within the proxy limit is a small jsonb value. A part holds at most 50 items (items R1), so the unpaginated part list stays bounded.

**D6. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1 and R4 scenarios on other teachers), and R1-S6 checks reorder add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No unit or hint text is logged (constitution section 5).

## Risks / Trade-offs

- [The shuffled order is the same for every student and may be close to the correct one] -> Accepted; the shown order is never exactly the correct one, and #125 may re-shuffle.
- [Invisible differences pass the distinct-unit check: a trailing space, a composed against a decomposed letter] -> Accepted; values are raw, as in the other types.
- [With repeated units, what counts as the correct answer is not decided here] -> Left to answer checking (#126).
- [U+00A0 and U+200B are non-blank, so a unit of only those is accepted] -> Known limit, kept so one blank rule applies to every field (items R5).
- [A rollback after use leaves `reorder` rows with an unknown type] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version (see the last risk).
