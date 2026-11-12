# Design: enrolment-reset-links

## Context

See proposal.md - Why. Current state on main:

- `one_time_tokens` holds set-password and reset links: `user_id`, `purpose`, times. A
  partial unique index allows one unused link per user, and `PasswordLinkService.issue`
  marks every older unused link of the user used before it inserts the new one.
- `PasswordLinkService.issue(userId, purpose)` has four callers:
  `AccountService.createPasswordlessUser` (admin creates a teacher, teacher registers a
  new student), `TeacherAdminService` reactivation of a teacher without a password,
  `TeacherAdminService.issueResetLink` (admin reset) and
  `StudentRegistrationService.assignExisting`. Join (R9, R10) and bootstrap issue none.
- `TeacherAdminService.issueResetLink` locks the user row, checks own account and active,
  then calls `issue`, which locks the same row again. `issue` itself checks nothing.
- Removal (`StudentRemovalService`) and release (`TeacherDeactivationListener`) call
  `invalidateLinks(studentId, SET_PASSWORD)`. That is correct only while a teacher can
  hold nothing but set-password links. Removal takes no lock.
- Students cannot be deactivated (accounts R6 applies to teachers only).
- Error codes are string literals in `new ApiException(status, "CODE")`; the throw site
  sets the status.
- Lock order (archived `enrolment-single-teacher` design, D7, and `enrolment-join-links`
  D3): teacher row, then student row, then links. Registration, join-link changes and
  joins lock the teacher row with `findActiveByIdForUpdate`; deactivation updates the
  teacher row first.
- Controllers read the acting user's id from `Principal` and pass it to services. No
  domain code reads the security context.

## Goals / Non-Goals

**Goals:** one rule for what removal and release invalidate (links the teacher issued);
one guarded place that issues reset links; existing approved tests keep passing, apart
from the changes named in D5 and D7.

**Non-Goals:** exposing the issuer in any API; changing what a password change or a
reset does to other links; any frontend change.

## Decisions

### D1. `issued_by` column, in two migrations

`one_time_tokens.issued_by uuid references users (id) on delete cascade`, the account
whose request issued the link: the admin, or the teacher.

- `V6__token_issuer.sql` (pass 1): adds the column as nullable. Existing code keeps
  inserting links, so the suite stays green while the tests are written.
- `V7__token_issuer_required.sql` (pass 2): `delete from one_time_tokens where issued_by
  is null`, then `set not null`. Only test and local data exist (proposal, answer 2), so
  deleting pending links loses nothing. This is safe only because there are no real users;
  a later migration like it would need a backfill.

A single `not null` V6 in pass 1 was considered. It would make every existing issuing path
fail on insert until pass 2, so the existing suite would fail during the test gate.

One nullable column with no second migration was considered. It is simpler, but nothing
would stop a later issuing path from forgetting the issuer, and such a link would then
survive removal. `on delete cascade` matches `user_id`; accounts are not deleted today.

No index is added: invalidation filters by `user_id`, which is already indexed, and a user
has at most one unused link.

### D2. The issuer is an explicit argument

Every issuing method takes the issuer's id (proposal, answer 5):

- `PasswordLinkService.issue(UUID userId, TokenPurpose purpose, UUID issuerId)` replaces
  the two-argument method. It is used for set-password links only.
- `PasswordLinkService.issueResetLink(UUID userId, UUID issuerId)` (D6).
- `AccountService.createPasswordlessUser` gains an `issuerId` argument.
- `TeacherAdminService.createTeacher` and `reactivateTeacher` gain the acting admin's id,
  as `issueResetLink` already has.
- `OneTimeToken` stores `issuedBy`.

Issuers by path: the admin for teacher creation, reactivation of a teacher without a
password and the admin reset; the teacher for registration (new or existing student) and
the teacher reset. The id comes from `Principal` in the controller, as the admin reset and
the teacher endpoints already do; `AdminController` passes it for creation and
reactivation too.

Reading the issuer from the security context inside `issue` was considered. It keeps the
signatures, but it would be the first hidden security-context input in domain code, and a
missing issuer would fail only at runtime.

Cost: `StudentRegistrationFailureTest` stubs `links.issue(any(), any())` in two places.
Pass 1 cannot add the new signature (only stubs and migrations), so these two stubs change
in pass 2, with the engineer's approval at that point (D7).

### D3. Teacher reset endpoint and locking

`POST /api/teacher/students/{id}/reset-link` in `TeacherStudentController`, 201 with the
same `LinkResponse` body as the admin endpoint (the public record in `accounts.api.dto`
is imported; a copy in the enrolment package would be a second definition of one shape), purpose `RESET_PASSWORD` whether or not
the student has a password (as the admin endpoint does). Malformed ids get 404 through
the existing `MalformedIdAdvice`.

A new enrolment service method, in one transaction:

1. Lock the teacher row with `findActiveByIdForUpdate`; 401 `UNAUTHENTICATED` if the
   teacher is no longer active, as registration does.
2. Check that the student is assigned to this teacher; otherwise 404 `NOT_FOUND`.
3. `links.issueResetLink(studentId, teacherId)` (D6), which locks the student row, then
   the links.

**Race with removal.** Without a shared lock, a reset that read the assignment could insert
its link after a concurrent removal by the same teacher (two tabs) invalidated the links.
The link would survive the removal. Removal therefore also locks the teacher row first,
with `findActiveByIdForUpdate`, before it deletes the assignment. Deactivation already
updates the teacher row first, so a reset and a release serialise too.

Lock order by path:

| Path | Locks, in order |
|------|-----------------|
| Teacher reset | teacher row, student row, links |
| Removal | teacher row, links |
| Deactivation and release | teacher row, links |
| Admin reset | target user row, links |

