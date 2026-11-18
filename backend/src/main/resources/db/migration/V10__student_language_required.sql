update users set native_language = 'en'
    where role = 'STUDENT' and native_language is null;

alter table users add constraint users_student_has_native_language
    check (role <> 'STUDENT' or native_language is not null);
