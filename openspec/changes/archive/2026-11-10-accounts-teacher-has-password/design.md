# Design

## Context

See `proposal.md` for the motivation. `GET /api/admin/teachers` maps each `User` to
`TeacherResponse` (`id`, `name`, `email`, `role`, `timeZone`, `active`), and no other
endpoint returns that record. `users.password_hash` is nullable: an account created by the
admin has `null` there until a password link is used (`PasswordLinkService`). No code path
sets it back to `null`. `TeacherAdminService` already decides between a new link and no
link on reactivation with the same `null` check.

## Goals / Non-Goals

**Goals:**

- The admin can tell from the list which teachers have not set a password.

**Non-Goals:**

- Telling apart "never had a link", "link unused" and "link expired". The page offers a new
  invite link in all three cases.

## Decisions

### D1. `hasPassword` is `password_hash != null`

`TeacherResponse.of` sets `hasPassword` from `user.getPasswordHash() != null`. The list
query already loads the whole `User`, so there is no new query and no new column.

- **Rejected: derive it from used password links.** It would need a join, and it would be
  wrong for an account that got its password another way, such as the first admin.

### D2. The field name is `hasPassword`

The frontend from PR #71 already reads this name. The record component is called
`hasPassword`, which Jackson writes under the same name.

### D3. The check that the hash is not returned lives in the R12-S2 test

A separate "no hash in the response" scenario would pass before any code is written, so it
could not fail for the right reason in pass 1. R12-S2 asserts both `hasPassword` and the
exact set of fields of the list item, so a field added later, the hash or anything derived
from it, fails the test until it is reviewed.

Changed in the final review: the test first checked that the response body does not contain
the stored hash, read from the database. That only caught the full stored string.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [A future code path clears or pre-fills `password_hash` for another reason] → The field
  would then be wrong. Nothing does this today. Both R12-S1 and R12-S2 create the teacher
  through the API (`POST /api/admin/teachers`): R12-S1 would catch a hash pre-filled on
  teacher creation, and R12-S2, which sets the password with the returned link, would catch
  a link that does not store the password.
- [A client that rejects unknown fields] → The only client is the frontend, which ignores
  them and already expects this one.

## Migration Plan

Deploys on its own, before or after PR #71. No data change. Rollback is a revert.
