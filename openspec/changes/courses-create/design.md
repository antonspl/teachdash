# Design: courses-create

## Context

See proposal.md - Why. Current state on main:

- No course or language code exists. Migrations end at V8.
- Feature packages follow constitution §3, e.g. `enrolment/{api,api/dto,domain,persistence}`.
  Controllers read the teacher id with `UUID.fromString(principal.getName())`; services
  start with `UserRepository.findActiveByIdForUpdate(teacherId)`, which throws 401
  `UNAUTHENTICATED` for a deactivated teacher and serialises all writes for one teacher.
- Role gates are URL-prefix rules in `common/SecurityConfig`: `/api/teacher/**` needs
  TEACHER, everything else outside the role prefixes only needs a session.
- Validation: one composed annotation per field in `common/validation/` (e.g. `@PersonName`
  = `@NotBlank` + `@Size(max = 200)` + a letter-or-digit `@Pattern`); request records
  normalise in their compact constructor with `Normalize.name`. `FieldErrorCodes` maps
  `NotBlank` to `REQUIRED`, `Size` to `TOO_SHORT`/`TOO_LONG` and anything else to
  `INVALID_FORMAT`.
- Duplicates: a unique index plus `catch (DuplicateKeyException)` mapped to a 409 code;
  `ApiException.field(status, code, field)` adds the `errors` entry. Case-insensitive
  uniqueness uses `unique index ... (lower(email))` (V1).
- Tests: `ApiTest` (Postgres 17 Testcontainer, truncates all tables except
  `flyway_schema_history` before each test), `loggedInAs`, `jdbc`, `Parallel.run`.

## Goals / Non-Goals

**Goals:** a language list the database enforces; course creation that keeps one rule per
field, stays correct under parallel requests and is scoped to the caller.

**Non-Goals:** reading, renaming or deleting courses; per-language settings; frontend.

## Decisions

### D1. Languages live in a table seeded by Flyway

`languages (code varchar(2) primary key, check (code ~ '^[a-z]{2}$'))`, seeded with
`en` and `ru` in the same migration. Course language columns are foreign keys to it, so
the database refuses an unknown code and blocks removing a language that courses use.
A new language is a new migration (reviewed in a PR, deployed by CI).

