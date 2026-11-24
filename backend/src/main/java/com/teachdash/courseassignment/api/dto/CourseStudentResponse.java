package com.teachdash.courseassignment.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.teachdash.courseassignment.domain.CourseStudent;

public record CourseStudentResponse(
		UUID id,
		String name,
		Instant assignedAt) {

	public static CourseStudentResponse from(CourseStudent student) {
		return new CourseStudentResponse(student.id(), student.name(), student.assignedAt());
	}
}
