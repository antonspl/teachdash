# Tasks

## 1. Failing tests

- [x] 1.0 Merge the `workflow` and `backend-skeleton` branches (later `main`) into `accounts`, so this PR's diff shows only the accounts change
- [x] 1.1 Add the accounts dependencies (Security, Data JDBC, Spring Session JDBC, Validation) to `build.gradle.kts`
- [x] 1.2 Test helpers per design D11: users created directly, `loggedInAs()` via the session store, browser-like `TestClient` cookies, `login()` without extra GETs, zone-aware `MutableClock`; verify the smoke test passes
- [x] 1.3 Stubs per design D2 layout: request/response records and controllers for every endpoint in D8 returning 501, each marked `STUB`; verify compilation
- [x] 1.4 Flyway `V1__accounts.sql`, `V2__spring_session.sql` per design D9 (Spring Session schema unchanged; principal name = user id, D4); verify the app starts on an empty database
- [x] 1.5 One test per scenario in `specs/accounts/spec.md` (30 scenarios), scenario ID in `@DisplayName`, `// given / when / then`; R2-S2 and R1 as in D11; verify they compile
- [x] 1.6 Run `scripts/check-scenario-coverage.sh` and `./gradlew test`; verify every scenario test fails at its THEN step (501 or missing behaviour), none in setup; list any exceptions
- [x] 1.7 PR description lists pass 1 production code (stubs: temporary; migrations: permanent, review as production code) and the commit range for the tests review
- [x] 1.8 Fixes from the owner's tests review on PR #17 (design D11): R4-S4 CSRF test, `APP_BASE_URL` in test setup, R1-S2 checks the whole admin row, `loggedInAs` uses the stored email, drop `ExcludeTestClasses`, R1 without a second app per test, RestAssured `CookieFilter`, low-cost bcrypt for test users; verify every scenario test fails at its THEN
- [x] 1.9 Fixes from the owner's second tests review on PR #17: principal name = user id (D4, D9), R5-S3 end-to-end link test, R8-S2 link session survives, R1-S2 same email as `ADMIN_EMAIL`, base URL checks in R5-S1/R8-S1, R7-S3 password unchanged, `TestClient` follows a rotated CSRF cookie, stub `SecurityConfig` logs no generated password, `BootstrapTest` in `accounts.domain`, `createToken` per D5; verify every scenario test fails at its THEN
- [x] 1.10 Fixes from the owner's third tests review on PR #17: R4-S5 admin rule on data-changing admin endpoints, token in the JSON body and the link fragment (D5, D8), partial unique index on unused tokens (D9), R1-S3, R4-S4 also sends the request with the token, remove `ClockConfig` from pass 1 (D6), test-code cleanups; verify every scenario test fails at its THEN
- [x] 1.11 Fixes from the owner's fourth tests review on PR #17: R6-S3 only teachers can be deactivated (404), R6-S1 checks `ACCOUNT_DEACTIVATED`; verify every scenario test fails at its THEN

## 2. Test review

- [x] 2.1 Reviewer approves tests in the PR — HARD STOP, no production code before approval

## 3. Backend implementation

- [ ] 3.2 Security config: delete the stub `SecurityConfig` (`permitAll`, CSRF off, empty user store) and write the real one: JSON login/logout, JDBC sessions, CSRF cookie, admin role rule, 401 with reason codes; verify R2–R4 tests pass (R4-S4: CSRF enabled)
- [ ] 3.3 Admin bootstrap runner (replaces the `AdminBootstrap` stub) and production `ClockConfig` (D6); verify R1 tests pass
- [ ] 3.4 One-time links (issue, hash, expire, invalidate older, use) and profile endpoints; verify R7, R9 tests pass
- [ ] 3.5 Admin teacher management and admin reset links, ending sessions on deactivation/reset; verify R5, R6, R8 tests pass
- [ ] 3.6 Structured JSON logging and Actuator health; verify logs are JSON with no tokens or passwords, and full `./gradlew test` is green

## 4. Local run

- [ ] 4.1 `backend/Dockerfile`, `infra/docker-compose.yml` (Postgres + backend), `.env.example` with dev defaults for every variable, README "Run locally" section; verify `docker compose up` from a fresh clone starts the backend, the admin can log in, and `/actuator/health` is UP

## 5. Review

- [ ] 5.1 Reviewer agent reviews the full diff against specs and constitution; confirmed findings fixed; verify full test suite is green

## Workflow follow-up

- Code review gate: a reviewer approves the code in the PR.
- Archive the change in the same PR; verify every scenario ID has a matching test before archiving.
- Mark the PR ready; the owner reviews everything and merges.
