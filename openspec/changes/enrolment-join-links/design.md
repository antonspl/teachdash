# Design: enrolment-join-links

## Context

See proposal.md (Why, Research). Relevant current code:

- `PasswordLinkService` generates tokens (32 random bytes, base64url) and stores a SHA-256
  hash in `one_time_tokens`. That table allows one unused token per user, and password
  flows void all of a user's tokens, so join links cannot live there.
- `StudentRegistrationService.register` locks the active teacher row
  (`findActiveByIdForUpdate`), reads the email without a lock, then locks an existing
  student's row. `teacher_students` has a unique index on the student.
- `PasswordLinkController` starts a session after a password is set, through
  `SessionStarter` (package-private in `accounts.api`).
- `TeacherAdminService.deactivateTeacher` deactivates the teacher, voids their links and
  publishes `TeacherDeactivated`; `TeacherDeactivationListener` releases the students in
  the same transaction.
- `SecurityConfig` permits `POST /api/auth/login` and `/api/password-links` without a
  session. CSRF (`csrf.spa()`, cookie repository) applies to every POST.

## Goals / Non-Goals

**Goals:** reuse the registration validators, lock order and session start; keep every
join safe against parallel joins, deactivation and link changes; no new dependency.

**Non-Goals:** a general rate limiting framework; limiting login or password links.

## Decisions

### D1. New table `teacher_join_links`, token in plain text

```
teacher_join_links(
  teacher_id  uuid primary key references users(id) on delete cascade,
  token       varchar(64) not null unique,
  created_at  timestamptz not null
)
```

One row per teacher with an active link. Generating upserts the row with a new token;
disabling deletes it. Deactivation deletes it, so reactivation has no link (R8-S5).

The token is stored in plain text because the teacher must be able to view the link at
any time (proposal, Q8). A link only lets someone create a student under that teacher, so
a database leak exposes far more than these links. Alternative: store a hash and show the
link only when generated; rejected because "view" would not work.

Token generation reuses 32 random bytes in base64url (43 characters). The generator is
extracted from `PasswordLinkService` into a small shared class in `common`, so both use
the same code.

Link format: `APP_BASE_URL + "/join#token=" + token`, like set-password links, so the
token stays out of server and proxy logs.

### D2. Endpoints

Teacher (role teacher, under `/api/teacher/**`):

- `GET /api/teacher/join-link` → 200 `{ "link": "<url>" | null }`
- `POST /api/teacher/join-link` → 200 `{ "link": "<url>" }` (generates or replaces)
- `DELETE /api/teacher/join-link` → 204, also when there is no link

Public (no session needed, CSRF required):

- `POST /api/join-links/preview` with `{ "token" }` → 200 `{ "teacherName" }`
- `POST /api/join-links/join` with `{ "token", "name", "email", "timeZone", "password" }`
  - no session → 201 `{ "studentId" }` and a new session (R9);
  - student session → 200 `{ "teacherId" }`, only the token is used (R10);
  - teacher or admin session → 403.

The preview is a POST so the token stays in the body, not in the URL or access logs, as
for `/api/password-links`. Alternative: `GET /api/join-links/{token}`; rejected for that
reason.

Request validation: the token is `@NotBlank @Size(max = 64)`. For a visitor, name, email
and time zone use `@PersonName`, `@EmailAddress`, `@TimeZoneId` and the same
normalisation as `RegisterStudentRequest`, and the password uses `@BcryptPassword`. A
student session needs only the token, so the join body is read as a token record, and
for a visitor the controller maps it to a separate visitor record and validates that with
`jakarta.validation.Validator`. Both paths answer 400 `VALIDATION_FAILED`. The preview
validates its token the same way.

The controller lives in `enrolment.api`, and `SessionStarter` becomes public so it can
start the visitor's session. Alternative: put the controller in `accounts.api`, next to
`SessionStarter`; rejected because `accounts` would then depend on `enrolment`, which
already depends on `accounts`.

### D3. Order of checks and locks

Before any lookup: a teacher or admin session → 403, then input validation → 400
(student-enrolment R9). A request whose session is no longer valid (logged out, or the
account deactivated) has no principal after `SessionRevocationFilter`, so it is treated
as a visitor, and `SessionStarter` replaces that session on success.

Join, in one transaction, after the password is hashed outside it (as `AccountService`
does, so no connection is held during bcrypt). Hashing before the link check costs a
bcrypt for every invalid link; the rate limiter (D5) bounds that cost.

1. Find the link by token. Not found → 410 `LINK_INVALID`.
2. Lock the teacher row with `findActiveByIdForUpdate`. Not found (deactivated) → 410
   `LINK_INVALID`.
3. Re-read the link under the teacher lock; if the token changed or the row is gone →
   410 `LINK_INVALID`.
4. Visitor: insert the user with the password hash and no one-time token
   (`DuplicateKeyException` → 409 `EMAIL_TAKEN`), then the assignment. A new
   `AccountService` method creates a user with a password; `createPasswordlessUser` is not
   used, because it issues a set-password link. Student: lock the student row, check the assignment (409
   `ALREADY_ENROLLED` or `STUDENT_EXISTS`), insert it; a lost insert race is mapped the
   same way, as in registration.
