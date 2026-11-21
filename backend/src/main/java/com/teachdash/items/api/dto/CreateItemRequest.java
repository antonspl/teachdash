package com.teachdash.items.api.dto;

import tools.jackson.databind.JsonNode;

public record CreateItemRequest(
		String type,
		JsonNode payload) {
}
