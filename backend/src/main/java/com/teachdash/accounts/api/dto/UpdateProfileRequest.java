package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;

import tools.jackson.databind.JsonNode;

public record UpdateProfileRequest(
		@PersonName String name,
		@TimeZoneId String timeZone,
		JsonNode nativeLanguage) {

	public UpdateProfileRequest {
		name = Normalize.name(name);
	}
}
