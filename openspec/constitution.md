# teachdash Constitution

Non-negotiable principles for every change. Proposals, designs, tasks and code
MUST comply. Changing this file requires explicit approval from the project owner.

## 1. Workflow

1. **Explore** (optional) — discuss the idea; no artifacts.
2. **Clarify** — before writing a proposal, list open questions and ambiguities
   and get the owner's answers. Record the answers in the proposal.
3. **Propose** — proposal, spec deltas, design, tasks. Every requirement has at
   least one Given/When/Then scenario. No code.
4. **Red** — write backend tests for every scenario; they MUST compile and fail
   for the right reason (missing behaviour, not missing classes — add stubs).
5. **Test review gate** — STOP. The owner reviews the tests. No production code
   until the owner approves.
6. **Green** — implement until all tests pass. Approved tests MUST NOT be changed;
   if a test looks wrong, stop and ask the owner.
7. **Archive** — merge spec deltas into `openspec/specs/`.

All artifacts, code, comments and commit messages are in English.

## 2. Testing (backend)

- JUnit 5 + Testcontainers (real Postgres, never H2) + RestAssured.
- One test per spec scenario. `@DisplayName` starts with the scenario ID, e.g.
  `@DisplayName("[accounts-and-roles/R1-S2] Teacher cannot open admin panel")`.
- Test body is structured with `// given`, `// when`, `// then` comments.
- CI fails if any spec scenario has no matching test.
- Frontend: Vitest for logic, Playwright E2E for key user flows. No review gate.

## 3. Architecture

Monorepo:

```
backend/    Spring Boot (Java, Gradle) — REST + WebSocket
frontend/   React + TypeScript + Vite
infra/      docker-compose, Caddyfile, Grafana Alloy config, encrypted secrets
openspec/   specs and changes
```

- **Database:** PostgreSQL. Schema changes only via Flyway migrations in `backend/`.
  Migrations are forward-only; never edit an applied migration.
- **Frontend** is static files served by Caddy. One domain: `/api/*` and `/ws/*`
  are proxied to the backend — no CORS.
- **Whiteboard:** Excalidraw on the frontend, Spring WebSocket relay on the backend.
- **Video:** external provider (LiveKit Cloud); no custom WebRTC. Until it is built,
  a lesson card holds a meeting link.
- **Roles:** admin, teacher, student. Authorization is checked on the backend for
  every endpoint; the frontend only hides UI.

## 4. Secrets

- Never commit plaintext secrets. Local: `.env` (gitignored), `.env.example` is
  committed and lists every variable.
- Production: SOPS + age encrypted file in `infra/`, decrypted by CI at deploy time
  into `.env` on the server (mode 600). The age key lives only with the owner and
  in GitHub Actions secrets.
- Config comes from environment variables; no secrets in `application.yml`.

## 5. Observability

- Backend logs structured JSON to stdout (Spring Boot structured logging). No log
  files. Never log secrets, passwords, tokens or full personal data.
- Metrics: Actuator + Micrometer (Prometheus format). Traces: OpenTelemetry Java agent.
- Grafana Alloy on the server ships logs, metrics and traces to Grafana Cloud.

## 6. Deployment

- One VPS, Docker Compose, Caddy (automatic HTTPS).
- GitHub Actions on push to `main`: tests → build images → push to GHCR → SSH to
  server → `docker compose pull && docker compose up -d`.
- Flyway runs on application start.
- Nightly `pg_dump` to off-server storage; restore is tested before real users join.
- Environments: local and prod. Add staging when real students use the platform.

## 7. Simplicity

- Prefer boring, well-known libraries. Add a dependency only with a stated reason
  in `design.md`.

## 8. Multiple teachers

- The platform serves several teachers from day one (a small number, not thousands).
- One shared database. Teacher-owned data (students' enrolments, lessons, exercises,
  homework, materials) carries an owning `teacher_id`.
- A teacher changes only their own data. Every backend query on teacher-owned
  data is scoped by the current teacher; tests cover access to another teacher's
  data.
- Exception: lessons and exercises are visible to all teachers for copying. A copy
  is an independent record owned by the copying teacher; later edits to the
  original or the copy do not affect the other. Student data, homework results and
  progress stay private to the teacher.
- Lessons and exercises (own or copied) can be copied into homework the same way.
- A student may study with more than one teacher and sees data from each of them.
- Admin manages teacher accounts and can see all data.

## 9. Subjects and languages

- **Initial scope: only the English–Russian pair.** Courses teach English or Russian,
  and translation works between English and Russian. Other languages and other
  subjects are not built until there is a request.
- The core (courses, lessons, exercises, schedule) MUST NOT depend on the subject,
  so other subjects can be added later.
- A course MAY have a target language, stored as an ISO 639-1 code. Supported
  languages are a configured list, initially `en` and `ru`. Code MUST NOT hard-code
  specific languages; language-specific behaviour (answer normalisation, LLM
  prompts) comes from per-language settings.
- Language features (translation, vocabulary) apply to courses with a target
  language. Translation works between any two supported languages.
- Features that handle learning content MUST work for every supported language,
  including Cyrillic text; specs include scenarios for `en` and `ru`.
- The UI language is separate from the target language. UI is English for now and
  every UI string goes through i18n.