5. Visitor only: after commit, start the session with `SessionStarter`.

Generate locks the teacher row with `findActiveByIdForUpdate` and answers 401
`UNAUTHENTICATED` when the teacher is no longer active, as registration does.
Otherwise a generate that passed the session check just before a deactivation could
create a link after the deactivation deleted it, and reactivation would revive it.
Disable locks the row with `findByIdForUpdate`; deleting a link is safe either way.
Deactivation already updates the teacher row first. So link changes, deactivation and
joins all serialise on the teacher row, and the lock order stays teacher → student →
links everywhere.

Checking the link before the email means only a holder of a valid link learns that an
email is registered (R9-S4).

Like registration, the race between a join and a deactivation cannot be timed in a test.
The lock is explained in a code comment and checked at code review, as agreed for
registration in `enrolment-single-teacher`.

### D4. Deactivation disables the link

`TeacherDeactivationListener` also deletes the teacher's row in `teacher_join_links`, in
the same transaction as the release of students and before it, so a failure while the
students are released also rolls back the link delete (R8-S9). Alternative: keep the row and refuse it
while the teacher is inactive; rejected because reactivation would silently revive an old
link (proposal, Q11).

### D5. Rate limiter

A `OncePerRequestFilter` in `common`, registered before the Spring Security filter chain,
so a limited request gets 429 before CSRF or session checks. It matches only
`POST /api/join-links/preview` and `POST /api/join-links/join`, each with its own
counter.

- Match: the exact paths, compared on `getRequestURI()`. Non-normalised variants
  (`/api/join-links/./join`) are not matched, and Spring Security's firewall rejects them.
- Key: `request.getRemoteAddr()`. Tomcat's `RemoteIpValve`
  (`server.forward-headers-strategy=native`) has already replaced it with the client IP
  from `X-Forwarded-For` when the direct peer is a trusted proxy. The filter never reads
  the header itself.
- Algorithm: fixed window per key, in a `ConcurrentHashMap<String, Window>`. A window
  stores its start and count. Every request counts, whatever its response (R11).
  Expired windows are removed on access and by a sweep when the map reaches a size cap
  (default 100 000 keys). If the map is still full after the sweep, a request from a new
  key gets 429 (fail closed), so rotating addresses cannot grow memory.
- `Retry-After` is computed from `Instant`s and clamped to 1..window seconds, so a clock
  moved backwards in tests cannot produce an out-of-range value.
- Time comes from the shared `Clock`, so R11-S3 moves the test clock instead of waiting.
- Response: 429, `Retry-After` = seconds until the window ends (rounded up, at least 1),
  ProblemJson with code `RATE_LIMITED`.
- Properties (defaults): `teachdash.rate-limit.join.max=30`,
  `teachdash.rate-limit.join.window=10m`, `teachdash.rate-limit.preview.max=120`,
  `teachdash.rate-limit.preview.window=10m`. The shared test configuration sets very high limits,
  so tests outside R11 are never limited. The R11 tests run in their own context with low
  limits, and each test uses its own client addresses, so counters never carry over.
  Tests send `X-Forwarded-For` from the loopback peer to act as different IPs (R11-S2).
  R11 tests must build their GIVEN by calling the limited endpoints, because there is no
  other way to fill a counter; the endpoints are implemented by then in pass 2, and in
  pass 1 those calls get 501, which still count.

Alternatives: Bucket4j (a dependency and its Spring Boot 4.1 support for one endpoint);
a Caddy plugin (custom Caddy build, not testable by the backend tests). Both rejected
under constitution §7.

### D6. No new dependencies

Everything above uses Spring Boot, Spring Security and the JDK.

## Migrations

- `V5__teacher_join_links.sql`: creates `teacher_join_links` (D1).

## Risks / Trade-offs

- [The backend may not see the real client IP in prod. The repo's Caddy listens on plain
  HTTP `:8081`, and whatever sits in front of it is not in the repo.] → If it does not
  forward the client IP, all visitors share one counter, and one prober blocks all joins.
  After deploy, check the logged remote address of a join request; follow-up if wrong.
- [A link has no expiry.] → Accepted in #29. The teacher can regenerate or disable it.
- [Anyone can register any email through a valid link, without proving it is theirs.]
  → Accepted until #46.
- [A 409 `EMAIL_TAKEN` reveals that an email is registered.] → Accepted. Only holders of
  a valid link get it, and the limiter slows probing.
- [Fixed windows allow up to twice the limit across a window boundary.] → Good enough for
  probing defence.
- [Counters are in memory: a restart resets them, and they break with several backend
  instances.] → One instance on one VPS (constitution §6). Revisit if that changes.
- [A token in plain text can be read from a database dump.] → See D1. Tokens are never
  logged (constitution §4); the code review checks the 410, 429 and view paths.
- [A released student without a password gets 409 `EMAIL_TAKEN` at the join page.] →
  They cannot log in or set a password; a teacher registering their email (R1) sends
  them a new set-password link.
