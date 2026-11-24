package com.teachdash.items.domain;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record Item(
		UUID id,
		String type,
		String title,
		JsonNode payload,
		int position,
		long version,
		Instant createdAt,
		Instant updatedAt) {
}
