# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `FreeTextPayload` (the record without annotations) and `FreeTextItemType` (code `free-text`; `studentView` returns `null`, marked STUB; `extraChecks` empty); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Write tests for item-free-text R1-S1 to R1-S6 in `items/api/FreeTextItemTest` (helpers of `ItemApiTest`: `teacherWithPart`, `body`, `expectValidation`, `expectNotFound`, `assertStoredPayload`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step (those the generic code already provides are expected green on the stubs)
- [x] 1.3 Write tests for item-free-text R1-S7 and R1-S8 in `items/domain/freetext/FreeTextItemTypeTest` (unit level; application JSON mapper for the serialisation test, English and Russian); verify each fails at its THEN step
- [x] 1.4 Write tests for item-free-text R2-S1 to R2-S7 in `items/api/FreeTextPayloadTest`; build the large prompts (20000 and 20001 letters, 10000 and 10001 emoji) in a test helper; verify each fails at its THEN step
- [x] 1.5 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: add, edit, list, scoping, the accept paths, JSON type errors, unknown property), and every scenario ID has a test

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `FreeTextPayload` with the Bean Validation of design D2 (`@NotBlank @Size(max = 20000)` on `prompt`, `@Min(1) @Max(10000)` on `minLength` and `maxLength`); `FreeTextItemType.extraChecks` stays empty until 3.2. In this step R2-S1 to R2-S5 pass (the Bean Validation errors and the JSON type errors) and R2-S6 passes only for its accept cases, and every scenario that needs the order error (R2-S6 rejects, R2-S7) still fails at its THEN step; verify exactly that
- [x] 3.2 `FreeTextItemType.extraChecks`: `minLength > maxLength` gives `maxLength` `INVALID_FORMAT` when both are present (Bean Validation has already passed); verify R2-S6 and R2-S7 pass
- [x] 3.3 `FreeTextItemType.studentView` as an allowlist `FreeTextStudentView` with `@JsonInclude(NON_NULL)` (design D4); verify R1-S7 and R1-S8 pass
- [x] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S6); check that no prompt is logged (constitution section 5)
- [x] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.6 Code-gate fixes: `FreeTextItemTypeTest` reads the student view's values with `isInstanceOf`, a cast and accessors (the record-components check stays on purpose: R1-S7 asserts exactly those fields); `FreeTextPayloads.raw()` escapes the prompt as a JSON string (engineer allowed the test change)

## 4. Review round 2

- [x] 4.1 Failing tests: in `FreeTextPayloadTest` add R2-S1 case (e) (prompt of only U+00A0, 201), change R2-S7(b) to `minLength` 10001 and `maxLength` 1 (only `payload.minLength` `INVALID_FORMAT`), add R2-S8 (NUL and lone surrogate in `prompt`, `payload` `INVALID_FORMAT`, no item stored) and drop `blank.get(0)` from the R2-S3 no-echo check; add `json()` to `FreeTextPayloads` and stop importing `GapFillPayloads.json` in `FreeTextItemTest` and `FreeTextPayloadTest`; run each changed test and report which fail at their THEN step (those the generic code already provides are expected green)
- [ ] 4.2 Engineer approves tests in the PR. HARD STOP: no production code before approval
- [ ] 4.3 Code, only if a test from 4.1 fails for a behaviour of this type (none expected: the changes are test-side and use existing validation); otherwise note "no code needed"
- [ ] 4.4 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and every scenario ID (R2-S8 included) has a test

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-free-text` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
