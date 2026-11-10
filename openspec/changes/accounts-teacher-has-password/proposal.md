# Proposal

Issue: #70.

## Clarifications

1. **Where does the requirement go?** The issue suggests a delta on accounts R6. The
   engineer chose a new requirement, R12, instead. R6's text is close to the 500-character
   limit of strict validation (#49), and R6 is about deactivation, not about passwords.
2. **Which responses carry the field?** Every response that returns the teacher list item
   (`TeacherResponse`). Today only `GET /api/admin/teachers` returns it. Creating a teacher
   returns a different shape and does not change.

**Research angles:** the engineer agreed none were needed. `users.password_hash` is already
nullable and stays `null` until a password link is used, so the field needs no new data.

## Why

A set-password link cannot be shown again after it is issued, because only its hash is
stored. The admin's teachers page (#34, PR #71) therefore needs to know which teachers have
not set a password yet, to label them "Waiting for password" and offer a new invite link
instead of a password reset. The teacher list does not say this today.

## What Changes

- Each item of `GET /api/admin/teachers` gains a boolean `hasPassword`: `false` until the
  teacher sets a password, `true` afterwards.
- The list still never returns the password hash or anything else derived from it.

### Out of scope

- **How links are issued:** reset-link and reactivate stay as they are (#72 covers reset
  links for deactivated accounts and the admin's own account).
- **Frontend:** the admin page from PR #71 already reads `hasPassword` and treats a missing
  value as `true`, so it needs no change.
- **Other lists:** the teacher's student list and the admin's student views are not changed.
- **Creation time and sort order of the list:** #74.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `accounts`: new requirement R12, the teacher list shows whether each teacher has set a
  password.

## Impact

- **Backend:** `TeacherResponse` and tests in `TeacherManagementTest`.
- **API:** one added field in the teacher list. Nothing is removed or renamed, so existing
  clients keep working.
- **Frontend:** none. No migrations and no new dependencies.
