# Design: items-core-model

## Context

See proposal.md. Current state on `main` (parts-edit-delete):

- `parts` (`V16`) has `lesson_id` (foreign key, `on delete cascade`), `teacher_id`, a dense `position` and `unique (lesson_id, position) deferrable initially deferred`. A part has no child tables yet.
- `PartService.lockForWrite` locks the user row (`UserRepository.findActiveByIdForUpdate`, none gives 401), then the course row, then the lesson row, each scoped by `teacher_id` and the parent ids; `PartRepository` is `JdbcClient` with hand-written SQL; a limit comes from a property read with `@Value`.
- `common/FieldErrorCodes` holds the field error codes; `NulRejectingJsonConfig` rejects NUL in typed JSON strings through a String deserializer (and now a lone surrogate); `StrictIntegerJsonConfig` makes integer parsing strict. Jackson is version 3 (`tools.jackson`).
- Teacher-owned data carries `teacher_id`; every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** an `items` table, a generic type registry with a mandatory typed student view, add an item to an own part, a payload that is read into a typed record, checked and stored as jsonb, a dense position under parallel requests.

**Non-Goals:** list, edit, reorder and delete items (#111, #112), answers (#126, #128), student endpoints, a list-of-types endpoint, real item types, frontend, OpenAPI.

## Decisions

### D1. Migration `V17__items.sql`

```sql
alter table parts add constraint parts_id_teacher_uq unique (id, teacher_id);

create table items (
    id          uuid        primary key,
    teacher_id  uuid        not null references users (id) on delete cascade,
    part_id     uuid        not null,
    type        varchar(50) not null check (btrim(type) <> ''),
    payload     jsonb       not null check (jsonb_typeof(payload) = 'object'),
    version     bigint      not null check (version >= 1),
    position    int         not null check (position >= 1),
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    constraint items_part_owner_fk
        foreign key (part_id, teacher_id) references parts (id, teacher_id) on delete cascade,
    constraint items_part_position_uq
        unique (part_id, position) deferrable initially deferred
);
create index items_teacher_idx on items (teacher_id);
```

- `V17` is edited in place: it is not on `main` yet, so no new migration is needed.
- Items hang off `part_id` only, never `lesson_id` or `course_id`: a lesson can be in several courses by copy, and the course and the lesson are found through `parts` and `lessons`.
- `teacher_id` is on the item so queries are scoped by the teacher without a join (constitution §8). The composite foreign key `(part_id, teacher_id)` to the unique `(id, teacher_id)` of `parts` makes the database refuse an item whose owner differs from its part's owner, so owner consistency does not rest on the service alone. The same gap from `parts` to `lessons` stays as it is (out of scope). No scenario tests the constraint; one database test without a scenario ID inserts an item with teacher B's id on teacher A's part and expects an integrity error (a constraint is tested, a migration run is not).
- `on delete cascade` on the composite key deletes a part's items with it (the later part delete and lesson delete need no change; answers, #126, add their own foreign key to items). `on delete cascade` on `teacher_id` covers account deletion.
- `version` starts at 1; the later item edit uses it for the optimistic 409.
- There is no `schema_version`: a payload shape never changes after release; a new shape is a new type code (D3).
- `type` is `varchar(50)`; `check (btrim(type) <> '')` refuses a blank code. The set of types is code, not schema, so there is no check of the values. `check (jsonb_typeof(payload) = 'object')` refuses a payload that is not an object, as the generic rules do.
- `updated_at` comes from `Clock`; every later change to an item row MUST set it, and an edit MUST increase `version`.
- Version `V17` is the next on `main`; renumbered at merge if needed (#98).

### D2. Endpoint and package

`com.teachdash.items` (`api`, `api/dto`, `domain`, `persistence`):

- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items`, `@ResponseStatus(CREATED)`, body `CreateItemRequest(String type, Map<String,Object> payload)`. No `SecurityConfig` change: `/api/teacher/**` is already TEACHER.
- Response `ItemResponse(id, type, payload, position, version, createdAt, updatedAt)`. Unknown fields of the request body outside `payload` (such as `teacherId` or `title`) are ignored, as everywhere (R2-S11).
- Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`, as for courses, lessons and parts.
- `payload` is a `Map`, so "missing", "null" and "not an object" can be told apart. A payload that is not an object (`[]`, `"Hi"`, `""`, `123`, `true`) fails while Spring binds the body to the `Map`, before the controller runs: the one error `payload` `INVALID_TYPE`, and `type` is not checked (the first binding error wins: the body is bound in key order and binding stops at the first error, so `{"type":123,"payload":[]}`, with `type` first, gives only `type` `INVALID_TYPE`; a blank or unknown string `type` is not a binding error, so with a non-object payload the payload error is the only one). Missing or null `payload`: `REQUIRED` (the controller). Map values go through the app's String deserializer. `type` is a `String`; the strict String coercion applies, a blank value is `REQUIRED`.
- `payload: ""`: expected `payload` `INVALID_TYPE`, like any other string. `StrictStringJsonConfig` and `StrictIntegerJsonConfig` set no coercion for `Map`, so Jackson's default for an empty string into a `Map` applies; task 1.1 checks it. If Jackson turns `""` into null or an empty map (which would give `REQUIRED` or an accepted empty payload), the implementer stops and reports; the expected result stays `INVALID_TYPE`.
- The controller is a few lines: look up the type in the registry (`UNKNOWN_TYPE`), call `ItemPayloadValidator`, serialise the record, call the service.

### D3. Item types and the registry

```java
public interface ItemType<P> {
    String code();
    Class<P> payloadClass();
    Object studentView(P payload);
    default List<FieldErrorDetail> extraChecks(P payload) { return List.of(); }
}
```

- A type is a Spring bean; adding a bean adds a type. `ItemTypeRegistry` collects `List<ItemType<?>>` in its constructor and builds an immutable map by `code()`; a duplicate code fails startup with an `IllegalStateException` naming the code. A unit test builds a registry with two types with one code and expects the failure. In production the registry is empty and every request gets `UNKNOWN_TYPE` until #111 adds a type; this is accepted.
- `payloadClass()` is a record with Bean Validation annotations. Rules for the records: wrapper types with `@NotNull`, never primitives (a missing field is then null and gives `REQUIRED`); no class-level constraints (cross-field rules go in `extraChecks`, which returns `FieldErrorDetail`s with fields under `payload.`); `@Size` or `@MaxUtf8Bytes` on every String field and every list, so the size of a payload is bounded by its record (D4); lists are declared `List<@Valid X>` so element paths are reported (`answers[0].text`); `Double` and `Float` are forbidden (`1e400` would become an infinite `Double` that jsonb cannot store); numbers use `Integer`, `Long` or `BigDecimal` (fractions: `BigDecimal`); every `BigDecimal` field carries `@Digits(integer = ..., fraction = ...)`, because Postgres `numeric` has a digit limit and `1e200000` or `1e-200000` would give a 500 at the jsonb cast (a `@Digits` violation is `INVALID_FORMAT`: `FieldErrorCodes.codeOf` maps every constraint it does not list to it). A record has no `Map` field and no custom key deserializer: map keys bypass the String deserializer (no NUL or lone-surrogate check), so a free-form object is not allowed in a payload; use a list of records. Records need `-parameters`, already used.
- `studentView(P)` is abstract and typed: a type cannot be registered without one. It builds the student view from the fields a student may see (an allowlist; it does not remove teacher-only fields from a copy), so a field added later is hidden until a type's author adds it on purpose. No endpoint calls it in this change; a unit test covers the test type's view.
- A new payload shape is a new type code; a released type never changes.
- `common` gets two small public helpers, extracted from existing code, so `items` does not copy it: `ApiException.fromJackson(DatabindException, String prefix)` (from `ApiExceptionHandler.fieldOf` and `jsonCodeOf`) and `ApiException.validation(Set<ConstraintViolation<?>>, String prefix)` (from `FieldErrorCodes.fromViolations`: `NotNull` and `NotBlank` give `REQUIRED`, `Size` gives `TOO_SHORT` or `TOO_LONG`, others `INVALID_FORMAT`). `fromJackson` gives the field as the Jackson path under the prefix and one of `INVALID_TYPE`, `INVALID_FORMAT` (the NUL and lone-surrogate exception is found in the cause chain) or `UNKNOWN_FIELD`. It never gives `REQUIRED`: a missing property becomes null and Bean Validation reports `REQUIRED`. A root mismatch (empty path) gives the field `payload` (the prefix alone). An unrecognised property always gives the field `payload`, never the property name, because the key is user text and is not echoed. Map keys are never echoed: the path is cut at the first `Map` key segment (a reference whose owner is a `Map`), so the field is the path up to the `Map` (for example `payload` for a value under `payload.secretKey`); `ApiExceptionHandler.fieldOf` does the same for body binding. The remaining path segments are record property names and list indexes (records have no `Map` fields, D3, and unknown fields report `payload`). `ApiException.validation(Set)` stays and the prefixed overload is added; existing callers are unchanged. The existing handler uses `fromJackson` too. `FieldErrorCodes` stays public in `common` with the new constants `UNKNOWN_TYPE` and `UNKNOWN_FIELD`, both in the default priority tier, the same as `INVALID_FORMAT`. The factory `ApiException.fields(List<FieldErrorDetail>)` (sorted, one error per field) turns the `extraChecks` list into an exception.
- The test type `test-note` lives in test sources (next to `QueryParamTestController`, which proves that test-only beans are found by component scan): a record `TestNotePayload(@NotNull @Size(max = 1000) String text, @Size(max = 200) String hint, Integer rating, @Digits(integer = 10, fraction = 20) BigDecimal weight)`; `rating` and `weight` are optional and carry the number scenario R2-S12; `studentView` returns `{text}` only and drops the other fields.

### D4. `ItemPayloadValidator` and the order of the payload checks

Input validation runs before the service, so a bad payload on an unknown part gives 400 and a malformed id gives 404 first (as parts R1). Steps:

1. `type` missing, null or blank gives `type` `REQUIRED`; an unregistered code gives `type` `UNKNOWN_TYPE`.
2. `payload` missing or `null` gives `payload` `REQUIRED`. A payload that is not an object never reaches the controller: binding the body to the `Map` fails first with the one error `payload` `INVALID_TYPE`, and `type` is not checked (D2).
3. Only if steps 1 and 2 both passed: `ItemPayloadValidator.validate(ItemType<P>, Map<String,Object>)` (in `items/domain`, reusable by the item edit in #111):
   a. `objectMapper.convertValue(payload, payloadClass)` with a mapper that rejects unknown properties for this conversion only (a derived mapper built once from the app's mapper, so the custom String deserializer and the strict coercions stay; the global `FAIL_ON_UNKNOWN_PROPERTIES` is not changed). A `DatabindException` becomes `payload.<path>` through `ApiException.fromJackson`; an unknown property is the field `payload` itself (D3). Jackson stops at the first unknown or mistyped property, so one such error is reported alone (R2-S10). A NUL or lone surrogate in a payload string is normally rejected earlier, at body binding (field `payload`, see Strings); the check in `fromJackson` of the cause chain of the `DatabindException` that `convertValue` throws (task 1.1 verifies this) stays as a safety net, for example for a value that is created by the conversion and not read from the body.
   b. The record is checked with `jakarta.validation.Validator`; violations become `payload.<path>` through `ApiException.validation`, and Bean Validation errors are reported together.
   c. Only if there are no violations, `extraChecks(record)` runs; its errors are reported as they are.
4. The controller serialises the record with the stored-payload mapper (`writeValueAsString`, D5) and passes that String on.

Errors of the steps 1 and 2 about different fields are reported together (a body `{}` gives `payload` and `type` `REQUIRED`, sorted by field); a binding error stops everything else (the first wins, D2); step 3 runs only if both passed, so a type's error never appears next to a generic payload error.

**Size.** There is no serialised-size check. The size of a payload is bounded by `@Size` or `@MaxUtf8Bytes` on every record field and list (D3). The whole request body is bounded by Caddy (D9).

**Strings.** The String deserializer in `NulRejectingJsonConfig` runs on Map values and during `convertValue`, so a NUL or lone surrogate in a payload String value is found already while Spring binds the body to the `Map`, before the conversion to the record. There known and unknown keys are both Map keys, so the cut at the Map key (D3) gives `payload` `INVALID_FORMAT` for every string value, a record field (`text`, `hint`) or not (R2-S5, R2-S13). The asymmetry is deliberate: an error found later, in `convertValue` or Bean Validation, has a record property path and keeps it (`{"text":42}` gives `payload.text` `INVALID_TYPE`, `@Size` gives `payload.text` `TOO_LONG`); a value rejected at binding cannot be told apart from an unknown key's value, and the key must not be echoed. The same deserializer now rejects a lone (unpaired) surrogate, on every endpoint: the parser accepts `\ud800`, but writing it escapes it to `\uD800`, which Postgres jsonb refuses (a 500). It does not apply to Map keys; a NUL in a key is an unknown property of the record, `UNKNOWN_FIELD` with the field `payload` (R2-S6): the key name is user text and is never echoed. For the same reason a NUL or lone surrogate in a value under an arbitrary key, found while binding the body to the `Map`, is reported with the field cut at the `Map` (`payload`, never `payload.<key>`): `{"text":"ok","secretKey":"a\u0000"}` gives `payload` `INVALID_FORMAT` (R2-S13).

**Numbers.** `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` is enabled globally: the request body is bound to a `Map` by the app's `ObjectMapper`, so the flag must be global. It changes only untyped `Object` and `Map` values (a JSON float becomes a `BigDecimal`, not a `Double`); typed fields are not affected. Payload records may use `BigDecimal` for fractions, so `0.12345678901234567890` keeps all its digits; `Double` and `Float` stay forbidden (D3). `1e400`, `1e200000` and `1e-200000` stay `BigDecimal`s when read, but a `BigDecimal` field is bounded by `@Digits` (D3), so they are `payload.<field>` `INVALID_FORMAT` and never reach jsonb; into an `Integer` or `Long` field they are `payload.<field>` `INVALID_TYPE` (the strict coercion below), as `1.5` is. R2-S12 and a unit test cover it.

**Strict integers.** `StrictIntegerJsonConfig` is strict for `BigInteger` only, so today `Integer` and `Long` fields accept "5" and coerce 5.7 to 5. The customizer is extended to `Integer` and `Long` (string, float, boolean and empty string fail: `INVALID_TYPE`). This affects every endpoint; no current body has such a field. The rule is in `api-errors` R1; a unit test with the app's mapper covers it (no existing endpoint has an `Integer` or `Long` body field, so there is no scenario of its own; the item scenario R2-S12 covers a `test-note` `Integer` field). `""` into an `Integer` would otherwise become null (`REQUIRED`); the strict config makes it `INVALID_TYPE`.

### D5. JSONB without JPA

There is no JPA in the project and no converter. `ItemRepository` writes with `JdbcClient`:

```sql
insert into items (id, teacher_id, part_id, type, payload, version, position, created_at, updated_at)
select :id, :teacherId, :partId, :type, cast(:payload as jsonb), 1,
       coalesce(max(position), 0) + 1, :now, :now
from items where part_id = :partId and teacher_id = :teacherId
returning payload, position
```

- `:payload` is the validated record serialised by a mapper derived from the app's `ObjectMapper` with `JsonInclude.Include.NON_NULL` as the default inclusion (`writeValueAsString`), passed as a String. The stored payload has no null fields: an absent `hint` is not stored as `"hint": null` (R2-S9). Only the stored payload uses this mapper; response serialisation is unchanged. A String cast is used rather than a `PGobject`: it keeps the repository free of driver types and of a custom `JdbcClient` parameter binding, uses the same one parameter style as the other queries, and the cast checks the text is valid JSON on the database side.
- The returned `payload` is read as text. Later reads use `readValue(json, payloadClass)`; the create response parses the stored text into a generic JSON value, so it shows what the database stored.
- jsonb does not keep key order or whitespace and keeps one value per key (the last one wins); the response reflects this. R2-S9 states it. Numbers keep their value but jsonb may print them in a normalised form; types use `Integer`, `Long` or `BigDecimal` (D3).
- The stored value is the serialised record, never the request map, so unknown properties cannot reach the database.

### D7. Add flow, locks and position

`ItemService.create(teacherId, courseId, lessonId, partId, type, payloadJson)` with the type code and the validated payload as its JSON String (D4 step 4), `@Transactional`. The validation of D4 runs before it, in the controller through `ItemPayloadValidator`. In the service:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. The course row `for update`, scoped by teacher; no row gives 404.
3. The lesson row `for update`, scoped by course and teacher; no row gives 404.
4. The part row `for update`, scoped by lesson and teacher; no row gives 404. A part of another lesson or course of the same teacher is 404 for the same reason as a foreign part: the lookup is scoped by all the parents and the teacher together.
5. `count(*) from items where part_id = :partId and teacher_id = :teacherId` at or above `teachdash.items.max-per-part` (50, read with `@Value`) gives 409 `ITEM_LIMIT_REACHED` (a plain `ApiException`, no `errors`).
6. Insert as in D5: position is `coalesce(max(position), 0) + 1`, which is count + 1 because positions are dense.

`PartRepository` already has `lockCourse` and `lockLesson`; the part lock is new and lives in `ItemRepository` (one place). The lock order extends the one of `PartService`: user, course, lesson, then part. A part delete takes user, course and lesson, then deletes the part row and the cascade deletes the items, so no cycle forms: an add racing a part delete queues behind the user and course locks, and either runs first (its item is cascaded) or finds no part and gets 404 (R1-S10). The user row comes first because the `teacher_id` foreign key puts a KEY SHARE lock on it into every write, and `CourseService.rename` locks it before the course row. `countInPart` and the `max(position)` subquery are scoped by `teacher_id` as well as `part_id`. The part lock lives in `ItemRepository`; the course and lesson locks reuse `PartRepository`.

The user row lock already serialises all of one teacher's writes, so the race scenarios do not prove the part lock alone. The part lock stays for 404 scoping and for position correctness; R1-S10 (a part delete racing adds) is the scenario that covers the part lock, R1-S5 proves the lock chain as a whole. The deferrable unique constraint is a safety net: a violation at commit would be a bug and surfaces as a 500; no code is mapped. Reordering items (#112) uses the same locks and one range shift, which a deferrable constraint allows.

No write changes the `updated_at` of the part, the lesson or the course (R1-S3). Order of checks: CSRF and role (403), deactivated (401), malformed id (404), input (400: D4), unknown or foreign id (404), limit (409). Accepted race, as for parts: a deactivation in mid-request gives 401 from step 1 after input validation.

### D8. Tests and helpers

Tests extend `support.ApiTest`. Helpers: `createItem(teacherId, partId, type, payload, position)` (direct SQL, times from the test `Clock`; no `schema_version`), `countItems(partId)`, `itemPositions(partId)`, and a reader of one item row (type, payload as jsonb text, `version`). R1-S5 and R1-S10 use `support.Parallel.run`. The test type and its registry tests live in test sources. `ItemPayloadEdgeTest` is deleted. Unit tests without a scenario ID: duplicate codes fail the registry, the `test-note` view returns `{text}` only, strict `Integer` and `Long` parsing, `BigDecimal` precision with `USE_BIG_DECIMAL_FOR_FLOATS`, the composite foreign key.

### D9. Caddy body limit

`infra/Caddyfile` gets `request_body { max_size 1MB }` inside the existing `handle /api/*` block, before `reverse_proxy`. A larger body is refused by Caddy with 413 before the app sees it; the response is Caddy's own (not an app error body), and `handle_errors` leaves `/api` errors alone, so the 413 reaches the client as Caddy writes it. Constitution §6 describes only the deployment (Caddy, Compose, CI), so this is a one-line config change described here and in the proposal Impact. There is no backend scenario (the tests do not run Caddy); it is proven by `caddy validate` and one manual request with a body over 1 MB, described in the PR. The 413 has no JSON body. Dev and tests have no limit. This is the only body limit; uploads (#55) will get their own route limit. Caddy is not in the app's test stack and no service is added.

## Dependencies

None added. Jackson 3 and Postgres jsonb are already available.

## Risks / Trade-offs

- **Body size.** Bounded by field limits on every payload record (D3) and by the Caddy limit of 1 MB on `/api/*` (D9), the only server-level limit: its 413 has no JSON body, dev and tests are unbounded, and uploads (#55) will get their own route limit. A type whose record misses a `@Size` on a field is unbounded up to 1 MB; the reviewer checks every record.
- **Empty registry in production.** Until #111 every add gives `UNKNOWN_TYPE`; the endpoint is not useful to a user before then.
- **jsonb normalisation.** The response differs from the request in key order, whitespace and duplicate keys. Clients must not rely on key order. A type that needs an order uses an array.
- **Owner consistency.** Enforced for items by the composite foreign key (D1). Not for `parts` to `lessons`: left as is.
- **Limits are guesses.** 50 items per part, 1 MB per body and the field sizes are estimates; the first is a property.
- **Item type removal.** A type removed from code leaves rows with a type no registry knows. Later reads must treat an unknown stored type explicitly; no read exists in this change.

## Migrations

- `V17__items.sql`: unique `(id, teacher_id)` on `parts`, table `items` (no `schema_version`) with the composite foreign key, the two check constraints, the deferrable unique `(part_id, position)`, the index on `teacher_id`.
