package com.teachdash.items.api;

import static com.teachdash.items.support.ReorderPayloads.entry;
import static com.teachdash.items.support.ReorderPayloads.entryWithHint;
import static com.teachdash.items.support.ReorderPayloads.json;
import static com.teachdash.items.support.ReorderPayloads.payload;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-reorder R1, REST scenarios. */
class ReorderItemTest extends ItemApiTest {

	private static Map<String, Object> reorder(Object payload) {
		return body("reorder", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-reorder/R1-S1] Add an English words item, it is stored and listed exactly")
	void addAnEnglishWordsItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type reorder with two entries and then lists the items of the part
		var sent = payload(
				"words",
				entryWithHint("Я люблю зелёные яблоки", "I", "like", "green", "apples"),
				entry("She", "reads", "books"));
		var response = setup.client().post(setup.items(), reorder(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type reorder, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("reorder"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and the first entry has exactly units and hint, the second only units
		Map<String, Object> first = response.path("payload.entries[0]");
		Map<String, Object> second = response.path("payload.entries[1]");
		assertEquals(2, first.size());
		assertEquals(1, second.size());
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-reorder/R1-S2] Add Russian sentences and letters items, Cyrillic is kept exactly")
	void addRussianSentencesAndLettersItems() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds a sentences item and then a letters item
		var sentences = payload(
				"sentences",
				entryWithHint(
						"Расскажи, как прошёл день",
						"Сначала мы позавтракали.",
						"Потом пошли в школу.",
						"После уроков вернулись домой."));
		var letters = payload(
				"letters",
				entry("Ё", "ж"),
				entry("е", "л", "к", "а"));
		var first = client.post(items(course, lesson, part), reorder(sentences));
		var second = client.post(items(course, lesson, part), reorder(letters));

		// then both responses are 201 with the payloads equal to the sent ones, in the positions 1 and 2
		first.then().statusCode(201)
				.body("payload", equalTo(sentences))
				.body("position", equalTo(1));
		second.then().statusCode(201)
				.body("payload", equalTo(letters))
				.body("payload.entries[0].units", equalTo(List.of("Ё", "ж")))
				.body("payload.entries[1].units", equalTo(List.of("е", "л", "к", "а")))
				.body("position", equalTo(2));
		// and the stored payloads equal the sent ones
		assertStoredPayload(UUID.fromString(first.path("id")), json(sentences));
		assertStoredPayload(UUID.fromString(second.path("id")), json(letters));
	}

	@Test
	@DisplayName("[item-reorder/R1-S3] Values are stored raw, with no trim, case change or normalisation")
	void valuesAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with spaces, different case, a composed and a decomposed "é" and a hint with spaces
		String composed = "\u00e9";
		String decomposed = "e\u0301";
		var sent = payload(
				"words",
				entryWithHint(" Order! ", " Go ", "GO", "go", composed, decomposed));
		var response = setup.client().post(setup.items(), reorder(sent));

		// then the response is 201 and the five units and the hint are exactly these strings
		response.then().statusCode(201)
				.body("payload.entries[0].units", equalTo(List.of(" Go ", "GO", "go", composed, decomposed)))
				.body("payload.entries[0].hint", equalTo(" Order! "));
		// and the stored payload is the same
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-reorder/R1-S4] Edit a reorder item: the payload is replaced, and the mode may change")
	void editAReorderItem() {
		// given a logged-in teacher A with a words item with a hint and version 1
		var setup = teacherWithPart();
		var original = payload(
				"words",
				entryWithHint("old", "I", "like", "tea"));
		UUID item = createItem(setup.teacher(), setup.part(), "reorder", json(original), 1);

		// when teacher A edits it with version 1 and a letters payload with two entries
		var replaced = payload(
				"letters",
				entry("c", "a", "t"),
				entry("d", "o", "g"));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type reorder, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("reorder"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload is the new one, without the old hint
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-reorder/R1-S5] Editing a reorder item follows the reorder rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with a reorder item with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(
				"words",
				entry("I", "like", "tea"));
		UUID item = createItem(setup.teacher(), setup.part(), "reorder", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload with one unit, and with a payload with a repeated unit
		var oneUnit = setup.client().patch(path, edit(1, payload("words", entry("only"))));
		var repeatedUnit = setup.client().patch(path, edit(1, payload("words", entry("go", "go"))));

		// then both responses are 400 VALIDATION_FAILED with the TOO_SHORT and INVALID_FORMAT errors
		expectValidation(oneUnit, error("payload.entries[0].units", "TOO_SHORT"));
		expectValidation(repeatedUnit, error("payload.entries[0].units", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-reorder/R1-S6] Another teacher cannot add or edit a reorder item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a reorder item in it
		var setup = teacherWithPart();
		var original = payload(
				"words",
				entry("I", "like", "tea"));
		UUID item = createItem(setup.teacher(), setup.part(), "reorder", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a reorder item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(
				"words",
				entry("a", "b"));
		var add = teacherB.post(setup.items(), reorder(valid));
		var editResponse = teacherB.patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, valid));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}
}
