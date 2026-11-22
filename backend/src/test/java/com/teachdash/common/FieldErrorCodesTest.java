package com.teachdash.common;

import static org.assertj.core.api.Assertions.assertThat;

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
}
