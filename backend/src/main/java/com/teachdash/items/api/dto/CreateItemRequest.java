package com.teachdash.items.api.dto;

import java.util.Map;

public record CreateItemRequest(
		String type,
		String title,
		Map<String, Object> payload) {
}
