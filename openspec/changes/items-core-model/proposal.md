# Proposal

## Clarifications

Issue #110. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Endpoint and response? | `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` with `{type, payload}`. Items have no title of their own; any title-like text lives in a type's payload. 201 with `{id, type, payload, position, version, createdAt, updatedAt}`, no `partId`, `lessonId`, `courseId` or `teacherId`. `payload` in the response is the stored payload as read back from the database. |
| 2 | Table? | Table `items`: `id uuid` primary key, `teacher_id` foreign key to `users` `on delete cascade`, `part_id` foreign key to `parts` `on delete cascade` (never `lesson_id` or `course_id`), `type varchar(50) not null` with `check (btrim(type) <> '')`, `payload jsonb not null` with `check (jsonb_typeof(payload) = 'object')`, `version bigint not null` (starts at 1; the later edit uses it for 409), `position int` at least 1 with `unique (part_id, position) deferrable initially deferred`, `created_at`, `updated_at`, an index on `teacher_id`. Same style as `V16__parts.sql`; the next number is `V17`, renumbered at merge if needed. Revised by rework 3: no `schema_version` column; `V17__items.sql` is edited in place (not merged to main). |
| 3 | Type registry? | Superseded by rework 2. Kept from the original: types are Spring beans collected into a registry, duplicate codes fail startup, no production type is registered (the first real type comes with #111), the student view is mandatory (an allowlist: a type cannot be registered without one), the projection is reachable from no endpoint here (a unit test covers it, not a scenario). Tests use a test-only type `test-note` (rework 13). |
| 4 | Generic payload rules? | Revised by reworks 1, 4, 6, 7, 8. All 400 `VALIDATION_FAILED` with field errors: `type` missing or blank gives `REQUIRED`; an unknown `type` gives `UNKNOWN_TYPE`; `payload` missing or null gives `REQUIRED`; `payload` not a JSON object gives `INVALID_TYPE`. Then the payload is converted to the type's record and validated (rework 4): Jackson errors and Bean Validation violations are `payload.<path>` fields, an unknown property is `UNKNOWN_FIELD`, then the type's `extraChecks`. The serialised 64 KiB check and the hand-written NUL walk are gone (reworks 7, 8). |
| 5 | Limit? | At most 50 items per part, property `teachdash.items.max-per-part=50`; the 51st gives 409 `ITEM_LIMIT_REACHED`, checked after the ownership 404. |
| 6 | Locks and position? | Lock order user row, course, lesson, part (`SELECT ... FOR UPDATE`, each scoped by `teacher_id` and the parent ids), then insert at the end (`coalesce(max(position), 0) + 1`), as `PartService`. Position is dense and 1-based. |
| 7 | Order of checks? | CSRF and role (403), deactivated (401), malformed id (404), input (400: the generic rules, then the type's validator), unknown, foreign or mismatched course, lesson or part (404, no `errors`, no data of another teacher in the body), limit (409). |
| 8 | JSONB handling? | Revised by rework 5. No JPA converter. The stored value is the validated record serialised by the app's `ObjectMapper` (`writeValueAsString`), passed to the repository as a String and written with `cast(:payload as jsonb)`; read back with `readValue(json, payloadClass)`. The design notes that jsonb keeps no key order and no duplicate keys (the last wins) and drops whitespace, so the response shows the stored form, and why a String cast is used rather than `PGobject`. |
| 9 | Non-goals? | List, edit, reorder and delete items (#111, #112); answers (edit keeps old results, delete deletes answers: #126, #128; the `part_id` cascade is ready for it); student endpoints; a list-of-types endpoint; HTTP body size limits at the server level (noted as a risk). |
| 10 | Requirements and scenarios? | Only `items` R1 and R2. R1: a teacher adds an item to a part (endpoint, end position, limit, locks and parallel requests, scoping 404 at course, lesson and part level including a part of another lesson or course of the same teacher, deactivated 401, check order); R1-S1..S10 stay. R2 (revised by the rework): a payload follows the generic rules and its type's rules (unknown type, required, not an object, an unknown property, NUL and a lone surrogate in a value, NUL in a key, type validator errors with the path `payload.text`, the stored value is the serialised record). R2-S4 (the 64 KiB boundary) is dropped; its ID is not reused. Scenarios cover `en` and `ru` content: Cyrillic payload text is stored and returned exactly. |
| 11 | After the spec critique? | `api-errors` R1 is MODIFIED (`UNKNOWN_TYPE` in the same priority tier as `INVALID_FORMAT`; scenario R1-S11). Scenarios added: both `type` and `payload` missing, blank and wrongly typed `type`, extra body fields ignored (the item is owned by the caller). Owner consistency is enforced in the schema: unique `(id, teacher_id)` on `parts` and a composite foreign key from `items`; the same gap from `parts` to `lessons` stays, out of scope. Revised by reworks 8 and 11: `common` gets the public helpers `ApiException.fromJackson` and `ApiException.validation` (design D3) in place of the earlier list factory alone, and the body size is bounded by field limits and a Caddy limit (rework 7), no longer an accepted risk. |

Rework after the final review (the archive was reverted to rework the payload design). Numbers R1 to R13 are the engineer's decisions, as given; they supersede rows 3, 4, 8, 10 and 11 where those say otherwise:

| # | Question | Engineer's answer |
|---|---|---|
| R1 | Request body? | `CreateItemRequest(String type, Map<String,Object> payload)`. Missing or null payload gives `payload` `REQUIRED`; a non-object gives `payload` `INVALID_TYPE`; a blank `type` gives `type` `REQUIRED`; an unknown `type` gives `type` `UNKNOWN_TYPE`. The existing behaviour of items R2-S1..S3 and api-errors R1-S11 keeps. |
| R2 | Item types? | Each item type is a Spring bean `ItemType<P>`: `String code()`, `Class<P> payloadClass()` (a record with Bean Validation annotations), `studentView(P)` typed, and `default List<FieldErrorDetail> extraChecks(P)` for cross-field rules. Types are found by adding beans; the registry fails on duplicate codes (keeps). |
| R3 | Payload shapes and the schema? | A new payload shape is a new type code; types never change after release. The `schema_version` column and `schemaVersion()` are dropped. `V17__items.sql` is edited in place (not merged to main). Both check constraints are kept (`btrim(type) <> ''`, `jsonb_typeof(payload) = 'object'`) and design D1 states them. |
| R4 | Validation? | A shared `ItemPayloadValidator` in `items/domain`: `<P> P validate(ItemType<P>, Map<String,Object>)`. `objectMapper.convertValue` to the record; Jackson errors map to `payload.<path>` fields; `jakarta.validation.Validator` violations are prefixed `payload.`; then `extraChecks`. Reusable for the item edit (#111). `ItemController` becomes a few lines. Input 400 still comes before the service's 404 and 409. |
| R5 | Stored value? | The validated record serialised by the app's `ObjectMapper` (`writeValueAsString`), passed to the repository as the String cast to jsonb. Reading later: `readValue(json, payloadClass)`. |
| R6 | Unknown properties? | Unknown properties in the payload are rejected (our frontend sending them is a bug): new field code `UNKNOWN_FIELD`, field `payload.<name>`, 400 `VALIDATION_FAILED`. Scope: payload only, not a global `FAIL_ON_UNKNOWN_PROPERTIES` change. Added to the `api-errors` R1 code list with its priority and a scenario. |
| R7 | Size? | The hand-written payload tree walk and the serialised 64 KiB check are removed (R2-S4 and the size parts of R2 dropped). Size is bounded by `@Size` or `@MaxUtf8Bytes` on every payload record field and list, plus Caddy `request_body { max_size 1MB }` on `/api/*` in `infra/Caddyfile`. An oversized body gets 413 as Caddy gives it, before the app. Constitution §6 only describes Caddy deployment, so the Caddyfile change is described in design D9 and Impact and proven by a task (no backend scenario). |
| R8 | NUL and surrogates? | A NUL in a record String field is already rejected by the global string deserializer: `INVALID_FORMAT`, field is the Jackson path (for example `payload.text`); R2-S5 rewritten. A NUL in a key is an unknown property: `UNKNOWN_FIELD`; R2-S6 rewritten. A lone surrogate is rejected globally in `NulRejectingJsonConfig`'s string deserializer: `INVALID_FORMAT`, on all endpoints. |
| R9 | Numbers? | `Double` and `Float` fields are forbidden in payload records (design rule); numbers use `Integer`, `Long` or `BigDecimal`. |
| R10 | Strict integers? | Pre-existing gap fixed: `StrictIntegerJsonConfig` is strict only for `BigInteger`; `Integer` and `Long` accept "5" and coerce 5.7 to 5. They become strict globally (`INVALID_TYPE`). This affects all endpoints (Impact). |
| R11 | Helpers in `common`? | Two public helpers: `ApiException.fromJackson(DatabindException, String prefix)` (extracted from `ApiExceptionHandler.fieldOf` and `jsonCodeOf`, including NUL detection) and `ApiException.validation(Set<ConstraintViolation<?>>, String prefix)`. `FieldErrorCodes` is otherwise unchanged and stays public in `common`. |
| R12 | Record rules? | Payload records use wrapper types with `@NotNull` (no primitives, so a missing field gives `REQUIRED`) and no class-level constraints (cross-field rules go in `extraChecks`). |
| R13 | Test type? | `test-note`: a record with `@NotNull @Size(max=1000) String text` and an optional `hint` with a declared limit (`@Size(max=200)`, a value chosen by the spec-writer, to confirm at the spec gate). R1-S1..S10 stay. R2-S9 (duplicate keys, last wins) is rewritten without `schema_version`: the stored value is the serialised record. |

Second rework, after the spec critique of the rework. The engineer's decisions, as given; they supersede R1 to R13 and rows 1 to 11 where those say otherwise (R8: a NUL in a key is `UNKNOWN_FIELD` with the field `payload`; R9: `BigDecimal` is read exactly):

| # | Question | Engineer's answer |
|---|---|---|
| R14 | Where does `UNKNOWN_FIELD` sit? | In the default priority tier next to `INVALID_FORMAT` and `UNKNOWN_TYPE`. |
| R15 | `hint` limit of `test-note`? | `@Size(max=200)`, kept. |
| R16 | Strict `Integer` and `Long`? | No scenario of its own; the rule is in `api-errors` R1 text plus a unit test. Kept. |
| R17 | Proof of the Caddy 413? | `caddy validate` plus one manual request with a body over 1 MB, described in the PR. Kept. |
| R18 | Field of `UNKNOWN_FIELD`? | Always `payload`, never the property name: the key is user text and must not be echoed. Items R2-S6 and R2-S10 and `api-errors` R1-S12 are rewritten; no escaped-NUL key comparison. |
| R19 | Several errors? | Jackson stops at the first unknown or mistyped property, so one such error is reported; Bean Validation errors are reported together. Kept and stated in the design. |
| R20 | Stored payload and nulls? | The payload is serialised without null fields (`JsonInclude.Include.NON_NULL` for the stored payload), so an absent `hint` is not stored as `"hint": null`. Items R2-S9 and design D5. |
| R21 | A payload that is not an object? | It fails while Spring binds the body to `Map`, before the controller: the one error `payload` `INVALID_TYPE`, and `type` is not checked. The combined case is dropped from R2-S1 and the "reported together" sentence from D2 and D4. A blank or unknown `type` with a valid object payload works as before. `payload: ""` is expected to give `INVALID_TYPE` (R2-S3). |
| R22 | Floats? | `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` is enabled globally (the body is bound to `Map` by the app's `ObjectMapper`; it changes only untyped `Object` and `Map` floats). Payload records may use `BigDecimal` for fractions; `Double` and `Float` stay forbidden. `1e400` stays a `BigDecimal` and is stored in jsonb as is; into an `Integer` or `Long` field it, like `1.5`, is `payload.<field>` `INVALID_TYPE`. Scenario R2-S12 and a unit test; `test-note` gets the optional fields `Integer rating` and `BigDecimal weight` for it. |
| R23 | Body size? | Only the Caddy 1 MB limit on `/api/*`; the 413 has no JSON body; dev and tests are unbounded; uploads (#55) get their own route limit. Written in design D9 and Risks. |

The change has 24 scenarios: `items` R1 10, R2 11 (R2-S1 to S3, S5 to S12; S4 is dropped), and `api-errors` R1-S11 to R1-S13.

## Research angles

The engineer agreed the research was done before the brief. Conclusions:

- Rework research (Jackson 3.1.7, Boot 4.1.2, Hibernate Validator 9.1.5, checked in a scratch experiment): the custom String deserializer applies to Map values and to `convertValue` into records, not to Map keys; `convertValue` throws `DatabindException` subclasses with real property paths (`answers[1].correct`), a non-object gives an empty path, missing creator arguments become null; Bean Validation paths are `question` and `answers[0].text` (needs `List<@Valid X>`), `NotNull`/`NotBlank` give `REQUIRED`, `Size` gives `TOO_SHORT`/`TOO_LONG`, the rest `INVALID_FORMAT`; the parser accepts `\ud800` but jsonb rejects the escaped form (500, from memory), hence the global surrogate check; no body size limit exists anywhere today; `1e400` becomes a `Double` Infinity, hence R9; `""` into an `Integer` becomes null; records need `-parameters` (already on).
- Persistence is `JdbcClient` with hand-written SQL and records as the domain, as `PartService` and `PartRepository` do: `ApiException.notFound()`, `ApiException.field(...)`, field codes in `common/FieldErrorCodes.java`, the lock helper `PartService.lockForWrite`, limits through `@Value`.
- There is no JSONB use in the code yet; Jackson is version 3. (Superseded by the rework: the payload is a `Map` converted to a record, so the String deserializer applies.)
- Tests extend `support.ApiTest` (`createTeacher`, `createCourse`, `createLesson`, `createPart`; new `createItem`, `countItems`, `itemPositions`), `@DisplayName("[items/R1-S1] ...")`, `support.Parallel.run`. A test-only bean in test sources is found by component scan (precedent: `common/QueryParamTestController`).

## Why

A part is a container; its content is made of items (exercises, text blocks and so on). The item table, the way a payload is stored and checked, and the type registry must exist before the first real item type (#111), editing and reordering (#112) and answers (#126). This change adds the foundation and the one write, add an item to an own part, proven with a test-only type.

## What Changes

- Table `items` (migration `V17__items.sql`, no `schema_version`).
- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` adds an item at the end of the caller's part (R1) after the payload rules (R2).
- A generic `ItemType<P>` interface, a registry and a shared `ItemPayloadValidator`; no production type.
- Strict `Integer` and `Long` parsing, a lone-surrogate check and `USE_BIG_DECIMAL_FOR_FLOATS` for all endpoints; public `ApiException.fromJackson` and `ApiException.validation` helpers.
- A 1 MB request body limit on `/api/*` in `infra/Caddyfile`.
- New field codes `UNKNOWN_TYPE` and `UNKNOWN_FIELD` (400) and the response code `409 ITEM_LIMIT_REACHED`.
- Property `teachdash.items.max-per-part=50`.

## Capabilities

### New Capabilities
- `items`: items owned by a teacher, ordered inside a part (R1 add, R2 payload rules).

### Modified Capabilities
- `api-errors`: R1 (the priority sentence names `UNKNOWN_TYPE` and `UNKNOWN_FIELD`; `Integer` and `Long` are strict; a lone surrogate is `INVALID_FORMAT`; new scenarios R1-S11 to R1-S13).

## Out of scope

- Listing, editing, reordering and deleting items (#111, #112).
- Answers and results: an item edit keeps old results, an item delete deletes its answers (#126, #128). The `part_id` foreign key with `on delete cascade` is ready for it.
- Student endpoints and the student projection endpoint.
- An endpoint that lists item types.
- Real item types (the first comes with #111).
- Frontend and OpenAPI.

## Impact

- Backend: new `items` feature package (`api`, `api/dto`, `domain` with `ItemType<P>`, the registry and `ItemPayloadValidator`, `persistence`); new field codes in `common/FieldErrorCodes`; two public helpers on `ApiException`; one property in `application.properties`.
- Behaviour change for all endpoints: `Integer` and `Long` body fields reject a string or a fraction with `INVALID_TYPE` (before, "5" was accepted and 5.7 became 5); a lone surrogate in any string is `INVALID_FORMAT`; `USE_BIG_DECIMAL_FOR_FLOATS` is enabled globally, so a JSON float bound to an untyped `Object` or `Map` is a `BigDecimal` and not a `Double` (typed fields are unchanged). No existing endpoint has an `Integer` or `Long` body field today (moves use `BigInteger`), so no current client changes.
- Infra: `infra/Caddyfile` gets `request_body { max_size 1MB }` in the `/api/*` handle; a larger body gets 413 from Caddy with no JSON body (the only body limit; dev and tests are unbounded).
- Database: migration `V17__items.sql`, edited in place (table `items` without `schema_version`, plus a unique `(id, teacher_id)` on `parts`), renumbered at merge if needed.
- API: one new endpoint, one new response code, new field codes `UNKNOWN_TYPE` and `UNKNOWN_FIELD` (`api-errors` R1 MODIFIED), 413 for a body over 1 MB.
- No new dependencies.
