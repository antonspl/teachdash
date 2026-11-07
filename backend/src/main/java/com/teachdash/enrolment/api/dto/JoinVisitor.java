package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.BcryptPassword;
import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;

import jakarta.validation.constraints.NotBlank;

public record JoinVisitor(
		@PersonName String name,
		@EmailAddress String email,
		@TimeZoneId String timeZone,
		@NotBlank @BcryptPassword String password) {

	public JoinVisitor {
		name = Normalize.name(name);
		email = Normalize.strip(email);
	}

	public static JoinVisitor of(JoinLinkJoinRequest request) {
		return new JoinVisitor(request.name(), request.email(), request.timeZone(), request.password());
	}
}
