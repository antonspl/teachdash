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
