# Proposal

Issues: #101, #89.

## Clarifications

Answered by the engineer:

- **Branch.** Stacked on `ui-form-polish` (#88), which is stacked on `ui-profile` (#84), so
  the profile and password forms get field errors too. Both were updated with `main`
  first, so the branch has the backend of #100 (`api-errors`) and e2e tests can run
  against it. The PR is re-based to `main` after #84 and #88 are merged.
- **Translations.** English only. The frontend has no `ru` resources yet; #103 adds
  Russian for the whole UI, these strings included.
- **Forms.** All five forms that send a body: login, set password, "Add teacher" dialog,
  profile, password change. Login and set password get it through the same mechanism at
  little cost.
- **A field the form does not show** (for example `token` on the set-password page), an
  unknown code, or a response without `errors`: the form shows its current form-level
  message, as today.
- **Messages.** One generic message per field code (`REQUIRED`, `TOO_SHORT`, `TOO_LONG`,
  `INVALID_FORMAT`, `INVALID_TYPE`). `EMAIL_TAKEN` and `WRONG_PASSWORD` keep the texts the
  forms use today. The backend does not send limits, so the messages do not name them.
- **Form-level message and focus.** When every error is shown under its field, there is
  no form-level message, and the focus moves to the first field with an error, like the
  hints in `web-client` R14-S12.
- **Clearing.** Editing a field removes that field's backend error. #89 is part of this
  change: editing a field on the set-password page also removes its form-level message.
- **Client hint and backend error on one field.** The client hint wins, because it
  describes what is typed now.
- **Existing form-level texts** (`validationFailed`, `passwordValidationFailed` and
  others) stay as the fallback.
- **Select.** The shared select gets the same error slot as the text input, so a
  `timeZone` error is shown under it.
- **Requirement numbers.** `web-client` R5 is modified. The engineer agreed on one new
  requirement, R17; it became R17 and R18, because one requirement's text was too long.
  R15-R16 are reserved for #103. #102 reserves R13-R14, which `ui-form-polish` already
  uses; the owner is told about that clash.

Research angles agreed with the engineer:

- **A. How the forms show errors today.** Every form keeps one form-level error string;
  none shows a backend error under a field. The `error` prop of the text input (used by
  the R14 hints) already renders a message under the field, marks it invalid and links
  it as the description; the select has no such prop. `ApiError` keeps only `code`;
  carrying `errors` is a small change in the API client. No existing test sends
  `errors`, so all of them keep passing on the fallback path.
- **B. Backend fields per form.** Login can get `email` and `password` errors; set
  password `token` (no input) and `password`; the dialog `name`, `email`, `timeZone`
  and `EMAIL_TAKEN`; profile `name` and `timeZone`; the password form `currentPassword`,
  `newPassword` and `WRONG_PASSWORD`. `EMAIL_TAKEN`, `WRONG_PASSWORD`, a too-long name
  and a too-long login email are reachable through the UI against the real backend;
  the rest only past the client checks, so they are tested with mocked responses.

## Why

Since #100 the backend says which field failed and why, but the frontend still shows one
message for the whole form ("Hmm, the backend didn't accept those values…"), and the
user has to guess which field is wrong. On the set-password page a backend message also
stays while the user fixes the fields (#89).

## What Changes

- The API client gives the caller the backend's list of field errors along with the
  translated message (`web-client` R5).
- The five forms show each backend field error under its field, mark the field invalid,
  move the focus to the first one and show no form-level message (`web-client` R17).
- Editing a field removes its backend error (`web-client` R18).
- Anything a form cannot place keeps today's form-level message.
- The set-password page removes its form-level message when a field is edited (#89,
  `web-client` R18).
- The password form's scenario about editing after `WRONG_PASSWORD` names the current
  password field, because that message now sits under it (`web-profile` R3-S10).
  `web-profile` R3-S6 and R3-S7 say they describe a response without `errors`.

Out of scope:

- Russian strings (#103).
- Live checks of name, email and time zone while typing (#44).
- Field errors in forms that do not exist yet (join link, teacher's students).
- Clearing the form-level message on edit in the login page and the dialog.
- Announcing errors through a live region (#73, item 3).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-client`: R5 "Translated API errors" modified (field errors reach the caller);
  R17 "Backend errors under their fields" and R18 "Backend field errors go away on
  edit" added.
- `web-profile`: R3 "Change password" modified (R3-S6 and R3-S7 name a response without
  `errors`; R3-S10 names the current password field).

## Impact

- `frontend/src/api/client.ts`: `ApiError` carries the field errors.
- `frontend/src/ui/Select.tsx`: error slot like `TextInput`.
- A small shared helper for mapping, focus and clearing, used by `LoginPage`,
  `SetPasswordPage`, `AdminTeachersPage` (create dialog), `ProfileForm` and
  `PasswordForm`.
- `frontend/src/i18n/en.json`: one message per field code.
- New Vitest and Playwright tests. Of the existing tests only web-profile R3-S10 changes,
  to follow its modified scenario; the others send no `errors` and keep passing.
- No backend, API or dependency changes.
