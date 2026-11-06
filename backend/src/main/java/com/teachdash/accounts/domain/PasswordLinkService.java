package com.teachdash.accounts.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.teachdash.accounts.persistence.OneTimeTokenRepository;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.common.ApiException;

/** One-time set/reset password links (design D5); reusable for other roles. */
@Service
public class PasswordLinkService {

	private static final Duration VALIDITY = Duration.ofDays(7);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final OneTimeTokenRepository tokens;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final TransactionTemplate transaction;
	private final String baseUrl;

	PasswordLinkService(
			OneTimeTokenRepository tokens,
			UserRepository users,
			PasswordEncoder passwordEncoder,
			Clock clock,
			PlatformTransactionManager transactionManager,
			@Value("${APP_BASE_URL}") String baseUrl) {
		this.tokens = tokens;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
		this.baseUrl = baseUrl.endsWith("/")
				? baseUrl.substring(0, baseUrl.length() - 1)
				: baseUrl;
	}

	/** Issues a link for the account and invalidates its older unused links. */
	@Transactional
	public String issue(UUID userId, TokenPurpose purpose) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant now = clock.instant();
		users.findByIdForUpdate(userId);
		tokens.markUnusedAsUsed(userId, now);
		try {
			tokens.save(OneTimeToken.create(
					hash(token),
					userId,
					purpose,
					now,
					now.plus(VALIDITY)));
		} catch (DuplicateKeyException e) {
			throw new ApiException(HttpStatus.CONFLICT, "LINK_CONFLICT");
		}
		return baseUrl + "/set-password#token=" + token;
	}

	/** Makes all unused links of the account unusable. */
	public void invalidateLinks(UUID userId) {
		tokens.markUnusedAsUsed(userId, clock.instant());
	}

	/** Makes the account's unused links of one purpose unusable. */
	public void invalidateLinks(UUID userId, TokenPurpose purpose) {
		tokens.markUnusedAsUsed(userId, purpose.name(), clock.instant());
	}

	/**
	 * Sets the password, consumes the link and revokes older sessions; returns the updated user.
	 * The link is checked and the password hashed before the transaction starts, so no database
	 * connection is held while hashing; the update itself re-checks the link atomically.
	 */
	public User use(String token, String password) {
		OneTimeToken stored = tokens.findById(hash(token))
				.orElseThrow(PasswordLinkService::invalid);
		Instant now = clock.instant();
		User user = users.findById(stored.getUserId())
				.orElseThrow(PasswordLinkService::invalid);
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_DEACTIVATED");
		}
		boolean usedOrExpired = stored.getUsedAt() != null || !stored.getExpiresAt().isAfter(now);
		if (usedOrExpired) {
			throw invalid();
		}
		String passwordHash = passwordEncoder.encode(password);
		return transaction.execute(status -> {
			users.findByIdForUpdate(user.getId()).orElseThrow(PasswordLinkService::invalid);
			if (tokens.markUsed(stored.getId(), now) != 1) {
				throw invalid();
			}
			users.updatePasswordHashAndRevokeSessions(user.getId(), passwordHash);
			return users.findById(user.getId()).orElseThrow(PasswordLinkService::invalid);
		});
	}

	private static ApiException invalid() {
		return new ApiException(HttpStatus.GONE, "LINK_INVALID");
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
