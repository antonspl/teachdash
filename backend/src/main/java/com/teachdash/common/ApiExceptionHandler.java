package com.teachdash.common;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handle(ApiException e) {
		ProblemDetail problem = ProblemDetail.forStatus(e.status());
		problem.setProperty("code", e.code());
		return ResponseEntity.status(e.status()).body(problem);
	}
}
