# Design

## Context

See `proposal.md` for the motivation and the owner's decisions. `AccountService.authenticate`
looks up the account and runs bcrypt only if a hash exists and the password is at most 72
bytes. The password encoder is Spring's delegating encoder, so new hashes are `{bcrypt}` at
the default cost of 10. Test fixtures hash at cost 4.

## Goals / Non-Goals

**Goals:**

- An anonymous caller learns nothing about an account from a login, unless they supply its
  correct password.

**Non-Goals:**

- Equal timing for a successful login: that caller already has the password.
- Rate limiting (see the proposal).

## Decisions

### D1. Check the password before the account state

`authenticate` always gets a match result first. If it fails, the code is
`INVALID_CREDENTIALS`. Only after a match does it check `isActive`. This gives R2's rule
with one failure path instead of a branch per reason.

### D2. Dummy hash created at startup

`AccountService` creates the dummy with `passwordEncoder.encode(<random UUID>)` once, in its
constructor, and checks against it when there is no real hash.

- **Rejected: a hard-coded hash.** It would go out of date without notice if the encoder's
  cost or algorithm changes. A hash made by the encoder always has the current cost.
- **Rejected: a fixed sleep.** It only imitates the cost, and must be tuned per machine.

Encoding once adds one bcrypt run (roughly 100 ms) to application start.

### D3. An over-long password also runs one check

Today a password over 72 bytes skips bcrypt, because bcrypt reads only the first 72 bytes
and could accept it. The new flow checks it against the dummy hash and ignores the result.
A login can succeed only if the check ran against a real hash and the password is within
the limit. A result from the dummy never counts as a match, even by chance.

`changePassword` keeps its current check: its caller is logged in.

### D4. Test the hash check, not the elapsed time

R10-S1 spies on the `PasswordEncoder` bean and asserts one `matches` call per login, with
a hash of the same cost as `passwordEncoder.encode(...)` returns. Measuring time would be
flaky on a loaded machine. Fixture hashes use cost 4, so the cost assertion also proves
that these logins did not use a fixture's real hash. `SessionFailureTest` already uses a
Mockito spy or mock bean, so this needs no new test dependency.

### D5. Codes kept on other endpoints

`WRONG_PASSWORD` stays on change-password, and `ACCOUNT_DEACTIVATED` stays on the
password-link endpoint (reasons in the proposal). So both remain in `en.json`, and
`web-client/R5-S1` remains valid.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [Users cannot tell whether the email or the password was wrong] → The single message
  covers both and includes the set-password hint.
- [Password guessing stays unlimited] → Not made worse here. Needs its own change.
- [A stored hash with a different cost would still differ in time] → All stored hashes come
  from this encoder at its one cost. If the cost changes later, old hashes differ until they
  are re-hashed. That change must consider this.
- [`matches` may reject an over-long password by throwing] → R10-S1 and R2-S8 cover it; the
  implementation must keep them at 401.

## Migration Plan

Deploys with the frontend. No data change. Rollback is a revert.
