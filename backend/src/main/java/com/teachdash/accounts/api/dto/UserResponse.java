package com.teachdash.accounts.api.dto;

import java.util.UUID;

import com.teachdash.accounts.domain.Role;

// STUB: compile scaffolding for pass 1; pass 2 may keep or rewrite it
public record UserResponse(UUID id, String name, String email, Role role, String timeZone) {
}
