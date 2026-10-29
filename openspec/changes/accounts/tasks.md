# Tasks

## 1. Failing tests

- [ ] 1.1 Create `backend/` skeleton (Gradle Kotlin DSL, Java 25, Spring Boot, Security, Data JDBC, Flyway, Spring Session JDBC, Actuator, Testcontainers, RestAssured) and verify `./gradlew build -x test` succeeds
- [ ] 1.2 Add the `ApiTest` base class (Testcontainers Postgres, random port, table cleanup, user/login helpers with CSRF) and a `Clock` bean that tests can move; verify an empty smoke test starts the context
- [ ] 1.3 Add stubs so tests compile: request/response records and controllers for every endpoint in design D8 returning 501; verify compilation
- [ ] 1.4 Write one test per scenario in `specs/accounts/spec.md` (25 scenarios) with the scenario ID in `@DisplayName` and `// given / when / then`; verify they compile
- [ ] 1.5 Add `scripts/check-scenario-coverage.sh` and verify it passes (every scenario ID has a test)
- [ ] 1.6 Run `./gradlew test` and verify every scenario test fails for the right reason (501 or missing behaviour, not setup errors); list any exceptions

## 2. Test review

- [ ] 2.1 Reviewer approves tests in the PR — HARD STOP, no production code before approval

## 3. Backend implementation

- [ ] 3.1 Flyway `V1__accounts.sql`, `V2__spring_session.sql`; verify the app starts on an empty database
- [ ] 3.2 Security config: JSON login/logout, JDBC sessions, CSRF cookie, admin role rule, 401 with reason codes; verify R2–R4 tests pass
- [ ] 3.3 Admin bootstrap runner; verify R1 tests pass
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
