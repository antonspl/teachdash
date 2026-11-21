# Proposal

## Clarifications

Issue #90. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | What is the change? | A pure refactor with no product behaviour change. Today two paths end an assignment, and both void the unused links the teacher issued for the student: `StudentRemovalService` (lock the teacher row with `users.findActiveByIdForUpdate`, `assignments.delete`, then `links.invalidateLinksIssuedBy(studentId, teacherId)`) and `TeacherDeactivationListener` (`deleteAllOfTeacher`, then a loop of `invalidateLinksIssuedBy`). The risk is structural: a future path (for example the unassign in #122 or an account deletion) could delete the assignment and forget the void. |
| 2 | Spec deltas? | None: `skip_specs: true`. Constitution §1 allows it for a change that does not change product behaviour; its examples are tooling, and this is the first product-code refactor to use it. The proof is that the existing scenarios stay green unchanged: student-enrolment R4 (R4-S3, R4-S11, R4-S13, R4-S15, R4-S16 and the others) and R7 (R7-S1, R7-S4, R7-S5, R7-S7). The only test change is the mock in decision 5. |
| 3 | Design? | One enrolment component (name `AssignmentEnding`, the writer's call) with two operations: end one assignment (teacher, student), which deletes the row, voids the unused links that teacher issued for that student and returns whether a row existed; and end all assignments of a teacher, which deletes them all and voids those links set-based, with no per-student loop (`delete ... returning student_id`, then one update with `user_id = any(:ids)` and `issued_by = :teacher`, or equivalent single statements). It is the only code that deletes from `teacher_students`: the `TeacherStudentRepository` delete methods are restricted to it with the simplest Java means. Both callers use it. |
| 4 | Locks? | They stay with the callers. The component requires the teacher row to be locked already: removal locks it, deactivation locks it through `users.deactivate`. Javadoc and design say so. Lock order unchanged: teacher row, then assignment, then links. |
| 5 | Tests? | `TeacherDeactivationFailureTest` mocks `links.invalidateLinksIssuedBy` to throw and checks the rollback; it is updated so that the new set-based void fails instead, with the same assertion. This is the only change to an existing test. One small new test (no scenario ID) proves that the set-based void touches only the teacher's own links for the teacher's own students when the teacher has several students, unless R7 already covers it. |
| 6 | Out of scope? | #93 (void on password change), the #94 items (an index on `issued_by`, the lock helper), and any database trigger or foreign key cascade. A cascade is rejected: tokens are marked used, not deleted; admin links have no assignment; the project has no triggers. |

Spec-writer's check for decision 5, and one finding:

- R7-S1 has teacher A with two students (S1 with a password, S2 with a teacher-A link) and teacher B with a student and a link; R7-S5 covers an admin's link, R7-S7 the teacher's reset link, R4-S11 a teacher's other students. None has a link issued by the teacher for a user who is not the teacher's student. The new test covers that case together with two students with links, so it is not fully covered by R7.
- `TeacherDeactivationJoinLinkFailureTest` (R8-S9) mocks the same `invalidateLinksIssuedBy(any(), any())` to throw. It needs the same update as `TeacherDeactivationFailureTest`, or it stops failing the deactivation. This is the same mock, not another behaviour; it is listed in design and tasks.

## Research angles

The engineer agreed no separate research was needed. The research conclusions:

- No other code path deletes assignments: there is no account deletion and no reassign; `assignExisting` and `JoinLinkService` only insert. The foreign keys on `teacher_students` (V3) have no `ON DELETE`.
- `PasswordLinkService.invalidateLinksIssuedBy` calls `OneTimeTokenRepository.markUnusedIssuedByAsUsed(userId, issuerId, now)`, which marks unused tokens used, of any purpose. Admin-issued links are untouched (R4-S16, R7-S5).
- The enrolment feature already depends on accounts' `PasswordLinkService`.

## Why

An assignment and the links its teacher issued for the student must end together. Today two callers each repeat the pair "delete the row, void the links", in two shapes (one student, then a loop). A new path that deletes from `teacher_students` directly would leave a teacher's reset link alive for a student the teacher no longer manages, and nothing would fail. One component that is the only deleter makes the void part of ending an assignment.

## What Changes

- New `AssignmentEnding` in the enrolment feature: `end(teacherId, studentId)` and `endAll(teacherId)`.
- `StudentRemovalService` and `TeacherDeactivationListener` call it; the per-student loop goes away.
- `TeacherStudentRepository.delete` and `deleteAllOfTeacher` become usable only by `AssignmentEnding`.
- `PasswordLinkService.invalidateLinksIssuedBy` and its repository query take a set of student ids, so the void is one update.
- No API, database or behaviour change. No migration.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. This is a refactor with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`. There are no spec deltas and no new scenarios; archive merges nothing into `openspec/specs/`.

## Out of scope

- #93 (void on password change) and the #94 items (an index on `issued_by`, the lock helper).
- A database trigger or foreign key cascade (rejected, see Clarifications 6).
- New ways to end an assignment (#122 and others); they will use the component.

## Impact

- Backend: `enrolment` (`AssignmentEnding`, `StudentRemovalService`, `TeacherDeactivationListener`, `TeacherStudentRepository`), `accounts` (`PasswordLinkService`, `OneTimeTokenRepository`).
- Tests: `TeacherDeactivationFailureTest` and `TeacherDeactivationJoinLinkFailureTest` mocks; one new test.
- Database, API, dependencies: none.
