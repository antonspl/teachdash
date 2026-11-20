# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V15__lessons_one_course.sql` (design D1): `course_id` and `position` on `lessons`, the deferrable unique constraint, backfill, drop `lesson_courses`. It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Rewrite the existing create and list queries in `LessonRepository` onto the new columns (design D4, unchanged behaviour). This is a required exception to constitution §1 (pass 1 production code is stubs and migrations only): the migration drops `lesson_courses`, so the queries cannot be split from it. Update the `ApiTest` helpers `createLesson`, `countLessons` and `lessonPositions` (direct SQL, and their Javadoc that mentions `lesson_courses`) and the direct SQL in `CreateLessonTest` that reads `lesson_courses` to the new schema, keeping the assertions. Verify the existing R1 and R2 tests still pass. No test for the migration itself
- [x] 1.2 Add stubs returning 501, marked `STUB`, for `PATCH .../lessons/{lessonId}`, `PATCH .../lessons/{lessonId}/position` and `DELETE .../lessons/{lessonId}` (design D2)
- [x] 1.3 Write tests for lessons R3-S1 to R3-S8 in `lessons/api/RenameLessonTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons and sessions in GIVEN with the helpers, never through the endpoints under test. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. In R3-S6 the anonymous client fetches a CSRF cookie first; courses use `en` and `ru` instruction languages as `CreateLessonTest`
- [x] 1.4 Write tests for lessons R4-S1 to R4-S10 in `lessons/api/MoveLessonTest`, same rules. R4-S8 uses `Parallel.run`
- [x] 1.5 Write tests for lessons R5-S1 to R5-S7 in `lessons/api/DeleteLessonTest`, same rules. R5-S7 uses `Parallel.run`
- [x] 1.6 The stubs answer 501 to any request shape: `PATCH` with a JSON body, `PATCH .../position` with a JSON body, `DELETE` without a body, for any ids. Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify each new test fails at its THEN step (not in setup). Some tests may pass against the stubs: pure 400 input checks (if validation runs before the stub), 403/401/CSRF scenarios (R3-S6, R4-S6, R4-S7 in part, R5-S5, R5-S6) and malformed-id 404s. Report each new test that passes against the stubs, with the reason; do not claim that every other test fails
- [x] 1.7 List the stubs (temporary), `V15__lessons_one_course.sql` and the rewritten `LessonRepository` queries (both permanent) in the PR description
- [x] 1.8 In the test-gate request list the `LessonRepository` diff for review as production code, next to the migration

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no further production code before approval (the migration and the repository rewrite of 1.1 are reviewed here as the stated exception)

## 3. Implementation

- [x] 3.1 Repository and requests: `LessonRepository` methods lookups of a lesson and its position scoped by course and teacher, `updateTitle`, `shiftRange`, `setPosition`, `deleteReturningPosition`, `closeGap` (design D4); `RenameLessonRequest` (`@Title`, `Normalize.name`) and `MoveLessonRequest` (`BigInteger position`, `@NotNull`) with the `BigInteger` coercion config: `Fail` for String, Float, Boolean and EmptyString input in the new `StrictIntegerJsonConfig` customizer (design D3)
- [x] 3.2 `LessonService.rename`, `move` and `delete`: user row lock first (`findActiveByIdForUpdate`, none gives 401), then the course row lock scoped by teacher (none gives 404), then the flow of design D4; the controller replaces its stubs (`PATCH`, `PATCH .../position`, `DELETE` 204). Add a comment at the locks saying the user row comes first to avoid a deadlock with `CourseService.rename`
- [x] 3.3 Check that no lesson title appears in logs (constitution §5: record IDs, not content)
- [x] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `lessons` Purpose line in the main spec to cover rename, move and delete.
- Mark the PR ready for the final review by a code owner other than the engineer.
