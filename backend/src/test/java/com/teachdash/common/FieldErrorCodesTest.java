package com.teachdash.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FieldErrorCodesTest {

	@Test
	@DisplayName("An empty property path gives the bare prefix, without a trailing dot")
	void emptyPathGivesBarePrefix() {
		assertThat(FieldErrorCodes.withPrefix("payload", "")).isEqualTo("payload");
	}

	@Test
	@DisplayName("A property path is joined to the prefix with a dot")
	void pathIsJoinedWithDot() {
		assertThat(FieldErrorCodes.withPrefix("payload", "text")).isEqualTo("payload.text");
	}

	@Test
	@DisplayName("Without a prefix the path is unchanged")
	void noPrefixKeepsPath() {
		assertThat(FieldErrorCodes.withPrefix("", "text")).isEqualTo("text");
	}

	@Test
	@DisplayName("List indexes in error fields are ordered as numbers and plain names keep their order")
	void listIndexesAreOrderedAsNumbers() {
		List<FieldErrorDetail> details = List.of(
				new FieldErrorDetail("payload.segments[10]", "REQUIRED"),
				new FieldErrorDetail("payload.segments[2].text", "REQUIRED"),
				new FieldErrorDetail("payload.segments[2]", "REQUIRED"),
				new FieldErrorDetail("payload.segments", "REQUIRED"),
				new FieldErrorDetail("title", "REQUIRED"),
				new FieldErrorDetail("payload.body", "REQUIRED"));

		List<String> fields = FieldErrorCodes.reduce(details).stream()
				.map(FieldErrorDetail::field)
				.toList();

		assertThat(fields).containsExactly(
				"payload.body",
				"payload.segments",
				"payload.segments[2]",
				"payload.segments[2].text",
				"payload.segments[10]",
				"title");
	}
}
