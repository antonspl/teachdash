package com.teachdash.items.domain.freetext;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FreeTextPayload(
		@NotBlank @Size(max = FreeTextPayload.MAX_PROMPT_LENGTH) String prompt,
		@Min(FreeTextPayload.MIN_LIMIT) @Max(FreeTextPayload.MAX_LIMIT) Integer minLength,
		@Min(FreeTextPayload.MIN_LIMIT) @Max(FreeTextPayload.MAX_LIMIT) Integer maxLength) {

	static final int MAX_PROMPT_LENGTH = 20000;

	static final int MIN_LIMIT = 1;

	static final int MAX_LIMIT = 10000;
}
