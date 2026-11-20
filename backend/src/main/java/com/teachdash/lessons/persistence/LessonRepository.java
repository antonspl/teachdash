package com.teachdash.lessons.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.lessons.domain.Lesson;

@Repository
public class LessonRepository {

	private final JdbcClient jdbc;

	LessonRepository(JdbcClient jdbc) {
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

	public boolean courseExists(UUID courseId, UUID teacherId) {
		return jdbc.sql("select id from courses where id = :courseId and teacher_id = :teacherId")
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(UUID.class)
				.optional()
				.isPresent();
	}

	public int countInCourse(UUID courseId) {
		return jdbc.sql("select count(*) from lessons where course_id = :courseId")
				.param("courseId", courseId)
				.query(Integer.class)
				.single();
	}

	public int insertAtEnd(Lesson lesson, UUID courseId) {
		return jdbc.sql("""
				insert into lessons (id, teacher_id, title, created_at, updated_at, course_id, position)
				select :id, :teacherId, :title, :createdAt, :updatedAt, :courseId, coalesce(max(position), 0) + 1
				from lessons
				where course_id = :courseId
				returning position
				""")
				.param("id", lesson.id())
				.param("teacherId", lesson.teacherId())
				.param("title", lesson.title())
				.param("createdAt", Timestamp.from(lesson.createdAt()))
				.param("updatedAt", Timestamp.from(lesson.updatedAt()))
				.param("courseId", courseId)
				.query(Integer.class)
				.single();
	}

	public List<Lesson> findByCourse(UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				select id, teacher_id, title, position, created_at, updated_at
				from lessons
				where course_id = :courseId and teacher_id = :teacherId
				order by position
				""")
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(LessonRepository::mapLesson)
				.list();
	}

	private static Lesson mapLesson(ResultSet rs, int rowNum) throws SQLException {
		return new Lesson(
				rs.getObject("id", UUID.class),
				rs.getObject("teacher_id", UUID.class),
				rs.getString("title"),
				rs.getInt("position"),
				rs.getTimestamp("created_at").toInstant(),
				rs.getTimestamp("updated_at").toInstant());
	}
}
