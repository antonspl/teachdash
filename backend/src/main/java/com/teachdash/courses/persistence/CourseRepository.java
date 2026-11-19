package com.teachdash.courses.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.courses.domain.Course;

@Repository
public class CourseRepository {

	private final JdbcClient jdbc;

	CourseRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public int countByTeacher(UUID teacherId) {
		return jdbc.sql("select count(*) from courses where teacher_id = :teacherId")
				.param("teacherId", teacherId)
				.query(Integer.class)
				.single();
	}

	public void insert(Course course) {
		jdbc.sql("""
				insert into courses (
					id, teacher_id, title, instruction_language, target_language, created_at, updated_at
				)
				values (
					:id, :teacherId, :title, :instructionLanguage, :targetLanguage, :createdAt, :updatedAt
				)
				""")
				.param("id", course.id())
				.param("teacherId", course.teacherId())
				.param("title", course.title())
				.param("instructionLanguage", course.instructionLanguage())
				.param("targetLanguage", course.targetLanguage())
				.param("createdAt", Timestamp.from(course.createdAt()))
				.param("updatedAt", Timestamp.from(course.updatedAt()))
				.update();
	}

	public List<Course> findByTeacher(UUID teacherId) {
		return jdbc.sql("""
				select id, teacher_id, title, instruction_language, target_language, created_at, updated_at
				from courses
				where teacher_id = :teacherId
				order by created_at desc, id
				""")
				.param("teacherId", teacherId)
				.query(CourseRepository::mapCourse)
				.list();
	}

	public Optional<Course> findByIdAndTeacher(UUID id, UUID teacherId) {
		return jdbc.sql("""
				select id, teacher_id, title, instruction_language, target_language, created_at, updated_at
				from courses
				where id = :id and teacher_id = :teacherId
				""")
				.param("id", id)
				.param("teacherId", teacherId)
				.query(CourseRepository::mapCourse)
				.optional();
	}

	private static Course mapCourse(ResultSet rs, int rowNum) throws SQLException {
		return new Course(
				rs.getObject("id", UUID.class),
				rs.getObject("teacher_id", UUID.class),
				rs.getString("title"),
				rs.getString("instruction_language"),
				rs.getString("target_language"),
				rs.getTimestamp("created_at").toInstant(),
				rs.getTimestamp("updated_at").toInstant());
	}
}
