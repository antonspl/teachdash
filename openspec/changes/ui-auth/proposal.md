# Proposal

## Clarifications

Defaults suggested by Claude; the engineer has not answered them yet and can change
any of them:

- **Where each role lands.** Separate paths: `/admin`, `/teacher`, `/student`.
- **What `/` does.** A logged-in user goes to their cabinet, an anonymous one to
  `/login`. The placeholder page is removed. The backend reachability status moves to
  the login page and is shown only when the backend cannot be reached.
- **Session ended mid-use** (a request gets 401). Go to `/login` with a "your session
  has ended" message; after logging in, return to the page the user was on.
- **Another role's page** (a teacher opens `/admin`). Redirect to the user's own
  cabinet.
- **Set-password form.** Two fields (password and confirmation), a "show password"
  toggle, a client check that both match and that the password has at least 8
  characters.
- **After setting a password.** The backend already starts a session on success
  (`accounts/R7-S1`), so the user goes straight to their cabinet.
- **Logged-in user opens a set-password link.** The page is shown anyway; on success
  the new session replaces the old one.
- **Logout and "forgot password".** A minimal header on cabinet pages with the user's
  name and "Log out". The login page says to ask the teacher or administrator for a
  new link, since there is no self-service reset.
- **Capability.** A new `web-auth` capability; `web-client` stays the application
  shell.
- **Session state.** Loaded once from `GET /api/me` on start and kept in the app; no
  new libraries.
- **Token in the URL.** Read from the fragment and removed from the address bar at
  once.
- **Client validation.** Only password length and match. Shared field rules are left
  to #44.

No separate research was done; facts about the backend come from `openspec/specs/accounts`
and the controllers in `backend/`.

## Why

The frontend has only a placeholder page, so nobody can use the platform in a browser.
Every other UI issue (#33 to #37, #57) needs a logged-in user with a role, so login,
logout, the set-password link and role routing come first.

## What Changes

- Login page at `/login`: email and password, translated errors, a hint about the
  set-password link, and the backend reachability warning.
- Current user loaded on start; redirects by login state and role.
- An empty cabinet page per role (`/admin`, `/teacher`, `/student`) with a header
  showing the user's name and a "Log out" button.
- Logout.
- Set-password page at `/set-password#token=…` (the path the backend puts in its links).
- When the session ends during use, the user is sent to the login page and brought back
  after logging in.
- **BREAKING (UI only):** `/` no longer shows the placeholder page; it redirects.
- Vitest tests for every scenario, and Playwright E2E for login → cabinet → logout and
  for setting a password from a link.

**Out of scope:**

- Profile and password change (#33), admin screens (#34), teacher and student lists
  (#35, #36), join links (#37), cabinet tabs (#57).
- Shared validation rules for user fields (#44).
- Self-service password reset, "remember me", login rate limiting (#12).
- Backend changes.

## Capabilities

### New Capabilities

- `web-auth`: login, logout, the set-password link page, the current user, role
  cabinets and routing by login state and role.

### Modified Capabilities

- `web-client`: R1 and R3 are replaced by R11 (the root path redirects instead of
  showing the placeholder page) and R12 (the reachability warning on the login page),
  because OpenSpec cannot drop a scenario from a modified requirement. R2's scenario
  now uses the login page.

## Impact

- `frontend/`: new pages, routing, session state, header; the placeholder page and its
  tests are removed or moved; new strings in `src/i18n/en.json`; new E2E files.
- No new dependencies. No backend or infrastructure changes.
