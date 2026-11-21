package com.teachdash.courseassignment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** STUB: answers 501 until pass 2 replaces it. */
@RestController
class CourseAssignmentController {

	@PutMapping("/api/teacher/courses/{courseId}/students/{studentId}")
	ResponseEntity<Void> assign(@PathVariable UUID courseId, @PathVariable UUID studentId) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@GetMapping("/api/teacher/courses/{courseId}/students")
	ResponseEntity<Void> list(@PathVariable UUID courseId) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@DeleteMapping("/api/teacher/courses/{courseId}/students/{studentId}")
	ResponseEntity<Void> unassign(@PathVariable UUID courseId, @PathVariable UUID studentId) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}
}
