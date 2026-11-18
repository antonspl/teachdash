package com.teachdash.common.validation;

import java.nio.charset.StandardCharsets;

public final class Utf8 {

	private Utf8() {
	}

	public static boolean exceeds(String value, int maxBytes) {
		return value.getBytes(StandardCharsets.UTF_8).length > maxBytes;
	}
}
