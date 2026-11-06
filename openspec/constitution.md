# teachdash Constitution

Non-negotiable principles for every change. Proposals, designs, tasks and code
MUST comply. Changing this file requires explicit approval from the project owner.

## 1. Workflow

Roles:

- **Engineer** — the team member who took the task. The engineer answers the
  clarifying questions, agrees the research angles, decides trade-offs and approves
  the gates: at each gate the engineer checks what Claude produced, helped by the
  spec-critic (spec gate) and the Opus review (code gate).
- **Code owner** — a team member listed in `.github/CODEOWNERS`. A code owner other
  than the engineer gives the final approval.
- **Owner** — the project owner. The only one who may merge into `main` without an
  approval. Changes to this file, `openspec/config.yaml` and `.github/CODEOWNERS`
  need the owner's approval. Any engineer may improve the rest of the workflow
  (`CLAUDE.md`, `CONTRIBUTING.md`, `.claude/`, the PR template) through a normal PR.

One change = one branch = one pull request. The PR goes through three review gates
approved by the engineer, then a code owner other than the engineer approves the
whole PR. Then anyone on the team merges it.

A change has at most 6 requirements, and fewer is better, so one person can review
it. The number of scenarios is not limited. A larger feature is split into several changes, each
with its own issue, spec and PR.

1. **Explore** (optional) — discuss the idea; no artifacts. The engineer decides
   whether it is needed: skip it when the issue already has the exploration.
2. **Clarify** — before writing a proposal, restate the task in plain words, list
   open questions and ambiguities, and propose research angles. Get the engineer's
   answers and agreement on the angles, or agreement that a small task needs none.
   Record the answers in the proposal. Keep it short: ask only what the issue and
   the exploration do not already answer.
3. **Research** — one researcher agent per agreed angle. If the findings raise new
   decisions, ask the engineer before proposing.
4. **Propose** — proposal, spec deltas, design, tasks. Every requirement has at
   least one Given/When/Then scenario. No code. Push the branch, open a draft PR.
5. **Spec critique** — the spec-critic agent attacks the draft, on every change.
   Fix what has a clear answer, ask the engineer about real trade-offs, and turn
   findings outside the change into issues. Run it again if a fix was non-trivial.
   Its report is not an approval; the gate request summarises what it found.
6. **Spec review gate** — STOP. The engineer approves the spec in the PR. No tests
   until approved.
7. **Red** — write backend tests for every scenario; they MUST compile and fail
   for the right reason: each test fails at its THEN step because the behaviour is
   missing — not in setup, not because classes are missing. Test setup MUST NOT
   depend on endpoints that are not implemented yet (create data and sessions
   directly). Pass 1 may add only:
   - **stubs** that return 501, each marked with a `STUB` comment; pass 2 rewrites them;
   - **schema migrations** — permanent production code; the engineer reviews them
     as production code at the test review gate.
8. **Test review gate** — STOP. The engineer approves the tests in the PR. No
   production code until approved.
9. **Green** — implement until all tests pass. Approved tests MUST NOT be changed;
   if a test looks wrong, stop and ask the engineer.
10. **Code review** — the Opus review runs, then the engineer approves the code in the PR. Security problems,
   data loss and spec violations are fixed in the PR. Every other finding becomes
   an issue, so rare edge cases do not add more review rounds.
11. **Archive** — merge spec deltas into `openspec/specs/`, in the same PR.
12. **Final review** — mark the PR ready. A code owner other than the engineer
    reviews spec, tests and code together and approves the PR on GitHub. Then anyone on
    the team merges it (with `ci-ok` green). Only the owner may merge without an
    approval.

**Tooling changes.** A change that does not change product behaviour (CI, scripts, repository tooling) may set `skip_specs: true`. It has no spec deltas or scenarios. The spec critique and spec review gate (steps 5 and 6) apply to its proposal and design. It has no Red step or test review gate. Its tasks say how each part is proven, and the proof is recorded in the PR for the code review. Archive merges nothing into `openspec/specs/`.

A gate is approved by a PR comment naming it (e.g. "tests approved") plus the
matching box ticked in the PR description. A GitHub review "Approve" is used only
for the final review: it does not say which gate it is for, and it stays on
the PR when the next phase is pushed.

At each gate Claude stops and posts the commit range of that phase
(e.g. `abc123..def456`), so the engineer reads only what is new.

All artifacts, code, comments and commit messages are in English.

## 2. Testing (backend)

- JUnit 5 + Testcontainers (real Postgres, never H2) + RestAssured.
- One test per spec scenario. `@DisplayName` starts with the scenario ID, e.g.
  `@DisplayName("[accounts/R4-S2] Teacher cannot call admin endpoints")`.
- Test body is structured with `// given`, `// when`, `// then` comments.
- Every spec scenario has a matching test. CI runs `scripts/check-scenario-coverage.sh`
  and fails on a missing test.
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
