# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V9__courses.sql` with the `languages` table, its `en`/`ru` seed and the `courses` table with `courses_teacher_title_uq` (design D2). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Exclude `languages` from the `ApiTest` truncation and reset it to `en`/`ru` after the truncate (design D1); verify the app starts and the existing suite still passes
- [ ] 1.2 Add stubs returning 501, each marked `STUB`, for `GET /api/languages` and `POST /api/teacher/courses` (design D3). Do not touch `SecurityConfig` yet
- [ ] 1.3 Add `ApiTest` helpers: `createCourse(teacherId, title, instructionLanguage, targetLanguage)` and `countCourses(teacherId)` (direct SQL)
- [ ] 1.4 Write tests for courses R1-S1 to R1-S3 in `languages/api/LanguagesTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`
- [ ] 1.5 Write tests for courses R2-S1 to R2-S14 in `courses/api/CreateCourseTest`. Create existing courses and sessions in GIVEN with the helpers, never through the stubbed endpoint. R2-S11 uses `Parallel.run`. In R2-S13 the anonymous client fetches a CSRF cookie first. The WHEN step only captures responses, so each test fails in THEN
- [ ] 1.6 Write a test for api-errors R2-S6 next to the other api-errors R2 tests
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report to the engineer any new test that passes against the stubs. R2-S13 may already pass through the existing security rules (anonymous, admin and student on `/api/teacher/**`; deactivated sessions). R1-S3 must fail: the student gets the stub's 501 until the `SecurityConfig` rule exists
- [ ] 1.8 List the stubs (temporary) and `V9__courses.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Languages: `LanguageRepository`, the `GET /api/languages` controller replacing its stub, and the `SecurityConfig` rule for TEACHER and ADMIN (design D3); verify R1 tests pass
- [ ] 3.2 Validation: `@CourseTitle` in `common/validation/` and `@LanguageCode` with its DB-backed validator (design D4)
- [ ] 3.3 Courses: `CreateCourseRequest` (normalising the title), `CourseResponse`, `CourseRepository`, `CourseService.create` with the lock, limit check and `TITLE_TAKEN` mapping in the order of design D5, the `teachdash.courses.max-per-teacher=20` property, and the controller replacing its stub; add a comment at the row lock saying it serialises the limit check
- [ ] 3.4 Add `TITLE_TAKEN` to the codes allowed to carry `errors` (api-errors R2); verify all R2 and api-errors tests pass
- [ ] 3.5 Check that no course title appears in logs (constitution §5: record IDs, not content)
- [ ] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Comment on #105 that rename must accept only the title (design D6), and on #106 that tables are plural and `lesson_courses` stays the lesson's only link to a course (proposal, angle C).
- Mark the PR ready for the final review by a code owner other than the engineer.
