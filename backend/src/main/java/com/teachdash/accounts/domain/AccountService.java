package com.teachdash.accounts.domain;

import java.time.Clock;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;
import com.teachdash.common.validation.BcryptPasswordValidator;

@Service
public class AccountService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final PasswordLinkService links;
	private final Clock clock;
	private final TransactionTemplate transaction;
	private final String dummyHash;

	AccountService(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			PasswordLinkService links,
			Clock clock,
			PlatformTransactionManager transactionManager) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.links = links;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
		this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/**
	 * Creates an account without a password and issues its set-password link; joins the caller's
	 * transaction. Callers check for an existing email first, so a duplicate here is a lost race.
	 */
	@Transactional
	public CreatedAccount createPasswordlessUser(Role role, String name, String email, String timeZone) {
		User user;
		try {
			user = users.save(User.create(
					email,
					name,
					role,
					null,
					timeZone,
					clock.instant()));
		} catch (DuplicateKeyException e) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN");
		}
		return new CreatedAccount(user.getId(), links.issue(user.getId(), TokenPurpose.SET_PASSWORD));
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

	/** Always runs one password check, so response time does not reveal an account. */
	public User authenticate(String email, String password) {
		User user = users.findByEmailIgnoreCase(email).orElse(null);
		boolean canCheckRealHash = user != null
				&& user.getPasswordHash() != null
				&& !BcryptPasswordValidator.exceedsBcryptLimit(password);
		String hashToCheck = canCheckRealHash ? user.getPasswordHash() : dummyHash;
		boolean passwordMatched = passwordEncoder.matches(password, hashToCheck);
		// The dummy check's result must never count as a match.
		if (!canCheckRealHash || !passwordMatched) {
			throw unauthorized("INVALID_CREDENTIALS");
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
		return users.updateProfile(id, name, timeZone)
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
