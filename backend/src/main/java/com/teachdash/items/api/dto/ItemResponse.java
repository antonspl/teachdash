package com.teachdash.items.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.teachdash.items.domain.Item;

import tools.jackson.databind.JsonNode;

public record ItemResponse(
		UUID id,
		String type,
		JsonNode payload,
		int position,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public static ItemResponse from(Item item) {
		return new ItemResponse(
				item.id(),
				item.type(),
				item.payload(),
				item.position(),
				item.version(),
				item.createdAt(),
				item.updatedAt());
	}
}
