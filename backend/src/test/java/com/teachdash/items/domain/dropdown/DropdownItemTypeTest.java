package com.teachdash.items.domain.dropdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	@DisplayName("[item-dropdown/R1-S8] The student view of a corrupt gap with a null option throws IllegalStateException")
	void theStudentViewOfAGapWithANullOptionThrowsIllegalState() {
		// given a stored payload whose gap has a null option
		var payload = new DropdownPayload(List.of(
				new DropdownSegment("gap", null, Arrays.asList("a", null), 0)));

		// when studentView is called with the payload, then it throws IllegalStateException, not NullPointerException
		assertThatThrownBy(() -> type.studentView(payload)).isInstanceOf(IllegalStateException.class);
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

	@Test
	@DisplayName("[item-dropdown/R1-S10] The student view throws IllegalStateException on corrupt stored data, with no stored value in the message")
	void theStudentViewThrowsIllegalStateExceptionOnCorruptStoredData() {
		// given the thirteen corrupt payloads (a) to (m) of the scenario, built with marker values
		Map<String, DropdownPayload> corrupt = new LinkedHashMap<>();
		corrupt.put("a segments null", new DropdownPayload(null));
		corrupt.put("b segments empty", new DropdownPayload(List.of()));
		corrupt.put("c null segment", new DropdownPayload(Arrays.asList((DropdownSegment) null)));
		corrupt.put("d image segment", payloadOf(new DropdownSegment("image", "SECRET-IMAGE", null, null)));
		corrupt.put("e null type", payloadOf(new DropdownSegment(null, "SECRET-TEXT", null, null)));
		corrupt.put("f text null", payloadOf(new DropdownSegment("text", null, null, null)));
		corrupt.put("g options null", payloadOf(new DropdownSegment("gap", null, null, 0)));
		corrupt.put("h options empty", payloadOf(new DropdownSegment("gap", null, List.of(), 0)));
		corrupt.put("i one option", payloadOf(new DropdownSegment("gap", null, List.of("SECRET-ONE"), 0)));
		corrupt.put("j null option", payloadOf(new DropdownSegment("gap", null, Arrays.asList("SECRET-A", null), 0)));
		corrupt.put("k correct null", payloadOf(new DropdownSegment("gap", null, List.of("SECRET-A", "SECRET-B"), null)));
		corrupt.put("l correct -1", payloadOf(new DropdownSegment("gap", null, List.of("SECRET-A", "SECRET-B"), -1)));
		corrupt.put("m correct out of range", payloadOf(new DropdownSegment("gap", null, List.of("SECRET-A", "SECRET-B"), 2)));

		// when studentView is called with each payload, then every call throws IllegalStateException
		// and the message contains no stored text, option or type value
		corrupt.forEach((name, payload) -> assertThatThrownBy(() -> type.studentView(payload))
				.as(name)
				.isExactlyInstanceOf(IllegalStateException.class)
				.message()
				.doesNotContain("SECRET")
				.doesNotContain("image"));
	}

	private static DropdownPayload payloadOf(DropdownSegment segment) {
		return new DropdownPayload(List.of(segment));
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
