alter table courses add constraint courses_id_teacher_uq unique (id, teacher_id);

create table course_assignments (
    course_id  uuid        not null,
    student_id uuid        not null,
    teacher_id uuid        not null,
    created_at timestamptz not null,
    primary key (course_id, student_id),
    foreign key (course_id, teacher_id)
        references courses (id, teacher_id) on delete cascade,
    foreign key (teacher_id, student_id)
        references teacher_students (teacher_id, student_id) on delete cascade
);
create index course_assignments_teacher_student_idx
    on course_assignments (teacher_id, student_id);
