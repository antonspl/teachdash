# Tasks

## 1. Failing tests

- [ ] 1.1 Add the migration `V19__item_title.sql` (`title varchar(200)`, nullable) and add `title` to `CreateItemRequest`, `EditItemRequest`, `ItemResponse` and `Item` with null placeholders, so the tests compile and fail at their THEN step; verify with `./gradlew compileTestJava`. List the migration (permanent) in the PR description
- [ ] 1.2 Add an `ApiTest.createItem` overload with a title (and an item row reader returning `title` if the existing one does not), without calling the endpoints under test; verify the helpers are used by the tests below
- [ ] 1.3 Write tests for items R1-S1 (changed) and R1-S11 to R1-S16 in `AddItemTest` with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [ ] 1.4 Update the exact-key assertions of `ListItemsTest` (R3-S1), `EditItemTest` (R4-S1) and `MoveItemTest` (R6-S1) to the new properties and titles, and write R4-S11 to R4-S14 in `EditItemTest`; verify each fails at its THEN step
- [ ] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new or changed test fails at its THEN step, not in setup, and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `ItemRepository`: `insertAtEnd`, `findInPart`, `setPosition`, `findByPart`, `updatePayload` (payload and title in one statement) and `toItem` carry `title`; verify `ListItemsTest`, `MoveItemTest` and R4-S1 pass
- [ ] 3.2 Title validation: normalise with `Normalize.name`, blank to null, validate a non-null value with the `@Title` rules and `FieldErrorCodes` (one shared helper, design D2); add its errors to `validateEnvelope` and `validateEditEnvelope`; verify R1-S13 to R1-S16 and R4-S13, R4-S14 pass
- [ ] 3.3 `ItemService.create` and `edit` take and store the title (a missing or null title clears it on edit); `ItemResponse` returns `title` for create, list, edit and move; verify R1-S1, R1-S11, R1-S12, R4-S11 and R4-S12 pass
- [ ] 3.4 Check that no title text appears in logs (constitution section 5), then run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `items` Purpose line in the main spec to mention the title.
- Mark the PR ready for the final review by a code owner other than the engineer.
