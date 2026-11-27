package com.teachdash.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolation;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handle(ApiException e) {
		ProblemDetail problem = problem(e.status(), e.code());
		if (!e.errors().isEmpty()) {
			problem.setProperty("errors", e.errors());
		}
		return ResponseEntity.status(e.status()).body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		if (!ex.getBindingResult().getGlobalErrors().isEmpty()) {
			return globalErrorsNotSupported(ex.getObjectName());
		}
		List<FieldErrorDetail> details = fieldErrors(ex);
		if (details.isEmpty()) {
			log.error("Validation failed with no field errors, object: {}", ex.getObjectName());
			return internalError();
		}
		return validationFailed(status, details);
	}

	private static List<FieldErrorDetail> fieldErrors(MethodArgumentNotValidException ex) {
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
		JacksonException jackson = firstWithPath(ex);
		String field = jackson == null ? "" : JacksonErrors.fieldOf(jackson, "");
		boolean unknownProperty = jackson instanceof UnrecognizedPropertyException;
		String fieldCode = field.isEmpty() || unknownProperty ? null : JacksonErrors.codeOf(ex, jackson);
		if (fieldCode == null) {
			return ResponseEntity.status(status).body(problem(status, "MALFORMED_REQUEST"));
		}
		ProblemDetail problem = problem(status, "VALIDATION_FAILED");
		problem.setProperty("errors", List.of(new FieldErrorDetail(field, fieldCode)));
		return ResponseEntity.status(status).body(problem);
	}

	private static JacksonException firstWithPath(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof JacksonException jackson
					&& (cause instanceof DatabindException || cause instanceof InputCoercionException)
					&& !jackson.getPath().isEmpty()) {
				return jackson;
			}
		}
		return null;
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		String field = ex.getPropertyName();
		if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
			if (mismatch.getParameter().hasParameterAnnotation(PathVariable.class)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(problem(HttpStatus.NOT_FOUND, "NOT_FOUND"));
			}
			field = mismatch.getName();
		}
		if (field == null) {
			return ResponseEntity.status(status).body(problem(status, "MALFORMED_REQUEST"));
		}
		return validationFailed(status, List.of(new FieldErrorDetail(field, FieldErrorCodes.INVALID_TYPE)));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		FieldErrorDetail detail = new FieldErrorDetail(ex.getParameterName(), FieldErrorCodes.REQUIRED);
		return validationFailed(status, List.of(detail));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		if (ex.isForReturnValue()) {
			log.error("Return value validation failed in {}", ex.getMethod());
			return internalError();
		}
		List<FieldErrorDetail> details = new ArrayList<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			if (result instanceof ParameterErrors errors) {
				if (errors.hasGlobalErrors()) {
					return globalErrorsNotSupported(errors.getObjectName());
				}
				for (FieldError error : errors.getFieldErrors()) {
					details.add(new FieldErrorDetail(error.getField(), codeOf(error)));
				}
			}
			else {
				String field = result.getMethodParameter().getParameterName();
				if (field == null) {
					log.error("Parameter names are not available in {}, compile with -parameters", ex.getMethod());
					return internalError();
				}
				for (MessageSourceResolvable error : result.getResolvableErrors()) {
					ConstraintViolation<?> violation = result.unwrap(error, ConstraintViolation.class);
					details.add(new FieldErrorDetail(field, FieldErrorCodes.codeOf(violation)));
				}
			}
		}
		if (details.isEmpty()) {
			log.error("Validation failed with no field errors in {}", ex.getMethod());
			return internalError();
		}
		return validationFailed(status, FieldErrorCodes.reduce(details));
	}

	private static ResponseEntity<Object> globalErrorsNotSupported(String objectName) {
		log.error("Class-level constraints are not supported, object: {}", objectName);
		return internalError();
	}

	private static ResponseEntity<Object> internalError() {
		return ResponseEntity.internalServerError()
				.body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR"));
	}

	private static ResponseEntity<Object> validationFailed(HttpStatusCode status, List<FieldErrorDetail> errors) {
		ProblemDetail problem = problem(status, "VALIDATION_FAILED");
		problem.setProperty("errors", errors);
		return ResponseEntity.status(status).body(problem);
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
