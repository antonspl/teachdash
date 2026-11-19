package com.teachdash.courses.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseController {

	// STUB: replaced by the real endpoint in pass 2
	@PostMapping("/api/teacher/courses")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void create() {
	}
}
