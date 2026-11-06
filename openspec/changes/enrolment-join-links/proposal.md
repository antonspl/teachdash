# Proposal: enrolment-join-links

## Clarifications

Questions asked on 2026-11-06 and 2026-11-07, and the engineer's answers. Scope follows
issue #29 as narrowed on 2026-11-05 after #62 (a student has at most one teacher).

| # | Question | Answer |
|---|----------|--------|
| 1 | May a logged-in student with no teacher join through a link, or is the link for new visitors only? | A student with no teacher is assigned. A student who has a teacher gets 409 (issue comment, 2026-11-05). |
| 2 | Does a visitor set a password in the join form, or get a set-password link? | They set the password in the form. |
| 3 | Does the join form ask for a time zone, like teacher registration? | Yes: name, email, time zone and password. |
| 4 | Is the link generated on first view, or only when the teacher asks? | Only when asked. Viewing has no side effects and shows "no link" when there is none. Generating replaces the old link. |
| 5 | How is the public endpoint rate limited? | Per client IP, in the backend, with no new dependency. 429 `RATE_LIMITED` with `Retry-After`. Limits are configurable. |
| 6 | Is a new visitor logged in right after joining? | Yes, like after setting a password through a link. |
| 7 | Is email verification (#46) in scope? | No. The gap is accepted until #46 lands. |
| 8 | Can the teacher see the link again later, or only when it is generated? | Again at any time. The token is stored in plain text. |
| 9 | What does an unknown, replaced or disabled link, or a deactivated teacher's link, answer? | The same answer for all of them: 410 `LINK_INVALID`, like password links. |
| 10 | Does a link expire? | No. It works until it is regenerated or disabled (accepted risk in #29). |
| 11 | What happens to the link when the teacher is deactivated? | It is disabled. Reactivation does not bring it back, as with students (student-enrolment R7). |
| 12 | Which 409 codes? | The existing ones: `EMAIL_TAKEN` for an email with an account, `STUDENT_EXISTS` for a student with another teacher, `ALREADY_ENROLLED` for a student of the same teacher. |
| 13 | Can a visitor see who they are joining before they register? | Yes. A public preview returns the teacher's name. |
| 14 | Does the real client IP reach the backend in prod? | Unknown. Recorded as a risk to check after deploy (design D6). |

## Research

Angles agreed with the engineer, and their conclusions:

- **A. Existing mechanics.** Nothing for join links exists. `one_time_tokens` does not
  fit: it allows one unused token per user and every password flow voids all of them, so
  a new table is needed. Reused as they are: the name, email, time zone and password
  validators, the session start used after setting a password, the existing error codes,
  and the lock order of registration (teacher row, then student row). The unique index on
  the student in `teacher_students` already enforces one teacher per student.
- **B. Rate limiting.** No limiting exists anywhere. A small in-memory per-IP limiter in
  the backend fits one VPS and one backend instance, needs no dependency, and is testable
  with a low configured limit. Bucket4j adds a dependency for one endpoint; Caddy would
  need a custom build and cannot be tested with the backend tests.
- **C. Old spec text** (`student-enrolment` branch, PR #2). Two of its seven scenarios
  assume several teachers per student and are dropped. The rest are kept in substance and
  reworded; most of the new scenarios had no old equivalent.

Assumptions (the engineer may override during review):

- The link is checked before anything else about the visitor, so a 409 about an email is
  given only to someone holding a valid link.
- A logged-in student joins with the token alone; other fields in the request are ignored.
- Login and set-password-by-link are not rate limited in this change; a follow-up issue
  covers them.
- The web client shows its generic error message for `RATE_LIMITED` and `LINK_INVALID`
  on the join path until the frontend change adds screens (web-client R5).
- accounts R6 and student-enrolment R7 are not changed to mention the join link, to stay
  within six requirements; R8 states that deactivation disables the link.

## Why

A teacher can only add students by typing their details. A join link lets the teacher
hand out one link and have students register and be assigned on their own, and lets a
student whose teacher removed them join a new teacher.

## What Changes

- Each teacher has at most one join link. The teacher can view it, generate a new one
  (the old one stops working) and disable it. Deactivating the teacher disables it.
- A visitor without a session can preview a link (the teacher's name) and register
  through it with name, email, time zone and password. The new student is assigned to
  the teacher and logged in.
- A logged-in student with no teacher who uses a link is assigned to that teacher.
- The public preview and join endpoints are rate limited per client IP.
- The join endpoints are added to the endpoints that need no session (accounts R4).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `student-enrolment`: new requirements R8 (teacher manages the join link), R9 (visitor
  joins through the link), R10 (student with no teacher joins), R11 (join endpoints are
  rate limited). The Purpose gains a sentence about join links at archive.
- `accounts`: R4 lists the join-link preview and join among the endpoints that need no
  session.

## Out of scope

- Frontend screens (separate change).
- Email verification (#46).
- Rate limiting login and set-password-by-link (follow-up issue).
- Per-email or per-link rate limits.
- Link expiry, and more than one link per teacher.

## Impact

- New Flyway migration: table `teacher_join_links`.
- New teacher endpoints under `/api/teacher/join-link` and public endpoints under
  `/api/join-links`.
- `SecurityConfig`: the public join endpoints need no session. CSRF still applies.
- Teacher deactivation also disables the teacher's join link, in the same transaction.
- A new servlet filter for rate limiting, with configuration properties.
- No new dependencies.
