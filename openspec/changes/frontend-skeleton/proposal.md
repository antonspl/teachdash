# Proposal

## Clarifications

- **How are frontend scenarios traced and gated?** Extend
  `scripts/check-scenario-coverage.sh` to also accept the `[id]` in Vitest and
  Playwright test titles. No test review gate: the gates are spec, then code, with
  tests and code reviewed together. `openspec/config.yaml` gets a frontend exception
  to its task rules in this PR.
- **How does the placeholder page show it can reach the backend?** It calls
  `GET /api/me`. A problem response such as the anonymous 401 counts as reachable.
  No backend change.
- **Which libraries?** npm, React Router, react-i18next, plain fetch, ESLint and
  Prettier.
- **How does the E2E get the real backend?** It needs a running stack:
  `docker compose up --build`, then the E2E command. It fails fast with a clear
  message if the stack is down.
- **Should the frontend work on phones?** The owner wants it to look good in mobile
  browsers. This is built as a responsive, mobile-first web app, set up here so every
  screen inherits it. A native iOS or Android app is not part of this.

## Why

The UI issues (#32 to #37) need a frontend to build on, and `frontend/` does not exist
yet. This is the first UI PR, as #19 was for the backend, and it settles the setup
once so that screen PRs contain only screens.

## What Changes

- New React + TypeScript + Vite app in `frontend/` with a router, a placeholder page
  and a not-found page.
- i18n from the start: English UI, every string through it.
- API client: same-origin `/api`, sends the CSRF header, maps a problem `code` to a
  translated message.
- Vitest, and one Playwright E2E file running against the real backend.
- Mobile-first base styles, so pages fit phone screens in mobile browsers.
- Linting and formatting.
- Local run: Vite dev proxy to the backend, and a Caddy container in `docker compose`
  that serves the app and proxies `/api/*`, with no CORS.
- `scripts/check-scenario-coverage.sh` also looks in frontend tests.
- `openspec/config.yaml`: task rules get an exception for frontend-only changes.
- README: how to run and test the frontend.

**Out of scope:**

- Product screens, login and session handling (#32 and later).
- Native iOS or Android apps, and installable or offline (PWA) features.
- `/ws/*` proxying: added with the whiteboard, since no WebSocket endpoint exists yet.
- Production Caddy config, HTTPS and domain (#9).
- CI (#7).
- UI languages other than English.
- Backend changes.

## Capabilities

### New Capabilities

- `web-client`: the browser application shell and how it talks to the backend:
  routing, UI strings, the API client and same-origin serving.

### Modified Capabilities

None.

## Impact

- **New:** `frontend/`, `infra/Caddyfile`.
- **Changed:** `infra/docker-compose.yml` and `infra/docker-compose.dev.yml` (new
  `frontend` service), `scripts/check-scenario-coverage.sh`, `openspec/config.yaml`,
  `README.md`.
- **Backend:** no code or API changes; it uses existing `GET /api/me`, the
  `XSRF-TOKEN` cookie and the problem `code`.
- **Dependencies:** a new npm dependency set, justified in `design.md`. Node is needed
  for local frontend work.
