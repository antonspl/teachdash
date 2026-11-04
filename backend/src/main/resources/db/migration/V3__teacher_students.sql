create table teacher_students (
    teacher_id uuid        not null references users (id),
    student_id uuid        not null references users (id),
    created_at timestamptz not null,
    primary key (teacher_id, student_id)
);

create index teacher_students_student_idx on teacher_students (student_id);
