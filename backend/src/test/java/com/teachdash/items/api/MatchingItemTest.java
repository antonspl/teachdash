package com.teachdash.items.api;

import static com.teachdash.items.support.MatchingPayloads.json;
import static com.teachdash.items.support.MatchingPayloads.pair;
import static com.teachdash.items.support.MatchingPayloads.payload;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-matching R1, REST scenarios. */
class MatchingItemTest extends ItemApiTest {

	private static Map<String, Object> matching(Object payload) {
		return body("matching", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-matching/R1-S1] Add an English matching item, it is stored and listed exactly")
	void addAnEnglishMatchingItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type matching with three pairs and then lists the items of the part
		var sent = payload(
				pair("apple", "яблоко"),
				pair("dog", "собака"),
				pair("house", "дом"));
		var response = setup.client().post(setup.items(), matching(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type matching, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("matching"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and no pair has a property it was not sent with
		for (int i = 0; i < 3; i++) {
			Map<String, Object> pair = response.path("payload.pairs[" + i + "]");
			assertEquals(2, pair.size());
		}
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-matching/R1-S2] Add a Russian matching item, Cyrillic is kept exactly, \"ё\" and \"е\" stay distinct")
	void addARussianMatchingItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type matching with four Cyrillic lefts
		var sent = payload(
				pair("Ёж", "животное"),
				pair("Еж", "колючка"),
				pair("ёлка", "дерево"),
				pair("елка", "хвоя"));
		var response = client.post(items(course, lesson, part), matching(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201)
				.body("payload", equalTo(sent))
				.body("payload.pairs.left", equalTo(List.of("Ёж", "Еж", "ёлка", "елка")));
		// and the stored payload has the same pairs
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-matching/R1-S3] Values are stored raw, with no trim, case change or normalisation")
	void valuesAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with spaces, different case, and a composed and a decomposed "é" as lefts
		String composed = "\u00e9";
		String decomposed = "e\u0301";
		var sent = payload(
				pair(" Go ", "идти"),
				pair("GO", "ехать"),
				pair("go", "ходить"),
				pair(composed, "e1"),
				pair(decomposed, "e2"));
		var response = setup.client().post(setup.items(), matching(sent));

		// then the response is 201 and the five lefts are exactly these strings in this order
		response.then().statusCode(201)
				.body("payload.pairs.left", equalTo(List.of(" Go ", "GO", "go", composed, decomposed)));
		// and the stored payload has the same five pairs
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-matching/R1-S4] A left may equal a right, and a pair may have the same text on both sides")
	void aLeftMayEqualARight() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds two items where a left equals a right
		var crossed = payload(
				pair("go", "went"),
				pair("went", "go"));
		var sameText = payload(
				pair("no", "no"),
				pair("ok", "ok"));
		var first = setup.client().post(setup.items(), matching(crossed));
		var second = setup.client().post(setup.items(), matching(sameText));

		// then both responses are 201 with the pairs exactly as sent, in the positions 1 and 2
		first.then().statusCode(201)
				.body("payload", equalTo(crossed))
				.body("position", equalTo(1));
		second.then().statusCode(201)
				.body("payload", equalTo(sameText))
				.body("position", equalTo(2));
	}

	@Test
	@DisplayName("[item-matching/R1-S5] Edit a matching item: the payload is replaced")
	void editAMatchingItem() {
		// given a logged-in teacher A with a matching item with two pairs and version 1
		var setup = teacherWithPart();
		var original = payload(
				pair("cat", "кошка"),
				pair("dog", "собака"));
		UUID item = createItem(setup.teacher(), setup.part(), "matching", json(original), 1);

		// when teacher A edits it with version 1 and a payload with three other pairs
		var replaced = payload(
				pair("dog", "пёс"),
				pair("bird", "птица"),
				pair("fish", "рыба"));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type matching, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("matching"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the new three pairs
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-matching/R1-S6] Editing a matching item follows the matching rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with a matching item with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(
				pair("cat", "кошка"),
				pair("dog", "собака"));
		UUID item = createItem(setup.teacher(), setup.part(), "matching", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload with one pair, and with a payload with a repeated left
		var onePair = setup.client().patch(path, edit(1, payload(pair("cat", "кошка"))));
		var repeatedLeft = setup.client().patch(path, edit(1, payload(pair("cat", "кошка"), pair("cat", "кот"))));

		// then both responses are 400 VALIDATION_FAILED with the TOO_SHORT and INVALID_FORMAT errors
		expectValidation(onePair, error("payload.pairs", "TOO_SHORT"));
		expectValidation(repeatedLeft, error("payload.pairs[1].left", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-matching/R1-S7] Another teacher cannot add or edit a matching item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a matching item in it
		var setup = teacherWithPart();
		var original = payload(
				pair("cat", "кошка"),
				pair("dog", "собака"));
		UUID item = createItem(setup.teacher(), setup.part(), "matching", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a matching item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(
				pair("a", "b"),
				pair("c", "d"));
		var add = teacherB.post(setup.items(), matching(valid));
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
