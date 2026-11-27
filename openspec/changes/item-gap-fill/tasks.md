# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `GapFillPayload`, `GapFillSegment` and `GapFillItemType` (code `gap-fill`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Write tests for item-gap-fill R1-S1 to R1-S7 in `items/api/GapFillItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.3 Write tests for item-gap-fill R1-S8 and R1-S9 in `items/domain/gapfill/GapFillItemTypeTest` (unit level; both assert the `{"segments": [...]}` wrapper; application JSON mapper for the serialisation test); verify each fails at its THEN step
- [x] 1.4 Write tests for item-gap-fill R2-S1 to R2-S15 in `items/api/GapFillPayloadTest`; build the large payloads (51 segments, 500-letter texts, 100-letter answers, emoji texts and answers counted in UTF-16 units: 250 and 251 emoji in a text, 50 and 51 in an answer) in test helpers; verify each fails at its THEN step
- [x] 1.5 Write one unit test without a scenario ID: the strict mapper still gives `UNKNOWN_FIELD` for an unknown property on `GapFillSegment` (design D2)
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping, accept paths, NUL/surrogate handling, JSON type errors, the D2 strict-mapper test), and every scenario ID has a test

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `GapFillPayload` and `GapFillSegment` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 51)` and `List<@Valid GapFillSegment>`, `@Size(max = 500)` on `text`, `@Size(max = 5)` on `answers`). `GapFillItemType.extraChecks` stays the empty placeholder from 1.1 (no errors) until 3.2, so in this step R2-S1 (limits, all four payloads are valid), R2-S2 (a), (b), (e), (f) and R2-S10 pass (R2-S1 is already green on the stubs and stays green) because Bean Validation and the JSON reading already give the expected errors, and every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [ ] 3.2 `GapFillItemType.extraChecks`: the per-segment rules, adjacent texts, answer rules, the 50-gap limit and the gap-less payload (design D3); the empty-text rule rejects only "" (not `@NotBlank`), whitespace-only text is accepted, and a gap with missing or empty `answers` counts as a gap while a null, typeless or unknown-type segment does not; verify R2-S2 (c), (d), R2-S3 to R2-S9, R2-S11, R2-S12, R2-S13, R2-S14 and R2-S15 pass
- [ ] 3.3 `GapFillItemType.studentView` as an allowlist (design D5); returns the `{"segments": [...]}` envelope; verify R1-S8 and R1-S9 pass
- [ ] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S7); check that the payload records have `@Size` on each list and text field, no `Map` field, and that no answer or text is logged (constitution section 5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-gap-fill` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
