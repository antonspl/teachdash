# Design: items-core-model

## Context

See proposal.md. Current state on `main` (parts-edit-delete):

- `parts` (`V16`) has `lesson_id` (foreign key, `on delete cascade`), `teacher_id`, a dense `position` and `unique (lesson_id, position) deferrable initially deferred`. A part has no child tables yet.
- `PartService.lockForWrite` locks the user row (`UserRepository.findActiveByIdForUpdate`, none gives 401), then the course row, then the lesson row, each scoped by `teacher_id` and the parent ids; `PartRepository` is `JdbcClient` with hand-written SQL; a limit comes from a property read with `@Value`.
- `common/FieldErrorCodes` holds the field error codes; `NulRejectingJsonConfig` rejects NUL in typed JSON strings through a String deserializer. Jackson is version 3 (`tools.jackson`).
- Teacher-owned data carries `teacher_id`; every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** an `items` table, a type registry with a mandatory student projection, add an item to an own part, a payload that is checked and stored safely as jsonb, a dense position under parallel requests.

**Non-Goals:** list, edit, reorder and delete items (#111, #112), answers (#126, #128), student endpoints, a list-of-types endpoint, real item types, server-level HTTP body size limits, frontend, OpenAPI.

## Decisions

### D1. Migration `V17__items.sql`

```sql
alter table parts add constraint parts_id_teacher_uq unique (id, teacher_id);

create table items (
    id             uuid        primary key,
    teacher_id     uuid        not null references users (id) on delete cascade,
    part_id        uuid        not null,
    type           varchar(50) not null,
    payload        jsonb       not null,
    schema_version int         not null check (schema_version >= 1),
    version        bigint      not null check (version >= 1),
    position       int         not null check (position >= 1),
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint items_part_owner_fk
        foreign key (part_id, teacher_id) references parts (id, teacher_id) on delete cascade,
    constraint items_part_position_uq
        unique (part_id, position) deferrable initially deferred
);
create index items_teacher_idx on items (teacher_id);
```

- Items hang off `part_id` only, never `lesson_id` or `course_id`: a lesson can be in several courses by copy, and the course and the lesson are found through `parts` and `lessons`.
- `teacher_id` is on the item so queries are scoped by the teacher without a join (constitution §8). The composite foreign key `(part_id, teacher_id)` to the unique `(id, teacher_id)` of `parts` makes the database refuse an item whose owner differs from its part's owner, so owner consistency does not rest on the service alone. The same gap from `parts` to `lessons` stays as it is (out of scope). No scenario tests the constraint: it is a migration constraint and no migration-only test is written.
- `on delete cascade` on the composite key deletes a part's items with it (the later part delete and lesson delete need no change; answers, #126, add their own foreign key to items). `on delete cascade` on `teacher_id` covers account deletion.
- `version` starts at 1; the later item edit uses it for the optimistic 409. `schema_version` is the version of the payload schema the type had when the payload was written; a later type version can migrate old payloads by it.
- `type` is `varchar(50)` and has no check constraint: the set of types is code, not schema.
- `updated_at` comes from `Clock`; every later change to an item row MUST set it, and an edit MUST increase `version`.
- Version `V17` is the next on `main`; renumbered at merge if needed (#98).

### D2. Endpoint and package

`com.teachdash.items` (`api`, `api/dto`, `domain`, `persistence`):

- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items`, `@ResponseStatus(CREATED)`, body `CreateItemRequest(String type, JsonNode payload)`. No `SecurityConfig` change: `/api/teacher/**` is already TEACHER.
- Response `ItemResponse(id, type, payload, position, version, createdAt, updatedAt)`. Unknown body fields such as `teacherId` or `title` are ignored.
- Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`, as for courses, lessons and parts.
- `payload` is read as a `JsonNode` so that "missing", "null" and "not an object" can be told apart and mapped to the codes of R2. `type` is a `String`; the existing strict String coercion applies to it, as for other text fields.

### D3. Type registry

```java
public interface ItemType {
    String code();
    int schemaVersion();
    ItemPayload validate(ObjectNode payload);   // the normalised payload, or field errors
    JsonNode studentProjection(ObjectNode payload);
}
```

- `validate` returns the normalised payload, or throws the field errors (`FieldErrorDetail` with `field` paths under `payload.`, for example `payload.text`). `FieldErrorCodes` is package-private and `ApiException.field` builds one detail, so `common` gets two small public additions (the simplest way, instead of moving the payload checks into `common`): a factory `ApiException.fields(List<FieldErrorDetail>)` that passes the list through `FieldErrorCodes.reduce` (sorted by field, one error per field) and returns a 400 `VALIDATION_FAILED`, and a public constant for the code, `FieldErrorCodes.UNKNOWN_TYPE` being made reachable through a public `FieldErrorDetail` factory (or a public constants holder). `UNKNOWN_TYPE` falls into the default priority tier in `FieldErrorCodes.priority`, the same as `INVALID_FORMAT`.
- `studentProjection` is abstract: a type cannot be registered without one. It is an allowlist: it builds a new tree from the fields a student may see; it does not remove teacher-only fields from a copy, so a field added later is hidden until a type's author adds it on purpose. No endpoint calls it in this change; a unit test covers the test type's projection and the rule that the projection does not return the stored tree.
- Types are Spring beans. `ItemTypeRegistry` collects `List<ItemType>` in its constructor and builds an immutable map by `code()`; a duplicate code fails startup with an `IllegalStateException` naming the code. A unit test builds a registry with two types with one code and expects the failure.
- No production type is registered. In production the registry is empty and every request gets `UNKNOWN_TYPE` until #111 adds a type; this is accepted and stated in the proposal.
- The test type `test-note` lives in test sources (`items/support` or next to `QueryParamTestController`, which proves that test-only beans are found by component scan): schema version 2, payload `{text}` with `text` required (missing or null: `payload.text` `REQUIRED`; not a string: `INVALID_TYPE`; over 1000 characters: `TOO_LONG`), other fields such as `hint` and `extra` are kept as they are, and the projection returns `{text}` only.

### D4. Generic payload rules and where they run

Input validation runs before the service, so a bad payload on an unknown part gives 400 and a malformed id gives 404 first (as parts R1). The order is:

1. `type` missing, null or blank gives `REQUIRED`; an unregistered code gives `UNKNOWN_TYPE` (a new constant in `FieldErrorCodes`).
2. `payload` missing or `null` gives `REQUIRED`; a payload that is not an object gives `INVALID_TYPE`.
3. Size: the payload is serialised once, compact, to bytes with Jackson; more than 65536 bytes gives `payload` `TOO_LONG`. The same bytes are written to the database, so the check applies to what is stored.
4. NUL: a walk over the tree rejects U+0000 in any string value and any field name, giving `payload` `INVALID_FORMAT`.
5. The type's `validate`, only if steps 1 to 4 passed; its errors are reported as they are.

Errors of the steps 1 and 2 about different fields are reported together (a body `{}` gives `payload` and `type` `REQUIRED`, sorted by field); an unknown type and a bad payload may be reported together too; the steps 3 to 5 each stop the check, so a type's error never appears next to a generic payload error.

The 64 KiB limit is a guess that fits text exercises with options and a few long explanations; it is a constant of the items package that a later change can move to a property. 64 KiB is checked on the parsed tree, so a request body is parsed in full before the check: a server-level body limit would protect against huge bodies, but it is outside this change (see Risks).

### D5. Why the NUL walk

`NulRejectingJsonConfig` registers a deserializer for `String` targets. A `JsonNode` tree is built by Jackson's own node deserializers, so string values inside `payload` never go through it, and object keys use key deserializers that it does not cover either. Postgres rejects U+0000 in jsonb text (`unsupported Unicode escape sequence`), so without the walk a NUL in a payload would reach the database and give a 500. The walk is a small recursive method over `ObjectNode`, `ArrayNode` and `TextNode`, run after the size check so that it runs on a bounded tree. A NUL in the `type` string is already covered by the typed String deserializer.

### D6. JSONB without JPA

There is no JPA in the project and no converter. `ItemRepository` writes with `JdbcClient`:

```sql
insert into items (id, teacher_id, part_id, type, payload, schema_version, version, position, created_at, updated_at)
select :id, :teacherId, :partId, :type, cast(:payload as jsonb), :schemaVersion, 1,
       coalesce(max(position), 0) + 1, :now, :now
from items where part_id = :partId
returning payload, position
```

- `:payload` is the compact string from Jackson 3 (`tools.jackson.databind.ObjectMapper#writeValueAsString`). A String cast is used rather than a `PGobject`: it keeps the repository free of driver types and of a custom `JdbcClient` parameter binding, uses the same one parameter style as the other queries, and the cast checks the text is valid JSON on the database side.
- The returned `payload` is read as text and parsed with `readTree`. The response shows what the database stored, so the stored and the returned forms are the same by construction.
- jsonb does not keep key order or whitespace and keeps one value per key (the last one wins); the response reflects this. R2-S9 states it. Numbers keep their value but jsonb may print them in a normalised form (for example `1.0` stays `1.0`, exponents are expanded); types that care about numbers must validate them.
- A type's `validate` returns the tree that is stored; the registry does not store the request tree directly.

### D7. Add flow, locks and position

`ItemService.create(teacherId, courseId, lessonId, partId, type, payload)`, `@Transactional`. The validation of D4 runs before it, in the controller (or a small validator it calls). In the service:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. The course row `for update`, scoped by teacher; no row gives 404.
3. The lesson row `for update`, scoped by course and teacher; no row gives 404.
4. The part row `for update`, scoped by lesson and teacher; no row gives 404. A part of another lesson or course of the same teacher is 404 for the same reason as a foreign part: the lookup is scoped by all the parents and the teacher together.
5. `count(*) from items where part_id = :partId` at or above `teachdash.items.max-per-part` (50, read with `@Value`) gives 409 `ITEM_LIMIT_REACHED` (a plain `ApiException`, no `errors`).
6. Insert as in D6: position is `coalesce(max(position), 0) + 1`, which is count + 1 because positions are dense.

`PartRepository` already has `lockCourse` and `lockLesson`; the part lock is new (in the items repository or `PartRepository`). The lock order extends the one of `PartService`: user, course, lesson, then part. A part delete takes user, course and lesson, then deletes the part row and the cascade deletes the items, so no cycle forms: an add racing a part delete queues behind the user and course locks, and either runs first (its item is cascaded) or finds no part and gets 404 (R1-S10). The user row comes first because the `teacher_id` foreign key puts a KEY SHARE lock on it into every write, and `CourseService.rename` locks it before the course row.

The user row lock already serialises all of one teacher's writes, so the race scenarios do not prove the part lock alone. The part lock stays for 404 scoping and for position correctness; R1-S10 (a part delete racing adds) is the scenario that covers the part lock, R1-S5 proves the lock chain as a whole. The deferrable unique constraint is a safety net: a violation at commit would be a bug and surfaces as a 500; no code is mapped. Reordering items (#112) uses the same locks and one range shift, which a deferrable constraint allows.

No write changes the `updated_at` of the part, the lesson or the course (R1-S3). Order of checks: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404), limit (409). Accepted race, as for parts: a deactivation in mid-request gives 401 from step 1 after input validation.

### D8. Tests and helpers

Tests extend `support.ApiTest`. New helpers: `createItem(teacherId, partId, type, payload, position)` (direct SQL, times from the test `Clock`, `schema_version` 2 for `test-note`), `countItems(partId)`, `itemPositions(partId)`, and a reader of one item row (type, payload as jsonb text, `schema_version`, `version`). R1-S5 and R1-S10 use `support.Parallel.run`. The test type and its registry tests live in test sources. The unit tests for the registry (duplicate code fails startup) and the projection (builds `{text}` only, drops `hint`) have no scenario ID, as the proposal says.

## Dependencies

None added. Jackson 3 and Postgres jsonb are already available.

## Risks / Trade-offs

- **No body size limit at the server (accepted).** The 64 KiB limit is applied after the full JSON is parsed. A huge request body is read and parsed before it is refused. No endpoint has a server-level body limit today, so this is not specific to items and no issue is opened. Tomcat's default limits (`max-http-form-post-size` does not apply to JSON) leave this open; a limit at Caddy or in Spring (`server.tomcat.max-swallow-size`, a request-size filter) is a separate change.
- **Empty registry in production.** Until #111 every add gives `UNKNOWN_TYPE`; the endpoint is not useful to a user before then.
- **jsonb normalisation.** The response differs from the request in key order, whitespace and duplicate keys. Clients must not rely on key order. A type that needs an order uses an array.
- **Owner consistency.** Enforced for items by the composite foreign key (D1). Not for `parts` to `lessons`: left as is.
- **Limits are guesses.** 50 items per part and 64 KiB are estimates; the first is a property, the second a constant.
- **Item type removal.** A type removed from code leaves rows with a type no registry knows. Later reads must treat an unknown stored type explicitly; no read exists in this change.

## Migrations

- `V17__items.sql`: unique `(id, teacher_id)` on `parts`, table `items` with the composite foreign key, the deferrable unique `(part_id, position)`, the index on `teacher_id`.
