package com.teachdash.courses.domain;

import java.time.Instant;
import java.util.UUID;

public record Course(
		UUID id,
		UUID teacherId,
		String title,
		String instructionLanguage,
		String targetLanguage,
		Instant createdAt,
		Instant updatedAt) {
}
