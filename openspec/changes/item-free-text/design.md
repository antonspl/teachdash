# Design

## Context

See proposal.md. Items hold a registered type (`ItemType<P>`, collected by `ItemTypeRegistry`); `ItemPayloadValidator` reads the payload map into the type's record with a strict Jackson mapper, runs Bean Validation, and calls `extraChecks` only when Bean Validation passed. This change adds `free-text` in the same way as `theory` and `gap-fill`.

## Goals / Non-Goals

**Goals:**
- A payload with a Markdown prompt and optional answer length limits, with limits of its own.
- A student projection of the prompt and the limits.

**Non-Goals:**
- Answer submission, the teacher verdict and the result states (#126, #129), the student endpoint (#125), the editor, word-count limits and a rubric.

## Decisions

**D1. Plug-in.** `items/domain/freetext/FreeTextItemType` (`@Component`, code `free-text`, `payloadClass` `FreeTextPayload`) and the record `FreeTextPayload(String prompt, Integer minLength, Integer maxLength)`. The package is self-contained: no shared class changes, so the parallel item-type branches stay independent. No controller, service, repository or registry change; the `items.type` column has no per-type check. No Flyway migration, no new error code, no delta of the `items` spec (Clarification 1).

**D2. Where each rule lives.**
- Bean Validation on the record: `prompt` is `@NotBlank @Size(max = 20000)` (`REQUIRED`, `TOO_LONG`; blank means `Character.isWhitespace` only, as items R5, so U+00A0 is accepted), `minLength` and `maxLength` are `@Min(1) @Max(10000)` on `Integer` (a null passes both). `FieldErrorCodes` maps `@Min` and `@Max` to `INVALID_FORMAT` (the default branch); the implementer confirms this in a test. A wrong JSON type (string, float, boolean in a limit; a number or list as `prompt`) is `INVALID_TYPE` from the strict mapper and `StrictIntegerJsonConfig`; an unknown property is `UNKNOWN_FIELD` at `payload`.
- `extraChecks` (runs only when Bean Validation passed): `minLength` and `maxLength` both present and `minLength > maxLength` gives `maxLength` `INVALID_FORMAT`; the prefix `payload.` is added by `ItemPayloadValidator`. Equal limits are allowed. Consequence, stated in R2: when any Bean Validation error exists (for example `prompt` blank), the order error is not reported until the others are fixed (R2-S7).
- The prompt limit is the theory text limit (items R5): one rule per field kind. Lengths are UTF-16 code units (`String.length`) for the prompt; for the limits the unit is the same, applied to the answer later (#126, #129).

**D3. The limits are not checked here against any answer.** The payload only stores `minLength` and `maxLength`; no answer exists in this change. The value 1 for `minLength` means "not empty"; a missing limit means no limit. The numbers are the spec's own, not config properties.

**D4. Student projection.** `studentView` builds a new record `FreeTextStudentView(String prompt, Integer minLength, Integer maxLength)` annotated `@JsonInclude(NON_NULL)`, copying the three fields explicitly (an allowlist: a future field of `FreeTextPayload` does not leak by default). The application's JSON mapper includes nulls (only the stored-payload mapper drops them), so the annotation is what removes an absent limit from the JSON. The payload holds no secret, so nothing is hidden today; the allowlist keeps it so. The stored payload is validated, so `studentView` has nothing to reject and does not throw. It is not wired to any endpoint (#125). Tests: a unit test on the projection (R1-S7) and a serialisation test with the application's JSON mapper on the exact keys, in English and Russian (R1-S8). There is no REST scenario until #125.

**D5. Limits and the body size.** The largest payload is a 20000-code-unit prompt and two small integers. In UTF-8 that is about 40 KB for Cyrillic (2 bytes per unit) and 60 KB at 3 bytes, and at most about 120 KB if every unit is written as a `\uXXXX` escape: well under the 1 MB `request_body max_size` of Caddy on `/api/*`. The part list is unpaginated, and a part holds at most 50 items (items R1), so the worst list is about 3 MB; accepted, as for `theory`.

**D6. Teacher scoping and languages.** Unchanged: every query on items is scoped by the current teacher (constitution section 8; items R1-S6, R1-S7 and the edit equivalents), and R1-S6 here checks the free-text add and edit with another teacher. The prompt is plain Unicode text stored raw; scenarios use English and Russian (constitution section 9), and no language is hard-coded. The prompt is not logged (constitution section 5).

## Risks / Trade-offs

- [The limits are only stored here, so nothing enforces them until the answers work] -> Accepted (Clarification 2, 6); #126 and #129 apply them.
- [Limits count UTF-16 code units, so an emoji counts as 2] -> Accepted, the same unit as every other length in the product (items R5).
- [A rollback after use leaves `free-text` rows with an unknown type, and there is no tooling yet to find or clean them] -> Accepted; the rows stay inert, and cleaning them is a manual database task.
- [A future field on `FreeTextPayload` leaks to students] -> The projection is an allowlist (D4), with a serialisation test on the exact keys.

## Migration Plan

None: no Flyway migration. The new type is additive; rollback is the previous version (see Risks).
