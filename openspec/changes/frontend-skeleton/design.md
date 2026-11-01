# Design

## Context

See `proposal.md` for motivation. Current state:

- `compose.yaml` includes `infra/docker-compose.yml` and `infra/docker-compose.dev.yml`
  with `infra/dev.env`. The base file publishes no ports; the dev file publishes them
  on loopback only.
- The backend listens on 8080 and has no context path. It uses
  `CookieCsrfTokenRepository.withHttpOnlyFalse()` in SPA mode, so the cookie is
  `XSRF-TOKEN` and the header is `X-XSRF-TOKEN`. No path is exempt from CSRF, including
  login.
- Errors are RFC 9457 problems with a string `code` property. Codes are string literals
  in the backend, with no central list.
- Anonymous `GET /api/me` returns a 401 problem and sets the CSRF cookie.
- `.gitignore` already covers `node_modules/` and `dist/`.

## Goals / Non-Goals

**Goals:**

- Screen PRs add only screens, translations and tests, with no setup work.
- The look of the whole app can be changed without editing pages.
- `docker compose up` gives the same serving shape as production: static files and
  `/api/*` proxied on one origin.

**Non-Goals:**

- UI component library, global state or data-fetching library: added when a screen
  needs one, with its own reason.
- Loading the CSRF token before the first POST, or refreshing it after login and
  logout: `ui-auth` (#32) needs this and adds it. The client reads the cookie at send
  time, so it will work with that flow.

## Decisions

### Layout

```
frontend/
  src/
    api/      client and problem handling
    i18n/     setup and en.json
    pages/    placeholder and not-found
    styles/   tokens.css (design values) and base.css
    ui/       shared visual components
  e2e/        Playwright tests
  Dockerfile
infra/Caddyfile
```

### Dependencies

All are mainstream and chosen with the owner (see Clarifications).

| Dependency | Reason |
| --- | --- |
| `react`, `react-dom` | Required by constitution §3 |
| `vite`, `@vitejs/plugin-react`, `typescript` | Required by constitution §3; build, dev proxy and types |
| `react-router` | Client routing. Every later screen needs it and it is the standard choice |
| `i18next`, `react-i18next` | Constitution §9 requires i18n. The usual React choice, and plurals and interpolation are ready for later screens |
| `vitest`, `jsdom`, `@testing-library/react` | Constitution §2 requires Vitest. The other two render pages for R2 and R3 without a browser |
| `@playwright/test` | Constitution §2 requires Playwright |
| `eslint`, `typescript-eslint`, `eslint-plugin-react-hooks` | Linting for TypeScript and React hook rules |
| `prettier`, `eslint-config-prettier` | Formatting; the config stops ESLint and Prettier from conflicting |
| `@types/react`, `@types/react-dom` | TypeScript types for React (dev only) |
| `@testing-library/dom` | Required peer dependency of `@testing-library/react` (dev only) |
| `@eslint/js` | ESLint's recommended rule set (dev only) |

TypeScript stays on 6.0.x, because `typescript-eslint` does not support 7 yet.

Plain `fetch` is used for HTTP. Alternative considered: axios has built-in XSRF
handling, but that saves only a few lines and adds a dependency.

npm is the package manager, with a committed `package-lock.json`. It needs no extra
tool in Docker or CI.

### API client

A small wrapper around `fetch`, with paths relative to `/api`.

- On POST, PUT, PATCH and DELETE it reads `XSRF-TOKEN` from `document.cookie` at send
  time and sets `X-XSRF-TOKEN`.
- A non-2xx response throws an `ApiError` holding the status, the `code` if present, the
  translated message and `isProblem`, which the reachability check uses.
- It sends `Accept: application/problem+json, application/json`, so the error format it
  depends on is explicit.
- A network failure or timeout throws `ApiError` with no status, with the generic message,
  and keeps the original error as `cause`. A deliberate cancel by the caller is rethrown
  as is.
- A response counts as a problem only when its content type is
  `application/problem+json`. The reachability check needs this, so that a 502 from
  Caddy is not taken for a backend answer.

### Translations

- One namespace, in `src/i18n/en.json`.
- Problem codes use the key `errors.<CODE>`. A missing key falls back to
  `errors.generic`.
- This change adds messages for all codes that exist in the backend at implementation
  time. The implementer makes the list by searching the backend, since there is no
  central list.

### Mobile browsers

- `index.html` has the standard viewport meta tag.
- Styles are mobile-first: the default styles are for a narrow screen, and `min-width`
  media queries add to them for wider ones. Text and spacing use relative units.
- Alternative considered: a native app, or a wrapper around the web app. Rejected: the
  goal is mobile browsers, and a native app would be a separate codebase and release
  process.

### Visual design is easy to change

The first look will be simple and is expected to change, possibly by a designer later. So
the look stays in two places, and pages do not own it:

- **Tokens:** `src/styles/tokens.css` holds every colour, font, text size, spacing, radius
  and shadow as a CSS custom property. Changing the look starts here.
- **Shared components:** `src/ui/` holds the visual building blocks, each with its own CSS
  Module. Pages use these and do not define their own appearance. This change adds only
  what its two pages need; screen PRs add the rest (buttons, forms and so on).
- **Enforced:** `npm run lint` also runs a small script that fails on:
  - a raw colour, font, `px` or `rem` value in CSS outside `tokens.css`, except in `@media`
    conditions, where `var()` does not work;
  - an inline `style=` attribute;
  - a CSS import or a `className` in `src/pages/`.

No new dependency: Vite supports CSS Modules and custom properties work in browsers.

Alternative considered: a ready-made component library (such as Mantine or MUI) would look
decent with no design work, but this change has no controls to use it on. `ui-auth` (#32)
adds the first forms and can make that choice. If it takes a library, the library goes
behind `src/ui/`, so pages do not depend on it directly.

### Reachability check

The placeholder page calls `GET /api/me` on load and shows "checking", then "reachable"
or "unreachable". The request times out after 10 seconds and then counts as a network
failure, so a hung backend shows "unreachable". While unreachable, the page tries again
every 5 seconds and stops once reachable. A 401 problem is the normal anonymous result. This also sets the CSRF
cookie. Alternatives were rejected with the owner: a new health endpoint needs backend
work, and proxying actuator would expose it.

### Serving

- `frontend/Dockerfile` is multi-stage: Node builds, a pinned `caddy:2.x-alpine` image serves. Its build
  context is the repo root, because it copies both `frontend/` and `infra/Caddyfile`. A
  `frontend/Dockerfile.dockerignore` keeps `backend/`, `node_modules/` and other
  unrelated paths out, without affecting the backend build.
- The `Caddyfile` listens on `:80`, with the admin API off, since the config never changes
  at runtime. Routes:
  - `/healthz` answers 200 (R10).
  - `/api/*` proxies to `{$BACKEND_UPSTREAM:backend:8080}`. Compose passes the variable and
    `.env.example` lists it.
  - `/assets/*` is a file server with a one-year immutable `Cache-Control` and no fallback, so
    a missing file is a real 404 (R1-S3). A cached old page then fails visibly.
  - Everything else is a file server with `try_files {path} /index.html` and
    `Cache-Control: no-cache`, which gives R1-S2 and also R6-S2, because `/actuator/health`
    returns the app, not health data.
  - Responses are compressed with `encode zstd gzip`. Static routes share the security
    headers in a snippet and drop the `Server` header. `/api` is left to Spring.
- The Content Security Policy is `default-src 'self'; object-src 'none'; base-uri 'self';
  form-action 'self'; frame-ancestors 'none'`. A later screen that needs more (for example
  Excalidraw or video) widens it in its own PR, with its reason.
- `frontend` in compose runs hardened: `read_only`, tmpfs for `/data` and `/config`,
  `cap_drop: [ALL]`, `cap_add: [NET_BIND_SERVICE]`, `no-new-privileges`, and
  `restart: unless-stopped`. If `no-new-privileges` stops the non-root binary from binding
  port 80, the container listens on an unprivileged port instead, with compose mapping to it.
- Healthchecks: `frontend` uses `/healthz` with the image's `wget`. `backend` uses
  `/actuator/health`. The backend image has neither `wget` nor `curl`, so its Dockerfile
  installs `curl`; this is the only backend change. `frontend` waits for
  `backend: service_healthy`, and `backend` already waits for `postgres`. So
  `docker compose up --wait` also waits for the backend, and the Playwright global setup no
  longer needs to poll.
- A new `frontend` service goes in `infra/docker-compose.yml`, with no ports. The dev file
  publishes `127.0.0.1:8081:80`. Port 80 is avoided because it often needs extra rights or
  is already in use.
- `vite.config.ts` proxies `^/api/` (so `/api-keys` is not matched, as in Caddy) to
  `http://localhost:8080` on port 5173, which is the
  origin `APP_BASE_URL` already uses in `infra/dev.env`.
- The Node version is pinned in `frontend/.nvmrc` and the Dockerfile, to the current LTS.

### Tests

- Each scenario has one test, and its title starts with the ID:
  `test("[web-client/R3-S2] Backend cannot be reached", ...)`.
- Vitest covers R2, R3-S2, R4 and R5, with `fetch` mocked.
- Playwright covers R1, R3-S1, R6 and R7 in one file, `e2e/skeleton.spec.ts`, against
  `http://localhost:8081`. This is the "one E2E" from the issue: one flow file, with one
  test per scenario as constitution §2 asks.
- Playwright has three projects: desktop Chromium, and the built-in phone profiles for
  Chrome (Pixel) and Safari (iPhone, using WebKit). The whole E2E file runs in all three.
  R7-S1 is skipped on desktop.
- A Playwright global setup checks that the stack responds. If not, it stops with a
  message telling the user to run `docker compose up --build`.
- `scripts/check-scenario-coverage.sh` accepts an ID if it finds the backend
  `@DisplayName("[id]` form, or a `test(` or `it(` call whose title starts with `[id]` in
  `frontend/src/**/*.test.ts(x)` or `frontend/e2e/**/*.spec.ts`. Comments, `test.skip(`
  calls and tests inside a `describe.skip` block do not count. The title may be on the line
  after the call, as Prettier can put it there. A missing test directory is not an error by
  itself, and the check must not give a false "MISSING" with a long ID list.
- Playwright's global setup reads the base URL from its config, so it is set in one place.

### Commands

`npm run dev`, `build`, `lint`, `format`, `format:check`, `test`, `test:e2e`.

### Flyway migrations

None.

## Risks / Trade-offs

- [The CSRF header is tested only with a mocked `fetch`, so a wrong name would not fail
  here] → The name is Spring's default and is written in the spec. `ui-auth` (#32) sends
  the first real POST, and its E2E covers login end to end.
- [Phone profiles are emulation, so real-device problems (for example iOS Safari address
  bar behaviour) are not caught] → Enough for a placeholder page. Check real phones when
  the first real screens land.
- [`/actuator/health` returns the app with a 200 through Caddy, which could confuse someone
  checking health through it] → The README names the direct backend URL for health.
- [The Vite dev server and the container use different ports] → The README says which one to
  use for what. Links the backend generates use `APP_BASE_URL` (5173), but no screen follows
  them until #32.
- [Backend and frontend error codes can drift, as there is no shared list] → An untranslated
  code shows the generic message, so the failure is mild. A shared list is not worth it now.

## Open Questions

- Should `APP_BASE_URL` in `infra/dev.env` point to the container (8081) rather than the dev
  server (5173)? Nothing follows these links until `ui-auth` (#32), which can decide.
