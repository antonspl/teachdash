package com.teachdash.items.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.items.support.TestNoteType;


class TestNoteTypeTest {

	@Test
	@DisplayName("Student view returns text only")
	void studentViewDropsTeacherOnlyFields() {
		// given
		var stored = new TestNoteType.TestNotePayload("Hello", "secret", 3, new BigDecimal("1.5"));

		// when
		Object view = new TestNoteType().studentView(stored);

		// then
		assertThat(view).isEqualTo(Map.of("text", "Hello"));
	}
}
