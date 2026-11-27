# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `MatchingPayload`, `MatchingPair` and `MatchingItemType` (code `matching`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/MatchingPayloads` (pair and payload builders, the 25-pair, 100-letter and emoji payloads)
- [x] 1.3 Write tests for item-matching R1-S1 to R1-S7 in `items/api/MatchingItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Write tests for item-matching R1-S8 to R1-S11 in `items/domain/matching/MatchingItemTypeTest` (unit level; R1-S8 and R1-S9 pin the shuffled orders; application JSON mapper for R1-S10); verify each fails at its THEN step
- [x] 1.5 Write tests for item-matching R2-S1 to R2-S11 in `items/api/MatchingPayloadTest`; verify each fails at its THEN step
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping R1-S1 to R1-S5 and R1-S7, the accept paths R2-S1, R2-S7, JSON type errors and unknown fields R2-S9, NUL and surrogate R2-S11), and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `MatchingPayload` and `MatchingPair` with the Bean Validation of design D2 (`@NotEmpty @Size(min = 2, max = 25)` and `List<@Valid MatchingPair>`). `MatchingItemType.extraChecks` stays the empty placeholder from 1.1 until 3.2, so in this step R2-S1, R2-S2 (a), R2-S3 (the `{}`, `null`, `[]` and one-pair cases), R2-S9, R2-S10 (a) and (b) and R2-S11 pass; every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [ ] 3.2 `MatchingItemType.extraChecks`: the null pair, the `left` and `right` rules (blank, length, exact duplicates per side, a value with its own error not compared; design D2); verify R1-S6, R2-S2 (b) to (e), R2-S4, R2-S5, R2-S6, R2-S7, R2-S8 and R2-S10 (c) pass
- [ ] 3.3 `MatchingItemType.studentView` as an allowlist with the sort, the seed and the shuffle of design D4, throwing on corrupt data; verify R1-S8, R1-S9, R1-S10 and R1-S11 pass
- [ ] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S5 and R1-S7); check that the payload records have `@Size` on the list, no `Map` field, and that no pair text is logged (constitution section 5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-matching` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
