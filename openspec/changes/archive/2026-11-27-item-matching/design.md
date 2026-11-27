# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. `theory` and `gap-fill` are existing types. This change adds `matching` in the same way.

## Goals / Non-Goals

**Goals:**
- A structured payload of left and right text pairs, with limits and a no-duplicates rule.
- A student projection that shows both columns but not the pairing.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, images in pairs, many-to-one matching and partial credit.

## Decisions

**D1. Plug-in.** `items/domain/matching/MatchingItemType` (`@Component`, code `matching`, `payloadClass` `MatchingPayload`) and the records `MatchingPayload(List<MatchingPair> pairs)` and `MatchingPair(String left, String right)`. No controller, service, repository or registry change. No Flyway migration, no new error code, no change to the `items` spec or to a shared class (Clarifications 1, 7).

**D2. Where each rule lives.**
- Bean Validation on `MatchingPayload`: `pairs` is `@NotEmpty @Size(min = 2, max = 25)` with `List<@Valid MatchingPair>`. An empty list breaks both `@NotEmpty` and `@Size(min)`; the field-error reduction keeps `REQUIRED` for the field. A single pair gives `TOO_SHORT`, 26 pairs `TOO_LONG`, at `payload.pairs`. `MatchingPair` has no constraint annotations.
- `extraChecks` (runs only when Bean Validation passed): a `null` pair (`REQUIRED` at `pairs[i]`); for `left` and `right` of each pair: missing, `null`, empty or whitespace-only is `REQUIRED` (`String.isBlank`, which is `Character.isWhitespace`; U+00A0 and U+200B count as non-blank, the same rule as the other fields); more than 100 UTF-16 code units (`String.length`) is `TOO_LONG`; a repeat of an earlier value of the same side, by `String.equals`, is `INVALID_FORMAT` at the later `.left` or `.right`. A value with its own error is not added to the seen set and not compared. The per-value rules are not container-element constraints, because Hibernate Validator would append a `<list element>` node to the path.
- Consequence, stated in R2: a payload that breaks the pair-count limit shows only that error; the pair errors appear once it is fixed (R2-S10).
- Every list is bounded with `@Size`, there is no `Map` field, and the elements are `List<@Valid X>`.

**D3. Left and right may repeat across sides.** The duplicate rule is per side: a left equal to some right is allowed (a word and the same word in the other column, "go" and "go"), and a pair with equal `left` and `right` is allowed. The comparison is exact: "Go" and "go", "ё" and "е", a composed and a decomposed "é" are different values.

**D4. Student projection.** `studentView` returns a new envelope `{"left": [...], "right": [...]}`, built field by field (an allowlist, fixed key order `left` then `right`); no pair index and no pairing field. It throws `IllegalStateException` on corrupt data (`pairs` `null`, a `null` pair, a `null` left or right), without the stored value in the message; stored payloads are always validated, so this only catches corrupt data.
- `left`: the left values in stored order.
- `right`: the right values copied into a list, sorted with `String.compareTo` (natural order), then `Collections.shuffle(list, new Random(seed))` with `seed = String.join("\u0000", sortedLefts + sortedRights).hashCode()` (an `int` widened to `long`), each side sorted with `String.compareTo`. The seed is a function of the two sets of values only, not of which right belongs to which left, so every pairing of the same values gives the same view and a student gains nothing by trying pairings (Clarification 7). Every student and every call sees the same order for the same values; an edit that changes a value can change it.
- Rotate rule: if the shuffled list equals the rights in stored order (every right next to its own left) and there are at least 2 pairs, `Collections.rotate(list, -1)` is applied. With distinct rights the result then never shows the stored pairing (R1-S12). The rights of one item are distinct because R2 rejects exact repeats on a side.
- Not wired to any endpoint (#125). Tests: unit tests on the projection (R1-S8, R1-S9, R1-S11) and a serialisation test with the application's JSON mapper (R1-S10).

**D5. Limits and the body size.** The limits are the spec's own, not config properties: 25 pairs of at most 100 characters per side is 5000 UTF-16 code units. Fully escaped as `\uXXXX` (6 bytes) that is 30 KB, plus about 1 KB of structure: far under the 1 MB `request_body max_size` of Caddy on `/api/*` (infra/Caddyfile). The payload is a small jsonb value. A part holds at most 50 items (items R1), so the unpaginated part list adds at most about 1.6 MB for matching items in the worst case; accepted as for gap-fill.

**D6. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1 and R4 scenarios on other teachers), and R1-S7 checks matching add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No pair text is logged (constitution section 5).

## Risks / Trade-offs

- [A student recovers the pairing by trying pairings and recomputing the seed] -> Gone: the seed does not depend on the pairing (D4, Clarification 7). What remains: the rotate rule excludes the stored pairing from the views, which tells a student one pairing that is wrong when the view is rotated; accepted. The same order for every student is a known limit; #125 may re-shuffle.
- [Invisible differences pass the duplicate check: a trailing space, a composed against a decomposed letter] -> Accepted (Clarification 5); values are raw, as in the other types.
- [Two lefts may differ only invisibly, so a student sees two identical-looking items] -> Same as above.
- [U+00A0 and U+200B are non-blank, so a value of only those is accepted] -> Known limit, kept so one blank rule applies to every field (items R5).
- [A rollback after use leaves `matching` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version (see the last risk).
