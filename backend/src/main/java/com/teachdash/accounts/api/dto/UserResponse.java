package com.teachdash.accounts.api.dto;

import java.util.UUID;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;

public record UserResponse(UUID id, String name, String email, Role role, String timeZone) {

	public static UserResponse of(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getTimeZone());
	}
}
