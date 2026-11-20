package com.teachdash.lessons.domain;

import java.time.Instant;
import java.util.UUID;

public record Lesson(
		UUID id,
		UUID teacherId,
		String title,
		int position,
		Instant createdAt,
		Instant updatedAt) {
}
