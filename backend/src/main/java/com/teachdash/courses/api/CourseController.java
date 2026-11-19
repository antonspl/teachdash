package com.teachdash.courses.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teachdash.courses.api.dto.CourseResponse;
import com.teachdash.courses.api.dto.CreateCourseRequest;
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
		return CourseResponse.from(courses.create(teacherId, request));
	}
}
