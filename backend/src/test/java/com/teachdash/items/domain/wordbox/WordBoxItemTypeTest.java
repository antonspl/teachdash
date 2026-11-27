package com.teachdash.items.domain.wordbox;

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

/** item-gap-fill-word-box R1, student view. */
@JsonTest
class WordBoxItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final WordBoxItemType type = new WordBoxItemType();

	private static WordBoxSegment text(String text) {
		return new WordBoxSegment("text", text, null);
	}

	private static WordBoxSegment gap(String answer) {
		return new WordBoxSegment("gap", null, answer);
	}

	private List<String> wordBox(JsonNode view) {
		assertThat(view.has("wordBox")).as("the view has a wordBox property").isTrue();
		List<String> words = new ArrayList<>();
		view.get("wordBox").forEach(word -> words.add(word.asString()));
		return words;
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S8] The student view keeps the text, shows gaps without an answer and lists answers and extra words as one box")
	void theStudentViewKeepsTheTextAndListsOneBox() {
		// given the word-box type and two payloads with the same segments and different extra words
		List<WordBoxSegment> segments = List.of(
				text("I "), gap("go"), text(" to school and "), gap("ran"), text(" home, "), gap("go"), text(" again."));
		var first = new WordBoxPayload(segments, List.of("went", "run"));
		var second = new WordBoxPayload(segments, List.of("swam", "jump"));

		// when studentView is called with each payload
		JsonNode firstView = mapper.valueToTree(type.studentView(first));
		JsonNode secondView = mapper.valueToTree(type.studentView(second));

		// then each result has exactly the properties segments and wordBox
		assertThat(firstView.propertyNames()).containsExactlyInAnyOrder("segments", "wordBox");
		assertThat(secondView.propertyNames()).containsExactlyInAnyOrder("segments", "wordBox");
		// and the segments are the seven entries in order, each gap with no property but its type
		JsonNode expectedSegments = mapper.readTree("""
				[
					{"type": "text", "text": "I "},
					{"type": "gap"},
					{"type": "text", "text": " to school and "},
					{"type": "gap"},
					{"type": "text", "text": " home, "},
					{"type": "gap"},
					{"type": "text", "text": " again."}
				]""");
		assertThat(firstView.get("segments")).isEqualTo(expectedSegments);
		// and the segments of the two views are equal
		assertThat(secondView.get("segments")).isEqualTo(firstView.get("segments"));
		// and the word box holds "go" twice, "ran" and the two extra words of that payload
		assertThat(wordBox(firstView)).containsExactlyInAnyOrder("go", "go", "ran", "went", "run");
		assertThat(wordBox(secondView)).containsExactlyInAnyOrder("go", "go", "ran", "swam", "jump");
		// and the two word boxes have the same size, and no property marks a word as extra
		assertThat(wordBox(firstView)).hasSameSizeAs(wordBox(secondView));
		assertThat(mapper.writeValueAsString(firstView)).doesNotContain("extra");
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S9] The serialised student view has no `answer` and no `extraWords` key, in English and Russian")
	void theSerialisedStudentViewHasNoAnswerAndNoExtraWords() {
		// given an English payload and a Russian payload with texts that contain neither "answer" nor "extraWords"
		var english = new WordBoxPayload(
				List.of(text("He "), gap("walks"), text(" and she "), gap("runs"), text(".")),
				List.of("swims"));
		var russian = new WordBoxPayload(
				List.of(text("Мой "), gap("Ёжик"), text(" любит "), gap("ежевику"), text(".")),
				List.of("ёжик", "ежик"));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then the JSON is an object whose keys are exactly segments and wordBox, in this order
		for (String json : List.of(englishJson, russianJson)) {
			assertThat(mapper.readTree(json).propertyNames()).containsExactly("segments", "wordBox");
			// and it contains no answer and no extraWords property name
			assertThat(json).doesNotContain("\"answer\"", "extraWords");
			// and each gap is exactly {"type":"gap"}
			assertThat(mapper.readTree(json).get("segments").get(1).toString()).isEqualTo("{\"type\":\"gap\"}");
			assertThat(mapper.readTree(json).get("segments").get(3).toString()).isEqualTo("{\"type\":\"gap\"}");
		}
		// and every text segment appears exactly with type before text
		assertThat(englishJson).contains(
				"{\"type\":\"text\",\"text\":\"He \"}",
				"{\"type\":\"text\",\"text\":\" and she \"}",
				"{\"type\":\"text\",\"text\":\".\"}");
		assertThat(russianJson).contains(
				"{\"type\":\"text\",\"text\":\"Мой \"}",
				"{\"type\":\"text\",\"text\":\" любит \"}",
				"{\"type\":\"text\",\"text\":\".\"}");
		// and the word box holds exactly the answers and extra words, "Ёжик" and "ёжик" as different entries
		assertThat(wordBox(mapper.readTree(englishJson))).containsExactlyInAnyOrder("walks", "runs", "swims");
		assertThat(wordBox(mapper.readTree(russianJson))).containsExactlyInAnyOrder("Ёжик", "ежевику", "ёжик", "ежик");
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S10] The box order is the seeded shuffle of the sorted words and does not depend on the gap order")
	void theBoxOrderIsTheSeededShuffleOfTheSortedWords() {
		// given a payload with the gaps "c", "a", "b" and extra words "d", "e", and a second one with the gaps "b", "c", "a" and extra words "e", "d"
		var first = new WordBoxPayload(
				List.of(text("A "), gap("c"), text(" B "), gap("a"), text(" C "), gap("b")),
				List.of("d", "e"));
		var second = new WordBoxPayload(
				List.of(text("A "), gap("b"), text(" B "), gap("c"), text(" C "), gap("a")),
				List.of("e", "d"));

		// when studentView is called twice with the first payload and once with the second
		JsonNode firstView = mapper.valueToTree(type.studentView(first));
		JsonNode firstAgain = mapper.valueToTree(type.studentView(first));
		JsonNode secondView = mapper.valueToTree(type.studentView(second));

		// then the word box of all three views is exactly [c, e, b, d, a]
		List<String> expected = List.of("c", "e", "b", "d", "a");
		assertThat(wordBox(firstView)).isEqualTo(expected);
		assertThat(wordBox(firstAgain)).isEqualTo(expected);
		assertThat(wordBox(secondView)).isEqualTo(expected);
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S11] The student view of corrupt data throws")
	void theStudentViewOfCorruptDataThrows() {
		// given payload records built directly: an unknown segment type, a text without text, a null segment a gap without an answer and a null extra word
		var unknownType = new WordBoxPayload(List.of(new WordBoxSegment("image", null, null), gap("go")), null);
		var nullText = new WordBoxPayload(List.of(new WordBoxSegment("text", null, null), gap("go")), null);
		var nullSegment = new WordBoxPayload(Arrays.asList(gap("go"), null), null);
		var nullAnswer = new WordBoxPayload(List.of(text("I "), new WordBoxSegment("gap", null, null)), null);
		var nullExtraWord = new WordBoxPayload(List.of(text("I "), gap("go")), Arrays.asList("x", null));

		// when studentView is called with each payload
		// then each call throws IllegalStateException
		for (WordBoxPayload payload : List.of(unknownType, nullText, nullSegment, nullAnswer, nullExtraWord)) {
			assertThatThrownBy(() -> type.studentView(payload)).isInstanceOf(IllegalStateException.class);
		}
	}
}
