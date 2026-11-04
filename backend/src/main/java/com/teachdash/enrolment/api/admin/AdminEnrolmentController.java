package com.teachdash.enrolment.api.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.UserSummary;

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

	// STUB: pass 2 reads {email}, validates it and returns the student
	@PostMapping("/api/admin/students/lookup")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	void lookupStudent() {
	}
}
