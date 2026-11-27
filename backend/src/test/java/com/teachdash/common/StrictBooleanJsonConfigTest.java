package com.teachdash.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.teachdash.support.ApiTest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

class StrictBooleanJsonConfigTest extends ApiTest {

	@Autowired
	private ObjectMapper mapper;

	@Test
	@DisplayName("Boolean parsing is strict: only true and false bind")
	void onlyTrueAndFalseBind() {
		// given the application's mapper

		// when / then true and false bind to Boolean and boolean
		assertThat(mapper.readValue("true", Boolean.class)).isTrue();
		assertThat(mapper.readValue("false", Boolean.class)).isFalse();
		assertThat(mapper.readValue("true", Boolean.TYPE)).isTrue();
		assertThat(mapper.readValue("false", Boolean.TYPE)).isFalse();
	}

	@Test
	@DisplayName("Boolean parsing is strict: a string, an integer, a float and an empty string fail as a wrong type")
	void everythingElseFails() {
		// given the application's mapper

		// when / then
		for (String json : new String[] { "\"true\"", "\"TRUE\"", "\"false\"", "1", "0", "2", "1.0", "\"\"" }) {
			assertThatThrownBy(() -> mapper.readValue(json, Boolean.class))
					.as("Boolean from %s", json)
					.isInstanceOf(MismatchedInputException.class);
			assertThatThrownBy(() -> mapper.readValue(json, Boolean.TYPE))
					.as("boolean from %s", json)
					.isInstanceOf(MismatchedInputException.class);
		}
	}
}
