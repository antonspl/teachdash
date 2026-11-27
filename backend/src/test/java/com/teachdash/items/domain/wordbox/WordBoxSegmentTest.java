package com.teachdash.items.domain.wordbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.items.domain.ItemPayloadValidator;

@JsonTest
@Import({ItemPayloadValidator.class, ValidationAutoConfiguration.class})
class WordBoxSegmentTest {

	@Autowired
	private ItemPayloadValidator validator;

	private ApiException validate(Map<String, Object> payload) {
		return catchThrowableOfType(
				ApiException.class,
				() -> validator.validate(new WordBoxItemType(), payload));
	}

	@Test
	@DisplayName("The strict mapper gives UNKNOWN_FIELD for an unknown property on a segment")
	void anUnknownPropertyOnASegmentIsUnknownField() {
		// given a payload whose text segment has the unknown property hint
		var payload = Map.<String, Object>of(
				"segments", List.of(Map.of("type", "text", "text", "Hi", "hint", "x")));

		// when the payload is validated for the word-box type
		ApiException error = validate(payload);

		// then the error is UNKNOWN_FIELD at payload, without the property name
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload", "UNKNOWN_FIELD"));
	}

	@Test
	@DisplayName("The strict mapper gives UNKNOWN_FIELD for an unknown property on the payload")
	void anUnknownPropertyOnThePayloadIsUnknownField() {
		// given a payload with the unknown property options
		var payload = Map.<String, Object>of(
				"segments", List.of(Map.of("type", "gap", "answer", "go")),
				"options", List.of("a"));

		// when the payload is validated for the word-box type
		ApiException error = validate(payload);

		// then the error is UNKNOWN_FIELD at payload
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload", "UNKNOWN_FIELD"));
	}
}
