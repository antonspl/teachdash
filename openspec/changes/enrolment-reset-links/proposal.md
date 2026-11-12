# Proposal: enrolment-reset-links

## Clarifications

Questions asked on 2026-11-08, 2026-11-09 and 2026-11-12, and the engineer's answers.
Scope follows issue #30 and its comments: after #62 a student has at most one teacher, so
the multi-teacher concern from #29 is gone. The two follow-ups from the review of #80 are
part of this change.

| # | Question | Answer |
|---|----------|--------|
| 1 | Removal and release invalidate only set-password links, so a teacher-issued reset link would survive and let the removed teacher log in as the student. Record who issued each link and void the removing teacher's links, keeping an admin's (R4-S13, R7-S5)? Or void every unused link on removal? | Record the issuer and void that teacher's links; an admin's link stays (option A). |
| 2 | Existing link rows have no issuer. Backfill them? | No real data exists yet, so the migration may delete them. |
| 3 | Include the two follow-ups from #80 here: the deactivated-account guard inside `PasswordLinkService`, and a separate error code for the 409 "target account is deactivated"? | Both here. |
| 4 | Name of the new code? | `TARGET_ACCOUNT_DEACTIVATED`. |
| 5 | How does `PasswordLinkService` learn the issuer: an explicit argument or the security context? | Explicit argument. The two approved mocks of `issue(any(), any())` are updated in pass 2 with the engineer's approval at that point. |
| 6 | The admin page never offers a reset link for a deactivated teacher, so only a stale list can get the renamed 409. Add an i18n string for it? | No. The change stays backend-only; the UI shows the generic message in that case. |
| 7 | A teacher can issue a reset link for an own student, use it themself and so know the password; after removal the former teacher keeps that password and session. Voiding unused links does not close this. | Accepted risk. Later the link will be sent to the student by email instead of being shown to the teacher. |
| 8 | The race between a reset and a removal by the same teacher is closed by a teacher-row lock in removal, but no test fails if the lock is forgotten. How is it checked? | By the reviewer, with a code comment at each lock, as in earlier changes. |

Assumptions (the engineer may override during review):

- A teacher may issue a reset link for an own student who has no password yet. It works
  like the admin's reset link for such an account, so a teacher who lost the set-password
  link can send a new one.
- The teacher's link has the same rules and effects as an admin's reset link (accounts R7
  and R8): it voids the student's older unused link, including an admin's, and using it
  ends the student's other sessions.
- Join (R9, R10) issues no password link, so it needs no issuer.
- Students cannot be deactivated (accounts R6), so the teacher endpoint never returns
  `TARGET_ACCOUNT_DEACTIVATED` and R12 has no scenario for it. The guard still runs on the
  teacher path, because it lives in the shared mechanism.

## Research

Two angles, agreed with the engineer on 2026-11-12.

- **Password link service on main.** The draft design still fits main after #69, #78 and
  #80: V6 is the next migration, the lock order is teacher row, student row, links, and no
  deadlock was found between teacher reset, removal, deactivation and admin reset.
  `PasswordLinkService.issue` has four callers and no check that the account is active;
  the admin path locks the user row twice. No domain code reads the security context
  today, and controllers already pass the acting user's id down, which is why the issuer
  is an explicit argument (answer 5). Removal takes no lock today and needs the
  teacher-row lock (design D3).
- **Frontend and the 409 code.** No frontend code compares against `ACCOUNT_DEACTIVATED`;
  messages come from `errors.<code>` with a generic fallback (web-client R5-S2). The admin
  page offers no link for a deactivated teacher (web-admin-teachers R5-S3), and no web
  scenario mentions the 409. So the rename needs no frontend work (answer 6).

## Why

A teacher cannot help an own student who forgot the password; only an admin can issue a
reset link. Once teachers can issue reset links, removal must stop the removed teacher
from keeping an unused one, or that teacher could later use it to log in as a student who
has left them. A link the teacher already used is a different, accepted risk (answer 7).

This change adds the second caller of the reset-link mechanism, so the rule "no reset link
for a deactivated account" moves into the mechanism, where a new caller cannot forget it.
`ACCOUNT_DEACTIVATED` has two meanings today (401: your own account; 409: the account you
act on), which a client that branches on the code cannot tell apart.

## What Changes

- New: a teacher issues a password reset link for a student assigned to them. Any other
  id gets 404.
- Every password link records who issued it.
- Removal (R4) and release on deactivation (R7) invalidate the student's unused links
  that this teacher issued, of any kind. Before, they invalidated set-password links only.
  An admin's link still stays usable.
- **BREAKING** (API): an admin's reset-link request for a deactivated account returns 409
  `TARGET_ACCOUNT_DEACTIVATED` instead of 409 `ACCOUNT_DEACTIVATED`. The 401
  `ACCOUNT_DEACTIVATED` on login and link use is unchanged.
- Internal: reset links are issued through one guarded entry point in
  `PasswordLinkService`, used by the admin path and the teacher path.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `student-enrolment`: new requirement R12 (teacher issues a reset link); R4 and R7
  invalidate the links the teacher issued instead of set-password links.
- `accounts`: R8 returns `TARGET_ACCOUNT_DEACTIVATED` for a deactivated target account.

## Out of scope

- Frontend: the teacher's screen for the reset link (separate change) and an i18n string
  for `TARGET_ACCOUNT_DEACTIVATED` (answer 6).
- Showing who issued a link, or any audit log.
- Rate limiting reset-link requests.
- Sending the link to the student by email, and any protection against a teacher who
  uses the link themself (answer 7).
- Deactivating students.
- Any other change to the admin's reset link (accounts R8).

## Impact

- Flyway migrations: `issued_by` on `one_time_tokens` (design D1).
- New teacher endpoint `POST /api/teacher/students/{id}/reset-link`.
- `PasswordLinkService`: issuer argument on issuing, guarded `issueResetLink`,
  invalidation by issuer (design D2, D4, D6). `AccountService.createPasswordlessUser`
  and the admin and registration paths pass the issuer.
- Removal locks the teacher row first (design D3).
- Test helper `createToken` takes the issuer; existing callers pass the issuer their
  scenario describes. The approved test for accounts R8-S4 expects the new code.
- No new dependencies.
