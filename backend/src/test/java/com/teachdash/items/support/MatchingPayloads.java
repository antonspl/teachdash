package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of matching payloads for the tests. */
public final class MatchingPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private MatchingPayloads() {
	}

	public static Map<String, Object> pair(String left, String right) {
		Map<String, Object> pair = new LinkedHashMap<>();
		pair.put("left", left);
		pair.put("right", right);
		return pair;
	}

	/** The pairs in the given order; an element may be null. */
	public static List<Object> pairs(Object... pairs) {
		return Arrays.asList(pairs);
	}

	public static Map<String, Object> payload(Object... pairs) {
		return payload(pairs(pairs));
	}

	public static Map<String, Object> payload(List<Object> pairs) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("pairs", pairs);
		return payload;
	}

	/** The pairs {"w1", "t1"} to {"w{count}", "t{count}"}. */
	public static List<Object> numberedPairs(int count) {
		List<Object> pairs = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			pairs.add(pair("w" + i, "t" + i));
		}
		return pairs;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** The raw JSON of a payload with the given pair JSON texts. */
	public static String rawPairs(String... pairs) {
		return "{\"pairs\": [" + String.join(", ", pairs) + "]}";
	}
}
