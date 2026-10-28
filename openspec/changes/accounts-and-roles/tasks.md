# Tasks

## 1. Failing tests

- [ ] 1.1 Create `backend/` skeleton (Gradle Kotlin DSL, Java 25, Spring Boot, Security, Data JDBC, Flyway, Spring Session JDBC, Actuator, Testcontainers, RestAssured) and verify `./gradlew build -x test` succeeds
- [ ] 1.2 Add the `ApiTest` base class (Testcontainers Postgres, random port, table cleanup, user/login helpers with CSRF) and a `Clock` bean that tests can move; verify an empty smoke test starts the context
- [ ] 1.3 Add stubs so tests compile: request/response records and controllers for every endpoint in design D8 returning 501; verify compilation
- [ ] 1.4 Write one test per scenario in `specs/accounts/spec.md` (R1–R9) with the scenario ID in `@DisplayName` and `// given / when / then`; verify they compile
- [ ] 1.5 Write one test per scenario in `specs/student-enrolment/spec.md` (R1–R7) the same way; verify they compile
- [ ] 1.6 Add `scripts/check-scenario-coverage.sh` and verify it passes (every scenario ID has a test)
- [ ] 1.7 Run `./gradlew test` and verify every scenario test fails for the right reason (501 or missing behaviour, not setup errors); list any exceptions

## 2. Test review

- [ ] 2.1 Owner reviews and approves tests — HARD STOP, no production code before approval

## 3. Backend implementation

- [ ] 3.1 Flyway `V1__accounts.sql`, `V2__enrolment.sql`, `V3__spring_session.sql`; verify the app starts on an empty database
- [ ] 3.2 Security config: JSON login/logout, JDBC sessions, CSRF cookie, role rules, uniform 401 body; verify accounts R2–R4 tests pass
- [ ] 3.3 Admin bootstrap runner; verify accounts R1 tests pass
- [ ] 3.4 One-time links (issue, hash, expire, invalidate older, use) and profile endpoints; verify accounts R7, R9 tests pass
- [ ] 3.5 Admin teacher management and reset links (admin + teacher), ending sessions on deactivation/reset; verify accounts R5, R6, R8 tests pass
- [ ] 3.6 Enrolment: create student, join links, join, lists, removal, admin views; verify all student-enrolment tests pass
- [ ] 3.7 Structured JSON logging and Actuator health; verify logs are JSON and contain no tokens or passwords; verify full `./gradlew test` is green

## 4. Frontend

- [ ] 4.1 Create `frontend/` (Vite, React, TS, Mantine, React Router, TanStack Query, react-i18next with `en.json`, `/api` dev proxy, CSRF header); verify `npm run build` and `npm run lint` pass
- [ ] 4.2 Login, set-password and join pages; verify Vitest tests for form validation pass
- [ ] 4.3 Teacher cabinet (students list, add student with copyable link, join link manage, reset link, remove); verify Vitest tests pass
- [ ] 4.4 Student cabinet (teachers list, upcoming lessons placeholder), admin page, profile page; verify Vitest tests pass
- [ ] 4.5 Playwright E2E: admin creates teacher → teacher sets password → teacher shares join link → student joins → both see each other; verify it passes locally

## 5. Local dev and CI

- [ ] 5.1 `infra/docker-compose.dev.yml` (Postgres), `.env.example` with every variable, README "Run locally" section; verify a fresh clone runs by following the README
- [ ] 5.2 GitHub Actions workflow: backend tests, frontend lint/Vitest/build, Playwright, scenario coverage; verify it is green on a push

## 6. Review

- [ ] 6.1 Reviewer agent reviews the full diff against specs and constitution; confirmed findings fixed; verify full test suite is green

## Workflow follow-up

- Archive the change after the owner accepts it; verify every scenario ID has a matching test before archiving.
