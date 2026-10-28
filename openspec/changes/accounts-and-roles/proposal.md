# Proposal: accounts-and-roles

## Clarifications

Questions asked on 2026-10-28 and the owner's answers.

| # | Question | Answer |
|---|----------|--------|
| 1 | UI language? | English only for now. Adding languages later must be cheap. |
| 2 | How does a student get an account? | Via a link from the teacher, or created directly by the teacher. |
| 3 | Is email sending needed in the first version? | No. The teacher gets links (set password, reset password) and sends them by hand (Telegram, WhatsApp). |
| 4 | How do teachers get accounts? | Created by the admin. Self-registration may come later if there is demand. |
| 5 | Student invite link: one-time or shared? | Both: a one-time link for one student, and a shared join link per teacher that can be disabled. |
| 6 | Login method? | Email + password. Google login maybe later. |
| 7 | Group lessons? | Yes, but groups belong to the schedule change, not this one. |
| 8 | Time zones? | Students live in different time zones, so every account stores its time zone. |

Defaults chosen by Claude (owner may override during review):

- One account has exactly one role. A teacher cannot also be a student with the same email.
- Set-password and reset links are single-use and expire after 7 days.
- A teacher can issue a password reset link for their own students; the admin for anyone.
- Minimum password length is 8 characters.

## Why

This is the first change of teachdash. Every later feature (courses, schedule,
translation, textbook import) needs accounts, roles and the link between a teacher
and their students. It also sets up the project skeleton, so later changes only add
features.

## What Changes

- Project skeleton: `backend/` (Spring Boot, Flyway, Testcontainers), `frontend/`
  (React + TypeScript + Vite, i18n-ready with English only), local `docker-compose`
  for Postgres, GitHub Actions CI running tests and the scenario coverage check.
- Accounts with roles admin, teacher, student; login and logout with email + password.
- First admin created from environment variables on startup.
- Admin creates, deactivates and reactivates teacher accounts.
- Single-use links to set or reset a password, sent by hand (no email).
- Teacher adds students: creates a student directly (gets a one-time link), or
  shares a join link that new and existing students can use.
- A student may be linked to several teachers.
- Teacher cabinet: list of own students, add student, manage join link, issue reset
  link, remove a student. Student cabinet: list of teachers and an empty "upcoming
  lessons" placeholder. Admin page: list of teachers and their students.
- Profile: name, time zone, change password.

## Out of Scope

- Email sending of any kind; Google or other external login.
- Teacher self-registration.
- Groups, courses, lessons, schedule (later changes).
- Production deployment (VPS, Caddy, SOPS secrets, Grafana) — a separate change.
- Login rate limiting and account lockout.
- Deleting accounts and personal data export.
- UI languages other than English.

## Capabilities

### New Capabilities

- `accounts`: user accounts, roles, login/logout, password links, admin management
  of teachers, profile.
- `student-enrolment`: the link between teachers and students — adding students,
  join links, student lists per teacher, teachers per student.

### Modified Capabilities

None.

## Impact

- New `backend/`, `frontend/`, `infra/` (local compose only), `.github/workflows/`.
- New REST API under `/api`.
- New tables: users, one-time tokens, teacher–student links, join links, HTTP sessions.
- New environment variables: database connection, `ADMIN_EMAIL`, `ADMIN_PASSWORD`,
  `ADMIN_NAME`, public base URL for links.
