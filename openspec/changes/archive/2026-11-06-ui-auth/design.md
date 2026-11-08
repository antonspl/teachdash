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
  start and exposes `status` (`loading`, `error`, `anonymous`, `user`), the user, and
  `login`, `logout`, `setPasswordWithLink`, `retry`. A 401 on start means `anonymous`;
  any other failure (network, 500) means `error`, shown as a message with "Try again",
  never as `anonymous`. Alternatives: React Query or Zustand — rejected, one value
  loaded once does not justify a dependency.
- **Route guards as components.** `RequireRole role="teacher"` wraps each cabinet;
  `AnonymousOnly` wraps `/login`; `/` renders a `HomeRedirect`. They read the context
  and use `<Navigate replace>`. Alternative: the data router with loaders — rejected,
  the skeleton uses `<Routes>` and the gain is small for now.
- **Who decides where to go after login.** The login page. On success it navigates
  itself: to the remembered page if it belongs to the new user's role, otherwise to
  their cabinet. `AnonymousOnly` redirects only when the page is opened by a user who
  is already logged in, not while a login submitted on that page is finishing, so the
  two never race.
- **401 handling in the API client.** `apiRequest` gets an optional `onUnauthorized`
  hook set by `AuthProvider`. A 401 from any request except login, logout,
  `/api/me` and password links switches the state to `anonymous` with reason
  `session-ended` and the current path. Nothing sends such a request yet (the cabinets
  are empty), so `web-auth` R6 is tested by sending a request through `apiRequest`
  from a test page; #33 to #37 get the behaviour for free. Alternative: check in every
  page — rejected, easy to forget.
- **The return is kept in memory only.** The reason and path live in the
  `AuthProvider` state, not in storage or the URL. A reload drops them: the user then
  just goes to their cabinet after login (`web-auth/R6-S4`).
- **Token from the fragment.** The set-password page reads `token` from
  `location.hash` in a `useState` initialiser, so it is read during the first render;
  then it calls `history.replaceState` to drop the fragment. Reading the hash in an
  effect would break under React strict mode, which runs effects twice: the second run
  would see no token (`web-auth/R5-S2` renders in strict mode to catch this).
- **Client checks on set-password.** Length ≥ 8 characters, ≤ 72 bytes in UTF-8
  (measured with `TextEncoder`, the backend's bcrypt limit), and the two fields match,
  checked before sending. The backend stays the authority.
- **Link errors use their codes.** The backend answers 410 with `LINK_INVALID` and
  409 with `LINK_CONFLICT`; both already have translations in `en.json`, so the API
  client's code mapping is enough. No mapping by status.
- **A logged-in user using a link.** The backend replaces the session; the page sets
  the user from the response and opens that user's cabinet (`web-auth/R5-S10`).
- **Logout.** Calls `POST /api/auth/logout`; on success or 401 it sets the state to
  `anonymous` (without the "session ended" reason) and navigates to `/login`. On any
  other failure the cabinet stays and shows the translated error.
- **Reachability moves to the login page.** `useReachability` is reused unchanged;
  the login page renders the warning only when unreachable. The placeholder page and
  its strings are removed.
- **Shared UI.** New components in `src/ui/` for text input, password input with a
  show toggle, button, error with "Try again", and app header, using design tokens, so
  pages stay free of raw styles (the design-token lint).
- **Tests.** Vitest with Testing Library for every scenario that can be checked with a
  mocked `fetch`; Playwright E2E for scenarios that need the real stack. Each scenario
  ID has exactly one test (see `tasks.md`). The E2E logs in as the admin from
  `ADMIN_EMAIL` / `ADMIN_PASSWORD`, creates a teacher through the admin API to get a
  link, and uses a unique email per run.
- **The login hint and `web-client` R5-S4.** R5-S4 is the `INVALID_CREDENTIALS`
  message after a failed login; `web-auth/R1-S5` is a separate static text always shown
  on the page. Both stay; they are tested separately.

No Flyway migrations. No new dependencies.

## Risks / Trade-offs

- [The E2E creates teachers on every run and the local database grows] → unique emails
  per run; the local stack can be reset with `docker compose down -v`.
- [E2E needs the admin credentials] → read them from the same `.env` that `compose`
  uses; fail fast with a clear message if they are missing.
- [A 401 during the start-up `/api/me` must not show "session ended"] → the start-up
  check and the auth endpoints are excluded from the `onUnauthorized` hook.
- [Removing the placeholder page breaks existing tests, including the phone-width and
  security-header E2E] → they move to the login page in the same change, matching
  `web-client` R7, R8, R11 and R12.
