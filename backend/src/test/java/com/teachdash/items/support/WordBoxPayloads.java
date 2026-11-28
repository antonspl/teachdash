package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of gap-fill-word-box payloads for the tests. */
public final class WordBoxPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private WordBoxPayloads() {
	}

	public static Map<String, Object> text(String text) {
		Map<String, Object> segment = new LinkedHashMap<>();
		segment.put("type", "text");
		segment.put("text", text);
		return segment;
	}

	public static Map<String, Object> gap(String answer) {
		Map<String, Object> segment = new LinkedHashMap<>();
		segment.put("type", "gap");
		segment.put("answer", answer);
		return segment;
	}

	/** A payload with the segments in the given order and no extra words; a segment may be null. */
	public static Map<String, Object> payload(Object... segments) {
		return payload(Arrays.asList(segments));
	}

	public static Map<String, Object> payload(List<Object> segments) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("segments", segments);
		return payload;
	}

	/** A payload with the segments and the extra words ({@code extraWords} may contain null). */
	public static Map<String, Object> payloadWith(List<Object> segments, String... extraWords) {
		Map<String, Object> payload = payload(segments);
		payload.put("extraWords", Arrays.asList(extraWords));
		return payload;
	}

	public static List<Object> segments(Object... segments) {
		return Arrays.asList(segments);
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** {@code count} gaps, each with a different answer "w1", "w2" and so on. */
	public static List<Object> gaps(int count) {
		List<Object> segments = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			segments.add(gap("w" + i));
		}
		return segments;
	}

	/** Texts and gaps alternating, {@code total} segments; each text has {@code textLength} letters, the gaps are "w1", "w2" and so on. */
	public static List<Object> alternating(int total, String letter, int textLength, boolean startWithText) {
		List<Object> segments = new ArrayList<>();
		int gapNumber = 0;
		for (int i = 0; i < total; i++) {
			boolean isText = (i % 2 == 0) == startWithText;
			segments.add(isText ? text(letter.repeat(textLength)) : gap("w" + (++gapNumber)));
		}
		return segments;
	}

	/** The extra words "x1" to "x{count}". */
	public static String[] extras(int count) {
		String[] words = new String[count];
		for (int i = 0; i < count; i++) {
			words[i] = "x" + (i + 1);
		}
		return words;
	}

	/** The raw JSON of a payload with the given segment JSON texts and no extra words. */
	public static String rawSegments(String... segments) {
		return "{\"segments\": [" + String.join(", ", segments) + "]}";
	}

	/** The raw JSON of a payload with the given segment JSON texts and the given extra-words JSON. */
	public static String rawSegmentsWith(String extraWordsJson, String... segments) {
		return "{\"segments\": [" + String.join(", ", segments) + "], \"extraWords\": " + extraWordsJson + "}";
	}
}
