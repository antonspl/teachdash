# Design

## Context

See proposal.md for the motivation and the decisions taken in Clarifications.

Backend contract (all under `/api/admin`, admin only, CSRF on POST):

| Call | Request | Success |
|---|---|---|
| List | `GET /teachers` | 200, bare array of `{id, name, email, role, timeZone, active, hasPassword}`, sorted by name, not paged. `hasPassword` comes with #70 |
| Create | `POST /teachers` `{name, email, timeZone}` | 201 `{id, setPasswordLink}` (no teacher object) |
| Deactivate | `POST /teachers/{id}/deactivate` | 204 (also on repeat) |
| Reactivate | `POST /teachers/{id}/reactivate` | 200 `{setPasswordLink}` if inactive and never set a password, otherwise 204 |
| Reset link | `POST /users/{id}/reset-link` | 201 `{link}` (note the different key) |

Errors are problem JSON with one `code`, no field details: `VALIDATION_FAILED`,
`MALFORMED_REQUEST`, `EMAIL_TAKEN` (409), `NOT_FOUND` (404), `UNAUTHENTICATED`,
`FORBIDDEN`, `CSRF_INVALID`, `INTERNAL_ERROR`. The translations already exist in
`en.json`.

Facts that shape the UI:
- A link is stored only as a hash, so it can be shown only in the response that
  issued it.
- Any new link for an account invalidates its other unused links.
- A link for a deactivated account is rejected when used (accounts R7-S6).
- Opening a link while logged in as admin replaces the admin session (web-auth R5-S10).
- The backend accepts any JVM zone id; there is no list endpoint.

Frontend today:
- `apiRequest` adds CSRF and throws `ApiError` with a translated message. A 401 on admin
  paths already goes through the session-ended flow (web-auth R6), so the page adds
  nothing for it.
- Pages may only compose `src/ui/` components: no CSS, `className` or inline style in
  `src/pages/`, and CSS uses design tokens only (`npm run lint`).
- `src/ui/` has no dialog, select, badge, list or clipboard component.

## Goals / Non-Goals

**Goals:**
- One page, plain React state, no new libraries.
- New UI pieces are generic `src/ui/` components that #35 and #36 can reuse.

**Non-Goals:**
- Caching or optimistic updates; the list is small and reloaded after each action.
- Client-side field rules beyond `required` and `type=email` (#44).

## Decisions

### D1 `/admin` renders the teachers page

`App.tsx` keeps `RequireRole role="ADMIN"`, and the admin route renders a new
`AdminTeachersPage` inside the existing cabinet layout (header with name and logout).
`/teacher` and `/student` stay as they are. Alternative: `/admin/teachers` plus a
redirect from `/admin`. Rejected for now: there is only one admin page, and the login
return logic already handles `/admin/...` if a sub-route is added later.

### D2 Typed API calls in `src/api/adminTeachers.ts`

Functions `listTeachers`, `createTeacher`, `deactivateTeacher`, `reactivateTeacher` and
`issueResetLink` wrap `apiRequest`. They return typed values, and the three link
responses map to one shape (`setPasswordLink` / `link` → `link`). `reactivateTeacher`
returns the link only on 200 and parses no body on 204. The page never reads raw JSON.
Until #70 is deployed the list has no `hasPassword`; a missing value is read as `true`
(status "Active", "Password reset link"). That is safe: a reset link also works for a
teacher who has no password yet.

### D3 State and reloading

The page uses `useState` for the list, the shown link, the open dialog and per-row busy
flags. After a successful create, deactivate, reactivate or link, and after a failed
deactivate, reactivate or link request, it loads the list again. A failed create
keeps the list as it is (R2). A failed action keeps the old list while the reload
runs. The shown link is separate state from the list: a failed reload shows the list's
error and "Try again" but never hides the link (R3-S5). The teacher's name for the link
box is taken from the submitted values before the form is cleared. The deactivate dialog
holds the teacher's id and name captured when it opened, not a list index, so a reload
while it is open cannot retarget it. Each row has one busy flag that disables all its
buttons (R5-S4); the dialog's Confirm uses the same flag, so it sends once (R4-S2).
An in-flight ref guards each submit, the same pattern as `LoginPage`. A 401 unmounts
the page through the session-ended flow; the page ignores responses that arrive after
unmount. Rejected:
react-query or a similar library, not worth a dependency for one list.

### D4 Modal dialog on the native `<dialog>` element

The new `ui/Dialog` component uses `<dialog>` with `showModal()`. That gives focus
trapping, Escape to close, a backdrop and `aria-modal` for free on all target browsers,
including Safari 15.4+. Escape fires `cancel`, which the component treats as "Cancel".
On close, focus returns to the button that opened it (R4-S8). jsdom does not implement
`showModal`/`close`, so the Vitest setup adds a minimal stub.
Rejected: a hand-made overlay (focus trap and Escape by hand), or a library such as
Radix (a dependency for one dialog).

### D5 Time zone list from `Intl`

The list is `Intl.supportedValuesOf('timeZone')` plus `UTC`, de-duplicated and sorted.
The browser's own zone (`Intl.DateTimeFormat().resolvedOptions().timeZone`) is added if
missing and preselected. If `supportedValuesOf` is unavailable, the list is just the
browser's zone and `UTC`. The list lives in a small module in `src/` (not a page), so it
can be unit-tested and reused by #33 (profile). Every IANA name the browser returns is
in the JVM's `ZoneId` set in practice. If one is not, the backend answers
`VALIDATION_FAILED`. On this form that code gets its own message that also suggests
another time zone (R2-S4), because the preselected browser zone may be newer than the
JVM's time zone data (e.g. `Europe/Kyiv` on an older JDK).

### D6 Issued link box

`ui/IssuedLink` shows the teacher's name, the link in a read-only text field (not an
`<a>`), the note, "Copy" and "Close". Copy calls `navigator.clipboard.writeText`. That
works on `localhost` and HTTPS, the only places the app runs. On rejection or when the
API is missing, the box shows the "copy by hand" message and selects the field's text.
There is one box on the page; a new link replaces it. The field's label names the
teacher, and the "Copied" / "copy by hand" messages use `StatusMessage`
(`role="status"`), so screen readers announce them.

