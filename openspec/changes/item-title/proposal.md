# Proposal

## Clarifications

Issue #191. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Where does the title live? | A new nullable column `title` on `items` (the item envelope, not the payload), so it works for every type. JSON property `title`. Migration `V19__item_title.sql`. |
| 2 | Validation? | Optional. The same rule as part titles (`@Title`, `Normalize.name`): normalise first; null, or blank after normalisation, is stored as null; any other value must pass the part-title rule (1-200 characters, at least one letter or digit, no control or format characters), else `TOO_LONG` or `INVALID_FORMAT`; a non-string (number, boolean, array, object) gives `INVALID_TYPE`. A title like "!!!" is not blank after normalisation, so it is `INVALID_FORMAT`, not null. |
| 3 | Where is it set and returned? | Set on create (`POST .../items`) and on edit (`PATCH .../items/{itemId}`). Returned by create, list and edit, and by move (R6 says the response is "as in R1", so it follows). |
| 4 | Edit semantics? | Edit replaces `payload` and `title` together, like the payload ("replaced, not merged"): a missing or null `title` clears it. `version` grows by 1. |
| 5 | Order of checks (2026-11-24)? | Title validation is part of the envelope 400. On edit it is checked with the `version` and `payload` presence checks, before the item 404, and its errors come in the same list sorted by field. On create it joins the `type` and `payload` envelope errors, before the payload-by-type checks. |
| 6 | Out of scope? | Showing the title in the frontend. Storing numbers like "Упражнение 1": the client derives them from type and position. |
| 7 | Languages (constitution section 9)? | Scenarios use a Russian title ("Немного теории") and an English one. |
| 8 | Binding errors? | A non-string `title` fails at JSON binding, and the 400 carries only `title INVALID_TYPE`, hiding other envelope errors (a missing payload, type or version) in the same body. This is the current API behaviour, stated as is. The same holds the other way: an earlier binding error hides a title error. Said in R1 and R4, with one scenario on create and one on edit. |
| 9 | Clear on missing? | Accepted: a PATCH with only `version` and `payload` clears the title; a future frontend always sends the current title back. Decision 4 stays. R4's title becomes "edits an item's payload and title". |
| 10 | Which columns does the edit write? | API semantics stay "replace" (decision 4), but the edit's Postgres UPDATE writes `payload` and `title` only when their value differs from the stored one; `version` and `updated_at` are always written. Design only (D4 and a task); no scenario. |
| 11 | `title: null` in the JSON? | Always serialised when unset (kept). The change is client-visible (a new `title` key); no client is affected. |

## Research angles

The engineer agreed on the conclusions from the code on `main`; no separate research angle was run. The conclusions:

- The item DTOs (`CreateItemRequest`, `EditItemRequest`, `ItemResponse`) and the `Item` record gain `title`. Neither request is `@Valid`: `ItemController` validates the envelope by hand and throws the collected errors, which `ApiException.fields` sorts by field. The edit order stays 403, 401, id 404, envelope 400, item 404, payload 400, version 409 (items R4-S8).
- `@Title` is `@NotBlank` plus `@Size(max = 200)` plus `@Pattern`, so it cannot sit on an optional field as is: the title is normalised, a blank one becomes null, and only a non-null value is validated, with the codes of `FieldErrorCodes`.
- Unknown body properties outside the payload are ignored (items R2-S11); `StrictStringJsonConfig` already turns a number or boolean into `INVALID_TYPE` for a `String`, and an array or object also gives `INVALID_TYPE`. A missing and a null `title` both reach the record as null.
- `ItemRepository` (`insertAtEnd`, `findInPart`, `setPosition`, `findByPart`, `updatePayload`, the row mapper) and `ItemService.create` and `edit` carry `title`.
- Constitution section 8: every item query is already scoped by `teacher_id`; the column adds no access path. Section 9: the `Title` pattern accepts Cyrillic.

## Why

A teacher cannot name an item today (items-read-edit, Clarification 10), for example "Немного теории" before a block of theory. A title in the item envelope works for every item type, and the client can still derive numbers like "Упражнение 1" from the type and the position.

## What Changes

- `items` gets a nullable `title` column (migration `V19__item_title.sql`); the item JSON gains `title`, always present and null when unset.
- `POST .../items` accepts an optional `title` (R1); the item list returns it (R3).
- `PATCH .../items/{itemId}` accepts `title` and replaces it together with the payload, a missing or null title clearing it (R4).
- A title is validated as a part title; its errors join the envelope 400 of create and edit.
- The move response (R6) returns the title unchanged.
- Client-visible: every item response has a new `title` property (null when unset); no client is affected.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `items`: R1 (create accepts and returns `title`; the extra-field scenario no longer sends `title`), R3 (list returns `title`), R4 (renamed to "edits an item's payload and title"; edit replaces payload and title together), R6 (the move response carries `title`). R2 and R5 keep their text: R2-S11 (unknown properties outside the payload are ignored) and R5-S5 (a `title` inside the theory payload is `UNKNOWN_FIELD`) stay true.

## Impact

- Backend: `items/api/ItemController`, the DTOs in `items/api/dto`, `items/domain` (`Item`, `ItemService`), `items/persistence/ItemRepository`, migration `V19__item_title.sql`.
- Tests: `AddItemTest`, `EditItemTest`, `ListItemsTest` and `MoveItemTest` assert the exact item keys and change; `support/ApiTest.createItem` gets a title overload.
- No new dependency, no new error code, no change to security configuration.

## Out of scope

- Showing or editing the title in the frontend.
- Storing generated numbers such as "Упражнение 1".
- Any change to the payload of an item type.
