package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;
import com.teachdash.languages.api.validation.NativeLanguage;

public record UpdateProfileRequest(
		@PersonName String name,
		@TimeZoneId String timeZone,
		@NativeLanguage String nativeLanguage) {

	public UpdateProfileRequest {
		name = Normalize.name(name);
		nativeLanguage = Normalize.strip(nativeLanguage);
	}
}
