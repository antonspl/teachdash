package com.teachdash.courseassignment.domain;

import java.time.Instant;
import java.util.UUID;

public record CourseStudent(
		UUID id,
		String name,
		Instant assignedAt) {
}
