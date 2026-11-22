# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs returning 501, marked `STUB`, for `PATCH .../items/{itemId}/position` and `DELETE .../items/{itemId}` under `/api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}` (design D1). No migration is needed
- [x] 1.2 Add `ApiTest` helpers reading one item's `updatedAt` and `version`, and the `updatedAt` of the part, the lesson and the course, if the existing helpers do not cover it (design D6). Whichever of this PR and PR #186 (items-read-edit: `ItemRepository.lockItem`, an item row reader in `support/ApiTest`, `createItem` with a `version` argument) merges second reuses the first one's helpers instead of adding its own
- [x] 1.3 Write tests for items R6-S1 to R6-S8 in `items/api/MoveItemTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons, parts, items and sessions in GIVEN with the helpers, never through the endpoints under test or the list endpoint. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. In R6-S6 the anonymous client fetches a CSRF cookie first (also in R7-S4); courses use `en` and `ru` instruction languages. R6-S8 uses `Parallel.run`
- [x] 1.4 Write tests for items R7-S1 to R7-S8 in `items/api/DeleteItemTest`, same rules. R7-S6, R7-S7 and R7-S8 use `Parallel.run`
- [x] 1.5 The stubs answer 501 to any request shape. Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify each new test fails at its THEN step (not in setup). Against the 501 stubs only the 403 role and CSRF cases and the 401 deactivated and no-session cases pass, because security runs before the controller (the whole of R6-S6, R7-S4 and R7-S5, and the 403 and 401 steps of R6-S7); everything else fails at THEN. Report each new test that passes against the stubs, with the reason
- [x] 1.6 List the stubs (temporary) in the PR description; there is no migration

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 `ItemRepository`: lookup of an item and its position scoped by part and teacher, `shiftRange`, `setPosition`, delete returning the position, `closeGap` (design D3); `MoveItemRequest` (`BigInteger position`, `@NotNull`; the strict coercion already exists, design D2)
- [ ] 3.2 `ItemService.move` and `delete`: one lock helper shared with `create`, kept compatible with `lockItem` of PR #186 (whichever merges second reuses the other's helper), in the order user row (`findActiveByIdForUpdate`, none gives 401), course row, lesson row, part row, then the item lookup (design D3); the controller replaces its stubs (`PATCH .../position`, `DELETE` 204)
- [ ] 3.3 Check that no payload content appears in logs (constitution §5: record IDs, not content)
- [ ] 3.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `items` Purpose line in the main spec to cover move and delete.
- Mark the PR ready for the final review by a code owner other than the engineer.
