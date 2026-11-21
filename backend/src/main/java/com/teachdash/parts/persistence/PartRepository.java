package com.teachdash.parts.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.parts.domain.Part;

@Repository
public class PartRepository {

	private final JdbcClient jdbc;

	PartRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public boolean lockCourse(UUID courseId, UUID teacherId) {
		return jdbc.sql("select id from courses where id = :courseId and teacher_id = :teacherId for update")
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(UUID.class)
				.optional()
				.isPresent();
	}

	public boolean lockLesson(UUID lessonId, UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				select id from lessons
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				for update
				""")
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(UUID.class)
				.optional()
				.isPresent();
	}

	public boolean lessonExists(UUID lessonId, UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				select id from lessons
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				""")
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(UUID.class)
				.optional()
				.isPresent();
	}

	public int countInLesson(UUID lessonId) {
		return jdbc.sql("select count(*) from parts where lesson_id = :lessonId")
				.param("lessonId", lessonId)
				.query(Integer.class)
				.single();
	}

	public int insertAtEnd(UUID id, UUID teacherId, UUID lessonId, String title, Instant now) {
		return jdbc.sql("""
				insert into parts (id, teacher_id, lesson_id, title, position, created_at, updated_at)
				select :id, :teacherId, :lessonId, :title, coalesce(max(position), 0) + 1, :createdAt, :updatedAt
				from parts
				where lesson_id = :lessonId
				returning position
				""")
				.param("id", id)
				.param("teacherId", teacherId)
				.param("lessonId", lessonId)
				.param("title", title)
				.param("createdAt", Timestamp.from(now))
				.param("updatedAt", Timestamp.from(now))
				.query(Integer.class)
				.single();
	}

	public List<Part> findByLesson(UUID lessonId, UUID teacherId) {
		return jdbc.sql("""
				select id, teacher_id, title, position, created_at, updated_at
				from parts
				where lesson_id = :lessonId and teacher_id = :teacherId
				order by position
				""")
				.param("lessonId", lessonId)
				.param("teacherId", teacherId)
				.query(PartRepository::mapPart)
				.list();
	}

	private static Part mapPart(ResultSet rs, int rowNum) throws SQLException {
		return new Part(
				rs.getObject("id", UUID.class),
				rs.getObject("teacher_id", UUID.class),
				rs.getString("title"),
				rs.getInt("position"),
				rs.getTimestamp("created_at").toInstant(),
				rs.getTimestamp("updated_at").toInstant());
	}
}
