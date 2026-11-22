package com.teachdash.courseassignment.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.courseassignment.api.dto.CourseStudentResponse;
import com.teachdash.courseassignment.domain.CourseAssignmentService;

@RestController
class CourseAssignmentController {

	private final CourseAssignmentService assignments;

	CourseAssignmentController(CourseAssignmentService assignments) {
		this.assignments = assignments;
	}

	@PutMapping("/api/teacher/courses/{courseId}/students/{studentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void assign(@PathVariable UUID courseId, @PathVariable UUID studentId, Principal principal) {
		assignments.assign(UUID.fromString(principal.getName()), courseId, studentId);
	}

	@GetMapping("/api/teacher/courses/{courseId}/students")
	List<CourseStudentResponse> list(@PathVariable UUID courseId, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return assignments.list(teacherId, courseId).stream().map(CourseStudentResponse::from).toList();
	}

	@DeleteMapping("/api/teacher/courses/{courseId}/students/{studentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unassign(@PathVariable UUID courseId, @PathVariable UUID studentId, Principal principal) {
		assignments.unassign(UUID.fromString(principal.getName()), courseId, studentId);
	}
}
