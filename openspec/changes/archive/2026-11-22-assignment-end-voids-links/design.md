# Design: assignment-end-voids-links

## Context

See proposal.md. Current state on `main`:

- `StudentRemovalService.removeStudent` (`@Transactional`): `users.findActiveByIdForUpdate(teacherId)` (none gives 401), `assignments.delete(teacherId, studentId)` (no row gives 404), `links.invalidateLinksIssuedBy(studentId, teacherId)`.
- `TeacherDeactivationListener.onTeacherDeactivated` (synchronous, in the deactivation transaction): `joinLinks.delete`, then `assignments.deleteAllOfTeacher` and a loop of `links.invalidateLinksIssuedBy(studentId, teacherId)`. The teacher row is already locked by `users.deactivate`.
- `PasswordLinkService.invalidateLinksIssuedBy(userId, issuerId)` calls `OneTimeTokenRepository.markUnusedIssuedByAsUsed(userId, issuerId, now)`: `update one_time_tokens set used_at = :now where user_id = :userId and issued_by = :issuerId and used_at is null`.

## Goals / Non-Goals

**Goals:** ending an assignment and voiding the links the teacher issued for that student are one operation, and the only way to delete from `teacher_students`; deactivation voids with one statement, not one per student; behaviour is unchanged.

**Non-Goals:** any product behaviour change, void on password change (#93), the #94 items, a trigger or cascade, new ways to end an assignment.

## Decisions

### D1. The component

`AssignmentEnding`, a Spring `@Component` in `com.teachdash.enrolment.persistence`, next to `TeacherStudentRepository` (reason in D3). Dependencies: `TeacherStudentRepository` and accounts' `PasswordLinkService`. Two public operations:

- `boolean end(UUID teacherId, UUID studentId)`: deletes the row; if a row existed, voids the unused links the teacher issued for that student, with the one-element set; returns whether a row existed. When no row existed it voids nothing: the caller answers 404 as before.
- `void endAll(UUID teacherId)`: deletes every assignment of the teacher with `delete ... returning student_id`, then, if the list is not empty, voids the unused links the teacher issued for those students in one statement.

Javadoc on the class and both methods: the teacher row must be locked in the current transaction (`select ... for update`) before the call; the component does not lock.

`@Transactional` is not put on the component: it runs in the caller's transaction, and a failure of the void propagates and rolls the caller's transaction back (R7-S4).

### D2. Set-based void

`OneTimeTokenRepository` gets a new method, `markUnusedIssuedByAsUsedForAll(Collection<UUID> userIds, UUID issuerId, Instant now)` (name is the implementer's call): `update one_time_tokens set used_at = :now where user_id in (:userIds) and issued_by = :issuerId and used_at is null` (the equivalent of `user_id = any(:ids)`; the form that works with the repository's parameter binding is the implementer's call). `markUnusedIssuedByAsUsed(userId, issuerId, now)` keeps its single-user form: `PasswordLinkService.store()` uses it for the one-live-link rule (a new link voids the issuer's older unused link for that user). Only the service method `PasswordLinkService.invalidateLinksIssuedBy(Collection<UUID> userIds, UUID issuerId)` becomes set-based, replacing the single-user one; its only caller becomes `AssignmentEnding`. The service returns without a query for an empty collection. The statement touches only tokens issued by this teacher, so admin-issued links and other teachers' links stay (R4-S13, R4-S16, R7-S5), and it marks tokens of any purpose, as today.

### D3. Only `AssignmentEnding` deletes assignments

Constitution §3 puts repositories in `persistence`, and a Java package-private method is visible only inside its own package. So `TeacherStudentRepository.delete` and `deleteAllOfTeacher` become package-private, and `AssignmentEnding` lives in the same package, `com.teachdash.enrolment.persistence`. This is the simplest Java means: no new dependency (an architecture-test library would fall under §7), no new test infrastructure, and the compiler enforces it. Tests that spy `TeacherStudentRepository` (for example `JoinLinkFailureTest`) must keep working with package-private methods; if the Spring proxy or the spy breaks that, the fallback is a public method on a final class, or a documented convention, and the implementer reports it. Javadoc on both delete methods says they are package-private on purpose and callers use `AssignmentEnding`; there is no reflection test. Cost: a component that coordinates a repository and another feature's service sits in `persistence`, not `domain`. This deviates from §3 (a service in `persistence`) and adds the dependency `enrolment.persistence` to `accounts.domain`; `AssignmentEnding` is the only class in `persistence` allowed to call another feature's service. Both callers stay in `domain` and call the public component.

### D4. Callers

- `StudentRemovalService.removeStudent`: lock the teacher row as today, then `if (!ending.end(teacherId, studentId)) throw ApiException.notFound()`. The `TeacherStudentRepository` and `PasswordLinkService` fields give way to `AssignmentEnding`; the `UserRepository` field stays for the lock. The comment about the concurrent reset stays: it explains why the lock is taken.
- `TeacherDeactivationListener`: `joinLinks.delete(teacherId)`, then `ending.endAll(teacherId)`. The `assignments` and `links` fields go. The class Javadoc "runs synchronously in the deactivation transaction" stays.

### D5. Locks

Unchanged and owned by the callers. Removal locks the teacher row with `findActiveByIdForUpdate`; deactivation holds it through `users.deactivate`, which runs before the event. Order: teacher row, then the assignment row (delete), then the token rows (update). A concurrent reset by the same teacher takes the teacher row first, so it cannot insert a link between the delete and the void.

### D6. Tests

No test covers the precondition that the teacher row is locked: the component does not check it, and the Javadoc is the only guard.

- `TeacherDeactivationFailureTest` (R7-S4): the spy on `PasswordLinkService` now throws from the set-based `invalidateLinksIssuedBy`; the assertions are the same (500, the teacher can log in, the student is still listed). `TeacherDeactivationJoinLinkFailureTest` (R8-S9) has the same mock and the same update, so it keeps failing the deactivation. These two are the only edits to existing tests.
- One new test in `TeacherDeactivationTest`, without a scenario ID (no spec to name): teacher A with two students that have unused set-password links issued by A, a third user with a link issued by A who is not A's student, an admin link for one of A's students, and teacher B with a student and a link. After deactivation, only A's links for A's two students are void; the third user's link, the admin link and B's link still work. R7 alone does not cover the unassigned user's link (see Clarifications).
- The R4 and R7 tests are otherwise untouched; the proof is the green suite and `git diff` over them.

## Dependencies

None added.

## Risks / Trade-offs

- **Component in `persistence`.** It reaches into accounts' service from there. Accepted for a compiler-enforced "only deleter"; moving it to `domain` would need public delete methods and a convention instead.
- **Mock-based failure tests.** They depend on the name and the arguments of the spied method; D6 updates the two that do.
- **Large id sets.** A teacher with very many students sends one `in (...)` list; the limit is far above what one teacher manages, and the old loop issued one statement per student.

## Migrations

None.
