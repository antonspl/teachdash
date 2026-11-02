# Proposal

## Clarifications

Issue #40 left three questions for the spec. The owner answered them before this proposal.

1. **Should login stop returning `PASSWORD_NOT_SET`?** Yes. It becomes
   `INVALID_CREDENTIALS`. The single message also tells users who have not set a password
   to use the link they were given, so they still get a hint.
2. **Should login stop returning `ACCOUNT_DEACTIVATED`?** Only when the password is not
   proven. A deactivated account given its correct password keeps `ACCOUNT_DEACTIVATED`:
   the caller already has the credentials, so nothing leaks and the teacher learns why.
   Every other failure on a deactivated account is `INVALID_CREDENTIALS`.
3. **Should the spec close the timing leak too?** Yes. Every login runs one password hash
   check, against a dummy hash when there is no real one to check.

## Why

`POST /api/auth/login` tells anonymous callers which emails have accounts, through its
error codes and through response time, because bcrypt runs only when the account has a
password. This reverses the choice made for accounts R2 in the #3 review.

## What Changes

- **BREAKING** Login returns `INVALID_CREDENTIALS` instead of `UNKNOWN_EMAIL`,
  `WRONG_PASSWORD` and `PASSWORD_NOT_SET`. No login page exists yet, so no client breaks.
- A deactivated account gets `ACCOUNT_DEACTIVATED` only with the correct password.
- Every login that passes validation runs exactly one password hash check, so response
  time does not reveal an account or a stored password.
- The English UI text gains `INVALID_CREDENTIALS` and loses `UNKNOWN_EMAIL` and
  `PASSWORD_NOT_SET`.

**Trade-off:** users no longer see whether the email or the password was wrong.

### Out of scope

- **`WRONG_PASSWORD` on `POST /api/me/password`** (400): the caller is logged in.
- **`ACCOUNT_DEACTIVATED` on `POST /api/password-links`** (401): the caller must hold a
  valid 32-byte random token, so it cannot be used to test emails.
- **`EMAIL_TAKEN`**: admin-only.
- **Rate limiting or lockout on login**: none exists today, so password guessing is not
  limited. It needs its own change.

The password-link endpoint and login are the only anonymous endpoints that touch
accounts, and there is no sign-up or email-based reset, so this change closes the known
anonymous email checks.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `accounts`: R2 and R5-S1 use the new code; new requirement R10 covers login timing.
- `web-client`: R5 gains the message for `INVALID_CREDENTIALS`.

## Impact

- **Backend:** `AccountService.authenticate` and its tests (`LoginTest`,
  `TeacherManagementTest`). `backend/local-check.sh` and `backend/local.http` also change,
  but they are local-only files (excluded in `.git/info/exclude`), so they are not in the PR.
- **Frontend:** `frontend/src/i18n/en.json` and one new Vitest test. `WRONG_PASSWORD`
  stays in `en.json` for the change-password endpoint.
- **API:** login error codes change. No migrations and no new dependencies.
