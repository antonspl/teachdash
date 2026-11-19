create table courses (
    id                   uuid         primary key,
    teacher_id           uuid         not null references users (id) on delete cascade,
    title                varchar(200) not null,
    instruction_language varchar(2)   not null references languages (code),
    target_language      varchar(2)   references languages (code),
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null
);
create unique index courses_teacher_title_uq on courses (teacher_id, lower(title));
