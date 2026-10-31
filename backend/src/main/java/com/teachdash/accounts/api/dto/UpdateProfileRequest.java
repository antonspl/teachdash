package com.teachdash.accounts.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 64) String timeZone) {
}
