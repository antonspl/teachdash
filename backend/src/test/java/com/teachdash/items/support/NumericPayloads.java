package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of numeric payloads for the tests. */
public final class NumericPayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private NumericPayloads() {
	}

	/** A question without a unit. */
	public static Map<String, Object> question(String text, String answer, String tolerance) {
		Map<String, Object> question = new LinkedHashMap<>();
		question.put("text", text);
		question.put("answer", answer);
		question.put("tolerance", tolerance);
		return question;
	}

	public static Map<String, Object> question(String text, String answer, String tolerance, String unit) {
		Map<String, Object> question = question(text, answer, tolerance);
		question.put("unit", unit);
		return question;
	}

	/** The valid question {"text": "Q", "answer": "1", "tolerance": "0"}. */
	public static Map<String, Object> validQuestion() {
		return question("Q", "1", "0");
	}

	/** A question with the answer replaced (a null value is sent as JSON null). */
	public static Map<String, Object> withAnswer(String answer) {
		return question("Q", answer, "0");
	}

	/** A question with the tolerance replaced (a null value is sent as JSON null). */
	public static Map<String, Object> withTolerance(String tolerance) {
		return question("Q", "1", tolerance);
	}

	/** A payload with the questions in the given order; a question may be null. */
	public static Map<String, Object> payload(Object... questions) {
		return payload(Arrays.asList(questions));
	}

	public static Map<String, Object> payload(List<Object> questions) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("questions", questions);
		return payload;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** {@code count} valid questions. */
	public static List<Object> validQuestions(int count) {
		List<Object> questions = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			questions.add(validQuestion());
		}
		return questions;
	}

	/** The raw JSON of a payload with the given question JSON texts. */
	public static String rawQuestions(String... questions) {
		return "{\"questions\": [" + String.join(", ", questions) + "]}";
	}

	/** The raw JSON of one question with the given field JSON fragments, such as {@code "\"text\": 5"}. */
	public static String rawQuestion(String... fields) {
		return "{" + String.join(", ", fields) + "}";
	}
}
