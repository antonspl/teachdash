# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. `theory`, `gap-fill` and `matching` are existing types. This change adds `reorder` in the same way.

## Goals / Non-Goals

**Goals:**
- A structured payload of entries, each a list of units in the correct order, in one of three modes, with limits.
- A student projection that shuffles the units in an order that does not depend on the stored order.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, automatic splitting of sentences and partial credit.

## Decisions

**D1. Plug-in.** `items/domain/reorder/ReorderItemType` (`@Component`, code `reorder`, `payloadClass` `ReorderPayload`) and the records `ReorderPayload(String mode, List<ReorderEntry> entries)` and `ReorderEntry(List<String> units, String hint)`. No controller, service, repository or registry change. No Flyway migration, no new error code, no change to the `items` spec or to a shared class (Clarifications 1, 9).

**D2. Where each rule lives.**
- Bean Validation: `entries` is `@NotEmpty @Size(max = 25)` with `List<@Valid ReorderEntry>`; `units` is `@NotEmpty @Size(min = 2, max = 25)` with `List<String>`. An empty list breaks both `@NotEmpty` and `@Size(min)`; the field-error reduction keeps `REQUIRED`. `ReorderEntry.hint` and `ReorderPayload.mode` have no annotations.
- `extraChecks` (runs only when Bean Validation passed): `mode` missing, `null` or blank is `REQUIRED`, a value other than `words`, `sentences`, `letters` (`String.equals`) is `INVALID_FORMAT`, at `mode`; a `null` entry is `REQUIRED` at `entries[i]`; per unit: `null` or blank (`String.isBlank`) is `REQUIRED` at `entries[i].units[j]`, and, only when the mode is valid, longer than the limit of the mode (`String.length`; words 100, sentences 500, letters 4) is `TOO_LONG`; over 200 units in all entries is `TOO_LONG` at `entries`; per entry, when no unit of it has its own error, fewer than 2 distinct units (`HashSet` of the units) is `INVALID_FORMAT` at `entries[i].units`; `hint`, when not `null`: blank is `REQUIRED`, longer than 100 is `TOO_LONG`, at `entries[i].hint`. The per-value rules are not container-element constraints, because Hibernate Validator would append a `<list element>` node to the path.
- Consequence, stated in R2: a payload that breaks a list size shows only that error; the other errors appear once it is fixed (R2-S9). The mode and the other `extraChecks` errors come together (R2-S10).
- Every list is bounded with `@Size`, there is no `Map` field, and the elements are `List<@Valid X>`.

**D3. Limits by mode.** Words are at most 100 and sentences (or paragraphs) at most 500 UTF-16 code units; letters at most 4, so a unit is a letter or a short group (a letter with a combining mark, a short emoji, a digraph such as "ch"). The limit counts UTF-16 code units like every other limit, not graphemes: an emoji such as "🙂" (2 units) fits, a ZWJ sequence such as "👩‍🏫" (5 units) is `TOO_LONG`. Repeated units are allowed ("a", "b", "a"); only an entry with fewer than 2 distinct units is rejected. The comparison is exact: no trim, no case folding, a composed and a decomposed "é" differ. Accepted risk: invisible differences pass.

**D4. Student projection.** `studentView` returns an allowlist envelope `{"mode", "entries": [{"units", "hint"?}]}` built field by field, in this key order; `hint` is omitted when absent (not `null`). It throws `IllegalStateException` on corrupt data (`mode` `null` or unknown, `entries` `null`, a `null` entry, `units` `null` or empty, a `null` unit, fewer than 2 distinct units in an entry), without the stored value in the message; stored payloads are always validated, so this only catches corrupt data.
- Per entry: copy the units, sort with `String.compareTo`, then `Collections.shuffle(list, new Random(seed))` with `seed = String.join("\u0000", sorted units).hashCode()` (an `int` widened to `long`). The seed is taken from the sorted units, so the view is the same for every stored order of the same units and tells nothing about the answer. There is no rotate: with a computable shuffle, rotating only when the shuffle matches the correct order would reveal the order (a view that differs from the computed shuffle would be the answer). A chance match of the correct order is accepted; the student cannot tell it from any other view.
- The shuffle is a seeded hash of the sorted units, not a secret, but the view is independent of the stored order: reordering the stored units gives the same view. Every student and every call sees the same order for the same units; #125 may re-shuffle.
- Not wired to any endpoint (#125). Tests: unit tests on the projection (R3-S1, R3-S2, R3-S3, R3-S5) and a serialisation test with the application's JSON mapper (R3-S4).

**D5. Limits and the body size.** The limits are the spec's own, not config properties: at most 25 entries of 25 units, at most 200 units in all entries (R2), each unit at most 500 UTF-16 code units. The worst case is 200 units of 500 characters, fully escaped as `\uXXXX` (6 bytes): about 600 KB plus structure, under the 1 MB `request_body max_size` of Caddy on `/api/*` (infra/Caddyfile). A part holds at most 50 items (items R1), so the unpaginated part list stays bounded.

**D6. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1 and R4 scenarios on other teachers), and R1-S6 checks reorder add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No unit or hint text is logged (constitution section 5).

## Risks / Trade-offs

- [The shuffled order is the same for every student and may equal or be close to the correct one] -> Accepted; a chance match is possible (for example two units) and the student cannot tell; #125 may re-shuffle.
- [Invisible differences pass the distinct-unit check: a trailing space, a composed against a decomposed letter] -> Accepted; values are raw, as in the other types.
- [With repeated units, what counts as the correct answer is not decided here] -> Left to answer checking (#126).
- [U+00A0 and U+200B are non-blank, so a unit of only those is accepted] -> Known limit, kept so one blank rule applies to every field (items R5).
- [A rollback after use leaves `reorder` rows with an unknown type] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version (see the last risk).
