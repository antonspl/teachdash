# teachdash

A lightweight teaching platform for language teachers and their students — lessons,
exercises, a shared whiteboard and video calls.

Built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec): specs live in `openspec/`.

## Run locally

Needs Docker.

```
docker compose up --build --wait
```

`--wait` returns once Postgres, the backend and the web container are healthy (it fails if one
never becomes healthy). The web container starts without waiting for the backend, so with the
backend down it still serves the page, which shows "The server cannot be reached."

The backend listens on http://localhost:8080 (`/actuator/health`) and Postgres on
`localhost:5432`, both bound to the loopback interface. The dev credentials, including the
first admin (`admin@example.com` / `admin-dev-password`), are in `infra/dev.env`. They are
for local use only. To override a value, put it in a root `.env` (see `.env.example`).
Stop with `docker compose down -v`.

Backend tests: `cd backend && ./gradlew test` (needs Docker for Testcontainers).

## Frontend

Needs Node 26 (`frontend/.nvmrc`). With the stack running (`docker compose up --build --wait`), the
container serves the built app on http://localhost:8081 and proxies `/api/*` to the backend.
Use it for E2E and to check the real setup. For development, `cd frontend && npm ci && npm run dev`
starts Vite on http://localhost:5173, which proxies `/api` to the backend on 8080.

Health endpoints: the backend at http://localhost:8080/actuator/health (on 8081 that path returns
the app), the web container at http://localhost:8081/healthz. The `/api/*` target defaults to
`backend:8080`; set `BACKEND_UPSTREAM` in a root `.env` to change it. Checks: `npm run lint`, `npm run format:check`, `npm test`, `npm run build`.

The placeholder page checks the backend once on load, when the tab becomes visible and when the
browser comes back online; it does not poll. A page opened before the backend is up shows
"unreachable" until you reload it or switch back to the tab.

### Frontend tests

Unit tests (Vitest) need no stack: `cd frontend && npm ci && npm test`.

E2E tests (Playwright) run against the container on http://localhost:8081, in desktop Chromium
and in Chrome and Safari phone profiles. Once, install the
browsers (after `npm ci`, so the locked version is used):
`cd frontend && npx --no-install playwright install chromium webkit`. Then start the stack
(`docker compose up --build --wait`) and run `npm run test:e2e`. Run `docker compose up -d --wait` first if the stack was
stopped. If the web container or the backend is down, the run stops with a message saying which. The HTML report goes to `frontend/playwright-report/`.

Spec coverage check, from the repo root: `scripts/check-scenario-coverage.sh`.
