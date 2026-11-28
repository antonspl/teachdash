package com.teachdash.items.api;

import static com.teachdash.items.support.SortPayloads.column;
import static com.teachdash.items.support.SortPayloads.json;
import static com.teachdash.items.support.SortPayloads.payload;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.TestClient;

/** item-sort R1, REST scenarios. */
class SortItemTest extends ItemApiTest {

	private static Map<String, Object> sort(Object payload) {
		return body("sort", payload);
	}

	private static Map<String, Object> edit(Object version, Object payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	@Test
	@DisplayName("[item-sort/R1-S1] Add an English sort item, it is stored and listed exactly")
	void addAnEnglishSortItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type sort with two columns and then lists the items of the part
		var sent = payload(
				column("Animals", "cat", "dog", "horse"),
				column("Fruits", "apple", "pear", "plum"));
		var response = setup.client().post(setup.items(), sort(sent));
		var listed = setup.client().get(setup.items());

		// then the add response is 201 with the type sort, position 1, version 1 and exactly the payload that was sent
		response.then().statusCode(201)
				.body("type", equalTo("sort"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload", equalTo(sent));
		// and no column has a property it was not sent with
		for (int i = 0; i < 2; i++) {
			Map<String, Object> column = response.path("payload.columns[" + i + "]");
			assertEquals(2, column.size());
		}
		// and the stored payload and the listed payload equal the sent one
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
		listed.then().statusCode(200).body("[0].payload", equalTo(sent));
	}

	@Test
	@DisplayName("[item-sort/R1-S2] Add a Russian sort item, Cyrillic is kept exactly, \"ё\" and \"е\" stay distinct")
	void addARussianSortItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds an item of the type sort with Cyrillic titles and words
		var sent = payload(
				column("Деревья", "ёлка", "дуб"),
				column("Хвоя", "елка", "Ёж", "Еж"));
		var response = client.post(items(course, lesson, part), sort(sent));

		// then the response is 201 and the payload equals the sent one character by character
		response.then().statusCode(201)
				.body("payload", equalTo(sent))
				.body("payload.columns[1].words", equalTo(List.of("елка", "Ёж", "Еж")));
		// and the stored payload has the same columns
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-sort/R1-S3] Edit a sort item: the payload is replaced")
	void editASortItem() {
		// given a logged-in teacher A with a sort item with two columns and version 1
		var setup = teacherWithPart();
		var original = payload(
				column("Cats", "cat"),
				column("Dogs", "dog"));
		UUID item = createItem(setup.teacher(), setup.part(), "sort", json(original), 1);

		// when teacher A edits it with version 1 and a payload with three other columns
		var replaced = payload(
				column("Pets", "dog", "cat"),
				column("Wild", "wolf", "fox"),
				column("Birds", "owl"));
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item), edit(1, replaced));

		// then the response is 200 with the type sort, the new payload exactly as sent, version 2 and the same position
		response.then().statusCode(200)
				.body("type", equalTo("sort"))
				.body("payload", equalTo(replaced))
				.body("version", equalTo(2))
				.body("position", equalTo(1));
		// and the stored payload has the new three columns
		assertStoredPayload(item, json(replaced));
	}

	@Test
	@DisplayName("[item-sort/R1-S4] Values are stored raw, with no trim, case change or normalisation")
	void valuesAreStoredRaw() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds an item with spaces, different case, and a composed and a decomposed "é"
		String composed = "\u00e9";
		String decomposed = "e\u0301";
		var sent = payload(
				column(" Verbs ", " Go ", "GO", "go"),
				column("Accents", composed, decomposed));
		var response = setup.client().post(setup.items(), sort(sent));

		// then the response is 201 and the titles and the five words are exactly these strings in this order
		response.then().statusCode(201)
				.body("payload.columns.title", equalTo(List.of(" Verbs ", "Accents")))
				.body("payload.columns[0].words", equalTo(List.of(" Go ", "GO", "go")))
				.body("payload.columns[1].words", equalTo(List.of(composed, decomposed)));
		// and the stored payload has the same columns
		assertStoredPayload(UUID.fromString(response.path("id")), json(sent));
	}

	@Test
	@DisplayName("[item-sort/R1-S5] Editing a sort item follows the sort rules and a bad edit changes nothing")
	void aBadEditChangesNothing() {
		// given a logged-in teacher A with a sort item with a valid payload and version 1
		var setup = teacherWithPart();
		var original = payload(
				column("Cats", "cat"),
				column("Dogs", "dog"));
		UUID item = createItem(setup.teacher(), setup.part(), "sort", json(original), 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A edits it with version 1 and a payload with one column, and with a payload with a repeated word
		var oneColumn = setup.client().patch(path, edit(1, payload(column("A", "a"))));
		var repeatedWord = setup.client().patch(path, edit(1, payload(column("A", "a"), column("B", "a"))));

		// then both responses are 400 VALIDATION_FAILED with the TOO_SHORT and INVALID_FORMAT errors
		expectValidation(oneColumn, error("payload.columns", "TOO_SHORT"));
		expectValidation(repeatedWord, error("payload.columns[1].words[0]", "INVALID_FORMAT"));
		// and the item keeps its payload and version 1
		assertStoredPayload(item, json(original));
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[item-sort/R1-S6] Another teacher cannot add or edit a sort item")
	void anotherTeacherCannotAddOrEdit() {
		// given a logged-in teacher B, and teacher A with a part and a sort item in it
		var setup = teacherWithPart();
		var original = payload(
				column("Cats", "cat"),
				column("Dogs", "dog"));
		UUID item = createItem(setup.teacher(), setup.part(), "sort", json(original), 1);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B adds a sort item to teacher A's part and edits teacher A's item with a valid payload
		var valid = payload(
				column("A", "a"),
				column("B", "b"));
		var add = teacherB.post(setup.items(), sort(valid));
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
