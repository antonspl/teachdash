package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.BcryptPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
		@NotBlank @Size(max = 64) String token,
		@NotBlank @BcryptPassword String password) {
}
