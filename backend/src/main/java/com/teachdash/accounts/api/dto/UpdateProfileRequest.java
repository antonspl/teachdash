package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;

public record UpdateProfileRequest(
		@PersonName String name,
		@TimeZoneId String timeZone) {
}
