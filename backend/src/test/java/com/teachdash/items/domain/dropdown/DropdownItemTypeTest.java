package com.teachdash.items.domain.dropdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** item-dropdown R1, student view. */
@JsonTest
class DropdownItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final DropdownItemType type = new DropdownItemType();

	private static DropdownSegment text(String text) {
		return new DropdownSegment("text", text, null, null);
	}

	private static DropdownSegment gap(int correct, String... options) {
		return new DropdownSegment("gap", null, List.of(options), correct);
	}

	@Test
	@DisplayName("[item-dropdown/R1-S8] The student view keeps the text and shows each gap with its options and no correct mark")
	void theStudentViewKeepsTheTextAndShowsEachGapWithItsOptions() {
		// given the dropdown type and a payload with a text, a gap with three options, a text and a gap with two options
		var payload = new DropdownPayload(List.of(
				text("I "), gap(1, "go", "goes", "went"), text(" to school."), gap(0, "a", "b")));
		var otherCorrect = new DropdownPayload(List.of(
				text("I "), gap(2, "go", "goes", "went"), text(" to school."), gap(1, "a", "b")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));
		JsonNode otherView = mapper.valueToTree(type.studentView(otherCorrect));

		// then the result is {"segments": [...]} with the four segments in order and each gap has only type and options
		JsonNode expected = mapper.readTree("""
				{"segments": [
					{"type": "text", "text": "I "},
					{"type": "gap", "options": ["go", "goes", "went"]},
					{"type": "text", "text": " to school."},
					{"type": "gap", "options": ["a", "b"]}
				]}""");
		assertThat(view).isEqualTo(expected);
		// and two payloads that differ only in correct give identical views
		assertThat(otherView).isEqualTo(view);
	}

	@Test
	@DisplayName("[item-dropdown/R1-S9] The serialised student view contains no `correct` key, in English and Russian")
	void theSerialisedStudentViewContainsNoCorrectKey() {
		// given an English payload and a Russian payload with text segments that do not contain the word "correct"
		var english = new DropdownPayload(List.of(
				text("He "), gap(1, "goes", "walks"), text(" to school and "),
				gap(0, "brings", "bring", "bringing"), text(" a book.")));
		var russian = new DropdownPayload(List.of(
				text("Вот "), gap(2, "Ёжик", "ёжик", "ежик"), text(" бежит, а там "),
				gap(0, "мышь", "мыши"), text(" спит.")));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the JSON is an object with the property segments and it contains no correct property name
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode tree = mapper.readTree(json);
			assertThat(tree.propertyNames()).containsExactly("segments");
			assertThat(json).doesNotContain("correct");
		}
		// and each gap's options appear exactly and in stored order
		assertThat(optionsOf(englishJson)).containsExactly(
				List.of("goes", "walks"), List.of("brings", "bring", "bringing"));
		assertThat(optionsOf(russianJson)).containsExactly(
				List.of("Ёжик", "ёжик", "ежик"), List.of("мышь", "мыши"));
		// and every text segment appears exactly
		assertThat(textsOf(englishJson)).containsExactly("He ", " to school and ", " a book.");
		assertThat(textsOf(russianJson)).containsExactly("Вот ", " бежит, а там ", " спит.");
	}

	private List<String> textsOf(String json) {
		return mapper.readTree(json).get("segments").valueStream()
				.filter(segment -> segment.has("text"))
				.map(segment -> segment.get("text").asString())
				.toList();
	}

	private List<List<String>> optionsOf(String json) {
		return mapper.readTree(json).get("segments").valueStream()
				.filter(segment -> segment.has("options"))
				.map(segment -> segment.get("options").valueStream().map(JsonNode::asString).toList())
				.toList();
	}
}
