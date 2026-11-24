# Design

## Context

See proposal.md for the motivation. Items have 7 fields (`id`, `type`, `payload`, `position`, `version`, `createdAt`, `updatedAt`). Create and edit validate their envelope by hand in `ItemController`, and `ApiException.fields` sorts the errors by field. Part titles use `@Title` on a required field (`CreatePartRequest` normalises in its compact constructor).

## Goals / Non-Goals

**Goals:**
- One optional `title` per item, for every type, with the part-title rule.
- No change to the order of checks except that the title joins the envelope step.

**Non-Goals:**
- A title inside any payload; frontend; generated numbers.

## Decisions

**D1. A column on `items`, not a payload field.** The title is the same for every type and is not governed by a type's payload rules. Alternative: a `title` in each payload record, rejected because every type would repeat it and R5-S5 (`title` in the theory payload is `UNKNOWN_FIELD`) stays true.

**D2. Optional title validation by normalise, then validate non-null.** `Normalize.name` runs first; null or blank becomes null; a non-null value is validated with the rules of `@Title` (length 1-200, a letter or digit, no control or format characters) through the existing validator (the `OptionalTitle` helper in `common`) and `FieldErrorCodes`; the helper's errors are thrown with `ApiException.fields` from the envelope step, so the codes equal those of part titles. `@Title` itself cannot go on the field because it includes `@NotBlank`. A shared helper keeps one rule for parts and items (the rule is not copied).

**D3. Envelope errors.** On create, the title errors are added to the `type` and `payload` envelope errors; on edit, to the `version` and `payload` presence errors. One sorted list, thrown before the item lookup (edit) and before the payload-by-type checks. A non-string title fails at binding with `INVALID_TYPE` as `type` does today (the first binding error wins): the 400 then carries only `title INVALID_TYPE` and hides the other envelope errors of the same body, and an earlier binding error hides a title error (Clarification 8).

**D4. Edit replaces the title with the payload.** `updatePayload` sets `payload`, `title`, `version = version + 1` and `updatedAt` in the one statement guarded by the stale-version check, returning the row. A missing or null title writes null (Clarification 9). The API semantics are "replace", but the SQL `UPDATE` writes `payload` and `title` only when the new value differs from the stored one (for example `payload = CASE WHEN payload IS DISTINCT FROM :payload THEN :payload ELSE payload END`, or a dynamic column list); `version` and `updated_at` are always written (Clarification 10).

**D5. Response.** `ItemResponse` has `title` after `type`, always serialised (no `NON_NULL`), so a client sees `title: null`. Create, list, edit and move share `ItemResponse.from`. Move does not touch `title`.

**D6. Repository.** `insertAtEnd`, `findInPart`, `setPosition`, `findByPart`, `updatePayload` and the `toItem` mapper select and return `title`.

**D7. Unpaired surrogates (Clarification 12).** `\p{Cs}` joins the rejected classes of the `@Title` pattern (`\p{Cc}\p{Cf}\p{Zl}\p{Zp}`), so a title with an unpaired surrogate is `INVALID_FORMAT` instead of being stored as "a?b". The pattern is shared, so part, course and lesson titles and item titles follow the one rule. Scenarios: items R1-S18 and parts R1-S14; course and lesson titles need none (same annotation, no separate rule; their title scenarios stay as they are).

## Risks / Trade-offs

- [A client that compares item JSON exactly breaks on the new key] → The frontend does not read items yet; the existing tests that assert exact keys change in this PR.
- [Two title rules drift apart] → One shared helper, and tests for the part and item codes.

## Migration Plan

Flyway `V19__item_title.sql`: `ALTER TABLE items ADD COLUMN title varchar(200);` (as `parts.title`) with no default and no backfill, so existing rows read as null. The column is nullable, so the old and new code can run during the deploy; rollback is the previous version.
