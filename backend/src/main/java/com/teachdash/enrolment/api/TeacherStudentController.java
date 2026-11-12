package com.teachdash.enrolment.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.accounts.api.dto.LinkResponse;
import com.teachdash.enrolment.api.dto.RegisterStudentRequest;
import com.teachdash.enrolment.api.dto.RegisteredStudent;
import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.UserSummary;
import com.teachdash.enrolment.domain.StudentRegistrationService;
import com.teachdash.enrolment.domain.StudentRemovalService;
import com.teachdash.enrolment.domain.StudentResetLinkService;

import jakarta.validation.Valid;

@RestController
class TeacherStudentController {

	private final StudentRegistrationService registration;
	private final EnrolmentQueryService queries;
	private final StudentRemovalService removal;
	private final StudentResetLinkService resetLinks;

	TeacherStudentController(
			StudentRegistrationService registration,
			EnrolmentQueryService queries,
			StudentRemovalService removal,
			StudentResetLinkService resetLinks) {
		this.registration = registration;
		this.queries = queries;
		this.removal = removal;
		this.resetLinks = resetLinks;
	}

	@PostMapping("/api/teacher/students")
	@ResponseStatus(HttpStatus.CREATED)
	RegisteredStudent registerStudent(
			@Valid @RequestBody RegisterStudentRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return RegisteredStudent.of(
				registration.register(teacherId, request.name(), request.email(), request.timeZone()));
	}

	@GetMapping("/api/teacher/students")
	List<UserSummary> listStudents(Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return queries.studentsOfTeacher(teacherId);
	}

	@GetMapping("/api/teacher/students/{id}")
	UserSummary studentDetails(@PathVariable UUID id, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return queries.studentOfTeacher(teacherId, id);
	}

	@DeleteMapping("/api/teacher/students/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeStudent(@PathVariable UUID id, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		removal.removeStudent(teacherId, id);
	}

	@PostMapping("/api/teacher/students/{id}/reset-link")
	@ResponseStatus(HttpStatus.CREATED)
	LinkResponse issueResetLink(@PathVariable UUID id, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return new LinkResponse(resetLinks.issueResetLink(teacherId, id));
	}
}
