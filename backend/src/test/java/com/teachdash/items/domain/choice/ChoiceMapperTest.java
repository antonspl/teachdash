package com.teachdash.items.domain.choice;

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
import com.teachdash.common.StrictJsonTestConfig;
import com.teachdash.items.domain.ItemPayloadValidator;

@JsonTest
@Import({ItemPayloadValidator.class, ValidationAutoConfiguration.class, StrictJsonTestConfig.class})
class ChoiceMapperTest {

	@Autowired
	private ItemPayloadValidator validator;

	private ApiException validate(Map<String, Object> payload) {
		return catchThrowableOfType(
				ApiException.class,
				() -> validator.validate(new TestItemType(), payload));
	}

	private static Map<String, Object> option(String text, boolean correct) {
		return Map.of("text", text, "correct", correct);
	}

	@Test
	@DisplayName("The strict mapper gives UNKNOWN_FIELD for an unknown property on the payload")
	void anUnknownPropertyOnThePayloadIsUnknownField() {
		// given a payload with the unknown property timer
		var payload = Map.<String, Object>of(
				"questions", List.of(Map.of("text", "Q", "options", List.of(option("A", true), option("B", false)))),
				"timer", 60);

		// when the payload is validated for the test type
		ApiException error = validate(payload);

		// then the error is UNKNOWN_FIELD at payload, without the property name
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload", "UNKNOWN_FIELD"));
	}

	@Test
	@DisplayName("The strict mapper gives UNKNOWN_FIELD for an unknown property on a question")
	void anUnknownPropertyOnAQuestionIsUnknownField() {
		// given a question with the unknown property shuffle
		var payload = Map.<String, Object>of(
				"questions", List.of(Map.of(
						"text", "Q",
						"shuffle", true,
						"options", List.of(option("A", true), option("B", false)))));

		// when the payload is validated for the test type
		ApiException error = validate(payload);

		// then the error is UNKNOWN_FIELD at payload
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload", "UNKNOWN_FIELD"));
	}

	@Test
	@DisplayName("The strict mapper gives UNKNOWN_FIELD for an unknown property on an option")
	void anUnknownPropertyOnAnOptionIsUnknownField() {
		// given an option with the unknown property id
		var payload = Map.<String, Object>of(
				"questions", List.of(Map.of(
						"text", "Q",
						"options", List.of(
								Map.of("text", "A", "correct", true, "id", "opt-1"),
								option("B", false)))));

		// when the payload is validated for the test type
		ApiException error = validate(payload);

		// then the error is UNKNOWN_FIELD at payload
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload", "UNKNOWN_FIELD"));
	}

	@Test
	@DisplayName("The strict mapper gives INVALID_TYPE for a string where a boolean is expected")
	void aStringCorrectMarkIsInvalidType() {
		// given an option whose correct is the string "true"
		var payload = Map.<String, Object>of(
				"questions", List.of(Map.of(
						"text", "Q",
						"options", List.of(
								Map.of("text", "A", "correct", "true"),
								option("B", false)))));

		// when the payload is validated for the test type
		ApiException error = validate(payload);

		// then the error is INVALID_TYPE at the exact field, so the strict configs are in effect
		assertThat(error.errors()).containsExactly(
				new FieldErrorDetail("payload.questions[0].options[0].correct", "INVALID_TYPE"));
	}
}
