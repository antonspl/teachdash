package com.teachdash.lessons.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

	public int insertAtEnd(UUID id, UUID teacherId, UUID courseId, String title, Instant now) {
		return jdbc.sql("""
				insert into lessons (id, teacher_id, title, created_at, updated_at, course_id, position)
				select :id, :teacherId, :title, :createdAt, :updatedAt, :courseId, coalesce(max(position), 0) + 1
				from lessons
				where course_id = :courseId
				returning position
				""")
				.param("id", id)
				.param("teacherId", teacherId)
				.param("title", title)
				.param("createdAt", Timestamp.from(now))
				.param("updatedAt", Timestamp.from(now))
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

	public Optional<Lesson> findByIdInCourse(UUID lessonId, UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				select id, teacher_id, title, position, created_at, updated_at
				from lessons
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				""")
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(LessonRepository::mapLesson)
				.optional();
	}

	public int updateTitle(UUID lessonId, UUID courseId, UUID teacherId, String title, Instant now) {
		return jdbc.sql("""
				update lessons set title = :title, updated_at = :now
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				""")
				.param("title", title)
				.param("now", Timestamp.from(now))
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.update();
	}

	public int shiftRange(UUID courseId, int from, int to, int delta) {
		return jdbc.sql("""
				update lessons set position = position + :delta
				where course_id = :courseId and position >= :from and position <= :to
				""")
				.param("delta", delta)
				.param("courseId", courseId)
				.param("from", from)
				.param("to", to)
				.update();
	}

	public int setPosition(UUID lessonId, UUID courseId, UUID teacherId, int position, Instant now) {
		return jdbc.sql("""
				update lessons set position = :position, updated_at = :now
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				""")
				.param("position", position)
				.param("now", Timestamp.from(now))
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.update();
	}

	public Optional<Integer> deleteReturningPosition(UUID lessonId, UUID courseId, UUID teacherId) {
		return jdbc.sql("""
				delete from lessons
				where id = :lessonId and course_id = :courseId and teacher_id = :teacherId
				returning position
				""")
				.param("lessonId", lessonId)
				.param("courseId", courseId)
				.param("teacherId", teacherId)
				.query(Integer.class)
				.optional();
	}

	public int closeGap(UUID courseId, int deletedPosition) {
		return jdbc.sql("update lessons set position = position - 1 where course_id = :courseId and position > :position")
				.param("courseId", courseId)
				.param("position", deletedPosition)
				.update();
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
