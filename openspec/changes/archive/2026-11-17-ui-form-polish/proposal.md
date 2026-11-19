# Proposal

Issues: #86, #87.

## Clarifications

Answered by the engineer:

- **One change for both issues.** Both are small frontend-only polish of the same forms,
  found while testing the profile page (PR #84). The branch is stacked on `ui-profile`,
  because the profile password form exists only there until #84 is merged.
- **Set-password page.** It gets the same hints as the profile password form: both pages
  ask for a new password twice and use the same rules.
- **When a hint first appears.** While typing, from the first character, not only after
  leaving the field. It goes away as soon as the value is fine.
- **The button while a hint is shown.** It stays enabled. Pressing it sends nothing and
  keeps the hints.
- **Current password.** Not checked while typing. Only the backend can check it, so that
  would be a password-guessing endpoint with a slow hash on every pause in typing, and
  login rate limiting (#12, #76) is not built. It is still checked on submit.
- **Research.** The engineer agreed none is needed: both parts are small and the code is
  known. The arrow is checked by eye on screenshots in Chrome, Firefox and Safari.

Defaults suggested by Claude; the engineer can change any of them:

- **Where a hint is shown.** Under its own field, and the field is marked invalid: the
  length hints under the new password, "passwords do not match" under the repeat field.
  Today all three share one message above the button.
- **Empty fields.** An empty field shows no hint of ours. All password fields are
  required, so the browser itself asks to fill an empty one when the button is pressed,
  as it does today.
- **Two hints at once.** A short new password and a different repeat show both hints,
  each under its own field.
- **Where the select rule lives.** In `web-client`, because the select is a shared
  control used on several pages.

## Why

The time zone select shows the browser's own arrow almost on the field border, which
looks broken next to the padded text fields (#86). The password rules are checked only
after the button is pressed, so the user learns about a typo in the repeat field one
step later than on most sites (#87).

## What Changes

- The shared select draws its own arrow, inset from the border like the text, and keeps
  a long option from running under it. Applies on the profile page and in the "Add
  teacher" dialog.
- The profile password form and the set-password page show the "at least 8 characters",
  "password is too long" and "passwords do not match" hints while the user types, under
  the field they belong to.
- The existing rule stays: a form with a broken password rule is not sent
  (web-profile R3-S3 to R3-S5, web-auth R5-S3, R5-S4 and R5-S8 stay true as written).

Out of scope:

- Checking the current password while typing (see Clarifications).
- A password strength meter or new password rules.
- Live checks of name, email and time zone (#44).
- Announcing hints to screen readers through a live region (#73, item 3).
- The other follow-ups in #73.
- Clearing a backend error on the set-password page when a field is edited (#89). This
  change only removes it when the button is pressed again.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-client`: two added requirements, R13 "Select arrow" and R14 "New-password hints
  while typing". No existing requirement changes.

## Impact

- `frontend/src/ui/Select.tsx`, `frontend/src/ui/Field.module.css`, and
  `frontend/src/styles/tokens.css` if the arrow needs a size token: the arrow.
- `frontend/src/passwordRules.ts`, `frontend/src/pages/PasswordForm.tsx`,
  `frontend/src/pages/SetPasswordPage.tsx`: the hints.
- Vitest and Playwright tests for the new scenarios; the tests of web-profile R3-S3 to
  R3-S5 and web-auth R5-S3, R5-S4 and R5-S8 are adjusted only where they look for the
  message.
- No backend, API, dependency or translation-key changes.
- The PR is based on `ui-profile` and is re-based to `main` after #84 is merged.
