package com.teachdash.accounts;

import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Role role, String timeZone) {
}
