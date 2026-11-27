package com.teachdash.items.domain.truefalse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrueFalseStatement(
		@NotBlank @Size(max = MAX_TEXT_LENGTH) String text,
		@NotBlank String answer) {

	public static final int MAX_TEXT_LENGTH = 500;
}
