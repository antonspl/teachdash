package com.teachdash.items.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.items.support.TestNoteType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class TestNoteTypeTest {

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	@DisplayName("Student projection returns text only and is a new tree")
	void projectionDropsTeacherOnlyFields() {
		// given
		ObjectNode stored = (ObjectNode) mapper.readTree("{\"text\":\"Hello\",\"hint\":\"secret\"}");

		// when
		JsonNode projection = new TestNoteType().studentProjection(stored);

		// then
		assertThat(projection).isEqualTo(mapper.readTree("{\"text\":\"Hello\"}"));
		assertThat(projection).isNotSameAs(stored);
		assertThat(stored.has("hint")).isTrue();
	}
}
