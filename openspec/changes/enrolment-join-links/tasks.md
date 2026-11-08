# Tasks

## 1. Failing tests

- [x] 1.1 Add migration `V5__teacher_join_links.sql` (design D1). It is a schema migration, which pass 1 may add; the engineer reviews it as production code at the test gate. Verify the app starts and the existing suite still passes
- [x] 1.2 Add stubs returning 501, each marked `STUB`, for the endpoints of design D2: `GET`, `POST`, `DELETE /api/teacher/join-link`, `POST /api/join-links/preview` and `POST /api/join-links/join`, with the public two permitted without a session in `SecurityConfig` (CSRF still on). Add the rate-limit properties of design D5 to configuration with their defaults, very high limits in the shared test configuration, and no filter
- [x] 1.3 Add a test helper in `com.teachdash.support` that creates a teacher's join link directly in the database, so test setup does not depend on the stubbed endpoints
- [x] 1.4 Write tests for student-enrolment R8-S1 to R8-S9, with the scenario ID in `@DisplayName` and `// given`, `// when`, `// then`
- [x] 1.5 Write tests for student-enrolment R9-S1 to R9-S12 and R10-S1 to R10-S6. R9-S9 injects the failure while the assignment is stored, as R1-S12 does, and R8-S9 as R7-S4 does. R9-S3 compares the raw bodies per endpoint
- [x] 1.6 Write tests for student-enrolment R11-S1 to R11-S3 in a separate test class with low limits set by properties (design D5), using `X-Forwarded-For` from the loopback peer for a second IP and `MutableClock` to pass the window. Their GIVEN calls the limited endpoints (design D5); each test uses its own `X-Forwarded-For` addresses, so counters from other tests never apply
- [x] 1.7 Run `./gradlew test` and `scripts/check-scenario-coverage.sh`; verify every new test fails at its THEN step (not in setup), every existing test passes, and the coverage script passes. Every scenario has a THEN step that needs the new behaviour; report any test that passes anyway to the engineer

## 2. Test review

- [x] 2.1 Engineer approves tests in the PR. HARD STOP: no production code before approval

## 3. Implementation

- [x] 3.1 Extract the token generator from `PasswordLinkService` into `common` (design D1), with no change in behaviour; verify the accounts tests pass
- [x] 3.2 Implement the teacher link endpoints with the teacher-row lock of design D3, and make teacher deactivation delete the link before releasing the students (design D4); verify all R8 tests pass
- [x] 3.3 Implement the preview and the visitor join with the check and lock order of design D3, hashing the password before the transaction and starting the session after it; add a comment explaining the teacher-row lock; verify R9 tests pass
- [x] 3.4 Implement the student join path (design D3, step 4) with the conflict mapping used by registration; verify R10 tests pass
- [x] 3.5 Implement the rate-limit filter of design D5, registered before the security filter chain, with the size cap and `Retry-After` clamp; verify R11 tests pass
- [x] 3.6 Check that no join token appears in logs on the view, preview, join, 410 and 429 paths (constitution §4); fix any that does
- [x] 3.7 Run `./gradlew cleanTest test` and `scripts/check-scenario-coverage.sh`; verify the full suite is green and the approved tests are unchanged
- [x] 3.8 Code review fix (Opus review): add a test for R11-S4 and confirm it fails on the current filter; the engineer approves this test in the PR. Then match the limiter on the decoded servlet path (design D5); verify all tests pass
- [x] 3.9 Code review fixes: no token in the link response's `toString`, no email or name in request records' `toString`, remove the unused import in `JoinLinkController`

## Workflow follow-up

- Code review gate: the Opus review runs, then the engineer approves the code in the PR.
- Open a follow-up issue for rate limiting login and set-password-by-link, and one to check the client IP seen by the backend after deploy (design, Risks).
- Archive in the same PR, after confirming every scenario ID has a matching test. At archive, add a sentence to the Purpose of `openspec/specs/student-enrolment/spec.md` about join links.
- Mark the PR ready for the final review by a code owner other than the engineer.
