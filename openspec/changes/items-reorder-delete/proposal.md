# Proposal

## Clarifications

Issue #112. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Move endpoint and `position` input? | `PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}/position` with `{position}` gives 200 with the moved item (the fields of the create response). `position` is a required strict JSON integer parsed as `BigInteger`, as parts move: missing or null gives `REQUIRED`; a string, a float (also `2.0` and `1e2`) or a boolean gives `INVALID_TYPE`; a broken or absent body gives `MALFORMED_REQUEST`. It is clamped to 1..count; there is no out-of-range error. |
| 2 | What does a move change? | The shift is one range update, as parts R4. The moved item's `updatedAt` is set, also on a same-position move. Its `version` is NOT changed: `version` guards payload edits only, so a move does not make a pending edit stale. Shifted items keep `updatedAt` and `version`. The `updatedAt` of the part, the lesson and the course is unchanged. The payload is unchanged. |
| 3 | Position integrity (the issue asks the design to pick)? | Keep both existing mechanisms: the lock chain (user row, course, lesson, part, then the item rows `FOR UPDATE`) enforces density; the existing deferrable unique `(part_id, position)` is the safety net and allows the temporary duplicate during the shift. The design records this and cites parts-edit-delete D5 and items-core-model design D7. |
| 4 | Delete? | `DELETE .../items/{itemId}` gives 204 with no body. A second delete gives 404. Later items shift down by one and keep `updatedAt` and `version`. The `updatedAt` of the part, the lesson and the course is unchanged. A deletion frees a slot under the 50-item limit. |
| 5 | "Delete of an item deletes its answers"? | No answers table exists yet (#126). The spec says only that the item row is removed. #126 adds its own foreign key to `items` with `ON DELETE CASCADE`, so a delete then removes the answers too. |
| 6 | Order of checks? | For both requirements: CSRF and role (403), deactivated (401), malformed id (404), input (400, move only), unknown or foreign id (404). |
| 7 | Concurrency scenarios? | 10 parallel moves in one part: all 200, positions dense 1..n. Parallel deletes and moves: no 500, positions dense. Deletes racing creates: each create is 201 or 409, positions dense. Deletes racing a delete of the part: each 204 or 404, no 500. |
| 8 | Migration and requirement IDs? | No migration. Requirement IDs R6 and R7 only (the reserved range). |

The issue's decisions also say: teacher scoping at every level (course, lesson, part, item), and content in both languages (`en` and `ru`) in the scenarios.

## Research angles

The engineer agreed no separate research was needed. The conclusions came from the parts code (parts-edit-delete, which this change copies) and the items code (items-core-model):

- No `SecurityConfig` change: `/api/teacher/**` is already TEACHER.
- The move and delete algorithms are `PartService.move` and `PartService.delete`: clamp with `BigInteger`, shift the range by one, set the moved row; delete returns the position of the deleted row, then closes the gap.
- `ItemService.create` already takes the locks user row, course, lesson, part; `ItemRepository.lockPart` exists. The new writes use the same chain and then look the item up scoped by part and teacher.
- The strict integer coercion for `BigInteger` is already configured (`StrictIntegerJsonConfig`).
- Code to touch: `items/api/ItemController` (two mappings), `items/api/dto` (`MoveItemRequest`), `items/domain/ItemService` (`move`, `delete`, one lock helper shared with `create`), `items/persistence/ItemRepository` (lookup scoped by part and teacher, `shiftRange`, `setPosition`, delete returning the position, `closeGap`).
- Tests: `MoveItemTest` and `DeleteItemTest` in `backend/src/test/java/com/teachdash/items/api/`, with the helpers `createItem`, `countItems`, `itemPositions`, `Parallel`, `MutableClock` from `support/ApiTest`.

## Why

Since #110 a teacher can add items to a part but not fix their order or remove one: a wrong order or a mistaken item can be fixed only by deleting the whole part, and a part at the 50-item limit cannot shrink.

## What Changes

- `PATCH .../items/{itemId}/position` moves an item to a position in its part, clamped to 1..count (R6).
- `DELETE .../items/{itemId}` deletes an item and closes the gap in positions (R7).
- No migration and no new error code.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `items`: adds R6 move and R7 delete. R1 and R2 keep their text. A parallel change, items-read-edit (#111), adds R3-R5; this change does not depend on it and its tests set data up directly.

## Out of scope

- List, read and edit of items (#111).
- Answers (#126, #128): no answers table exists, so there is nothing to cascade yet; #126 adds its own `ON DELETE CASCADE` foreign key to `items`.
- Moving an item to another part, copying items.
- Changes to part, lesson or course behaviour.
- Frontend and OpenAPI.

## Impact

- Backend: `items` package (controller, service, repository, one request record); `ItemResponse` is reused.
- Database: none.
- API: two new endpoints; no new error code.
- No new dependencies.
