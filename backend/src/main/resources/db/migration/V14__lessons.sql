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
