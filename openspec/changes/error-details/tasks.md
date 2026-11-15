# Tasks

## 1. Failing tests

- [ ] 1.1 Before writing tests, spike (code not committed): check whether a NUL character in a request field reaches `handleHttpMessageNotReadable` inside a `DatabindException` whose path names the field (design D4). Record the result in design D4. It decides how task 3.3 builds the NUL path
- [ ] 1.2 Write tests for api-errors R1-S1 to R1-S8 in `com.teachdash.common.ApiErrorsTest`, extending `ApiTest`. Put the scenario ID in `@DisplayName` and use `// given`, `// when`, `// then`. Create accounts, sessions and join links directly with the `ApiTest` helpers. Assert `errors` exactly as each scenario states, and that the body does not contain the values named in R1-S3, R1-S6 and R1-S7 (design D5). Send `null` and missing fields as raw JSON so the difference reaches the server
- [ ] 1.3 Write tests for api-errors R2-S1 to R2-S5 in `com.teachdash.common.FieldRejectionErrorsTest`, in the same way
- [ ] 1.4 Add the test-only `QueryParamTestController` under test sources (design D5) and write the test for R1-S9 against it in `ApiErrorsTest`
- [ ] 1.5 Write tests for api-errors R3-S1 and R3-S2 in `com.teachdash.common.MalformedIdTest`
- [ ] 1.6 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`. Verify every new test fails at its THEN step (not in setup), every other test passes, and the coverage script passes. R1-S8, R2-S5 and R3-S2 are guard tests that already pass (proposal, answer 11); report any other new test that passes to the engineer

## 2. Test review

- [ ] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [ ] 3.1 Add `FieldErrorDetail`, `FieldErrorCodes` (mapping and per-field priority) and the `errors` property in `ApiExceptionHandler.handleMethodArgumentNotValid`, sorted by field, then code. Throw `IllegalStateException` for a global (class-level) error (design D1, D2, D3, Risks)
- [ ] 3.2 Remove `@ReportAsSingleViolation` from `@PersonName` and `@EmailAddress`. Rebuild `@BcryptPassword` as `@NotBlank` + `@Size(min = 8)` + new `@MaxUtf8Bytes(72)`, move `exceedsBcryptLimit` to the new validator, update its two callers in `AccountService`, and delete `BcryptPasswordValidator` (design D2). Verify R1-S1 to R1-S4 pass and the accounts R2, R9, R10, R11 and student-enrolment tests still pass
- [ ] 3.3 Build the Jackson path in `handleHttpMessageNotReadable`: `INVALID_FORMAT` for NUL, `INVALID_TYPE` for a mismatch, `MALFORMED_REQUEST` with no `errors` when there is no path. If task 1.1 showed that the NUL error has no path, throw it from the deserializer through the `DeserializationContext` (design D4). Verify R1-S6 to R1-S8 and accounts R2-S6 pass
- [ ] 3.4 Add the optional field list to `ApiException` with the factories `validation(...)` and `field(...)`. Set `errors` in `handle(ApiException)`. Make `JoinLinkController` throw `ApiException.validation(violations)`, validating the token and the visitor together when there is no student session (design D3). Verify R1-S5 and the student-enrolment R9 tests pass
- [ ] 3.5 Use `ApiException.field` at the four `EMAIL_TAKEN` sites (including the duplicate-key path) and the `WRONG_PASSWORD` site (design D3). Verify R2-S1 to R2-S5 pass
- [ ] 3.6 Override `handleTypeMismatch`, `handleMissingServletRequestParameter` and `handleHandlerMethodValidationException` in `ApiExceptionHandler`, and delete `MalformedIdAdvice` (design D6). Verify R1-S9, R3-S1, R3-S2 and the student-enrolment malformed-id tests (R2-S6, R3-S5, R4-S6, R12) pass
- [ ] 3.7 Check that no rejected value reaches the logs or the response body on any path you changed (constitution §5)
- [ ] 3.8 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`. Verify the full suite is green and the approved tests are unchanged

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR.
- Archive in the same PR, after confirming every scenario ID has a matching test.
- Mark the PR ready for the final review by a code owner other than the engineer.
