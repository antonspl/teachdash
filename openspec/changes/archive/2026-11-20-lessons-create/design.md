# Design: lessons-create

## Context

See proposal.md. Current state on `main`:

- `courses` exists (V11) with `CourseService.create` (teacher row lock, limit check, insert; lessons use the same teacher row lock first, see D4), `CourseRepository` on `JdbcClient`, `@CourseTitle` in `common/validation`, `teachdash.courses.max-per-teacher`. Migrations end at V13.
- Role gates are URL-prefix rules in `common/SecurityConfig`: `/api/teacher/**` needs TEACHER.
- Controllers read the teacher id with `UUID.fromString(principal.getName())`; a deactivated teacher gets 401 `UNAUTHENTICATED` from `UserRepository.findActiveByIdForUpdate`.
- Teacher-owned data carries `teacher_id` and every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** lesson tables, create and list in an own course, ordered by a dense position that stays correct under parallel requests, a schema that later reordering, copying and sharing can use without rework.

**Non-Goals:** view, rename, reorder, remove, delete, copy; parts and items; the "store" course; frontend.

## Decisions

### D1. Migration `V14__lessons.sql`

```sql
create table lessons (
    id         uuid         primary key,
    teacher_id uuid         not null references users (id) on delete cascade,
    title      varchar(200) not null,
    created_at timestamptz  not null,
    updated_at timestamptz  not null
);
create index lessons_teacher_idx on lessons (teacher_id);

create table lesson_courses (
    lesson_id uuid not null references lessons (id) on delete cascade,
    course_id uuid not null references courses (id) on delete cascade,
    position  int  not null check (position >= 1),
    primary key (lesson_id, course_id),
    constraint lesson_courses_course_position_uq
        unique (course_id, position) deferrable initially deferred
);
```

