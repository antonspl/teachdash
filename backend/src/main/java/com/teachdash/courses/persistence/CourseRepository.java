package com.teachdash.courses.persistence;

import java.sql.Timestamp;
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
}
