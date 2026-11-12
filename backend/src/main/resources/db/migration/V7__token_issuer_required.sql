delete from one_time_tokens where issued_by is null;

alter table one_time_tokens
    alter column issued_by set not null;
