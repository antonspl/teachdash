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

- Method-parameter validation (`HandlerMethodValidationException`), type mismatch on path
  or query parameters, missing query parameters. No endpoint uses them today
  (proposal, answer 9). Their hooks stay as they are.
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
validator; delete `BcryptPasswordValidator`. `TimeZoneIdValidator` and `@MaxUtf8Bytes`
treat `null` and blank as valid, leaving them to `@NotBlank`, so a blank value gives
only `REQUIRED`.

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
  dropping the violations. A second factory `ApiException.field(status, code, field)`
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
- otherwise (a `MismatchedInputException` or `InvalidFormatException`) gives `INVALID_TYPE`;
- no such exception, or an empty path (broken JSON, missing body, a top-level array)
  stays `MALFORMED_REQUEST` with no `errors`.

Jackson is expected to wrap the deserializer's `RuntimeException` in a
`DatabindException` with the path. This is not verified. Task 1.1 checks it. If it is
not wrapped, the deserializer throws a `MismatchedInputException` through the
`DeserializationContext` instead, keeping `NulCharacterException` as its cause so the two
cases stay apart. Every request body is a JSON object, so a NUL always has a path.

### D5. Tests

Tests live in a new `com.teachdash.common.ApiErrorsTest` (R1) and
`com.teachdash.common.FieldRejectionErrorsTest` (R2). They use the existing helpers in
`com.teachdash.support` to create accounts, sessions and join links directly. They assert
`errors` exactly, with RestAssured `body("errors", equalTo(List.of(Map.of(...))))`.
R1-S8 and R2-S5 assert that `errors` is absent (`body("$", not(hasKey("errors")))`).

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
- [A future query parameter or constrained path parameter falls through to Spring's
  default handling] → Follow-up issue (proposal, Out of scope).
- [Jackson stops at the first mapping error] → A body with several wrong types reports
  only the first. That is acceptable: the client sends JSON of the wrong shape, which is a
  bug in the client, not in what the user typed.
- [The NUL deserializer path is unverified] → Task 1.1 (D4).

## Migration Plan

Deploy as usual. The frontend ignores `errors`, and no client relies on
`MALFORMED_REQUEST` for a wrong type. Rollback is a redeploy of the previous image.
