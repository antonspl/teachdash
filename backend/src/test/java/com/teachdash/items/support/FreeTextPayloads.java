package com.teachdash.items.support;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of free-text payloads for the tests. */
public final class FreeTextPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private FreeTextPayloads() {
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
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
		StringBuilder json = new StringBuilder("{\"prompt\": \"").append(escape(prompt)).append('"');
		for (String member : members) {
			json.append(", ").append(member);
		}
		return json.append('}').toString();
	}

	private static String escape(String text) {
		StringBuilder escaped = new StringBuilder();
		for (char c : text.toCharArray()) {
			switch (c) {
				case '"' -> escaped.append("\\\"");
				case '\\' -> escaped.append("\\\\");
				case '\n' -> escaped.append("\\n");
				case '\r' -> escaped.append("\\r");
				case '\t' -> escaped.append("\\t");
				default -> {
					if (c < 0x20) {
						escaped.append(String.format("\\u%04x", (int) c));
					}
					else {
						escaped.append(c);
					}
				}
			}
		}
		return escaped.toString();
	}
}
