# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs returning 501, marked `STUB`, for `PATCH .../parts/{partId}`, `PATCH .../parts/{partId}/position` and `DELETE .../parts/{partId}` under `/api/teacher/courses/{courseId}/lessons/{lessonId}` (design D1). No migration is needed
- [ ] 1.2 Add an `ApiTest` helper reading one part's `updatedAt`, and the `updatedAt` of the lesson and of the course, if the existing helpers do not cover it (design D6)
- [ ] 1.3 Write tests for parts R3-S1 to R3-S6 in `parts/api/RenamePartTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons, parts and sessions in GIVEN with the helpers, never through the endpoints under test. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. In R3-S5 the anonymous client fetches a CSRF cookie first (also in R4-S6 and R5-S4); courses use `en` and `ru` instruction languages as `CreatePartTest`
- [ ] 1.4 Write tests for parts R4-S1 to R4-S8 in `parts/api/MovePartTest`, same rules. R4-S8 uses `Parallel.run`
- [ ] 1.5 Write tests for parts R5-S1 to R5-S7 in `parts/api/DeletePartTest`, same rules. R5-S6 and R5-S7 use `Parallel.run`
- [ ] 1.6 The stubs answer 501 to any request shape. Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify each new test fails at its THEN step (not in setup). Against the 501 stubs only the 403 role and CSRF cases and the 401 deactivated and no-session cases pass, because security runs before the controller (the whole of R3-S5, R4-S6, R5-S4 and R5-S5, and the 403 and 401 steps of R3-S6 and R4-S7); everything else fails at THEN. Report each new test that passes against the stubs, with the reason
- [ ] 1.7 List the stubs (temporary) in the PR description; there is no migration

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 `PartRepository`: lookup of a part and its position scoped by lesson and teacher, `updateTitle`, `shiftRange`, `setPosition`, delete returning the position, `closeGap` (design D3); `RenamePartRequest` (`@Title`, `Normalize.name`) and `MovePartRequest` (`BigInteger position`, `@NotNull`; the strict coercion already exists, design D2)
- [ ] 3.2 `PartService.rename`, `move` and `delete`: one lock helper in the order user row (`findActiveByIdForUpdate`, none gives 401), course row, lesson row, then the part lookup (design D3); the controller replaces its stubs (`PATCH`, `PATCH .../position`, `DELETE` 204). Add a comment at the locks saying the user row comes first to avoid a deadlock with `CourseService.rename`
- [ ] 3.3 Check that no part title appears in logs (constitution §5: record IDs, not content)
- [ ] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `parts` Purpose line in the main spec to cover rename, move and delete.
- Mark the PR ready for the final review by a code owner other than the engineer.
