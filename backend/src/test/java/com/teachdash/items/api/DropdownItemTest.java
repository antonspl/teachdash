package com.teachdash.items.api;

import static com.teachdash.items.support.DropdownPayloads.gap;
import static com.teachdash.items.support.DropdownPayloads.json;
import static com.teachdash.items.support.DropdownPayloads.payload;
import static com.teachdash.items.support.DropdownPayloads.text;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

import io.restassured.response.Response;

/** item-dropdown R1. */
class DropdownItemTest extends ItemApiTest {

	private static Map<String, Object> dropdown(Object payload) {
		return body("dropdown", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-dropdown/R1-S1] Add an English dropdown item, it is stored and listed exactly")
	void addAnEnglishDropdownItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type dropdown and then lists the items of the part
		var sent = payload(text("She "), gap(1, "is", "was", "are"), text(" a teacher yesterday."));
		var response = setup.client().post(setup.items(), dropdown(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type dropdown, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("dropdown"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-dropdown/R1-S2] Add a Russian dropdown item, Cyrillic is kept exactly, \"ё\" and \"е\" stay distinct")
	void addARussianDropdownItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type dropdown with Cyrillic texts and three options that differ by "ё", "е" and case
		var sent = payload(
				text("  Выберите слово: «Ёж» и "),
				gap(2, "ёлка", "елка", "Ёлка"),
				text(".  "));
		var response = client.post(items(course, lesson, part), dropdown(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201).body("payload", equalTo(sent));
		// and the stored payload has the same segments
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-dropdown/R1-S3] Options are stored raw, with no trim, case change or normalisation")
	void optionsAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type dropdown with one gap whose options differ by spaces, case and Unicode form
		List<String> options = List.of(" go ", "GO", "go", "é", "é");
		var sent = payload(gap(2, options.toArray(String[]::new)));
		var response = setup.client().post(setup.items(), dropdown(sent));

		// then the response is 201 and the options are exactly these five strings in this order with correct 2
		response.then().statusCode(201)
				.body("payload.segments[0].options", equalTo(options))
				.body("payload.segments[0].correct", equalTo(2));
		// and the stored payload has the same five options and correct
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-dropdown/R1-S4] A gap may be first, last, next to another gap or the only segment")
	void aGapMayBeFirstLastAdjacentOrAlone() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type dropdown with the segments [gap, text, gap], [gap, gap, text], [text, gap, gap] and [gap]
		List<Map<String, Object>> sent = List.of(
				payload(gap(0, "go", "went"), text(" and "), gap(1, "walk", "ran")),
				payload(gap(0, "go", "went"), gap(1, "walk", "ran"), text(" to school.")),
				payload(text("I "), gap(0, "go", "went"), gap(1, "walk", "ran")),
				payload(gap(0, "go", "went")));
		List<Response> responses = sent.stream()
				.map(payload -> setup.client().post(setup.items(), dropdown(payload)))
				.toList();

		// then all four responses are 201 with the segments exactly as sent, in the positions 1 to 4
		for (int i = 0; i < sent.size(); i++) {
			responses.get(i).then().statusCode(201)
					.body("payload", equalTo(sent.get(i)))
					.body("position", equalTo(i + 1));
		}
	}

	@Test
	@DisplayName("[item-dropdown/R1-S5] Edit a dropdown item: the payload is replaced, the options and the correct index are replaced together")
	void editADropdownItem() {
		// given a logged-in teacher A with an item of the type dropdown with two gaps and version 1
		var setup = teacherWithPart();
		var original = payload(
				text("She "), gap(0, "is", "are"), text(" a teacher and he "), gap(0, "is", "are"), text(" a doctor."));
		UUID item = createItem(setup.teacher(), setup.part(), "dropdown", json(original), 1);

		// when teacher A edits it with version 1 and a payload whose first gap is a different one and whose second gap has three options
		var replaced = payload(
				gap(0, "She", "He"), text(" is a teacher and he "), gap(1, "is", "'s", "are"), text(" a doctor."));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type dropdown, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("dropdown"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the new segments, so the gap at index 1 has three options and correct 1
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-dropdown/R1-S6] Editing a dropdown item follows the dropdown rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with an item of the type dropdown with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(text("I "), gap(0, "go", "went"));
		UUID item = createItem(setup.teacher(), setup.part(), "dropdown", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload without a gap and a payload whose correct is out of range
		var noGap = setup.client().patch(path, edit(1, payload(text("No gap here."))));
		var outOfRange = setup.client().patch(path, edit(1, payload(gap(2, "a", "b"))));

		// then both responses are 400 VALIDATION_FAILED with errors on payload.segments and payload.segments[0].correct
		expectValidation(noGap, error("payload.segments", "REQUIRED"));
		expectValidation(outOfRange, error("payload.segments[0].correct", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-dropdown/R1-S7] Another teacher cannot add or edit a dropdown item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a dropdown item in it
		var setup = teacherWithPart();
		var original = payload(text("I "), gap(0, "go", "went"));
		UUID item = createItem(setup.teacher(), setup.part(), "dropdown", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a dropdown item to teacher A's part and edits teacher A's item with a valid payload
		var add = teacherB.post(setup.items(), dropdown(payload(text("Hi "), gap(0, "go", "went"))));
		var editResponse = teacherB.patch(
				item(setup.course(), setup.lesson(), setup.part(), item),
				edit(1, payload(text("Hi "), gap(0, "go", "went"))));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}
}
