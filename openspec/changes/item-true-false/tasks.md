# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs so the tests compile and fail at their THEN step: `TrueFalsePayload`, `TrueFalseStatement` (records without constraints) and `TrueFalseItemType` (code `true-false`; `studentView` returns a placeholder and `extraChecks` returns no errors, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [ ] 1.2 Write tests for item-true-false R1-S1 to R1-S6 in `items/api/TrueFalseItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step (R1-S1 to R1-S4 and R1-S6 are green on the stubs: generic add, edit, list and scoping; they are regression guards)
- [ ] 1.3 Write tests for item-true-false R1-S7 to R1-S9 in `items/domain/truefalse/TrueFalseItemTypeTest` (unit level; the application JSON mapper for the serialisation test; R1-S9 builds the corrupt payloads in memory); verify each fails at its THEN step
- [ ] 1.4 Write tests for item-true-false R2-S1 to R2-S10 in `items/api/TrueFalsePayloadTest`; build the large payloads (25 texts of 500 letters, 501 Cyrillic letters, 250 and 251 emoji counted in UTF-16 units, 26 statements) in test helpers; verify each fails at its THEN step (R2-S1, R2-S8 and R2-S9 are green on the stubs: accept paths, JSON type errors and unknown properties come from the generic code; they are regression guards)
- [ ] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup, and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `TrueFalsePayload` and `TrueFalseStatement` with the Bean Validation of design D2 (`@NotEmpty @Size(max = 25)` and `List<@Valid TrueFalseStatement>`, `@NotBlank @Size(max = 500)` on `text`, `@NotBlank` on `answer`). `extraChecks` stays the empty placeholder from 1.1 until 3.2, so in this step pass: R2-S2 (all), R2-S3 (a), (b), (c), R2-S4 (a) to (e), R2-S6 (a) to (d), R2-S10 (a) and (c), and R1-S5 second payload; every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [ ] 3.2 `TrueFalseItemType.extraChecks`: the null statement, the exact duplicate text, the answer value and the `not-stated` rule (design D2), all errors together; verify R2-S3 (d), R2-S5, R2-S6 (e) to (k), R2-S7, R2-S10 (b) and R1-S5 first payload pass
- [ ] 3.3 `TrueFalseItemType.studentView` as an allowlist (design D3); verify R1-S7, R1-S8 and R1-S9 pass
- [ ] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S6); check that the payload records have `@Size` on the list and the text, no `Map` field, and that no statement text is logged (constitution section 5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-true-false` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
