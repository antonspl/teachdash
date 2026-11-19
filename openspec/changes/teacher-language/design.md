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
- `UpdateProfileRequest` gets a nullable `nativeLanguage` with a listed-language check but without `@NotBlank` (so a student or an admin can omit it), plus a role-aware check in the controller path: for a teacher a missing or blank value is `REQUIRED` and an invalid value `INVALID_FORMAT`; for other roles the value is neither checked nor stored. Implemented by validating in the service/controller with the same validator logic, so error codes and the `errors` shape (field `nativeLanguage`) are those of `@NativeLanguage` and all failing fields are reported together (R13-S6). `INVALID_TYPE` for an array or object comes from the existing JSON-reading handler (api-errors R1).
- `AccountService.updateProfile` gets the language; `UserRepository.updateProfile` sets `native_language` for a teacher and leaves it alone for other roles (one targeted update, never `save()`).
- `TeacherResponse` gets `nativeLanguage` (always set for teachers). `UserSummary` already has it; teachers now have a value, so the admin's student's-teachers list shows it with no change (R15-S2). `UserResponse` needs no change: `NON_NULL` hides it for admins only.
- Existing tests that build teachers (`ApiTest.createUser`) set `native_language` for teachers (default `en`, overridable), as they do for students.

### D3. Frontend: language list and select

- A languages client in `frontend/src/api/languages.ts` calls `GET /api/languages` and returns the codes. One hook, `useLanguages`, loads the list when a form that needs it mounts and exposes `retry`. No cache beyond the component.
- Names come from `frontend/src/i18n/en.json` under `languages.<code>` (`de` to `uk`, the ten codes). A code with no name shows the code itself, so a new language needs no frontend change to be usable.
- `Select` gains optional option labels and an optional empty placeholder option ("Choose a language"), so it can show names and start empty. A small `LanguageSelect` wraps `Select` with the names and the list from `useLanguages`; the profile form and the dialog both use it, and #144 will reuse it. If the stored language is missing from the list, the select adds it (R2-S12 of web-profile).
- `ProfileForm` shows `LanguageSelect` only when `user.role` is teacher; `updateProfile` in `profile.ts` sends `nativeLanguage` only for a teacher. The `User` type in `AuthProvider` gets an optional `nativeLanguage`.
- `AdminTeachersPage` dialog gets `LanguageSelect` with no initial value; submit with no value shows the translated "choose a language" message at the select and sends nothing. `NewTeacher` gets `nativeLanguage`. Backend field errors use the existing `useFieldErrors` mapping (key `nativeLanguage`).

### D4. Constitution §9

One sentence changes: "Every student has a native language ... set by their teacher or chosen by the student when joining through a link" becomes "Every student and every teacher has a native language ...; a teacher's is set by the admin when creating the account and changed by the teacher", and "Translation goes from the course's target language into the student's native language" becomes "into the reader's native language (a student's or a teacher's)". "Features ... a student whose native language is the target language gets no translation" becomes "A reader whose native language ...". The list stays "student languages" in name only; the edit stays minimal. The owner approves it in this PR.

### D5. Role of the field per role (summary)

| Role | `native_language` | API |
|---|---|---|
| admin | null | never returned; `PATCH /api/me` ignores it |
| teacher | required | returned everywhere; set at creation by the admin; changed by `PATCH /api/me` |
| student | required | returned; changed by the teacher only |

### D6. Decision for the engineer to confirm

`PATCH /api/me` requires `nativeLanguage` from a teacher, like name and time zone: the profile form always sends all fields. The alternative (optional, a missing value leaves it unchanged) is also possible; the brief says "required for teachers" and "same rule as for students", which this follows. Scenarios accounts/R13-S6 and R13-S7 depend on it.

### D7. Tests

Backend: one test per scenario of `accounts` R13 and R12, and `student-enrolment` R15 (class names by the implementer, in the existing packages). R13-S9 migrates a test database to V12, inserts rows, then runs V13 (Flyway `target`). Frontend: one Vitest test per scenario of `web-profile` R2-S9 to R2-S12, R3-S10, and `web-admin-teachers` R2-S9 to R2-S13, with "[<scenario ID>]" at the start of the title, written with the code.

### D8. Stale wording of `languages` R1

`languages` R1 says "Every listed language may be a student's native language". It is true but incomplete after this change. Changing it would be a seventh modified requirement, over the limit of six, so it is not in the deltas. The wording is fixed by hand in `openspec/specs/languages/spec.md` at archive, with the Purpose line ("a student's or teacher's native language"), and the engineer sees that edit in the PR. Alternatively the engineer may drop another requirement from the change or approve a seventh.

## Risks / Trade-offs

- The V13 check makes any code path that creates a teacher without a language fail with 500: the admin endpoint and the test helpers are the only ones, and both are changed. The admin bootstrap creates an admin, not a teacher.
- Deployed data: existing teachers become `en`, including the owner's account, until they change it in the profile.
