# Design: error-details

## Context

See proposal.md for motivation and the engineer's answers. Current state:

- `common/ApiExceptionHandler` extends `ResponseEntityExceptionHandler`. It renders every
  error as a `ProblemDetail` with a `code` property. `handleMethodArgumentNotValid`
  returns `VALIDATION_FAILED` without details. `handleHttpMessageNotReadable` returns
  `VALIDATION_FAILED` when a `NulCharacterException` is in the cause chain, and
  `MALFORMED_REQUEST` otherwise.
- `common/NulRejectingJsonConfig` replaces Jackson's `String` deserializer. On a NUL
  character it throws a bare `NulCharacterException` (a `RuntimeException`).
- `common/validation`: `@PersonName` (NotBlank + Size(max 200) + Pattern), `@EmailAddress`
  (NotBlank + Email + Size(max 320) + Pattern) and `@TimeZoneId` (NotBlank + custom
  validator). The first two carry `@ReportAsSingleViolation`. `@BcryptPassword` has one
  validator for three reasons (blank, under 8 characters, over 72 UTF-8 bytes).
- `enrolment/api/JoinLinkController` validates `JoinVisitor` by hand and throws
  `ApiException(400, VALIDATION_FAILED)`, dropping the violations. Validation there is
  conditional (a visitor's fields are checked; a student session's are ignored).
- `ApiException` holds only a status and a code.
- Spring Boot 4.1, Jackson 3 (`tools.jackson`). Request DTOs are records with no
  `@JsonProperty` renames, so JSON names equal Java component names.

## Goals / Non-Goals

**Goals:**

- One mapping from a constraint violation to a field code, used by every source of
  validation errors.
- The same `errors` shape on every response that has one.

**Non-Goals:**

- Constraints on path parameters: none exist. A constrained path parameter would reach
  the same hook as a query parameter (D6) and be named like one.
- Class-level (cross-field) constraints: none exist. See Risks.

## Decisions

### D1. Response shape

`errors` is a `ProblemDetail` property, a JSON array of objects `{"field": "...",
"code": "..."}`. Its Java type is a record `FieldErrorDetail(String field, String code)`
in `common`. The handler sorts the list by `field`, then by `code`, so responses and
tests are deterministic. The property is set only when the list is not empty (spec R1,
R2). Nothing else changes in the problem body.

Alternative considered: the RFC 9457 example items `{pointer, detail}`. Rejected by the
engineer: dotted paths and codes, no messages (answers 2 and 5).

### D2. Constraints report each rule; the handler picks one code per field

Remove `@ReportAsSingleViolation` from `@PersonName` and `@EmailAddress`, so each
composing constraint reports its own violation. Rebuild `@BcryptPassword` as a
composite: `@NotBlank`, `@Size(min = 8)` and a new `@MaxUtf8Bytes(72)` with its own
validator, which treats `null` as valid. `BcryptPasswordValidator` goes away; its static
`exceedsBcryptLimit`, used by `AccountService` for the rule that an over-long password is
a wrong password (accounts R2, R9), moves to the `MaxUtf8Bytes` validator as a static
helper. `@TimeZoneId` already reports `NotBlank` and its own violation separately and
stays as it is: for a blank value the priority below keeps only `REQUIRED`.

A package-private `FieldErrorCodes` in `common` maps one `ConstraintViolation` to a code,
by the constraint's annotation type:

| Constraint | Code |
|---|---|
| `NotBlank`, `NotNull`, `NotEmpty` | `REQUIRED` |
| `Size` | `TOO_SHORT` if the value is shorter than `min`, otherwise `TOO_LONG` |
| `MaxUtf8Bytes` | `TOO_LONG` |
| `Email`, `Pattern`, `TimeZoneId` and any other | `INVALID_FORMAT` |

When several violations hit one field, the code with the highest priority wins:
`REQUIRED` > `TOO_SHORT` = `TOO_LONG` > `INVALID_FORMAT`. `Size` cannot be too short and
too long at once, so the equal priority never conflicts.

The field is the violation's property path (Spring's `FieldError.getField()` for `@Valid`
bodies, `ConstraintViolation.getPropertyPath()` for manual validation). Both use the
dotted form with `[i]` indices.

Alternative considered: keep the composites and put the code into each constraint's
`message`. Rejected: one composite with one message cannot say which of its rules
failed, and messages would become a hidden contract.

### D3. Where the list is built

- `handleMethodArgumentNotValid`: unwrap each `FieldError` to its `ConstraintViolation`,
  map it with `FieldErrorCodes`, reduce to one entry per field (D2), sort, and set
  `errors`. A `FieldError` that is not backed by a violation (none exist today) maps to
  `INVALID_FORMAT`.
- `ApiException` gets an optional list of `FieldErrorDetail`. A static factory
  `ApiException.validation(Set<ConstraintViolation<?>>)` builds a 400
  `VALIDATION_FAILED` with the mapped list. `JoinLinkController` throws it instead of
  dropping the violations. Without a student session, `join` validates the token and the
  `JoinVisitor` together and reports both sets of violations in one response (spec
  R1-S5); `JoinVisitor` uses the request's JSON names, so the paths match. With a student
  session only the token is checked, as today. The order of checks in
  student-enrolment R9 does not change: all invalid input is still one 400. A second factory `ApiException.field(status, code, field)`
  builds a single-field error with one entry whose code is the response code (spec R2).
  `handle(ApiException)` sets `errors` when the list is not empty.
