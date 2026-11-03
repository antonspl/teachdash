package com.teachdash.enrolment.api.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.enrolment.domain.EnrolmentQueryService;
import com.teachdash.enrolment.domain.Person;

@RestController
class AdminEnrolmentController {

	private final EnrolmentQueryService queries;

	AdminEnrolmentController(EnrolmentQueryService queries) {
		this.queries = queries;
	}

	@GetMapping("/api/admin/teachers/{id}/students")
	List<Person> teacherStudents(@PathVariable UUID id) {
		return queries.studentsOfTeacherForAdmin(id);
	}

	@GetMapping("/api/admin/students/{id}/teachers")
	List<Person> studentTeachers(@PathVariable UUID id) {
		return queries.teachersOfStudent(id);
	}
}
