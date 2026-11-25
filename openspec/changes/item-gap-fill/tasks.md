# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs so the tests compile and fail at their THEN step: `GapFillPayload`, `GapFillSegment` and `GapFillItemType` (code `gap-fill`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [ ] 1.2 Write tests for item-gap-fill R1-S1 to R1-S7 in `items/api/GapFillItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [ ] 1.3 Write tests for item-gap-fill R1-S8 and R1-S9 in `items/domain/gapfill/GapFillItemTypeTest` (unit level, application JSON mapper for the serialisation test); verify each fails at its THEN step
- [ ] 1.4 Write tests for item-gap-fill R2-S1 to R2-S11 in `items/api/GapFillPayloadTest`; build the large payloads (101 segments, 1000-letter texts, 200-letter answers, emoji answers) in test helpers; verify each fails at its THEN step
- [ ] 1.5 Write one unit test without a scenario ID: the strict mapper still gives `UNKNOWN_FIELD` for an unknown property on `GapFillSegment` (design D2)
- [ ] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step, not in setup, and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `GapFillPayload` and `GapFillSegment` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 101)` and `List<@Valid GapFillSegment>`, `@Size(max = 1000)` on `text`, `@Size(max = 10)` on `answers`); verify R2-S1 (limits), R2-S2 (a), (d), (e) and R2-S10 pass
- [ ] 3.2 `GapFillItemType.extraChecks`: the per-segment rules, adjacent texts, answer rules, the 50-gap limit and the gap-less payload (design D3); verify R2-S2 (b), (c), R2-S3 to R2-S9 and R2-S11 pass
- [ ] 3.3 `GapFillItemType.studentView` as an allowlist (design D5); verify R1-S8 and R1-S9 pass
- [ ] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S7); check that the payload records have `@Size` on each list and text field, no `Map` field, and that no answer or text is logged (constitution section 5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-gap-fill` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
