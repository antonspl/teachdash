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
- A non-problem error response has its body cancelled, so a slow body cannot hold a
  connection and block later requests.
- A network failure or timeout throws `ApiError` with no status, with the generic message,
  and keeps the original error as `cause`. A deliberate cancel by the caller is rethrown
  as is, whatever its reason: the client checks that the caller's signal is aborted with a
  reason other than `TimeoutError`, not the error's name.
- The `Accept` value is a constant in a module with no other imports, so the E2E test uses the
  same value as the client.
- A response counts as a problem only when its content type is
  `application/problem+json`. The reachability check needs this, so that a 502 from
  Caddy is not taken for a backend answer. If the connection fails while reading the body,
  it does not count as a problem response, because the backend has not really answered. A
  complete body that is not valid JSON still counts.

### Translations

- One namespace, in `src/i18n/en.json`.
- Problem codes use the key `errors.<CODE>`. A missing key falls back to
  `errors.generic`.
- This change adds messages for all codes that exist in the backend at implementation
  time and that the backend names itself. The implementer makes the list by searching the
  backend, since there is no central list. Framework errors use HTTP status names as codes,
  such as `METHOD_NOT_ALLOWED`. That list is open-ended and means nothing to a user, so they
  get the generic message.

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
    conditions, where `var()` does not work. A font value must be exactly one `var(...)`;
  - a custom property defined in CSS outside `tokens.css`, which would hide raw values;
  - an inline style: a JSX `style` attribute or a `style` property passed to `createElement`,
    found through the TypeScript syntax tree rather than a regex, as TypeScript is already a
    dependency;
  - a CSS import or a `className` in `src/pages/`, except in test files.

  It scans `.ts` and `.tsx` files. Units other than `px` and `rem` are not checked.

No new dependency: Vite supports CSS Modules and custom properties work in browsers.

