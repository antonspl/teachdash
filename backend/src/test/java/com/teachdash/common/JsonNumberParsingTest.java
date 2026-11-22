package com.teachdash.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.teachdash.support.ApiTest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

class JsonNumberParsingTest extends ApiTest {

	@Autowired
	private ObjectMapper mapper;

	@Test
	@DisplayName("Integer parsing is strict: a string, a fraction and 1e400 fail as a wrong type")
	void integerParsingIsStrict() {
		// given the application's mapper

		// when / then
		for (String json : new String[] { "\"5\"", "1.5", "1e400" }) {
			assertThatThrownBy(() -> mapper.readValue(json, Integer.class))
					.as("Integer from %s", json)
					.isInstanceOf(MismatchedInputException.class);
		}
		assertThat(mapper.readValue("5", Integer.class)).isEqualTo(5);
	}

	@Test
	@DisplayName("Long parsing is strict: a string, a fraction and 1e400 fail as a wrong type")
	void longParsingIsStrict() {
		// given the application's mapper

		// when / then
		for (String json : new String[] { "\"5\"", "1.5", "1e400" }) {
			assertThatThrownBy(() -> mapper.readValue(json, Long.class))
					.as("Long from %s", json)
					.isInstanceOf(MismatchedInputException.class);
		}
		assertThat(mapper.readValue("5", Long.class)).isEqualTo(5L);
	}

	@Test
	@DisplayName("Floating-point numbers in an untyped object stay BigDecimal with every digit")
	void decimalsKeepEveryDigit() {
		// given a JSON object with a 20-digit fraction and 1e200000

		// when
		Map<?, ?> read = mapper.readValue("{\"a\": 0.12345678901234567890, \"b\": 1e200000}", Map.class);

		// then
		assertThat(read.get("a")).isInstanceOf(BigDecimal.class);
		assertThat((BigDecimal) read.get("a")).isEqualByComparingTo(new BigDecimal("0.12345678901234567890"));
		assertThat(read.get("b")).isInstanceOf(BigDecimal.class);
		assertThat((BigDecimal) read.get("b")).isEqualByComparingTo(new BigDecimal("1e200000"));
	}
}
