# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs returning 501, marked `STUB`, for `GET` and `PATCH .../parts/{partId}/items` and `.../items/{itemId}` (design D1). No migration is needed and no stub for the `theory` type: until it exists, adding a `theory` item gives `UNKNOWN_TYPE` and the tests fail at their THEN step
- [ ] 1.2 Add or extend `ApiTest` helpers: `createItem` with a `version` argument, a reader of one item row (type, payload as jsonb text, `version`, `updatedAt`), reading the `updatedAt` of a part, a lesson and a course, if the existing helpers do not cover it (design D7)
- [ ] 1.3 Write tests for items R3-S1 to R3-S5 in `items/api/ListItemsTest`, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`. Create courses, lessons, parts, items and sessions in GIVEN with the helpers, never through the endpoints under test
- [ ] 1.4 Write tests for items R4-S1 to R4-S10 in `items/api/EditItemTest`, same rules. R4-S9 and R4-S10 use `support.Parallel.run`
- [ ] 1.5 Write tests for items R5-S1 to R5-S6 in `items/api/TheoryItemTest`, same rules. A `theory` item in GIVEN is inserted with the helper. Write the unit test of the `theory` student view (no scenario ID)
- [ ] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify each new test fails at its THEN step (not in setup). Against the 501 stubs only the 403 role and CSRF cases and the 401 cases pass, because security runs before the controller (R3-S4, R3-S5, R4-S7 and the admin and deactivated steps of R4-S8)
- [ ] 1.7 List the stubs (temporary) in the PR description; there is no migration

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 `TheoryPayload` and `TheoryItemType` as a registered bean (design D5)
- [ ] 3.2 `ItemRepository`: `lockItem` (item row `for update`, scoped by item, part and teacher), `updatePayload` (design D3 step 6), `findByPart` ordered by position and a part existence check scoped by course, lesson and teacher (design D4); `EditItemRequest`
- [ ] 3.3 `ItemService.list` (read-only, no locks) and `edit` (lock order user, course, lesson, part, item; payload validation, then the version check; design D3); the controller replaces its stubs (`GET` 200, `PATCH` 200) with the envelope check of design D2 (`version` and `payload` `REQUIRED` reported together)
- [ ] 3.4 Check that no payload content and no property name of a rejected payload appears in logs or error responses, and that the 409 body has no payload (constitution §5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `items` Purpose line in the main spec to cover list, edit and the `theory` type.
- Mark the PR ready for the final review by a code owner other than the engineer.
