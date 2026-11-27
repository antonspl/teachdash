package com.teachdash.items.domain.matching;

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

/** item-matching R1, student view. */
@JsonTest
class MatchingItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final MatchingItemType type = new MatchingItemType();

	private static MatchingPair pair(String left, String right) {
		return new MatchingPair(left, right);
	}

	private List<String> column(JsonNode view, String name) {
		assertThat(view.has(name)).as("the view has a " + name + " property").isTrue();
		List<String> values = new ArrayList<>();
		view.get(name).forEach(value -> values.add(value.asString()));
		return values;
	}

	@Test
	@DisplayName("[item-matching/R1-S8] The student view lists the lefts in order and the rights in the seeded shuffled order")
	void theStudentViewListsLeftsInOrderAndRightsShuffled() {
		// given the matching type and a payload with four pairs
		var payload = new MatchingPayload(List.of(
				pair("apple", "яблоко"),
				pair("dog", "собака"),
				pair("house", "дом"),
				pair("tree", "дерево")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the result has exactly the properties left and right, in this order
		assertThat(view.propertyNames()).containsExactly("left", "right");
		// and left is in stored order and right is the seeded shuffle of the sorted rights
		assertThat(column(view, "left")).containsExactly("apple", "dog", "house", "tree");
		assertThat(column(view, "right")).containsExactly("собака", "яблоко", "дом", "дерево");
	}

	@Test
	@DisplayName("[item-matching/R1-S9] The shuffled order is the same on every call and does not depend on the pairing, in Russian too")
	void theShuffledOrderIsTheSameOnEveryCall() {
		// given a payload with four Russian lefts and English rights, and the same pairs in the reverse order
		var payload = new MatchingPayload(List.of(
				pair("Москва", "Russia"),
				pair("Париж", "France"),
				pair("Лондон", "England"),
				pair("Берлин", "Germany")));
		var reversed = new MatchingPayload(List.of(
				pair("Берлин", "Germany"),
				pair("Лондон", "England"),
				pair("Париж", "France"),
				pair("Москва", "Russia")));

		// when studentView is called three times with the payload and once with the reversed one
		JsonNode first = mapper.valueToTree(type.studentView(payload));
		JsonNode second = mapper.valueToTree(type.studentView(payload));
		JsonNode third = mapper.valueToTree(type.studentView(payload));
		JsonNode reversedView = mapper.valueToTree(type.studentView(reversed));

		// then the three results are equal
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
		// and each has the lefts in stored order and the rights in the seeded order
		assertThat(column(first, "left")).containsExactly("Москва", "Париж", "Лондон", "Берлин");
		assertThat(column(first, "right")).containsExactly("Germany", "France", "Russia", "England");
		// and the reversed payload gives the same right order, whatever the pairing
		assertThat(column(reversedView, "left")).containsExactly("Берлин", "Лондон", "Париж", "Москва");
		assertThat(column(reversedView, "right")).containsExactly("Germany", "France", "Russia", "England");
	}

	@Test
	@DisplayName("[item-matching/R1-S10] The serialised student view has no pairing, in English and Russian")
	void theSerialisedStudentViewHasNoPairing() {
		// given an English payload and a Russian payload with two pairs each
		var english = new MatchingPayload(List.of(
				pair("cat", "кошка"),
				pair("dog", "собака")));
		var russian = new MatchingPayload(List.of(
				pair("Ёжик", "hedgehog"),
				pair("мышь", "mouse")));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		String englishJson = mapper.writeValueAsString(type.studentView(english));
		String russianJson = mapper.writeValueAsString(type.studentView(russian));

		// then each JSON is an object with exactly the properties left and right, each an array of two elements
		for (String json : List.of(englishJson, russianJson)) {
			JsonNode view = mapper.readTree(json);
			assertThat(view.propertyNames()).containsExactlyInAnyOrder("left", "right");
			assertThat(view.get("left").isArray()).isTrue();
			assertThat(view.get("right").isArray()).isTrue();
			assertThat(view.get("left")).hasSize(2);
			assertThat(view.get("right")).hasSize(2);
			// and it contains no property name pairs and no index
			assertThat(json).doesNotContain("pairs", "index");
		}
		// and every left and every right appears exactly once as an array element
		JsonNode englishView = mapper.readTree(englishJson);
		assertThat(column(englishView, "left")).containsExactlyInAnyOrder("cat", "dog");
		assertThat(column(englishView, "right")).containsExactlyInAnyOrder("кошка", "собака");
		JsonNode russianView = mapper.readTree(russianJson);
		assertThat(column(russianView, "left")).containsExactlyInAnyOrder("Ёжик", "мышь");
		assertThat(column(russianView, "right")).containsExactlyInAnyOrder("hedgehog", "mouse");
	}

	@Test
	@DisplayName("[item-matching/R1-S11] The student view of corrupt data fails without echoing the stored values")
	void theStudentViewOfCorruptDataFailsWithoutEchoingValues() {
		// given corrupt payloads: null pairs, a null pair, a null left and a null right, each next to a valid pair with "secret-pair"
		var secret = pair("secret-pair", "secret-pair");
		var nullPairs = new MatchingPayload(null);
		var nullPair = new MatchingPayload(Arrays.asList(secret, null));
		var nullLeft = new MatchingPayload(List.of(secret, pair(null, "b")));
		var nullRight = new MatchingPayload(List.of(secret, pair("a", null)));

		// when studentView is called with each payload
		// then each call throws IllegalStateException and no message contains "secret-pair"
		for (MatchingPayload payload : List.of(nullPairs, nullPair, nullLeft, nullRight)) {
			assertThatThrownBy(() -> type.studentView(payload))
					.isInstanceOf(IllegalStateException.class)
					.message().isNotNull().doesNotContain("secret-pair");
		}
	}
}
