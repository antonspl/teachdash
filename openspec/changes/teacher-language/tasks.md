# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V12__teacher_language.sql` (design D1). It is a schema migration, which pass 1 may add. Make `ApiTest.createUser` set `native_language` for teachers (default `en`, overridable) and add a way to read a user's language for any role. Verify the app starts and the existing suite still passes
- [ ] 1.2 Add `nativeLanguage` to the bodies of every existing test that creates a teacher through the admin endpoint (R5 and R11 tests, `TeacherManagementTest`, `AccountFieldRulesTest`) choosing `en` unless the scenario says otherwise. Existing teacher `PATCH /api/me` bodies in `ProfileTest` keep omitting `nativeLanguage` (this proves the optional rule); do not add it to them; change bodies only, never assertions. Grep the test tree for other bodies. Verify the existing suite still passes
- [ ] 1.3 Write tests for accounts R5-S1 and R5-S4 to R5-S6, R9-S12 to R9-S17, R11-S6, R12-S2 and R13-S1 to R13-S2, and for student-enrolment R15-S1 to R15-S5, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create data and sessions in GIVEN directly. R13-S2 migrates to version 12 and then to 13 with Flyway `target` (design D7). The WHEN step only captures responses, so each test fails in THEN
- [ ] 1.4 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report any new test that passes already
- [ ] 1.5 List `V12__teacher_language.sql` (permanent; V13 follows in pass 2) and the helper and body edits to existing tests in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Add `V13__teacher_language_required.sql` (design D1); `nativeLanguage` in `CreateTeacherRequest` and `TeacherAdminService.createTeacher`; `nativeLanguage` in `TeacherResponse` (design D2); verify accounts R5, R11-S6, R12 and R13 tests pass
- [ ] 3.2 `PATCH /api/me`: `UpdateProfileRequest.nativeLanguage`, the role-aware check (no `@NotBlank`/`@NativeLanguage` on the profile field; one shared lookup function used by the annotation and the check; absent is Java `null`, JSON `null` is `NullNode`), `AccountService.updateProfile` and `UserRepository.updateProfile` (design D2); verify accounts R9-S12 to R9-S17 and student-enrolment R15 tests pass
- [ ] 3.3 Frontend: languages client and `useLanguages`, `languages.*` names in `en.json`, `Select` labels and placeholder, `LanguageSelect` (design D3); Vitest tests for its own states
- [ ] 3.4 Frontend: language select in `ProfileForm` for teachers, `User` type, `updateProfile`; Vitest tests `[web-profile/R2-S9]` to `[web-profile/R2-S15]`, the reworded `[web-profile/R2-S2]` and `[web-profile/R2-S5]`, and `[web-profile/R3-S10]` (updated)
- [ ] 3.5 Frontend: language field in the "Add teacher" dialog, `NewTeacher`; Vitest tests `[web-admin-teachers/R2-S9]` to `[web-admin-teachers/R2-S14]`, and existing R2 tests updated to choose a language
- [ ] 3.6 Constitution §9 edit: exactly the four sentences listed in design D4; §8 is not changed
- [ ] 3.7 Run `./gradlew cleanTest test`, the frontend tests and lint, and `scripts/check-scenario-coverage.sh`; verify everything is green and the approved backend tests are unchanged

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR. The owner approves the constitution edit in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, edit by hand the Purposes (deltas do not carry them): `languages` ("a student's or teacher's native language"), and a sentence about teachers' native language in `accounts` and `web-profile` where they list the fields.
- Mark the PR ready for the final review by a code owner other than the engineer.
