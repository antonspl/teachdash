package com.teachdash.items.api.dto;

import java.math.BigInteger;
import java.util.Map;

public record EditItemRequest(
		BigInteger version,
		String title,
		Map<String, Object> payload) {
}
