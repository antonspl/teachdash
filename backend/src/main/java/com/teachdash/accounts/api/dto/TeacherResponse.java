package com.teachdash.accounts.api.dto;

import java.util.UUID;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;

public record TeacherResponse(
		UUID id,
		String name,
		String email,
		Role role,
		String timeZone,
		String nativeLanguage,
		boolean active,
		boolean hasPassword) {

	public static TeacherResponse of(User user) {
		return new TeacherResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getTimeZone(),
				user.getNativeLanguage(),
				user.isActive(),
				user.getPasswordHash() != null);
	}
}
