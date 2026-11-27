package com.teachdash.items.api;

import static com.teachdash.items.support.TrueFalsePayloads.json;
import static com.teachdash.items.support.TrueFalsePayloads.payload;
import static com.teachdash.items.support.TrueFalsePayloads.payloadWith;
import static com.teachdash.items.support.TrueFalsePayloads.statement;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-true-false R1, the API scenarios. */
class TrueFalseItemTest extends ItemApiTest {

	private static Map<String, Object> trueFalse(Object payload) {
		return body("true-false", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-true-false/R1-S1] Add an English true / false item with \"not stated\", it is stored and listed exactly")
	void addAnEnglishTrueFalseItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type true-false with three statements and then lists the items of the part
		var sent = payloadWith(
				true,
				statement("London is the capital of France.", "false"),
				statement("Water boils at 100 degrees Celsius at sea level.", "true"),
				statement("The teacher has a brother.", "not-stated"));
		var response = setup.client().post(setup.items(), trueFalse(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type true-false, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("true-false"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-true-false/R1-S2] Add a Russian item without allowNotStated, Cyrillic text is kept exactly")
	void addARussianItemWithoutAllowNotStated() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type true-false with two Cyrillic statements and no allowNotStated
		var sent = payload(
				statement("  «Ёж» — это животное. ", "true"),
				statement("Ёлка и елка — одно и то же слово.", "false"));
		var response = client.post(items(course, lesson, part), trueFalse(sent));

		// then the response is 201 and the payload equals the sent one character by character, with no allowNotStated property
		response.then().statusCode(201).body("payload", equalTo(sent));
		// and the stored payload has the same statements and no allowNotStated key
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-true-false/R1-S3] allowNotStated false is stored as sent, null is dropped")
	void allowNotStatedFalseIsStoredAndNullIsDropped() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type true-false with allowNotStated false and with allowNotStated null
		var withFalse = payloadWith(false, statement("A cat is an animal.", "true"));
		var withNull = payloadWith(null, statement("A cat is an animal.", "true"));
		var first = setup.client().post(setup.items(), trueFalse(withFalse));
		var second = setup.client().post(setup.items(), trueFalse(withNull));

		// then both responses are 201 in the positions 1 and 2
		first.then().statusCode(201).body("position", equalTo(1));
		second.then().statusCode(201).body("position", equalTo(2));
		// and the first payload has allowNotStated false
		var expectedFirst = payloadWith(false, statement("A cat is an animal.", "true"));
		first.then().body("payload", equalTo(expectedFirst));
		// and the second has no allowNotStated property
		var expectedSecond = payload(statement("A cat is an animal.", "true"));
		second.then().body("payload", equalTo(expectedSecond));
		// and the stored payloads equal the response payloads
		assertStoredPayload(UUID.fromString(first.path("id")), json(expectedFirst));
		assertStoredPayload(UUID.fromString(second.path("id")), json(expectedSecond));
	}

	@Test
	@DisplayName("[item-true-false/R1-S4] Edit a true / false item: the payload is replaced, not merged")
	void editATrueFalseItem() {
		// given a logged-in teacher A with an item of the type true-false with allowNotStated true, two statements and version 1
		var setup = teacherWithPart();
		var original = payloadWith(
				true,
				statement("Sofia is in Bulgaria.", "true"),
				statement("Sofia has a metro of ten lines.", "not-stated"));
		UUID item = createItem(setup.teacher(), setup.part(), "true-false", json(original), 1);

		// when teacher A edits it with version 1 and a payload with three statements in a new order and no allowNotStated
		var replaced = payload(
				statement("Sofia has a metro of ten lines.", "false"),
				statement("Sofia is in Bulgaria.", "true"),
				statement("Sofia is a port.", "false"));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type true-false, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("true-false"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload equals it
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-true-false/R1-S5] Editing follows the true-false rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with an item of the type true-false with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(statement("The sky is blue.", "true"));
		UUID item = createItem(setup.teacher(), setup.part(), "true-false", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a "not-stated" answer without allowNotStated, and with no statements
		var notStated = setup.client().patch(path, edit(1, payload(statement("The sky is blue.", "not-stated"))));
		var noStatements = setup.client().patch(path, edit(1, payloadWith(true)));

		// then both responses are 400 VALIDATION_FAILED with the errors of the scenario
		expectValidation(notStated, error("payload.statements[0].answer", "INVALID_FORMAT"));
		expectValidation(noStatements, error("payload.statements", "REQUIRED"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-true-false/R1-S6] Another teacher cannot add or edit a true / false item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a true-false item in it
		var setup = teacherWithPart();
		var original = payload(statement("The sky is blue.", "true"));
		UUID item = createItem(setup.teacher(), setup.part(), "true-false", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a true-false item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(statement("Grass is green.", "true"));
		var add = teacherB.post(setup.items(), trueFalse(valid));
		var editResponse = teacherB.patch(
				item(setup.course(), setup.lesson(), setup.part(), item),
				edit(1, valid));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}
}
