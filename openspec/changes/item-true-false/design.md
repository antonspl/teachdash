# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `true-false` in the same way as `gap-fill`, as its own type (Clarification 1).

## Goals / Non-Goals

**Goals:**
- A structured payload of statements with a verdict each, an optional "not stated" option, and limits.
- A student projection that cannot leak a verdict.

**Non-Goals:**
- Answer checking (#126), the student endpoint (#125), the editor, explanations per statement and shuffling.

## Decisions

**D1. Plug-in.** `items/domain/truefalse/TrueFalseItemType` (`@Component`, code `true-false`, `payloadClass` `TrueFalsePayload`) and the records `TrueFalsePayload(Boolean allowNotStated, List<TrueFalseStatement> statements)` and `TrueFalseStatement(String text, String answer)`. `answer` is a `String`, not an enum: an enum would turn a wrong value into a Jackson error and the code could not be `INVALID_FORMAT` (Clarification 5). No controller, service, repository or registry change; no Flyway migration; no new error code. Nothing is shared with item-test (Clarification 1).

**D2. Where each rule lives.**
- Bean Validation on the records: `statements` is `@NotEmpty @Size(max = 25)` with `List<@Valid TrueFalseStatement>`; `text` is `@NotBlank @Size(max = 500)`; `answer` is `@NotBlank`. This gives `REQUIRED` and `TOO_LONG` at `payload.statements`, `payload.statements[i].text` and `payload.statements[i].answer`. Blank means `Character.isWhitespace` only (as items R5); length is `String.length` (UTF-16 code units). No `Map` field.
- `extraChecks` (runs only when Bean Validation passed): a null statement (`REQUIRED` at `statements[i]`; a `@NotNull` on the element would add a `<list element>` node to the path), an exact duplicate `text` (`INVALID_FORMAT` at the later `statements[i].text`), an `answer` other than `true`, `false` or `not-stated`, compared case-sensitively with no trim (`INVALID_FORMAT` at `statements[i].answer`), and `not-stated` when `allowNotStated` is not `true` (`INVALID_FORMAT` at the same field). `ItemPayloadValidator` adds the `payload.` prefix.
- Consequence, stated in R2-S10: a payload that breaks a Bean Validation rule shows only those errors; the `extraChecks` errors appear after they are fixed, and all `extraChecks` errors come together, sorted by field.
- A wrong JSON type is `INVALID_TYPE` with the path. The strict string configuration already rejects a number or boolean for `text` and `answer`. `allowNotStated` is a `Boolean`; strictness (only JSON booleans) comes from PR #242 (Clarification 8). Until it merges, Jackson's default coercion also accepts "true", `1` and `0`; the scenarios use only values that fail either way ("yes", an array).

**D3. Student projection.** `studentView` builds the envelope `{"allowNotStated": boolean, "statements": [{"text"}]}` from an allowlist, with a fixed key order (`allowNotStated` first): `allowNotStated` is `Boolean.TRUE.equals(...)`, each statement is a view record with `text` only, in stored order. It never copies a field from the stored record, so a future field does not leak by default. It throws `IllegalStateException` on corrupt data (null `statements`, a null statement, a null `text`), with a message that holds no stored value (stored payloads are always validated, so this only catches corruption). It does not read `answer`. It is not wired to any endpoint (#125). Tests: a unit test of the projection (R1-S7), a serialisation test with the application's JSON mapper that no verdict and no `answer` name appears (R1-S8), and a corrupt-data test (R1-S9).

**D4. Limits and the body size.** The limits (25 statements, 500 characters per text) are the spec's own, not config properties. Worst case of one request: 25 x 500 = 12500 UTF-16 code units of text; fully written as `\uXXXX` escapes (6 bytes) that is about 75 KB, plus about 2 KB of structure, far under the 1 MB `request_body max_size` of Caddy on `/api/*`. The worst stored item is about 25 KB (Cyrillic) to 38 KB (3 bytes per character).

**D5. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1, R4); R1-S6 checks add and edit with another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No statement text is logged (constitution section 5). The payload has no integer field, so the out-of-range JSON integer issue (items, PR #238) does not apply.

## Risks / Trade-offs

- [A statement has no stable id] -> Accepted: #126 keys answers by (student, item) with a snapshot; the consumers key by index.
- [Texts are raw, so "A" and "A " are different statements] -> Accepted (Clarification 4): duplicates are rejected on an exact match only.
- [A future field on `TrueFalseStatement` leaks to students] -> The projection is an allowlist built field by field (D3), with a serialisation test.
- [The unpaginated part list returns every item with its full payload: a part holds at most 50 items (items R1) and a true-false item is at most about 38 KB, so the worst list is about 1.3 to 1.9 MB] -> Accepted for now; pagination or a lighter list is a separate change.
- [A rollback after use leaves `true-false` rows with an unknown type, and there is no tooling yet to clean them] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration. Existing items are unchanged and the new type is additive; rollback is the previous version.
