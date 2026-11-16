package com.teachdash.enrolment.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record UserSummary(
		UUID id,
		String name,
		String email,
		String timeZone,
		boolean active,
		@JsonInclude(JsonInclude.Include.NON_NULL) String nativeLanguage) {
}
