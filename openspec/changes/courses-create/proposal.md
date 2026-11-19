# Proposal

## Clarifications

| Question | Engineer's answer |
|---|---|
| Who can create a course? | Teachers only. |
| Where does the language list live? | The `languages` table and `GET /api/languages` from #143 (languages R1). This change reuses them and adds no language list of its own. |
| Which languages may a course use? | Listed languages with `content` true (now `en` and `ru`), for both the instruction and the target language. |
| How is a listed language with `content` false reported? | 400 `VALIDATION_FAILED` with the field code `UNSUPPORTED_COURSE_LANGUAGE`. Ideally the frontend never sends it: it offers only languages with `content` true (#115). |
| May the target language equal the instruction language? | Yes, e.g. Russian A1 taught in Russian. Translation goes into the student's native language, not the instruction language (constitution §9). |
| Limits? | Title of 1–200 characters. At most 20 courses per teacher, because we do not know yet how heavy a course will be. |
| May one teacher have two courses with the same title? | No. Titles are unique per teacher, ignoring case. |
| Scope? | Create only. Listing, viewing and renaming are #105; deleting is #131. |
| How is a duplicate title reported? | 409 `TITLE_TAKEN`, naming the `title` field, like `EMAIL_TAKEN`. |
| Which characters may a title have? | Any punctuation (`: , . ? !`), as textbook names use it; at least one letter or digit. |
| Lessons without a course? | Not now (angle C). A teacher can keep a "library" course per language pair; its lessons can later join other courses with the same pair (#95). |

## Research angles

- **A. Code conventions.** Reuse the enrolment patterns:
  - a `@Valid` request record with one shared annotation per field, normalised with `Normalize`;
  - a teacher-only route under `/api/teacher/**`;
  - a row lock on the teacher's `users` row (`findActiveByIdForUpdate`), so the count check is safe under parallel requests;
  - a unique index on `lower(...)`, like the one for emails;
  - 409 with a domain error code.
- **B. Language list.** Settled by #143: a `languages` table with a `content` flag, seeded by Flyway. Course columns are foreign keys to it. Language codes follow the same rule as `nativeLanguage` (trimmed, listed).
- **C. Lessons not attached to any course.** Not now. #106 already makes `lesson_course` a lesson's only link to a course, so lessons without a course can be added later without moving data. This change adds nothing about lessons.
- **D. Weight of a course.** Estimated, not measured: about 2 MB of database data per typical course; uploaded files (#55) drive capacity, not the course count. 20 courses per teacher protects against abuse; the cap lives in a property so it can be raised without code changes.

## Why

Courses are the root of all teaching content: lessons, assignments and sharing all hang off them. A teacher must be able to create a course in a course language before any of that work can start.

## What Changes

- A `courses` table owned by a teacher: title, instruction language, optional target language, creation time.
- `POST /api/teacher/courses` creates a course for the current teacher. Rules:
  - The title is normalised, 1–200 characters, and unique per teacher ignoring case.
  - Both language codes must be listed languages with `content` true.
  - A teacher has at most 20 courses.
- New error codes: `409 TITLE_TAKEN` (with field `title`), `409 COURSE_LIMIT_REACHED`, and the field code `UNSUPPORTED_COURSE_LANGUAGE`.
- Course languages are fixed at creation. No endpoint changes them; #105 (rename) must keep it so.

## Capabilities

### New Capabilities
- `courses`: courses owned by a teacher.

### Modified Capabilities
- `api-errors`: R1 ranks the new field code `UNSUPPORTED_COURSE_LANGUAGE` with `INVALID_FORMAT`; R2 adds `TITLE_TAKEN` to the codes that name their field.

## Out of scope

- Listing, viewing and renaming courses (#105), deleting (#131), assigning students (#122).
- Lessons and the lesson-to-course language rule (#106, #95).
- Lessons without a course.
- Changes to the language list or its endpoint (languages R1).
- Per-language settings (#127) and translation.
- Frontend (#115).
- File size and storage quotas (#55).

## Impact

- Backend: new `courses` feature package; `@CourseTitle` and `@CourseLanguage` validation annotations; one field code in `FieldErrorCodes`; one property in `application.properties`.
- Database: migration `V11__courses.sql`. The version is set at merge (#98).
- API: one new endpoint, two new response codes and one new field code.
- No new dependencies.
