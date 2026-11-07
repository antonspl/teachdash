package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.Normalize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Size(max = 320) String email,
		@NotBlank String password) {

	public LoginRequest {
		email = Normalize.strip(email);
	}

	@Override
	public String toString() {
		return "LoginRequest[email=" + email + "]";
	}
}
