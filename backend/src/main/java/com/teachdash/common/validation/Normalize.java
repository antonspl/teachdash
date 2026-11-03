package com.teachdash.common.validation;

public final class Normalize {

	private Normalize() {
	}

	public static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
