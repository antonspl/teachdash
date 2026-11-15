# Proposal: error-details

## Clarifications

Questions asked on 2026-11-15, and the engineer's answers. Scope follows issue #100.

| # | Question | Answer |
|---|----------|--------|
| 1 | Keep the top-level `code` (e.g. `VALIDATION_FAILED`) and add a list `errors: [{field, code}]` to the problem response? | Yes. |
| 2 | Field path format: dotted (`lessons[0].title`) or JSON Pointer (`/lessons/0/title`)? | Dotted. |
| 3 | Our own small code vocabulary or the raw constraint names (`NotBlank`, `Size`)? Include limits such as `max: 200`? | Our own vocabulary, no limits. |
| 4 | Which failures are covered: body validation, NUL characters, wrong JSON types, path and query parameters, single-field checks in services? | All of them (narrowed by answer 9). |
| 5 | Human-readable messages in the response? | No. The frontend translates codes through i18n. |
| 6 | Name, email and password checks are combined constraints that report one failure, so an empty value cannot be told from a too-long one. When one field breaks several rules, what is returned? | One entry per field, with the first rule that fails in this order: `REQUIRED`, then `TOO_SHORT` or `TOO_LONG`, then `INVALID_FORMAT`. |
| 7 | The password check has three reasons (blank, under 8 characters, over 72 bytes). Codes? | `REQUIRED`, `TOO_SHORT`, `TOO_LONG`. |
| 8 | `EMAIL_TAKEN` (409) and `WRONG_PASSWORD` (400) are about one field. Do they carry `errors`? | Yes, with the same code: `[{field: "email", code: "EMAIL_TAKEN"}]` and `[{field: "currentPassword", code: "WRONG_PASSWORD"}]`. Status and top-level code stay. |
| 9 | A wrong JSON type for a field returns `MALFORMED_REQUEST` today. And path and query parameters? | A wrong type becomes 400 `VALIDATION_FAILED` with code `INVALID_TYPE` for the field. `MALFORMED_REQUEST` stays for broken JSON and a missing body. A malformed path id stays 404 `NOT_FOUND` (student-enrolment R2-S6, R3-S5, R4-S6). Query parameters: see answer 13. |
| 10 | When is `errors` present, and in what order? | On every `VALIDATION_FAILED` response, always with at least one entry, and on `EMAIL_TAKEN` and `WRONG_PASSWORD`. No other response has it. Entries are sorted by field, then by code. |
| 11 | Spec critique: R1-S8 and R2-S5 pass before any code changes, against constitution §1 step 7. Keep them? | Keep them as guard tests: they protect behaviour this change touches. |
| 12 | Research found that a malformed id on the admin endpoints (`accounts`) probably returns 400 `BAD_REQUEST`, because the 404 rule covers only the enrolment controllers. Follow-up issue or fix here? Where is it specified? | Fix here, as a new requirement `api-errors` R3 (one rule for every endpoint). This extends the R range reserved in #100 from R1-R2 to R1-R3. |
| 13 | No endpoint has query parameters. Cover them here? | Yes: query parameters are named in `errors` like body fields (R1-S9), tested through a controller that exists only in test sources. |

Assumptions (the engineer may override during review):

- A NUL character in a string gets code `INVALID_FORMAT` for that field.
- The issue's teacher-scoping decision does not apply: the change touches no
  teacher-owned data. Content in both languages applies only as Cyrillic and Latin input
  values in the scenarios.

## Research

Two angles, agreed with the engineer on 2026-11-15.

- **What the backend validates today.** Seven endpoints use `@Valid` request bodies; the
  join-link endpoints validate by hand and throw away which field failed. Name, email and
  time zone are composite constraints reporting one violation, and the password validator
  has three reasons in one check (answers 6 and 7). `EMAIL_TAKEN` is thrown in four
  places, `WRONG_PASSWORD` in one. No endpoint has query parameters; malformed path ids
  are pinned to 404 by existing scenarios (answer 9). Existing tests assert only `code`,
  and the frontend reads only `code`, so adding `errors` breaks neither.
- **Spring, Jackson and RFC 9457.** Spring Boot 4.1 with Jackson 3. Field paths come from
  the binding result for body validation and from the Jackson path of a mapping error for
  wrong types; malformed JSON has no path. RFC 9457 allows extension members (clients
  ignore unknown ones) and its own validation example uses an `errors` array, so the
  shape is compliant though its items differ from the RFC example. The RFC says nothing
  about order, so we sort (answer 10). Whether the NUL error keeps its Jackson path was
  not verified; the first test task checks it (design D4).

## Why

A validation error says only `VALIDATION_FAILED`, so a form cannot tell the user which
field is wrong or why. Forms for courses, lessons and content are coming next, and each
would otherwise guess. A field path and a machine-readable code per failing field let
the frontend show a translated message next to the field.

## What Changes

- Every 400 `VALIDATION_FAILED` response carries `errors`: one `{field, code}` entry per
  failing field, sorted, never echoing the rejected value. Codes: `REQUIRED`,
  `TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_TYPE`.
- The join-link endpoints report the failing fields too.
- **BREAKING** (API): a wrong JSON type for a field (e.g. an array where a string is
  expected) returns 400 `VALIDATION_FAILED` instead of 400 `MALFORMED_REQUEST`. No
  client or spec relies on the old code.
- `EMAIL_TAKEN` and `WRONG_PASSWORD` responses also carry `errors` naming the field.
- A query parameter that is missing, of the wrong type or breaks a constraint gives 400
  `VALIDATION_FAILED` naming the parameter. No endpoint has one yet.
- **BREAKING** (API): a malformed path id returns 404 `NOT_FOUND` on every endpoint. The
  admin endpoints for teachers and reset links returned 400 `BAD_REQUEST`; the enrolment
  endpoints already returned 404.
- Internal: the composite name, email, time-zone and password constraints report each
  rule separately, so a code can be chosen.

## Capabilities

### New Capabilities

- `api-errors`: the shape of field-level error details in API problem responses (R1,
  R2), and the response to a malformed path id (R3).

### Modified Capabilities

None. The status codes and top-level codes in `accounts` and `student-enrolment` do not
change; the new requirements apply on top of them.

## Out of scope

- Frontend: showing errors next to fields (separate FE issue). The current frontend
  ignores `errors`.
- Constraints on path parameters other than the id type: none exist.
- Limits or messages in the entries (answers 3 and 5).
- Errors that are not about one field (`INVALID_CREDENTIALS`, `ALREADY_ENROLLED`,
  `STUDENT_EXISTS`, `LINK_INVALID` and others): no `errors`.

## Impact

- `ApiExceptionHandler`, `ApiException` (optional field), the join-link controller, the
  NUL-rejecting string deserializer.
- Composite constraints `@PersonName`, `@EmailAddress`, `@TimeZoneId`, `@BcryptPassword`
  (design D2).
- The four `EMAIL_TAKEN` sites and the `WRONG_PASSWORD` site.
- `enrolment/api/MalformedIdAdvice` is replaced by one rule in `ApiExceptionHandler`
  (design D6).
- A test-only controller with query parameters (design D5).
- No migrations, no new dependencies, no frontend change.
