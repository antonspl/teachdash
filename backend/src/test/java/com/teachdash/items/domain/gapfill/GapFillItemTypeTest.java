package com.teachdash.items.domain.gapfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** item-gap-fill R1, student view. */
@JsonTest
class GapFillItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final GapFillItemType type = new GapFillItemType();

	private static GapFillSegment text(String text) {
		return new GapFillSegment("text", text, null);
	}

	private static GapFillSegment gap(String... answers) {
		return new GapFillSegment("gap", null, List.of(answers));
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S8] The student view keeps the text and shows each gap as a bare gap")
	void theStudentViewKeepsTheTextAndShowsEachGapAsABareGap() {
		// given the gap-fill type and a payload with a text, a gap with two answers, a text and a gap with one answer
		var payload = new GapFillPayload(List.of(text("I "), gap("go", "walk"), text(" to school."), gap("x")));
		var sameShape = new GapFillPayload(List.of(text("I "), gap("run"), text(" to school."), gap("y", "z")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));
		JsonNode sameShapeView = mapper.valueToTree(type.studentView(sameShape));

		// then the result is {"segments": [...]} with the four segments in order and each gap has no property but its type
		JsonNode expected = mapper.readTree("""
				{"segments": [
					{"type": "text", "text": "I "},
					{"type": "gap"},
					{"type": "text", "text": " to school."},
					{"type": "gap"}
				]}""");
		assertThat(view).isEqualTo(expected);
		// and the two gaps with different answer lists give identical views
		assertThat(sameShapeView).isEqualTo(view);
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S9] The serialised student view contains no accepted answer, in English and Russian")
	void theSerialisedStudentViewContainsNoAcceptedAnswer() {
		// given an English payload and a Russian payload with text segments that contain no answer
		var english = new GapFillPayload(List.of(
				text("He "), gap("goes", "walks"), text(" to school and "), gap("brings"), text(" a book.")));
		var russian = new GapFillPayload(List.of(
				text("Вот "), gap("Ёжик", "ёжик", "ежик"), text(" бежит, а там "), gap("мышь"), text(" спит.")));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the JSON is an object with the property segments, and it contains no accepted answer and no property named answers
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode tree = mapper.readTree(json);
			assertThat(tree.propertyNames()).containsExactly("segments");
			assertThat(json).doesNotContain("answers");
		}
		assertThat(englishJson).doesNotContain("goes", "walks", "brings");
		assertThat(russianJson).doesNotContain("Ёжик", "ёжик", "ежик", "мышь");
		// and every text segment appears exactly
		List<String> englishTexts = List.of("He ", " to school and ", " a book.");
		List<String> russianTexts = List.of("Вот ", " бежит, а там ", " спит.");
		assertThat(textsOf(englishJson)).isEqualTo(englishTexts);
		assertThat(textsOf(russianJson)).isEqualTo(russianTexts);
	}

	private List<String> textsOf(String json) {
		return mapper.readTree(json).get("segments").valueStream()
				.filter(segment -> segment.has("text"))
				.map(segment -> segment.get("text").asString())
				.toList();
	}

	@Test
	@DisplayName("The student view of a segment that is neither text nor gap is rejected")
	void anUnknownSegmentTypeHasNoStudentView() {
		var payload = new GapFillPayload(List.of(new GapFillSegment("image", null, null)));

		assertThatThrownBy(() -> type.studentView(payload)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("The student view of a text segment without text is rejected")
	void aTextSegmentWithoutTextHasNoStudentView() {
		var payload = new GapFillPayload(List.of(new GapFillSegment("text", null, null)));

		assertThatThrownBy(() -> type.studentView(payload)).isInstanceOf(IllegalStateException.class);
	}
}
