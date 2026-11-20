# Design: parts-create

## Context

See proposal.md. Current state on `main`:

- `lessons` (V14, V15) has `course_id` and a dense `position` with `unique (course_id, position) deferrable initially deferred`; a lesson is in exactly one course. `LessonService` locks the user row (`UserRepository.findActiveByIdForUpdate`, none gives 401), then the course row (`LessonRepository.lockCourse`), for create, move and delete.
- `@Title` and `Normalize.name` in `common/validation` give the title rule.
- Role gates are URL-prefix rules: `/api/teacher/**` needs TEACHER; no `SecurityConfig` change.
- Teacher-owned data carries `teacher_id` and every query is scoped by it (constitution §8).

## Goals / Non-Goals

**Goals:** a `parts` table, create and list in an own lesson, a dense position that stays correct under parallel requests and under a racing lesson delete.

**Non-Goals:** rename, reorder, delete, copy of parts; items; frontend.

## Decisions

### D1. Migration `V16__parts.sql`

```sql
create table parts (
    id         uuid         primary key,
    teacher_id uuid         not null references users (id) on delete cascade,
    lesson_id  uuid         not null references lessons (id) on delete cascade,
    title      varchar(200) not null,
    position   int          not null check (position >= 1),
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint parts_lesson_position_uq
        unique (lesson_id, position) deferrable initially deferred
);
create index parts_teacher_idx on parts (teacher_id);
```

- Parts hang off `lesson_id` only, never `course_id`: a lesson's course is found through `lessons`, so a future copy of a lesson needs no change here.
- `teacher_id` is on the part so queries are scoped by the teacher without a join (constitution §8), as for lessons. The service sets `teacher_id` from the locked lesson row. Ownership is checked through the lesson lookup (id, course_id, teacher_id); the `parts.teacher_id` filter in the list query (R2) is defence in depth and is not tested separately. No composite foreign key is added.
- `on delete cascade` on `lesson_id` deletes a lesson's parts with it (lessons R5-S8); `on delete cascade` on `teacher_id` covers account deletion.
- `unique (lesson_id, position)` also serves "parts of a lesson in order" and `max(position)` per lesson.
- `updated_at` comes from `Clock`; every later change to a part row MUST set it.
- Version `V16` is the next on `main`; renumbered at merge if needed (#98).

### D2. Endpoints and package

`com.teachdash.parts` (`api`, `api/dto`, `domain`, `persistence`):

- `POST /api/teacher/courses/{courseId}/lessons/{lessonId}/parts`, `@ResponseStatus(CREATED)`, body `CreatePartRequest(title)` (`@Title`, normalised with `Normalize.name` in the compact constructor, as `CreateLessonRequest`).
- `GET /api/teacher/courses/{courseId}/lessons/{lessonId}/parts`.
- Response `PartResponse(id, title, position, createdAt, updatedAt)`. Unknown body fields such as `teacherId` are ignored.
- A malformed course or lesson id is 404 `NOT_FOUND`, not 400, as in lessons.

### D3. Create flow

`PartService.create(teacherId, courseId, lessonId, title)`, `@Transactional`:

1. `UserRepository.findActiveByIdForUpdate(teacherId)`: none gives 401 `UNAUTHENTICATED`.
2. Lock the course row (`LessonRepository.lockCourse`-style: `where id = :courseId and teacher_id = :teacherId for update`); no row gives 404.
3. Lock the lesson row: `select id from lessons where id = :lessonId and course_id = :courseId and teacher_id = :teacherId for update`; no row gives 404. This makes a lesson of the caller's other course 404.
4. Under the locks, `count(*) from parts where lesson_id = :lessonId` at or above `maxPerLesson`: 409 `PART_LIMIT_REACHED` (plain `ApiException`, no `errors`).
5. Insert in one statement: `insert into parts (...) select ..., coalesce(max(position), 0) + 1 from parts where lesson_id = :lessonId returning position`, with `created_at` and `updated_at` from one `Clock` time.

Order of checks, as lessons R1: CSRF and role (403), deactivated (401), malformed ids (404), input (400), unknown or foreign course or lesson (404), limit (409). `maxPerLesson` is `teachdash.parts.max-per-lesson=20`, read with `@Value` like the lessons limit.

List: `PartService.list`, `@Transactional(readOnly = true)`: a lesson-in-course-of-teacher check without a lock (like `findByIdInCourse`), 404 if none, then the lesson's parts `order by position`, scoped by `parts.teacher_id` too. An empty lesson gives an empty array.

### D4. Position integrity and lock order

Invariant: in one lesson the positions are 1..n without gaps. Every writer locks the user row first, then the course row, then the lesson row, as the rule from lessons-create D5 says (users, then the parent rows in order). The user row comes first because the foreign key on `teacher_id` puts a KEY SHARE lock on it into every write, and `CourseService.rename` locks the user row before the course row; the opposite order could deadlock.

Lessons delete and move lock user, then course, and delete or update lesson rows; the cascade deletes parts under those locks. A parts create racing a lesson delete therefore queues behind the user and course locks: if the create goes first, the delete cascades over its part; if the delete goes first, the create finds no lesson and gets 404. The lesson row lock is taken after the course lock, so it adds no ordering conflict. R1-S13 tests this race: one delete and four creates at once give 204 for the delete, 201 or 404 for each create (never 500), no lesson and no part rows left.

The user row lock already serialises all of one teacher's writes, so the race tests R1-S9 and R1-S13 do not prove the lesson lock alone. The lesson lock stays for 404 scoping, for position correctness and for writers outside the user-lock path. The deferrable unique constraint is a safety net: a violation at commit can only come from a bug and surfaces as a 500; no error code is mapped. Reordering parts (a later change) uses the same locks and one `UPDATE` that shifts a range, which a deferrable constraint allows.

### D5. Tests and helpers

Tests extend `support.ApiTest`. New helpers: `createPart(teacherId, lessonId, title, position)` (direct SQL, times from the test `Clock`), `countParts(lessonId)`, `partPositions(lessonId)`. R1-S9 and R1-S13 use `support.Parallel.run`.

## Dependencies

None added.

## Risks / Trade-offs

- **Owner consistency.** Nothing in the database forces `parts.teacher_id` to equal `lessons.teacher_id`; only create inserts, with the `teacher_id` of the locked lesson row.
- **No fixing of parts until #109.** Until parts-edit-delete (#109) lands there is no rename, delete or move of a part: a typo or a full lesson can only be fixed by deleting the lesson.
- **Limit is a guess.** 20 parts per lesson is an estimate, held in a property so it can change by PR. Lowering it leaves lessons above it; they only cannot get more parts.
- **Unicode and length.** As for lesson titles: `@Size` counts UTF-16 units, titles are not NFC-normalised, invisible format characters inside a title give `INVALID_FORMAT`.

## Migrations

- `V16__parts.sql`: table `parts`, the deferrable unique `(lesson_id, position)`, the index on `teacher_id`.
