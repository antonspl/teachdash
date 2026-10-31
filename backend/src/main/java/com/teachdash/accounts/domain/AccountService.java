package com.teachdash.accounts.domain;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;

@Service
public class AccountService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JdbcIndexedSessionRepository sessions;

	AccountService(UserRepository users, PasswordEncoder passwordEncoder, JdbcIndexedSessionRepository sessions) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.sessions = sessions;
	}

	/** Deletes every session of the account (after commit when called inside a transaction) (the principal name is the user id, design D4). */
	public void endSessions(UUID userId) {
		Runnable end = () -> sessions.findByPrincipalName(userId.toString()).keySet().forEach(sessions::deleteById);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					end.run();
				}
			});
		} else {
			end.run();
		}
	}

	public void changePassword(UUID id, String currentPassword, String newPassword) {
		User user = get(id);
		if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD");
		}
		requireEncodable(newPassword);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		users.save(user);
	}

	/** Checks credentials; the failure reason is an error code (design D7, R2). */
	public User authenticate(String email, String password) {
		User user = users.findByEmailIgnoreCase(email.strip())
				.orElseThrow(() -> unauthorized("UNKNOWN_EMAIL"));
		if (user.getPasswordHash() == null) {
			throw unauthorized("PASSWORD_NOT_SET");
		}
		if (tooLongForBcrypt(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
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
		requireValidTimeZone(timeZone);
		User user = get(id);
		user.updateProfile(name.strip(), timeZone);
		return users.save(user);
	}

	/** bcrypt rejects passwords over 72 bytes; multi-byte characters can exceed it under a character limit. */
	static void requireEncodable(String password) {
		if (tooLongForBcrypt(password)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		}
	}

	private static boolean tooLongForBcrypt(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > 72;
	}

	static void requireValidTimeZone(String timeZone) {
		if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_ZONE");
		}
	}

	private static ApiException unauthorized(String code) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code);
	}
}
