package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of true-false payloads for the tests. */
public final class TrueFalsePayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private TrueFalsePayloads() {
	}

	public static Map<String, Object> statement(String text, String answer) {
		Map<String, Object> statement = new LinkedHashMap<>();
		statement.put("text", text);
		statement.put("answer", answer);
		return statement;
	}

	/** A payload without allowNotStated and with the statements in the given order; a statement may be null. */
	public static Map<String, Object> payload(Object... statements) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("statements", Arrays.asList(statements));
		return payload;
	}

	/** A payload with allowNotStated, which may be null, written first. */
	public static Map<String, Object> payloadWith(Boolean allowNotStated, Object... statements) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("allowNotStated", allowNotStated);
		payload.put("statements", Arrays.asList(statements));
		return payload;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** The raw JSON of a payload with the given statement JSON texts. */
	public static String rawStatements(String... statements) {
		return "{\"statements\": [" + String.join(", ", statements) + "]}";
	}

	/** The raw JSON of a statement; the answer is written as given, so it may be null or any JSON. */
	public static String rawStatement(String text, String answerJson) {
		return "{\"text\": " + json(text) + ", \"answer\": " + answerJson + "}";
	}

	/** {@code count} statements "Statement 1", "Statement 2" and so on, each with the answer "true". */
	public static List<Object> numbered(int count) {
		List<Object> statements = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			statements.add(statement("Statement " + i, "true"));
		}
		return statements;
	}
}
