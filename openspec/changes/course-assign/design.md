# Design: course-assign

## Context

See proposal.md. Current state (on top of `assignment-end-voids-links`):

- `courses` (`V11`): `id`, `teacher_id` (foreign key to `users`, `on delete cascade`), title and languages. Nothing deletes a course yet.
- `teacher_students` (`V3`, `V4`): primary key `(teacher_id, student_id)`, unique `student_id`, no cascades. `AssignmentEnding.end` and `endAll` delete from it; callers hold the teacher row lock (`StudentRemovalService`, `TeacherDeactivationListener`).
- Teacher-owned data carries `teacher_id` and every query is scoped by it (constitution section 8).

## Goals / Non-Goals

**Goals:** assign, read and unassign a course for an own student; assignments disappear with the student's removal and the teacher's deactivation, without any code that could forget it.

**Non-Goals:** lesson access, student endpoints, answers, course delete, groups, frontend.

## Decisions

### D1. Table and migration `V17`

```sql
alter table courses add constraint courses_id_teacher_uq unique (id, teacher_id);

create table course_assignments (
    course_id  uuid        not null,
    student_id uuid        not null,
    teacher_id uuid        not null,
    created_at timestamptz not null,
    primary key (course_id, student_id),
    foreign key (course_id, teacher_id)
        references courses (id, teacher_id) on delete cascade,
    foreign key (teacher_id, student_id)
        references teacher_students (teacher_id, student_id) on delete cascade
);
create index course_assignments_teacher_student_idx
    on course_assignments (teacher_id, student_id);
```

- The primary key makes one student take a course once and serves the course-side read.
- Both composite foreign keys carry `teacher_id`, so a row can only exist when the course and the student belong to the same teacher, whatever code inserts it (constitution section 8).
- The index serves the student-side cascade (`teacher_students` delete) and later student-side reads.
- Groups-ready: this table holds direct per-student assignments. Groups (#54) add their own table (for example group courses) and need no change here.
- PR #181 also uses `V17`; whichever merges second renumbers its file (the issue and #98).

### D2. Cleanup by the database

`AssignmentEnding` is the only deleter of `teacher_students` (#90). The cascade of the second foreign key deletes a student's assignments in the same statement, for `end` (removal of one student) and for `endAll` (teacher deactivation). `AssignmentEnding` and its callers are unchanged, and a future code path that deletes `teacher_students` rows is covered too. The cascade of the first foreign key deletes the assignments of a course when the course is deleted (#131 later); nothing deletes courses today, so this is not tested here.

Later tables (lesson access #123, answers #128) can reference `course_assignments (course_id, student_id)` with `on delete cascade` and get the same guarantee: student removal deletes the assignment, which deletes its lesson access and answers.

### D3. Endpoints, locks and flows

New package `com.teachdash.courseassignment` (api, domain, persistence), one controller under `/api/teacher/courses/{courseId}/students`; no `SecurityConfig` change (`/api/teacher/**` is TEACHER). Path ids are `UUID`; a malformed id is 404 `NOT_FOUND`.

- `PUT .../students/{studentId}`: 204, no body, no request body read. After the locks, `insert into course_assignments (course_id, student_id, teacher_id, created_at) values (...) on conflict (course_id, student_id) do nothing`; a repeat keeps the original row and `created_at`. `created_at` is truncated to milliseconds from the `Clock`, as in other tables.
- `DELETE .../students/{studentId}`: 204 if `delete ... where course_id and student_id and teacher_id` removed a row, else 404.
- `GET .../students`: after the course check, `select u.id, u.name, a.created_at as assigned_at from course_assignments a join users u on u.id = a.student_id where a.course_id = :c and a.teacher_id = :t order by u.name collate "und-x-icu", u.id`. The response record has exactly `id`, `name`, `assignedAt`.

Assign and unassign run in one `@Transactional` method with this order:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`; none gives 401 `UNAUTHENTICATED`.
2. The course row `for update`, scoped by `teacher_id` (`LessonRepository.lockCourse` is reused instead of a third copy; if a cross-package dependency on `lessons` is unwanted, the helper moves to `CourseRepository`, task 3.1); none gives 404.
3. The student check through `teacher_students` by `(teacher_id, student_id)`; none gives 404. This covers an unknown id, another teacher's student, a student without a teacher and an id that is not a student, with the same 404.

Teacher row first matches `LessonService` and `StudentRemovalService`, so there is no deadlock cycle, and an assign racing a removal of the same student either runs before it (the cascade then removes the row) or after it (step 3 gives 404). Removal by `StudentRemovalService` runs under the same teacher row lock, so the two are serialised; the parallel scenario proves the end state, not the lock alone. A deactivation in mid-request gives 401 from step 1 (accepted race, as for lessons).

The read takes no lock: one `select` for the course (scoped by `teacher_id`, 404) and one for the list.

No limit on the students of a course: the number is bounded by the teacher's own students.

Order of checks: CSRF and role (403), deactivated (401), malformed id (404), unknown or foreign course (404), student (404). A GET has no CSRF check and no student step.

### D4. Tests and helpers

Tests extend `support.ApiTest`: `AssignCourseTest`, `CourseStudentsTest`, `UnassignCourseTest`, in `courseassignment/api/`. Existing `createTeacher`, `createStudent`, `createAdmin`, `assignStudent`, `createCourse`, `loggedInAs`, `countRows` and `Parallel` are reused. Added: `assignCourse(course, student)` (a direct insert into `course_assignments`, so GIVEN never calls the endpoints under test) and a read of the assignment rows with `created_at`. `cleanDatabase` truncates the new table automatically.

## Dependencies

None added.

## Risks / Trade-offs

- **Cascade is invisible in code.** The behaviour lives in the schema; R3-S6 and R3-S7 are its proof, and a reviewer should check the `V17` foreign keys, not Java.
- **`V17` clash with #181.** Renumbering is mechanical; Flyway fails on a duplicate, and #98 catches it.
- **Reusing `lockCourse` from `lessons`.** A small coupling; accepted over a third copy.

## Migrations

`V17__course_assignments.sql`: `courses_id_teacher_uq` and the table `course_assignments` with its index (D1).
