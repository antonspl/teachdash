package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.BcryptPassword;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank @BcryptPassword String newPassword) {
}
