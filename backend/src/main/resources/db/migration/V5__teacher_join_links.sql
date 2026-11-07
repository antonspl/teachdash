create table teacher_join_links (
    teacher_id uuid        primary key references users (id) on delete cascade,
    token      varchar(64) not null unique,
    created_at timestamptz not null
);
