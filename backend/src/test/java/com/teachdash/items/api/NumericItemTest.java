package com.teachdash.items.api;

import static com.teachdash.items.support.NumericPayloads.json;
import static com.teachdash.items.support.NumericPayloads.payload;
import static com.teachdash.items.support.NumericPayloads.question;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-numeric R1, add and edit. */
class NumericItemTest extends ItemApiTest {

	private static Map<String, Object> numeric(Object payload) {
		return body("numeric", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-numeric/R1-S1] Add an English numeric item, it is stored and listed exactly")
	void addAnEnglishNumericItem() {
		// given a logged-in teacher A with the course "Physics B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type numeric and then lists the items of the part
		var sent = payload(
				question("A train covers 120 km in 2 hours. How fast is it?", "60", "0.5", "km/h"),
				question("What is 1/4 as a decimal?", "0.25", "0"));
		var response = setup.client().post(setup.items(), numeric(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type numeric, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("numeric"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent))
				.body("payload.questions[1].containsKey('unit')", equalTo(false));
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-numeric/R1-S2] Add a Russian numeric item, Cyrillic, spaces and number text are kept exactly")
	void addARussianNumericItem() {
		// given a logged-in teacher A with the course "Физика 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Физика 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Движение", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type numeric with spaces in a text and numbers with a sign and leading or trailing zeros
		var sent = payload(
				question("  Масса тела?  ", "-0", "0.10", "кг"),
				question("Скорость автобуса?", "0012.50", "007", "км/ч"));
		var response = client.post(items(course, lesson, part), numeric(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201).body("payload", equalTo(sent));
		// and the stored payload has the same content
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-numeric/R1-S3] Edit a numeric item: the payload is replaced")
	void editANumericItem() {
		// given a logged-in teacher A with an item of the type numeric with one question and version 1
		var setup = teacherWithPart();
		var original = payload(question("2 + 2?", "4", "0"));
		UUID item = createItem(setup.teacher(), setup.part(), "numeric", json(original), 1);

		// when teacher A edits it with version 1 and a payload with two other questions
		var replaced = payload(
				question("Pi to two places?", "3.14", "0.01"),
				question("Вес яблока?", "150", "10", "г"));
		var response = setup.client().patch(
				item(setup.course(), setup.lesson(), setup.part(), item),
				edit(1, replaced));

		// then the response is 200 with the type numeric, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("numeric"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the two new questions
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-numeric/R1-S4] Editing a numeric item follows the numeric rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with an item of the type numeric with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(question("2 + 2?", "4", "0"));
		UUID item = createItem(setup.teacher(), setup.part(), "numeric", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and the payloads with no questions and with a negative tolerance
		var noQuestions = setup.client().patch(path, edit(1, payload()));
		var negativeTolerance = setup.client().patch(path, edit(1, payload(question("Q", "1", "-1"))));

		// then both responses are 400 VALIDATION_FAILED with exactly the errors of the scenario
		expectValidation(noQuestions, error("payload.questions", "REQUIRED"));
		expectValidation(negativeTolerance, error("payload.questions[0].tolerance", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-numeric/R1-S5] Another teacher cannot add or edit a numeric item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a numeric item in it
		var setup = teacherWithPart();
		var original = payload(question("2 + 2?", "4", "0"));
		UUID item = createItem(setup.teacher(), setup.part(), "numeric", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a numeric item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(question("Секрет?", "7", "1", "м"));
		var add = teacherB.post(setup.items(), numeric(valid));
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
		add.then().body(not(containsString("2 + 2?")));
	}
}
