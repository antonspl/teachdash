# teachdash Constitution

How work on teachdash is meant to be done. This file, the OpenSpec specs and the
rest of the process documents are helpers, not blockers.

**The core principle: everyone is independent and interchangeable.** Whoever takes
a task is the **engineer** for it and decides what to do. The rules below show how
the work is normally built: they guide AI assistants, and they show engineers the
usual way to do things. An engineer may depart from them when they judge it right;
the place to discuss that is the PR review on GitHub. Changes to this file land
through a pull request like any other change.

## 1. Workflow

The **engineer** is the team member who takes the task. The engineer leads the
change and decides how it is done: which of the steps below to use, in which order,
and what each one covers. No step is mandatory and the order is not fixed.

One change = one branch = one pull request. What goes into the final version of the
PR is decided by the engineer together with the reviewers on GitHub. The **owner**
merges PRs into `main`; only the owner merges.

Keeping a change small makes it easier to review: about 6 requirements or fewer is
a good size. A larger feature can be split into several changes, each with its own
issue, spec and PR.

Steps the engineer can use:

- **Explore** — discuss the idea; no artifacts.
- **Clarify** — restate the task in plain words, list open questions and
  ambiguities, and propose research angles. The engineer answers them. Answers can
  be recorded in the proposal.
- **Research** — one researcher agent per angle.
- **Propose** — proposal, spec deltas, design, tasks. Every requirement has at
  least one Given/When/Then scenario.
- **Spec critique** — the spec-critic agent attacks the draft. Fix what has a clear
  answer, decide trade-offs, and turn findings outside the change into issues.
- **Review** — at any point the engineer may ask a team member or the reviewer
  agent to review the spec, the tests or the code. A review is advice; the engineer
  decides what to change.
- **Red** — write tests for the scenarios that fail for the right reason: at their
  THEN step because the behaviour is missing, not in setup. Test setup does not
  depend on endpoints that are not implemented yet; create data and sessions
  directly. Temporary stubs returning 501 are marked with a `STUB` comment.
- **Green** — implement until all tests pass.
- **Archive** — merge spec deltas into `openspec/specs/`, in the same PR, so specs
  on `main` describe what the code does.

**Tooling changes.** A change that does not change product behaviour (CI, scripts,
repository tooling) may set `skip_specs: true`. It has no spec deltas or scenarios.
Its tasks say how each part is proven, and the proof is recorded in the PR.

All artifacts, code, comments and commit messages are in English.

## 2. Testing (backend)

- JUnit 5 + Testcontainers (real Postgres, never H2) + RestAssured.
- One test per spec scenario. `@DisplayName` starts with the scenario ID, e.g.
  `@DisplayName("[accounts/R4-S2] Teacher cannot call admin endpoints")`.
- Test body is structured with `// given`, `// when`, `// then` comments.
- Every spec scenario has a matching test. CI runs `scripts/check-scenario-coverage.sh`
  and fails on a missing test.
- Frontend: Vitest for logic, Playwright E2E for key user flows.

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

### Code layout (backend)

Package by feature first, then by role inside the feature:

```
com/teachdash/
  <feature>/          e.g. accounts
    api/              controllers
      dto/            request/response records
    domain/           entities, enums, services
    persistence/      repositories
  common/             cross-feature code: security, errors, clock
```

Classes used across subpackages are `public`. Tests mirror the main packages;
shared test helpers live in `com.teachdash.support`.

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

- The platform serves several teachers from day one (about 1000 teachers).
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
- A student has at most one teacher.
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

## 10. Access from Russia and the EU

- The platform MUST be usable on fixed-line internet from both Russia and the EU.
  Mobile internet in Russia is not required.
- The browser reaches the app and its files only through the app's own domain. Direct
  links to third-party storage or CDNs are not used, because they can be throttled or
  blocked in Russia.
- Any other external service the browser must reach, such as video, is checked for
  access from both places before it is chosen.
- A hosting provider is tested from inside Russia before it is chosen.
