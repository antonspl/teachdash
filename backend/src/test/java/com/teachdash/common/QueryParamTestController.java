package com.teachdash.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Size;

/** Test-only endpoint with query parameters; no real endpoint has one yet. */
@RestController
class QueryParamTestController {

	@GetMapping("/api/teacher/test-query")
	void query(
			@RequestParam int page,
			@RequestParam(required = false) @Size(max = 10) String q) {
	}
}
