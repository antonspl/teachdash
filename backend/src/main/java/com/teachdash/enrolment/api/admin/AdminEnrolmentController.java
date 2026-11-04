package com.teachdash.enrolment.api.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.api.dto.StudentLookupRequest;
import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.UserSummary;

import jakarta.validation.Valid;

@RestController
class AdminEnrolmentController {

	private final EnrolmentQueryService queries;

	AdminEnrolmentController(EnrolmentQueryService queries) {
		this.queries = queries;
	}

	@GetMapping("/api/admin/teachers/{id}/students")
	List<UserSummary> teacherStudents(@PathVariable UUID id) {
		return queries.studentsOfTeacherForAdmin(id);
	}

	@GetMapping("/api/admin/students/{id}/teachers")
	List<UserSummary> studentTeachers(@PathVariable UUID id) {
		return queries.teachersOfStudent(id);
	}

	@PostMapping("/api/admin/students/lookup")
	UserSummary lookupStudent(@Valid @RequestBody StudentLookupRequest request) {
		return queries.studentByEmail(request.email());
	}
}
