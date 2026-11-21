# Tasks

This is a refactor with no product behaviour change and `skip_specs: true`. The spec gate covers the proposal and design. There is no "Failing tests" section and no test review stop. The proof for each task is recorded in the PR and reviewed at the code gate. All tasks start after the spec gate.

## 1. Implementation

- [x] 1.1 `PasswordLinkService.invalidateLinksIssuedBy` `PasswordLinkService.invalidateLinksIssuedBy` takes a set of user ids and returns without a query for an empty set; add a set-based method to `OneTimeTokenRepository` and keep `markUnusedIssuedByAsUsed` single-user for `store()` (design D2). Verify: the module compiles, `store()` is unchanged
- [x] 1.2 Add `AssignmentEnding` with `end` and `endAll` in `enrolment/persistence`, with Javadoc saying the teacher row must already be locked (design D1, D5)
- [x] 1.3 Make `TeacherStudentRepository.delete` and `deleteAllOfTeacher` package-private (design D3). Verify: `grep -rn "assignments.delete\|deleteAllOfTeacher" backend/src/main` shows only `AssignmentEnding`, and the build compiles. Add Javadoc on both methods (package-private on purpose, use `AssignmentEnding`). The removal and deactivation tests, including those that spy `TeacherStudentRepository` (`JoinLinkFailureTest`), must pass; if the Spring proxy or the spy breaks, use the fallback in design D3 and report it
- [x] 1.4 `StudentRemovalService` and `TeacherDeactivationListener` call `AssignmentEnding`; the per-student loop goes (design D4)
- [x] 1.5 Update the mock in `TeacherDeactivationFailureTest` (R7-S4) and `TeacherDeactivationJoinLinkFailureTest` (R8-S9) to the set-based method, same assertions (design D6)
- [x] 1.6 Add the new test in `TeacherDeactivationTest`, without a scenario ID, for the teacher with several students and links (design D6). Verify that it passes, and that it fails if the void ignores `issued_by` (record this check in the PR)

## 2. Proof

- [x] 2.1 Run `../gradlew cleanTest test` from `backend/`: the full suite is green
- [x] 2.2 Run `scripts/check-scenario-coverage.sh`: it passes
- [x] 2.3 List the R4 and R7 tests (`StudentRemovalTest`, `TeacherDeactivationTest`, `TeacherDeactivationFailureTest`) and the R8-S9 test with `git diff main --stat` and `git diff main` over them: only the two mocks in 1.5 and the one added test in 1.6 differ. Also list the existing concurrency tests that touch these paths (none for removal or deactivation; `Parallel` is used only in the join and registration tests) and say no test covers the teacher-lock precondition. Record the list in the PR

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR. It merges nothing into `openspec/specs/` (`skip_specs: true`).
- Mark the PR ready for the final review by a code owner other than the engineer.
