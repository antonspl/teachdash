# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V17__items.sql` (design D1, with the unique `(id, teacher_id)` on `parts` and the composite foreign key; no test for the constraint). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [ ] 1.2 Add the types and the registry skeleton the tests need to compile (the `ItemType` interface and `ItemTypeRegistry`, design D3), the property `teachdash.items.max-per-part=50`, the public `ApiException.fields(List)` factory in `common`, and `FieldErrorCodes` made public with the new `UNKNOWN_TYPE` constant (design D3), with no behaviour. Add a stub returning 501, marked `STUB`, for `POST .../parts/{partId}/items` under `/api/teacher/courses/{courseId}/lessons/{lessonId}` (design D2)
- [ ] 1.3 Add the test-only type `test-note` in test sources (design D3: schema version 2, `text` required, at most 1000 characters, `hint` and other fields kept, projection `{text}` only)
- [ ] 1.4 Add `ApiTest` helpers `createItem(teacherId, partId, type, payload, position)` (inserts the `items` row directly, times from the test `Clock`), `countItems(partId)`, `itemPositions(partId)` and a reader of one item row (design D8)
- [ ] 1.5 Write tests for items R1-S1 to R1-S10 in `items/api/AddItemTest` and R2-S1 to R2-S9 in `items/api/ItemPayloadTest` and api-errors R1-S11 in `items/api/ItemTypeErrorTest`, with the scenario ID in `@DisplayName` (`[items/R1-S1] ...`) and `// given`, `// when`, `// then`. Create courses, lessons, parts, items and sessions in GIVEN with the helpers, never through the stubbed endpoint. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. R1-S5 and R1-S10 use `Parallel.run`; R1-S10 deletes the part with a direct SQL or the existing part delete endpoint. In R1-S8 the anonymous client fetches a CSRF cookie first
- [ ] 1.6 Write unit tests with no scenario ID: the registry fails startup on a duplicate code, and the `test-note` projection returns `{text}` only and drops `hint` (design D3), and one database test: inserting an item with teacher B's id on teacher A's part fails with an integrity error (design D1)
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup). Against the 501 stub only the 403 role and CSRF cases and the 401 cases pass, because security runs before the controller (the whole of R1-S8 and the 403 and 401 steps of R1-S9); the unit tests may pass or fail as their subject exists. Report each new test that passes against the stub, with the reason
- [ ] 1.8 List the stub (temporary) and `V17__items.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 `ItemTypeRegistry`: collect the `ItemType` beans, fail startup on a duplicate code (design D3)
- [ ] 3.2 `CreateItemRequest` (`String type`, `JsonNode payload`) and `ItemResponse`; the payload check of design D4 (type, required, object, 64 KiB size on the compact serialisation, NUL walk over values and keys, then the type's `validate`) in the controller before the service, using the `common` additions of design D3 (`ApiException.fields`, the public `FieldErrorCodes.UNKNOWN_TYPE` in the default priority tier)
- [ ] 3.3 `ItemRepository` on `JdbcClient`: part lock scoped by lesson and teacher, count, insert at the end with `cast(:payload as jsonb)` returning the stored payload and the position, read back with `readTree` (design D6)
- [ ] 3.4 `ItemService.create` in the lock order of design D7 (user row, course, lesson, part, limit check, insert) with the limit from `teachdash.items.max-per-part`; the controller replaces its stub (201). Add a comment at the locks saying the user row comes first to avoid a deadlock with `CourseService.rename`
- [ ] 3.5 Check that no payload content appears in logs (constitution §5: record IDs, not content)
- [ ] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
