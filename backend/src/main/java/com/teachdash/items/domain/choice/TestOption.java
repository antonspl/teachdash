package com.teachdash.items.domain.choice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestOption(
		@NotBlank @Size(max = MAX_TEXT_LENGTH) String text,
		@NotNull Boolean correct) {

	public static final int MAX_TEXT_LENGTH = 100;
}
