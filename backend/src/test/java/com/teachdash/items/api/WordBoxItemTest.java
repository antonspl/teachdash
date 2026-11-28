package com.teachdash.items.api;

import static com.teachdash.items.support.WordBoxPayloads.gap;
import static com.teachdash.items.support.WordBoxPayloads.json;
import static com.teachdash.items.support.WordBoxPayloads.payload;
import static com.teachdash.items.support.WordBoxPayloads.payloadWith;
import static com.teachdash.items.support.WordBoxPayloads.segments;
import static com.teachdash.items.support.WordBoxPayloads.text;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

import io.restassured.response.Response;

/** item-gap-fill-word-box R1, REST scenarios. */
class WordBoxItemTest extends ItemApiTest {

	private static Map<String, Object> wordBox(Object payload) {
		return body("gap-fill-word-box", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S1] Add an English word-box item, it is stored and listed exactly")
	void addAnEnglishWordBoxItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type gap-fill-word-box with two gaps and two extra words and then lists the items of the part
		var sent = payloadWith(
				segments(text("She "), gap("is"), text(" a teacher and they "), gap("are"), text(" doctors.")),
				"was", "be");
		var response = setup.client().post(setup.items(), wordBox(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type gap-fill-word-box, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("gap-fill-word-box"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and no segment has a property it was not sent with
		for (int i = 0; i < 5; i++) {
			Map<String, Object> segment = response.path("payload.segments[" + i + "]");
			assertEquals(2, segment.size());
			assertFalse(segment.containsValue(null));
		}
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S2] Add a Russian item without extra words, Cyrillic is kept exactly, \"ё\" and \"е\" stay distinct")
	void addARussianWordBoxItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds three items with Cyrillic texts: no extraWords, extraWords null and extraWords with "елка" and "Ёлка"
		var segments = segments(text("  Выберите слово: «Ёж» и "), gap("ёлка"), text(".  "));
		var withoutExtras = payload(segments);
		var withNull = new LinkedHashMap<>(withoutExtras);
		withNull.put("extraWords", null);
		var withExtras = payloadWith(segments, "елка", "Ёлка");
		String path = items(course, lesson, part);
		var first = client.post(path, wordBox(withoutExtras));
		var second = client.post(path, wordBox(withNull));
		var third = client.post(path, wordBox(withExtras));

		// then the three responses are 201 and each payload equals the sent one character by character
		first.then().statusCode(201).body("payload", equalTo(withoutExtras));
		second.then().statusCode(201).body("payload", equalTo(withoutExtras));
		third.then().statusCode(201).body("payload", equalTo(withExtras));
		// and the first two payloads have no extraWords property at all
		first.then().body("payload", not(hasKey("extraWords")));
		second.then().body("payload", not(hasKey("extraWords")));
		// and the stored payloads equal the responses
		assertStoredPayload(UUID.fromString(first.path("id")), json(withoutExtras));
		assertStoredPayload(UUID.fromString(second.path("id")), json(withoutExtras));
		assertStoredPayload(UUID.fromString(third.path("id")), json(withExtras));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S3] Words are stored raw, and the same answer may fill two gaps")
	void wordsAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with the answers " go ", " go ", "GO" and the extra words "go", composed "é" and decomposed "é"
		String composed = "é";
		String decomposed = "é";
		var sent = payloadWith(
				segments(gap(" go "), text(" and "), gap(" go "), text(", "), gap("GO")),
				"go", composed, decomposed);
		var response = setup.client().post(setup.items(), wordBox(sent));

		// then the response is 201 and the answers and the extra words are exactly as sent, in this order
		response.then().statusCode(201)
				.body("payload.segments[0].answer", equalTo(" go "))
				.body("payload.segments[2].answer", equalTo(" go "))
				.body("payload.segments[4].answer", equalTo("GO"))
				.body("payload.extraWords", equalTo(List.of("go", composed, decomposed)));
		// and the stored payload equals the response
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S4] A gap may be first, last, next to another gap or the only segment, and a whitespace-only text may separate gaps")
	void aGapMayBeFirstLastAdjacentOrAlone() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds six items with gaps in different places, each without extraWords
		List<Map<String, Object>> sent = List.of(
				payload(gap("a"), text(" b "), gap("c")),
				payload(gap("a"), gap("b"), text("c")),
				payload(text("a"), gap("b"), gap("c")),
				payload(gap("a")),
				payload(text("I "), gap("go"), text(" "), gap("walk"), text(" to school.")),
				payload(gap("go"), text("\n"), gap("walk"), text("  \n\t")));
		List<Response> responses = sent.stream()
				.map(payload -> setup.client().post(setup.items(), wordBox(payload)))
				.toList();

		// then all six responses are 201 with the segments exactly as sent, in the positions 1 to 6
		for (int i = 0; i < sent.size(); i++) {
			responses.get(i).then().statusCode(201)
					.body("payload", equalTo(sent.get(i)))
					.body("position", equalTo(i + 1));
		}
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S5] Edit a word-box item: the payload is replaced, the segments and the extra words together")
	void editAWordBoxItem() {
		// given a logged-in teacher A with a word-box item with extraWords ["are"] and version 1
		var setup = teacherWithPart();
		var original = payloadWith(segments(text("She "), gap("is"), text(" a teacher.")), "are");
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill-word-box", json(original), 1);

		// when teacher A edits it with version 1, new segments and no extraWords
		var replaced = payload(gap("She"), text(" "), gap("is"), text(" a teacher."));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type gap-fill-word-box, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("gap-fill-word-box"))
				.body("payload", equalTo(replaced))
				.body("payload", not(hasKey("extraWords")))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the new segments and no extraWords
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S6] Editing a word-box item follows the word-box rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with a word-box item with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(text("I "), gap("go"));
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill-word-box", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload without a gap, and with an extra word equal to the answer
		var noGap = setup.client().patch(path, edit(1, payload(text("No gap here."))));
		var equalToAnswer = setup.client().patch(path, edit(1, payloadWith(segments(text("I "), gap("go")), "go")));

		// then both responses are 400 VALIDATION_FAILED with errors on payload.segments (REQUIRED) and payload.extraWords[0] (INVALID_FORMAT)
		expectValidation(noGap, error("payload.segments", "REQUIRED"));
		expectValidation(equalToAnswer, error("payload.extraWords[0]", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-gap-fill-word-box/R1-S7] Another teacher cannot add or edit a word-box item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a word-box item in it
		var setup = teacherWithPart();
		var original = payload(text("I "), gap("go"));
		UUID item = createItem(setup.teacher(), setup.part(), "gap-fill-word-box", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a word-box item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(text("Hi "), gap("go"));
		var add = teacherB.post(setup.items(), wordBox(valid));
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
