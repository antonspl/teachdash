package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;
import com.teachdash.languages.api.validation.NativeLanguage;

import jakarta.validation.constraints.NotBlank;

public record CreateTeacherRequest(
		@PersonName String name,
		@EmailAddress String email,
		@TimeZoneId String timeZone,
		@NotBlank @NativeLanguage String nativeLanguage) {

	public CreateTeacherRequest {
		name = Normalize.name(name);
		email = Normalize.strip(email);
		nativeLanguage = Normalize.strip(nativeLanguage);
	}
}
