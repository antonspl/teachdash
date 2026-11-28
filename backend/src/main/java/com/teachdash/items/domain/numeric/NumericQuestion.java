package com.teachdash.items.domain.numeric;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NumericQuestion(
		@NotBlank @Size(max = MAX_TEXT_LENGTH) String text,
		@NotBlank @Pattern(regexp = ANSWER_PATTERN) String answer,
		@NotBlank @Pattern(regexp = TOLERANCE_PATTERN) String tolerance,
		String unit) {

	public static final int MAX_TEXT_LENGTH = 500;

	public static final int MAX_UNIT_LENGTH = 20;

	/** Up to 15 integer and 10 fraction digits, a dot, no exponent or plus. */
	public static final String ANSWER_PATTERN = "^-?\\d{1,15}(\\.\\d{1,10})?$";

	/** The same, with no minus. */
	public static final String TOLERANCE_PATTERN = "^\\d{1,15}(\\.\\d{1,10})?$";
}
