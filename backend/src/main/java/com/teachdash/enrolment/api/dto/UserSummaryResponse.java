package com.teachdash.enrolment.api.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teachdash.enrolment.domain.UserSummary;

public record UserSummaryResponse(
		UUID id,
		String name,
		String email,
		String timeZone,
		boolean active,
		@JsonInclude(JsonInclude.Include.NON_NULL) String nativeLanguage) {

	public static UserSummaryResponse from(UserSummary user) {
		return new UserSummaryResponse(
				user.id(),
				user.name(),
				user.email(),
				user.timeZone(),
				user.active(),
				user.nativeLanguage());
	}

	public static List<UserSummaryResponse> from(List<UserSummary> users) {
		return users.stream().map(UserSummaryResponse::from).toList();
	}
}
