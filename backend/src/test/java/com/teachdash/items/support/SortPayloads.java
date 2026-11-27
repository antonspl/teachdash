package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of sort payloads for the tests. */
public final class SortPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private SortPayloads() {
	}

	public static Map<String, Object> column(String title, String... words) {
		return column(title, Arrays.asList(words));
	}

	public static Map<String, Object> column(String title, List<String> words) {
		Map<String, Object> column = new LinkedHashMap<>();
		column.put("title", title);
		column.put("words", words);
		return column;
	}

	/** The columns in the given order; an element may be null. */
	public static List<Object> columns(Object... columns) {
		return Arrays.asList(columns);
	}

	public static Map<String, Object> payload(Object... columns) {
		return payload(columns(columns));
	}

	public static Map<String, Object> payload(List<Object> columns) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("columns", columns);
		return payload;
	}

	/** The words "w{from}" to "w{to}". */
	public static List<String> words(int from, int to) {
		List<String> words = new ArrayList<>();
		for (int i = from; i <= to; i++) {
			words.add("w" + i);
		}
		return words;
	}

	/** The columns "T1" to "T{count}" with {wordsEach} words each, numbered "w1", "w2" and so on across the columns. */
	public static List<Object> numberedColumns(int count, int wordsEach) {
		List<Object> columns = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			int first = (i - 1) * wordsEach + 1;
			columns.add(column("T" + i, words(first, first + wordsEach - 1)));
		}
		return columns;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** The raw JSON of a payload with the given column JSON texts. */
	public static String rawColumns(String... columns) {
		return "{\"columns\": [" + String.join(", ", columns) + "]}";
	}
}
