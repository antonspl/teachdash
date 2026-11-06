package com.teachdash.enrolment.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.common.ApiException;

@RestController
class TeacherJoinLinkController {

	// STUB: pass 2 implements the teacher join link endpoints.
	@GetMapping("/api/teacher/join-link")
	void view() {
		throw notImplemented();
	}

	// STUB
	@PostMapping("/api/teacher/join-link")
	void generate() {
		throw notImplemented();
	}

	// STUB
	@DeleteMapping("/api/teacher/join-link")
	void disable() {
		throw notImplemented();
	}

	private static ApiException notImplemented() {
		return new ApiException(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED");
	}
}
