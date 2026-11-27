package com.teachdash.items.domain.reorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
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

/** item-reorder R3, student view. */
@JsonTest
class ReorderItemTypeTest {

	@Autowired
	private JsonMapper mapper;

	private final ReorderItemType type = new ReorderItemType();

	/** An entry with units in the stored order and no hint; a unit may be null. */
	private static ReorderEntry entry(String... units) {
		return new ReorderEntry(Arrays.asList(units), null);
	}

	private static ReorderEntry entry(String hint, List<String> units) {
		return new ReorderEntry(units, hint);
	}

	private static JsonNode entryAt(JsonNode view, int index) {
		assertThat(view.has("entries")).as("the view has an entries property").isTrue();
		return view.get("entries").get(index);
	}

	private List<String> units(JsonNode entry) {
		assertThat(entry.has("units")).as("the entry has a units property").isTrue();
		List<String> units = new ArrayList<>();
		entry.get("units").forEach(unit -> units.add(unit.asString()));
		return units;
	}

	@Test
	@DisplayName("[item-reorder/R3-S1] The student view shuffles each entry's units with the seeded algorithm, in English and Russian")
	void theStudentViewShufflesEachEntry() {
		// given the reorder type and a payload with an English entry with a hint and a Russian entry without
		var payload = new ReorderPayload("words", List.of(
				entry("Я люблю зелёные яблоки", List.of("I", "like", "green", "apples")),
				entry("Я", "люблю", "читать", "книги")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the result has exactly the properties mode and entries, in this order, with the mode words and two entries
		assertThat(view.propertyNames()).containsExactly("mode", "entries");
		assertThat(view.get("mode").asString()).isEqualTo("words");
		assertThat(view.get("entries")).hasSize(2);
		// and the first entry has exactly units and hint, in this order, with the seeded shuffle and the stored hint
		JsonNode first = entryAt(view, 0);
		assertThat(first.propertyNames()).containsExactly("units", "hint");
		assertThat(units(first)).containsExactly("green", "apples", "like", "I");
		assertThat(first.get("hint").asString()).isEqualTo("Я люблю зелёные яблоки");
		// and the second entry has exactly the property units, with the seeded shuffle
		JsonNode second = entryAt(view, 1);
		assertThat(second.propertyNames()).containsExactly("units");
		assertThat(units(second)).containsExactly("книги", "читать", "люблю", "Я");
	}

	@Test
	@DisplayName("[item-reorder/R3-S2] A shuffle equal to the correct order is rotated left by one")
	void aShuffleEqualToTheCorrectOrderIsRotated() {
		// given the reorder type and a payload with three Russian sentences and with go, went
		var payload = new ReorderPayload("sentences", List.of(
				entry("Сначала мы позавтракали.", "Потом пошли в школу.", "После уроков вернулись домой."),
				entry("go", "went")));

		// when studentView is called with the payload
		JsonNode view = mapper.valueToTree(type.studentView(payload));

		// then the first entry's units are the stored order rotated left by one
		JsonNode first = entryAt(view, 0);
		assertThat(units(first)).containsExactly(
				"Потом пошли в школу.",
				"После уроков вернулись домой.",
				"Сначала мы позавтракали.");
		// and the second entry's units are went, go: the shuffle gave go, went, which was rotated
		JsonNode second = entryAt(view, 1);
		assertThat(units(second)).containsExactly("went", "go");
	}

	@Test
	@DisplayName("[item-reorder/R3-S3] The shuffled order is the same on every call, depends on the stored order, and keeps repeated units")
	void theShuffledOrderIsStableAndKeepsRepeatedUnits() {
		// given three letters payloads with the units a, b and b, a and a, b, a
		var ab = new ReorderPayload("letters", List.of(entry("a", "b")));
		var ba = new ReorderPayload("letters", List.of(entry("b", "a")));
		var aba = new ReorderPayload("letters", List.of(entry("a", "b", "a")));

		// when studentView is called three times with the first payload and once with each of the others
		JsonNode first = mapper.valueToTree(type.studentView(ab));
		JsonNode second = mapper.valueToTree(type.studentView(ab));
		JsonNode third = mapper.valueToTree(type.studentView(ab));
		JsonNode reversed = mapper.valueToTree(type.studentView(ba));
		JsonNode repeated = mapper.valueToTree(type.studentView(aba));

		// then the three results are equal, with the units b, a (rotated)
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
		assertThat(units(entryAt(first, 0))).containsExactly("b", "a");
		// and b, a gives a, b, a different order from the first
		assertThat(units(entryAt(reversed, 0))).containsExactly("a", "b");
		// and a, b, a gives a, a, b with both a kept
		assertThat(units(entryAt(repeated, 0))).containsExactly("a", "a", "b");
	}

	@Test
	@DisplayName("[item-reorder/R3-S4] The serialised student view has only the allowed properties, in English and Russian")
	void theSerialisedStudentViewHasOnlyAllowedProperties() {
		// given an English payload with a hint and a null hint, and a Russian letters payload
		var english = new ReorderPayload("words", List.of(
				entry("A short sentence", List.of("The", "cat", "sat")),
				entry(null, List.of("go", "went"))));
		var russian = new ReorderPayload("letters", List.of(
				entry("Животное", List.of("к", "о", "т"))));

		// when each payload's student view is serialised to JSON with the application's JSON mapper
		JsonNode englishView = mapper.readTree(mapper.writeValueAsString(type.studentView(english)));
		JsonNode russianView = mapper.readTree(mapper.writeValueAsString(type.studentView(russian)));

		// then each JSON is an object with exactly the properties mode and entries
		assertThat(englishView.propertyNames()).containsExactly("mode", "entries");
		assertThat(russianView.propertyNames()).containsExactly("mode", "entries");
		assertThat(englishView.get("mode").asString()).isEqualTo("words");
		assertThat(russianView.get("mode").asString()).isEqualTo("letters");
		// and each entry has exactly units and, only when the stored hint is not null, hint
		JsonNode withHint = entryAt(englishView, 0);
		JsonNode withNullHint = entryAt(englishView, 1);
		JsonNode russianEntry = entryAt(russianView, 0);
		assertThat(withHint.propertyNames()).containsExactly("units", "hint");
		assertThat(withNullHint.propertyNames()).containsExactly("units");
		assertThat(russianEntry.propertyNames()).containsExactly("units", "hint");
		assertThat(withHint.get("hint").asString()).isEqualTo("A short sentence");
		assertThat(russianEntry.get("hint").asString()).isEqualTo("Животное");
		// and each units array has the same elements as the stored units
		assertThat(units(withHint)).containsExactlyInAnyOrder("The", "cat", "sat");
		assertThat(units(withNullHint)).containsExactlyInAnyOrder("go", "went");
		assertThat(units(russianEntry)).containsExactlyInAnyOrder("к", "о", "т");
	}

	@Test
	@DisplayName("[item-reorder/R3-S5] The student view of corrupt data fails without echoing the stored values")
	void theStudentViewOfCorruptDataFailsWithoutEchoingValues() {
		// given corrupt payloads, each next to a valid entry with the unit "secret-unit"
		var valid = entry("secret-unit", "other-unit");
		Map<String, ReorderPayload> corrupt = new LinkedHashMap<>();
		corrupt.put("null mode", new ReorderPayload(null, List.of(valid)));
		corrupt.put("unknown mode", new ReorderPayload("secret-mode", List.of(valid)));
		corrupt.put("null entries", new ReorderPayload("words", null));
		corrupt.put("null entry", new ReorderPayload("words", Arrays.asList(valid, null)));
		corrupt.put("null units", new ReorderPayload("words", List.of(valid, new ReorderEntry(null, null))));
		corrupt.put("null unit", new ReorderPayload("words", List.of(valid, entry("a", null))));
		corrupt.put("empty units", new ReorderPayload("words", List.of(valid, entry())));
		corrupt.put("one distinct unit", new ReorderPayload("words", List.of(valid, entry("x", "x"))));

		// when studentView is called with each payload
		// then each of the eight calls throws IllegalStateException and no message contains a stored value
		assertThat(corrupt).hasSize(8);
		corrupt.forEach((name, payload) -> assertThatThrownBy(() -> type.studentView(payload))
				.as(name)
				.isInstanceOf(IllegalStateException.class)
				.message().isNotNull()
				.doesNotContain("secret-unit", "other-unit", "secret-mode"));
	}
}
