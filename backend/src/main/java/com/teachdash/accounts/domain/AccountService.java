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

	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JdbcIndexedSessionRepository sessions;

	AccountService(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			JdbcIndexedSessionRepository sessions) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.sessions = sessions;
	}

	/**
	 * Deletes every session of the account; inside a transaction, only after it commits.
	 * The principal name of a session is the user id (design D4).
	 */
	public void endSessions(UUID userId) {
		endSessionsExcept(userId, null);
	}

	/** Like endSessions, but keeps the session with the given id. */
	public void endSessionsExcept(UUID userId, String keepSessionId) {
		Runnable endSessions = () -> sessions.findByPrincipalName(userId.toString())
				.keySet()
				.stream()
				.filter(sessionId -> !sessionId.equals(keepSessionId))
				.forEach(sessions::deleteById);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					endSessions.run();
				}
			});
		} else {
			endSessions.run();
		}
	}

	public void changePassword(
			UUID id,
			String currentPassword,
			String newPassword,
			String currentSessionId) {
		User user = get(id);
		boolean currentPasswordMatches = user.getPasswordHash() != null
				&& passwordEncoder.matches(currentPassword, user.getPasswordHash());
		if (!currentPasswordMatches) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD");
		}
		requireEncodable(newPassword);
		users.updatePasswordHash(id, passwordEncoder.encode(newPassword));
		endSessionsExcept(id, currentSessionId);
	}

	/** Checks credentials; the failure reason is an error code (design D7, R2). */
	public User authenticate(String email, String password) {
		User user = users.findByEmailIgnoreCase(email.strip())
				.orElseThrow(() -> unauthorized("UNKNOWN_EMAIL"));
		if (user.getPasswordHash() == null) {
			throw unauthorized("PASSWORD_NOT_SET");
		}
		boolean passwordMatches = !tooLongForBcrypt(password)
				&& passwordEncoder.matches(password, user.getPasswordHash());
		if (!passwordMatches) {
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
		get(id);
		users.updateProfile(id, name.strip(), timeZone);
		return get(id);
	}

	/** bcrypt rejects passwords over 72 bytes; multi-byte characters can exceed it under a character limit. */
	static void requireEncodable(String password) {
		if (tooLongForBcrypt(password)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		}
	}

	private static boolean tooLongForBcrypt(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
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
