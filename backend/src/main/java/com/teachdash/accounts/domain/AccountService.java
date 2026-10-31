package com.teachdash.accounts.domain;

import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;

@Service
public class AccountService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	AccountService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	/** Checks credentials; the failure reason is an error code (design D7, R2). */
	public User authenticate(String email, String password) {
		User user = users.findByEmailIgnoreCase(email.strip())
				.orElseThrow(() -> unauthorized("UNKNOWN_EMAIL"));
		if (user.getPasswordHash() == null) {
			throw unauthorized("PASSWORD_NOT_SET");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw unauthorized("WRONG_PASSWORD");
		}
		if (!user.isActive()) {
			throw unauthorized("ACCOUNT_DEACTIVATED");
		}
		return user;
	}

	public User get(UUID id) {
		return users.findById(id).orElseThrow(() -> unauthorized("UNAUTHENTICATED"));
	}

	public User updateProfile(UUID id, String name, String timeZone) {
		if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_ZONE");
		}
		User user = get(id);
		user.updateProfile(name, timeZone);
		return users.save(user);
	}

	private static ApiException unauthorized(String code) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code);
	}
}
