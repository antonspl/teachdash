# Proposal

## Clarifications

Answered by the engineer:

- **Confirm before deactivating?** Yes, in a modal dialog. Deactivation releases all
  the teacher's students (student-enrolment R7) and reactivation does not bring them
  back, so the dialog says so.
- **Time zone input.** A select built from the browser's list of time zones, plus
  `UTC`, with the admin's own browser zone preselected.
- **Getting a new link for a teacher.** The page needs to know who has not set a
  password yet, to show "waiting for password" and offer a new invite link instead
  of a password reset. The list does not say this today. The backend part (a
  `hasPassword` field in the teacher list) is a separate issue, #70. The page reads
  the field and treats a missing value as "has a password", so this change does not
  wait for #70: until it lands, every active teacher shows as "Active" with
  "Password reset link" (a reset link also works for a teacher with no password).
  The three statuses are tested with stubbed responses (R1-S1).
- **Client-side checks of name and email (#44).** Left to #44. The form uses only
  the browser's `required` and `type=email`, and shows the backend's translated
  errors.

Defaults suggested by Claude; the engineer can change any of them:

- **Where the page lives.** The admin cabinet `/admin` becomes the teachers page. No
  sub-route and no tabs (#57 adds tabs to the other cabinets later).
- **How a link is shown.** As plain text, not a clickable link, with a "Copy" button
  and a note: the link is shown only now, works for 7 days and once, and opening it
  in this browser would log the admin in as the teacher (web-auth R5-S10). If copying
  fails, the page says so and selects the text for copying by hand.
- **Which actions each row has.** Active teacher: "Deactivate" and a new link ("New
  invite link" if they have no password yet, "Password reset link" otherwise).
  Deactivated teacher: "Reactivate" only, because a link for a deactivated account is
  rejected (accounts R7-S6).
- **Reactivate and new links need no confirmation.** Reactivation is reversible; a new
  link changes nothing until it is used. A new link does replace any unused one, and
  the note under the link says so.
- **After any action** the list is loaded again from the backend.

Research: one researcher collected the backend contract of the admin endpoints and the
frontend's patterns (API client, UI components, tests). Its findings are in
`design.md`. No other angles were needed.

## Why

The backend lets an admin create, deactivate and reactivate teachers and issue reset
links (accounts R5, R6, R8), but there is no page for it. Today a teacher can only be
created through the API, so nobody can try the teacher and student pages (#35, #36)
by hand.

## What Changes

- `/admin` shows the list of teachers with each one's name, email, time zone and
  status: active, waiting for password, or deactivated.
- A form creates a teacher (name, email, time zone) and shows the set-password link.
- Deactivate (after a confirmation dialog) and reactivate from the list; reactivating
  a teacher who never set a password shows the new link.
- A new invite or password reset link for an active teacher.
- Issued links are shown once as text with a "Copy" button.
- New UI components: a dialog, a select, a status badge, a list of rows and the
  issued-link box.

## Capabilities

### New Capabilities
- `web-admin-teachers`: the admin's page for managing teacher accounts in the web
  client.

### Modified Capabilities
- `web-auth`: R3 Role cabinets. The admin cabinet is no longer empty; it shows the
  teachers page.

## Out of scope

- The backend `hasPassword` field (#70).
- Shared client-side field rules (#44).
- Editing a teacher's name, email or time zone: the backend has no endpoint for it.
- Paging, search or sorting in the list; the backend returns all teachers sorted by
  name.
- The admin's view of a teacher's students (#36).
- Links for students (#35) and the admin's own account.

## Impact

- `frontend/src/pages`, `frontend/src/ui` (new components), `frontend/src/api` (typed
  calls for the admin endpoints), `frontend/src/i18n/en.json`, Vitest and Playwright
  tests.
- `scripts/check-scenario-coverage.sh`: adds `web-admin-teachers` to the frontend
  capabilities.
- No backend change in this PR, no new dependencies, no migrations. Nothing here
  waits for #70.