The lock order stays teacher, student, links. Removal and the release listener update
links without locking the student row; they can wait on a concurrent `issue` or `use` but
never take a lock after that, so they cannot close a cycle (as before). An admin reset of
a student never takes a teacher lock; an admin reset of a teacher and that teacher's
deactivation serialise on the teacher row. An admin reset racing a removal is safe in
either order: removal voids only links with `issued_by` equal to the teacher.

A removal or a reset by a teacher deactivated after the session check returns 401
`UNAUTHENTICATED`, as registration does. It is reachable only in that race: deactivation
ends the teacher's sessions, so a later request gets 401 from the session check anyway
(accounts R6-S1). The specs therefore do not name it. No test can force this timing, as in
the earlier changes; a code comment at each lock explains it and the reviewer checks it.

Locking the assignment row (`select ... for update` on `teacher_students`) was considered.
It also works, but adds a second locking pattern next to the teacher-row lock every other
teacher-scoped write uses.

### D4. Invalidation by issuer

`PasswordLinkService.invalidateLinks(UUID userId, TokenPurpose purpose)` is replaced by
`invalidateLinks(UUID userId, UUID issuerId)`: mark used the user's unused links where
`issued_by = issuerId`, through a new query in `OneTimeTokenRepository`. Removal and
release call it with the teacher's id. The name and arity stay, so the failure tests that
mock `invalidateLinks(any(), any())` (R7-S4) keep working unchanged.
`invalidateLinks(UUID userId)` (password change, deactivation of the teacher's own links)
is unchanged.

### D5. Test helper

`ApiTest.createToken(userId, purpose, used)` gains an `issuerId` argument. Each of its 19
callers passes the issuer its scenario describes: the teacher for a student's
set-password link, an admin for an admin's link. A three-argument overload with a default
issuer was considered; it would hide the issuer exactly where R4 and R7 depend on it, and
it could not satisfy V7. With D1's nullable column the tests pass in pass 1; after V7 they
need the issuer. This is a change to approved test code, made in pass 1 and reviewed at
the test gate.

### D6. Guarded entry point for reset links

`PasswordLinkService.issueResetLink(UUID userId, UUID issuerId)`, transactional:

1. Lock the user row with `findByIdForUpdate`; 404 `NOT_FOUND` if there is none.
2. 409 `TARGET_ACCOUNT_DEACTIVATED` if the account is inactive. Nothing is stored or
   invalidated before this check (accounts R8).
3. Void the older unused link and insert the new one, through a private method shared
   with `issue`. The shared method does not lock the row again.

`issue(userId, RESET_PASSWORD, ...)` is rejected with an `IllegalArgumentException`, so a
reset link cannot be issued around the guard. `issue` keeps its own row lock for
set-password links. Two parts of this decision have no scenario, because no API call can
reach them, and are checked by the reviewer: the rejection of `RESET_PASSWORD` in `issue`,
and the guard on the teacher path (students cannot be deactivated).

`TeacherAdminService.issueResetLink` keeps only the own-account check (409 `OWN_ACCOUNT`),
which needs no row, and then delegates. An unknown id still gets 404, because the admin's
own id always exists. The teacher path (D3) calls the same method.

Reactivation keeps using `issue` with `SET_PASSWORD`: it runs in the transaction that
makes the account active.

Keeping the guard in each caller was considered and rejected: issue #30 (review of #80)
shows a new caller can forget it.

### D7. Error code and approved tests that change

`TARGET_ACCOUNT_DEACTIVATED` is a string literal at the one throw site in D6, with status
409. The 401 `ACCOUNT_DEACTIVATED` in login and link use is untouched. No frontend change:
an unknown code gets the generic message (web-client R5-S2), and the admin page offers no
link for a deactivated teacher (web-admin-teachers R5-S3).

Approved tests that change, each reviewed by the engineer:

| Test | Change | When |
|------|--------|------|
| `PasswordLinkTest`, accounts R8-S4 | expects `TARGET_ACCOUNT_DEACTIVATED` | pass 1 (the spec changes) |
| 19 callers of `createToken` | pass the issuer (D5) | pass 1 |
| `StudentRegistrationFailureTest`, two stubs of `issue` | three matchers instead of two (D2) | pass 2, after the engineer approves |
| Direct test callers of `createPasswordlessUser`, `createTeacher` or `reactivateTeacher`, if any | pass an issuer (D2) | pass 2, after the engineer approves |

## Flyway migrations

- `V6__token_issuer.sql`: add nullable `issued_by` (pass 1).
- `V7__token_issuer_required.sql`: delete links without an issuer, set `issued_by not null`
  (pass 2).

## Risks / Trade-offs

- [An issuing path passes the wrong issuer or none] → V7's `not null` rejects a missing
  one; the R4, R7 and R12 scenarios check the teacher's and the admin's links separately.
- [Removal now waits on the teacher row, so it serialises with that teacher's
  registrations and joins] → these are rare, short transactions for one teacher.
- [V7 deletes pending links in prod] → only test data exists (proposal, answer 2).
- [A client that keyed on 409 `ACCOUNT_DEACTIVATED` breaks] → the only client is the
  frontend, which does not (proposal, Research).
- [A teacher uses the reset link themself and keeps the student's password after removal]
  → accepted (proposal, answer 7); an admin can do the same today. The student notices,
  because the old password stops working. Sending the link by email later removes it.
- [The removal lock of D3 has no test; forgetting it leaves the race open] → reviewer-checked,
  with a comment at each lock (proposal, answer 8).
- [The admin sees a generic message on a stale list] → accepted (proposal, answer 6); the
  list reloads and shows the teacher as deactivated.

## Migration Plan

Deploy as usual; Flyway runs V6 and V7 on start. Rollback is a new forward migration.
