package com.teachdash.accounts.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import com.teachdash.common.TokenGenerator;

/** One-time set/reset password links (design D5); reusable for other roles. */
@Service
public class PasswordLinkService {

	private static final Duration VALIDITY = Duration.ofDays(7);

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

	/** Issues a set-password link and invalidates the account's older unused links. */
	@Transactional
	public String issue(UUID userId, TokenPurpose purpose, UUID issuerId) {
		if (purpose == TokenPurpose.RESET_PASSWORD) {
			throw new IllegalArgumentException("Reset links go through issueResetLink");
		}
		// Locks the user row first: user before links, see lock order.
		users.findByIdForUpdate(userId);
		return store(userId, purpose, issuerId);
	}

	/**
	 * Issues a reset link for an active account and invalidates its older unused links. Nothing
	 * is stored or invalidated for a missing or deactivated account.
	 */
	@Transactional
	public String issueResetLink(UUID userId, UUID issuerId) {
		User user = users.findByIdForUpdate(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.CONFLICT, "TARGET_ACCOUNT_DEACTIVATED");
		}
		return store(userId, TokenPurpose.RESET_PASSWORD, issuerId);
	}

	private String store(UUID userId, TokenPurpose purpose, UUID issuerId) {
		String token = TokenGenerator.generate();
		Instant now = clock.instant();
		tokens.markUnusedAsUsed(userId, now);
		try {
			tokens.save(OneTimeToken.create(
					hash(token),
					userId,
					issuerId,
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

	/** Makes the account's unused links that the given account issued unusable. */
	public void invalidateLinks(UUID userId, UUID issuerId) {
		tokens.markUnusedIssuedByAsUsed(userId, issuerId, clock.instant());
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
			// Locks the user row first: user before links, see lock order.
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
