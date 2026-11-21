# Proposal

## Clarifications

Issue #122. The engineer's decisions, as given:

| # | Question | Engineer's answer |
|---|---|---|
| 1 | Dependencies? | Stacked on #182 (`assignment-end-voids-links`, issue #90); rebase after it merges. #121 (frontend removal warnings) is not waited for: before M2 only the owner's data exists, and the dialogs come with #121. |
| 2 | Table? | `course_assignments(course_id, teacher_id, student_id, created_at)`, primary key `(course_id, student_id)`. Composite foreign key `(course_id, teacher_id)` to `courses (id, teacher_id)` on delete cascade (a unique `(id, teacher_id)` is added to `courses`), composite foreign key `(teacher_id, student_id)` to `teacher_students (teacher_id, student_id)` on delete cascade, index on `(teacher_id, student_id)`. Groups-ready: this table holds direct per-student assignments; groups (#54) later add their own table (for example group courses) without changing this one. Migration `V17` (PR #181 also uses `V17`; whichever merges second renumbers, as the issue says). |
| 3 | Cleanup on student removal and teacher deactivation? | Done by the database. Removing a student and deactivating a teacher already delete `teacher_students` rows in one place (`AssignmentEnding`, #90); the foreign key cascade deletes the course assignments in the same statement, so no code path can forget it. `AssignmentEnding` is unchanged. Later tables (lesson access #123, answers #128) can cascade from `course_assignments` the same way. These data rules are ADDED requirements in `course-assignment` (R3); there is no MODIFIED requirement of `student-enrolment` R4 or R7. There are scenarios for removal and for deactivation. |
| 4 | Assign? | `PUT /api/teacher/courses/{courseId}/students/{studentId}`, no body, idempotent: 204 with no body whether the student was newly assigned or already assigned (`insert ... on conflict do nothing`); a repeat keeps the original `created_at`. Checks: CSRF and role (403), deactivated teacher (401), malformed id (404), unknown or foreign course (404), unknown student, another teacher's student or an id that is not a student (404). No limit on the students of a course: it is bounded by the teacher's own students. |
| 5 | Read? | `GET /api/teacher/courses/{courseId}/students` gives 200 with an array of `{id, name, assignedAt}` and nothing else (no email, no `teacherId`), ordered as student-enrolment R2 (`name collate "und-x-icu", id`), unpaginated. A foreign, unknown or malformed course gives 404. Check order as the lessons list (lessons R2): role (403), deactivated (401), malformed id (404), unknown or foreign course (404); a GET has no CSRF check. |
| 6 | Unassign? | `DELETE /api/teacher/courses/{courseId}/students/{studentId}` gives 204 if the assignment existed; 404 `NOT_FOUND` if the course is not the caller's, the student is not the caller's, or the caller's own student is not assigned to the caller's own course. Unassigning one course does not touch the student's other assignments. |
| 7 | Locks? | Assign and unassign lock the teacher row (`findActiveByIdForUpdate`, none gives 401), then the course row (scoped by `teacher_id`, none gives 404), then check the student through `teacher_students` (none gives 404). Same order as `LessonService` and `StudentRemovalService` (teacher row first), so an assign racing a student removal runs either before (the cascade removes the row) or after (it gets 404). A parallel scenario proves that the race ends with no assignment of a removed student. |
| 8 | Requirements and scenarios? | R1 a teacher assigns an own course to an own student (idempotency, scoping at course and student level, race with removal, check order). R2 a teacher reads the students assigned to an own course (`en` and `ru` names, order, only that course, empty list, no leak of another teacher's data). R3 a teacher unassigns; assignments also end with the student's removal and the teacher's deactivation (removal deletes all assignments of that student; deactivation deletes all assignments of the teacher's courses; other teachers' data is untouched). Scenarios cover `en` and `ru` content: course titles and student names in Cyrillic. Target about 20 scenarios; the change has 21. |
| 9 | Out of scope? | Lesson access (#123), student endpoints (#124), answers (#126, #128), course delete (#131), frontend (#132, #121). |

## Research angles

The engineer agreed no separate research was needed. The research conclusions came from the code and the specs:

- `courses` (`V11`) has `id`, `teacher_id` (foreign key to `users`, `on delete cascade`); nothing deletes courses yet. A unique `(id, teacher_id)` is needed as the target of the composite foreign key.
- `teacher_students` (`V3`, `V4`) has primary key `(teacher_id, student_id)`, unique `student_id`, and no cascades. `AssignmentEnding.end` and `endAll` are its only deleters, and their callers hold the teacher row lock.
- `GET /api/teacher/students` orders by `name collate "und-x-icu", id`; a foreign student is 404 (student-enrolment R2).
- `lockCourse(courseId, teacherId)` exists in `LessonRepository` and `PartRepository`; the new code reuses the lessons one rather than adding a third copy (design D3).
- Test helpers in `support/ApiTest`: `createTeacher`, `createStudent`, `createAdmin`, `assignStudent`, `createCourse`, `loggedInAs`, `countRows`; `assignCourse` and a read of the assignment rows are added.

## Why

Since #105 a teacher can create and read courses, and since #54-era enrolment can have students, but nothing links the two: a student cannot be given a course. Lesson access (#123), student endpoints (#124) and answers (#128) all hang off "this student takes this course", so the link comes first.

## What Changes

- New table `course_assignments` (migration `V17`) and a unique `(id, teacher_id)` on `courses`.
- `PUT /api/teacher/courses/{courseId}/students/{studentId}` assigns a course to a student (R1).
- `GET /api/teacher/courses/{courseId}/students` lists the assigned students (R2).
- `DELETE /api/teacher/courses/{courseId}/students/{studentId}` unassigns; student removal and teacher deactivation delete assignments by cascade (R3).
- No new error code.

## Capabilities

### New Capabilities
- `course-assignment`: R1 assign, R2 read, R3 unassign and the data rules for removal and deactivation.

### Modified Capabilities
None. `student-enrolment` R4 and R7 keep their text; their effect on assignments is stated in `course-assignment` R3.

## Out of scope

- Lesson access per student (#123), student-facing endpoints (#124), answers (#126, #128).
- Deleting a course (#131) and what it does to assignments.
- Groups (#54): they add their own table later.
- Frontend (#132) and the removal warning dialogs (#121).

## Impact

- Backend: new package `courseassignment` (controller, service, repository, one response record); `LessonRepository.lockCourse` is reused.
- Database: `V17` adds `course_assignments` and `courses_id_teacher_uq`.
- API: three new endpoints under `/api/teacher/courses/{courseId}/students`; no `SecurityConfig` change.
- No new dependencies.
