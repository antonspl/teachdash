package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of reorder payloads for the tests. */
public final class ReorderPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private ReorderPayloads() {
	}

	/** An entry with the units in the given order; a unit may be null. */
	public static Map<String, Object> entry(String... units) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("units", Arrays.asList(units));
		return entry;
	}

	public static Map<String, Object> entryWithHint(String hint, String... units) {
		Map<String, Object> entry = entry(units);
		entry.put("hint", hint);
		return entry;
	}

	/** The entries in the given order; an element may be null. */
	public static List<Object> entries(Object... entries) {
		return Arrays.asList(entries);
	}

	public static Map<String, Object> payload(String mode, Object... entries) {
		return payload(mode, entries(entries));
	}

	public static Map<String, Object> payload(String mode, List<Object> entries) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("mode", mode);
		payload.put("entries", entries);
		return payload;
	}

	/** The payload without a mode property. */
	public static Map<String, Object> payloadWithoutMode(Object... entries) {
		Map<String, Object> payload = payload("words", entries);
		payload.remove("mode");
		return payload;
	}

	/** The one-letter units "a", "b", ... (count at most 26). */
	public static String[] letters(int count) {
		String[] units = new String[count];
		for (int i = 0; i < count; i++) {
			units[i] = String.valueOf((char) ('a' + i));
		}
		return units;
	}

	/** An entry of the units "a", "b", ... (count at most 26). */
	public static Map<String, Object> lettersEntry(int count) {
		return entry(letters(count));
	}

	/** The given number of entries {"units": ["a", "b"]}. */
	public static List<Object> pairEntries(int count) {
		List<Object> entries = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			entries.add(entry("a", "b"));
		}
		return entries;
	}

	/** The given number of entries of 25 units "a" to "y" (the 25-unit entry). */
	public static List<Object> fullEntries(int count) {
		List<Object> entries = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			entries.add(lettersEntry(25));
		}
		return entries;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** The raw JSON of a payload with the given mode JSON and entry JSON texts. */
	public static String raw(String modeJson, String... entries) {
		return "{\"mode\": " + modeJson + ", \"entries\": [" + String.join(", ", entries) + "]}";
	}

	/** The raw JSON of a payload with the mode "words". */
	public static String rawWords(String... entries) {
		return raw("\"words\"", entries);
	}
}
