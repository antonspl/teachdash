package com.teachdash.lessons.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// STUB: temporary, replaced by the real controller in pass 2 (lessons-create task 3.2)
@RestController
class LessonController {

	@PostMapping("/api/teacher/courses/{courseId}/lessons")
	void create(@PathVariable UUID courseId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	@GetMapping("/api/teacher/courses/{courseId}/lessons")
	void list(@PathVariable UUID courseId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}
}
