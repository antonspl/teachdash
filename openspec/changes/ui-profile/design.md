# Design

## Context

See `proposal.md` for the motivation and the engineer's answers. This file records what
the two research angles found and the choices that follow.

Backend contract (accounts R9, R11, R4; `MeController`, read by the researcher, not run):

- `GET /api/me` returns `{id, name, email, role, timeZone}`.
- `PATCH /api/me` takes `{name, timeZone}`. Both are required. It returns 200 with the
  same shape, holding the saved values; the name comes back trimmed.
- `POST /api/me/password` takes `{currentPassword, newPassword}` and returns 204 with no
  body. It ends the user's other sessions, replaces this session's cookie and sends a
  new `XSRF-TOKEN` cookie. The old CSRF token stops working.
- Errors are `{status, title, code}` with no field name: 400 `VALIDATION_FAILED` (name,
  time zone, new password, blank current password), 400 `WRONG_PASSWORD`, 401
  `UNAUTHENTICATED`, 403 `CSRF_INVALID`. Validation runs before the current password is
  checked, so a short new password with a wrong current one gives `VALIDATION_FAILED`.
- The new password must have at least 8 UTF-16 units and at most 72 UTF-8 bytes. A new
  password equal to the current one is accepted.
- A time zone is accepted only if it is exactly one of the JVM's zone ids. No endpoint
  lists them.

Frontend today (read by the researcher and partly by the main agent):

- `App.tsx` declares flat routes, each inside `RequireRole`, which takes one role and
  sends any other role to its own cabinet.
- `AuthProvider` holds the current user and exposes `login`, `logout`,
  `setPasswordWithLink` and `retry`. Nothing updates the user after login.
- `api/client.ts` reads the CSRF cookie on every non-GET request. A 401 calls the
  session-ended handler unless the path is in `UNAUTHORIZED_EXEMPT`, which holds `me`
  for every method.
- `LoginPage` returns the user to the page they were on only if its path is their
  cabinet path or under it.
- `ui/AppHeader` shows the app name, "{name} ({role})" and "Log out". Only
  `CabinetPage` renders it.
- `SetPasswordPage` holds the password rule as private constants and its three messages
  under `setPassword.*`.
- `timeZones.ts` builds the option list from the browser; it takes no extra value.
- `scripts/check-scenario-coverage.sh` lists the frontend capabilities by name.

## Goals / Non-Goals

**Goals:**

- The profile page works for all three roles with the building blocks that exist.
- The header shows the new name right after a save.
- The password rule has one definition in the frontend.

**Non-Goals:**

