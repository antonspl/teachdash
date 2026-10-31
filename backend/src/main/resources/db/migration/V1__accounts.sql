create table users (
    id            uuid primary key,
    email         varchar(320) not null,
    name          varchar(200) not null,
    role          varchar(20)  not null check (role in ('ADMIN', 'TEACHER', 'STUDENT')),
    password_hash varchar(200),
    time_zone     varchar(64)  not null,
    active        boolean      not null default true,
    created_at    timestamptz  not null
);

create unique index users_email_lower_uq on users (lower(email));

create table one_time_tokens (
    token_hash varchar(64) primary key,
    user_id    uuid         not null references users (id) on delete cascade,
    purpose    varchar(20)  not null check (purpose in ('SET_PASSWORD', 'RESET_PASSWORD')),
    expires_at timestamptz  not null,
    used_at    timestamptz,
    created_at timestamptz  not null
);

create index one_time_tokens_user_idx on one_time_tokens (user_id);
create unique index one_time_tokens_one_unused_idx on one_time_tokens (user_id) where used_at is null;
