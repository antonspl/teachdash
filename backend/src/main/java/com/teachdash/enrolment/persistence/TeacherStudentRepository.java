package com.teachdash.enrolment.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.enrolment.domain.Person;

@Repository
public class TeacherStudentRepository {

	private static final RowMapper<Person> PERSON = (rs, rowNum) -> new Person(
			rs.getObject("id", UUID.class),
			rs.getString("name"),
			rs.getString("email"),
			rs.getString("time_zone"));

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

	public List<Person> findStudents(UUID teacherId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone
				from teacher_students ts
				join users u on u.id = ts.student_id
				where ts.teacher_id = :teacherId
				order by u.name, u.id
				""")
				.param("teacherId", teacherId)
				.query(PERSON)
				.list();
	}

	public Optional<Person> findStudent(UUID teacherId, UUID studentId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone
				from teacher_students ts
				join users u on u.id = ts.student_id
				where ts.teacher_id = :teacherId and ts.student_id = :studentId
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.query(PERSON)
				.optional();
	}

	public List<Person> findTeachers(UUID studentId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone
				from teacher_students ts
				join users u on u.id = ts.teacher_id
				where ts.student_id = :studentId
				order by u.name, u.id
				""")
				.param("studentId", studentId)
				.query(PERSON)
				.list();
	}
}
