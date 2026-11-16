# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V9__student_language.sql` with the relaxed check only (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Keep `languages` out of the `ApiTest` truncation, make `ApiTest.createUser`/`createStudent` set `native_language` for students (default `en`, overridable) and add `studentLanguage(id)` (design D7). Verify the app starts (including the admin bootstrap) and the existing suite still passes
- [ ] 1.2 Add `nativeLanguage` to the bodies of every existing registration and join test listed in design D7, choosing `en` unless the scenario says otherwise; change bodies only, never assertions. Then grep the test tree for other registration (`/api/teacher/students` POST) and join bodies and add it there too. Verify the existing suite still passes
- [ ] 1.3 Add stubs returning 501, each marked `STUB`, for `GET /api/languages` and `PATCH /api/teacher/students/{id}`. Do not touch `SecurityConfig` yet
- [ ] 1.4 Write tests for languages R1-S1 and R1-S2 in `languages/api/LanguagesTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`
- [ ] 1.5 Write tests for student-enrolment R13-S1 to R13-S7, R14-S1 to R14-S4 and R15-S1 to R15-S3 in `enrolment/api/StudentLanguageTest`. Create students, links, languages and sessions in GIVEN directly, never through the stubbed endpoint; check "teacher A's student details show" through the details endpoint (it exists; it fails in THEN until the field is added) and "is still/unchanged" with `studentLanguage(id)`. Before writing R15-S3, check that `PATCH /api/me` with an unknown field is accepted today; if it is rejected, stop and ask the engineer. The WHEN step only captures responses, so each test fails in THEN
- [ ] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report to the engineer any new test that passes already. Expected: R13-S4 and R13-S7 fail in THEN only because the details lack the field; R14-S4 may fail only on teacher A's CSRF check or pass outright (role, session and CSRF rules already exist); languages R1-S1 must fail (anonymous GET is 401 until the `SecurityConfig` rule)
- [ ] 1.7 List the stubs (temporary), `V9__student_language.sql` (permanent; V10 follows in pass 2) and the helper edits to existing tests in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Languages: `LanguageRepository`, the `GET /api/languages` controller replacing its stub, the public `SecurityConfig` rule (design D2); verify languages R1 tests pass
- [ ] 3.2 `@NativeLanguage` validator (design D3); add `nativeLanguage` to `RegisterStudentRequest`, `JoinLinkJoinRequest` and `JoinVisitor`
- [ ] 3.3 `User`, `User.create` and the two `AccountService` create methods carry the language; teacher creation and bootstrap pass `null` (design D4). Add `V10__student_language_required.sql` (design D1); verify R13 tests and the bootstrap test pass
- [ ] 3.4 `PATCH /api/teacher/students/{id}` with `StudentLanguageService` and `UserRepository.updateNativeLanguage`, replacing its stub (design D5); comment at the teacher-row lock on what it serialises; verify R14 tests pass
- [ ] 3.5 `nativeLanguage` in `UserSummary` and `UserResponse` with `NON_NULL`, and in the four repository selects (design D6); verify R15 tests pass
- [ ] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, add a sentence about the native language to the Purpose of `openspec/specs/student-enrolment/spec.md`.
- Update #144 (frontend) with the final API: `GET /api/languages`, `nativeLanguage` in the registration and join bodies, the new PATCH.
- Mark the PR ready for the final review by a code owner other than the engineer.
