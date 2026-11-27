package com.teachdash.items.domain.truefalse;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record TrueFalsePayload(
		Boolean allowNotStated,
		@NotEmpty @Size(max = MAX_STATEMENTS) List<@Valid TrueFalseStatement> statements) {

	public static final int MAX_STATEMENTS = 25;
}