- `EMAIL_TAKEN` (`AccountService` two sites, `TeacherAdminService`,
  `StudentRegistrationService`) and `WRONG_PASSWORD` (`AccountService`) use
  `ApiException.field`. This includes the `DuplicateKeyException` path of parallel
  joins (student-enrolment R9-S10).

### D4. Jackson errors: NUL and wrong types

In `handleHttpMessageNotReadable`, walk the cause chain to the first
`tools.jackson.databind.DatabindException` whose path is not empty. Build the field from
its references: a property name is joined with `.`, an index as `[i]`. Then:

- a `NulCharacterException` in the chain gives `INVALID_FORMAT`;
- a `MismatchedInputException` (which includes `InvalidFormatException`) gives
  `INVALID_TYPE`, except `UnrecognizedPropertyException`, which Boot does not raise by
  default;
- anything else (broken JSON, a missing body, a top-level array, a
  `ValueInstantiationException` from a record constructor), or an empty path, stays
  `MALFORMED_REQUEST` with no `errors`.

Jackson coerces JSON numbers and booleans to `String` by default, so in practice
`INVALID_TYPE` means an array or an object where a scalar is expected. This change keeps
that coercion.

Jackson is expected to wrap the deserializer's `RuntimeException` in a
`DatabindException` with the path. This is not verified. Task 1.1 checks it before any
test is written and records the result here. Rule: if the handler sees a
`DatabindException` whose path names the field and whose cause chain holds the
`NulCharacterException`, keep the deserializer as it is. If it is not wrapped, the deserializer throws a `MismatchedInputException` through the
`DeserializationContext` instead, keeping `NulCharacterException` as its cause so the two
cases stay apart. Every request body is a JSON object, so a NUL always has a path.

### D5. Tests

Tests live in a new `com.teachdash.common.ApiErrorsTest` (R1) and
`com.teachdash.common.FieldRejectionErrorsTest` (R2). They use the existing helpers in
`com.teachdash.support` to create accounts, sessions and join links directly. They assert
`errors` exactly, with RestAssured `body("errors", equalTo(List.of(Map.of(...))))`.
R1-S8 and R2-S5 assert that `errors` is absent (`body("$", not(hasKey("errors")))`).
R3 goes into `com.teachdash.common.MalformedIdTest`.

R1-S9 needs an endpoint with query parameters, and none exists. A
`@RestController` in test sources (`com.teachdash.common.QueryParamTestController`)
maps `GET /api/teacher/test-query` with `@RequestParam int page` and
`@RequestParam(required = false) @Size(max = 10) String q`, and returns 200. It sits
under the teacher path so the existing security rules apply to it without a test-only
security change. It is picked up only by test component scanning and never ships.

### D6. Path and query parameters in one handler

`enrolment/api/MalformedIdAdvice` covers only the enrolment controllers, so the admin
endpoints in `accounts` fall through to the generic 400 `BAD_REQUEST`. Delete it and
override in `ApiExceptionHandler`:

- `handleTypeMismatch`: for a `MethodArgumentTypeMismatchException` on a
  `@PathVariable`, 404 `NOT_FOUND` with no `errors` (spec R3). Otherwise 400
  `VALIDATION_FAILED` with `[{field: <parameter name>, code: "INVALID_TYPE"}]`.
- `handleMissingServletRequestParameter`: 400 `VALIDATION_FAILED` with
  `[{field: <parameter name>, code: "REQUIRED"}]`.
- `handleHandlerMethodValidationException`: for each parameter result, map its
  violations with `FieldErrorCodes` (D2), using the parameter name as field. A
  `@Valid` bean parameter in the same result contributes its field errors as in D3.

Parameter names come from the `-parameters` compiler flag, which the Spring Boot Gradle
plugin sets. R1-S9 fails with `arg0` if it is missing.

Alternative considered: make `MalformedIdAdvice` global. Rejected: two advices for the
same exception with an order between them is harder to follow than one handler.

## Flyway migrations

None.

## New dependencies

None.

## Risks / Trade-offs

- [Removing `@ReportAsSingleViolation` returns several violations per field] → D2
  reduces them to one entry per field. R1-S1, S2 and S4 cover fields that break more than
  one rule.
- [A future `@JsonProperty` rename would make Jackson paths (JSON names) differ from
  validation paths (Java names)] → The reviewer checks that request DTOs do not rename
  properties. A rename needs its own change to this design.
- [A future class-level constraint has no field] → It would produce a
  `VALIDATION_FAILED` with no entry, breaking R1. `handleMethodArgumentNotValid` throws
  an `IllegalStateException` for a global error, so the first such constraint fails its
  own tests loudly and forces a decision.
- [The query-parameter rule is tested only through a test-only controller] → Accepted
  (proposal, answer 13). The first real endpoint with a query parameter adds its own
  scenario.
- [Jackson stops at the first mapping error] → A body with several wrong types reports
  only the first. That is acceptable: the client sends JSON of the wrong shape, which is a
  bug in the client, not in what the user typed.
- [The NUL deserializer path is unverified] → Task 1.1 (D4).
- [String sort puts `items[10]` before `items[2]`] → Accepted: no request has lists
  today, and the order is still stable. The `[i]` path format is not covered by a
  scenario for the same reason; the reviewer checks the path builder.
- [R1-S8, R2-S5 and R3-S2 pass before any code changes] → They guard behaviour this
  change could break (the handler for unreadable JSON, `ApiException`, and the
  malformed-id rule moving out of `MalformedIdAdvice`). The engineer agreed to keep
  them as guards (proposal, answer 11).

## Migration Plan

Deploy as usual. The frontend ignores `errors`, and no client relies on
`MALFORMED_REQUEST` for a wrong type. Rollback is a redeploy of the previous image.
