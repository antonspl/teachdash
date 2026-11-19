# Design: teacher-language

## Context

See proposal.md. Current state on main:

- `users.native_language` exists (V9, V10), with checks that allow it for students only and require it for students. `User.create` and both `AccountService` create methods take it; teacher creation passes `null`.
- `UserResponse` (`GET/PATCH /api/me`, login, set-password) has `nativeLanguage` with `NON_NULL`. `TeacherResponse` (admin's teacher list) does not. `UserSummary` backs the admin's student's-teachers list.
- `UpdateProfileRequest(name, timeZone)`; `AccountService.updateProfile(id, name, timeZone)` runs one targeted update. Jackson ignores unknown fields, so a student's `nativeLanguage` is ignored today.
- `@NativeLanguage` (`languages/api/validation`) is the one rule: `@NotBlank` plus a listed-language check.
- Frontend: `Select` takes plain string options; there is no languages client; `en.json` is the only translation file.

## Goals / Non-Goals

**Goals:** every teacher has a database-enforced native language; one validation rule for it; the teacher and the admin can set it; the profile and the "Add teacher" dialog show a language select.

**Non-Goals:** translation, the UI language, languages for admins, the student form (#144).

## Decisions

### D1. Migrations `V12__teacher_language.sql` (pass 1) and `V13__teacher_language_required.sql` (pass 2)

The same split as V9/V10 of student-language. V12 relaxes the schema so teachers may hold a language:

```sql
alter table users drop constraint users_native_language_students_only;
alter table users add constraint users_admin_has_no_native_language
    check (role <> 'ADMIN' or native_language is null);
```

V13, with the production code in pass 2:

```sql
update users set native_language = 'en'
    where role = 'TEACHER' and native_language is null;
alter table users drop constraint users_student_has_native_language;
alter table users add constraint users_student_teacher_have_native_language
    check (role = 'ADMIN' or native_language is not null);
```

The strict check waits for pass 2 because until then the code creates teachers without a language. The admin check is in V12, so after V13 the two checks together mean: null for admins, set for students and teachers. The names of the dropped constraints are the ones V9 and V10 created.

### D2. Backend

- `CreateTeacherRequest` gets `@NativeLanguage String nativeLanguage`, stripped with `Normalize.strip` in the compact constructor, like `JoinVisitor`. `TeacherAdminService.createTeacher` passes it to `accounts.createPasswordlessUser` in place of `null`.
- `UpdateProfileRequest` gets `JsonNode nativeLanguage`, read tolerantly and with no bean annotation: an annotation cannot see the caller's role, and a typed `String` would make Jackson fail on an array or object even for a student or an admin. The controller takes the `BindingResult` after `@Valid`; for a teacher only, it then checks the node and adds errors for field `nativeLanguage` to the same result before the existing handler builds the response, so name, time zone and language failures are reported together. The checks: field absent (`node == null`) keeps the stored value; JSON `null` or a blank string is `REQUIRED`; a non-text node (array, object, number, boolean) is `INVALID_TYPE`; a stripped text not in the list is `INVALID_FORMAT` (the list lookup of `@NativeLanguage`, shared as one function so creation and profile use one rule). For a student or an admin the node is not looked at. Only a valid text value reaches the service.
- `AccountService.updateProfile` gets the language; `UserRepository.updateProfile` takes an optional language: when it is present (only for a teacher) it sets `native_language`, otherwise the column is untouched (one targeted update, never `save()`); the update is scoped to the caller's own id, so no other account changes.
- `TeacherResponse` gets `nativeLanguage` (always set for teachers). `UserSummary` already has it; teachers now have a value, so the admin's student's-teachers list shows it with no change (R15-S2). `UserResponse` needs no change: `NON_NULL` hides it for admins only. The creation response (`CreatedAccount`: id and link) is unchanged.
- Existing tests that build teachers (`ApiTest.createUser`) set `native_language` for teachers (default `en`, overridable), as they do for students.

### D3. Frontend: language list and select

- A languages client in `frontend/src/api/languages.ts` calls `GET /api/languages` and returns the codes. One hook, `useLanguages`, loads the list when a form that needs it mounts and exposes `retry`. No cache beyond the component.
- Names come from `frontend/src/i18n/en.json` under `languages.<code>` (`de` to `uk`, the ten codes). A code with no name shows the code itself, so a new language needs no frontend change to be usable.
- `Select` gains optional option labels and an optional empty placeholder option ("Choose a language"), so it can show names and start empty. A small `LanguageSelect` wraps `Select` with the names and the list from `useLanguages`; the profile form and the dialog both use it, and #144 will reuse it. If the stored language is missing from the list, the select adds it (R2-S12 of web-profile).
- `ProfileForm` shows `LanguageSelect` only when `user.role` is teacher; `updateProfile` in `profile.ts` sends `nativeLanguage` only for a teacher. The `User` type in `AuthProvider` gets an optional `nativeLanguage`.
- `AdminTeachersPage` dialog gets `LanguageSelect` with no initial value; submit with no value shows the translated "choose a language" message at the select and sends nothing. `NewTeacher` gets `nativeLanguage`. Backend field errors use the existing `useFieldErrors` mapping (key `nativeLanguage`).

### D4. Constitution §9

§8 needs no change: a teacher's language is not teacher-owned data. In §9, exactly these four sentences change (the lines are 198-207 of the constitution at the time of writing), and nothing else:

1. "Every student has a native language from a configured list of student languages (ISO 639-1 codes), set by their teacher or chosen by the student when joining through a link; it is student data (section 8)." becomes "Every student and every teacher has a native language from a configured list of native languages (ISO 639-1 codes). A student's is set by their teacher or chosen by the student when joining through a link, and is student data (section 8); a teacher's is set by the admin when creating the account and changed by the teacher."
2. "The supported course languages are a subset of that list." stays: "that list" now names the list of native languages.
3. "Translation goes from the course's target language into the student's native language, so every language in the list of student languages MUST be supported as a translation target." becomes "... into the reader's native language (a student's or a teacher's), so every language in the list of native languages MUST be supported as a translation target."
4. "A student whose native language is the target language gets no translation." becomes "A reader whose native language is the target language gets no translation."

The sentence "A course does not record who takes it: students with different native languages may take the same course" stays: it is about students. The owner approves the edit in this PR.

### D5. Role of the field per role (summary)

| Role | `native_language` | API |
|---|---|---|
| admin | null | never returned; `PATCH /api/me` ignores it |
| teacher | required | returned everywhere; set at creation by the admin; changed by `PATCH /api/me` |
| student | required | returned; changed by the teacher only |

### D6. `PATCH /api/me`: the language is optional for a teacher

Decided by the engineer (proposal Clarifications 10, 11): a teacher's `PATCH /api/me` without `nativeLanguage` keeps the stored value, so clients that do not know the field keep working. `null`, "" and blank are `REQUIRED`, an unlisted code `INVALID_FORMAT`, a wrong type `INVALID_TYPE`; the value rule is that of creation (accounts R5, R9, R11). For a student or an admin the field is ignored whatever its type, which is why the check is role-aware (D2). The profile form still always sends the language for a teacher.

### D7. Tests

Backend: one test per new or changed scenario of `accounts` R5 (S1, S4 to S6), R9 (S12 to S17), R11-S6, R12-S2 and R13, and `student-enrolment` R15 (class names by the implementer, in the existing packages). R13-S2 migrates a test database with Flyway `target("12")`, inserts rows, then migrates to `target("13")`; the version numbers are those of D1 and the test changes with them if they are renumbered at merge (#98). Frontend: one Vitest test per scenario of `web-profile` R2-S9 to R2-S15, R3-S10, and `web-admin-teachers` R2-S9 to R2-S14, with "[<scenario ID>]" at the start of the title, written with the code.

## Risks / Trade-offs

- The V13 check makes any code path that creates a teacher without a language fail with 500: the admin endpoint and the test helpers are the only ones, and both are changed. The admin bootstrap creates an admin, not a teacher.
- Rollback: after V13, an old application version that creates a teacher without a language fails with 500 on the check. A rollback of the app therefore needs the V13 constraint dropped first (and V12's admin check can stay); there is no automatic down migration.
- Deployed data: existing teachers become `en`, including the owner's account, until they change it in the profile.
