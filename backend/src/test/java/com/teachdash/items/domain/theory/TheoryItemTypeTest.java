package com.teachdash.items.domain.theory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TheoryItemTypeTest {

	@Test
	@DisplayName("Student view returns the payload unchanged")
	void studentViewReturnsThePayloadUnchanged() {
		// given
		var payload = new TheoryPayload("## Present Simple\n\nUse **do/does**.");

		// when
		Object view = new TheoryItemType().studentView(payload);

		// then
		assertThat(view).isEqualTo(payload);
	}
}
