# Design

## Context

The frontend skeleton (`openspec/specs/web-client`) gives a React Router app with
`<Routes>` in `frontend/src/App.tsx`, an API client (`frontend/src/api/client.ts`:
`apiRequest`, `ApiError`, `messageForCode`) that sends the CSRF header and maps problem
codes to translated messages, i18n through `src/i18n/en.json`, shared UI components in
`src/ui/`, and a placeholder page with a reachability check (`useReachability`).

Backend endpoints used (`openspec/specs/accounts`):

- `POST /api/auth/login` — starts a session, rotates the CSRF token (`accounts/R4-S7`).
- `POST /api/auth/logout` — 401 `UNAUTHENTICATED` without a session.
- `GET /api/me` — the current user with role and name, 401 when anonymous.
- `POST /api/password-links` with `{ token, password }` — sets the password and starts a
  session (`accounts/R7-S1`); 410 for a used, expired or replaced link; 400 for a short
  password; 401 `ACCOUNT_DEACTIVATED`.
- Links have the form `${APP_BASE_URL}/set-password#token=<token>`.

## Goals / Non-Goals

**Goals:**
- One place that knows the current user, used by routing and the header.
- One place that reacts to a 401 from any request.
- Screens that later issues (#33 to #37, #57) extend without reworking routing.

**Non-Goals:**
- Tabs or navigation inside cabinets (#57).
- A general form library or validation framework (#44).

## Decisions

- **Session state in a React context.** An `AuthProvider` loads `GET /api/me` on
  start and exposes `status` (`loading`, `anonymous`, `user`), the user, and `login`,
  `logout`, `setPasswordWithLink`, `refresh`. Alternatives: React Query or Zustand —
  rejected, one value loaded once does not justify a dependency.
- **Route guards as components.** `RequireRole role="teacher"` wraps each cabinet;
  `AnonymousOnly` wraps `/login`; `/` renders a `HomeRedirect`. They read the context
  and use `<Navigate replace>`. The login page receives the page to return to in
  router state (`from`). Alternative: the data router with loaders — rejected, the
  skeleton uses `<Routes>` and the gain is small for now.
- **401 handling in the API client.** `apiRequest` gets an optional `onUnauthorized`
  hook set by `AuthProvider`. A 401 from any request except login, logout,
  `/api/me` and password links switches the state to `anonymous` with reason
  `session-ended`; the guard then sends the user to `/login` with `from` and the
  message. Alternative: check in every page — rejected, easy to forget.
- **Token from the fragment.** The set-password page reads `token` from
  `location.hash` once, keeps it in component state and immediately calls
  `history.replaceState` to drop the fragment, so the token is not left in history.
- **Client checks on set-password.** Length ≥ 8 and the two fields match, checked
  before sending. The backend stays the authority (400 messages are still shown).
- **410 message.** The API client maps codes, but 410 may come without a code we
  translate; the set-password page maps status 410 to its own "link no longer valid"
  string.
- **Logout.** Calls `POST /api/auth/logout`; on success or 401 it sets the state to
  `anonymous` (without the "session ended" reason) and navigates to `/login`.
- **Reachability moves to the login page.** `useReachability` is reused unchanged;
  the login page renders the warning only when unreachable. The placeholder page and
  its strings are removed.
- **Shared UI.** New components in `src/ui/` for text input, password input with a
  show toggle, button and app header, using design tokens, so pages stay free of raw
  styles (the design-token lint).
- **Tests.** Vitest with Testing Library for every scenario that can be checked with a
  mocked `fetch`. Playwright E2E against the real stack for `web-auth/R1-S1`,
  `R4-S1`, `R5-S1` and the `web-client` R11/R12 scenarios that need the real stack. The E2E logs in as the
  admin from `ADMIN_EMAIL` / `ADMIN_PASSWORD`, creates a teacher through the admin API
  to get a link, and uses a unique email per run.

No Flyway migrations. No new dependencies.

## Risks / Trade-offs

- [The E2E creates teachers on every run and the local database grows] → unique emails
  per run; the local stack can be reset with `docker compose down -v`.
- [E2E needs the admin credentials] → read them from the same `.env` that `compose`
  uses; fail fast with a clear message if they are missing.
- [A 401 during the start-up `/api/me` must not show "session ended"] → the start-up
  check and the auth endpoints are excluded from the `onUnauthorized` hook.
- [Removing the placeholder page breaks existing tests] → its tests are rewritten for
  the login page in the same change, matching the modified `web-client` scenarios.
