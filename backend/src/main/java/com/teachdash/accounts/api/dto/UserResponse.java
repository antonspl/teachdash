package com.teachdash.accounts.api.dto;

import java.util.UUID;

import com.teachdash.accounts.domain.Role;

public record UserResponse(UUID id, String name, String email, Role role, String timeZone) {
}
