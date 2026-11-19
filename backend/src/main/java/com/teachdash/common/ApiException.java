package com.teachdash.common;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import jakarta.validation.ConstraintViolation;

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

	public static ApiException validation(BindingResult result) {
		if (!result.getGlobalErrors().isEmpty()) {
			throw new IllegalStateException("Class-level constraints are not supported, object: " + result.getObjectName());
		}
		List<FieldErrorDetail> details = FieldErrorCodes.fromFieldErrors(result.getFieldErrors());
		if (details.isEmpty()) {
			throw new IllegalStateException("Validation failed with no field errors, object: " + result.getObjectName());
		}
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", details);
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
