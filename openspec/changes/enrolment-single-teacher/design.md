# Design: enrolment-single-teacher

## Context

`teacher_students` (V3) has the primary key `(teacher_id, student_id)` and a plain index on
`student_id`, so a student may have any number of teachers. Registration
(`StudentRegistrationService`) returns 409 for every existing email. Removal
(`StudentRemovalService`) invalidates the student's unused SET_PASSWORD link only when no
assignment is left. See proposal.md for the motivation.

## Goals / Non-Goals

**Goals:**
- The database enforces at most one teacher per student, so no code path or race can
  create a second assignment.
- Registration of a student with no teacher reuses the existing account.

**Non-Goals:**
- No change to the list endpoints or their response shapes.
- No issuer on `one_time_tokens`. With one teacher, nothing needs it yet (#30 decides).

## Decisions

### D1. Unique index on the student

Migration `V4__one_teacher_per_student.sql`:

```sql
drop index teacher_students_student_idx;
create unique index teacher_students_student_uq on teacher_students (student_id);
```

The primary key stays, so lookups by `(teacher_id, student_id)` keep using it. The new
index replaces the old plain index. Nothing is deployed, so no existing rows can break it.

Alternative: make `student_id` the primary key. Rejected because lookups by teacher and
student would lose their index, and the meaning of the table does not change.

### D2. Registering an existing student

In one transaction, as today:

1. A user with that email that is not a student → 409 `EMAIL_TAKEN`.
2. Assigned to this teacher → 409 `ALREADY_ENROLLED`.
3. Assigned to another teacher → 409 `STUDENT_EXISTS`.
4. Otherwise insert the assignment with `insert ... on conflict do nothing`.
   - If no row was inserted, a parallel request won the race. Check again: assigned to
     this teacher → `ALREADY_ENROLLED`, otherwise `STUDENT_EXISTS`.
   - `on conflict do nothing` keeps the transaction usable for that check.
   - Under READ COMMITTED, the insert waits for the other transaction to commit, so the
     check sees its row.
5. If the student has no password, issue a set-password link with the existing
   `PasswordLinkService.issue`, in the same transaction. A failure there rolls back the
   assignment (R1-S17).

The account's name, email and time zone are never changed by this path.

A new email keeps today's path. The new account's assignment uses the same insert. It
cannot conflict, because the account was created in the same transaction.

Alternative: catch `DuplicateKeyException` from a plain insert. Rejected because Postgres
aborts the transaction, so the check in step 4 would need a second transaction.

### D3. Response

Always 201 with `CreatedAccount(id, setPasswordLink)`. `setPasswordLink` is omitted from
the JSON when no link was issued (`@JsonInclude(NON_NULL)` on the record). The controller
does not change.

Alternative: 200 for an existing account. Rejected because it needs a flag through the
service and controller and gives a client nothing it needs.

### D4. Removal

`StudentRemovalService` drops the `existsForStudent` check and always invalidates the
student's unused SET_PASSWORD links. It still targets only SET_PASSWORD links, so an admin's
reset link survives (R4-S13).

### D5. Tests

- `ApiTest.assignStudent` stays a direct insert. A test that assigns a second teacher now
  fails on the unique index, which flags any leftover multi-teacher setup.
- The tests for R3-S7, R4-S2, R4-S12, R5-S3, R5-S4 and R5-S5 are deleted.
- The tests for R1-S2, R1-S5, R1-S7, R1-S9, R3-S3, R3-S8, R4-S3, R4-S7, R4-S13 and
  R5-S1 are rewritten to the new scenarios. Their GIVEN steps change to one
  teacher.
- R1-S14 to R1-S18, R3-S9, R4-S14 and R7-S1 to R7-S3 are new.
- R1-S17 uses the same failure injection as R1-S12.
- R1-S16 uses the same parallel-request helper as R1-S13.

### D7. Deactivation releases students

`TeacherAdminService.deactivateTeacher` (accounts) publishes a `TeacherDeactivated` event
inside its transaction. An enrolment listener (`@EventListener`, synchronous, so it runs in
the same transaction) deletes the teacher's assignments and invalidates each released
student's unused SET_PASSWORD link, using the same queries as removal (D4). A failure rolls
back the deactivation.

Enrolment already depends on accounts (`PasswordLinkService`), so a direct call from
accounts into enrolment would make the packages depend on each other. The event keeps the
dependency one way. Spring's event publisher is already on the classpath.

Alternative: `on delete cascade`-style cleanup in SQL. Rejected because deactivation does
not delete the user, and the link invalidation needs code anyway.

### D6. Constitution §8

The constitution changes in this PR, with the owner's approval recorded in the proposal:

- "The platform serves several teachers from day one (a small number, not thousands)"
  becomes "(about 1000 teachers)".
- "A student may study with more than one teacher and sees data from each of them" becomes
  "A student has at most one teacher."

### Flyway migrations

- `V4__one_teacher_per_student.sql` (D1).

No new dependencies.

## Risks / Trade-offs

- [Any teacher can claim a student with no teacher by knowing their email] → They see only
  the name, email and time zone. The previous teacher's data stays private to that teacher.
  The owner accepted this (proposal, answer 2).
- [Claiming a student who never set a password hands the claiming teacher a set-password
  link, so that teacher can log in as the student] → Such an account was never used, so it
  holds nothing of the student's. This is the same power the registering teacher has today.
- [Issuing a new link voids an admin's unused reset link for a student without a password]
  → Both links set the password, and the student gets the new one from the teacher.
- [A removal that runs in parallel with another teacher's registration can make the
  registration return 409 `STUDENT_EXISTS`] → The student has no teacher afterwards, and
  the teacher can retry.
- [The scenarios R4-S2, R4-S12 and R5-S5 and their tests are deleted] → They describe
  behaviour that no longer exists. The R4 and R5 blocks are replaced as a whole, because
  OpenSpec cannot drop scenarios from a modified requirement. R3 is replaced the same way.
  At archive, R3, R4 and R5 are moved back after R2 in the main spec.
- [A reactivated teacher has lost their students] → Accepted by the owner (proposal,
  answer 6). Their teacher-owned data stays theirs; they re-register students who have not
  moved to another teacher.

## Migration Plan

Nothing is deployed. V4 runs on start. Rollback would be a new migration that restores the
plain index.
