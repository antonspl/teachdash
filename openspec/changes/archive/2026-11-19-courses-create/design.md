# Design: courses-create

## Context

See proposal.md - Why. Current state on main:

- No course code exists. Migrations end at V10.
- `languages (code, content)` exists (V9), seeded with ten codes; `en` and `ru` have
  `content` true. `com.teachdash.languages` has `LanguageRepository` (`findAll`, `exists`),
  `GET /api/languages` and `@NativeLanguage` (`@NotBlank` plus `NativeLanguageValidator`,
  which accepts `null` and otherwise needs a 2-character listed code). Requests strip the
  code with `Normalize.strip` first.
- `ApiTest` already keeps `languages` out of the truncation.
- Feature packages follow constitution §3, e.g. `enrolment/{api,api/dto,domain,persistence}`.
  Controllers read the teacher id with `UUID.fromString(principal.getName())`; services
  start with `UserRepository.findActiveByIdForUpdate(teacherId)`, which throws 401
  `UNAUTHENTICATED` for a deactivated teacher and serialises all writes for one teacher.
- Role gates are URL-prefix rules in `common/SecurityConfig`: `/api/teacher/**` needs
  TEACHER.
- Validation: one composed annotation per field in `common/validation/` (e.g. `@PersonName`
  = `@NotBlank` + `@Size(max = 200)` + a letter-or-digit `@Pattern`). `FieldErrorCodes`
  maps the constraint type to a code: `NotBlank`/`NotNull` to `REQUIRED`, `Size` to
  `TOO_SHORT`/`TOO_LONG`, anything else to `INVALID_FORMAT`.
- Duplicates: a unique index plus `catch (DuplicateKeyException)` mapped to a 409 code;
  `ApiException.field(status, code, field)` adds the `errors` entry.

## Goals / Non-Goals

**Goals:** course creation that uses the language list, keeps one rule per field, stays
correct under parallel requests and is scoped to the caller.

**Non-Goals:** reading, renaming or deleting courses; changes to the language list;
per-language settings; frontend.

## Decisions

### D1. Migration `V11__courses.sql`

```sql
create table courses (
    id                   uuid         primary key,
    teacher_id           uuid         not null references users (id) on delete cascade,
    title                varchar(200) not null,
    instruction_language varchar(2)   not null references languages (code),
    target_language      varchar(2)   references languages (code),
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null
);
create unique index courses_teacher_title_uq on courses (teacher_id, lower(title));
```

