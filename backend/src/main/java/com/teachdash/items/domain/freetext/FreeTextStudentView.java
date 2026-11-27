package com.teachdash.items.domain.freetext;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FreeTextStudentView(
		String prompt,
		Integer minLength,
		Integer maxLength) {
}
