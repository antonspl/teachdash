package com.teachdash.accounts.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank String name, @NotBlank String timeZone) {
}
