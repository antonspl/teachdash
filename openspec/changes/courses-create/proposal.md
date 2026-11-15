# Proposal

## Clarifications

| Question | Engineer's answer |
|---|---|
| Who can create a course? | Teachers only. |
| Who can read the language list? | Teachers and admins. Students cannot. |
| May the target language equal the instruction language? | Yes. Such a course has translation off. |
| Limits? | Title of 1–200 characters. At most 20 courses per teacher, because we do not know yet how heavy a course will be. |
| May one teacher have two courses with the same title? | No. Titles are unique per teacher, ignoring case. |
| Scope? | Create only. Listing, viewing and renaming are #105; deleting is #131. |
| Where does the language list live? | A database table seeded by Flyway (research angle B). |
| How is a duplicate title reported? | 409 `TITLE_TAKEN`, naming the `title` field, like `EMAIL_TAKEN`. |
| Lessons without a course? | Not now (angle C). A teacher can keep a "library" course per language pair; its lessons can later join other courses with the same pair (#95). |

## Research angles

- **A. Code conventions.** Reuse the enrolment patterns:
  - a `@Valid` request record with one shared annotation per field, normalised with `Normalize.name`;
  - a teacher-only route under `/api/teacher/**`;
  - a row lock on the teacher's `users` row (`findActiveByIdForUpdate`), so the count check is safe under parallel requests;
  - a unique index on `lower(...)`, like the one for emails;
  - 409 with a domain error code.
- **B. Where the language list lives.** A `languages` table seeded by Flyway; course columns are foreign keys to it. Adding a language is a migration that goes through a PR. A Java enum would hard-code languages (constitution §9). A properties-only list would let the database keep codes that the list no longer contains.
- **C. Lessons not attached to any course.** Not now. #106 already makes `lesson_course` a lesson's only link to a course, so lessons without a course can be added later without moving data. This change adds nothing about lessons.
- **D. Weight of a course.** Estimated, not measured: about 2 MB of database data per typical course; uploaded files (#55) drive capacity, not the course count. 20 courses per teacher protects against abuse; the cap lives in a property so it can be raised without code changes.

## Why

Courses are the root of all teaching content: lessons, assignments and sharing all hang off them. A teacher must be able to create a course in a supported language before any of that work can start.

## What Changes

- A `languages` table with the supported ISO 639-1 codes, seeded with `en` and `ru`, and `GET /api/languages` for teachers and admins.
- A `courses` table owned by a teacher: title, instruction language, optional target language, creation time.
- `POST /api/teacher/courses` creates a course for the current teacher. Rules:
  - The title is normalised, 1–200 characters, and unique per teacher ignoring case.
  - Both language codes must be in the language list.
  - A teacher has at most 20 courses.
- New error codes: `409 TITLE_TAKEN` (with field `title`) and `409 COURSE_LIMIT_REACHED`.
- Course languages are fixed at creation. No endpoint changes them; #105 (rename) must keep it so.

## Capabilities

### New Capabilities
- `courses`: courses owned by a teacher, and the list of supported languages they use.

### Modified Capabilities
- `api-errors`: R2 adds `TITLE_TAKEN` to the codes that name their field.

## Out of scope

- Listing, viewing and renaming courses (#105), deleting (#131), assigning students (#122).
- Lessons and the lesson-to-course language rule (#106, #95).
- Lessons without a course.
- Display names of languages: the frontend shows them through i18n.
- Adding or removing languages through the API: a new language is a migration.
- Per-language settings (#127) and translation.
- Frontend (#115).
- File size and storage quotas (#55).

## Impact

- Backend: new `courses` and `languages` feature packages, a `@CourseTitle` validation annotation, one line in `SecurityConfig`, one property in `application.properties`.
- Database: migration `V9__courses.sql`. The version is set at merge (#98).
- API: two new endpoints, two new error codes.
- No new dependencies.
