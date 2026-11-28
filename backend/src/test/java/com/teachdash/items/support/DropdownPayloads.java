package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of dropdown payloads for the tests. */
public final class DropdownPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private DropdownPayloads() {
	}

	public static Map<String, Object> text(String text) {
		Map<String, Object> segment = new LinkedHashMap<>();
		segment.put("type", "text");
		segment.put("text", text);
		return segment;
	}

	/** A gap with the given options and the index of the correct one. */
	public static Map<String, Object> gap(int correct, String... options) {
		Map<String, Object> segment = new LinkedHashMap<>();
		segment.put("type", "gap");
		segment.put("options", Arrays.asList(options));
		segment.put("correct", correct);
		return segment;
	}

	/** A payload with the segments in the given order; a segment may be null. */
	public static Map<String, Object> payload(Object... segments) {
		return payload(Arrays.asList(segments));
	}

	public static Map<String, Object> payload(List<Object> segments) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("segments", segments);
		return payload;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** {@code count} gaps, each {@code ["go", "went"] -> 0}. */
	public static List<Object> gaps(int count) {
		List<Object> segments = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			segments.add(gap(0, "go", "went"));
		}
		return segments;
	}

	/** Texts and gaps alternating; {@code total} segments, each text of {@code textLength} letters, each gap {@code ["go", "went"] -> 1}. */
	public static List<Object> alternating(int total, String letter, int textLength, boolean startWithText) {
		List<Object> segments = new ArrayList<>();
		for (int i = 0; i < total; i++) {
			boolean isText = (i % 2 == 0) == startWithText;
			segments.add(isText ? text(letter.repeat(textLength)) : gap(1, "go", "went"));
		}
		return segments;
	}

	/** The raw JSON of a payload with the given segment JSON texts. */
	public static String rawSegments(String... segments) {
		return "{\"segments\": [" + String.join(", ", segments) + "]}";
	}
}
