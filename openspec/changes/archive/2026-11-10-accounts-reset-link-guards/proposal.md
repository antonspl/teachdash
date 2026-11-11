# Proposal

Issue: #72.

## Clarifications

1. **May an admin issue a reset link for their own account?** No. The request is rejected
   with 409 and changes nothing. The admin changes their own password in the profile
   (accounts R9). Reset links for other accounts, including another admin's, still work.
2. **Which error does a deactivated account get?** 409 with the existing code
   `ACCOUNT_DEACTIVATED`. The UI already has a text for it, so the frontend does not change.

**Research angles:** the engineer agreed none were needed. The endpoint, accounts R7 and R8
and their tests were read before the proposal, and the change is two checks in one method.

**Assumption, not asked:** the code for the admin's own account is `OWN_ACCOUNT`. The
engineer chose the status and that it is rejected; the name of the code is ours.

## Why

`POST /api/admin/users/{id}/reset-link` issues a link for any existing account. In two cases
the link is useless or harmful:

- **Deactivated account.** The admin gets 201 and a link, but using a link for a deactivated
  account is rejected (accounts R7), so the link can never work.
- **The admin's own account.** Using the link raises the account's session version, so
  every session of the admin ends except the one the link starts.

The admin page (#34) offers neither action on purpose, so this is a gap in the API only.

## What Changes

- A reset-link request for a deactivated account returns 409 `ACCOUNT_DEACTIVATED`.
- A reset-link request for the admin's own account returns 409 `OWN_ACCOUNT`.
- Neither request stores a link or invalidates a link the account already has.
- **BREAKING** for API callers that relied on either case returning 201. The only client is
  the frontend. Its admin page shows the reset button only for active teachers, so it can
  send the first request only from a stale list, when another admin has just deactivated
  the teacher. It then shows the existing text "This account is deactivated.", which is
  right. It cannot send the second request: the page lists only teachers.

### Out of scope

- **Frontend:** no change. It reacts to a 401 status, not to the code, so a 409 with
  `ACCOUNT_DEACTIVATED` does not end the admin's session. `OWN_ACCOUNT` gets no UI text,
  because the page cannot trigger it; the API client shows its generic message for an
  unknown code (web-client R5).
- **A test for 404 on an unknown id:** the endpoint has none today. A scenario for it
  would pass before any code is written, so it is a follow-up issue.
- **A reset link for a teacher who never set a password:** unchanged; the page from #34
  offers a new invite link there, which is the same endpoint.
- **Reset links issued by a teacher for their own students:** #30.
- **Reactivation returning a link:** unchanged (accounts R6).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `accounts`: R8 no longer says "any account". It gains two scenarios, for a deactivated
  account and for the admin's own account.

## Impact

- **Backend:** `TeacherAdminService.issueResetLink`, `AdminController.resetLink` (passes the
  current admin's id) and tests in `PasswordLinkTest`.
- **API:** two requests that returned 201 now return 409. No migrations and no new
  dependencies.
- **Frontend:** none.
