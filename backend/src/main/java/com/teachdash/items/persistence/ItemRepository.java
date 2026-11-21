package com.teachdash.items.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class ItemRepository {

	public record Stored(
			JsonNode payload,
			int position) {
	}

	private final JdbcClient jdbc;

	private final ObjectMapper mapper;

	ItemRepository(JdbcClient jdbc, ObjectMapper mapper) {
		this.jdbc = jdbc;
		this.mapper = mapper;
	}

	public boolean lockPart(UUID partId, UUID lessonId, UUID teacherId) {
		return jdbc.sql("""
				select id from parts
				where id = :partId and lesson_id = :lessonId and teacher_id = :teacherId
				for update
				""")
				.param("partId", partId)
				.param("lessonId", lessonId)
				.param("teacherId", teacherId)
				.query(UUID.class)
				.optional()
				.isPresent();
	}

	public int countInPart(UUID partId, UUID teacherId) {
		return jdbc.sql("select count(*) from items where part_id = :partId and teacher_id = :teacherId")
				.param("partId", partId)
				.param("teacherId", teacherId)
				.query(Integer.class)
				.single();
	}

	public Stored insertAtEnd(
			UUID id,
			UUID teacherId,
			UUID partId,
			String type,
			String payloadJson,
			int schemaVersion,
			Instant now) {
		return jdbc.sql("""
				insert into items (id, teacher_id, part_id, type, payload, schema_version, version, position,
				                   created_at, updated_at)
				select :id, :teacherId, :partId, :type, cast(:payload as jsonb), :schemaVersion, 1,
				       coalesce(max(position), 0) + 1, :now, :now
				from items
				where part_id = :partId and teacher_id = :teacherId
				returning payload::text as payload, position
				""")
				.param("id", id)
				.param("teacherId", teacherId)
				.param("partId", partId)
				.param("type", type)
				.param("payload", payloadJson)
				.param("schemaVersion", schemaVersion)
				.param("now", Timestamp.from(now))
				.query((rs, rowNum) -> new Stored(mapper.readTree(rs.getString("payload")), rs.getInt("position")))
				.single();
	}
}
