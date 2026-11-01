package com.teachdash.accounts.api.dto;

import com.teachdash.common.validation.TimeZoneId;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @TimeZoneId String timeZone) {
}
