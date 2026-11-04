package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.Normalize;

public record StudentLookupRequest(@EmailAddress String email) {

	public StudentLookupRequest {
		email = Normalize.strip(email);
	}
}
