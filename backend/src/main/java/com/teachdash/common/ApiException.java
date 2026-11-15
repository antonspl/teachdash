package com.teachdash.common;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;

import jakarta.validation.ConstraintViolation;

/** Error with a machine-readable code, rendered as a ProblemDetail (design D7). */
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

	public static ApiException field(HttpStatus status, String code, String field) {
		return new ApiException(status, code, List.of(new FieldErrorDetail(field, code)));
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	List<FieldErrorDetail> errors() {
		return errors;
	}
}
