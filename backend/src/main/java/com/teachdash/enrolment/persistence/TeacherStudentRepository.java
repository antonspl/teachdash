package com.teachdash.enrolment.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TeacherStudentRepository {

	private final JdbcClient jdbc;

	TeacherStudentRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public boolean exists(UUID teacherId, UUID studentId) {
		return jdbc.sql("""
				select exists (
					select 1 from teacher_students
					where teacher_id = :teacherId and student_id = :studentId
				)
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.query(Boolean.class)
				.single();
	}

	public void insert(UUID teacherId, UUID studentId, Instant createdAt) {
		jdbc.sql("""
				insert into teacher_students (teacher_id, student_id, created_at)
				values (:teacherId, :studentId, :createdAt)
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.param("createdAt", Timestamp.from(createdAt))
				.update();
	}
}
