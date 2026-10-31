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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final AccountService accounts;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final String baseUrl;

	PasswordLinkService(OneTimeTokenRepository tokens, UserRepository users, AccountService accounts,
			PasswordEncoder passwordEncoder, Clock clock, @Value("${APP_BASE_URL}") String baseUrl) {
		this.tokens = tokens;
		this.users = users;
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	/** Issues a link for the account and invalidates its older unused links. */
	@Transactional
	public String issue(UUID userId, TokenPurpose purpose) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant now = clock.instant();
		tokens.markUnusedAsUsed(userId, now);
		tokens.save(OneTimeToken.create(hash(token), userId, purpose, now, now.plus(VALIDITY)));
		return baseUrl + "/set-password#token=" + token;
	}

	/** Sets the password, consumes the link and ends the account's sessions; returns the user. */
	@Transactional
	public User use(String token, String password) {
		AccountService.requireEncodable(password);
		OneTimeToken stored = tokens.findById(hash(token)).orElseThrow(PasswordLinkService::invalid);
		if (tokens.markUsed(stored.getId(), clock.instant()) != 1) {
			throw invalid();
		}
		User user = users.findById(stored.getUserId()).orElseThrow(PasswordLinkService::invalid);
		user.setPasswordHash(passwordEncoder.encode(password));
		users.save(user);
		accounts.endSessions(user.getId());
		return user;
	}

	private static ApiException invalid() {
		return new ApiException(HttpStatus.GONE, "LINK_INVALID");
	}

	private static String hash(String token) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
