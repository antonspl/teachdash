# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs returning 501, marked `STUB`, for `GET /api/teacher/courses`, `GET /api/teacher/courses/{id}` and `PATCH /api/teacher/courses/{id}` in `CourseController` (design D1, D3). No migration is needed
- [x] 1.2 Add `ApiTest` helpers if missing: a course insert with a given `created_at`, a read of a course row (title, languages, owner, `created_at`, `updated_at`) by direct SQL
- [x] 1.3 Write tests for courses R3-S1 to R3-S8 in `courses/api/ListCoursesTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses and sessions in GIVEN with the helpers, never through the stubbed endpoints. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. R3-S3 inserts three courses with one `created_at` and fixed ids (`00000000-0000-0000-0000-000000000001` to `...03`), so Java and Postgres UUID order cannot disagree
- [x] 1.4 Write tests for courses R4-S1 to R4-S3 in `courses/api/ViewCourseTest`. 404 assertions use `.body("$", not(hasKey("errors")))`. In R4-S3 the anonymous client fetches a CSRF cookie first
- [x] 1.5 Write tests for courses R5-S1 to R5-S10 in `courses/api/RenameCourseTest`. Advance the test clock (`testClock()`) between creating the course and renaming it, so `updatedAt` differs from `createdAt`. R5-S10 uses `Parallel.run`. R5-S9 uses valid bodies and a CSRF token except for teacher A's request
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup) and every other test passes. Report to the engineer any new test that passes against the stubs (the 401/403 parts of R3-S8, R4-S3 and R5-S9 may pass through the existing security rules, but the tests still fail at the other asserts)
- [x] 1.7 List the stubs (temporary) in the PR description; there is no migration

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 `CourseRepository`: `findByTeacher` (order `created_at desc, id`), `findByIdAndTeacher`, `updateTitle(id, teacherId, title, updatedAt)` returning the row count (design D2)
- [x] 3.2 `CourseService`: `list` and `get` (404 `NOT_FOUND` through the owner in the query); extract the `courses_teacher_title_uq` to `TITLE_TAKEN` translation from `create` into one method (design D3); verify the courses R1 tests still pass
- [x] 3.3 `RenameCourseRequest(title)` with `@CourseTitle` and `Normalize.name`; `CourseService.rename` with the teacher row lock, the update setting `updated_at` from the clock, 404 on zero rows and the shared `TITLE_TAKEN` translation (design D3); add a comment at the row lock saying why it is taken
- [x] 3.4 `CourseController`: replace the three stubs, mapping to `CourseResponse.from` (design D1). The PATCH handler declares `@PathVariable UUID id` before `@Valid @RequestBody`, as `TeacherStudentController.changeLanguage` does, so a malformed id is 404 before body validation (design D3)
- [x] 3.5 Check that no course title appears in logs (constitution §5: record IDs, not content)
- [x] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## 4. Code-review fix: no scalar coercion

- [x] 4.1 Write the failing test for courses R5-S11 (a new test in `RenameCourseTest`; the approved tests stay unchanged)
- [ ] 4.2 Turn off scalar-to-string coercion in the shared Jackson config (design D5)
- [ ] 4.3 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the suite is green and no other endpoint relied on the coercion

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
