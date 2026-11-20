# Proposal

## Clarifications

Issue #161.

| # | Question | Engineer's answer |
|---|---|---|
| 1 | What is a teacher's language? | The same concept as the student's: the language the teacher reads translations in. It reuses the existing field `users.native_language` / `nativeLanguage`, the same list (`languages` R1), the same validation and the same error codes (`REQUIRED`, `INVALID_FORMAT`, `INVALID_TYPE`). It is not the UI language (#102). |
| 2 | Required? | Required for teachers. The migration backfills existing teachers with `en`. Admins have no language. The database checks become: null for admins, required for students and teachers. |
| 3 | Who sets it at creation? | The admin, when creating a teacher. `CreateTeacherRequest` gets a required `nativeLanguage` (same rule as for students: one validation rule per field). The admin's "Add teacher" dialog gets a language field: required, no preselected value, as in the student form (#144); language names come from i18n. |
| 4 | Who changes it later? | The teacher, through `PATCH /api/me` (`UpdateProfileRequest`) and a language dropdown in the profile form (`ProfilePage`/`ProfileForm`). A student still cannot change theirs through `PATCH /api/me`. |
| 5 | Where is it returned? | For teachers: in `/api/me`, the login and set-password responses, the admin's teacher list and the admin's list of a student's teachers. There is no admin view of a single teacher, so none is added. The response to creating a teacher stays as it is (id and link) and does not carry it. |
| 6 | Translation? | Not in this change: no content exists yet (#13, #106). |
| 7 | Constitution? | Update §9 in this PR: every student and teacher has a native language, and translation goes into the reader's (student's or teacher's) native language. The owner approves it in this PR. The edit stays minimal: exactly the sentences listed in design D4; §8 needs no change. |
| 8 | More research? | No further research. |
| 9 | Size of the change? | Everything stays in this one change, backend and frontend. The engineer approves an explicit exception to the limit of six requirements: fixing the specs properly touches 11 requirements (one added, ten modified: accounts R5, R9, R11, R12, R13; student-enrolment R3, R15; languages R1; web-profile R2, R3; web-admin-teachers R2). The constitution's limit is not edited. |
| 10 | Is the field required in `PATCH /api/me`? | No. For a teacher it is optional: a `PATCH /api/me` without `nativeLanguage` keeps the stored value. JSON `null` or `""` (also a blank string) is rejected with `REQUIRED`; a code not in the list with `INVALID_FORMAT`; a wrong JSON type (array, object, number, boolean) with `INVALID_TYPE`. The value rule is otherwise the same as at creation. |
| 11 | Students and admins in `PATCH /api/me`? | `nativeLanguage` of any type (string, unlisted code, array, object, `null`) is ignored: no 400, the stored value does not change. Design says where the role-aware check runs. |
| 13 | Second critique | Student-enrolment R3 is modified too (a teacher's entry has the language); "all failing fields at once" in R5 and R9 means validation failures only (a JSON type error stops at the first field, as in api-errors); R5-S6 also covers `5` and `true`; the profile check does not use the annotations (design D2); existing teacher `PATCH /api/me` bodies in ProfileTest keep omitting the language; web-client R17 is unchanged; the deploy window is recorded in design Risks. |
| 12 | Other critique answers | Design Risks has a rollback note. New scenarios: a teacher's change leaves other users' languages alone; a code with no i18n name shows the code; retry after the language list fails (profile and admin dialog); the current-user state has the new language after a save without a reload. `languages` R1 is modified in the delta (the Purpose lines are fixed at archive, as Purposes are not part of deltas); design D8 is dropped. |
| 14 | Test the backfill? | No. Scenario accounts/R13-S2 (the migration test that backfills existing teachers with `en`, checked by migrating Flyway to version 12 and on) is dropped: it is not worth a test, it needs Flyway targets and is fragile against later migrations, and only the owner's teacher account exists before M2. R13 keeps its backfill text and R13-S1; other scenarios are not renumbered. |
| 15 | Simpler `PATCH /api/me` rules (code review) | Supersedes the parts of rows 10 and 11 about `null` being `REQUIRED` and about any type being ignored for students and admins. `nativeLanguage` is a plain `String` checked by the existing `@NativeLanguage` annotation (null allowed); no `JsonNode`, no role-aware validation. Absent or JSON `null` keeps the stored value, for every role. ""/blank is `REQUIRED`, an unlisted code `INVALID_FORMAT`, a non-string (array, object, number, boolean) `INVALID_TYPE`, for every role (student, teacher, admin); a JSON type error stops at the first field, as in api-errors. A valid code is saved for a teacher and ignored for a student or an admin (stored value unchanged; an admin stays without a language). Reason: the `JsonNode` design was complex; students will pick their language in our frontend later, so a hand-made bad value from a student getting 400 is acceptable. |

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
- The teacher changes it through `PATCH /api/me` and a dropdown in the profile form. A student's and an admin's `PATCH /api/me` ignores a valid code.
- `nativeLanguage` is returned for teachers in `/api/me`, login, set-password, the admin's teacher list and the admin's student's-teachers list.
- The language is optional in `PATCH /api/me` (absent or `null` keeps the stored value); "", an unlisted code or a wrong type is rejected for every role; a valid code is ignored for students and admins.
- Constitution §9: every student and teacher has a native language; translation goes into the reader's native language.
- Frontend: a shared language select with names from i18n.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `accounts`: R5 (creating a teacher takes a required native language), R9 (a teacher may change it through `PATCH /api/me`, optional; a valid code is ignored for others), R11 (one rule per field mentions it), R12 (the teacher list shows it), and added R13 (database rule and backfill).
- `student-enrolment`: R3 (a teacher's entry in the admin's list carries the native language) and R15 now covering teachers (R15-S2, R15-S3 change, R15-S5 added).
- `languages`: R1 wording ("a student's or teacher's native language").
- `web-profile`: R2 (the teacher's language select; R2-S2 and R2-S5 reworded; new scenarios R2-S9 to R2-S15) and R3 (R3-S10 request carries the language).
- `web-admin-teachers`: R2 (the language field in the "Add teacher" dialog; new scenarios R2-S9 to R2-S14).

## Out of scope

- Translation itself (#13, #106).
- The UI language (#102).
- A native language for admins.
- A student changing their own language.
- The student form and join form fields (#144), except the shared language select this change adds.
- Showing the teacher's language in the admin's teacher list page (the field is in the API only).
- The Purpose lines of `languages`, `accounts` and `web-profile`: Purposes are not part of deltas, so they are edited by hand at archive (tasks).

## Impact

- Backend: `CreateTeacherRequest`, `UpdateProfileRequest`, `TeacherResponse`, `UserSummary`, `AccountService.updateProfile`, `UserRepository.updateProfile`, `TeacherAdminService`.
- Database: migrations `V12__teacher_language.sql` and `V13__teacher_language_required.sql` (the version numbers are set at merge, #98).
- Frontend: `ProfileForm`, `AdminTeachersPage`, `Select`, `adminTeachers.ts`, `profile.ts`, `AuthProvider` user type, a languages API client, `en.json`.
- Constitution §9 (owner approval in this PR).
- Tests: existing helpers that create teachers (`ApiTest`) and every test that creates a teacher through the admin endpoint or updates a teacher's profile send a language.
- No new dependencies.