- `lessons` has no `course_id` and no language columns. The language pair belongs to the course (constitution §9); a lesson's only link to a course is `lesson_courses`. This lets a lesson later sit in several courses, or in the "store" course, without moving data. It is the stated exception to "one new table per change".
- `teacher_id` is on the lesson so queries on lessons are scoped by the teacher without a join (constitution §8). A lesson and every course it is linked to have the same owner; the API guarantees it (a lesson is created inside the caller's course; later linking checks both owners).
- `unique (course_id, position)` also serves "lessons of a course in order" (the index starts with `course_id`) and `max(position)` per course.
- `updated_at` is set from `Clock` like `courses.updated_at`; every later change to a lesson row MUST set it.
- Table names are plural, like `users` and `courses`. The version is renumbered at merge if needed (#98).

### D2. Endpoints and package

`com.teachdash.lessons` (`api`, `api/dto`, `domain`, `persistence`):

- `POST /api/teacher/courses/{courseId}/lessons`, `@ResponseStatus(CREATED)`, body `CreateLessonRequest(title)`.
- `GET /api/teacher/courses/{courseId}/lessons`.
- Response `LessonResponse(id, title, position, createdAt, updatedAt)`.

Unknown body fields such as `teacherId` are ignored (Spring Boot default); the owner is the principal. No `SecurityConfig` change. A malformed `courseId` is a 404 `NOT_FOUND` as in courses R4, not a 400.

### D3. Title validation

The annotation `CourseTitle` is renamed to a neutral `Title` (same package, `common/validation`; `Title.MAX_LENGTH` for tests and the column length), and `CreateCourseRequest`, the rename request and `CreateLessonRequest` use it. It stays `@NotBlank` + `@Size(max = 200)` + the letter-or-digit pattern; `FieldErrorCodes` still maps constraint types, so the codes are `REQUIRED`, `TOO_LONG`, `INVALID_FORMAT`. The request normalises the title with `Normalize.name` in its compact constructor, as `CreateCourseRequest` does. Course behaviour does not change; existing course tests are the proof. Lesson titles have no unique index and no `TITLE_TAKEN`.

### D4. Create flow

`LessonService.create(teacherId, courseId, title)`, `@Transactional`:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`: locks the teacher's user row; none (deactivated or missing): 401 `UNAUTHENTICATED`. This comes first because the insert into `lessons` takes a KEY SHARE lock on the user row through the `teacher_id` foreign key, and `CourseService.rename` locks the user row and then the course row. Locking the course row first would be the opposite order and could deadlock (a code review found this). It also stops a teacher deactivated mid-request from creating a lesson.
2. `select id from courses where id = :courseId and teacher_id = :teacherId for update`. Second lock, after the user row; it scopes the lookup by teacher and keeps the position correct (the lock #107's move will also use). No row: `ApiException` 404 `NOT_FOUND`, the same for unknown and foreign ids, so nothing leaks.
3. Under both locks, `count(*) from lesson_courses where course_id = :courseId` at or above `maxPerCourse`: `409 LESSON_LIMIT_REACHED` (plain `ApiException`, no `errors`).
4. Insert the lesson (new UUID, `created_at` and `updated_at` from one `clock` time, explicit insert through `JdbcClient`, as for courses), then the `lesson_courses` row with `position = coalesce(max(position), 0) + 1` for the course.

Order of checks, as courses R5: CSRF and role (403), deactivated teacher (401), malformed course id (404), input (400), unknown or another teacher's course (404), limit (409). List: the same without the input step. Because ownership is checked before the limit, a full foreign course gives 404, not 409 (R1-S11), and input is validated before the course is looked up (R1-S13). The user row lock serialises all of one teacher's creates, which makes steps 3 and 4 safe under parallel requests. So the race test R1-S9 no longer proves the course lock alone; the course lock stays for the 404 scoping, for position correctness and for writes that do not go through the user lock path. `maxPerCourse` comes from `teachdash.lessons.max-per-course=100` in `application.properties`, read with `@Value`, like the courses limit. The limit counts `lesson_courses` rows, so it applies to every course, including the future "store" course.

List: `LessonService.list(teacherId, courseId)` checks that the course belongs to the teacher (404 otherwise, same query without `for update`), then selects the course's lessons joined with `lesson_courses`, `order by position`, scoped by `lessons.teacher_id` as well.

No payload limit beyond the title length: a lesson has no other field. Content comes later as parts and items (#108, #110), which carry their own limits.

### D5. Positions and reordering (designed now, built later)

Invariant: in one course the positions are 1..n without gaps. Every writer takes the teacher's user row lock first and the course row lock (`for update`) second. General rule for teacher-content writes: lock users, then the parent row (course, then lesson, and so on) in that order; a write touching several courses locks the course rows in `id` order after the user row (#107's move follows this). The order matters because the foreign key on `teacher_id` puts a KEY SHARE lock on the user row into every lesson write, and `CourseService.rename` locks the user row before the course row. The deferrable unique constraint is a safety net, not the mechanism: a violation at commit can only come from a bug (the lock forgotten) and surfaces as a 500; no error code is mapped.

Why deferrable: Postgres checks a non-deferrable unique constraint row by row, so one `UPDATE` that shifts a range of positions can fail halfway on a temporary duplicate. A deferrable constraint is checked at commit, so the shift is allowed. A deferrable constraint cannot be an `ON CONFLICT` arbiter; this change does not need that.

Intended behaviour for #107 and later changes (not implemented here, so no migration rework is needed):

- **Move.** API body `{position: n}`; the server clamps `n` to 1..count. One `UPDATE` shifts the range between the old and the new position by one and sets the moved row, under the course row lock. Up and down are `n - 1` and `n + 1`.
- **Remove from a course or delete a lesson.** Delete the `lesson_courses` row, then close the gap by decrementing the positions above it, under the course row lock.
- **Delete a lesson that is in several courses.** Lock all those course rows in `id` order (to avoid deadlocks between two such deletes), then delete and close the gap in each.

The same pattern (parent row lock, dense 1-based position, deferrable unique (parent, position)) is the intended pattern for parts under a lesson (#108) and items under a part (#110).

### D6. Language rule

A lesson joins only a course with the same (instruction, target) pair (constitution §9). This change satisfies it trivially: a lesson has no language and is created inside its course. The rule matters for the later "add an existing lesson to a course" and copy changes, which must check it.

## Dependencies

None added.

## Risks / Trade-offs

- **Owner consistency.** Nothing in the database forces `lessons.teacher_id` to equal the owner of the linked courses. Today only create inserts, with the owner taken from the locked course row; later linking changes must check both owners.
- **Deadlock on multi-course delete.** Avoided by locking the user row first and then the course rows in `id` order (D5); the future change must follow it.
- **Limit is a guess.** 100 per course is an estimate, held in a property so it can change by PR.
- **Lowering the limit** leaves courses above it; they only cannot get more lessons.
- **Unicode and length.** As for course titles: `@Size` counts UTF-16 units, titles are not NFC-normalised, and invisible format characters inside a title give `INVALID_FORMAT`.
- **Files and copies.** When a lesson with files is copied, whether file records are duplicated or shared with a reference count must be decided before item-image-audio (#55), or media cleanup could delete a shared file. Parts and items must hang off `lesson_id` only, never `course_id`.
