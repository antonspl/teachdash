package com.teachdash.enrolment.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StudentTeacherController {

	// STUB: pass 2 replaces this with the student's list of active teachers
	@GetMapping("/api/student/teachers")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	List<Object> listTeachers() {
		return List.of();
	}
}
