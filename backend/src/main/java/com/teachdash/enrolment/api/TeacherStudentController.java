package com.teachdash.enrolment.api;

import java.security.Principal;
import java.util.List;
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
import com.teachdash.enrolment.api.dto.PersonResponse;
import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.StudentRegistrationService;

import jakarta.validation.Valid;

@RestController
class TeacherStudentController {

	private final StudentRegistrationService registration;
	private final EnrolmentQueryService queries;

	TeacherStudentController(StudentRegistrationService registration, EnrolmentQueryService queries) {
		this.registration = registration;
		this.queries = queries;
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

	@GetMapping("/api/teacher/students")
	List<PersonResponse> listStudents(Principal principal) {
		return queries.studentsOfTeacher(UUID.fromString(principal.getName())).stream()
				.map(PersonResponse::from)
				.toList();
	}

	@GetMapping("/api/teacher/students/{id}")
	PersonResponse studentDetails(@PathVariable UUID id, Principal principal) {
		return PersonResponse.from(queries.studentOfTeacher(UUID.fromString(principal.getName()), id));
	}
}
