package com.teachdash.courses.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.courses.api.dto.CourseResponse;
import com.teachdash.courses.api.dto.CreateCourseRequest;
import com.teachdash.courses.api.dto.RenameCourseRequest;
import com.teachdash.courses.domain.CourseService;

import jakarta.validation.Valid;

@RestController
class CourseController {

	private final CourseService courses;

	CourseController(CourseService courses) {
		this.courses = courses;
	}

	@PostMapping("/api/teacher/courses")
	@ResponseStatus(HttpStatus.CREATED)
	CourseResponse create(@Valid @RequestBody CreateCourseRequest request, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return CourseResponse.from(courses.create(
				teacherId,
				request.title(),
				request.instructionLanguage(),
				request.targetLanguage()));
	}

	@GetMapping("/api/teacher/courses")
	List<CourseResponse> list(Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return courses.list(teacherId).stream().map(CourseResponse::from).toList();
	}

	@GetMapping("/api/teacher/courses/{id}")
	CourseResponse view(@PathVariable UUID id, Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return CourseResponse.from(courses.get(teacherId, id));
	}

	@PatchMapping("/api/teacher/courses/{id}")
	CourseResponse rename(
			@PathVariable UUID id,
			@Valid @RequestBody RenameCourseRequest request,
			Principal principal) {
		UUID teacherId = UUID.fromString(principal.getName());
		return CourseResponse.from(courses.rename(teacherId, id, request.title()));
	}
}
