# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs so the tests compile and fail at their THEN step: `TestPayload`, `TestQuestion`, `TestOption` and `TestItemType` in `items/domain/choice/` (code `test`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [ ] 1.2 Add the test helper `items/support/ChoicePayloads` (like `GapFillPayloads`, adapted to questions and options)
- [ ] 1.3 Write tests for item-test R1-S1 to R1-S5 and R3-S1 in `items/api/ChoiceItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [ ] 1.4 Write tests for item-test R3-S2 to R3-S5 in `items/domain/choice/ChoiceItemTypeTest` (unit level; assert the `{"questions"}` envelope and the key order; application JSON mapper for the serialisation test; records built directly for the corrupt data); verify each fails at its THEN step
- [ ] 1.5 Write tests for item-test R2-S1 to R2-S12 in `items/api/ChoicePayloadTest`; build the large payloads (25 questions, 500-letter texts, 100-letter options, emoji texts counted in UTF-16 units) in test helpers; verify each fails at its THEN step
- [ ] 1.6 Write unit tests without a scenario ID: in `items/domain/choice/ChoiceMapperTest` the strict mapper gives `UNKNOWN_FIELD` for an unknown property on `TestPayload`, `TestQuestion` and `TestOption`; and a unit test of `StrictBooleanJsonConfig` in `common/` (only `true` and `false` bind; `"true"`, `1`, `0`, `2`, `1.0` and `""` fail). Before relying on R2-S8, run its other cases (`"yes"`, `[]`, the list and text cases) against the mapper first and report any difference; keep the scenario as written
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping R1-S1 to R1-S3 and R1-S5, R3-S1, accept paths, NUL/surrogate handling, JSON type errors of strings and lists, the unknown-property tests), and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `TestPayload`, `TestQuestion` and `TestOption` with the Bean Validation of design D2 (lists `@NotEmpty` and `@Size`, texts `@NotBlank @Size`, `correct` `@NotNull`). `extraChecks` stays the empty placeholder until 3.2, so in this step the accept scenarios (R1-S1 to R1-S3, R2-S1, R3-S1), R1-S4, R2-S2, R2-S3 (the first three cases), R2-S4, R2-S5 (the first four cases), R2-S6, R2-S8, R2-S9, R2-S10, R2-S11 (a) and (d) pass because Bean Validation and the JSON reading already give the expected errors, and every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [ ] 3.2 `extraChecks`, structure: a null question and a null option (`REQUIRED`); verify R2-S3 (fourth case), R2-S5 (fifth case) and R2-S11 (b) pass
- [ ] 3.3 `extraChecks`, duplicates: a repeated option text in one question by exact match, the same text in two questions allowed; verify R2-S7 and R2-S11 (b) and (c) pass
- [ ] 3.4 `TestItemType.studentView` as an allowlist (design D5): the `{"questions"}` envelope, `multiple` from the count of correct options, stored order, throws on corrupt data; verify R3-S2 to R3-S5 pass
- [ ] 3.5 Add `common/StrictBooleanJsonConfig` (design D2a, mirroring `StrictStringJsonConfig`); grep that no existing request DTO or payload relies on Boolean coercion and report the result; verify R2-S12 and the `StrictBooleanJsonConfig` unit test pass and the existing tests stay green
- [ ] 3.6 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S5, R3-S1); check that the payload records have `@Size` on each list and text field, no `Map` field, that no shared class changed, and that no question or option is logged (constitution section 5)
- [ ] 3.7 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-test` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
