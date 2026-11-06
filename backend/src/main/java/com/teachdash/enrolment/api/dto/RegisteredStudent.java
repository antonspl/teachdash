package com.teachdash.enrolment.api.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teachdash.accounts.domain.CreatedAccount;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisteredStudent(
		UUID id,
		String setPasswordLink) {

	public static RegisteredStudent of(CreatedAccount account) {
		return new RegisteredStudent(account.id(), account.setPasswordLink());
	}
}
