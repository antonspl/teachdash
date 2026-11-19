# Proposal

## Clarifications

Issue #161.

| # | Question | Engineer's answer |
|---|---|---|
| 1 | What is a teacher's language? | The same concept as the student's: the language the teacher reads translations in. It reuses the existing field `users.native_language` / `nativeLanguage`, the same list (`languages` R1), the same validation and the same error codes (`REQUIRED`, `INVALID_FORMAT`, `INVALID_TYPE`). It is not the UI language (#102). |
| 2 | Required? | Required for teachers. The migration backfills existing teachers with `en`. Admins have no language. The database checks become: null for admins, required for students and teachers. |
| 3 | Who sets it at creation? | The admin, when creating a teacher. `CreateTeacherRequest` gets a required `nativeLanguage` (same rule as for students: one validation rule per field). The admin's "Add teacher" dialog gets a language field: required, no preselected value, as in the student form (#144); language names come from i18n. |
| 4 | Who changes it later? | The teacher, through `PATCH /api/me` (`UpdateProfileRequest`) and a language dropdown in the profile form (`ProfilePage`/`ProfileForm`). A student still cannot change theirs through `PATCH /api/me`: the field is ignored, as today. |
| 5 | Where is it returned? | For teachers: in `/api/me`, the login response and the admin's teacher list and details. |
| 6 | Translation? | Not in this change: no content exists yet (#13, #106). |
| 7 | Constitution? | Update §9 in this PR: every student and teacher has a native language, and translation goes into the reader's (student's or teacher's) native language. The owner approves it in this PR. The edit stays minimal. |
| 8 | More research? | No further research. |

## Research angles

The engineer agreed no further research. The exploration before the proposal concluded:

- **Origin of the field.** The archived change `2026-11-17-student-language` added `V9__student_language.sql` (the `languages` table, `users.native_language` with a foreign key, check `users_native_language_students_only`) and `V10__student_language_required.sql` (backfill `en` for students, check `users_student_has_native_language`). Code: `accounts/domain/User.java`, `accounts/api/dto/UserResponse.java` (`@JsonInclude(NON_NULL)`), `languages/api/validation/NativeLanguage.java`.
- **Teacher creation.** Only the admin creates teachers: `TeacherAdminService.createTeacher` passes `null` as the language; `CreateTeacherRequest` has no language.
- **Specs touched.** `student-enrolment` R15-S2 and R15-S3 say teachers have no `nativeLanguage`; `accounts` R12-S2 lists the exact fields of the admin's teacher list; `web-profile` R2/R3 and `web-admin-teachers` R2 describe the forms. `accounts` owns `PATCH /api/me` (R9) and admin teacher creation (R5).
- **Frontend.** It has no language handling yet (#144 is open for the student form), so this change adds the shared language list loading and select.

## Why

Translation goes into the reader's native language (constitution §9). Teachers read translations too, so a teacher must carry a native language, as a student does, before translation (#13, #106) is built.

## What Changes

- Every teacher has a native language, required, from the same list as a student's. The migration backfills existing teachers with `en`.
- The admin picks it when creating a teacher (`CreateTeacherRequest.nativeLanguage`, "Add teacher" dialog).
- The teacher changes it through `PATCH /api/me` and a dropdown in the profile form. A student's and an admin's `PATCH /api/me` ignores the field.
- `nativeLanguage` is returned for teachers in `/api/me`, login, set-password, the admin's teacher list and the admin's student's-teachers list.
- Constitution §9: every student and teacher has a native language; translation goes into the reader's native language.
- Frontend: a shared language select with names from i18n.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `accounts`: adds R13 (a teacher has a native language: creation, own change, database rule, backfill); R12 (the admin's teacher list shows it).
- `student-enrolment`: R15 now covers teachers (R15-S2 and R15-S3 change, R15-S5 added).
- `web-profile`: R2 (the teacher's language select, new scenarios R2-S9 to R2-S12) and R3 (R3-S10 request carries the language).
- `web-admin-teachers`: R2 (the language field in the "Add teacher" dialog, new scenarios R2-S9 to R2-S13).

## Out of scope

- Translation itself (#13, #106).
- The UI language (#102).
- A native language for admins.
- A student changing their own language.
- The student form and join form fields (#144), except the shared language select this change adds.
- Showing the teacher's language in the admin's teacher list page (the field is in the API only).
- `languages` R1 wording ("Every listed language may be a student's native language") is not a modified requirement here, to stay within six requirements; see design D8.

## Impact

- Backend: `CreateTeacherRequest`, `UpdateProfileRequest`, `TeacherResponse`, `UserSummary`, `AccountService.updateProfile`, `UserRepository.updateProfile`, `TeacherAdminService`.
- Database: migrations `V12__teacher_language.sql` and `V13__teacher_language_required.sql` (the version numbers are set at merge, #98).
- Frontend: `ProfileForm`, `AdminTeachersPage`, `Select`, `adminTeachers.ts`, `profile.ts`, `AuthProvider` user type, a languages API client, `en.json`.
- Constitution §9 (owner approval in this PR).
- Tests: existing helpers that create teachers (`ApiTest`) and every test that creates a teacher through the admin endpoint or updates a teacher's profile send a language.
- No new dependencies.
