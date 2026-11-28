# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `WordBoxPayload`, `WordBoxSegment` and `WordBoxItemType` (code `gap-fill-word-box`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/WordBoxPayloads` (copied from `GapFillPayloads`, adapted to `answer` and `extraWords`)
- [x] 1.3 Write tests for item-gap-fill-word-box R1-S1 to R1-S7 in `items/api/WordBoxItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step
- [x] 1.4 Write tests for item-gap-fill-word-box R1-S8 to R1-S11 in `items/domain/wordbox/WordBoxItemTypeTest` (unit level; assert the `{"segments", "wordBox"}` envelope; application JSON mapper for the serialisation test); verify each fails at its THEN step
- [x] 1.5 Write tests for item-gap-fill-word-box R2-S1 to R2-S13 in `items/api/WordBoxPayloadTest`; build the large payloads (51 segments, 500-letter texts, 100-letter words, emoji texts and words counted in UTF-16 units) in test helpers; verify each fails at its THEN step
- [x] 1.6 Write unit tests without a scenario ID: the strict mapper still gives `UNKNOWN_FIELD` for an unknown property on `WordBoxSegment` and `WordBoxPayload` (`WordBoxSegmentTest`); `INVALID_TYPE` for a number in `extraWords` is not needed as a unit test: the `@JsonTest` slice lacks the app's package-private `StrictStringJsonConfig`, so the number is coerced there, and R2-S10 already covers it end to end (design D2)
- [x] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: generic add/edit/list/scoping R1-S1 to R1-S5 and R1-S7, accept paths, NUL/surrogate handling, JSON type errors, the D2 strict-mapper tests), and every scenario ID has a test

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `WordBoxPayload` and `WordBoxSegment` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 51)` and `List<@Valid WordBoxSegment>`, `@Size(max = 500)` on `text`, `@Size(max = 10)` on `extraWords`). `extraChecks` stays the empty placeholder until 3.2, so in this step the accept scenarios (R1-S4, R2-S1), R2-S2 (a), (b), (e), (h), R2-S3 (e) to (g), R2-S8 (f), R2-S10, R2-S11 (first, fourth and fifth case) and R2-S12 pass because Bean Validation and the JSON reading already give the expected errors, and every scenario that needs an `extraChecks` error still fails at its THEN step; verify exactly that
- [x] 3.2 `extraChecks`, segment structure: null segment, `type` rules, the text rule (only "" and missing are `REQUIRED`, not `@NotBlank`), fields of the other segment type (`UNKNOWN_FIELD`), adjacent texts; verify R2-S3 (a) to (c), R2-S7, R2-S8 (a) to (e), R2-S9 pass
- [x] 3.3 `extraChecks`, words: missing or blank `answer` (`REQUIRED`), answer length, null or blank extra word, extra-word length; verify R2-S2 (c), (d), (f), (g), R2-S4 and R2-S5 pass
- [x] 3.4 `extraChecks`, duplicates: a repeated extra word and an extra word equal to a gap's answer (exact match), the same answer in two gaps allowed; verify R2-S6, the second and third case of R2-S11 and R1-S6 pass (R1-S6 also needs 3.5)
- [x] 3.5 `extraChecks`, gap count: more than 25 gaps (`TOO_LONG`) and the gap-less payload (`REQUIRED`); a gap with a missing answer counts as a gap, a null, typeless or unknown-type segment does not; verify R2-S2 (i), R2-S3 (d), R2-S13 and R1-S6 pass
- [x] 3.6 `WordBoxItemType.studentView` as an allowlist (design D5): the `{"segments", "wordBox"}` envelope, the sorted and seeded shuffle, throws on corrupt data; verify R1-S8 to R1-S11 pass
- [x] 3.7 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S7); check that the payload records have `@Size` on each list and text field, no `Map` field, that no shared class changed, and that no answer or word is logged (constitution section 5)
- [x] 3.8 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.9 Code-gate fixes: `wordBox` throws on a null word, `checkWord` returns whether the word is valid, the student-view records move to the end of the class, design D4 states that a word with a length or blank error is not checked for duplicates
- [x] 3.10 Code-owner review: R1-S11 case (e), null extra word

## 4. Review round 2

- [ ] 4.1 Failing tests: R1-S11 cases (f) `null` and (g) empty `segments`, and the no-stored-value message check for each case, in `WordBoxItemTypeTest`; R2-S11 sixth case in `WordBoxPayloadTest`; R1-S8 also asserts the second view has no "extra"; verify each new test fails at its THEN step (the R2-S11 case and the R1-S8 assertion may already pass)
- [ ] 4.2 Engineer approves the tests in the PR. HARD STOP: no production code before approval
- [ ] 4.3 Code: `studentView` throws `IllegalStateException` with a fixed message for a `null` or empty `segments` list and for an unknown type (no raw value in the message); in `wordBox` add the gap answer directly, without `requireWord(segment.answer())`
- [ ] 4.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-gap-fill-word-box` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
