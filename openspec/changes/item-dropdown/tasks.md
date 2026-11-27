# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `DropdownPayload`, `DropdownSegment` and `DropdownItemType` (code `dropdown`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/DropdownPayloads` (copied from `GapFillPayloads`, adapted to `options` and `correct`)
- [x] 1.3 Write tests for item-dropdown R1-S1 to R1-S7 in `items/api/DropdownItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Write tests for item-dropdown R1-S8 and R1-S9 in `items/domain/dropdown/DropdownItemTypeTest` (unit level; both assert the `{"segments": [...]}` wrapper; application JSON mapper for the serialisation test); verify each fails at its THEN step
- [x] 1.5 Write tests for item-dropdown R2-S1 to R2-S14 in `items/api/DropdownPayloadTest`; build the large payloads (51 segments, 500-letter texts, 100-letter options, emoji texts and options counted in UTF-16 units: 250 and 251 emoji in a text, 50 and 51 in an option) in test helpers; verify each fails at its THEN step
- [x] 1.6 Write unit tests without a scenario ID: the strict mapper still gives `UNKNOWN_FIELD` for an unknown property on `DropdownSegment`, and `INVALID_TYPE` for a string boolean or fractional `correct` (design D2); and `@Size(min = 2)` on a list gives `TOO_SHORT` in `FieldErrorCodes` (design D3)
- [x] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping, accept paths, NUL/surrogate handling, JSON type errors, the D2 strict-mapper tests), and every scenario ID has a test

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `DropdownPayload` and `DropdownSegment` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 51)` and `List<@Valid DropdownSegment>`, `@Size(max = 500)` on `text`, `@Size(min = 2, max = 5)` on `options`). `DropdownItemType.extraChecks` stays the empty placeholder from 1.1 until 3.2, so in this step the accept scenarios, R2-S2 (a), (b), (e), (f), R2-S4 (the `TOO_SHORT` cases), R2-S7 (g), R2-S9 and R2-S11 (first, fourth and fifth case) pass because Bean Validation and the JSON reading already give the expected errors, and every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [x] 3.2 `extraChecks`, segment structure: null segment, `type` rules, the text rule (only "" and missing are `REQUIRED`, not `@NotBlank`), fields of the other segment type (`UNKNOWN_FIELD`), adjacent texts; verify R2-S3 (a) to (c), R2-S6, R2-S7 (a) to (c), R2-S8 pass
- [x] 3.3 `extraChecks`, options: missing `options` (`REQUIRED`), blank or null option, option length, exact duplicates; verify R2-S2 (c), (d), R2-S4, R2-S5 and the option cases of R2-S11 pass
- [x] 3.4 `extraChecks`, `correct`: missing (`REQUIRED`), range (`INVALID_FORMAT`), skipped when the gap's `options` has an error; verify R2-S10 and R1-S6 pass
- [x] 3.5 `extraChecks`, gap count: more than 25 gaps (`TOO_LONG`) and the gap-less payload (`REQUIRED`); a gap with missing fields counts as a gap, a null, typeless or unknown-type segment does not; verify R2-S2 (g), R2-S3 (d), R2-S12, R2-S13 and R2-S14 pass
- [x] 3.6 `DropdownItemType.studentView` as an allowlist (design D5); returns the `{"segments": [...]}` envelope, throws on corrupt data; verify R1-S8 and R1-S9 pass
- [x] 3.7 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S7); check that the payload records have `@Size` on each list and text field, no `Map` field, that no shared class changed, and that no option or text is logged (constitution section 5)
- [x] 3.8 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.9 Slice-review fixes: `checkType` and `checkGapCount` helpers extracted from `extraChecks`; the limits are public constants in `DropdownItemType`, also used by the record annotations

## 4. Integer overflow (amendment)

- [x] 4.1 Failing tests: items R2-S14 (test-note `rating` 2147483648, -2147483649, 99999999999999999999 and the accepted 2147483647) in the items payload tests, and the `correct` 2147483648 case of item-dropdown R2-S9; verify each overflow case fails at its THEN step with 500 (not in setup)
- [x] 4.2 Engineer approves the tests of 4.1 in the PR. HARD STOP: no production code before approval
- [x] 4.3 Map `InputCoercionException` to `INVALID_TYPE` at the field path in `JacksonErrors` and `ApiException.fromJackson` (design D8); verify items R2-S14 and R2-S9 pass
- [x] 4.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 4.5 Code-gate fixes: `ApiExceptionHandler` back to `DatabindException` (D8 scope); `jackson` for the renamed variables in `JacksonErrors`; `FieldErrorCodesTest` split lines and DisplayName (the engineer allowed this one change to an approved test); no stored type value in the `studentView` exception message

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-dropdown` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
