# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V14__lessons.sql` with `lessons`, `lesson_courses` and the deferrable unique constraint (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [x] 1.2 Add stubs returning 501, marked `STUB`, for `POST` and `GET /api/teacher/courses/{courseId}/lessons` (design D2)
- [x] 1.3 Add `ApiTest` helpers: `createLesson(teacherId, courseId, title, position)` (inserts the `lessons` and `lesson_courses` rows directly), `countLessons(courseId)` and `lessonPositions(courseId)` (direct SQL). `createLesson` takes its times from the test `Clock` (or an optional `createdAt`), so `createdAt` and `updatedAt` assertions (R2-S2, R1-S3) are exact. Tests do not depend on the `@Title` rename
- [ ] 1.4 Write tests for lessons R1-S1 to R1-S13 in `lessons/api/CreateLessonTest` and R2-S1 to R2-S7 in `lessons/api/ListLessonsTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons and sessions in GIVEN with the helpers, never through the stubbed endpoints. GIVEN has no asserts. R1-S9 uses `Parallel.run`. In R1-S12, R2-S6 and R2-S7 the anonymous client fetches a CSRF cookie first. The WHEN step only captures responses, so each test fails in THEN
- [ ] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup), except the security scenarios (403, 401, CSRF: R1-S12, R2-S6, R2-S7 and the 403/401 parts of others), which may already pass through the existing rules; every other test passes. Report to the engineer each new test that passes against the stub
- [ ] 1.6 List the stubs (temporary) and `V14__lessons.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.0 Rename `@CourseTitle` to `@Title` and update its uses in courses (design D3); verify the existing courses tests still pass
- [ ] 3.1 `lessons` package: `CreateLessonRequest` (normalising title, `@Title`), `LessonResponse`, `LessonRepository` (JdbcClient: course lookup with and without `for update`, count, insert lesson, insert `lesson_courses` at the next position, list by position)
- [ ] 3.2 `LessonService.create` with a plain active-teacher read (no lock), the course row lock (the only lock), limit check and insert in the order of design D4, `LessonService.list`, the `teachdash.lessons.max-per-course=100` property, and the controller replacing its stubs; add a comment at the course lock saying it serialises the limit check and the position, and that #107's move uses the same lock
- [ ] 3.3 Check that no lesson title appears in logs (constitution §5: record IDs, not content)
- [ ] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Comment on #107 and the part and item issues (#108, #110) with the position pattern of design D5, on item-image-audio (#55) with the open file-copy question (design Risks), and on the "store" course issue, if any, with the open question: one store course per language pair?
- Mark the PR ready for the final review by a code owner other than the engineer.
