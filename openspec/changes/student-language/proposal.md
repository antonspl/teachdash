# Proposal

## Clarifications

| Question | Engineer's answer |
|---|---|
| Which native languages? | Ten: `de`, `en`, `es`, `fr`, `it`, `pl`, `pt`, `ru`, `sr` (Serbian), `uk`. More are added on request. |
| What does "fall back to English if not supported" mean? | Every listed language is a translation target. The fallback covers only a language that translation (M7) decides it cannot serve; M7 configures it. |
| Who reads the language list? | Anyone, including a visitor on the join form: the list is not sensitive. One public endpoint, codes only; the browser shows names in the UI language. |
| Required at registration? | Yes. There are no real students yet, so any migration is fine. |
| Join link? | The visitor picks their native language in the join form. |
| Can it change? | The teacher can change it. The student sees it in their profile but cannot change it. |
| Assigning an existing student? | The student keeps their stored language. |
| Size? | One change, even though it touches many existing requirements. Structured as new requirements, so the existing ones stay as they are. |

## Research angles

- **A. Code paths.** A student is a `users` row (no students table). Two paths create students, the teacher's registration and the visitor join, both through `User.create`. Assigning an existing student touches no account fields, so it keeps the language with no code change. There is no endpoint where a teacher edits a student; the closest patterns are `PATCH /api/me` and the own-student checks of removal and reset links. Student summaries (`UserSummary`) back the teacher's and admin's student views; `/api/me` backs the profile. Tests send registration and join bodies without a language, so their helpers change; no test asserts exact key sets of these responses.
- **B. The language list.** One `languages` table with a `content` flag: all ten may be a student's native language; only `en` and `ru` may be course languages (constitution §9). This change creates the table; courses-create (#104) reuses it and checks `content`. Display names come from `Intl.DisplayNames` in the browser, so a new language needs no frontend change.

## Why

Translation goes into the student's native language (constitution §9, #142), and a course does not record who takes it. So the student account must carry a native language before courses (#104) and translation (M7) are built.

## What Changes

- A `languages` table seeded with ten codes; `en` and `ru` are flagged as course languages. `GET /api/languages` returns `[{code, content}]` to anyone.
- A student has a native language. Registering a new student and joining through a join link require it. An existing student keeps theirs.
- `PATCH /api/teacher/students/{id}` lets a teacher change their own student's native language.
- Student data shows `nativeLanguage` to the teacher, the admin and the student (`/api/me`). The student cannot change it.

## Capabilities

### New Capabilities
- `languages`: the configured list of languages the platform knows, and which of them may be course languages.

### Modified Capabilities
- `student-enrolment`: adds R13–R15 (native language at registration and join, teacher changes it, it is shown). Existing requirements keep their text; their scenarios gain the field only in test bodies.

## Out of scope

- Translation itself and the fallback setting (M7).
- Courses (#104), which will reuse `languages`.
- Frontend: the student form and join form fields (#144).
- Teachers and admins have no native language.
- A student changing their own language.
- Display names on the backend.

## Impact

- Backend: new `languages` package; `nativeLanguage` in the registration and join requests, `UserSummary`, `UserResponse`; a new teacher endpoint; one `SecurityConfig` line (public `GET /api/languages`).
- Database: migration `V9__student_language.sql` (the version is set at merge, #98).
- Tests: existing registration and join helpers send a language.
- No new dependencies.
- Issue #143 reserved `student-enrolment` R13–R14; this change uses R13–R15, agreed by the engineer.
