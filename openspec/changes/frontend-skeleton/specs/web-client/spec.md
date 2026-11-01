# Spec Delta

## Purpose

The browser application shell and how it talks to the backend: routing, translated UI
strings, the API client and same-origin serving. Product screens build on it.

## ADDED Requirements

### Requirement: R1 Application pages

The web client SHALL serve a placeholder page at the root path and SHALL show a
not-found page for any other path, including when that path is opened directly. A missing
file under `/assets/` SHALL be a real 404, so a stale page fails visibly instead of
loading HTML as a script.

#### Scenario: web-client/R1-S1 Root path shows the placeholder page
- **GIVEN** the local stack is running
- **WHEN** a user opens `/`
- **THEN** the placeholder page is shown with the application name

#### Scenario: web-client/R1-S2 Unknown path opened directly shows the not-found page
- **GIVEN** the local stack is running
- **WHEN** a user opens `/no-such-page` directly
- **THEN** the response is the web application, not a server error page
- **AND** the not-found page is shown

#### Scenario: web-client/R1-S3 Missing asset is a real 404
- **GIVEN** the local stack is running
- **WHEN** a client requests `/assets/no-such-file.js`
- **THEN** the response status is 404
- **AND** the response is not the web application's HTML

### Requirement: R2 Translated UI strings

Every string the web client shows to a user SHALL come from the translation resources.
English is the only UI language for now.

#### Scenario: web-client/R2-S1 Page text comes from the translation resources
- **GIVEN** the translation resources are replaced with test values
- **WHEN** the placeholder page and the not-found page are rendered
- **THEN** each page shows the test values
- **AND** no English text from the real resources is shown

### Requirement: R3 Backend reachability status

The placeholder page SHALL show whether the backend can be reached. A successful
response or a problem response from the backend means reachable. A network failure or
any other response means unreachable. While unreachable, the page SHALL keep checking, so
it recovers without a reload.

#### Scenario: web-client/R3-S1 Anonymous user sees that the backend is reachable
- **GIVEN** the local stack is running and the user is not logged in
- **WHEN** a user opens `/`
- **THEN** the page shows the translated "reachable" status

#### Scenario: web-client/R3-S2 Backend cannot be reached
- **GIVEN** the status request fails with a network error, or returns a 502 that is not a problem response
- **WHEN** the placeholder page is rendered
- **THEN** the page shows the translated "unreachable" status

#### Scenario: web-client/R3-S3 Page recovers when the backend comes back
- **GIVEN** the placeholder page shows the "unreachable" status
- **WHEN** a later status request gets a problem response
- **THEN** the page shows the translated "reachable" status without a reload

### Requirement: R4 CSRF header

The API client SHALL send requests to the same-origin `/api` path. On a state-changing
request it SHALL send the value of the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header.

#### Scenario: web-client/R4-S1 State-changing request carries the CSRF token
- **GIVEN** the browser has an `XSRF-TOKEN` cookie with value `abc`
- **WHEN** the API client sends a POST to `auth/login`
- **THEN** the request goes to the same-origin path `/api/auth/login`
- **AND** the request has the header `X-XSRF-TOKEN: abc`

### Requirement: R5 Translated API errors

When a request fails, the API client SHALL give the caller a translated message chosen
by the problem `code`. A code without a translation, or a response that is not a problem
response, SHALL give a generic translated message.

#### Scenario: web-client/R5-S1 Known problem code gives its translated message
- **GIVEN** the backend answers with a problem response with code `WRONG_PASSWORD`
- **WHEN** the API client handles the response
- **THEN** the caller gets the translated message for `WRONG_PASSWORD`

#### Scenario: web-client/R5-S2 Unknown code or non-problem response gives the generic message
- **GIVEN** the response has a code without a translation, or is not a problem response
- **WHEN** the API client handles the response
- **THEN** the caller gets the generic translated error message

#### Scenario: web-client/R5-S3 Real backend error is recognised as a problem response
- **GIVEN** the local stack is running and the admin account exists
- **WHEN** a client sends a login with a wrong password and the headers the API client uses
- **THEN** the response has content type `application/problem+json` and code `WRONG_PASSWORD`

### Requirement: R6 Same-origin serving

The web container SHALL proxy `/api/*` to the backend on the same origin as the web
application, with no CORS, and SHALL NOT expose other backend paths.

#### Scenario: web-client/R6-S1 API request reaches the backend on the same origin
- **GIVEN** the local stack is running
- **WHEN** a client sends `GET /api/me` with an `Origin` header to the web container, anonymously
- **THEN** the response is the backend's 401 problem with code `UNAUTHENTICATED`
- **AND** the response has no `Access-Control-Allow-Origin` header

#### Scenario: web-client/R6-S2 Actuator is not exposed through the web container
- **GIVEN** the local stack is running
- **WHEN** a client sends `GET /actuator/health` to the web container
- **THEN** the response does not contain the backend's health data

### Requirement: R7 Mobile browsers

Pages SHALL fit the screen in mobile browsers, down to a width of 360 CSS pixels, without
horizontal scrolling and without the browser shrinking the page to fit.

#### Scenario: web-client/R7-S1 Pages fit a phone screen
- **GIVEN** the local stack is running and the browser has a phone-sized screen
- **WHEN** a user opens the placeholder page and the not-found page
- **THEN** each page has no horizontal scrolling
- **AND** the page is laid out at the phone's own width, not zoomed out

### Requirement: R8 Security headers

Web application responses SHALL carry a Content Security Policy, `X-Content-Type-Options`
and `Referrer-Policy` headers, and SHALL NOT allow other sites to frame the application.
The application SHALL work under that policy.

#### Scenario: web-client/R8-S1 Page is served with security headers and works under them
- **GIVEN** the local stack is running
- **WHEN** a user opens `/`
- **THEN** the response has a Content Security Policy with `default-src 'self'` and `frame-ancestors 'none'`
- **AND** the response has `X-Content-Type-Options: nosniff` and a `Referrer-Policy` header
- **AND** the page renders with no Content Security Policy violations

### Requirement: R9 Caching and compression

Hashed assets SHALL be cached for a long time and served compressed. The HTML page SHALL be
revalidated on every load, so a new deploy reaches users.

#### Scenario: web-client/R9-S1 Hashed asset is long-lived and compressed
- **GIVEN** the local stack is running
- **WHEN** a client that accepts compression requests a script file under `/assets/`
- **THEN** the response has `Cache-Control` with `immutable` and a one-year `max-age`
- **AND** the response is compressed

#### Scenario: web-client/R9-S2 HTML page is revalidated
- **GIVEN** the local stack is running
- **WHEN** a client requests `/`
- **THEN** the response has `Cache-Control: no-cache`

### Requirement: R10 Container health endpoint

The web container SHALL answer `/healthz` with a 200 without depending on the backend, so
orchestration can check the container itself.

#### Scenario: web-client/R10-S1 Health endpoint answers without the application
- **GIVEN** the local stack is running
- **WHEN** a client requests `/healthz`
- **THEN** the response status is 200
- **AND** the response is not the web application's HTML
