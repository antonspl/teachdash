package com.teachdash.enrolment.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.api.dto.RegisterStudentRequest;
import com.teachdash.enrolment.api.dto.RegisterStudentResponse;
import com.teachdash.enrolment.domain.StudentRegistrationService;

import jakarta.validation.Valid;

@RestController
class TeacherStudentController {

	private final StudentRegistrationService registration;

	TeacherStudentController(StudentRegistrationService registration) {
		this.registration = registration;
	}

	@PostMapping("/api/teacher/students")
	@ResponseStatus(HttpStatus.CREATED)
	RegisterStudentResponse registerStudent(
			@Valid @RequestBody RegisterStudentRequest request,
			Principal principal) {
		var registered = registration.register(
				UUID.fromString(principal.getName()),
				request.name(),
				request.email(),
				request.timeZone());
		return new RegisterStudentResponse(registered.id(), registered.setPasswordLink());
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
