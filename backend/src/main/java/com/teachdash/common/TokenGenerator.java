package com.teachdash.common;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenGenerator() {
	}

	/** Returns 32 random bytes, base64url without padding (43 characters). */
	public static String generate() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
