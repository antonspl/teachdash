package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;

public record RegisterStudentRequest(
		@PersonName String name,
		@EmailAddress String email,
		@TimeZoneId String timeZone) {
}
