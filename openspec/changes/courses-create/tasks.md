# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V11__courses.sql` with the `courses` table and `courses_teacher_title_uq` (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [x] 1.2 Add a stub returning 501, marked `STUB`, for `POST /api/teacher/courses` (design D2)
- [x] 1.3 Add `ApiTest` helpers: `createCourse(teacherId, title, instructionLanguage, targetLanguage)` and `countCourses(teacherId)` (direct SQL)
- [x] 1.4 Write tests for courses R1-S1 to R1-S16 in `courses/api/CreateCourseTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create existing courses and sessions in GIVEN with the helpers, never through the stubbed endpoint. R1-S9 relies on the seeded `content` false for "de" and "uk"; GIVEN has no asserts. R1-S13 uses `Parallel.run`. In R1-S15 the anonymous client fetches a CSRF cookie first. The WHEN step only captures responses, so each test fails in THEN
- [x] 1.5 Write tests for api-errors R1-S10 and R2-S6 next to the other api-errors tests
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report to the engineer any new test that passes against the stub. R1-S15 may already pass through the existing security rules (anonymous, admin and student on `/api/teacher/**`; deactivated sessions)
- [x] 1.7 List the stub (temporary) and `V11__courses.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 Language validation: `LanguageRepository.find`, `@ListedLanguage` (moved from `NativeLanguageValidator`, with `@NativeLanguage` composed from it), `@ContentLanguage`, `@CourseLanguage`, and the `UNSUPPORTED_COURSE_LANGUAGE` mapping in `FieldErrorCodes` (design D3); verify the student-enrolment R13 tests still pass
- [x] 3.2 `@CourseTitle` in `common/validation/` (design D3)
- [x] 3.3 Courses: `CreateCourseRequest` (normalising title and codes), `CourseResponse`, `CourseRepository`, `CourseService.create` with the lock, limit check and `TITLE_TAKEN` mapping in the order of design D4, the `teachdash.courses.max-per-teacher=20` property, and the controller replacing its stub; add a comment at the row lock saying it serialises the limit check
- [x] 3.4 Add `TITLE_TAKEN` to the codes allowed to carry `errors` (api-errors R2); verify all courses and api-errors tests pass
- [x] 3.5 Check that no course title appears in logs (constitution §5: record IDs, not content)
- [x] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Comment on #105 that rename must accept only the title (design D6) and must set `updated_at` (design D5), on #106 that tables are plural and `lesson_courses` stays the lesson's only link to a course (proposal, angle C), and on #115 that the course form offers only languages with `content` true.
- Mark the PR ready for the final review by a code owner other than the engineer.
