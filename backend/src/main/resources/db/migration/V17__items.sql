alter table parts add constraint parts_id_teacher_uq unique (id, teacher_id);

create table items (
    id             uuid        primary key,
    teacher_id     uuid        not null references users (id) on delete cascade,
    part_id        uuid        not null,
    type           varchar(50) not null,
    payload        jsonb       not null,
    schema_version int         not null check (schema_version >= 1),
    version        bigint      not null check (version >= 1),
    position       int         not null check (position >= 1),
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint items_part_owner_fk
        foreign key (part_id, teacher_id) references parts (id, teacher_id) on delete cascade,
    constraint items_part_position_uq
        unique (part_id, position) deferrable initially deferred
);
create index items_teacher_idx on items (teacher_id);
