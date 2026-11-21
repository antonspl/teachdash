# Proposal

## Clarifications

Issue #109. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Requirements and endpoints? | `parts` R3 (rename), R4 (move to a position), R5 (delete), with the same endpoint shapes, error codes, order of checks and `updatedAt` rules as lessons R3-R5. `PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}` with `{title}` gives 200 with the R1 part body; the title follows R1; `updatedAt` is set also when the title is unchanged. `PATCH .../parts/{partId}/position` with `{position}` gives 200 with the part body; `position` is an integer with strict coercion as lessons R4, clamped to 1..count of the parts in the lesson; only the moved part's `updatedAt` changes, also on a same-position move; shifted parts keep theirs. `DELETE .../parts/{partId}` gives 204; later parts move up by one without gaps; survivors keep `updatedAt`; a deletion frees a slot under the 20-part limit (a later create gets 201 at position 20); a second delete gives 404. The `updatedAt` of the lesson and of the course never changes on a part write. Order of checks: CSRF and role (403), deactivated (401), malformed id (404), input (400), unknown or foreign id (404); delete has no input step. |
| 2 | What does "cascade" mean here? | Parts have no child tables yet. Delete removes the part and closes the gap. Later child tables (items, files, links) bring their own `ON DELETE CASCADE` foreign key in their own changes. No placeholder tables now. |
| 3 | Position integrity and locks? | All three writes, rename included, lock the user row, then the course row, then the lesson row, as parts create does, and then look the part up scoped by lesson and teacher. No new constraint: the existing deferrable unique `(lesson_id, position)` from `V16` stays as the safety net and lets the move shift work as for lessons. No migration. |
| 4 | Scenarios? | About 20 in total (issue target: 3 requirements, about 20 scenarios), in `en` and `ru` wherever content is handled. Each requirement tests another teacher's access at every level it touches: a 404 matrix for course, lesson and part (foreign, unknown, malformed, a part of another lesson of the same teacher, a lesson under another course), and the responses never leak the foreign titles. Concurrency: R4 parallel moves keep the positions 1..n distinct (as lessons R4-S8); R5 mixed parallel deletes and moves (as lessons R5-S7), and parallel deletes racing creates keep positions without gaps and within the limit, with no 500. Lessons-style scenarios are merged to stay near 20: title normalisation into the Russian rename scenario, the same-title `updatedAt` bump into the English rename scenario, the single-part move into the clamp scenario, input-before-lookup into the check-order scenario. |

After the spec critique, R4 has 8 scenarios (the roles and the check-order scenarios are split, as in R3), so the change has 21 scenarios: R3 6, R4 8, R5 7. The parallel scenarios claim only what they prove: no 500, allowed codes, positions 1..n without gaps, at most 20 parts.

## Research angles

The engineer agreed no separate research was needed. The research conclusions came from the lessons code (`com.teachdash.lessons`) and the parts code from #108, which this change mirrors:

- No `SecurityConfig` change: `/api/teacher/**` is already TEACHER.
- The move algorithm is `LessonService.move`: clamp with `BigInteger`, shift the range by one, set the moved row. Delete returns the position of the deleted row, then closes the gap.
- The lock order user, course, lesson matches the lessons writers (user, course) and the lesson delete that cascades to parts, so there is no deadlock cycle.
- Code to touch: `parts/api/PartController` (three mappings), `parts/api/dto` (`RenamePartRequest`, `MovePartRequest` with `BigInteger`), `parts/domain/PartService` (`rename`, `move`, `delete`, one lock helper), `parts/persistence/PartRepository` (lookup scoped by lesson and teacher, `updateTitle`, `shiftRange`, `setPosition`, delete returning the position, close the gap).
- Tests: `RenamePartTest`, `MovePartTest`, `DeletePartTest` in `backend/src/test/java/com/teachdash/parts/api/`, with the helpers `createPart`, `countParts`, `partPositions`, `Parallel`, `MutableClock` from `support/ApiTest`; a small helper to read one part's `updatedAt` may be needed.

## Why

Since #108 a teacher can create and list parts but not fix them: a typo or a wrong order can be fixed only by deleting the whole lesson, and a lesson at the 20-part limit cannot shrink. Items (#110), files and links hang off parts and need parts that can be edited.

## What Changes

- `PATCH /api/teacher/courses/{courseId}/lessons/{lessonId}/parts/{partId}` renames a part (R3).
- `PATCH .../parts/{partId}/position` moves a part to a position in its lesson, clamped to 1..count (R4).
- `DELETE .../parts/{partId}` deletes a part and closes the gap in positions (R5).
- No migration and no new error code.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `parts`: adds R3 rename, R4 move, R5 delete. R1 and R2 keep their text.

## Out of scope

- Items and exercises (#110), files (#55) and links of a part, and their delete cascade: each later table brings its own `ON DELETE CASCADE` foreign key.
- Moving a part to another lesson, copying parts.
- Changes to lesson or course behaviour.
- Frontend and OpenAPI.

## Impact

- Backend: `parts` package (controller, service, repository, two request records); `PartResponse` is reused.
- Database: none.
- API: three new endpoints; no new error code.
- No new dependencies.
