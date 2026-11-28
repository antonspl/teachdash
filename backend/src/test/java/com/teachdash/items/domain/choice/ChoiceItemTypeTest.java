package com.teachdash.items.domain.choice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import com.teachdash.common.StrictJsonTestConfig;
import com.teachdash.items.support.ChoicePayloads;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** item-test R3, student view. */
@JsonTest
@Import(StrictJsonTestConfig.class)
class ChoiceItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final TestItemType type = new TestItemType();

	private static TestOption option(String text, boolean correct) {
		return new TestOption(text, correct);
	}

	private static TestQuestion question(String text, TestOption... options) {
		return new TestQuestion(text, null, Arrays.asList(options));
	}

	/** A question with a stored multiple, read from JSON like the application does (the record has no such field yet). */
	private TestQuestion questionWith(String text, Boolean multiple, TestOption... options) {
		return mapper.convertValue(
				ChoicePayloads.questionWith(
						text,
						multiple,
						Arrays.stream(options).map(o -> ChoicePayloads.option(o.text(), o.correct())).toArray()),
				TestQuestion.class);
	}

	private static TestPayload payload(TestQuestion... questions) {
		return new TestPayload(Arrays.asList(questions));
	}

	@Test
	@DisplayName("[item-test/R3-S2] The student view shows `multiple` from the stored setting, not from the correct marks")
	void theStudentViewShowsTheStoredMultipleSetting() {
		// given the test type and a payload with five questions: no multiple, true with two correct, true with none correct, false with one correct, null with one correct
		var payload = payload(
				question("Q1", option("A", true), option("B", false), option("C", false)),
				questionWith("Q2", true, option("A", true), option("B", true), option("C", false)),
				questionWith("Q3", true, option("A", false), option("B", false)),
				questionWith("Q4", false, option("A", true), option("B", false)),
				questionWith("Q5", null, option("A", true), option("B", false)));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the result has exactly the property questions with five entries in the same order
		assertThat(view.propertyNames()).containsExactly("questions");
		JsonNode questions = view.get("questions");
		assertThat(questions.size()).isEqualTo(5);
		// and each has exactly the properties text, multiple and options, with multiple false, true, true, false, false
		List<Boolean> multiple = List.of(false, true, true, false, false);
		for (int i = 0; i < 5; i++) {
			JsonNode question = questions.get(i);
			assertThat(question.propertyNames()).containsExactly("text", "multiple", "options");
			assertThat(question.get("text").asString()).isEqualTo("Q" + (i + 1));
			assertThat(question.get("multiple").asBoolean()).isEqualTo(multiple.get(i));
		}
		// and each option has exactly the property text, in the stored order
		for (JsonNode question : questions) {
			for (JsonNode option : question.get("options")) {
				assertThat(option.propertyNames()).containsExactly("text");
			}
		}
		assertThat(texts(questions.get(0))).containsExactly("A", "B", "C");
		assertThat(texts(questions.get(1))).containsExactly("A", "B", "C");
		assertThat(texts(questions.get(2))).containsExactly("A", "B");
		assertThat(texts(questions.get(3))).containsExactly("A", "B");
		assertThat(texts(questions.get(4))).containsExactly("A", "B");
	}

	private static List<String> texts(JsonNode question) {
		return question.get("options").valueStream()
				.map(option -> option.get("text").asString())
				.toList();
	}

	@Test
	@DisplayName("[item-test/R3-S3] The options keep their stored order, \"all of the above\" stays last")
	void theOptionsKeepTheirStoredOrder() {
		// given the test type and a payload with the options apple, pear, bread, "all of the above" (multiple true) and c, a, b
		var payload = payload(
				questionWith(
						"Which are fruits?",
						true,
						option("apple", true),
						option("pear", true),
						option("bread", false),
						option("all of the above", false)),
				question("Second", option("c", false), option("a", true), option("b", false)));

		// when studentView is called twice with the payload
		JsonNode first = mapper.valueToTree(type.studentView(payload));
		JsonNode second = mapper.valueToTree(type.studentView(payload));

		// then both views are equal
		assertThat(second).isEqualTo(first);
		// and the option texts are in the stored order
		assertThat(texts(first.get("questions").get(0)))
				.containsExactly("apple", "pear", "bread", "all of the above");
		assertThat(texts(first.get("questions").get(1))).containsExactly("c", "a", "b");
	}

	@Test
	@DisplayName("[item-test/R3-S4] The serialised student view contains no correct mark, in English and Russian")
	void theSerialisedStudentViewContainsNoCorrectMark() {
		// given an English payload and a Russian payload with no text containing "correct" or "multiple"
		var english = payload(
				question("Past of go?", option("went", true), option("goed", false)),
				questionWith("Pick verbs.", true, option("run", true), option("walk", true), option("table", false)));
		var russian = payload(
				question("Столица России?", option("Москва", true), option("Ёж", false)),
				questionWith("Выберите глаголы.", true, option("бежать", true), option("идти", true), option("стол", false)));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then each JSON is an object with the property questions
		for (String json : List.of(englishJson, russianJson)) {
			assertThat(mapper.readTree(json).propertyNames()).containsExactly("questions");
			// and no correct property name, and no true or false except the values of multiple
			assertThat(json).doesNotContain("correct");
			assertThat(json.replace("\"multiple\":true", "").replace("\"multiple\":false", ""))
					.doesNotContain("true", "false");
			// and each question has the keys text, multiple, options in this order, and each option is exactly {"text": ...}
			for (JsonNode question : mapper.readTree(json).get("questions")) {
				assertThat(question.propertyNames()).containsExactly("text", "multiple", "options");
				for (JsonNode option : question.get("options")) {
					assertThat(option.propertyNames()).containsExactly("text");
				}
			}
		}
		// and every text appears exactly
		assertThat(allTexts(englishJson))
				.containsExactly("Past of go?", "went", "goed", "Pick verbs.", "run", "walk", "table");
		assertThat(allTexts(russianJson))
				.containsExactly("Столица России?", "Москва", "Ёж", "Выберите глаголы.", "бежать", "идти", "стол");
	}

	private List<String> allTexts(String json) {
		return mapper.readTree(json).get("questions").valueStream()
				.flatMap(question -> java.util.stream.Stream.concat(
						java.util.stream.Stream.of(question.get("text").asString()),
						question.get("options").valueStream().map(option -> option.get("text").asString())))
				.toList();
	}

	@Test
	@DisplayName("[item-test/R3-S6] The student view is the same whatever the answer key")
	void theStudentViewIsTheSameWhateverTheAnswerKey() {
		// given three payloads with the same texts, order and multiple, and three different answer keys
		var first = payload(
				questionWith("Which are right?", true, option("A", true), option("B", true)),
				questionWith("Pick one.", false, option("A", true), option("B", false), option("C", false)));
		var second = payload(
				questionWith("Which are right?", true, option("A", true), option("B", false)),
				questionWith("Pick one.", false, option("A", false), option("B", true), option("C", false)));
		var third = payload(
				questionWith("Which are right?", true, option("A", false), option("B", false)),
				questionWith("Pick one.", false, option("A", false), option("B", false), option("C", true)));

		// when studentView is called with each payload and each view is serialised to JSON with the application's JSON mapper
		List<String> jsons = List.of(first, second, third).stream()
				.map(payload -> mapper.writeValueAsString(type.studentView(payload)))
				.toList();
		List<JsonNode> views = jsons.stream().map(mapper::readTree).toList();

		// then the three views are equal and the three JSON strings are identical
		assertThat(views.get(1)).isEqualTo(views.get(0));
		assertThat(views.get(2)).isEqualTo(views.get(0));
		assertThat(jsons.get(1)).isEqualTo(jsons.get(0));
		assertThat(jsons.get(2)).isEqualTo(jsons.get(0));
		// and the first question has multiple true and the second false in all of them
		for (JsonNode view : views) {
			assertThat(view.get("questions").get(0).get("multiple").asBoolean()).isTrue();
			assertThat(view.get("questions").get(1).get("multiple").asBoolean()).isFalse();
		}
		// and nothing in a view tells which option is correct, or how many
		assertThat(jsons.get(0)).doesNotContain("correct");
	}

	@Test
	@DisplayName("[item-test/R3-S5] The student view of corrupt data throws")
	void theStudentViewOfCorruptDataThrows() {
		// given payload records built directly with a null question, question text, options, option, option text and correct, a null questions list and a null payload
		var good = option("A", true);
		List<TestPayload> corrupt = Arrays.asList(
				new TestPayload(Arrays.asList((TestQuestion) null)),
				payload(new TestQuestion(null, null, List.of(good, option("B", false)))),
				payload(new TestQuestion("Secret question", null, null)),
				payload(new TestQuestion("Secret question", null, Arrays.asList(good, null))),
				payload(question("Secret question", good, new TestOption(null, false))),
				payload(question("Secret question", good, new TestOption("Secret option", null))),
				new TestPayload(null),
				null);

		// when studentView is called with each payload
		// then each call throws IllegalStateException with no stored text in the message
		for (TestPayload payload : corrupt) {
			assertThatThrownBy(() -> type.studentView(payload))
					.isInstanceOf(IllegalStateException.class)
					.message().satisfiesAnyOf(
							message -> assertThat(message).isNull(),
							message -> assertThat(message).doesNotContain("Secret"));
		}
	}
}