- No refactor of the guards or of the 401 exemption list beyond what the page needs
  (#75 does that).
- No change to `ui/StatusMessage` (#73 item 3 covers how messages are announced).
- No shared rules for name and time zone (#44).

## Decisions

### D1. A new guard for any logged-in user

`/profile` is wrapped in a new guard, `RequireUser`, in `auth/guards.tsx`. It shows the
same loading, startup-error and anonymous branches as `RequireRole` and renders its
children for any role.

- **Rejected: a `roles` prop on `RequireRole`.** It changes a guard every route uses,
  and #75 plans to merge the duplicated branches anyway.

### D2. The profile page renders the header itself

`ProfilePage` renders `ui/Page`, `ui/AppHeader` and the two forms. `AppHeader` gets one
new prop that says which button to show before "Log out": "Profile" (goes to
`/profile`) or "Back to cabinet" (goes to `cabinetPath[role]`). `CabinetPage` passes
"Profile". Both are the existing `ui/Button` in its secondary variant, like "Log out",
and navigate with the router on click. The engineer chose a button over a real link:
no link-styled-as-button component exists, and the spec says "button".

The header's name gets a wrapping rule (`overflow-wrap: anywhere` and `min-width: 0` on
the flex item), so a long name without spaces wraps and does not widen the page. The
header has no such rule today, and the second button makes the row wider
(web-profile R1-S5 checks the profile page and a cabinet).

- **Rejected: reuse `CabinetPage` with a title prop.** Its `role` prop selects the
  cabinet title, and the profile has no role of its own.

### D3. The API calls live in `api/profile.ts`; the stored user is replaced through `AuthProvider`

`api/profile.ts` has `updateProfile({name, timeZone})`, which returns the user, and
`changePassword({currentPassword, newPassword})`, which parses no body. `AuthProvider`
gets `updateProfile`, which calls the API and puts the returned user into its state, the
same way `login` does. The profile form then takes its field values from that user.

- **Rejected: read `/api/me` again after a save.** One more request, and it can race
  with a session change.

### D4. A 401 on `PATCH /api/me` is not exempt

`UNAUTHORIZED_EXEMPT` exempts `me` because the startup read answers 401 for an anonymous
visitor. A save by a logged-in user is different: its 401 means the session ended
(web-profile R4-S1). The exemption for `me` is narrowed to GET requests. `me/password`
is a different path and is not exempt today.

The exemption check gets the request method as well as the path. This touches the list
#75 wants to replace with a per-request option; the change is one condition, and #75
can replace it later.

### D5. `/profile` is restored after logging in again

`LoginPage` restores the previous path only when it is the user's cabinet path or under
it. `/profile` is added as a path that belongs to every role (web-profile R4-S3,
web-auth R6). The comparison ignores a trailing slash, so `/profile/` is restored too.
The rule for cabinet paths does not change (web-auth R6-S2 to R6-S4 still pass).
`/profile/x` matches no route and shows the not-found page (web-client R11).

### D6. One password rule module

The constants and the check (mismatch, then too short, then too long) move from
`SetPasswordPage` to `src/passwordRules.ts`, which returns a translation key or nothing.
The three messages move from `setPassword.*` to shared `password.*` keys. Both pages use
the module. The set-password tests (web-auth R5-S3, R5-S4, R5-S8) stay as they are and
must still pass.

The minimum is counted the way `SetPasswordPage` counts it today. #22 may change the
backend's count to code points; then one place changes here.

### D7. The select always holds the stored zone

`timeZoneOptions` takes an optional extra zone and adds it to the set before sorting.
The profile form passes the user's stored zone (web-profile R2-S7). The add-teacher
form keeps calling it with no argument.

A zone the browser lists and the JVM does not is still possible. The backend then
answers `VALIDATION_FAILED` and the form shows its general message (web-profile R2-S5).

### D8. Messages and double submit follow the set-password page

Each form has one `ui/StatusMessage` above its button: tone `danger` for errors, tone
`success` after a save. Client-side password messages use the same place. The message
is cleared when a field of that form changes and when the form is submitted; editing
the other form does not clear it. A `VALIDATION_FAILED` answer gets its own friendly
string per form; every other error, including 403 `CSRF_INVALID` ("reload the page"),
uses the message already on `ApiError`.

The password form's `VALIDATION_FAILED` string is neutral ("check the passwords"),
because the backend gives the same code for a bad new password and for a blank current
one (the engineer's choice over a blank check in the browser).

Each form has its own in-flight ref and `submitting` state, as `SetPasswordPage` has,
so a second press sends nothing. The page also knows when either form is sending and
disables the other form's button until it ends (web-profile R3-S11). This keeps a save
and a password change from running at the same time, which matters because the
password change replaces the session and the CSRF token (see Risks).

A form's fields are disabled while it is sending, so the values the backend returns
never overwrite something typed meanwhile.

### D9. Nothing extra after a password change

The browser stores the new session and CSRF cookies from the response. `apiRequest`
reads the CSRF cookie on every request, so the next save uses the new token
(web-profile R3-S1 proves it against the real backend). The stored user does not
change, so nothing is read again.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [A request already running when the password changes gets 401 or 403, because the
  session and the CSRF token were replaced] → This is #65. Here the two forms are the
  only senders on the page, each blocks its own double submit, and each form's button
  is disabled while the other is sending (D8). A request from another tab can still
  collide; that stays with #65.
- [Pressing "Profile" on the admin page drops a link that is shown there] → Accepted:
  the link is page state and the admin can issue a new one. An open dialog makes the
  header inert, so typed dialog values are safe.
- [The success message may not be read out by a screen reader, because the live region
  mounts together with its text] → Known, #73 item 3. Not made worse here.
- [The 200-character name limit and the name rules are not checked in the browser] →
  The backend rejects them and the form shows the general message. #44 adds the rules.
- [The browser's zone list and the JVM's differ] → See D7.
- [Narrowing the `me` exemption changes the API client used by every page] → Only a
  non-GET request to `me` is affected, and only this page sends one. The startup tests
  (web-auth R2) and the session-ended tests (web-auth R6) must still pass.
