package com.teachdash.items.api;

import static com.teachdash.items.support.FreeTextPayloads.json;
import static com.teachdash.items.support.FreeTextPayloads.payload;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

import io.restassured.response.Response;

/** item-free-text R1. */
class FreeTextItemTest extends ItemApiTest {

	private static Map<String, Object> freeText(Object payload) {
		return body("free-text", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	private static Set<String> keys(Response response) {
		return response.<Map<String, Object>>path("payload").keySet();
	}

	@Test
	@DisplayName("[item-free-text/R1-S1] Add an English free-text item with both limits, it is stored and listed exactly")
	void addAnEnglishFreeTextItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type free-text with both limits and then lists the items of the part
		var sent = payload("## Write\n\nDescribe **your day** in 3-5 sentences.\n", 50, 500);
		var response = setup.client().post(setup.items(), freeText(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type free-text, position 1, version 1 and a payload with exactly the sent properties
		response.then().statusCode(201)
				.body("type", equalTo("free-text"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		assertEquals(Set.of("prompt", "minLength", "maxLength"), keys(response));
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-free-text/R1-S2] Add a Russian item, Cyrillic is kept exactly, and a missing or null limit is not stored")
	void addARussianFreeTextItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");
		String items = items(course, lesson, part);

		// when teacher A adds four items with spaces and guillemets, null limits, only minLength and only maxLength
		String spaced = "  Напишите сочинение о «Ёжике в тумане».  ";
		var first = client.post(items, rawBody("free-text", "{\"prompt\": \"" + spaced + "\"}"));
		var second = client.post(items, rawBody("free-text",
				"{\"prompt\": \"Опишите ёлку.\", \"minLength\": null, \"maxLength\": null}"));
		var third = client.post(items, rawBody("free-text", "{\"prompt\": \"Опишите ёлку.\", \"minLength\": 20}"));
		var fourth = client.post(items, rawBody("free-text", "{\"prompt\": \"Опишите ёлку.\", \"maxLength\": 300}"));

		// then all four responses are 201, in the positions 1 to 4, each prompt equal to the sent one
		List<Response> responses = List.of(first, second, third, fourth);
		for (int i = 0; i < responses.size(); i++) {
			responses.get(i).then().statusCode(201).body("position", equalTo(i + 1));
		}
		first.then().body("payload.prompt", equalTo(spaced));
		for (Response response : responses.subList(1, 4)) {
			response.then().body("payload.prompt", equalTo("Опишите ёлку."));
		}
		// and the payloads have exactly the properties the scenario lists
		assertEquals(Set.of("prompt"), keys(first));
		assertEquals(Set.of("prompt"), keys(second));
		assertEquals(Set.of("prompt", "minLength"), keys(third));
		third.then().body("payload.minLength", equalTo(20));
		assertEquals(Set.of("prompt", "maxLength"), keys(fourth));
		fourth.then().body("payload.maxLength", equalTo(300));
		// and the stored payloads equal the responses
		for (Response response : responses) {
			assertStoredPayload(UUID.fromString(response.path("id")), json(response.path("payload")));
		}
	}

	@Test
	@DisplayName("[item-free-text/R1-S3] The prompt is stored raw, and the limits may be 1, 10000 or equal")
	void thePromptIsStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items with HTML and $ signs and the limits 1 and 10000, and with equal limits
		var html = payload("<b>Hi</b> $x^2$ <img src=x onerror=alert(1)>", 1, 10000);
		var equal = payload("Write one word.", 5, 5);
		var first = setup.client().post(setup.items(), freeText(html));
		var second = setup.client().post(setup.items(), freeText(equal));

		// then both responses are 201 with the payload exactly as sent
		first.then().statusCode(201).body("payload", equalTo(html));
		second.then().statusCode(201).body("payload", equalTo(equal));
		// and the stored payloads equal the responses
		assertStoredPayload(UUID.fromString(first.path("id")), json(html));
		assertStoredPayload(UUID.fromString(second.path("id")), json(equal));
	}

	@Test
	@DisplayName("[item-free-text/R1-S4] Edit a free-text item: the payload is replaced, the limits with it")
	void editReplacesThePayload() {
		// given a logged-in teacher A with a free-text item with both limits and version 1
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "free-text",
				json(payload("Describe your day.", 50, 500)), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a new prompt and only maxLength
		var replaced = payload("Опишите свой день.", null, 200);
		var response = setup.client().patch(path, edit(1, replaced));

		// then the response is 200 with the type free-text, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("free-text"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		assertEquals(Set.of("prompt", "maxLength"), keys(response));
		// and the stored payload has exactly the properties prompt and maxLength
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-free-text/R1-S5] Editing a free-text item follows the free-text rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with a free-text item with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload("Describe your day.", 10, 20);
		UUID item = createItem(setup.teacher(), setup.part(), "free-text", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a blank prompt, a minimum above the maximum and an unknown property
		var blank = setup.client().patch(path, editBody(1, "{\"prompt\": \"   \"}"));
		var order = setup.client().patch(path, editBody(1, "{\"prompt\": \"Ok\", \"minLength\": 10, \"maxLength\": 9}"));
		var unknown = setup.client().patch(path, editBody(1, "{\"prompt\": \"Ok\", \"answer\": \"x\"}"));

		// then the responses are 400 VALIDATION_FAILED with the errors of the scenario
		expectValidation(blank, error("payload.prompt", "REQUIRED"));
		expectValidation(order, error("payload.maxLength", "INVALID_FORMAT"));
		expectValidation(unknown, error("payload", "UNKNOWN_FIELD"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-free-text/R1-S6] Another teacher cannot add or edit a free-text item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a free-text item in it
		var setup = teacherWithPart();
		var original = payload("Describe your day.", 10, 20);
		UUID item = createItem(setup.teacher(), setup.part(), "free-text", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a free-text item to teacher A's part and edits teacher A's item with a valid payload
		var add = teacherB.post(setup.items(), freeText(payload("Hi.")));
		var editResponse = teacherB.patch(
				item(setup.course(), setup.lesson(), setup.part(), item),
				edit(1, payload("Hi.")));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}
}
