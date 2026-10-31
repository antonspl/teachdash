# Proposal: accounts

## Clarifications

Questions asked on 2026-10-28 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | UI language? | English only for now. Adding languages later must be cheap. |
| 2 | How does a student get an account? | Via a link from the teacher, or created directly by the teacher (change `student-enrolment`). |
| 3 | Is email sending needed in the first version? | No. Links (set password, reset password) are sent by hand (Telegram, WhatsApp). |
| 4 | How do teachers get accounts? | Created by the admin. Self-registration may come later if there is demand. |
| 5 | Login method? | Email + password. Google login maybe later. |
| 6 | Time zones? | Users live in different time zones, so every account stores its time zone. |
| 7 | Size of the first change? | Split in three for reviewable PRs: `accounts` (this), `student-enrolment`, `cabinets-ui`. |
| 8 | Login errors: same response for every failure? | No (PR review). Tell the user the reason: unknown email, wrong password, deactivated, password not set. |
| 9 | CI in this change? | No (PR review). A separate DevOps task; this change only needs to run locally in Docker. |
| 10 | Secrets? | None for now, local build only. Production secret storage is a separate DevOps task. |
| 11 | Where do the backend skeleton and the coverage script live? (PR #17 review, 2026-10-31) | In separate PRs merged before this one: `backend-skeleton` (Spring Boot app, Gradle wrapper, Testcontainers test base, smoke test) and `workflow` (constitution, CONTRIBUTING, `scripts/check-scenario-coverage.sh`). |
| 12 | May pass 1 add production code? (PR #17 review) | Only 501 stubs marked `STUB` and schema migrations. Migrations are reviewed as production code at the tests gate. |
| 13 | Package layout? (PR #17 review) | Feature first, then role: `accounts/api`, `accounts/api/dto`, `accounts/domain`, `accounts/persistence`; shared code in `common`. |
| 14 | Session principal: email or user id? (PR #17 tests review) | User id (UUID): no letter-case mismatch, survives email changes, fits Spring Session's default column. |

Defaults chosen by Claude (owner may override during review):

- One account has exactly one role. A teacher cannot also be a student with the same email.
- Set-password and reset links are single-use and expire after 7 days.
- Minimum password length is 8 characters.

## Why

Every feature of teachdash needs accounts and roles. This change also adds a local
Docker setup, so the backend can be run by hand.

Depends on `backend-skeleton` and `workflow`, which merge into `main` first.

## What Changes

- A local `docker compose` setup that runs Postgres and the backend.
- Accounts with roles admin, teacher, student; login and logout with email + password.
- First admin created from environment variables on startup.
- Admin creates, deactivates and reactivates teacher accounts.
- Single-use links to set or reset a password, sent by hand (no email). In this
  change only the admin issues reset links.
- Profile: name, time zone, change password.

## Out of Scope

- Students joining teachers, teacher reset links for students — `student-enrolment`.
- All UI — `cabinets-ui`. This change is backend only.
- Email sending; Google or other external login; teacher self-registration.
- CI (GitHub Actions) — separate DevOps task.
- Production deployment, production secrets, observability — separate DevOps tasks.
- Login rate limiting and account lockout.
- Deleting accounts and personal data export.

## Capabilities

### New Capabilities

- `accounts`: user accounts, roles, login/logout, password links, admin management
  of teachers, profile.

### Modified Capabilities

None.

## Impact

- New `accounts` and `common` packages and Flyway migrations in `backend/`; `infra/`
  (local compose only).
- New REST API under `/api`.
- New tables: users, one-time tokens, HTTP sessions.
- Environment variables for local runs only, with dev defaults in `.env.example`:
  database connection, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`, `APP_BASE_URL`.
  No real secrets.
