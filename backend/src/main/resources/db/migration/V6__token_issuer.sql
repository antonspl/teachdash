alter table one_time_tokens
    add column issued_by uuid references users (id) on delete cascade;
