package com.teachdash.common;

import org.springframework.http.HttpStatus;

/** Error with a machine-readable code, rendered as a ProblemDetail (design D7). */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code) {
		super(code);
		this.status = status;
		this.code = code;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}
}
