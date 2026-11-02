# Tasks

Frontend-only change: no failing-tests pass and no test review gate (constitution §2).
Each group lands its own tests, with the scenario ID at the start of the test title.

## 1. Project setup

- [x] 1.1 Create the Vite + React + TypeScript app in `frontend/` with the dependencies from `design.md`, `package-lock.json` and `.nvmrc`; verify `npm ci && npm run build` succeeds
- [x] 1.2 Configure ESLint and Prettier and add the `lint`, `format` and `format:check` scripts; verify `npm run lint` and `npm run format:check` pass
- [x] 1.3 Configure Vitest with jsdom and add a `test` script; verify `npm test` runs
- [x] 1.4 Add the Vite dev proxy from `/api` to `http://localhost:8080`; verify that with the backend running, `curl -i localhost:5173/api/me` returns the backend's 401 problem

## 2. i18n and API client

- [x] 2.1 Set up i18n with `src/i18n/en.json`, including `errors.generic` and an `errors.<CODE>` message for each code the backend names in `backend/src/main` (framework status-name codes such as `METHOD_NOT_ALLOWED` get the generic message); verify the search result matches the keys
- [x] 2.2 Implement the API client (same-origin `/api`, CSRF header, `ApiError`); verify with the test `[web-client/R4-S1]`
- [x] 2.3 Implement mapping of problem codes to translated messages; verify with the tests `[web-client/R5-S1]` and `[web-client/R5-S2]`

## 3. Pages

- [x] 3.1 Add the router, the placeholder page and the not-found page, with all text through i18n; verify with the test `[web-client/R2-S1]`
- [x] 3.2 Add the reachability status to the placeholder page, using `GET /api/me`; verify with the test `[web-client/R3-S2]`
- [x] 3.3 Add the viewport meta tag, `src/styles/tokens.css`, the mobile-first `base.css` and the `src/ui/` components the two pages need, with no appearance defined in the pages; verify by changing one colour token and seeing both pages change. `[web-client/R7-S1]` (task 5.2) checks the mobile layout
- [x] 3.4 Add the token check script to `npm run lint`; verify it fails on a temporary raw colour in a CSS Module and on a temporary inline `style=`, and passes once they are removed

## 4. Container

- [x] 4.1 Add `frontend/Dockerfile`, `frontend/Dockerfile.dockerignore` and `infra/Caddyfile` as in `design.md`; verify the image builds
- [x] 4.2 (Port replaced by 8.2.) Add the `frontend` service to `infra/docker-compose.yml` and publish `127.0.0.1:8081:80` in `infra/docker-compose.dev.yml`; verify `docker compose up --build` serves the placeholder page at `http://localhost:8081` and it shows "reachable"
- [x] 4.3 Add "Frontend" run instructions to `README.md`: dev server, container, which port is for what, direct backend URL for health; verify the commands work as written

## 5. E2E and scenario coverage

- [x] 5.1 Add the Playwright config with the desktop and two phone projects from `design.md`, and a global setup that stops with a clear message if the stack is down, plus a `test:e2e` script; verify the message appears with the stack stopped
- [x] 5.2 Write `e2e/skeleton.spec.ts` with `[web-client/R1-S1]`, `[web-client/R1-S2]`, `[web-client/R3-S1]`, `[web-client/R6-S1]`, `[web-client/R6-S2]` and `[web-client/R7-S1]`; verify `npm run test:e2e` passes in all projects against the running stack
- [x] 5.3 Extend `scripts/check-scenario-coverage.sh` to accept IDs in frontend test titles, as in `design.md`; verify it reports all scenarios covered, and that it fails when one frontend test title is temporarily changed
- [x] 5.4 Add test instructions to `README.md`; verify from a clean `docker compose down -v` that `docker compose up --build`, `npm test` and `npm run test:e2e` all pass as written

## 6. Owner review: container

- [x] 6.1 Rewrite `infra/Caddyfile` as in `design.md` (admin off, `/healthz`, configurable upstream, `/assets/*` rule, security headers, compression, cache headers), list `BACKEND_UPSTREAM` in `.env.example`; verify with the tests `[web-client/R1-S3]`, `[web-client/R8-S1]`, `[web-client/R9-S1]`, `[web-client/R9-S2]` and `[web-client/R10-S1]`, and that port 2019 does not answer inside the container
- [x] 6.2 Add the compose hardening and healthchecks from `design.md`, including `curl` in the backend image; verify `docker compose up --build --wait` reports all services healthy, the frontend still serves as a non-root user with a read-only filesystem, and the backend tests still pass
- [x] 6.3 Remove the polling from the Playwright global setup, read the base URL from its config, and add `[web-client/R5-S3]`; verify `npm run test:e2e` passes and the stack-down message still appears
- [x] 6.4 Update the README for health endpoints and `--wait`; verify the commands work as written

## 7. Owner review: code

