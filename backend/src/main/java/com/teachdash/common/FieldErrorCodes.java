package com.teachdash.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.teachdash.common.validation.MaxUtf8Bytes;
import com.teachdash.languages.api.validation.ContentLanguage;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class FieldErrorCodes {

	static final String REQUIRED = "REQUIRED";
	static final String TOO_SHORT = "TOO_SHORT";
	static final String TOO_LONG = "TOO_LONG";
	static final String INVALID_FORMAT = "INVALID_FORMAT";
	static final String INVALID_TYPE = "INVALID_TYPE";
	static final String UNSUPPORTED_COURSE_LANGUAGE = "UNSUPPORTED_COURSE_LANGUAGE";

	private FieldErrorCodes() {
	}

	static boolean isFieldCode(String code) {
		return REQUIRED.equals(code) || INVALID_TYPE.equals(code) || INVALID_FORMAT.equals(code);
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
		if (constraint == ContentLanguage.class) {
			return UNSUPPORTED_COURSE_LANGUAGE;
		}
		return INVALID_FORMAT;
	}

	static List<FieldErrorDetail> fromViolations(Collection<? extends ConstraintViolation<?>> violations) {
		return reduce(violations.stream()
				.map(violation -> new FieldErrorDetail(violation.getPropertyPath().toString(), codeOf(violation)))
				.toList());
	}

	static List<FieldErrorDetail> reduce(Collection<FieldErrorDetail> details) {
		Map<String, FieldErrorDetail> byField = new TreeMap<>();
		for (FieldErrorDetail detail : details) {
			byField.merge(detail.field(), detail, FieldErrorCodes::higherPriority);
		}
		return List.copyOf(byField.values());
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
