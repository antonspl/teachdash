# Design: items-read-edit

## Context

See proposal.md. Current state on `main` (items-core-model):

- `items` (`V17`) has `id`, `teacher_id`, `part_id`, `type`, `payload jsonb`, `version bigint` (from 1), `position`, `created_at`, `updated_at`, the composite foreign key `(part_id, teacher_id)` to `parts` with `on delete cascade`, and the deferrable unique `(part_id, position)`.
- `ItemService.create` locks the user row (`findActiveByIdForUpdate`, none gives 401), the course, the lesson and the part (`ItemRepository.lockPart`), then counts and inserts. `ItemPayloadValidator.validate(type, payload)` reads the map into the type's record (unknown properties rejected), runs Bean Validation and `extraChecks`, and `toStoredJson` serialises without null fields. `ItemController` checks `type` and `payload` presence before the service.
- `ItemTypeRegistry` collects `ItemType<?>` beans; none exists in main sources; tests use the test-only `test-note`.
- `StrictIntegerJsonConfig` makes `Integer`, `Long` and `BigInteger` strict (string, float, boolean and empty string fail, mapped to `INVALID_TYPE`); a strict string deserializer, NUL and lone surrogate checks exist.
- `PartService.list` is a read-only transaction without write locks.

## Goals / Non-Goals

**Goals:** list a part's items, edit a payload under an optimistic version, the first real type `theory`.

