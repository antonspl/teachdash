package com.teachdash.enrolment.api.admin;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// STUB: replaced by the real implementation in pass 2
@RestController
class AdminEnrolmentController {

	// STUB
	@GetMapping("/api/admin/teachers/{id}/students")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void teacherStudents(@PathVariable UUID id) {
	}

	// STUB
	@GetMapping("/api/admin/students/{id}/teachers")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void studentTeachers(@PathVariable UUID id) {
	}
}
