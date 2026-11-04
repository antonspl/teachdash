package com.teachdash.common.validation;

import java.util.regex.Pattern;

public final class Normalize {

	private Normalize() {
	}

	private static final Pattern EDGE_SEPARATORS_AND_CONTROLS = Pattern.compile("^[\\p{Z}\\p{C}]+|[\\p{Z}\\p{C}]+$");

	public static String name(String value) {
		return value == null ? null : EDGE_SEPARATORS_AND_CONTROLS.matcher(value).replaceAll("");
	}

	public static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
