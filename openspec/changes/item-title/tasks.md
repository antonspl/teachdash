# Tasks

## 1. Failing tests

- [x] 1.1 Add the migration `V19__item_title.sql` (`title varchar(200)`, nullable) and add `title` to `CreateItemRequest`, `EditItemRequest`, `ItemResponse` and `Item` with null placeholders, so the tests compile and fail at their THEN step; verify with `./gradlew compileTestJava`. List the migration (permanent) in the PR description
- [x] 1.2 Add an `ApiTest.createItem` overload with a title and add `title` to `ApiTest.ItemRow` and `readItem` (they have none today), without calling the endpoints under test; verify the helpers are used by the tests below
- [x] 1.3 Write tests for items R1-S1 (changed) and R1-S11 to R1-S17 in `AddItemTest` with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Update the exact-key sets of `AddItemTest:69` (R1-S1), `ListItemsTest:47` (R3-S1), `MoveItemTest:124` (R6-S1) `EditItemTest:32` (R4-S1) and R4-S9 in `EditItemTest` to the new properties and titles; check `ItemApiTest` and `ItemPayloadTest` (they read `from items`) for key assertions and update them (not needed: neither has key assertions); write R4-S11 to R4-S15 in `EditItemTest`; verify each fails at its THEN step
- [x] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new or changed test fails at its THEN step, not in setup, and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `ItemRepository`: `insertAtEnd`, `findInPart`, `setPosition`, `findByPart`, `updatePayload` (payload and title in one statement, each written only when it differs from the stored value; `version` and `updated_at` always, design D4) and `toItem` carry `title`; check that the `Stored` record returned by `insertAtEnd` carries the title, or that `ItemService` uses the normalised input for the response; verify `ListItemsTest`, `MoveItemTest` and R4-S1 pass
- [x] 3.2 Title validation: normalise with `Normalize.name`, blank to null, validate a non-null value with the `@Title` rules and `FieldErrorCodes` (one shared helper, design D2); add its errors to `validateEnvelope` and `validateEditEnvelope`; verify R1-S13 to R1-S17 and R4-S13 to R4-S15 pass
- [x] 3.3 `ItemService.create` and `edit` take and store the title (a missing or null title clears it on edit); `ItemResponse` returns `title` for create, list, edit and move; verify R1-S1, R1-S11, R1-S12, R4-S11 and R4-S12 pass
- [x] 3.4 Check that no title text is logged (constitution section 5: record IDs, sizes and outcomes, not user text), then run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, update the `items` Purpose line in the main spec to mention the title.
- Mark the PR ready for the final review by a code owner other than the engineer.
