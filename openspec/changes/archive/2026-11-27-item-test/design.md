# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `test` in the same way as `gap-fill`, in its own package and with no shared code.

## Goals / Non-Goals

**Goals:**
- A structured payload of questions with options and correct marks, with limits.
- A student projection that cannot leak which options are correct: `multiple` is a teacher-authored setting, never computed from the answer key.

**Non-Goals:**
- A timer, points per answer, answer checking (#126), the student endpoint (#125), the editor, shuffling options and images in options.

## Decisions

**D1. Plug-in and names.** `items/domain/choice/TestItemType` (`@Component`, code `test`, `payloadClass` `TestPayload`) and the records `TestPayload(List<TestQuestion> questions)`, `TestQuestion(String text, Boolean multiple, List<TestOption> options)` and `TestOption(String text, Boolean correct)`. The package is `choice`, not `test` (Clarification 9). No controller, service, repository or registry change; the only shared change is the new `StrictBooleanJsonConfig` (D2a); no migration, no new error code, no `items` spec delta.

**D2. Where each rule lives.**
- Bean Validation on the records: `questions` is `@NotEmpty @Size(max = 25)` with `List<@Valid TestQuestion>`; question `text` is `@NotBlank @Size(max = 500)`; `options` is `@NotEmpty @Size(min = 2, max = 10)` with `List<@Valid TestOption>`; option `text` is `@NotBlank @Size(max = 100)`; `correct` is `@NotNull`. An empty list gives `REQUIRED` and one option `TOO_SHORT` because `REQUIRED` has the highest priority when two codes meet at one field. `@NotBlank` is the `Character.isWhitespace` rule of items R5, so U+00A0 and U+200B are not blank: a known limit kept so one rule applies to every field. Length is `String.length` (UTF-16 units).
- `extraChecks` (runs only when Bean Validation passed): a `null` question or option (`REQUIRED` at `questions[i]` and `questions[i].options[j]`), a repeated option text in one question (`INVALID_FORMAT` at the later `.text`) and, when `multiple` is not `true`, more than one correct option (`INVALID_FORMAT` at `questions[i].options`). A `null` element is not a container-element constraint, because Hibernate Validator appends a `<list element>` node to such paths and the contract paths are `payload.questions[i]` and `payload.questions[i].options[j]`. `ItemPayloadValidator` adds the `payload.` prefix; `FieldErrorCodes.reduce` keeps one code per field, sorted by field with list indexes compared as numbers.
- The strict mapper turns an unknown property into `UNKNOWN_FIELD` (reported as `payload`, so `shuffle`, `timer` and `points` are unknown; `multiple` is a known field since review round 2) and a wrong JSON type into `INVALID_TYPE` with the path. Consequence, stated in R2 and pinned in R2-S11: a payload that breaks a Bean Validation rule (a limit, a blank text, a missing `correct`) shows only those errors; the structure errors (null elements, duplicates, the correct count) appear after they are fixed.

**D2a. Strict `Boolean`.** A new `common/StrictBooleanJsonConfig` (`@Configuration`, a `JsonMapperBuilderCustomizer` like `StrictStringJsonConfig`) sets the coercion of `Boolean` and `boolean` from String, Integer and Float (and the empty string) to `Fail`, so only JSON `true` and `false` bind and everything else is `INVALID_TYPE` at the path; `null` stays `REQUIRED` through `@NotNull`. It applies to every `Boolean` or `boolean` bound by these mappers (Clarification 10), not only to `correct`; `multiple` is bound the same way (a wrong type is `INVALID_TYPE` at `.multiple`). Before the config, the mapper took `"true"`, `"TRUE"`, `"false"`, `1`, `0` and any other integer (true). It is specified in R2-S12, not as an `items` delta (PR #238 modifies items R2). Pass 2 checks with grep that no existing request DTO or payload record relies on Boolean coercion, and a unit test of the config sits in `common/`.

**D3. Duplicates.** Option texts of one question are compared by exact string equality: no trim and no case folding ("Go" and "go", "ё" and "е" differ, "go" and "go " differ). Each later repeat is reported at its own `.text` (a third copy gives two errors). The same text in two questions is allowed. A `null` option is not compared. The correct marks are not compared: two equal texts are rejected whatever their marks.

**D4. `multiple` is a teacher setting; no-correct and all-correct accepted (review round 2).** `TestQuestion.multiple` is an optional `Boolean` with no `@NotNull`: missing or `null` means `false`, the same pattern as `allowNotStated` in item-true-false. It is stored as sent: when not sent it is not stored (R1-S1), and a `null` is dropped (the stored mapper is NON_NULL, as `allowNotStated` in #243). Round 1 had no `multiple` and computed it from the count of correct options; that made the student view depend on the answer key (with two options, `multiple: true` revealed both are correct), so the view now reads the stored setting only. In `extraChecks`, after Bean Validation, a question with `multiple` not `true` and more than one correct option gives `INVALID_FORMAT` at `questions[i].options` (R2-S14); with `multiple` `true` any number is allowed. A question with no correct option is allowed in both modes and stored as sent; the answers work (#126) decides whether it is checked by the teacher or is a poll. A question whose options are all correct needs `multiple` `true` when it has two or more options.

**D5. Student projection.** `studentView` builds a new envelope `{"questions": [...]}` from an allowlist: each question becomes `{text, multiple, options: [{text}]}` with a fixed key order (`@JsonPropertyOrder`), `multiple` is `true` only when the stored `multiple` is `true`, else `false` (absent, `null` or `false`); it never reads `correct`, so two payloads that differ only in their answer keys give equal views (R3-S6). Options keep the stored order. The method never copies a record or a field from the stored data, so a future field does not leak by default. A `null` payload (a stored JSON `null`), a `null` `questions` list, a `null` question, text, options, option, option text or `correct` throws `IllegalStateException` (stored payloads are validated, so this only catches corrupt data); the message names no stored value. Tests: the unit tests on the projection (R3-S2, R3-S3, R3-S5, R3-S6) and a serialisation test with the application's JSON mapper (R3-S4).

**D6. Limits and the body size.** Worst case of one request: 25 questions of 500 UTF-16 units plus 10 options of 100 each (1000 units) is 37500 units; fully escaped as `\uXXXX` (6 bytes each) that is about 225 KB, plus about 3 KB of structure: about 230 KB, under the 1 MB `request_body max_size` of Caddy on `/api/*`. A part holds at most 50 items, and the unpaginated part list returns every payload, so the worst case is a few MB per list: accepted for now, as for gap-fill.

**D7. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1-S6, R1-S7 and the edit equivalents); R1-S5 checks the add and the edit of another teacher. Content is plain Unicode text; scenarios use English and Russian (constitution section 9) and no language is hard-coded. No question or option is logged (constitution section 5).

## Risks / Trade-offs

- [`multiple` says something about the key] -> `multiple: false` tells the student at most one option is correct; that is the teacher's chosen mode, not the key. `multiple: true` says nothing about how many options are correct (one, several or none).
- [Stored order can reveal the answer: a teacher who always puts the correct option first] -> Accepted (Clarification 7): options are not shuffled, a teacher may want "all of the above" last; shuffling is out of scope.
- [A future field on `TestQuestion` or `TestOption` leaks to students] -> The projection is an allowlist built field by field (D5), with a serialisation test.
- [Making `Boolean` strict (D2a) changes every `Boolean` bound by these mappers] -> Accepted (Clarification 10); pass 2 greps for DTOs and payloads that relied on coercion, and the config has its own unit test.
- [Exact match hides differences a teacher cannot see: "go" and "go " look equal but are different options] -> Accepted, consistent with gap-fill's raw rule; an editor may warn.
- [Question text is plain text, so markup is shown as typed (R1-S2)] -> The frontend must render it as text, not as HTML.
- [A rollback after use leaves `test` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert.

## Migration Plan

None: no Flyway migration, and no stored `test` items exist yet (no teachers before M2), so no round-1 payloads need migrating. Existing items are unchanged and the new type is additive; rollback is the previous version.
