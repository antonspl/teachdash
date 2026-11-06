package com.teachdash.enrolment.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.common.ApiException;

@RestController
class JoinLinkController {

	// STUB: pass 2 implements the public preview.
	@PostMapping("/api/join-links/preview")
	void preview() {
		throw notImplemented();
	}

	// STUB: pass 2 implements the visitor and student join.
	@PostMapping("/api/join-links/join")
	void join() {
		throw notImplemented();
	}

	private static ApiException notImplemented() {
		return new ApiException(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED");
	}
}
