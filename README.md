# teachdash

A lightweight teaching platform for language teachers and their students — lessons,
exercises, a shared whiteboard and video calls.

Built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec): specs live in `openspec/`.

## Run locally

Needs Docker.

```
docker compose up --build
```

The backend listens on http://localhost:8080 (`/actuator/health`) and Postgres on
`localhost:5432`, both bound to the loopback interface. The dev credentials, including the
first admin (`admin@example.com` / `admin-dev-password`), are in `infra/dev.env`. They are
for local use only. To override a value, put it in a root `.env` (see `.env.example`).
Stop with `docker compose down -v`.

Backend tests: `cd backend && ./gradlew test` (needs Docker for Testcontainers).

## Frontend

Needs Node 26 (`frontend/.nvmrc`). With the stack running (`docker compose up --build`), the
container serves the built app on http://localhost:8081 and proxies `/api/*` to the backend.
Use it for E2E and to check the real setup. For development, `cd frontend && npm ci && npm run dev`
starts Vite on http://localhost:5173, which proxies `/api` to the backend on 8080.

Backend health is only at http://localhost:8080/actuator/health: `/actuator/health` on 8081 returns
the app. Checks: `npm run lint`, `npm run format:check`, `npm test`, `npm run build`.
