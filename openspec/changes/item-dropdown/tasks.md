# Tasks

## 1. Failing tests

- [ ] 1.1 Add stubs so the tests compile and fail at their THEN step: `DropdownPayload`, `DropdownSegment` and `DropdownItemType` (code `dropdown`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [ ] 1.2 Add the test helper `items/support/DropdownPayloads` (copied from `GapFillPayloads`, adapted to `options` and `correct`)
- [ ] 1.3 Write tests for item-dropdown R1-S1 to R1-S7 in `items/api/DropdownItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [ ] 1.4 Write tests for item-dropdown R1-S8 and R1-S9 in `items/domain/dropdown/DropdownItemTypeTest` (unit level; both assert the `{"segments": [...]}` wrapper; application JSON mapper for the serialisation test); verify each fails at its THEN step
- [ ] 1.5 Write tests for item-dropdown R2-S1 to R2-S14 in `items/api/DropdownPayloadTest`; build the large payloads (51 segments, 500-letter texts, 100-letter options, emoji texts and options counted in UTF-16 units: 250 and 251 emoji in a text, 50 and 51 in an option) in test helpers; verify each fails at its THEN step
- [ ] 1.6 Write unit tests without a scenario ID: the strict mapper still gives `UNKNOWN_FIELD` for an unknown property on `DropdownSegment`, and `INVALID_TYPE` for a string or boolean `correct` (design D2); and `@Size(min = 2)` on a list gives `TOO_SHORT` in `FieldErrorCodes` (design D3)
- [ ] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping, accept paths, NUL/surrogate handling, JSON type errors, the D2 strict-mapper tests), and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [ ] 3.1 `DropdownPayload` and `DropdownSegment` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 51)` and `List<@Valid DropdownSegment>`, `@Size(max = 500)` on `text`, `@Size(min = 2, max = 5)` on `options`; if `@Size(min)` does not give `TOO_SHORT`, do the minimum in `extraChecks`). `DropdownItemType.extraChecks` stays the empty placeholder from 1.1 until 3.2, so in this step the accept scenarios, R2-S2 (a), (b), (e), (f) and R2-S4 (the `TOO_SHORT` cases) and R2-S9 pass because Bean Validation and the JSON reading already give the expected errors, and every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [ ] 3.2 `DropdownItemType.extraChecks`: the per-segment rules, adjacent texts, option rules, `correct` presence and range (skipped when the gap's `options` has an error), the 25-gap limit and the gap-less payload (design D3); the empty-text rule rejects only "" (not `@NotBlank`), whitespace-only text is accepted, and a gap with missing `options` or `correct` counts as a gap while a null, typeless or unknown-type segment does not; verify R2-S2 (c), (d), (g), R2-S3 to R2-S8, R2-S10 to R2-S14 pass
- [ ] 3.3 `DropdownItemType.studentView` as an allowlist (design D5); returns the `{"segments": [...]}` envelope; verify R1-S8 and R1-S9 pass
- [ ] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S7); check that the payload records have `@Size` on each list and text field, no `Map` field, that no shared class changed, and that no option or text is logged (constitution section 5)
- [ ] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-dropdown` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
