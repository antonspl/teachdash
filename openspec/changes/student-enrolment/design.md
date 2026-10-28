# Design: student-enrolment

## Context

Builds on `accounts`: users, roles, sessions, one-time link service, `ApiTest`.

## Decisions

### D1. Package
`com.teachdash.enrolment`. Uses the accounts one-time link service for set-password
and reset links; does not duplicate it.

### D2. Per-teacher scoping
Every teacher query joins on `teacher_students` with the current teacher's id.
A student not linked to the current teacher is 404, never 403, so a teacher cannot
probe other teachers' students.

### D3. Join links
Token = 32 random bytes, base64url, stored in plain text: the teacher must see the
link again, and it only grants "become this teacher's student". Regenerate replaces
the row; disable deletes it. URL: `{APP_BASE_URL}/join/{token}`. Unknown, replaced or
disabled tokens all return 404.

### D4. API (all under `/api`)

| Method | Path | Role | Spec |
|---|---|---|---|
| GET/POST | `/teacher/students` | teacher | R1, R4 |
| GET/DELETE | `/teacher/students/{id}` | teacher | R4, R5 |
| POST | `/teacher/students/{id}/reset-link` | teacher | R8 |
| GET/POST/DELETE | `/teacher/join-link` | teacher | R2 |
| POST | `/join/{token}` | none or student | R3 |
| GET | `/student/teachers` | student | R6 |
| GET | `/admin/teachers/{id}/students` | admin | R7 |

`POST /join/{token}` without a session takes name, email, password, time zone and
starts a session; with a student session it takes no body.

### D5. Flyway migration
- `V3__enrolment.sql` — `teacher_students` (teacher_id, student_id, created_at, PK
  both, FKs to users), `join_links` (teacher_id PK, token unique, created_at).

No new dependencies.

## Risks / Trade-offs

- [Teacher can reset the password of a student shared with another teacher] →
  acceptable with a few trusted teachers; revisit (admin-only or student-confirmed)
  if teachers become untrusted.
- [Join link tokens stored in plain text] → grant only enrolment; can be regenerated
  at once.
