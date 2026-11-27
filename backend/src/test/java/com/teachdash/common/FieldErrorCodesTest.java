package com.teachdash.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.constraints.Size;

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

	@Test
	@DisplayName("Distinct paths that differ only in separators or index padding keep separate errors")
	void distinctPathsKeepSeparateErrors() {
		List<FieldErrorDetail> details = List.of(
				new FieldErrorDetail("a[x]", "REQUIRED"),
				new FieldErrorDetail("a.x", "REQUIRED"),
				new FieldErrorDetail("a.b", "REQUIRED"),
				new FieldErrorDetail("a..b", "REQUIRED"),
				new FieldErrorDetail("a[01]", "REQUIRED"),
				new FieldErrorDetail("a[1]", "REQUIRED"));

		List<String> fields = FieldErrorCodes.reduce(details).stream()
				.map(FieldErrorDetail::field)
				.toList();

		assertThat(fields).containsExactlyInAnyOrder("a[x]", "a.x", "a.b", "a..b", "a[01]", "a[1]");
	}

	private record Options(
			@Size(min = 2, max = 5) List<String> options) {
	}

	@Test
	@DisplayName("A list shorter than the Size minimum is TOO_SHORT and a longer than the maximum is TOO_LONG")
	void aListBelowTheSizeMinimumIsTooShort() {
		var validator = Validation.buildDefaultValidatorFactory().getValidator();

		var tooShort = FieldErrorCodes.fromViolations(validator.validate(new Options(List.of("a"))));
		var tooLong = FieldErrorCodes.fromViolations(validator.validate(new Options(List.of("a", "b", "c", "d", "e", "f"))));

		assertThat(tooShort).containsExactly(new FieldErrorDetail("options", "TOO_SHORT"));
		assertThat(tooLong).containsExactly(new FieldErrorDetail("options", "TOO_LONG"));
	}
}
