package com.teachdash.common;

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

import com.teachdash.items.domain.ItemPayloadValidator;
import com.teachdash.items.domain.dropdown.DropdownItemType;

/** Lives in this package because the strict integer configuration is package-private. */
@JsonTest
@Import({StrictIntegerJsonConfig.class, StrictStringJsonConfig.class, ItemPayloadValidator.class,
		ValidationAutoConfiguration.class})
class DropdownCorrectJsonTypeTest {

	@Autowired
	private ItemPayloadValidator validator;

	@Test
	@DisplayName("A string, a boolean or a fraction for correct gives INVALID_TYPE at its path")
	void aWronglyTypedCorrectIsInvalidType() {
		for (Object correct : List.of("1", true, 1.5)) {
			// given a gap whose correct is the string "1", the boolean true or the fraction 1.5
			var segment = Map.of("type", "gap", "options", List.of("a", "b"), "correct", correct);
			var payload = Map.<String, Object>of("segments", List.of(segment));

			// when the payload is validated for the dropdown type
			ApiException error = catchThrowableOfType(
					ApiException.class,
					() -> validator.validate(new DropdownItemType(), payload));

			// then the error is INVALID_TYPE at payload.segments[0].correct
			assertThat(error).isNotNull();
			assertThat(error.errors())
					.containsExactly(new FieldErrorDetail("payload.segments[0].correct", "INVALID_TYPE"));
		}
	}
}
