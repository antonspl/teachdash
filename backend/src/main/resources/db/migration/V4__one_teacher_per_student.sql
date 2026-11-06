drop index teacher_students_student_idx;
create unique index teacher_students_student_uq on teacher_students (student_id);
