# Design

## Context

See `proposal.md` for the motivation and the engineer's decisions.

`AdminController.resetLink` calls `TeacherAdminService.issueResetLink(userId)`. That method
loads the user, answers 404 `NOT_FOUND` for an unknown id, and calls
`PasswordLinkService.issue(userId, RESET_PASSWORD)`. `issue` locks the user row, marks the
account's unused links as used and stores the new one. Nothing checks who the account
belongs to or whether it is active.

Only teachers can be deactivated (accounts R6), and deactivation already invalidates the
teacher's unused links. Using a link for a deactivated account answers 401
`ACCOUNT_DEACTIVATED` (R7).

## Goals / Non-Goals

**Goals:**

- The admin never receives a reset link that cannot work or that ends their own session.
- A rejected request changes nothing: no link stored, no existing link invalidated.

**Non-Goals:**

- Changing how links are used, or what reactivation returns.
- A UI text for `OWN_ACCOUNT` (see the proposal).

## Decisions

### D1. Both checks live in `TeacherAdminService.issueResetLink`

The method takes the current admin's id as a second argument and checks, in this order:

1. unknown id → 404 `NOT_FOUND` (as today);
2. the id is the current admin's → 409 `OWN_ACCOUNT`;
3. the account is not active → 409 `ACCOUNT_DEACTIVATED`;
4. otherwise issue the link.

An admin cannot be deactivated, so checks 2 and 3 never both apply. The checks run before
`PasswordLinkService.issue`, so a rejected request never reaches the code that invalidates
older links.

- **Rejected: checks in `PasswordLinkService.issue`.** Reactivation calls `issue` for an
  account that is still inactive at that moment (R6-S7), so `issue` cannot refuse inactive
  accounts.

### D2. The controller passes the current user's id

`AdminController.resetLink` reads the id from the `Principal`, the same way `MeController`
does, and passes it to the service. The service does not read the security context itself.

### D3. The active check reads the user under the row lock

`issueResetLink` becomes transactional and loads the user with the existing
`findByIdForUpdate` before the active check. A deactivation running at the same time then
either finishes first, and the request gets 409, or waits until the link is stored and
invalidates it, as deactivation already does. `issue` takes the same lock inside the same
transaction, so the lock order (user before links) does not change.

### D4. `ACCOUNT_DEACTIVATED` is reused with 409

The code already means "this account is deactivated" and has a UI text. Here the status is
409, not 401, because the caller is a logged-in admin: a 401 would make the frontend treat
the admin's own session as ended.

### D5. R8-S5 also covers another admin's account

A separate scenario "a reset link for another admin is allowed" would pass before any code
is written, so it could not fail for the right reason in pass 1. R8-S5 asserts both: the
admin's own account is rejected and a second admin's account still gets a link. This guards
against an implementation that refuses every admin account.

### D6. The tests seed an unused link

"No link is stored" alone cannot tell a request rejected before `issue` from one rejected
after it, because `issue` first marks the account's unused links as used. So R8-S4 and
R8-S5 start with an unused link for the account, inserted directly, and assert that it is
still unused and is the account's only link. For the admin this state is reachable in
production: a second admin issues the link. For a deactivated teacher it is not, because
deactivation invalidates the teacher's links; the test inserts the row to prove the order
of the checks.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [The only admin forgets their password] → Not made worse: they could not log in to issue
  a link before either. Recovery stays outside the app.
- [Two admins reset each other's passwords] → Allowed on purpose; it is how one admin helps
  another who lost their password.
- [The concurrent case in D3 has no test] → It would need two timed transactions. The
  lock is the existing one, and the outcome in both orders is correct by R6 and R8.

## Migration Plan

Deploys on its own. No data change. Rollback is a revert.
