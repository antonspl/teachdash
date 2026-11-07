package com.teachdash.enrolment.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TeacherJoinLinkRepository {

	private final JdbcClient jdbc;

	TeacherJoinLinkRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<String> findToken(UUID teacherId) {
		return jdbc.sql("select token from teacher_join_links where teacher_id = :teacherId")
				.param("teacherId", teacherId)
				.query(String.class)
				.optional();
	}

	public Optional<UUID> findTeacherId(String token) {
		return jdbc.sql("select teacher_id from teacher_join_links where token = :token")
				.param("token", token)
				.query(UUID.class)
				.optional();
	}

	public void upsert(UUID teacherId, String token, Instant createdAt) {
		jdbc.sql("""
				insert into teacher_join_links (teacher_id, token, created_at)
				values (:teacherId, :token, :createdAt)
				on conflict (teacher_id) do update
				set token = excluded.token, created_at = excluded.created_at
				""")
				.param("teacherId", teacherId)
				.param("token", token)
				.param("createdAt", Timestamp.from(createdAt))
				.update();
	}

	public void delete(UUID teacherId) {
		jdbc.sql("delete from teacher_join_links where teacher_id = :teacherId")
				.param("teacherId", teacherId)
				.update();
	}
}
