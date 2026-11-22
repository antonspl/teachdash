package com.teachdash.items.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.teachdash.common.ApiException;
import com.teachdash.common.FieldErrorDetail;
import com.teachdash.support.ApiTest;

class ItemPayloadValidatorTest extends ApiTest {

	@Autowired
	private ItemPayloadValidator validator;

	record AnswersPayload(String answers) {
	}

	static class AnswersType implements ItemType<AnswersPayload> {

		@Override
		public String code() {
			return "answers-test";
		}

		@Override
		public Class<AnswersPayload> payloadClass() {
			return AnswersPayload.class;
		}

		@Override
		public Object studentView(AnswersPayload payload) {
			return Map.of();
		}

		@Override
		public List<FieldErrorDetail> extraChecks(AnswersPayload payload) {
			return List.of(new FieldErrorDetail("answers", "INVALID_FORMAT"));
		}
	}

	@Test
	@DisplayName("extraChecks fields get the payload prefix")
	void extraChecksFieldsArePrefixed() {
		// given
		var type = new AnswersType();

		// when
		ApiException error = catchThrowableOfType(
				ApiException.class,
				() -> validator.validate(type, Map.of("answers", "x")));

		// then
		assertThat(error.errors()).containsExactly(new FieldErrorDetail("payload.answers", "INVALID_FORMAT"));
	}
}
