# Proposal

## Clarifications

Issue #110. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Endpoint and response? | `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` with `{type, payload}`. Items have no title of their own; any title-like text lives in a type's payload. 201 with `{id, type, payload, position, version, createdAt, updatedAt}`, no `partId`, `lessonId`, `courseId` or `teacherId`. `payload` in the response is the stored payload as read back from the database. |
| 2 | Table? | Table `items`: `id uuid` primary key, `teacher_id` foreign key to `users` `on delete cascade`, `part_id` foreign key to `parts` `on delete cascade` (never `lesson_id` or `course_id`), `type varchar(50)`, `payload jsonb not null`, `schema_version int not null` (at least 1), `version bigint not null` (starts at 1; the later edit uses it for 409), `position int` at least 1 with `unique (part_id, position) deferrable initially deferred`, `created_at`, `updated_at`, an index on `teacher_id`. Same style as `V16__parts.sql`; the next number is `V17`, renumbered at merge if needed. |
| 3 | Type registry? | An `ItemType` interface with `code()`, `schemaVersion()`, `validate(payload)` (returns the normalised payload or field errors with field paths under `payload.`) and `studentProjection(payload)`. The projection is an abstract method, so a type cannot be registered without one (an allowlist: the projection builds a new tree, it does not strip fields). Types are Spring beans collected into a registry; duplicate codes fail startup. No production type is registered in this change; the first real type comes with #111. Tests use a test-only type in test sources (code `test-note`, payload `{text}` required, at most 1000 characters, projection returns `{text}` only and drops a teacher-only field such as `hint`). The projection is reachable from no endpoint here; a unit test covers it, not a scenario. |
| 4 | Generic payload rules? | All 400 `VALIDATION_FAILED` with field errors: `type` missing gives `REQUIRED`; an unknown `type` gives the new field code `UNKNOWN_TYPE`; `payload` missing or null gives `REQUIRED`; `payload` not a JSON object gives `INVALID_TYPE`; a payload whose serialisation is over 64 KiB (UTF-8 bytes) gives `payload` `TOO_LONG`; a NUL character in any string value or object key anywhere in the payload tree gives `payload` `INVALID_FORMAT` (`NulRejectingJsonConfig` covers typed strings only; see design D5). Then the type's own validator. The stored `schema_version` is the type's `schemaVersion()`. |
| 5 | Limit? | At most 50 items per part, property `teachdash.items.max-per-part=50`; the 51st gives 409 `ITEM_LIMIT_REACHED`, checked after the ownership 404. |
| 6 | Locks and position? | Lock order user row, course, lesson, part (`SELECT ... FOR UPDATE`, each scoped by `teacher_id` and the parent ids), then insert at the end (`coalesce(max(position), 0) + 1`), as `PartService`. Position is dense and 1-based. |
| 7 | Order of checks? | CSRF and role (403), deactivated (401), malformed id (404), input (400: the generic rules, then the type's validator), unknown, foreign or mismatched course, lesson or part (404, no `errors`, no data of another teacher in the body), limit (409). |
| 8 | JSONB handling? | No JPA converter. Write with `cast(:payload as jsonb)` from the string serialised by Jackson 3 (`tools.jackson`), read with `readTree`. The design notes that jsonb keeps no key order and no duplicate keys (the last wins) and drops whitespace, so the response shows the stored form, and why a String cast is used rather than `PGobject`. |
| 9 | Non-goals? | List, edit, reorder and delete items (#111, #112); answers (edit keeps old results, delete deletes answers: #126, #128; the `part_id` cascade is ready for it); student endpoints; a list-of-types endpoint; HTTP body size limits at the server level (noted as a risk). |
| 10 | Requirements and scenarios? | Only `items` R1 and R2. R1: a teacher adds an item to a part (endpoint, end position, limit, locks and parallel requests, scoping 404 at course, lesson and part level including a part of another lesson or course of the same teacher, deactivated 401, check order). R2: a payload follows the generic rules and its type's rules (unknown type, required, not an object, the 64 KiB boundary, NUL in a value and in a key, type validator errors with the path `payload.text`, `schema_version` stored). Scenarios cover `en` and `ru` content: Cyrillic payload text is stored and returned exactly. About 20 scenarios. |
| 11 | After the spec critique? | `api-errors` R1 is MODIFIED (`UNKNOWN_TYPE` in the same priority tier as `INVALID_FORMAT`; scenario R1-S11). `common` gets a public `ApiException` factory for a list of field errors and a public constant for `UNKNOWN_TYPE` (design D3). Scenarios added: both `type` and `payload` missing, blank and wrongly typed `type`, extra body fields ignored (the item is owned by the caller), a payload over the limit without `text` gives only `TOO_LONG`. R1-S5 claims only that the lock chain serialises; R1-S10 covers the part lock. Owner consistency is enforced in the schema: unique `(id, teacher_id)` on `parts` and a composite foreign key from `items`; the same gap from `parts` to `lessons` stays, out of scope. The missing server-level body limit stays an accepted risk. |

The change has 20 scenarios: `items` R1 10, R2 9, and `api-errors` R1-S11.

## Research angles

The engineer agreed the research was done before the brief. Conclusions:

- Persistence is `JdbcClient` with hand-written SQL and records as the domain, as `PartService` and `PartRepository` do: `ApiException.notFound()`, `ApiException.field(...)`, field codes in `common/FieldErrorCodes.java`, the lock helper `PartService.lockForWrite`, limits through `@Value`.
- There is no JSONB use in the code yet; Jackson is version 3. Untyped trees (`JsonNode`) bypass the String deserializer, so a NUL in a payload would reach Postgres and give a 500 today.
- Tests extend `support.ApiTest` (`createTeacher`, `createCourse`, `createLesson`, `createPart`; new `createItem`, `countItems`, `itemPositions`), `@DisplayName("[items/R1-S1] ...")`, `support.Parallel.run`. A test-only bean in test sources is found by component scan (precedent: `common/QueryParamTestController`).

## Why

A part is a container; its content is made of items (exercises, text blocks and so on). The item table, the way a payload is stored and checked, and the type registry must exist before the first real item type (#111), editing and reordering (#112) and answers (#126). This change adds the foundation and the one write, add an item to an own part, proven with a test-only type.

## What Changes

- Table `items` (migration `V17__items.sql`).
- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}/items` adds an item at the end of the caller's part (R1) after the payload rules (R2).
- An `ItemType` interface and a registry; no production type.
- New field codes `UNKNOWN_TYPE` (400) and the response code `409 ITEM_LIMIT_REACHED`.
- Property `teachdash.items.max-per-part=50`.

## Capabilities

### New Capabilities
- `items`: items owned by a teacher, ordered inside a part (R1 add, R2 payload rules).

### Modified Capabilities
- `api-errors`: R1 (the priority sentence names `UNKNOWN_TYPE`; new scenario R1-S11).

## Out of scope

- Listing, editing, reordering and deleting items (#111, #112).
- Answers and results: an item edit keeps old results, an item delete deletes its answers (#126, #128). The `part_id` foreign key with `on delete cascade` is ready for it.
- Student endpoints and the student projection endpoint.
- An endpoint that lists item types.
- Real item types (the first comes with #111).
- HTTP body size limits at the server level (a risk, see design).
- Frontend and OpenAPI.

## Impact

- Backend: new `items` feature package (`api`, `api/dto`, `domain` with `ItemType` and the registry, `persistence`); a new field code in `common/FieldErrorCodes`; one property in `application.properties`.
- Database: migration `V17__items.sql` (table `items`, plus a unique `(id, teacher_id)` on `parts`), renumbered at merge if needed.
- API: one new endpoint, one new response code, one new field code `UNKNOWN_TYPE` (`api-errors` R1 MODIFIED).
- No new dependencies.
