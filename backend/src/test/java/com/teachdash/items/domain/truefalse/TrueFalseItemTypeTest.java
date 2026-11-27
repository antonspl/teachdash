package com.teachdash.items.domain.truefalse;

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

/** item-true-false R1, student view. */
@JsonTest
class TrueFalseItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final TrueFalseItemType type = new TrueFalseItemType();

	private static TrueFalseStatement statement(String text, String answer) {
		return new TrueFalseStatement(text, answer);
	}

	/** A list that may hold a null element, which List.of does not allow. */
	private static List<TrueFalseStatement> listOf(TrueFalseStatement... statements) {
		return new ArrayList<>(Arrays.asList(statements));
	}

	private JsonNode view(TrueFalsePayload payload) {
		return mapper.valueToTree(type.studentView(payload));
	}

	private static List<String> propertyNamesOf(JsonNode node) {
		return new ArrayList<>(node.propertyNames());
	}

	@Test
	@DisplayName("[item-true-false/R1-S7] The student view keeps the texts in order and has no answer, allowNotStated is false when missing, and a bad answer does not matter")
	void theStudentViewKeepsTheTextsAndHasNoAnswer() {
		// given the true-false type and three payloads
		var first = new TrueFalsePayload(true, listOf(
				statement("Rome is in Italy.", "true"),
				statement("Rome has a river called Seine.", "false"),
				statement("The guide speaks Italian.", "not-stated")));
		var second = new TrueFalsePayload(null, listOf(statement("Rome is in Italy.", "false")));
		var third = new TrueFalsePayload(null, listOf(
				statement("Rome is in Italy.", null),
				statement("Rome is big.", "garbage")));
		var secondWithTrue = new TrueFalsePayload(null, listOf(statement("Rome is in Italy.", "true")));

		// when studentView is called with each payload
		JsonNode firstView = view(first);
		JsonNode secondView = view(second);
		JsonNode thirdView = view(third);
		JsonNode secondWithTrueView = view(secondWithTrue);

		// then the first result has exactly the properties allowNotStated (true) and statements
		assertThat(propertyNamesOf(firstView)).containsExactly("allowNotStated", "statements");
		assertThat(firstView.get("allowNotStated").isBoolean()).isTrue();
		assertThat(firstView.get("allowNotStated").asBoolean()).isTrue();
		// and three statements in the stored order, each with exactly the property text
		assertThat(textsOf(firstView)).containsExactly(
				"Rome is in Italy.",
				"Rome has a river called Seine.",
				"The guide speaks Italian.");
		firstView.get("statements").valueStream()
				.forEach(statement -> assertThat(propertyNamesOf(statement)).containsExactly("text"));
		// and the second has allowNotStated false (the property exists) and one statement with only text
		assertThat(propertyNamesOf(secondView)).containsExactly("allowNotStated", "statements");
		assertThat(secondView.get("allowNotStated").isBoolean()).isTrue();
		assertThat(secondView.get("allowNotStated").asBoolean()).isFalse();
		assertThat(secondView.get("statements")).hasSize(1);
		assertThat(propertyNamesOf(secondView.get("statements").get(0))).containsExactly("text");
		// and the third still returns a view with the two texts only
		assertThat(textsOf(thirdView)).containsExactly("Rome is in Italy.", "Rome is big.");
		thirdView.get("statements").valueStream()
				.forEach(statement -> assertThat(propertyNamesOf(statement)).containsExactly("text"));
		// and the second payload with the answer true and with the answer false serialise to equal JSON
		assertThat(mapper.writeValueAsString(secondWithTrueView)).isEqualTo(mapper.writeValueAsString(secondView));
	}

	@Test
	@DisplayName("[item-true-false/R1-S8] The serialised student view contains no answer, in English and Russian")
	void theSerialisedStudentViewContainsNoAnswer() {
		// given an English payload and a Russian payload whose texts contain none of the answer strings
		var english = new TrueFalsePayload(true, listOf(
				statement("Paris is in France.", "true"),
				statement("Paris is in Spain.", "false"),
				statement("The guide is tall.", "not-stated")));
		var russian = new TrueFalsePayload(null, listOf(
				statement("Москва стоит на Волге.", "false"),
				statement("Волга впадает в Каспийское море.", "true")));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the JSON of each view is an object with exactly the properties allowNotStated and statements
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode tree = mapper.readTree(json);
			assertThat(propertyNamesOf(tree)).containsExactlyInAnyOrder("allowNotStated", "statements");
			// and allowNotStated is a JSON boolean
			assertThat(tree.get("allowNotStated").isBoolean()).isTrue();
			// and every statement is an object with exactly the property text
			tree.get("statements").valueStream()
					.forEach(statement -> assertThat(propertyNamesOf(statement)).containsExactly("text"));
			// and no JSON string value equals "true", "false" or "not-stated"
			assertThat(stringValues(tree)).doesNotContain("true", "false", "not-stated");
			// and the JSON contains no property named answer
			assertThat(json).doesNotContain("\"answer\"");
		}
		// and every text appears exactly
		assertThat(textsOf(mapper.readTree(englishJson))).containsExactly(
				"Paris is in France.",
				"Paris is in Spain.",
				"The guide is tall.");
		assertThat(textsOf(mapper.readTree(russianJson))).containsExactly(
				"Москва стоит на Волге.",
				"Волга впадает в Каспийское море.");
	}

	private static List<String> textsOf(JsonNode view) {
		return view.get("statements").valueStream()
				.map(statement -> statement.get("text").asString())
				.toList();
	}

	private static List<String> stringValues(JsonNode node) {
		List<String> values = new ArrayList<>();
		if (node.isString()) {
			values.add(node.asString());
		}
		node.values().forEach(child -> values.addAll(stringValues(child)));
		return values;
	}

	@Test
	@DisplayName("[item-true-false/R1-S9] The student view of corrupt data fails without echoing stored values")
	void theStudentViewOfCorruptDataFails() {
		// given three payloads built in memory, not validated
		var nullStatements = new TrueFalsePayload(null, null);
		var nullElement = new TrueFalsePayload(null, listOf(statement("Berlin is in Germany.", "true"), null));
		var nullText = new TrueFalsePayload(null, listOf(
				statement("Berlin is in Germany.", "true"),
				statement(null, "true")));

		// when studentView is called with each payload
		// then each call throws IllegalStateException and no message contains the stored text
		for (TrueFalsePayload payload : List.of(nullStatements, nullElement, nullText)) {
			assertThatThrownBy(() -> type.studentView(payload))
					.isInstanceOf(IllegalStateException.class)
					.satisfies(thrown -> assertThat(String.valueOf(thrown.getMessage()))
							.doesNotContain("Berlin is in Germany."));
		}
	}
}
