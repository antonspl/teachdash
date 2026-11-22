package com.teachdash.common;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;

import jakarta.validation.ConstraintViolation;
import tools.jackson.databind.DatabindException;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<FieldErrorDetail> errors;

	public ApiException(HttpStatus status, String code) {
		this(status, code, List.of());
	}

	private ApiException(HttpStatus status, String code, List<FieldErrorDetail> errors) {
		super(code);
		this.status = status;
		this.code = code;
		this.errors = errors;
	}

	public static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
	}

	public static ApiException validation(Set<? extends ConstraintViolation<?>> violations) {
		return new ApiException(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				FieldErrorCodes.fromViolations(violations));
	}

	public static ApiException validation(Set<? extends ConstraintViolation<?>> violations, String prefix) {
		return new ApiException(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				FieldErrorCodes.fromViolations(violations, prefix));
	}

	/**
	 * Never gives REQUIRED (a missing property is null and Bean Validation reports it) and never echoes a Map key
	 * or an unknown property name, because both are user text. Any Jackson error that is not bad input
	 * (for example a definition problem) is rethrown and surfaces as a logged 500.
	 */
	public static ApiException fromJackson(DatabindException exception, String prefix) {
		String code = JacksonErrors.codeOf(exception);
		if (code == null) {
			throw exception;
		}
		String field = code.equals(FieldErrorCodes.UNKNOWN_FIELD) ? prefix : JacksonErrors.fieldOf(exception, prefix);
		return new ApiException(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				List.of(new FieldErrorDetail(field, code)));
	}

	public static ApiException fields(List<FieldErrorDetail> details) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", FieldErrorCodes.reduce(details));
	}

	public static ApiException field(HttpStatus status, String code, String field) {
		return new ApiException(status, code, List.of(new FieldErrorDetail(field, code)));
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public List<FieldErrorDetail> errors() {
		return errors;
	}
}
