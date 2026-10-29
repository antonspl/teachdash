# Design: accounts

## Context

Empty repository except `openspec/`. See proposal.md for scope. The constitution
fixes the stack (Spring Boot, Postgres + Flyway, one domain with `/api/*` proxied,
no CORS) and the test approach. This change is backend only.

## Goals / Non-Goals

**Goals:**
- A backend skeleton that later changes extend without restructuring.
- Session-based auth that survives backend restarts and deploys.

**Non-Goals:**
- Frontend (`cabinets-ui`), enrolment (`student-enrolment`).
- CI, production deployment, production secrets, observability shipping (only JSON
  logs + Actuator health now).
- Any outgoing email.

## Decisions

### D1. Versions and build
Java 25 (LTS), latest stable Spring Boot, Gradle with Kotlin DSL. The implementer pins
exact versions at setup.

### D2. Backend layout — package by feature
`com.teachdash.accounts`, `com.teachdash.common` (security, errors). Later features
add their own packages. Alternative (layered `controller/service/repository` at top
level) mixes features and grows badly.

### D3. Persistence — Spring Data JDBC
Simple aggregates, explicit SQL in Flyway, no lazy-loading surprises.
Alternative: JPA/Hibernate — more magic than this project needs.
Dependency: `spring-boot-starter-data-jdbc`.

### D4. Authentication — server-side session + cookie
Spring Security with a JSON login endpoint, `HttpSession` stored in Postgres via
**Spring Session JDBC** (dependency: `spring-session-jdbc`) so sessions survive
restarts and can be ended per user (deactivation, password reset).
Cookie: `HttpOnly`, `SameSite=Lax`, `Secure` in prod. CSRF protection with a
cookie-held token (`XSRF-TOKEN` → `X-XSRF-TOKEN` header), which the future SPA sends.
Alternative: JWT — no clean way to revoke sessions, and no benefit on one domain.
Passwords: Spring Security `DelegatingPasswordEncoder` (bcrypt).

### D5. One-time links
Token = 32 random bytes, base64url. Only the SHA-256 hash is stored, with purpose
(`SET_PASSWORD`, `RESET_PASSWORD`), account, expiry (7 days), `used_at`. Issuing a
new token marks older unused tokens of that account as used. The API returns the
full URL built from `APP_BASE_URL`: `{base}/set-password?token=…`. The token service
is reusable: `student-enrolment` will issue links for students through it.

### D6. Time source
A `Clock` bean, so tests can move time (R7-S3 expiry) without sleeping.

### D7. Errors
RFC 7807 `ProblemDetail` for all errors, with a machine-readable `code` property
(e.g. `WRONG_PASSWORD`) that the UI maps to a translated message.

### D8. API (all under `/api`)

| Method | Path | Role | Spec |
|---|---|---|---|
| POST | `/auth/login` | — | R2 |
| POST | `/auth/logout` | any | R3 |
| GET | `/me` | any | R2, R9 |
| PATCH | `/me` | any | R9 |
| POST | `/me/password` | any | R9 |
| POST | `/password-links/{token}` | — | R7 |
| GET/POST | `/admin/teachers` | admin | R4, R5 |
| POST | `/admin/teachers/{id}/deactivate`, `/reactivate` | admin | R6 |
| POST | `/admin/users/{id}/reset-link` | admin | R8 |

### D9. Data model and Flyway migrations
- `V1__accounts.sql` — `users` (id uuid, email, name, role, password_hash nullable,
  time_zone, active, created_at; unique index on `lower(email)`),
  `one_time_tokens` (token_hash, user_id, purpose, expires_at, used_at, created_at).
- `V2__spring_session.sql` — Spring Session JDBC tables (copied from its schema).

### D10. Admin bootstrap
An `ApplicationRunner` reads `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` and creates
the admin only if no admin exists (R1).

### D11. Tests and local run
- A base class `ApiTest` starts the app on a random port with a Testcontainers
  Postgres (reused across tests), cleans tables between tests, and offers helpers to
  create users of any role directly and log in (RestAssured with cookie + CSRF).
  Student accounts in tests are created by the helper, since enrolment does not
  exist yet.
- `scripts/check-scenario-coverage.sh`: collects scenario IDs from
  `openspec/specs/**/spec.md` and `openspec/changes/*/specs/**/spec.md` (excluding
  `archive/`) and fails if any ID has no `@DisplayName("[<id>]` in `backend/src/test`.
- Local: `backend/Dockerfile` and `infra/docker-compose.yml` running Postgres and the
  backend (`docker compose up` from a fresh clone). `.env.example` lists every
  variable with dev defaults; `.env` is gitignored. CI comes in a separate task and
  reuses the same test command and coverage script.

### D12. Logging
Spring Boot structured logging (`logging.structured.format.console`) to stdout. No
passwords, tokens or token hashes in logs. Actuator `health` exposed only.

## Risks / Trade-offs

- [Login errors reveal whether an email is registered (owner's choice for clearer
  UX); together with no rate limiting this helps targeted password guessing] →
  add rate limiting before real users join (backlog task).
- [Without email, a user who loses their password depends on the admin (later also
  their teacher)] → reset links can be issued at any time.
- [Backend without UI is hard to try by hand] → `cabinets-ui` follows; until then,
  tests are the check.
