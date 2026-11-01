package com.teachdash.accounts.domain;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.common.validation.BcryptPasswordValidator;

@Service
public class AccountService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final PasswordLinkService links;
	private final TransactionTemplate transaction;

	AccountService(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			PasswordLinkService links,
			PlatformTransactionManager transactionManager) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.links = links;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	/**
	 * Changes the password if the caller's session version is still current, revokes older
	 * sessions and unused links; returns the user and the new session version. Bcrypt runs before the
	 * transaction starts, so no database connection is held while hashing.
	 */
	public PasswordChanged changePassword(
			UUID id,
			String currentPassword,
			String newPassword,
			int expectedSessionVersion) {
		User user = get(id);
		boolean currentPasswordMatches = user.getPasswordHash() != null
				&& matches(currentPassword, user.getPasswordHash());
		if (!currentPasswordMatches) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD");
		}
		String newPasswordHash = passwordEncoder.encode(newPassword);
		int newSessionVersion = transaction.execute(status -> {
			links.invalidateLinks(id);
			return users.updatePasswordHashIfSessionCurrent(id, newPasswordHash, expectedSessionVersion)
					.orElseThrow(() -> unauthorized("UNAUTHENTICATED"));
		});
		return new PasswordChanged(user, newSessionVersion);
	}

	/** Checks credentials; the failure reason is an error code (design D7, R2). */
	public User authenticate(String email, String password) {
		User user = users.findByEmailIgnoreCase(email.strip())
				.orElseThrow(() -> unauthorized("UNKNOWN_EMAIL"));
		if (user.getPasswordHash() == null) {
			throw unauthorized(user.isActive() ? "PASSWORD_NOT_SET" : "ACCOUNT_DEACTIVATED");
		}
		if (!matches(password, user.getPasswordHash())) {
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
		return users.updateProfile(id, name.strip(), timeZone)
				.orElseThrow(() -> unauthorized("UNAUTHENTICATED"));
	}

	private boolean matches(String password, String passwordHash) {
		return !BcryptPasswordValidator.exceedsBcryptLimit(password)
				&& passwordEncoder.matches(password, passwordHash);
	}

	public record PasswordChanged(
			User user,
			int sessionVersion) {
	}

	private static ApiException unauthorized(String code) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code);
	}
}
