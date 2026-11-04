package com.teachdash.enrolment.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.TeacherName;

@RestController
class StudentTeacherController {

	private final EnrolmentQueryService queries;

	StudentTeacherController(EnrolmentQueryService queries) {
		this.queries = queries;
	}

	@GetMapping("/api/student/teachers")
	List<TeacherName> listTeachers(Principal principal) {
		UUID studentId = UUID.fromString(principal.getName());
		return queries.teachersForStudent(studentId);
	}
}
