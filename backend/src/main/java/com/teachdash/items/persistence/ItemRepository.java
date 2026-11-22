package com.teachdash.items.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.teachdash.items.domain.Item;

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
			Instant now) {
		return jdbc.sql("""
				insert into items (id, teacher_id, part_id, type, payload, version, position, created_at, updated_at)
				select :id, :teacherId, :partId, :type, cast(:payload as jsonb), 1,
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
				.param("now", Timestamp.from(now))
				.query((rs, rowNum) -> new Stored(mapper.readTree(rs.getString("payload")), rs.getInt("position")))
				.single();
	}

	public Optional<Item> findInPart(UUID itemId, UUID partId, UUID teacherId) {
		return jdbc.sql("""
				select id, type, payload::text as payload, position, version, created_at, updated_at
				from items
				where id = :itemId and part_id = :partId and teacher_id = :teacherId
				""")
				.param("itemId", itemId)
				.param("partId", partId)
				.param("teacherId", teacherId)
				.query((rs, rowNum) -> toItem(rs))
				.optional();
	}

	public void shiftRange(UUID partId, UUID teacherId, int from, int to, int delta) {
		jdbc.sql("""
				update items set position = position + :delta
				where part_id = :partId and teacher_id = :teacherId
				  and position >= :from and position <= :to
				""")
				.param("delta", delta)
				.param("partId", partId)
				.param("teacherId", teacherId)
				.param("from", from)
				.param("to", to)
				.update();
	}

	public Item setPosition(UUID itemId, UUID partId, UUID teacherId, int position, Instant now) {
		return jdbc.sql("""
				update items set position = :position, updated_at = :now
				where id = :itemId and part_id = :partId and teacher_id = :teacherId
				returning id, type, payload::text as payload, position, version, created_at, updated_at
				""")
				.param("position", position)
				.param("now", Timestamp.from(now))
				.param("itemId", itemId)
				.param("partId", partId)
				.param("teacherId", teacherId)
				.query((rs, rowNum) -> toItem(rs))
				.single();
	}

	public Optional<Integer> deleteReturningPosition(UUID itemId, UUID partId, UUID teacherId) {
		return jdbc.sql("""
				delete from items
				where id = :itemId and part_id = :partId and teacher_id = :teacherId
				returning position
				""")
				.param("itemId", itemId)
				.param("partId", partId)
				.param("teacherId", teacherId)
				.query(Integer.class)
				.optional();
	}

	public void closeGap(UUID partId, UUID teacherId, int deletedPosition) {
		jdbc.sql("""
				update items set position = position - 1
				where part_id = :partId and teacher_id = :teacherId and position > :position
				""")
				.param("partId", partId)
				.param("teacherId", teacherId)
				.param("position", deletedPosition)
				.update();
	}

	private Item toItem(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new Item(
				rs.getObject("id", UUID.class),
				rs.getString("type"),
				mapper.readTree(rs.getString("payload")),
				rs.getInt("position"),
				rs.getLong("version"),
				rs.getTimestamp("created_at").toInstant(),
				rs.getTimestamp("updated_at").toInstant());
	}
}
