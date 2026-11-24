# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V17__course_assignments.sql` (design D1) so the tests compile against the real schema; renumber if `main` already has a `V17` at the time
- [x] 1.2 Add stubs returning 501, marked `STUB`, for `PUT`, `GET` and `DELETE` under `/api/teacher/courses/{courseId}/students` (design D3)
- [x] 1.3 Add `ApiTest` helpers `assignCourse` (direct insert) and a read of the assignment rows with `created_at` (design D4)
- [x] 1.4 Write tests for course-assignment R1-S1 to R1-S8 in `courseassignment/api/AssignCourseTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, students, assignments and sessions in GIVEN with the helpers, never through the endpoints under test. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. R1-S8 uses `Parallel.run`; the anonymous client fetches a CSRF cookie first (R1-S6)
- [x] 1.5 Write tests for R2-S1 to R2-S6 in `courseassignment/api/CourseStudentsTest`, same rules
- [x] 1.6 Write tests for R3-S1 to R3-S7 in `courseassignment/api/UnassignCourseTest`, same rules. R3-S6 removes the student with the existing endpoint and R3-S7 deactivates the teacher with the existing admin endpoint, as in student-enrolment R4 and R7
- [x] 1.7 The stubs answer 501 to any request shape. Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify each new test fails at its THEN step (not in setup). Against the stubs the 403 and 401 steps pass, because security runs before the controller; row-level cascade assertions (R3-S6, R3-S7) may pass at pass 1, because the migration is pass-1 production code, while list assertions fail on the 501; record both in the PR and report each new test that passes against the stubs, with the reason
- [x] 1.8 List the stubs (temporary) and the migration `V17` (permanent) in the PR description

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 `courseassignment/persistence`: repository on `JdbcClient` with `insertIfAbsent`, `delete` (returns whether a row was removed), `findStudentsOfCourse`, `studentOfTeacherExists`, and the course row lock (`CourseRepository.lockCourse`, design D3; the copies in `LessonRepository` and `PartRepository` stay); the domain record `CourseStudent`; the response record `CourseStudentResponse(id, name, assignedAt)` lives in `api/dto`
- [x] 3.2 `CourseAssignmentService.assign` and `unassign` in the lock order teacher row, course row, student check (design D3), and `list`; the controller replaces its stubs (`PUT` 204, `DELETE` 204, `GET` 200). Add a comment at the locks saying the teacher row comes first as in `LessonService` and `StudentRemovalService`
- [x] 3.3 Check that no course title or student name appears in logs (constitution section 5: record IDs, not content)
- [x] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Rebase on `main` after `assignment-end-voids-links` (#182) merges, and renumber `V17` if needed; post the gate range again after the rebase.
- Mark the PR ready for the final review by a code owner other than the engineer.
