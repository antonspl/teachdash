package com.teachdash.enrolment.api.dto;

import com.teachdash.common.validation.BcryptPassword;
import com.teachdash.common.validation.EmailAddress;
import com.teachdash.common.validation.Normalize;
import com.teachdash.common.validation.PersonName;
import com.teachdash.common.validation.TimeZoneId;
import com.teachdash.languages.api.validation.NativeLanguage;

public record JoinVisitor(
		@PersonName String name,
		@EmailAddress String email,
		@TimeZoneId String timeZone,
		@NativeLanguage String nativeLanguage,
		@BcryptPassword String password) {

	public JoinVisitor {
		name = Normalize.name(name);
		email = Normalize.strip(email);
		nativeLanguage = Normalize.strip(nativeLanguage);
	}

	@Override
	public String toString() {
		return "JoinVisitor[timeZone=" + timeZone + "]";
	}

	public static JoinVisitor of(JoinLinkJoinRequest request) {
		return new JoinVisitor(
				request.name(),
				request.email(),
				request.timeZone(),
				request.nativeLanguage(),
				request.password());
	}
}
