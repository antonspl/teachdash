package com.teachdash.items.domain.sort;

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

/** item-sort R1, student view. */
@JsonTest
class SortItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final SortItemType type = new SortItemType();

	private static SortColumn column(String title, String... words) {
		return new SortColumn(title, List.of(words));
	}

	private List<String> strings(JsonNode view, String name) {
		assertThat(view.has(name)).as("the view has a " + name + " property").isTrue();
		List<String> values = new ArrayList<>();
		view.get(name).forEach(value -> values.add(value.asString()));
		return values;
	}

	@Test
	@DisplayName("[item-sort/R1-S7] The student view lists the titles in order and the words in the seeded shuffled order")
	void theStudentViewListsTitlesInOrderAndWordsShuffled() {
		// given the sort type and a payload with two columns of three words
		var payload = new SortPayload(List.of(
				column("Animals", "cat", "dog", "horse"),
				column("Fruits", "apple", "pear", "plum")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the result has exactly the properties titles and words, in this order
		assertThat(view.propertyNames()).containsExactly("titles", "words");
		// and titles are in stored order and words are the seeded shuffle of the sorted words
		assertThat(strings(view, "titles")).containsExactly("Animals", "Fruits");
		assertThat(strings(view, "words")).containsExactly("cat", "horse", "apple", "pear", "dog", "plum");
	}

	@Test
	@DisplayName("[item-sort/R1-S8] The shuffled order is the same on every call, in Russian too, and does not depend on which column holds which word")
	void theShuffledOrderIsTheSameOnEveryCall() {
		// given a payload with three Russian columns, the same columns with the first one moved last,
		// and the English payload of R1-S7 where cat and apple swap columns
		var payload = new SortPayload(List.of(
				column("Мужской род", "стол", "дом"),
				column("Женский род", "книга", "лампа"),
				column("Средний род", "окно", "море")));
		var reordered = new SortPayload(List.of(
				column("Женский род", "книга", "лампа"),
				column("Средний род", "окно", "море"),
				column("Мужской род", "стол", "дом")));
		var swapped = new SortPayload(List.of(
				column("Animals", "apple", "dog", "horse"),
				column("Fruits", "cat", "pear", "plum")));

		// when studentView is called three times with the payload and once with the reordered one
		JsonNode first = mapper.valueToTree(type.studentView(payload));
		JsonNode second = mapper.valueToTree(type.studentView(payload));
		JsonNode third = mapper.valueToTree(type.studentView(payload));
		JsonNode reorderedView = mapper.valueToTree(type.studentView(reordered));
		JsonNode swappedView = mapper.valueToTree(type.studentView(swapped));

		// then the three results are equal
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
		// and each has the titles in stored order and the words in the seeded order
		assertThat(strings(first, "titles")).containsExactly("Мужской род", "Женский род", "Средний род");
		assertThat(strings(first, "words")).containsExactly("стол", "лампа", "море", "книга", "дом", "окно");
		// and the reordered payload gives its own titles and the same words in the same order
		assertThat(strings(reorderedView, "titles")).containsExactly("Женский род", "Средний род", "Мужской род");
		assertThat(strings(reorderedView, "words")).containsExactly("стол", "лампа", "море", "книга", "дом", "окно");
		// and the swapped payload gives its own titles and the same words as R1-S7
		assertThat(strings(swappedView, "titles")).containsExactly("Animals", "Fruits");
		assertThat(strings(swappedView, "words")).containsExactly("cat", "horse", "apple", "pear", "dog", "plum");
	}

	@Test
	@DisplayName("[item-sort/R1-S9] The serialised student view has no column of a word, in English and Russian")
	void theSerialisedStudentViewHasNoColumnOfAWord() {
		// given an English payload and a Russian payload with two columns of two words each
		var english = new SortPayload(List.of(
				column("Animals", "cat", "dog"),
				column("Fruits", "apple", "pear")));
		var russian = new SortPayload(List.of(
				column("Глаголы", "бежать", "читать"),
				column("Существительные", "ёжик", "мышь")));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then each JSON is an object with exactly the properties titles and words, each an array of strings
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode view = mapper.readTree(json);
			assertThat(view.propertyNames()).containsExactlyInAnyOrder("titles", "words");
			assertThat(view.get("titles")).hasSize(2).allMatch(JsonNode::isString);
			assertThat(view.get("words")).hasSize(4).allMatch(JsonNode::isString);
			// and it contains no property name columns
			assertThat(json).doesNotContain("columns");
		}
		// and every title and every word appears exactly once as an array element
		JsonNode englishView = mapper.readTree(englishJson);
		assertThat(strings(englishView, "titles")).containsExactlyInAnyOrder("Animals", "Fruits");
		assertThat(strings(englishView, "words")).containsExactlyInAnyOrder("cat", "dog", "apple", "pear");
		JsonNode russianView = mapper.readTree(russianJson);
		assertThat(strings(russianView, "titles")).containsExactlyInAnyOrder("Глаголы", "Существительные");
		assertThat(strings(russianView, "words")).containsExactlyInAnyOrder("бежать", "читать", "ёжик", "мышь");
	}

	@Test
	@DisplayName("[item-sort/R1-S10] The student view of corrupt data fails without echoing the stored values")
	void theStudentViewOfCorruptDataFailsWithoutEchoingValues() {
		// given corrupt payloads: null columns, a null column, a null title, null words and a null word,
		// each (except the first) next to a valid column with "secret-title" and "secret-word"
		var secret = column("secret-title", "secret-word");
		var nullColumns = new SortPayload(null);
		var nullColumn = new SortPayload(Arrays.asList(secret, null));
		var nullTitle = new SortPayload(List.of(secret, new SortColumn(null, List.of("a"))));
		var nullWords = new SortPayload(List.of(secret, new SortColumn("b", null)));
		var nullWord = new SortPayload(List.of(secret, new SortColumn("c", Arrays.asList("d", null))));

		// when studentView is called with each payload
		// then each call throws IllegalStateException and no message contains a stored value
		for (SortPayload payload : List.of(nullColumns, nullColumn, nullTitle, nullWords, nullWord)) {
			assertThatThrownBy(() -> type.studentView(payload))
					.isInstanceOf(IllegalStateException.class)
					.message().isNotNull().doesNotContain("secret-title", "secret-word");
		}
	}
}
