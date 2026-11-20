# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V15__lessons_one_course.sql` (design D1): `course_id` and `position` on `lessons`, the deferrable unique constraint, backfill, drop `lesson_courses`. It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Rewrite the existing create and list queries in `LessonRepository` onto the new columns (design D4, unchanged behaviour) and update the `ApiTest` helpers `createLesson`, `countLessons` and `lessonPositions` (direct SQL) to the new schema. Verify the existing R1 and R2 tests still pass. No test for the migration itself
- [ ] 1.2 Add stubs returning 501, marked `STUB`, for `PATCH .../lessons/{lessonId}`, `PATCH .../lessons/{lessonId}/position` and `DELETE .../lessons/{lessonId}` (design D2)
- [ ] 1.3 Write tests for lessons R3-S1 to R3-S7 in `lessons/api/RenameLessonTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons and sessions in GIVEN with the helpers, never through the endpoints under test. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. In R3-S6 the anonymous client fetches a CSRF cookie first; courses use `en` and `ru` instruction languages as `CreateLessonTest`
- [ ] 1.4 Write tests for lessons R4-S1 to R4-S8 in `lessons/api/MoveLessonTest`, same rules. R4-S8 uses `Parallel.run`
- [ ] 1.5 Write tests for lessons R5-S1 to R5-S7 in `lessons/api/DeleteLessonTest`, same rules. R5-S7 uses `Parallel.run`
- [ ] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup), except the security scenarios (403, 401, CSRF: R3-S6, R4-S6, R4-S7 in part, R5-S5, R5-S6) which may already pass through the existing rules; every other test passes. Report each new test that passes against the stubs
- [ ] 1.7 List the stubs (temporary) and `V15__lessons_one_course.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Repository and requests: `LessonRepository` methods lookups of a lesson and its position scoped by course and teacher, `updateTitle`, `shiftRange`, `setPosition`, `deleteReturningPosition`, `closeGap` (design D4); `RenameLessonRequest` (`@Title`, `Normalize.name`) and `MoveLessonRequest` (`BigInteger position`, `@NotNull`, integer-only coercion, design D3)
- [ ] 3.2 `LessonService.rename`, `move` and `delete`: user row lock first (`findActiveByIdForUpdate`, none gives 401), then the course row lock scoped by teacher (none gives 404), then the flow of design D4; the controller replaces its stubs (`PATCH`, `PATCH .../position`, `DELETE` 204). Add a comment at the locks saying the user row comes first to avoid a deadlock with `CourseService.rename`
- [ ] 3.3 Check that no lesson title appears in logs (constitution §5: record IDs, not content)
- [ ] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
