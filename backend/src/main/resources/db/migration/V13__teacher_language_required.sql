update users set native_language = 'en'
    where role = 'TEACHER' and native_language is null;

alter table users drop constraint users_student_has_native_language;

alter table users add constraint users_student_teacher_have_native_language
    check (role = 'ADMIN' or native_language is not null);
