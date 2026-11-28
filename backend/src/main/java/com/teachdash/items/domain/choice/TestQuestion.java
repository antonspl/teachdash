package com.teachdash.items.domain.choice;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record TestQuestion(
		@NotBlank @Size(max = MAX_TEXT_LENGTH) String text,
		Boolean multiple,
		@NotEmpty @Size(min = MIN_OPTIONS, max = MAX_OPTIONS) List<@Valid TestOption> options) {

	public TestQuestion(String text, List<TestOption> options) {
		this(text, null, options);
	}

	public static final int MAX_TEXT_LENGTH = 500;

	public static final int MIN_OPTIONS = 2;

	public static final int MAX_OPTIONS = 10;
}
