package com.teachdash.items.api;

import static com.teachdash.items.support.ChoicePayloads.json;
import static com.teachdash.items.support.ChoicePayloads.option;
import static com.teachdash.items.support.ChoicePayloads.payload;
import static com.teachdash.items.support.ChoicePayloads.question;
import static com.teachdash.items.support.ChoicePayloads.questionWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-test R1 and R3-S1. */
class ChoiceItemTest extends ItemApiTest {

	private static Map<String, Object> testItem(Object payload) {
		return body("test", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-test/R1-S1] Add an English test item, it is stored and listed exactly")
	void addAnEnglishTestItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type test with two questions (the second with multiple true) and then lists the items of the part
		var sent = payload(
				question(
						"Which word is a verb?",
						option("run", true),
						option("table", false),
						option("green", false)),
				questionWith(
						"Pick the past forms.",
						true,
						option("went", true),
						option("go", false),
						option("saw", true)));
		var response = setup.client().post(setup.items(), testItem(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type test, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("test"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and no object has a property it was not sent with (no multiple in the first question, no null field)
		response.then()
				.body("payload.questions[0]", not(hasKey("multiple")))
				.body("payload.questions[1].multiple", equalTo(true));
		// and the second question keeps multiple true, and the stored and listed payloads equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-test/R1-S2] Add a Russian test item, Cyrillic and spaces are kept exactly")
	void addARussianTestItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type test with padded texts and four options that differ by "ё", "е", case and markup
		var sent = payload(question(
				"  Кто написал «Ёж»?  ",
				option(" Ёлка ", true),
				option("елка", false),
				option("ЁЛКА", false),
				option("<b>ёлка</b>", false)));
		var response = client.post(items(course, lesson, part), testItem(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201).body("payload", equalTo(sent));
		// and the stored payload has the same content
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-test/R1-S3] Edit a test item: the payload is replaced")
	void editATestItem() {
		// given a logged-in teacher A with an item of the type test with one question and version 1
		var setup = teacherWithPart();
		var original = payload(question("2 + 2?", option("3", false), option("4", true)));
		UUID item = createItem(setup.teacher(), setup.part(), "test", json(original), 1);

		// when teacher A edits it with version 1 and a payload of two other questions
		var replaced = payload(
				question("2 + 3?", option("5", true), option("6", false)),
				question("3 + 3?", option("6", true), option("7", false)));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type test, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("test"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the two new questions
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-test/R1-S4] Editing a test item follows the test rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with an item of the type test with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(question("Q", option("A", true), option("B", false)));
		UUID item = createItem(setup.teacher(), setup.part(), "test", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and the payloads without questions and with a question with one option
		var noQuestions = setup.client().patch(path, edit(1, payload(List.of())));
		var oneOption = setup.client().patch(path, edit(1, payload(question("Q", option("A", true)))));

		// then both responses are 400 VALIDATION_FAILED with the errors of the scenario
		expectValidation(noQuestions, error("payload.questions", "REQUIRED"));
		expectValidation(oneOption, error("payload.questions[0].options", "TOO_SHORT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-test/R1-S5] Another teacher cannot add or edit a test item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a test item in it
		var setup = teacherWithPart();
		var original = payload(question("Q", option("A", true), option("B", false)));
		UUID item = createItem(setup.teacher(), setup.part(), "test", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a test item to teacher A's part and edits teacher A's item, each with a valid payload
		var valid = payload(question("Hi", option("A", true), option("B", false)));
		var add = teacherB.post(setup.items(), testItem(valid));
		var editResponse = teacherB.patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, valid));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-test/R3-S1] A question with no correct option and one with all options correct are accepted, multiple is kept only where sent")
	void noCorrectAndAllCorrectAreAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with a no-correct, an all-correct (multiple true) and a one-correct question
		var sent = payload(
				question("What do you think?", option("Yes", false), option("No", false)),
				questionWith("Pick all.", true, option("A", true), option("B", true), option("C", true)),
				question("Pick one.", option("A", true), option("B", false)));
		var added = setup.client().post(setup.items(), testItem(sent));
		added.then().statusCode(201);
		UUID item = UUID.fromString(added.path("id"));

		// and edits it with version 1 so that the first question's option "Yes" is correct
		var edited = payload(
				question("What do you think?", option("Yes", true), option("No", false)),
				questionWith("Pick all.", true, option("A", true), option("B", true), option("C", true)),
				question("Pick one.", option("A", true), option("B", false)));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, edited));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the payload exactly as sent
		added.then().statusCode(201).body("payload", equalTo(sent));
		// and the edit response is 200 with version 2 and the edited payload exactly as sent
		response.then().statusCode(200)
				.body("version", equalTo(2))
				.body("payload", equalTo(edited));
		// and neither payload, stored or listed, has a multiple property in the first and the third question
		assertStoredPayload(item, json(edited));
		listed.then().statusCode(200).body("[0].payload", equalTo(edited));
		for (var body : List.of(added, response, listed)) {
			String prefix = body == listed ? "[0].payload" : "payload";
			body.then()
					.body(prefix + ".questions[0]", not(hasKey("multiple")))
					.body(prefix + ".questions[2]", not(hasKey("multiple")));
		}
		// and only "Pick all." keeps "multiple": true
		response.then().body("payload.questions[1].multiple", equalTo(true));
		assertEquals(1, readItem(item).payload().split("\"multiple\"", -1).length - 1);
	}
}
