# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `SortPayload`, `SortColumn` and `SortItemType` (code `sort`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/SortPayloads` (column and payload builders, the 50-word, 100-letter and emoji payloads)
- [x] 1.3 Write tests for item-sort R1-S1 to R1-S6 in `items/api/SortItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Write tests for item-sort R1-S7 to R1-S10 in `items/domain/sort/SortItemTypeTest` (unit level; R1-S7 and R1-S8 pin the shuffled orders; application JSON mapper for R1-S9); verify each fails at its THEN step
- [x] 1.5 Write tests for item-sort R2-S1 to R2-S13 in `items/api/SortPayloadTest`; verify each fails at its THEN step
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping R1-S1 to R1-S4 and R1-S6, the accept paths R2-S1 and R2-S9, JSON type errors and unknown fields R2-S11, NUL and surrogate R2-S13), and every scenario ID has a test

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `SortPayload` and `SortColumn` with the Bean Validation of design D2 (`@NotEmpty @Size(min = 2, max = 10)` and `List<@Valid SortColumn>` on `columns`; `@NotEmpty @Size(max = 25)` on `words`). `SortItemType.extraChecks` stays the empty placeholder from 1.1 until 3.2; verify that the scenarios whose errors come only from Bean Validation or JSON reading pass and that the others still fail at their THEN step
- [x] 3.2 `SortItemType.extraChecks`: the null column, the title and word rules (blank, length, exact duplicates, a value with its own error not compared), the total of 50 (design D2); verify the remaining R2 scenarios and R1-S5 pass
- [x] 3.3 `SortItemType.studentView` as an allowlist with the sort, the seed and the shuffle of design D4, throwing on corrupt data; verify R1-S7, R1-S8, R1-S9 and R1-S10 pass
- [x] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S4 and R1-S6); check that the payload records have `@Size` on each list, no `Map` field, and that no title or word is logged (constitution section 5)
- [x] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.6 Code-gate fixes: `assertNoEcho` on the R2-S2 (a) and (f) bodies, a `hasNullWord` helper in `SortItemType.requireComplete`, Java escapes for `EMOJI` and the U+FFFD literal in the tests

## 4. Code-owner review fixes

- [x] 4.1 Update the tests of R1-S7 and R1-S8 in `SortItemTypeTest` to the new pinned orders (seed from the sorted words); the reordered-columns case asserts the same words as the original, and add the case where `cat` and `apple` swap columns with the same view; verify each fails at its THEN step on the current code
- [ ] 4.2 Test review: engineer approves the changed tests in the PR. HARD STOP: no production code before approval
- [ ] 4.3 `SortItemType.shuffledWords`: seed from `String.join("\u0000", sorted words).hashCode()`, shuffle the sorted words; verify R1-S7 and R1-S8 pass
- [ ] 4.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and coverage holds

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-sort` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
