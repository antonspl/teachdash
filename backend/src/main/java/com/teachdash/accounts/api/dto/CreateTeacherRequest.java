package com.teachdash.accounts.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTeacherRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank String timeZone) {
}
