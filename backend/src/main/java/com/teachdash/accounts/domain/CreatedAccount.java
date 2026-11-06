package com.teachdash.accounts.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatedAccount(
		UUID id,
		String setPasswordLink) {
}
