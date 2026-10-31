# Design: accounts

## Context

Builds on `backend-skeleton` (Spring Boot app, Gradle, Testcontainers test base)
and `workflow` (coverage script). See proposal.md for scope. The constitution
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
Versions and the build come from `backend-skeleton` (Java 25, Spring Boot, Gradle
Kotlin DSL). This change adds Spring Security, Spring Data JDBC, Spring Session JDBC
and Bean Validation (reasons in D3, D4).

### D2. Backend layout — feature first, then role (constitution §3 "Code layout")
```
com/teachdash/
  accounts/
    api/            AdminController, AuthController, MeController, PasswordLinkController
      dto/          LoginRequest, CreateTeacherRequest, UserResponse, ...
    domain/         Role (pass 2: User, AccountService, PasswordLinkService)
    persistence/    (pass 2: UserRepository, OneTimeTokenRepository)
  common/           ClockConfig, SecurityConfig, error handling
```
Classes used across subpackages are `public`. Alternative (one flat `accounts`
package) mixes controllers, DTOs and domain and grows badly in pass 2.

### D3. Persistence — Spring Data JDBC
Simple aggregates, explicit SQL in Flyway, no lazy-loading surprises.
Alternative: JPA/Hibernate — more magic than this project needs.
Dependency: `spring-boot-starter-data-jdbc`.
Request validation (e.g. password rules, required fields) uses Bean Validation:
`spring-boot-starter-validation`, the standard Spring way, instead of hand-written checks.

### D4. Authentication — server-side session + cookie
Spring Security with a JSON login endpoint, `HttpSession` stored in Postgres via
**Spring Session JDBC** (dependency: `spring-session-jdbc`) so sessions survive
restarts and can be ended per user (deactivation, password reset).
Cookie: `HttpOnly`, `SameSite=Lax`, `Secure` in prod. CSRF protection with a
cookie-held token (`XSRF-TOKEN` → `X-XSRF-TOKEN` header), which the future SPA sends.
The token cookie is set eagerly on every response, also for anonymous requests
(a logged-out `GET /api/me` returns 401 and sets `XSRF-TOKEN`), and the raw cookie
value is accepted in the header (SPA-style handler), so a client can log in.
Alternative: JWT — no clean way to revoke sessions, and no benefit on one domain.
Passwords: Spring Security `DelegatingPasswordEncoder` (bcrypt).
Session contents (tests create sessions this way, D11): the `SecurityContext` under
`SPRING_SECURITY_CONTEXT` holds a standard Spring `User` principal, name = email,
authorities `ROLE_ADMIN` / `ROLE_TEACHER` / `ROLE_STUDENT`; no custom principal
class. The current user is looked up by `authentication.getName()`. Spring Session
defaults: `SESSION` cookie (base64 id), JDK-serialized attributes, principal-name
index = email.

### D5. One-time links
Token = 32 random bytes, base64url. Only the hash is stored —
`hex(SHA-256(UTF-8 bytes of the base64url string))`, 64 characters — with purpose
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

JSON shapes:
- User (`GET/PATCH /me`, teacher list items): `id`, `name`, `email`, `role`
  (`ADMIN` / `TEACHER` / `STUDENT`), `timeZone` (IANA id, e.g. `Europe/Moscow`).
- `POST /auth/login`: `{email, password}`. `PATCH /me`: `{name, timeZone}`.
  `POST /me/password`: `{currentPassword, newPassword}`.
- `POST /admin/teachers`: request `{name, email, timeZone}`, response 201
  `{id, setPasswordLink}`.
- `POST /admin/users/{id}/reset-link`: response `{link}`.
- `POST /password-links/{token}`: request `{password}`; 200 starts a session; a used,
  expired or replaced link returns 410.
- Errors: `ProblemDetail` with a top-level `code` property (D7).

### D9. Data model and Flyway migrations
- `V1__accounts.sql` — `users` (id uuid, email, name, role, password_hash nullable,
  time_zone, active, created_at; unique index on `lower(email)`),
  `one_time_tokens` (token_hash, user_id, purpose, expires_at, used_at, created_at).
- `V2__spring_session.sql` — Spring Session JDBC tables (copied from its schema),
  with `principal_name` widened to `varchar(320)` to match `users.email`; with the
  default `varchar(100)` a long email fails on session save, and R6/R8 cannot find
  that user's sessions.

Both are written in pass 1 (tests need the schema) and are reviewed as production
code at the tests gate. They are not applied anywhere yet, so they may still be
edited until this change merges.

### D10. Admin bootstrap
An `ApplicationRunner` reads the properties `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`
(by these exact names, e.g. `${ADMIN_EMAIL}`, not relaxed-bound `admin.*`) and creates
the admin only if no admin exists (R1).

### D11. Tests and local run
- `ApiTest` (from `backend-skeleton`: app on a random port, shared Testcontainers
  Postgres, tables cleaned between tests) gets accounts helpers:
  - create users of any role directly in the database (students too, since
    enrolment does not exist yet);
  - `loggedInAs(user)` creates the session **directly in the session store**, not
    through `/auth/login`, so tests that are not about login never depend on an
    unimplemented endpoint and fail at their THEN step. Only R2 tests call
    `/auth/login`.
- `TestClient` uses RestAssured's `CookieFilter` for cookies (no hand-written cookie
  jar). A new client makes its own
  `XSRF-TOKEN` cookie + header pair (double-submit), so `login()` is one POST with no
  extra GETs. Known gap: no test checks that the server sets `XSRF-TOKEN` on every
  response (D4); `cabinets-ui` E2E tests cover it.
- `MutableClock` (the movable test clock for D6): one shared instant; `withZone(z)`
  returns a clock with that zone over the same instant, so zone bugs are not hidden.
- R1 tests run the real bootstrap `ApplicationRunner` bean from a cached test
  context that has the `ADMIN_*` properties set, calling `run()` after the tables are
  emptied ("the application starts"). No second Spring app per test.
- `TestClockConfig` stays `@TestConfiguration`, imported by `ApiTest`: Spring Boot's
  test scan filter skips it, so the test clock exists only where imported (a plain
  `@Configuration` there was checked: component scanning picks it up in every test).
- Test setup sets `APP_BASE_URL`, so pass 2 can make it required.
- `loggedInAs` uses the email as stored in the database (principal name = stored
  email, D9). Users created by helpers get a low-cost bcrypt hash (strength 4) to
  keep tests fast; the app's encoder still verifies them.
- R2-S2 checks "no session is started" directly: no `SESSION` cookie on the client
  and no `spring_session` row.
- Coverage: `scripts/check-scenario-coverage.sh` (from `workflow`), by hand until CI.
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
