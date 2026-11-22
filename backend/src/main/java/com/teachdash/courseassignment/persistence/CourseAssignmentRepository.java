package com.teachdash.courseassignment.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.courseassignment.domain.CourseStudent;

@Repository
public class CourseAssignmentRepository {

	private final JdbcClient jdbc;

	CourseAssignmentRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertIfAbsent(UUID courseId, UUID studentId, UUID teacherId, Instant createdAt) {
		jdbc.sql("""
				insert into course_assignments (course_id, student_id, teacher_id, created_at)
				values (:courseId, :studentId, :teacherId, :createdAt)
				on conflict (course_id, student_id) do nothing
				""")
				.param("courseId", courseId)
				.param("studentId", studentId)
				.param("teacherId", teacherId)
				.param("createdAt", Timestamp.from(createdAt))
				.update();
	}

	public boolean delete(UUID courseId, UUID studentId, UUID teacherId) {
		return jdbc.sql("""
				delete from course_assignments
				where course_id = :courseId and student_id = :studentId and teacher_id = :teacherId
				""")
				.param("courseId", courseId)
				.param("studentId", studentId)
				.param("teacherId", teacherId)
				.update() > 0;
	}

	public List<CourseStudent> findStudentsOfCourse(UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				select u.id, u.name, a.created_at as assigned_at
				from course_assignments a
				join users u on u.id = a.student_id
				where a.course_id = :courseId and a.teacher_id = :teacherId
				order by u.name collate "und-x-icu", u.id
				""")
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query((rs, rowNum) -> new CourseStudent(
						rs.getObject("id", UUID.class),
						rs.getString("name"),
						rs.getTimestamp("assigned_at").toInstant()))
				.list();
	}

	public boolean studentOfTeacherExists(UUID teacherId, UUID studentId) {
		return jdbc.sql("select 1 from teacher_students where teacher_id = :teacherId and student_id = :studentId")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.query(Integer.class)
				.optional()
				.isPresent();
	}
}
