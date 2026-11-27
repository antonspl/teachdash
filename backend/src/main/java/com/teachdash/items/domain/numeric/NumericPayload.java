package com.teachdash.items.domain.numeric;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record NumericPayload(
		@NotEmpty @Size(max = MAX_QUESTIONS) List<@Valid NumericQuestion> questions) {

	public static final int MAX_QUESTIONS = 25;
}
