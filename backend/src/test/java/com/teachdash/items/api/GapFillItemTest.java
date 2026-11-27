package com.teachdash.items.api;

import static com.teachdash.items.support.GapFillPayloads.gap;
import static com.teachdash.items.support.GapFillPayloads.json;
import static com.teachdash.items.support.GapFillPayloads.payload;
import static com.teachdash.items.support.GapFillPayloads.text;
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

/** item-gap-fill R1. */
class GapFillItemTest extends ItemApiTest {

	private static Map<String, Object> gapFill(Object payload) {
		return body("gap-fill", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S1] Add an English gap-fill item, it is stored and listed exactly")
	void addAnEnglishGapFillItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type gap-fill and then lists the items of the part
		var sent = payload(text("I "), gap("go", "walk"), text(" to school every day."));
		var response = setup.client().post(setup.items(), gapFill(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type gap-fill, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("gap-fill"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S2] Add a Russian gap-fill item, Cyrillic is kept exactly, \"ё\" and \"е\" stay distinct")
	void addARussianGapFillItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type gap-fill with Cyrillic texts and three answers that differ by "ё", "е" and case
		var sent = payload(
				text("  Вставьте слово: «Ёж» и "),
				gap("ёлка", "елка", "Ёлка"),
				text(".  "));
		var response = client.post(items(course, lesson, part), gapFill(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201).body("payload", equalTo(sent));
		// and the stored payload has the same segments
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S3] Answers are stored raw, with no trim, case change or normalisation")
	void answersAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type gap-fill with one gap whose answers differ by spaces, case and Unicode form
		List<String> answers = List.of(" go ", "GO", "go", "é", "é");
		var sent = payload(gap(answers.toArray(String[]::new)));
		var response = setup.client().post(setup.items(), gapFill(sent));

		// then the response is 201 and the answers are exactly these five strings in this order
		response.then().statusCode(201).body("payload.segments[0].answers", equalTo(answers));
		// and the stored payload has the same five answers
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S4] A gap may be first, last, next to another gap or the only segment")
	void aGapMayBeFirstLastAdjacentOrAlone() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds items of the type gap-fill with the segments [gap, text, gap], [gap, gap, text], [text, gap, gap] and [gap]
		List<Map<String, Object>> sent = List.of(
				payload(gap("go"), text(" and "), gap("walk")),
				payload(gap("go"), gap("walk"), text(" to school.")),
				payload(text("I "), gap("go"), gap("walk")),
				payload(gap("go")));
		List<Response> responses = sent.stream()
				.map(payload -> setup.client().post(setup.items(), gapFill(payload)))
				.toList();

		// then all four responses are 201 with the segments exactly as sent, in the positions 1 to 4
		for (int i = 0; i < sent.size(); i++) {
			responses.get(i).then().statusCode(201)
					.body("payload", equalTo(sent.get(i)))
					.body("position", equalTo(i + 1));
		}
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S5] Edit a gap-fill item: the payload is replaced, the gaps are numbered anew")
	void editAGapFillItem() {
		// given a logged-in teacher A with an item of the type gap-fill with two gaps and version 1
		var setup = teacherWithPart();
		var original = payload(text("She "), gap("is"), text(" a teacher and he "), gap("is"), text(" a doctor."));
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill", json(original), 1);

		// when teacher A edits it with version 1 and a payload whose first gap is a different one and whose second gap has two answers
		var replaced = payload(gap("She"), text(" is a teacher and he "), gap("is", "'s"), text(" a doctor."));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type gap-fill, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("gap-fill"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the new segments, so the gap at index 1 has the answers "is" and "'s"
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S6] Editing a gap-fill item follows the gap-fill rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with an item of the type gap-fill with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(text("I "), gap("go"));
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload without a gap and a payload with a gap without answers
		var noGap = setup.client().patch(path, edit(1, payload(text("No gap here."))));
		var noAnswers = setup.client().patch(path, edit(1, payload(gap())));

		// then both responses are 400 VALIDATION_FAILED with errors on payload.segments and payload.segments[0].answers
		expectValidation(noGap, error("payload.segments", "REQUIRED"));
		expectValidation(noAnswers, error("payload.segments[0].answers", "REQUIRED"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-gap-fill/R1-S7] Another teacher cannot add or edit a gap-fill item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a gap-fill item in it
		var setup = teacherWithPart();
		var original = payload(text("I "), gap("go"));
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a gap-fill item to teacher A's part and edits teacher A's item with a valid payload
		var add = teacherB.post(setup.items(), gapFill(payload(text("Hi "), gap("go"))));
		var editResponse = teacherB.patch(
				item(setup.course(), setup.lesson(), setup.part(), item),
				edit(1, payload(text("Hi "), gap("go"))));

		// then both responses are 404
		expectNotFound(add);
		expectNotFound(editResponse);
		// and teacher A's part still has exactly the one item with its payload and version unchanged
		assertEquals(1, countItems(setup.part()));
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}
}