The foreign keys refuse unknown codes and block removing a language that courses use.
The `content` flag is checked by the API only (D3), not by the database: turning a flag
off later must not break existing courses. The unique index also serves lookups and
counts by `teacher_id`. Table names are plural like `users`; #106 should follow
(`lessons`, `lesson_courses`). The version is set at merge (#98).

`lower()` follows the database ctype. The `postgres` image defaults to `en_US.utf8`, which
folds Cyrillic; R1-S11 and api-errors/R2-S6 check Cyrillic on the test container. Prod
uses the same image.

### D2. Endpoint and package

`com.teachdash.courses` (`api`, `api/dto`, `domain`, `persistence`):
`POST /api/teacher/courses`, `@ResponseStatus(CREATED)`, body
`CreateCourseRequest(title, instructionLanguage, targetLanguage)`, response
`CourseResponse(id, title, instructionLanguage, targetLanguage, createdAt, updatedAt)`. Unknown body
fields such as `teacherId` are ignored (Spring Boot default); the owner is always the
principal (R1-S14). No `SecurityConfig` change: `/api/teacher/**` already needs TEACHER.

### D3. Validation

- `@CourseTitle` in `common/validation/`: `@NotBlank`, `@Size(max = 200)` and the same
  letter-or-digit pattern as `@PersonName`. `CourseTitle.MAX_LENGTH` is reused by tests;
  the column length mirrors it.
- Language codes, in `languages/api/validation`:
  - `@ListedLanguage`: the current `NativeLanguageValidator` check (`null` passes;
    otherwise a 2-character listed code), reported as `INVALID_FORMAT`.
    `@NativeLanguage` becomes `@NotBlank` + `@ListedLanguage`, so student languages keep
    their behaviour.
  - `@ContentLanguage`: passes for `null`, for a code that is not listed (that is
    `@ListedLanguage`'s failure) and for a listed code with `content` true. It fails only
    for a listed code with `content` false. `FieldErrorCodes` maps it to
    `UNSUPPORTED_COURSE_LANGUAGE`. `common.FieldErrorCodes` thus imports
    `languages.api.validation.ContentLanguage`; this is intentional (`common` already imports
    `accounts`) and simpler than carrying the code in the constraint payload.
  - `@CourseLanguage` = `@ListedLanguage` + `@ContentLanguage`. `targetLanguage` uses it
    alone; `instructionLanguage` adds `@NotBlank`.
  - `languages.domain.LanguageCatalog` holds the languages with their flags and offers
    `find(code)` and `all()`; the validators and `GET /api/languages` use it.
- `CreateCourseRequest` normalises in its compact constructor: the title with
  `Normalize.name`, both codes with `Normalize.strip` (as `nativeLanguage`). "" and "  "
  therefore give `REQUIRED` for the instruction language and `INVALID_FORMAT` for the
  target language.
- At most one language rule fails per field, so the existing priority in
  `FieldErrorCodes` (`REQUIRED` first) is enough; `UNSUPPORTED_COURSE_LANGUAGE` ranks with
  `INVALID_FORMAT`.
- All rules are Bean Validation, so all failing fields come back in one response
  (R1-S10), sorted as api-errors R1 defines. Rejected values are never echoed.

`LanguageCatalog` loads the table once at startup, after the Flyway migrations, and never
invalidates it. A new or changed language needs a migration and a deploy, which restarts
the app. Validation therefore costs no database query, and any odd string is a plain map
miss.

### D4. Create flow and errors

`CourseService.create(teacherId, title, instructionLanguage, targetLanguage)`
(plain values; the controller maps the request), `@Transactional`:

1. `users.findActiveByIdForUpdate(teacherId)` — 401 if deactivated; locks the teacher row.
2. `courses.countByTeacher(teacherId) >= maxPerTeacher` → `409 COURSE_LIMIT_REACHED`
   (plain `ApiException`, no `errors`). This runs before the insert, so a teacher at the
   limit gets the limit error even for a taken title (R1-S12).
3. Insert with a new UUID, and `created_at` and `updated_at` both set to one `clock`
   time, through an explicit insert query (as in
   `TeacherStudentRepository`), not `save()`: Spring Data JDBC treats an entity with a
   preassigned id as existing and would issue an UPDATE. A `DuplicateKeyException` whose
   `PSQLException` cause reports the constraint name `courses_teacher_title_uq`
   (`getServerErrorMessage().getConstraint()`, not a message search) → `ApiException.field(CONFLICT, "TITLE_TAKEN", "title")`.

Bean Validation runs before the service, so the order is: input (400), limit (409
`COURSE_LIMIT_REACHED`), title (409 `TITLE_TAKEN`). R1-S12 covers both steps.

The row lock makes step 2 safe under parallel requests (R1-S13); the unique index covers
the title race even without it. Only a violation of `courses_teacher_title_uq` becomes
`TITLE_TAKEN`; any other duplicate is rethrown. `TITLE_TAKEN` carries `errors` because it
is built with `ApiException.field`; there is no central allow-list, so api-errors R2 holds
by only using `field` for the codes it names.

`maxPerTeacher` comes from `teachdash.courses.max-per-teacher=20` in
`application.properties`, read with `@Value`. It is not an environment variable: it is
not a secret and changes through a PR. Raising it is safe; lowering it leaves teachers
above the limit, who then simply cannot create more.

### D5. `updated_at`

The first `updated_at` in the schema. It is set by the application from `clock`, like
`created_at`, not by a database trigger, so tests control it with `MutableClock`. Every
later change to a course row (rename in #105) MUST set it; #105's spec states that.
Changes to lessons do not touch it: lessons have their own rows.

### D6. Languages are immutable

No endpoint in this change updates a course. #105 (rename) must accept only the title.
No database trigger is added: the rule is enforced by the API surface and by #105's spec.

### D7. Tests rely on the seeded flags

R1-S9, R1-S10 and api-errors R1-S10 use the seeded `content` false for "de" and "uk"; no
test switches a flag (the engineer chose not to test changing the list). A validator
that hard-coded `en`/`ru` would also pass them; constitution §9 forbids that, and the
reviewers check it.

## Dependencies

None added.

## Risks / Trade-offs

- **Validation before the active-account check.** A deactivated teacher with a live
  session who sends an invalid body gets 400, not 401; same as the enrolment endpoints.
- **Content flag turned off later.** Existing courses keep that language; only new
  courses are refused. Whether such courses need handling is a question for the change
  that turns a flag off.
- **Validation vs. insert race.** A language removed or switched off between validation
  and insert is not handled; a removal gives a 500 through the foreign key. Languages
  change only by migration at deploy, so this is accepted.
- **Collation.** If prod Postgres ever runs with the `C` ctype, `lower()` stops folding
  Cyrillic and "Грамматика"/"ГРАММАТИКА" become distinct. Covered by tests on the same
  image; noted for any database move.
- **Unicode forms.** Titles are not NFC-normalised, so "й" typed as one code point and as
  "и" + combining breve are different titles. `Normalize.name` does not fold forms for
  names either; changing that is a separate change for both fields.
- **Length in UTF-16 units.** `@Size` counts UTF-16 units, so an emoji counts as 2 and a
  title of 101 emoji gets `TOO_LONG`. `@PersonName` does the same; the engineer accepted
  this for both fields, with no follow-up.
- **Invisible characters inside a title.** Like names, a title with a Unicode format
  character inside (soft hyphen, zero-width joiner in some emoji) gets `INVALID_FORMAT`;
  edges are trimmed first. The engineer accepted this: the teacher retypes the title.
- **Limit is a guess.** 20 is from angle D's estimate, not measured. Capacity really
  depends on file limits in #55.
