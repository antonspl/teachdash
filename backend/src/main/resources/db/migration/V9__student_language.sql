create table languages (
    code    varchar(2) primary key check (code ~ '^[a-z]{2}$'),
    content boolean    not null
);

insert into languages (code, content) values
    ('de', false), ('en', true),  ('es', false), ('fr', false), ('it', false),
    ('pl', false), ('pt', false), ('ru', true),  ('sr', false), ('uk', false);

alter table users add column native_language varchar(2) references languages (code);

alter table users add constraint users_native_language_students_only
    check (native_language is null or role = 'STUDENT');
