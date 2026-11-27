package com.teachdash.items.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builders of free-text payloads for the tests. */
public final class FreeTextPayloads {

	public static final String EMOJI = "😀";

	private FreeTextPayloads() {
	}

	/** A payload with the prompt and the limits; a null limit is left out. */
	public static Map<String, Object> payload(String prompt, Integer minLength, Integer maxLength) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("prompt", prompt);
		if (minLength != null) {
			payload.put("minLength", minLength);
		}
		if (maxLength != null) {
			payload.put("maxLength", maxLength);
		}
		return payload;
	}

	public static Map<String, Object> payload(String prompt) {
		return payload(prompt, null, null);
	}

	/** A raw payload JSON with the prompt as a JSON string and the given extra members, such as {@code "minLength": 5}. */
	public static String raw(String prompt, String... members) {
		StringBuilder json = new StringBuilder("{\"prompt\": \"").append(prompt).append('"');
		for (String member : members) {
			json.append(", ").append(member);
		}
		return json.append('}').toString();
	}
}
