package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.TimeZoneId;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @TimeZoneId String timeZone) {
}
