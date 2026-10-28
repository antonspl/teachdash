# Design: accounts-and-roles

## Context

Empty repository except `openspec/`. See proposal.md for scope. The constitution
fixes the stack (Spring Boot, Postgres + Flyway, React + TS + Vite, one domain with
`/api/*` proxied, no CORS) and the test approach.

## Goals / Non-Goals

**Goals:**
- A skeleton that later changes extend without restructuring.
- Session-based auth that survives backend restarts and deploys.
- Per-teacher scoping of student data from the first endpoint.

**Non-Goals:**
- Production deployment, observability shipping (only JSON logs + Actuator health now).
- Any outgoing email.

## Decisions

### D1. Versions and build
Java 25 (LTS), latest stable Spring Boot, Gradle with Kotlin DSL. Node LTS for the
frontend. The implementer pins exact versions at setup.

### D2. Backend layout — package by feature
`com.teachdash.accounts`, `com.teachdash.enrolment`, `com.teachdash.common`
(security, errors). Later features add their own packages. Alternative (layered
`controller/service/repository` at top level) mixes features and grows badly.

### D3. Persistence — Spring Data JDBC
Simple aggregates, explicit SQL in Flyway, no lazy-loading surprises.
Alternative: JPA/Hibernate — more magic than this project needs.
Dependency: `spring-boot-starter-data-jdbc`.

### D4. Authentication — server-side session + cookie
Spring Security with a JSON login endpoint, `HttpSession` stored in Postgres via
**Spring Session JDBC** (dependency: `spring-session-jdbc`) so sessions survive
restarts and can be ended per user (deactivation, password reset).
Cookie: `HttpOnly`, `SameSite=Lax`, `Secure` in prod. CSRF protection with a
cookie-held token (`XSRF-TOKEN` → `X-XSRF-TOKEN` header), which the SPA sends.
Alternative: JWT — no clean way to revoke sessions, and no benefit on one domain.
Passwords: Spring Security `DelegatingPasswordEncoder` (bcrypt).

### D5. One-time links
Token = 32 random bytes, base64url. Only the SHA-256 hash is stored, with purpose
(`SET_PASSWORD`, `RESET_PASSWORD`), account, expiry (7 days), `used_at`. Issuing a
new token marks older unused tokens of that account as used. The API returns the
full URL built from `APP_BASE_URL`: `{base}/set-password?token=…`.
Join links: token stored in plain text (teacher must see it again; it is revocable
and grants only "become this teacher's student"). URL: `{base}/join/{token}`.

### D6. Time source
A `Clock` bean, so tests can move time (R7-S3 expiry) without sleeping.

### D7. Errors
RFC 7807 `ProblemDetail` for all errors. Login failures always return the same body.
Not found across teachers is 404, never 403, so a teacher cannot probe other
teachers' students.

### D8. API (all under `/api`)

| Method | Path | Role | Spec |
|---|---|---|---|
| POST | `/auth/login` | — | accounts R2 |
| POST | `/auth/logout` | any | accounts R3 |
| GET | `/me` | any | accounts R2, R9 |
| PATCH | `/me` | any | accounts R9 |
| POST | `/me/password` | any | accounts R9 |
| POST | `/password-links/{token}` | — | accounts R7 |
| GET/POST | `/admin/teachers` | admin | accounts R5, enrolment R7 |
| POST | `/admin/teachers/{id}/deactivate`, `/reactivate` | admin | accounts R6 |
| POST | `/admin/users/{id}/reset-link` | admin | accounts R8 |
| GET | `/admin/teachers/{id}/students` | admin | enrolment R7 |
| GET/POST | `/teacher/students` | teacher | enrolment R1, R4 |
| GET/DELETE | `/teacher/students/{id}` | teacher | enrolment R4, R5 |
| POST | `/teacher/students/{id}/reset-link` | teacher | accounts R8 |
| GET/POST/DELETE | `/teacher/join-link` | teacher | enrolment R2 |
| POST | `/join/{token}` | none or student | enrolment R3 |
| GET | `/student/teachers` | student | enrolment R6 |

### D9. Data model and Flyway migrations
- `V1__accounts.sql` — `users` (id uuid, email, name, role, password_hash nullable,
  time_zone, active, created_at; unique index on `lower(email)`),
  `one_time_tokens` (token_hash, user_id, purpose, expires_at, used_at, created_at).
- `V2__enrolment.sql` — `teacher_students` (teacher_id, student_id, created_at,
  PK both), `join_links` (teacher_id PK, token unique, created_at).
  Disabled = row deleted.
- `V3__spring_session.sql` — Spring Session JDBC tables (copied from its schema).

### D10. Admin bootstrap
An `ApplicationRunner` reads `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` and creates
the admin only if no admin exists (accounts R1).

### D11. Frontend
- React Router for pages; TanStack Query for server calls (caching, loading states —
  avoids hand-written fetch state). Dependencies: `react-router`, `@tanstack/react-query`.
- **Mantine** UI kit: ready forms, tables, modals, and a date/time picker needed by the
  schedule change. Alternative: Tailwind + hand-made components — slower to build.
- **react-i18next**: every UI string is a key in `locales/en.json`, so adding a
  language is one new file.
- Pages: login, set password, join, teacher cabinet (students, add student, join link,
  reset link, remove), student cabinet (teachers, "upcoming lessons" placeholder),
  admin (teachers, create, deactivate, students per teacher), profile.
- Browser time zone (`Intl.DateTimeFormat().resolvedOptions().timeZone`) prefills the
  time zone field.
- Vite dev server proxies `/api` to the backend, matching the prod Caddy setup.

### D12. Tests and CI
- Backend: a base class `ApiTest` starts the app on a random port with a Testcontainers
  Postgres (reused across tests), cleans tables between tests, and offers helpers to
  create users and log in (RestAssured with cookie + CSRF handling).
- `scripts/check-scenario-coverage.sh`: collects scenario IDs from
  `openspec/specs/**/spec.md` and `openspec/changes/*/specs/**/spec.md` and fails if
  any ID has no `@DisplayName("[<id>]` in `backend/src/test`.
- GitHub Actions on push and PR: backend tests, frontend lint + Vitest + build,
  coverage script. Playwright E2E runs in CI against the backend started with a
  Postgres service.
- Local: `infra/docker-compose.dev.yml` with Postgres; `.env.example` lists all vars.

### D13. Logging
Spring Boot structured logging (`logging.structured.format.console`) to stdout. No
passwords, tokens or token hashes in logs. Actuator `health` exposed only.

## Risks / Trade-offs

- [A teacher can reset the password of a student who also studies with another
  teacher, i.e. take over that account] → acceptable with a few trusted teachers;
  revisit if teachers become untrusted (make reset admin-only or student-confirmed).
- [No login rate limiting — password guessing is possible] → strong-enough minimum
  length now; add rate limiting before public launch.
- [Join link tokens stored in plain text] → they grant only enrolment and can be
  regenerated at once.
- [Without email, a student who loses their password depends on the teacher] →
  the teacher can issue a reset link at any time.
