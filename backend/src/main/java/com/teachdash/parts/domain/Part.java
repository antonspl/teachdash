package com.teachdash.parts.domain;

import java.time.Instant;
import java.util.UUID;

public record Part(
		UUID id,
		UUID teacherId,
		String title,
		int position,
		Instant createdAt,
		Instant updatedAt) {
}
