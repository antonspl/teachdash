package com.teachdash.items.domain.numeric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** item-numeric R1, student view. */
@JsonTest
class NumericItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final NumericItemType type = new NumericItemType();

	private static NumericQuestion question(String text, String answer, String tolerance, String unit) {
		return new NumericQuestion(text, answer, tolerance, unit);
	}

	private static List<String> propertyNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		node.propertyNames().forEach(names::add);
		return names;
	}

	private static List<String> textsOf(JsonNode view) {
		return view.get("questions").valueStream()
				.map(entry -> entry.get("text").asString())
				.toList();
	}

	@Test
	@DisplayName("[item-numeric/R1-S6] The student view has the text and the unit, never the number or the tolerance")
	void theStudentViewHasTheTextAndTheUnitOnly() {
		// given the numeric type and a payload with three questions, the third with no unit
		var payload = new NumericPayload(List.of(
				question("Speed?", "60", "0.5", "km/h"),
				question("Масса?", "75.5", "0", "кг"),
				question("Count the sides.", "4", "0", null)));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the result has exactly the property questions and three entries in the same order
		assertThat(propertyNames(view)).containsExactly("questions");
		JsonNode questions = view.get("questions");
		assertThat(questions.size()).isEqualTo(3);
		// and the first two entries have exactly the properties text and unit, in this order, with the stored values
		assertThat(propertyNames(questions.get(0))).containsExactly("text", "unit");
		assertThat(questions.get(0).get("text").asString()).isEqualTo("Speed?");
		assertThat(questions.get(0).get("unit").asString()).isEqualTo("km/h");
		assertThat(propertyNames(questions.get(1))).containsExactly("text", "unit");
		assertThat(questions.get(1).get("text").asString()).isEqualTo("Масса?");
		assertThat(questions.get(1).get("unit").asString()).isEqualTo("кг");
		// and the third has exactly the property text (no unit key, not null)
		assertThat(propertyNames(questions.get(2))).containsExactly("text");
		assertThat(questions.get(2).get("text").asString()).isEqualTo("Count the sides.");
		// and no entry has answer or tolerance
		for (JsonNode entry : questions) {
			assertThat(entry.has("answer")).isFalse();
			assertThat(entry.has("tolerance")).isFalse();
		}
	}

	@Test
	@DisplayName("[item-numeric/R1-S7] The questions keep their stored order")
	void theQuestionsKeepTheirStoredOrder() {
		// given the numeric type and a payload with the questions Q3, Q1, Q2
		var payload = new NumericPayload(List.of(
				question("Q3", "3", "0", null),
				question("Q1", "1", "0", null),
				question("Q2", "2", "0", "m")));

		// when studentView is called twice with the payload
		JsonNode first = mapper.valueToTree(type.studentView(payload));
		JsonNode second = mapper.valueToTree(type.studentView(payload));

		// then both views are equal and their question texts are exactly Q3, Q1, Q2
		assertThat(second).isEqualTo(first);
		assertThat(textsOf(first)).containsExactly("Q3", "Q1", "Q2");
	}

	@Test
	@DisplayName("[item-numeric/R1-S8] The serialised student view contains no number or tolerance, in English and Russian")
	void theSerialisedStudentViewContainsNoNumberOrTolerance() {
		// given an English payload and a Russian payload whose texts and units contain no digit of a number or a tolerance
		var english = new NumericPayload(List.of(
				question("Boiling point of water?", "100", "0.125", "°C"),
				question("Speed of sound?", "343.5", "1.25", null)));
		var russian = new NumericPayload(List.of(
				question("Температура кипения воды?", "100", "0.125", "°C"),
				question("Сколько ног у паука?", "8", "0", null)));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the JSON is an object with the property questions and each question has text, and unit only when it had one
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode tree = mapper.readTree(json);
			assertThat(propertyNames(tree)).containsExactly("questions");
			JsonNode questions = tree.get("questions");
			assertThat(questions.size()).isEqualTo(2);
			assertThat(propertyNames(questions.get(0))).containsExactly("text", "unit");
			assertThat(propertyNames(questions.get(1))).containsExactly("text");
			// and the JSON has no answer or tolerance property name and none of the number strings as a value
			assertThat(json).doesNotContain("answer", "tolerance");
			for (JsonNode entry : questions) {
				entry.propertyNames().forEach(name ->
						assertThat(entry.get(name).asString()).isNotIn("100", "0.125", "343.5", "1.25", "8"));
			}
		}
		// and every text and unit appears exactly
		assertThat(textsOf(mapper.readTree(englishJson)))
				.containsExactly("Boiling point of water?", "Speed of sound?");
		assertThat(textsOf(mapper.readTree(russianJson)))
				.containsExactly("Температура кипения воды?", "Сколько ног у паука?");
		assertThat(mapper.readTree(englishJson).get("questions").get(0).get("unit").asString()).isEqualTo("°C");
		assertThat(mapper.readTree(russianJson).get("questions").get(0).get("unit").asString()).isEqualTo("°C");
	}

	@Test
	@DisplayName("[item-numeric/R1-S9] The student view of corrupt data throws")
	void theStudentViewOfCorruptDataThrows() {
		// given payload records built directly: a null questions list, a null question and a question with a null text
		var nullList = new NumericPayload(null);
		var nullQuestion = new NumericPayload(Arrays.asList((NumericQuestion) null));
		var nullText = new NumericPayload(List.of(question(null, "123.456", "9.87", "секрет-unit")));

		// when studentView is called with each payload
		// then each call throws IllegalStateException and no message contains a stored value
		assertThatThrownBy(() -> type.studentView(nullList)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> type.studentView(nullQuestion)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> type.studentView(nullText))
				.isInstanceOf(IllegalStateException.class)
				.message()
				.doesNotContain("123.456", "9.87", "секрет-unit");
	}
}
