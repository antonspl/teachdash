# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V16__parts.sql` (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [x] 1.2 Add stubs returning 501, marked `STUB`, for `POST` and `GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts` (design D2)
- [x] 1.3 Add `ApiTest` helpers `createPart(teacherId, lessonId, title, position)` (inserts the `parts` row directly, times from the test `Clock`), `countParts(lessonId)` and `partPositions(lessonId)` (design D5). Add to the lessons delete test a way to read parts (direct SQL), if needed for R5-S8
- [x] 1.4 Write tests for parts R1-S1 to R1-S13 in `parts/api/CreatePartTest`, R2-S1 to R2-S7 in `parts/api/ListPartsTest`, and lessons R5-S8 in `lessons/api/DeleteLessonTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons, parts and sessions in GIVEN with the helpers, never through the stubbed endpoints. GIVEN has no asserts. R1-S9 and R1-S13 use `Parallel.run`. In R1-S11 and R2-S6 the anonymous client fetches a CSRF cookie first. The WHEN step only captures responses, so each test fails in THEN
- [x] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup), except the security scenarios (403, 401, CSRF: R1-S11, R2-S6, R2-S7 and the 403/401 parts of others), which may already pass through the existing rules, and R5-S8, which may already pass once the migration cascades. Report to the engineer each new test that passes against the stub
- [x] 1.6 List the stubs (temporary) and `V16__parts.sql` (permanent) in the PR description

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 `parts` package: `CreatePartRequest` (normalising title, `@Title`), `PartResponse`, `Part`, `PartRepository` (JdbcClient: lock course, lock lesson in course of teacher, lesson-in-course check without lock, count, insert at the end, list by position)
- [x] 3.2 `PartService.create` in the lock order of design D3 and D4 (user row, course row, lesson row, limit check, insert), `PartService.list` (read-only), the property `teachdash.parts.max-per-lesson=20` added to `application.properties` (and to any test properties file the tests need), and the controller replacing its stubs; add a one-line comment at the locks: the user row comes first to avoid a deadlock with `CourseService.rename`
- [x] 3.3 Check that no part title appears in logs (constitution §5: record IDs, not content)
- [x] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
