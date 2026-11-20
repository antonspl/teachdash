alter table lessons add column course_id uuid references courses (id) on delete cascade;
alter table lessons add column position int;

update lessons l
set course_id = lc.course_id, position = lc.position
from lesson_courses lc
where lc.lesson_id = l.id;

delete from lessons where id not in (select lesson_id from lesson_courses);

alter table lessons alter column course_id set not null;
alter table lessons alter column position set not null;
alter table lessons add constraint lessons_position_check check (position >= 1);
alter table lessons add constraint lessons_course_position_uq
    unique (course_id, position) deferrable initially deferred;

drop table lesson_courses;
