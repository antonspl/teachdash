package com.teachdash.enrolment.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.teachdash.enrolment.api")
class MalformedIdAdvice {

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handle(MethodArgumentTypeMismatchException e) {
		boolean malformedPathId = e.getParameter().hasParameterAnnotation(PathVariable.class);
		HttpStatus status = malformedPathId ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setProperty("code", status.name());
		return ResponseEntity.status(status).body(problem);
	}
}
