package com.teachdash.common.validation;

public final class Normalize {

	private Normalize() {
	}

	// Linear scan, not a regex: a regex with an end anchor is quadratic on a long inner run of separators.
	public static String name(String value) {
		if (value == null) {
			return null;
		}
		int start = 0;
		int end = value.length();
		while (start < end && isSeparatorOrControl(value.codePointAt(start))) {
			start += Character.charCount(value.codePointAt(start));
		}
		while (end > start && isSeparatorOrControl(value.codePointBefore(end))) {
			end -= Character.charCount(value.codePointBefore(end));
		}
		return value.substring(start, end);
	}

	private static boolean isSeparatorOrControl(int codePoint) {
		return switch (Character.getType(codePoint)) {
			case Character.SPACE_SEPARATOR,
					Character.LINE_SEPARATOR,
					Character.PARAGRAPH_SEPARATOR,
					Character.CONTROL,
					Character.FORMAT,
					Character.SURROGATE,
					Character.PRIVATE_USE,
					Character.UNASSIGNED -> true;
			default -> false;
		};
	}

	public static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
