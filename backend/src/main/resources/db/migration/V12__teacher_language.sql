alter table users drop constraint users_native_language_students_only;

alter table users add constraint users_admin_has_no_native_language
    check (role <> 'ADMIN' or native_language is null);
