# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `ReorderPayload`, `ReorderEntry` and `ReorderItemType` (code `reorder`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/ReorderPayloads` (entry and payload builders, the 25-entry, 25-unit, 200-unit and 201-unit, 100-, 500-letter, emoji and `hint` payloads)
- [x] 1.3 Write tests for item-reorder R1-S1 to R1-S6 in `items/api/ReorderItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Write tests for item-reorder R3-S1 to R3-S5 in `items/domain/reorder/ReorderItemTypeTest` (unit level; R3-S1 to R3-S3 pin the shuffled orders; application JSON mapper for R3-S4); verify each fails at its THEN step
- [x] 1.5 Write tests for item-reorder R2-S1 to R2-S11 in `items/api/ReorderPayloadTest`; verify each fails at its THEN step
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping R1-S1 to R1-S4 and R1-S6, the accept paths R2-S1 and the accepting cases of R2-S7, JSON type errors and unknown fields R2-S8, NUL and surrogate R2-S11), and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `ReorderPayload` and `ReorderEntry` with the Bean Validation of design D2 (`entries` `@NotEmpty @Size(max = 25)`, `units` `@NotEmpty @Size(min = 2, max = 25)`). `ReorderItemType.extraChecks` stays the empty placeholder from 1.1 until 3.2, so in this step R2-S1 (including (g) and (h)), R2-S2 (a) and (b), R2-S3, R2-S8, R2-S9 (a) and (b) and R2-S11 pass; every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [x] 3.2 `ReorderItemType.extraChecks`: the mode, the null entry, the unit rules (blank, length by mode only for a valid mode), the distinct-unit rule, the total-units rule and the hint rules (design D2); verify R1-S5, R2-S2 (c) to (h), R2-S4, R2-S5, R2-S6, R2-S7, R2-S9 (c) and R2-S10 pass
- [x] 3.3 `ReorderItemType.studentView` as an allowlist with the sort, the seed, the shuffle and the rotate of design D4, throwing on corrupt data; verify R3-S1, R3-S2, R3-S3, R3-S4 and R3-S5 pass
- [x] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S4 and R1-S6); check that the payload records have `@Size` on the lists, no `Map` field, and that no unit or hint text is logged (constitution section 5)
- [x] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.6 Code-gate fixes: `isKnownMode` and `hasTwoDistinctUnits` helpers, one guard per line in `requireComplete`, and the composed and decomposed "é" written as Java escapes in the tests

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-reorder` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
