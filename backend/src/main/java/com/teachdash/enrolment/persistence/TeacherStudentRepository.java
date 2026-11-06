package com.teachdash.enrolment.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.enrolment.domain.TeacherName;
import com.teachdash.enrolment.domain.UserSummary;

@Repository
public class TeacherStudentRepository {

	private static final RowMapper<UserSummary> USER_SUMMARY = (rs, rowNum) -> new UserSummary(
			rs.getObject("id", UUID.class),
			rs.getString("name"),
			rs.getString("email"),
			rs.getString("time_zone"),
			rs.getBoolean("active"));

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

	public boolean insert(UUID teacherId, UUID studentId, Instant createdAt) {
		int rows = jdbc.sql("""
				insert into teacher_students (teacher_id, student_id, created_at)
				values (:teacherId, :studentId, :createdAt)
				on conflict do nothing
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.param("createdAt", Timestamp.from(createdAt))
				.update();
		return rows > 0;
	}

	public boolean delete(UUID teacherId, UUID studentId) {
		int rows = jdbc.sql("""
				delete from teacher_students
				where teacher_id = :teacherId and student_id = :studentId
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.update();
		return rows > 0;
	}

	public boolean hasTeacher(UUID studentId) {
		return jdbc.sql("select exists (select 1 from teacher_students where student_id = :studentId)")
				.param("studentId", studentId)
				.query(Boolean.class)
				.single();
	}

	public List<UserSummary> findStudents(UUID teacherId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone, u.active
				from teacher_students ts
				join users u on u.id = ts.student_id
				where ts.teacher_id = :teacherId
				order by u.name collate "und-x-icu", u.id
				""")
				.param("teacherId", teacherId)
				.query(USER_SUMMARY)
				.list();
	}

	public Optional<UserSummary> findStudent(UUID teacherId, UUID studentId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone, u.active
				from teacher_students ts
				join users u on u.id = ts.student_id
				where ts.teacher_id = :teacherId and ts.student_id = :studentId
				""")
				.param("teacherId", teacherId)
				.param("studentId", studentId)
				.query(USER_SUMMARY)
				.optional();
	}

	public List<UserSummary> findTeachers(UUID studentId) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone, u.active
				from teacher_students ts
				join users u on u.id = ts.teacher_id
				where ts.student_id = :studentId
				order by u.name collate "und-x-icu", u.id
				""")
				.param("studentId", studentId)
				.query(USER_SUMMARY)
				.list();
	}

	public Optional<UserSummary> findStudentByEmail(String email) {
		return jdbc.sql("""
				select u.id, u.name, u.email, u.time_zone, u.active
				from users u
				where u.role = 'STUDENT' and lower(u.email) = lower(:email)
				""")
				.param("email", email)
				.query(USER_SUMMARY)
				.optional();
	}

	public List<TeacherName> findActiveTeacherNames(UUID studentId) {
		return jdbc.sql("""
				select u.id, u.name
				from teacher_students ts
				join users u on u.id = ts.teacher_id
				where ts.student_id = :studentId and u.active
				order by u.name collate "und-x-icu", u.id
				""")
				.param("studentId", studentId)
				.query((rs, rowNum) -> new TeacherName(
						rs.getObject("id", UUID.class),
						rs.getString("name")))
				.list();
	}
}
