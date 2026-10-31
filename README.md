# teachdash

A lightweight teaching platform for language teachers and their students — lessons,
exercises, a shared whiteboard and video calls.

Built spec-first with [OpenSpec](https://github.com/Fission-AI/OpenSpec): specs live in `openspec/`.

## Run locally

Needs Docker.

```
cp .env.example .env
docker compose -f infra/docker-compose.yml up -d --build
```

The backend listens on http://localhost:8080 (`/actuator/health`). The first admin is
created from `ADMIN_EMAIL` / `ADMIN_PASSWORD` in `.env`. Stop with
`docker compose -f infra/docker-compose.yml down -v`.

Backend tests: `cd backend && ./gradlew test` (needs Docker for Testcontainers).
