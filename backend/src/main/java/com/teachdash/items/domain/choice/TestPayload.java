package com.teachdash.items.domain.choice;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record TestPayload(
		@NotEmpty @Size(max = MAX_QUESTIONS) List<@Valid TestQuestion> questions) {

	public static final int MAX_QUESTIONS = 25;
}
