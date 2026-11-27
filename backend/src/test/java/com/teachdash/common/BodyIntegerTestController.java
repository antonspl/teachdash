package com.teachdash.common;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint with an Integer field in the request body; no real endpoint has one outside a payload. */
@RestController
class BodyIntegerTestController {

	record Body(
			Integer count) {
	}

	@PostMapping("/api/teacher/test-body")
	void body(@RequestBody Body body) {
	}
}
