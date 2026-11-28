# Tasks

## 1. Failing tests

- [x] 1.1 Add stubs so the tests compile and fail at their THEN step: `NumericPayload`, `NumericQuestion` and `NumericItemType` (code `numeric`; `studentView` and `extraChecks` return a placeholder, each marked STUB); no migration; verify with `./gradlew compileTestJava`. List the stubs (temporary) in the PR description
- [x] 1.2 Add the test helper `items/support/NumericPayloads` (copied from the gap-fill payload helper, adapted to `text`, `answer`, `tolerance`, `unit`)
- [x] 1.3 Write tests for item-numeric R1-S1 to R1-S5 in `items/api/NumericItemTest` (helpers of `ItemApiTest`) with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`; verify each fails at its THEN step or is green on the stubs (generic add, edit, list and scoping)
- [x] 1.4 Write tests for item-numeric R1-S6 to R1-S9 in `items/domain/numeric/NumericItemTypeTest` (unit level; the application JSON mapper for the serialisation test); verify each fails at its THEN step
- [x] 1.5 Write tests for item-numeric R2-S1 to R2-S13 in `items/api/NumericPayloadTest`; build the large payloads (25 questions, 500-letter texts, emoji texts counted in UTF-16 units) in test helpers; verify each fails at its THEN step
- [x] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every scenario ID has a test and every test whose behaviour is missing fails at its THEN step, not in setup (tests whose behaviour the generic code already provides are expected green on the stubs: add, edit, list, scoping, accept paths, NUL and surrogate handling, JSON type errors)

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation (backend)

- [x] 3.1 `NumericPayload` and `NumericQuestion` with the Bean Validation of design D3 (`@NotEmpty @Size(max = 25)` and `List<@Valid NumericQuestion>`; `@NotBlank @Size(max = 500)` on `text`; `@NotBlank @Pattern` on `answer` and `tolerance`; no Bean Validation on `unit`). `extraChecks` stays the empty placeholder until 3.2, so in this step R1-S1 to R1-S5, R2-S1, R2-S2 (a), (b), (e), R2-S4, R2-S6 to R2-S12 and the Bean Validation cases of R2-S13 pass, and the scenarios that need `extraChecks` (the `null` question in R2-S3, the `unit` cases of R2-S2 (c), (d) and R2-S5, the `extraChecks` cases of R2-S13) still fail at their THEN step; verify exactly that
- [x] 3.2 `extraChecks`: a `null` question (`REQUIRED`); a present `unit`, blank first (`REQUIRED` at `.unit`) then over 20 (`TOO_LONG`); verify R2-S2 (c), (d), R2-S3, R2-S5 and R2-S13 pass
- [x] 3.3 `NumericItemType.studentView` as an allowlist (design D5): the `{"questions"}` envelope, `unit` only when present, stored order, throws on corrupt data; verify R1-S6 to R1-S9 pass
- [x] 3.4 Verify add, edit, list and scoping work with no other change (R1-S1 to R1-S5); check that the payload records have `@Size` on the list and on each text field, no `Map` field, that no shared class changed, and that no question, number or unit is logged (constitution section 5)
- [x] 3.5 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.6 Code-gate fixes: R1-S1 builds its course explicitly, R1-S5 checks the edit's 404 body for teacher A's text, R2-S8 comment on the echo check
- [x] 3.7 Code-owner review: Javadoc on null elements, R1-S9 message checks, import

## Workflow follow-up

- Code review gate: the `code-gate-reviewer` runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, write the Purpose line of the new `item-numeric` main spec.
- Mark the PR ready for the final review by a code owner other than the engineer.