Alternatives: a `teachdash.languages` property is simpler but lets stored codes drift
from the list; a Java enum hard-codes languages (constitution §9). Per-language settings
(#127) can later be configuration keyed by code, with a startup check that every row has
settings; that is #127's decision.

Seeding is data in a migration. ApiTest truncation must keep the seed: add `languages`
to the tables that `ApiTest` does not truncate, and after the truncate run
`delete from languages where code not in ('en', 'ru')`. Courses are already gone by then
(truncating `users` cascades to `courses`), so the foreign key does not block it. The reset keeps tests independent if a later change inserts
another language. The test helper names `en` and `ru`; production code does not.

Languages are never removed (a removal would be a migration that must first deal with the
courses using it). So a language that disappears between validation (D4) and insert is
not handled; it would surface as a 500.

### D2. Migration `V9__courses.sql`

```sql
create table languages (
    code varchar(2) primary key check (code ~ '^[a-z]{2}$')
);
insert into languages (code) values ('en'), ('ru');

create table courses (
    id                   uuid        primary key,
    teacher_id           uuid        not null references users (id) on delete cascade,
    title                varchar(200) not null,
    instruction_language varchar(2)  not null references languages (code),
    target_language      varchar(2)  references languages (code),
    created_at           timestamptz not null
);
create unique index courses_teacher_title_uq on courses (teacher_id, lower(title));
```

The unique index also serves lookups and counts by `teacher_id`. Table names are plural
like `users`; #106 should follow (`lessons`, `lesson_courses`). The version is set at
merge (#98). This change adds no other migration.

`lower()` follows the database ctype. The `postgres` image defaults to `en_US.utf8`, which
folds Cyrillic; R2-S9 and api-errors/R2-S6 check Cyrillic on the test container. Prod
uses the same image.

### D3. Endpoints and packages

- `com.teachdash.languages` (`api`, `persistence`): `GET /api/languages` returns
  `List<LanguageResponse(code)>` ordered by code. `SecurityConfig` adds
  `requestMatchers(GET, "/api/languages").hasAnyRole("TEACHER", "ADMIN")` before the
  generic `authenticated()` rule; students get 403 `FORBIDDEN` through the existing
  denied handler. Languages are cross-feature (later answers, translation), hence their
  own package.
- `com.teachdash.courses` (`api`, `api/dto`, `domain`, `persistence`):
  `POST /api/teacher/courses`, `@ResponseStatus(CREATED)`, body
  `CreateCourseRequest(title, instructionLanguage, targetLanguage)`, response
  `CourseResponse(id, title, instructionLanguage, targetLanguage, createdAt)`.
  Unknown body fields such as `teacherId` are ignored (Spring Boot default); the owner is
  always the principal (R2-S12).

### D4. Validation

- `@CourseTitle` in `common/validation/`: `@NotBlank`, `@Size(max = 200)` and the same
  letter-or-digit pattern as `@PersonName`. `CreateCourseRequest` normalises the title
  with `Normalize.name`. `CourseTitle.MAX_LENGTH` is reused by tests; the column length
  mirrors it.
- `@LanguageCode` in `languages/api` (public, used by courses): a `ConstraintValidator`
  that accepts `null` and otherwise requires a code present in `languages`. It reads the
  table through `LanguageRepository` (Spring injects validator dependencies). Any other
  value, including "", "EN" and "eng", falls through to `INVALID_FORMAT`.
  `instructionLanguage` adds `@NotNull` (`REQUIRED`).
- Because both rules are Bean Validation, all failing fields come back in one response
  (R2-S8), in the order api-errors R1 defines. Rejected values are never echoed.

A per-request query of a two-row table is cheap. A cache is not added: it would need
invalidation when a migration adds a language.

### D5. Create flow and errors

`CourseService.create(teacherId, request)`, `@Transactional`:

1. `users.findActiveByIdForUpdate(teacherId)` — 401 if deactivated; locks the teacher row.
2. `courses.countByTeacher(teacherId) >= maxPerTeacher` → `409 COURSE_LIMIT_REACHED`
   (plain `ApiException`, no `errors`). This runs before the insert, so a teacher at the
   limit gets the limit error even for a taken title (R2-S10).
3. Insert with a new UUID and `clock` time through an explicit insert query (as in
   `TeacherStudentRepository`), not `save()`: Spring Data JDBC treats an entity with a
   preassigned id as existing and would issue an UPDATE. `DuplicateKeyException` on
   `courses_teacher_title_uq` → `ApiException.field(CONFLICT, "TITLE_TAKEN", "title")`.

Bean Validation runs before the service, so the order is: input (400), limit (409
`COURSE_LIMIT_REACHED`), title (409 `TITLE_TAKEN`). R2-S10 covers both steps.

The row lock makes step 2 safe under parallel requests (R2-S11); the unique index covers
the title race even without it. `ApiExceptionHandler` / the api-errors R2 allow-list gets
`TITLE_TAKEN`.

`maxPerTeacher` comes from `teachdash.courses.max-per-teacher=20` in
`application.properties`, read with `@Value`. It is not an environment variable: it is
not a secret and changes through a PR. Raising it is safe; lowering it leaves teachers
above the limit, who then simply cannot create more.

### D6. Languages are immutable

No endpoint in this change updates a course. #105 (rename) must accept only the title.
No database trigger is added: the rule is enforced by the API surface and by #105's spec.

## Dependencies

None added.

## Risks / Trade-offs

- **Seed data vs. test truncation.** If `ApiTest` truncates `languages`, every test that
  creates a course fails with a foreign key error. D1 excludes it and resets it to the
  seed before each test.
- **Collation.** If prod Postgres ever runs with the `C` ctype, `lower()` stops folding
  Cyrillic and "Грамматика"/"ГРАММАТИКА" become distinct. Covered by tests on the same
  image; noted for any database move.
- **Unicode forms.** Titles are not NFC-normalised, so "й" typed as one code point and as
  "и" + combining breve are different titles. `Normalize.name` does not fold forms for
  names either; changing that is a separate change for both fields.
- **Limit is a guess.** 20 is from angle D's estimate, not measured. Capacity really
  depends on file limits in #55.
