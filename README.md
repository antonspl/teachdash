# teachdash

A lightweight teaching platform for language teachers and their students — lessons,
exercises, a shared whiteboard and video calls.

Built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec): specs live in `openspec/`.

## Run locally

Needs Docker.

```
docker compose up --build
```

The backend listens on http://localhost:8080 (`/actuator/health`). The first admin is
`admin@example.com` / `admin-dev-password`. To override any setting, copy `.env.example`
to `.env` in the repo root and edit it. Stop with `docker compose down -v`.

Backend tests: `cd backend && ./gradlew test` (needs Docker for Testcontainers).