**Non-Goals:** delete and reorder (#112), answers (#126), student endpoints, a list-of-types endpoint, frontend, OpenAPI, other types.

## Decisions

### D1. Endpoints and requests

In `ItemController`, under `/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items`, no `SecurityConfig` change (`/api/teacher/**` is TEACHER):

- `GET` returns `List<ItemResponse>` (the R1 response record, no new fields). 200.
- `PATCH /{itemId}` with `EditItemRequest(BigInteger version, Map<String,Object> payload)`. 200 with `ItemResponse`. Properties outside `version` and `payload` (such as `type`, `position`, `teacherId`) are ignored, as everywhere (R2-S11).

Path ids are `UUID`; a malformed id is 404 `NOT_FOUND` (api-errors R3).

### D2. Edit input

- `version` is a `BigInteger`: `StrictIntegerJsonConfig` already makes it strict, so a string, a float, a boolean or `1e400` fails while binding the body with `INVALID_TYPE`. A `Long` would turn `99999999999999999999` into a Jackson overflow (`MALFORMED_REQUEST`), so the controller reports `version` `INVALID_TYPE` when `bitLength() > 63` and passes a `long` to the service. A missing or `null` `version` is `REQUIRED`, checked in the controller. A negative or zero `version` is a valid integer and simply never matches (409).
- `payload` is a `Map`, as in create: missing or `null` is `REQUIRED` (controller), a non-object is `INVALID_TYPE` while binding. The first binding error wins (the body is bound in key order), so `{"version": "1", "payload": []}` gives only `version` `INVALID_TYPE`. Presence errors of `version` and `payload` are reported together, sorted by field (`{}` gives `payload` and `version` `REQUIRED`).
- The type is not in the body, so the payload cannot be checked in the controller: it is checked in the service after the item is found (D3). `ItemPayloadValidator.validate` is reused unchanged (its javadoc in `items-core-model` anticipated this).

### D3. Edit flow, locks and order

`ItemService.edit(teacherId, courseId, lessonId, partId, itemId, version, payload)`, `@Transactional`. The controller has already run the envelope checks (D2). In the service:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401.
2. The course row `for update` (404), the lesson row `for update` (404), the part row `for update` (`ItemRepository.lockPart`, 404): the lock chain of create.
3. The item row `for update`, scoped by item id, part id and teacher id (`ItemRepository.lockItem`, returning the stored `type`, `version`); no row gives 404. An item addressed under another part, lesson or course of the same teacher is 404 for the same reason as a foreign item: the lookup is scoped by all the parents and the teacher together.
4. The type is looked up in the registry by the stored code and the payload is validated with `ItemPayloadValidator.validate(type, payload)`; a failure is 400 (`payload.<path>`), thrown before the version is compared, so a bad payload with a stale version is 400.
5. The stored `version` differs from the given one: 409 `VERSION_CONFLICT`, a plain `ApiException` (no `errors`, no stored payload in the body).
6. `update items set payload = cast(:payload as jsonb), version = version + 1, updated_at = :now where id = :itemId and part_id = :partId and teacher_id = :teacherId returning payload::text, position, created_at, version, updated_at`. `now` comes from `Clock`, truncated to milliseconds as in create. No other row or table is written: not the other items, the part, the lesson or the course.

Order of checks: CSRF and role (403), deactivated (401), malformed id (404), envelope (400), unknown or foreign id (404), payload by type (400), version (409). Accepted race, as for create: a deactivation in mid-request gives 401 from step 1 after the envelope check.

The user row lock already serialises one teacher's writes, so the parallel scenario proves the version check under the whole chain, not the item lock alone. The item lock stays so that the read of `version` and the update are one atomic step whatever the outer locks are. Two edits with the same version: the first updates and commits, the second then reads the new version and gets 409; the version ends at old plus one (R4-S9). An edit racing a part delete (R4-S10) queues behind the user and course locks: either the edit runs first (the delete cascades to the item) or finds no part and gets 404.

The stored type is always in the registry: types are never removed from code, so an unregistered stored type is not reachable and has no scenario or rule.

### D4. List flow

`ItemService.list(teacherId, courseId, lessonId, partId)`, `@Transactional(readOnly = true)`, no row locks, as `PartService.list`: one query checks that the part exists under the course, lesson and teacher (404 otherwise), then `select ... from items where part_id = :partId and teacher_id = :teacherId order by position`. The list does not need the registry: it returns the stored type and payload as is. The stored payload is read as text and parsed into a JSON value, as in create. It takes no write locks. The check order is role (403), deactivated (401, by the security layer as for parts R2), malformed id (404), unknown or foreign (404).

### D5. The `theory` type

`com.teachdash.items.domain.theory` (or next to the other types, one package): `TheoryItemType implements ItemType<TheoryPayload>` as a `@Component`, code `theory`; `record TheoryPayload(@NotBlank @Size(max = 20000) String text)`.

- `@NotBlank` gives `REQUIRED` for a missing, null, empty or whitespace-only text (`FieldErrorCodes.codeOf`, as for course and part titles); `@Size(max = 20000)` gives `TOO_LONG`, counted in `String.length()` (UTF-16 units, as `@Size`); a number or array is `INVALID_TYPE` from the strict string deserializer; an unknown property is `UNKNOWN_FIELD` from the strict mapper of the validator.
- No trimming and no HTML handling: the record keeps the text, the stored mapper serialises it as is. Raw HTML is stored as text; the client renders Markdown with raw HTML off and formulas with KaTeX (#114).
- `studentView(payload)` returns the payload unchanged (the record); this is not a spec requirement. No endpoint calls it here; a unit test with no scenario ID covers it.
- 20000 characters of text fit under the 1 MB body limit at the Caddy level (design D9 of items-core-model), so no new body limit.

### D6. api-errors

No `api-errors` delta. `VERSION_CONFLICT` has no field list, and `api-errors` R2 already says that any code other than the four named carries no `errors`. The behaviour is proven by items R4-S5.

### D7. Tests and helpers

Tests extend `support.ApiTest`. `createItem` gets a `version` argument (or an overload) so a scenario can start at `version` 3; a reader of one item row (type, payload jsonb text, `version`, `updatedAt`) exists. R4-S9 and R4-S10 use `support.Parallel.run`. Both `test-note` (test sources) and `theory` (main sources) are registered in tests. Unit tests with no scenario ID: the `theory` student view returns the payload unchanged.

## Dependencies

None added.

## Risks / Trade-offs

- **Stale version tells nothing.** The 409 does not return the stored item; the client reloads with the list. Accepted, simple.
- **Unchanged payload still bumps the version.** Decided (decision 5): a client cannot tell "nothing changed" from the response; harmless.
- **Registry no longer empty in production.** Any other code is still `UNKNOWN_TYPE`.
- **Limits are guesses.** 20000 characters of theory text and 50 items per part are estimates; the second is a property.

## Migrations

None.