- [x] 7.1 (Replaced by 12.4.) Retry the status check while unreachable; verify with the test `[web-client/R3-S3]`
- [x] 7.2 API client: explicit `Accept`, rethrow a deliberate cancel, keep `cause` on other failures; verify with unit tests for both
- [x] 7.3 Vite proxy key `^/api/`; verify `/api/me` still proxies and `/api-keys` does not
- [x] 7.4 Extend the token check as in `design.md`; verify it fails on a temporary raw `px` in a CSS Module, a raw `rem`, a CSS import in a page and a `className` in a page, and passes with `rem` in an `@media` condition
- [x] 7.5 Coverage script: `describe.skip`, missing `frontend/e2e` and the `pipefail` false "MISSING"; verify each with a temporary edit or a scratch copy

## 8. Second owner review

- [x] 8.1 (Replaced by 9.1.) `frontend` depends on `backend: service_started`; verify that with the backend stopped or never healthy, `/` loads and shows "unreachable" and `/healthz` answers 200, and that `docker compose up --wait` still waits for the backend
- [x] 8.2 Caddy listens on `:8081`, compose maps `127.0.0.1:8081:8081`, healthcheck follows, `cap_add` removed; verify the server really runs and binds with `cap_drop: [ALL]` and `no-new-privileges`
- [x] 8.3 Pin the `app` user's UID and GID in `frontend/Dockerfile`, matching the tmpfs mounts; verify no permission errors in the frontend logs
- [x] 8.4 Playwright global setup also checks `/api/me`; verify a stopped backend gives the clear message, and that the full suite passes on a healthy stack
- [x] 8.5 Replace the Perl parser in `scripts/check-scenario-coverage.sh` with the plain text match from `design.md`; verify on a scratch copy that a missing ID is reported, that a changed title is reported, that the three reported parser bugs no longer break it, and that the real tree reports all covered

## 9. Third owner review

- [x] 9.1 (The recovery part is replaced by 12.4.) Remove the frontend's `depends_on`; verify with Postgres unhealthy that the frontend is created, `/` shows "unreachable" and `/healthz` answers, and that the page recovers once the backend is up
- [x] 9.2 `[web-client/R8-S1]` allows only the 401 on `/api/me`; verify a broken CSS file makes it fail
- [x] 9.3 `[web-client/R5-S3]` uses the empty-body login from the spec; verify it passes with `ADMIN_EMAIL` overridden
- [x] 9.4 API client: an abort during the body read is rethrown; verify with a unit test
- [x] 9.5 Token check: close the declaration gap after a nested block and fix the reported line number; verify on a temporary violation of each

## 10. Fourth owner review

- [x] 10.1 API client: a failure while reading a problem body sets `isProblem` to false, so the page shows "unreachable"; verify with unit tests on `isProblem` and on the page status
- [x] 10.2 Correct the coverage script comment: the script stays, as constitution §2 says CI runs it, and only its frontend match is replaced under #7; verify the comment matches the constitution

## 11. Fourth owner review: remaining findings

- [x] 11.1 (Polling replaced by 12.4.) Status check keeps polling while reachable, needs JSON on a 2xx, and cancels on unmount; verify with `[web-client/R3-S2]` extended for the HTML case, `[web-client/R3-S4]`, and a test that unmount aborts the request
- [x] 11.2 API client recognises a deliberate cancel by the caller's signal, with any reason, and still treats a timeout as a failure; verify with unit tests on both the fetch and the body-read paths
- [x] 11.3 Share the `Accept` constant between the client and `[web-client/R5-S3]`; verify changing the constant reaches the test
- [x] 11.4 Token check: syntax-tree inline style check, exact font `var()`, no custom properties outside `tokens.css`; verify `const style = getComputedStyle(el)` passes, and `createElement('div', { style })`, `font-family: var(--font-body), Arial` and `--my-font: Arial` fail
- [x] 11.5 Coverage script: frontend capability list; verify on a scratch copy that a backend ID in a frontend test is MISSING, and the real tree reports all covered
- [x] 11.6 Add the favicon, its link and the `/robots.txt` and `/favicon.ico` 404s; verify with `[web-client/R1-S3]` extended, and `[web-client/R8-S1]` still passing

## 12. Fifth owner review

- [x] 12.1 Status check reads the 2xx body inside the timeout; verify with `[web-client/R3-S2]` extended for a dropped body and a hung body
- [x] 12.2 Status check uses `credentials: 'omit'`; verify with `[web-client/R3-S3]` in E2E, in all projects
- [x] 12.3 API client cancels the body of a non-problem error response; verify with a unit test
- [x] 12.4 Replace timer polling (tasks 7.1 and 11.1) with a check on load, on `visibilitychange` to visible and on `online`; verify with `[web-client/R3-S4]`, a test for `online`, and a test that no request is sent on a timer
- [x] 12.5 A `visibilitychange` or `online` event during a running check causes one more check when it ends; verify with `[web-client/R3-S4]` or an untagged test where the first check fails and the second succeeds, plus tests that fail if the request timer is not cleared or a non-JSON 2xx body is not discarded

## Workflow follow-up

- Reviewer (Sonnet) reviews each implementation slice; one Opus review before asking for the code gate.
- Code review gate: a reviewer approves the code in the PR, with tests and code reviewed together. Post the commit range.
- Archive the change in the same PR: merge the spec delta into `openspec/specs/`, after confirming with `scripts/check-scenario-coverage.sh` that every scenario ID has a test.
- Mark the PR ready for the owner's review.
