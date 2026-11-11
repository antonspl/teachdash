# Proposal

Issue: #33.

## Clarifications

Answered by the engineer:

- **Where the profile lives.** One `/profile` page for all three roles, not a section
  inside each cabinet.
- **One form or two.** Two separate forms on the page, each with its own button:
  "Profile" (name, time zone) and "Password". A wrong current password never blocks a
  name change.
- **Repeat the new password?** Same as the set-password page. That page asks twice
  (web-auth R5), so the password form asks for the new password twice, with the same
  show/hide toggle and the same checks.
- **Client-side checks of name and time zone (#44).** Left to #44. The form uses only
  the browser's `required`, and shows a friendly general message when the backend
  rejects the values. The backend does not say which field failed.
- **The late 401 from an old session (#65).** Left to #65. This change only blocks a
  double submit on both forms.
- **"Profile" in the header.** A secondary button next to "Log out", in the same style.
- **Email.** Shown as plain text, not as a disabled field. It cannot be changed.

Defaults suggested by Claude; the engineer can change any of them:

- **Way back.** On `/profile` the header shows a "Back to cabinet" button in the place
  where the cabinets show "Profile". It opens the user's own cabinet.
- **After a profile save** the form and the header show the values the backend
  returned (it trims the name). No extra request is made.
- **A stored time zone that the browser does not list** is added to the select, so the
  select is never empty and saving the name alone does not change the zone.
- **Success feedback.** Each form shows its own message under its fields. It goes away
  on the next edit of that form.
- **After a password change** the three password fields are emptied and the user stays
  logged in on the page. Other sessions of the user end (accounts R9-S4); the page says
  so in the success message.
- **A new password equal to the current one** is sent as is. The backend accepts it.
- **Strings** are informal and friendly, like the admin teachers page.

Research, two angles agreed with the engineer:

- **Backend contract.** The backend already has everything the page needs, so this
  change is frontend only. `PATCH /api/me` needs both `name` and `timeZone` and returns
  the saved user; `POST /api/me/password` returns 204, ends the user's other sessions
  and replaces this session's cookie and CSRF token. Validation errors are a bare
  `VALIDATION_FAILED` with no field name; a wrong current password is `WRONG_PASSWORD`.
  There is no endpoint that lists time zones. Details are in `design.md`.
- **Frontend reuse.** The form components, the password input, the select, the status
  message, the API client and the time zone list exist. Missing: a route guard for any
  logged-in role, a way to update the stored user, a success message pattern and a
  shared password rule (today private to the set-password page). Details are in
  `design.md`.

## Why

The backend lets a user change their name, time zone and password (accounts R9), but
there is no page for it. A teacher created by the admin keeps the name and time zone
the admin typed, and can change the password only by asking the admin for a reset link.

## What Changes

- A `/profile` page for a logged-in user of any role, with the email as text, a
  profile form (name, time zone) and a password form (current password, new password
  twice).
- The cabinet header gets a "Profile" button next to "Log out"; on the profile page a
  "Back to cabinet" button takes its place.
- After a profile save the header shows the new name without a reload.
- A 401 on a profile save now starts the "session ended" flow. Today every request to
  `/api/me` is exempt from it, which is right only for the startup read.
- The password rule (two fields match, at least 8 characters, at most 72 bytes) moves
  to one shared place, used by the set-password page and the password form.

## Capabilities

### New Capabilities
- `web-profile`: the profile page of the web client, where a user changes their own
  name, time zone and password.

### Modified Capabilities
- `web-auth`: R3 Role cabinets. The header of every cabinet page gets a "Profile"
  button.

## Out of scope

- Any backend change. The backend's gaps found by the research (no field name in
  validation errors, no rate limit on the current-password check) are not fixed here.
- Shared client-side field rules (#44).
- The late 401 from an old session (#65) and the other auth follow-ups (#75), except
  the one-line exemption change above.
- Follow-ups from the admin teachers review (#73), including how status messages are
  announced.
- Changing the email (email verification is #46).
- Cabinet tabs and navigation (#57).

## Impact

- `frontend/src/pages` (new page), `frontend/src/ui/AppHeader`, `frontend/src/auth`
  (guard for any role, updating the stored user), `frontend/src/api` (two typed calls,
  the 401 exemption), `frontend/src/timeZones.ts`, `frontend/src/i18n/en.json`, Vitest
  and Playwright tests.
- `scripts/check-scenario-coverage.sh`: adds `web-profile` to the frontend
  capabilities.
- No backend change, no new dependencies, no migrations.
