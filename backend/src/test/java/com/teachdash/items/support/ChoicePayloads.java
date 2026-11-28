package com.teachdash.items.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** Builders of test-item payloads (questions with options) for the tests. */
public final class ChoicePayloads {

	public static final String EMOJI = "😀";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private ChoicePayloads() {
	}

	public static Map<String, Object> option(String text, boolean correct) {
		Map<String, Object> option = new LinkedHashMap<>();
		option.put("text", text);
		option.put("correct", correct);
		return option;
	}

	/** A question with the given options; an option may be null. */
	public static Map<String, Object> question(String text, Object... options) {
		return question(text, Arrays.asList(options));
	}

	public static Map<String, Object> question(String text, List<Object> options) {
		Map<String, Object> question = new LinkedHashMap<>();
		question.put("text", text);
		question.put("options", options);
		return question;
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

	/** A question with the given {@code multiple} (null is sent as a JSON null) and options. */
	public static Map<String, Object> questionWith(String text, Boolean multiple, Object... options) {
		Map<String, Object> question = new LinkedHashMap<>();
		question.put("text", text);
		question.put("multiple", multiple);
		question.put("options", Arrays.asList(options));
		return question;
	}

	/** Options with the given texts; only the first one is correct. */
	public static List<Object> optionsOf(String... texts) {
		List<Object> options = new ArrayList<>();
		for (int i = 0; i < texts.length; i++) {
			options.add(option(texts[i], i == 0));
		}
		return options;
	}

	/** {@code count} questions "Q", each with the options "A" (correct) and "B". */
	public static List<Object> questions(int count) {
		List<Object> questions = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			questions.add(question("Q", option("A", true), option("B", false)));
		}
		return questions;
	}

	/** The options "o1" to "o{count}", the last one correct. */
	public static List<Object> numberedOptions(int count) {
		List<Object> options = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			options.add(option("o" + i, i == count));
		}
		return options;
	}

	public static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** The raw JSON of a payload with the given question JSON texts. */
	public static String rawQuestions(String... questions) {
		return "{\"questions\": [" + String.join(", ", questions) + "]}";
	}

	/** The raw JSON of a question with the given text JSON and options JSON (both written as is). */
	public static String rawQuestion(String textJson, String optionsJson) {
		return "{\"text\": " + textJson + ", \"options\": " + optionsJson + "}";
	}
}
