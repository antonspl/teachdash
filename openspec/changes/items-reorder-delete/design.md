# Design: items-reorder-delete

## Context

See proposal.md. Current state on `main` (items-core-model):

- Table `items` (`V17`) with `part_id` (composite foreign key `(part_id, teacher_id)` to `parts`, `ON DELETE CASCADE`), `teacher_id`, `version`, a dense `position` (check `>= 1`) and `unique (part_id, position) deferrable initially deferred`.
- `ItemService.create` locks the user row, the course, the lesson and the part, counts, then inserts at the end. `ItemRepository` is `JdbcClient` with hand-written SQL (`lockPart`, `countInPart`, `insertAtEnd`); the limit comes from `teachdash.items.max-per-part` (50).
- `PartService.move` and `PartService.delete` are the model for the two new writes.
- Teacher-owned data carries `teacher_id`; every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** move and delete an item; positions stay dense and 1-based under parallel requests; the same behaviour and error shape as parts R4 and R5.

**Non-Goals:** list, read and edit of items (#111), answers (#126, #128), moving an item to another part, copying, frontend, OpenAPI.

## Decisions

### D1. Endpoints and requests

In `ItemController`, under `/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items/{itemId}`, no `SecurityConfig` change:

- `PATCH .../position` with `MoveItemRequest(BigInteger position)`, `@NotNull`. 200 with `ItemResponse`.
- `DELETE`, `@ResponseStatus(NO_CONTENT)`, no body.

Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`, as for the course, lesson and part ids.

### D2. `position` input

As parts-edit-delete D2: the field is a `BigInteger` so that any integer is clamped (`min(max(p, 1), count)` with `BigInteger` comparison before narrowing to `int`). The strict coercion (`Fail` for String, Float, Boolean and EmptyString shapes, mapped to `INVALID_TYPE`) is already configured for `BigInteger` in `StrictIntegerJsonConfig`; no new config. Missing and `null` give `REQUIRED` through `@NotNull`; broken JSON and an empty body give `MALFORMED_REQUEST`.

### D3. Locks and flows

Every write runs in one `@Transactional` method with the lock order of create (items-core-model D7), in one service helper used by `create`, `move` and `delete`:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. The course row `for update`, scoped by teacher; no row gives 404.
3. The lesson row `for update`, scoped by course and teacher; no row gives 404.
4. The part row `for update`, scoped by lesson and teacher (`ItemRepository.lockPart`); no row gives 404.
5. The item lookup scoped by `part_id`, `teacher_id` and the item id; no row gives 404.

The user row comes first to avoid a deadlock with `CourseService.rename`. The order matches the add flow and the part delete (user, course, lesson, then the part row deleted with its items by the cascade), so no cycle can form.

After the locks:

- **Move:** read the item's `position` and `count(*)` of the part; clamp the target `t` to `1..count`; then
  - `t > old`: `update items set position = position - 1 where part_id = :p and teacher_id = :t and position > :old and position <= :t`
  - `t < old`: `update items set position = position + 1 where part_id = :p and teacher_id = :t and position >= :t and position < :old`
  - then `update items set position = :t, updated_at = :now where id = :itemId and part_id = :p and teacher_id = :teacherId`.
  The shifts touch neither `updated_at` nor `version`. The last update sets `updated_at` and not `version`: `version` guards payload edits only, so a move does not make a pending edit stale (the edit, #111, compares `version`). For `t == old` only the last update runs, so the item gets 200 and a new `updatedAt`. The payload is not touched. The deferrable unique constraint is checked at commit, so the temporary duplicate between the shift and the final update is allowed.
- **Delete:** `delete from items where id = :itemId and part_id = :partId and teacher_id = :teacherId returning position`; no row gives 404; then `update items set position = position - 1 where part_id = :partId and teacher_id = :teacherId and position > :old`. A second delete finds no row: 404.

No write changes the `updated_at` of the part, the lesson or the course. An item of another part of the same teacher, addressed under the wrong part, is 404 for the same reason as a foreign item: the lookup is always scoped by part and teacher together, after the three parent locks are scoped by the lesson, the course and the teacher.

Order of checks: CSRF and role (403), deactivated (401), malformed id (404), input (400, move only), unknown or foreign id (404). Input validation runs in the controller before the service, so a missing position on an unknown id gives 400, and a malformed id gives 404 first. Accepted race, as for parts: a deactivation in mid-request gives 401 from step 1 after input validation.

### D4. Delete and answers

No answers table exists yet (#126), so delete removes the item row and closes the gap. Answers (#126) add their own table with a foreign key to `items` with `ON DELETE CASCADE` in their own change, so deleting an item then deletes its answers; this change adds no placeholder table. A part delete already removes its items through the `part_id` cascade (items-core-model D1). Deleting an item frees a slot under the 50-item limit.

### D5. Position integrity

The issue asks the design to pick between a parent lock and a deferrable unique `(parent, position)`. This change keeps both, as parts-edit-delete D5 and items-core-model D7 did:

- The lock chain (user row, course, lesson, part) enforces density: all writes to one part are serialised, and every write that moves or removes a row restores 1..n inside its own transaction.
- The deferrable unique `(part_id, position)` from `V17` is the safety net, and it allows the temporary duplicate during the range shift. A violation at commit would be a bug and surfaces as a 500; no code is mapped.

The user row lock already serialises all of one teacher's writes, so the race scenarios do not prove the part lock alone; they prove that the combined lock chain keeps positions dense and within the limit.

### D6. Tests and helpers

Tests extend `support.ApiTest`: `MoveItemTest` and `DeleteItemTest`. Existing helpers `createItem` (direct SQL, no use of the list or edit endpoints, which come with #111), `countItems`, `itemPositions`, `Parallel` and `MutableClock` are reused; a small helper reading one item's `updatedAt` and `version`, and the `updatedAt` of the part, the lesson and the course, is added if missing. Scenarios read stored rows through helpers, never through a list endpoint.

## Dependencies

None added.

## Risks / Trade-offs

- **User row lock.** It serialises a teacher's item writes with the teacher's other writes. Accepted, as for parts.
- **No answers cascade test.** The answers table does not exist; #126 tests its own cascade from an item delete.
- **`BigInteger` for `position`.** As for parts; float and string rejection depends on the existing Jackson coercion, so R6-S4 is the proof.
- **Items-read-edit in parallel.** The edit (#111) compares `version`; a move does not change it, so the two do not conflict through `version`. Both use the same lock chain.

## Migrations

None. The existing `V17__items.sql` (deferrable unique `(part_id, position)`) is enough.
