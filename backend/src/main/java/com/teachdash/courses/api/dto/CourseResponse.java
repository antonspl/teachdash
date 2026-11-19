package com.teachdash.courses.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.teachdash.courses.domain.Course;

public record CourseResponse(
		UUID id,
		String title,
		String instructionLanguage,
		String targetLanguage,
		Instant createdAt,
		Instant updatedAt) {

	public static CourseResponse from(Course course) {
		return new CourseResponse(
				course.id(),
				course.title(),
				course.instructionLanguage(),
				course.targetLanguage(),
				course.createdAt(),
				course.updatedAt());
	}
}
