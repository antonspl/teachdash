package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;

public record CreateTeacherRequest(
		@PersonName String name,
		@EmailAddress String email,
		@TimeZoneId String timeZone) {

	public CreateTeacherRequest {
		name = Normalize.name(name);
		email = Normalize.strip(email);
	}
}
