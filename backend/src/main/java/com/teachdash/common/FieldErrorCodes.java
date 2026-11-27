package com.teachdash.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.teachdash.common.validation.MaxUtf8Bytes;
import com.teachdash.languages.api.validation.ContentLanguage;
import com.teachdash.languages.api.validation.NativeLanguage;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FieldErrorCodes {

	public static final String REQUIRED = "REQUIRED";
	public static final String TOO_SHORT = "TOO_SHORT";
	public static final String TOO_LONG = "TOO_LONG";
	public static final String INVALID_FORMAT = "INVALID_FORMAT";
	public static final String INVALID_TYPE = "INVALID_TYPE";
	public static final String UNSUPPORTED_COURSE_LANGUAGE = "UNSUPPORTED_COURSE_LANGUAGE";
	public static final String UNKNOWN_TYPE = "UNKNOWN_TYPE";
	public static final String UNKNOWN_FIELD = "UNKNOWN_FIELD";

	private static final Pattern PATH_PART = Pattern.compile("\\[(\\d+)]|([^.\\[\\]]+)");

	private FieldErrorCodes() {
	}

	static String codeOf(ConstraintViolation<?> violation) {
		Class<?> constraint = violation.getConstraintDescriptor().getAnnotation().annotationType();
		if (constraint == NotBlank.class || constraint == NotNull.class || constraint == NotEmpty.class) {
			return REQUIRED;
		}
		if (constraint == Size.class) {
			return isShorterThanMin(violation) ? TOO_SHORT : TOO_LONG;
		}
		if (constraint == MaxUtf8Bytes.class) {
			return TOO_LONG;
		}
		if (constraint == NativeLanguage.class && violation.getInvalidValue() instanceof String text && text.isBlank()) {
			return REQUIRED;
		}
		if (constraint == ContentLanguage.class) {
			return UNSUPPORTED_COURSE_LANGUAGE;
		}
		return INVALID_FORMAT;
	}

	static List<FieldErrorDetail> fromViolations(Collection<? extends ConstraintViolation<?>> violations) {
		return fromViolations(violations, "");
	}

	static List<FieldErrorDetail> fromViolations(Collection<? extends ConstraintViolation<?>> violations, String prefix) {
		return reduce(violations.stream()
				.map(violation -> new FieldErrorDetail(
						withPrefix(prefix, violation.getPropertyPath().toString()),
						codeOf(violation)))
				.toList());
	}

	public static String withPrefix(String prefix, String path) {
		if (prefix.isEmpty()) {
			return path;
		}
		return path.isEmpty() ? prefix : prefix + "." + path;
	}

	static List<FieldErrorDetail> reduce(Collection<FieldErrorDetail> details) {
		Map<String, FieldErrorDetail> byField = new TreeMap<>(FieldErrorCodes::comparePaths);
		for (FieldErrorDetail detail : details) {
			byField.merge(detail.field(), detail, FieldErrorCodes::higherPriority);
		}
		return List.copyOf(byField.values());
	}

	private static int comparePaths(String a, String b) {
		Matcher left = PATH_PART.matcher(a);
		Matcher right = PATH_PART.matcher(b);
		while (true) {
			boolean hasLeft = left.find();
			boolean hasRight = right.find();
			if (!hasLeft || !hasRight) {
				return Boolean.compare(hasLeft, hasRight);
			}
			int result = comparePart(left, right);
			if (result != 0) {
				return result;
			}
		}
	}

	private static int comparePart(Matcher left, Matcher right) {
		String leftIndex = left.group(1);
		String rightIndex = right.group(1);
		if (leftIndex != null && rightIndex != null) {
			return Integer.compare(Integer.parseInt(leftIndex), Integer.parseInt(rightIndex));
		}
		if (leftIndex != null || rightIndex != null) {
			return leftIndex != null ? 1 : -1;
		}
		return left.group(2).compareTo(right.group(2));
	}

	private static FieldErrorDetail higherPriority(FieldErrorDetail a, FieldErrorDetail b) {
		return priority(b.code()) > priority(a.code()) ? b : a;
	}

	private static int priority(String code) {
		return switch (code) {
			case REQUIRED -> 3;
			case TOO_SHORT, TOO_LONG -> 2;
			default -> 1;
		};
	}

	private static boolean isShorterThanMin(ConstraintViolation<?> violation) {
		Size size = (Size) violation.getConstraintDescriptor().getAnnotation();
		Object value = violation.getInvalidValue();
		if (value instanceof CharSequence text) {
			return text.length() < size.min();
		}
		if (value instanceof Collection<?> collection) {
			return collection.size() < size.min();
		}
		return false;
	}
}
