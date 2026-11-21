package com.teachdash.parts.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// STUB: pass 2 replaces this controller with the real create and list
@RestController
class PartController {

	@PostMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts")
	Object create(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	@GetMapping("/api/teacher/courses/{courseId}/lessons/{lessonId}/parts")
	Object list(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}
}
