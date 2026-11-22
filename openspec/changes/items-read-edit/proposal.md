# Proposal

## Clarifications

Issue #111. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | List endpoint? | `GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` returns 200 with an unpaginated JSON array ordered by `position`, each element with exactly the create response fields `{id, type, payload, position, version, createdAt, updatedAt}`; an empty part gives `[]`; only that part's items. A foreign, unknown, malformed or wrong-parent id gives 404 `NOT_FOUND`. Checks: role 403 (no CSRF on a GET), deactivated 401, malformed id 404, unknown or foreign 404. Read-only transaction, no write locks (as parts R2). |
| 2 | Edit endpoint? | `PATCH .../parts/{partId}/items/{itemId}` with `{version, payload}` returns 200 with the updated item. The payload is validated by the item's stored type with all R2 rules (paths `payload.<path>`, `UNKNOWN_FIELD`, `INVALID_FORMAT` and so on); the stored payload is replaced by the serialised record without null fields. `type` in the body is ignored like any other extra body field (the type is immutable; R2-S11 convention); the response shows the stored type. |
| 3 | `version` input? | Required, a strict JSON integer (`StrictIntegerJsonConfig`, as parts move does for `position`): missing or null gives 400 `REQUIRED`, a string, float or boolean gives 400 `INVALID_TYPE`. A missing or null payload gives `REQUIRED`, a non-object `INVALID_TYPE`, as in R2. |
| 4 | Version mismatch? | A `version` different from the stored one gives 409 `VERSION_CONFLICT` with no `errors` property and nothing changes. The `api-errors` spec needs no delta (see 18). |
| 5 | What a success changes? | `version` plus one and `updatedAt` = now, even if the payload is unchanged; `position`, `createdAt` and `type` are unchanged; the other items and the `updatedAt` of the part, the lesson and the course are unchanged. |
| 6 | Order of edit checks and locks? | CSRF and role 403, deactivated 401, malformed id 404, envelope input 400 (`version`, `payload` presence and shape), unknown or foreign id 404, payload by type 400, version 409. Lock chain as create: user row, course, lesson, part, then the item row `for update`. |
| 7 | Races? | Parallel edits with the same version: exactly one 200, the others 409, the version ends at old plus one. An edit racing a part delete: 200 or 404, never 500. |
| 8 | "Edit keeps old results"? | No answers or results table exists yet (#126), so the spec says nothing about results. The edit is in place (one `update` of the item row) and touches no other table. |
| 9 | Theory type? | The first real type in main sources, code `theory`, payload `{"text": string}`. `text` is required and not blank (blank or whitespace gives `payload.text` with the code the project already uses for a blank required string), at most 20000 characters (Java `String` length, as `@Size`), `TOO_LONG` above. Stored as sent: no trimming, raw HTML kept as text (rendering with raw HTML off and `$...$` KaTeX is the frontend's job, #114). `studentView` returns the payload unchanged. Scenarios: add `en` and `ru` theory with Markdown and `$x^2$` kept exactly, a text with `<script>` stored as text, 20000 accepted and 20001 rejected (Cyrillic), blank rejected, an unknown property rejected, an edit of a theory item validated by the theory rules. |
| 10 | Limits? | Items per part 50 (existing, `teachdash.items.max-per-part`); theory text 20000 characters; items have no title. |
| 11 | Requirement IDs? | `items` R3 to R5 only. |
| 12 | R4-S10 outcome? (spec critique) | Each edit response is 200, 404 or 409, at most one is 200, none is 500; the part is gone and no item row references it. |
| 13 | What R4 says about a success? | A success sets `updatedAt` to now even if the payload is unchanged; `position`, `createdAt` and `type` stay; the other items and the `updatedAt` of the part, lesson and course stay. |
| 14 | 409 body and 400 kinds? | The 409 body carries no stored payload. R4 names the two 400 kinds in check order: envelope (before the 404), payload by type (after it, before the version 409). R4-S1 also checks that listing the part shows the new payload, same position, `version` 2. |
| 15 | R3 and R4-S3 wording? | "The read takes no write locks" leaves R3's text (kept in the design). R4-S3 gets a `version` of 99999999999999999999 with `INVALID_TYPE` (D2 claim kept); R4-S3 is not split. |
| 16 | Stored type not registered on edit? | Design only: not reachable, types are never removed. No scenario and nothing in R4. |
| 17 | R5 wording? | "Characters" means UTF-16 code units (Java `String.length`, as `@Size`); the length scenario adds 10001 emoji (20002 units) giving `TOO_LONG`. Blank is `Character.isWhitespace` (as `@NotBlank`); R5-S5 adds a text of only NBSP (U+00A0), accepted and stored as sent. The "studentView returns the payload unchanged" claim moves from R5 to the design. |
| 18 | `api-errors` delta? | Dropped entirely: R2 already says any other code has no `errors`. |
| 19 | List and the registry? | Design notes that the list does not need the registry (it returns the stored type and payload as is). |

Research: the brief carried no separate research angles. One fact was checked in the code for decision 9: `@NotBlank` and `@NotNull` both give the field code `REQUIRED` (`FieldErrorCodes.codeOf`), as for a blank course or part title (parts R1-S5), so a blank theory `text` is `payload.text` `REQUIRED`.

## Why

An item can be added to a part (#110) but not read back or corrected, and no real type exists, so a teacher cannot yet build lesson content. This change adds the list, the edit with an optimistic version, and the first real type, `theory`, so the frontend (#114) and the exercise types can build on them.

## What Changes

- `GET .../parts/{partId}/items` lists a part's items by position (R3).
- `PATCH .../parts/{partId}/items/{itemId}` replaces the payload under an optimistic `version`, 409 `VERSION_CONFLICT` on a mismatch (R4).
- The `theory` item type: Markdown text of at most 20000 characters, stored as sent (R5).
- New response code `409 VERSION_CONFLICT`, proven by R4-S5; `api-errors` R2 already covers a code with no `errors`.
- No migration: the `items` table already has `version` and `updated_at`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `items`: R3 list, R4 edit, R5 `theory` type (ADDED).

## Out of scope

- Delete and reorder of items (#112), student endpoints and the student projection endpoint, an endpoint that lists item types.
- Answers and results (#126, #128): the edit is in place and touches no other table, so nothing is said about results here.
- Rendering Markdown, raw HTML handling and KaTeX in the browser (frontend, #114); the backend stores and returns the text as sent.
- Files, links and further item types; a title of an item.
- Pagination of the list (a part has at most 50 items).
- Frontend and OpenAPI.

## Impact

- Backend: `items` package gets `GET` and `PATCH` in `ItemController`, `ItemService.list` and `edit`, new `ItemRepository` queries, `EditItemRequest(Long version, Map<String,Object> payload)`; new `theory` type (`TheoryItemType`, `TheoryPayload`) registered as a Spring bean, so the production registry is no longer empty.
- API: two new endpoints, one new response code (`VERSION_CONFLICT`), one new item type code.
- Database: no migration.
- Limits: 50 items per part (existing), 20000 characters of theory text, no title.
- No new dependencies.
