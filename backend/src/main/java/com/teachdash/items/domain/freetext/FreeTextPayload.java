package com.teachdash.items.domain.freetext;

// STUB: no validation until task 3.1
public record FreeTextPayload(
		String prompt,
		Integer minLength,
		Integer maxLength) {
}
