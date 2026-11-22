# Tasks

## 1. Failing tests

- [ ] 1.1 Add migration `V17__items.sql` (design D1, edited in place: no `schema_version`, with the two check constraints, the unique `(id, teacher_id)` on `parts` and the composite foreign key; no test for the constraint). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [ ] 1.2 Add the types the tests need to compile (the generic `ItemType<P>`, `ItemTypeRegistry` and an `ItemPayloadValidator` skeleton, design D3 and D4), the property `teachdash.items.max-per-part=50`, the public `ApiException.fields(List)`, `fromJackson` and `validation` helpers as signatures, and `FieldErrorCodes` public with `UNKNOWN_TYPE` and `UNKNOWN_FIELD` (design D3), with no behaviour. Add a stub returning 501, marked `STUB`, for `POST .../parts/{partId}/items` under `/api/teacher/courses/{courseId}/lessons/{lessonId}` (design D2)
- [ ] 1.3 Add the test-only type `test-note` in test sources (design D3: record `TestNotePayload(@NotNull @Size(max = 1000) String text, @Size(max = 200) String hint)`, `studentView` returns `{text}` only)
- [ ] 1.4 Add `ApiTest` helpers `createItem(teacherId, partId, type, payload, position)` (inserts the `items` row directly, times from the test `Clock`, no `schema_version`), `countItems(partId)`, `itemPositions(partId)` and a reader of one item row (design D8)
- [ ] 1.5 Write tests for items R1-S1 to R1-S10 in `items/api/AddItemTest` and R2-S1 to R2-S3 and R2-S5 to R2-S11 in `items/api/ItemPayloadTest` (R2-S4 is dropped), api-errors R1-S11 and R1-S12 in `items/api/ItemTypeErrorTest` and api-errors R1-S13 next to the existing NUL test; delete `ItemPayloadEdgeTest`, with the scenario ID in `@DisplayName` (`[items/R1-S1] ...`) and `// given`, `// when`, `// then`. Create courses, lessons, parts, items and sessions in GIVEN with the helpers, never through the stubbed endpoint. GIVEN has no asserts. The WHEN step only captures responses, so each test fails in THEN. R1-S5 and R1-S10 use `Parallel.run`; R1-S10 deletes the part with a direct SQL or the existing part delete endpoint. In R1-S8 the anonymous client fetches a CSRF cookie first
- [ ] 1.6 Write unit tests with no scenario ID: the registry fails startup on a duplicate code, the `test-note` view returns `{text}` only and drops `hint` (design D3), `Integer` and `Long` parsing is strict (string and fraction give `INVALID_TYPE`, design D4), and one database test: inserting an item with teacher B's id on teacher A's part fails with an integrity error (design D1)
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup). Against the 501 stub only the 403 role and CSRF cases and the 401 cases pass, because security runs before the controller (the whole of R1-S8 and the 403 and 401 steps of R1-S9); the unit tests may pass or fail as their subject exists. Report each new test that passes against the stub, with the reason
- [ ] 1.8 List the stub (temporary) and `V17__items.sql` (permanent) in the PR description

## 2. Test review

- [ ] 2.1 Engineer approves the reworked tests in the PR (re-approval after the rework). HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 `ItemTypeRegistry`: collect the `ItemType<?>` beans, fail startup on a duplicate code (design D3)
- [ ] 3.2 `CreateItemRequest` (`String type`, `Map<String,Object> payload`) and `ItemResponse`; the controller of a few lines and `ItemPayloadValidator` (design D4: convert with unknown properties rejected, Bean Validation, `extraChecks`), the `common` helpers `ApiException.fromJackson` (extracted from `ApiExceptionHandler`) and `ApiException.validation`, the codes `UNKNOWN_TYPE` and `UNKNOWN_FIELD` in the default priority tier
- [ ] 3.3 `ItemRepository` on `JdbcClient`: part lock scoped by lesson and teacher, count, insert at the end with `cast(:payload as jsonb)` taking the serialised record as a String and returning the stored payload and the position (design D5)
- [x] 3.4 `ItemService.create` in the lock order of design D7 (user row, course, lesson, part, limit check, insert) with the limit from `teachdash.items.max-per-part`; the controller replaces its stub (201). Add a comment at the locks saying the user row comes first to avoid a deadlock with `CourseService.rename`
- [x] 3.5 Check that no payload content appears in logs (constitution §5: record IDs, not content)
- [ ] 3.6 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## 4. Rework after the final review (pass 2, after the rework tests are approved)

- [ ] 4.1 Reject a lone surrogate in the String deserializer of `NulRejectingJsonConfig` (`INVALID_FORMAT`, all endpoints) and make `StrictIntegerJsonConfig` strict for `Integer` and `Long` (design D4)
- [ ] 4.2 Remove the payload tree walk and the 64 KiB check; check every payload record has `@Size` or `@MaxUtf8Bytes` on each String field and list, wrapper types with `@NotNull`, no `Double` or `Float` (design D3)
- [ ] 4.3 Add `request_body { max_size 1MB }` to the `handle /api/*` block of `infra/Caddyfile` (design D9). Proof, recorded in the PR: `caddy validate` passes and a request with a body over 1 MB gets 413
- [ ] 4.4 Check that no payload content appears in logs (constitution §5), including the unknown-field names in error responses
- [ ] 4.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
