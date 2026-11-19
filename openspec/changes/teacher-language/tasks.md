# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V12__teacher_language.sql` (design D1). It is a schema migration, which pass 1 may add. Make `ApiTest.createUser` set `native_language` for teachers (default `en`, overridable) and add a way to read a user's language for any role. Verify the app starts and the existing suite still passes
- [ ] 1.2 Add `nativeLanguage` to the bodies of every existing test that creates a teacher through the admin endpoint (R5 and R11 tests, `TeacherManagementTest`, `AccountFieldRulesTest`) and, for a teacher, every `PATCH /api/me` body (`ProfileTest`), choosing `en` unless the scenario says otherwise; change bodies only, never assertions. Grep the test tree for other bodies. Verify the existing suite still passes
- [ ] 1.3 Write tests for accounts R13-S1 to R13-S9 and R12-S1 to R12-S3, and for student-enrolment R15-S1 to R15-S5, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create data and sessions in GIVEN directly. R13-S9 migrates to V12 and runs V13 with Flyway (design D7). The WHEN step only captures responses, so each test fails in THEN
- [ ] 1.4 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report any new test that passes already
- [ ] 1.5 List `V12__teacher_language.sql` (permanent; V13 follows in pass 2) and the helper and body edits to existing tests in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Add `V13__teacher_language_required.sql` (design D1); `nativeLanguage` in `CreateTeacherRequest` and `TeacherAdminService.createTeacher`; `nativeLanguage` in `TeacherResponse` (design D2); verify accounts R13-S1 to R13-S4, R13-S8, R13-S9 and R12 tests pass
- [ ] 3.2 `PATCH /api/me`: `UpdateProfileRequest.nativeLanguage`, the role-aware check, `AccountService.updateProfile` and `UserRepository.updateProfile` (design D2); verify R13-S5 to R13-S7 and student-enrolment R15 tests pass
- [ ] 3.3 Frontend: languages client and `useLanguages`, `languages.*` names in `en.json`, `Select` labels and placeholder, `LanguageSelect` (design D3); Vitest tests for its own states
- [ ] 3.4 Frontend: language select in `ProfileForm` for teachers, `User` type, `updateProfile`; Vitest tests `[web-profile/R2-S9]` to `[web-profile/R2-S12]` and `[web-profile/R3-S10]` (updated)
- [ ] 3.5 Frontend: language field in the "Add teacher" dialog, `NewTeacher`; Vitest tests `[web-admin-teachers/R2-S9]` to `[web-admin-teachers/R2-S13]`, and existing R2 tests updated to choose a language
- [ ] 3.6 Constitution §9 edit (design D4), kept to the sentences named there
- [ ] 3.7 Run `./gradlew cleanTest test`, the frontend tests and lint, and `scripts/check-scenario-coverage.sh`; verify everything is green and the approved backend tests are unchanged

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR. The owner approves the constitution edit in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, fix by hand the `languages` R1 wording and the Purpose of `openspec/specs/languages/spec.md` (design D8), and add a sentence about teachers' native language to the Purposes of `accounts` and `web-profile` where they list the fields.
- Mark the PR ready for the final review by a code owner other than the engineer.
