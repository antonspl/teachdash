package com.teachdash.enrolment.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TeacherJoinLinkRepository {

	public static final Duration VALIDITY = Duration.ofDays(7);

	public record ValidLink(
			String token,
			Instant expiresAt) {
	}

	private final JdbcClient jdbc;

	TeacherJoinLinkRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** A link is valid while now < created_at + VALIDITY; an expired row counts as missing. */
	public Optional<ValidLink> findValid(UUID teacherId, Instant now) {
		return jdbc.sql("select token, created_at from teacher_join_links where teacher_id = :teacherId and created_at > :cutoff")
				.param("teacherId", teacherId)
				.param("cutoff", cutoff(now))
				.query((rs, i) -> new ValidLink(
						rs.getString("token"),
						rs.getTimestamp("created_at").toInstant().plus(VALIDITY)))
				.optional();
	}

	public Optional<UUID> findTeacherId(String token, Instant now) {
		return jdbc.sql("select teacher_id from teacher_join_links where token = :token and created_at > :cutoff")
				.param("token", token)
				.param("cutoff", cutoff(now))
				.query(UUID.class)
				.optional();
	}

	private static Timestamp cutoff(Instant now) {
		return Timestamp.from(now.minus(VALIDITY));
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