Alternative considered: a ready-made component library (such as Mantine or MUI) would look
decent with no design work, but this change has no controls to use it on. `ui-auth` (#32)
adds the first forms and can make that choice. If it takes a library, the library goes
behind `src/ui/`, so pages do not depend on it directly.

### Reachability check

The placeholder page calls `GET /api/me` on load and shows "checking", then "reachable"
or "unreachable". The request times out after 10 seconds and then counts as a network
failure, so a hung backend shows "unreachable". The page checks once on load and again
on `visibilitychange` to visible and on `online`, one request at a time, with no timer
polling. An event during a running check causes one more check when it ends. Unmounting cancels the running request. A 2xx response counts only if its content type is JSON, so a
captive portal page is not taken for the backend. A 401 problem is the normal anonymous result.

The 10-second timeout also covers reading the body, so a body that drops or hangs means
"unreachable". Checks use `credentials: 'omit'`, because a session cookie on a check would
refresh its last-access time and add a users lookup. As a result the browser ignores the
response's cookies, and this page does not set the CSRF cookie; `ui-auth` (#32) must load it
itself.

Timer polling was tried and removed. It kept sessions alive, and it produced most of the review
findings on this change, for a page that real screens will replace. The trade-off: while the tab
stays visible, the page does not notice the backend going down or coming back until a reload,
and a page opened before the backend is up stays "unreachable" until then.

Alternatives were rejected with the owner: a new health endpoint needs backend
work, and proxying actuator would expose it.

### Serving

- `frontend/Dockerfile` is multi-stage: Node builds, a pinned `caddy:2.x-alpine` image serves. Its build
  context is the repo root, because it copies both `frontend/` and `infra/Caddyfile`. A
  `frontend/Dockerfile.dockerignore` keeps `backend/`, `node_modules/` and other
  unrelated paths out, without affecting the backend build.
- The `Caddyfile` listens on `:8081`, an unprivileged port, with the admin API off, since the config never changes
  at runtime. Routes:
  - `/healthz` answers 200 (R10).
  - `/api/*` proxies to `{$BACKEND_UPSTREAM:backend:8080}`. Compose passes the variable and
    `.env.example` lists it.
  - `/assets/*` is a file server with a one-year immutable `Cache-Control` and no fallback, so
    a missing file is a real 404 (R1-S3). A cached old page then fails visibly.
  - `/robots.txt` and `/favicon.ico` are explicit 404s (R1-S3). The icon is a small
    self-hosted file linked from `index.html`; a `data:` icon would break the CSP.
  - Everything else is a file server with `try_files {path} /index.html` and
    `Cache-Control: no-cache`, which gives R1-S2 and also R6-S2, because `/actuator/health`
    returns the app, not health data.
  - Responses are compressed with `encode zstd gzip`. Static routes share the security
    headers in a snippet and drop the `Server` header. `/api` is left to Spring.
  - A `handle_errors` block for paths outside `/api/*` applies the security headers to static
    errors, with `Cache-Control: no-store`, so a 404 is never cached as immutable. API and
    proxy errors pass through unchanged. The route headers also use `defer`; no test depends
    on it, because `handle_errors` already covers the 404 case.
- The Content Security Policy is `default-src 'self'; object-src 'none'; base-uri 'self';
  form-action 'self'; frame-ancestors 'none'`. A later screen that needs more (for example
  Excalidraw or video) widens it in its own PR, with its reason.
- `frontend` in compose runs hardened: `read_only`, tmpfs for `/data` and `/config`,
  `cap_drop: [ALL]`, `no-new-privileges`, and `restart: unless-stopped`. It needs no added
  capability, because it does not bind a low port. The Dockerfile removes the file capability
  from the Caddy binary, because with it the binary fails to start under these settings. It pins the `app` user's UID and
  GID, because the tmpfs mounts name them, so an image bump cannot make them unwritable.
- Healthchecks: `frontend` uses `/healthz` with the image's `wget`. `backend` uses
  `/actuator/health`. The backend image has neither `wget` nor `curl`, so its Dockerfile
  installs `curl`; this is the only backend change. `frontend` has no
  `depends_on`, because Caddy finds the backend per request. So a failed migration, a slow
  backend or an unhealthy Postgres leaves the page up, showing "unreachable", and `/healthz`
  answering. `docker compose up --wait` still waits for every
  healthcheck, including the backend's, so the E2E step uses it. The Playwright global setup
  checks both the web container and `/api/me`, without polling, so a stopped backend gives the
  same clear message as a stopped stack.
- A new `frontend` service goes in `infra/docker-compose.yml`, with no ports. The dev file
  publishes `127.0.0.1:8081:8081`. Port 80 is avoided on the host because it often needs extra
  rights or is already in use, and in the container because it needs a capability.
- `vite.config.ts` proxies `^/api/` (so `/api-keys` is not matched, as in Caddy) to
  `http://localhost:8080` on port 5173, which is the
  origin `APP_BASE_URL` already uses in `infra/dev.env`.
- The Node version is pinned in `frontend/.nvmrc` and the Dockerfile, to the current LTS.

### Tests

- Each scenario has at least one test, and each title starts with the ID:
  `test("[web-client/R3-S2] Backend cannot be reached", ...)`.
- Vitest covers R2, R3-S2, R3-S4, R4, R5-S1 and R5-S2, with `fetch` mocked.
- Playwright covers R1, R3-S1, R3-S3, R5-S3, R6, R7, R8, R9 and R10 in one file, `e2e/skeleton.spec.ts`, against
  `http://localhost:8081`. This is the "one E2E" from the issue: one flow file, with one
  test per scenario as constitution §2 asks.
- Playwright has three projects: desktop Chromium, and the built-in phone profiles for
  Chrome (Pixel) and Safari (iPhone, using WebKit). The whole E2E file runs in all three.
  R7-S1 is skipped on desktop.
- A Playwright global setup checks that the stack responds. If not, it stops with a
  message telling the user to run `docker compose up --build --wait`.
- `scripts/check-scenario-coverage.sh` accepts an ID if it finds the backend
  `@DisplayName("[id]` form, or a string starting with `[id]` in
  `frontend/src/**/*.test.ts(x)` or `frontend/e2e/**/*.spec.ts`. A list at the top of the
  script names the frontend capabilities. Their IDs are looked up only in frontend tests, and
  all others only in backend tests, so a backend scenario cannot pass on a frontend test. A new
  frontend capability missing from the list shows up as MISSING. It is a plain text match,
  not a parser, so a skipped or commented-out test still counts until CI (#7) replaces this
  with `vitest list --json` and `playwright test --list`. Reviewers watch for skips until
  then. An earlier version parsed the test files in Perl and kept hitting new edge cases.
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
