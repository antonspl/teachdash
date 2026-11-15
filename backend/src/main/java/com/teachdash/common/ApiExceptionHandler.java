package com.teachdash.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolation;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handle(ApiException e) {
		return ResponseEntity.status(e.status()).body(problem(e.status(), e.code()));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problem = problem(status, "VALIDATION_FAILED");
		problem.setProperty("errors", fieldErrors(ex));
		return ResponseEntity.status(status).body(problem);
	}

	private static List<FieldErrorDetail> fieldErrors(MethodArgumentNotValidException ex) {
		if (!ex.getBindingResult().getGlobalErrors().isEmpty()) {
			throw new IllegalStateException("Class-level constraints are not supported: " + ex.getObjectName());
		}
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorDetail(error.getField(), codeOf(error)))
				.toList();
		return FieldErrorCodes.reduce(details);
	}

	private static String codeOf(FieldError error) {
		if (error.contains(ConstraintViolation.class)) {
			return FieldErrorCodes.codeOf(error.unwrap(ConstraintViolation.class));
		}
		return FieldErrorCodes.INVALID_FORMAT;
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		String code = containsNul(ex) ? "VALIDATION_FAILED" : "MALFORMED_REQUEST";
		return ResponseEntity.status(status).body(problem(status, code));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex,
			Object body,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem && !hasCode(problem)) {
			HttpStatus resolved = HttpStatus.resolve(status.value());
			problem.setProperty("code", resolved == null ? "ERROR" : resolved.name());
		}
		return response;
	}

	private static boolean containsNul(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof NulRejectingJsonConfig.NulCharacterException) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasCode(ProblemDetail problem) {
		return problem.getProperties() != null && problem.getProperties().containsKey("code");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception e) throws Exception {
		if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
			throw e; // left to the security filter chain
		}
		log.error("Unhandled exception", e);
		return ResponseEntity.internalServerError()
				.body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR"));
	}

	private static ProblemDetail problem(HttpStatusCode status, String code) {
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setProperty("code", code);
		return problem;
	}
}
