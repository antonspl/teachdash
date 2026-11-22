package com.teachdash.common;

import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

final class JacksonErrors {

	private JacksonErrors() {
	}

	/** The path under the prefix, cut at the first Map key: a key is user text and is never echoed. */
	static String fieldOf(DatabindException exception, String prefix) {
		StringBuilder field = new StringBuilder(prefix);
		for (JacksonException.Reference reference : exception.getPath()) {
			if (reference.getPropertyName() != null) {
				if (reference.from() instanceof Map<?, ?>) {
					break;
				}
				if (!field.isEmpty()) {
					field.append('.');
				}
				field.append(reference.getPropertyName());
			}
			else if (reference.getIndex() >= 0) {
				field.append('[').append(reference.getIndex()).append(']');
			}
		}
		return field.toString();
	}

	static String codeOf(DatabindException exception) {
		return codeOf(exception, exception);
	}

	/** Null when the error has no field code. */
	static String codeOf(Throwable error, DatabindException databind) {
		if (containsNul(error)) {
			return FieldErrorCodes.INVALID_FORMAT;
		}
		if (databind instanceof UnrecognizedPropertyException) {
			return FieldErrorCodes.UNKNOWN_FIELD;
		}
		if (databind instanceof MismatchedInputException) {
			return FieldErrorCodes.INVALID_TYPE;
		}
		return null;
	}

	private static boolean containsNul(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof NulRejectingJsonConfig.NulCharacterException) {
				return true;
			}
		}
		return false;
	}
}
