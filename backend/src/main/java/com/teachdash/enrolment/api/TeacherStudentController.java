package com.teachdash.enrolment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// STUB: replaced by the real implementation in pass 2
@RestController
class TeacherStudentController {

	// STUB
	@PostMapping("/api/teacher/students")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void registerStudent() {
	}

	// STUB
	@GetMapping("/api/teacher/students")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void listStudents() {
	}

	// STUB
	@GetMapping("/api/teacher/students/{id}")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void studentDetails(@PathVariable UUID id) {
	}
}