### D7 Other new UI components

- `Select`: labelled native `<select>`, styled like `TextInput`.
- `StatusBadge`: three tones mapped to the statuses.
- `TeacherList` / row layout: generic `List` and `ListItem` components that stack
  name, details, badge and actions. They are a card per row rather than a table, so
  they fit phone widths (web-client R7).
- `Button`: a `danger` variant for "Deactivate" and the dialog's confirm.

All CSS uses tokens from `styles/tokens.css`, and new tokens are added there if needed.

### D8 Tests

- Vitest (fetch stubbed, same pattern as `SetPasswordPage.test.tsx`) covers every
  scenario except R2-S1 and R1-S4. R3-S3 checks the selection through the field's
  `selectionStart`/`selectionEnd`.
- Playwright covers R2-S1 (create against the real backend) and R1-S4 (phone width:
  `scrollWidth` not larger than the viewport after a teacher with a long email is
  created and its link shown) against the compose stack. They need a UI login as
  admin, so a `loginAsAdminInUi(page)` helper is added to `e2e/accounts.ts`, using the
  existing admin credentials. Each test uses a unique email, the same way
  `createTeacher` does. The clipboard is not tested in e2e (Vitest covers R3).
- Every test title starts with its scenario ID.
- `scripts/check-scenario-coverage.sh` gets `web-admin-teachers` in
  `FRONTEND_CAPABILITIES`.
- web-auth R3-S1 is unchanged and its test stays. web-auth R6-S1 still says the
  cabinets send no requests yet; that note is left as is, because changing R6 would
  take this change over 6 requirements. R1-S5 covers 401 on this page.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [#70 not merged yet] → a missing `hasPassword` is read as `true` (D2), so the page
  works without it. The statuses are tested in Vitest with stubbed responses (R1-S1);
  the e2e tests do not check them, so they pass with or without #70.
- [Browser zone list wider than the JVM's] → generic `VALIDATION_FAILED` message
  (R2-S4); acceptable for a rare id.
- [A new link replaces a pending one silently] → the note under every shown link says
  so; reset needs no confirm (a Claude default in proposal.md).
- [The list grows with no paging] → fine for the expected number of teachers per
  admin; paging is out of scope.
- [A teacher's email typo cannot be fixed] → no backend endpoint; out of scope.
