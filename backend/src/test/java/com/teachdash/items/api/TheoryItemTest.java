package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.TestClient;

/** items R5. */
class TheoryItemTest extends ItemApiTest {

	private static Map<String, Object> theory(String text) {
		return body("theory", Map.of("text", text));
	}

	@Test
	@DisplayName("[items/R5-S1] Add an English theory item, Markdown and a formula are kept exactly")
	void addAnEnglishTheoryItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with a part
		var setup = teacherWithPart();

		// when teacher A adds an item of the type theory with Markdown, formulas and a trailing newline
		String text = "## Present Simple\n\nUse **do/does** with *I, you, we*.\n\n- I work\n- She works\n\n"
				+ "The formula is $x^2 + y^2 = z^2$ and $$E = mc^2$$.\n";
		var response = setup.client().post(setup.items(), theory(text));

		// then the response is 201 with the type theory, position 1, version 1 and a payload with exactly the property text, equal to the sent text
		response.then().statusCode(201)
				.body("type", equalTo("theory"))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("payload.text", equalTo(text));
		assertEquals(Set.of("text"), response.<Map<String, Object>>path("payload").keySet());
		// and the stored payload and the listed payload have the same text
		assertEquals(text, storedText(setup.part(), 1));
		setup.client().get(setup.items()).then().statusCode(200).body("[0].payload.text", equalTo(text));
	}

	@Test
	@DisplayName("[items/R5-S2] Add a Russian theory item, Cyrillic Markdown is kept exactly")
	void addARussianTheoryItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		TestClient client = loggedInAs("a@example.com");

		// when teacher A adds a theory item with spaces at both ends
		String text = "  ## Падежи\n\nСклоняйте слово **«мышь»**: `ёж` & ёлка.\n\nФормула: $a^2 + b^2 = c^2$.  ";
		var response = client.post(items(course, lesson, part), theory(text));

		// then the response is 201 with a text exactly equal to the sent text
		response.then().statusCode(201).body("payload.text", equalTo(text));
		// and the stored payload has the same text
		assertEquals(text, storedText(part, 1));
	}

	@Test
	@DisplayName("[items/R5-S3] Raw HTML is stored as text, not removed")
	void rawHtmlIsStoredAsText() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds a theory item with raw HTML
		String text = "<script>alert(1)</script><b>Hi</b> <img src=x onerror=alert(1)>";
		var response = setup.client().post(setup.items(), theory(text));

		// then the response is 201 with a text exactly equal to the sent text
		response.then().statusCode(201).body("payload.text", equalTo(text));
		// and the stored payload has the same text
		assertEquals(text, storedText(setup.part(), 1));
		// and the list of the part returns it unchanged
		setup.client().get(setup.items()).then().statusCode(200).body("[0].payload.text", equalTo(text));
	}

	@Test
	@DisplayName("[items/R5-S4] A theory text of 20000 characters is accepted, 20001 is too long")
	void aTheoryTextOf20000CharactersIsAccepted() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds theory items with 20000 "Я", 20001 "Я", 20000 "a" and 10001 emoji
		String exact = "Я".repeat(20000);
		var first = setup.client().post(setup.items(), theory(exact));
		var tooLong = setup.client().post(setup.items(), theory("Я".repeat(20001)));
		var english = setup.client().post(setup.items(), theory("a".repeat(20000)));
		var emoji = setup.client().post(setup.items(), theory("😀".repeat(10001)));

		// then the first response is 201 with the text as sent and position 1
		first.then().statusCode(201).body("payload.text", equalTo(exact)).body("position", equalTo(1));
		// and the second and the fourth are 400 VALIDATION_FAILED with payload.text TOO_LONG and no rejectedValue
		for (Response response : List.of(tooLong, emoji)) {
			expectValidation(response, error("payload.text", "TOO_LONG"));
			response.then().body(not(containsString("rejectedValue")));
		}
		// and the third is 201 with position 2
		english.then().statusCode(201).body("position", equalTo(2));
		// and the part has exactly 2 items
		assertEquals(2, countItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R5-S5] A blank, wrongly typed or unknown payload is rejected")
	void aBlankWronglyTypedOrUnknownPayloadIsRejected() {
		// given a logged-in teacher A with a part
		var setup = teacherWithPart();

		// when teacher A adds theory items with the nine payloads of the scenario
		List<String> blank = List.of(
				"{}",
				"{\"text\": null}",
				"{\"text\": \"\"}",
				"{\"text\": \"   \"}",
				"{\"text\": \"\\n\\t  \"}");
		List<String> wrongType = List.of("{\"text\": 5}", "{\"text\": [\"Привет\"]}");
		List<Response> blankResponses = blank.stream()
				.map(payload -> setup.client().post(setup.items(), rawBody("theory", payload)))
				.toList();
		List<Response> wrongTypeResponses = wrongType.stream()
				.map(payload -> setup.client().post(setup.items(), rawBody("theory", payload)))
				.toList();
		var unknown = setup.client().post(setup.items(), rawBody("theory", "{\"text\": \"ok\", \"title\": \"Intro\"}"));
		var noBreakSpace = setup.client().post(setup.items(), rawBody("theory", "{\"text\": \"\\u00A0\"}"));

		// then the first five responses are 400 VALIDATION_FAILED with payload.text REQUIRED
		for (Response response : blankResponses) {
			expectValidation(response, error("payload.text", "REQUIRED"));
		}
		// and the next two have payload.text INVALID_TYPE
		for (Response response : wrongTypeResponses) {
			expectValidation(response, error("payload.text", "INVALID_TYPE"));
		}
		// and the next has payload UNKNOWN_FIELD
		expectValidation(unknown, error("payload", "UNKNOWN_FIELD"));
		// and the last is 201 with the text exactly as sent
		noBreakSpace.then().statusCode(201).body("payload.text", equalTo("\u00A0"));
		// and no body of the rejected ones contains "Привет" or "Intro"
		List<Response> rejected = new java.util.ArrayList<>(blankResponses);
		rejected.addAll(wrongTypeResponses);
		rejected.add(unknown);
		for (Response response : rejected) {
			response.then().body(not(containsString("Привет"))).body(not(containsString("Intro")));
		}
		// and the part has exactly one item, the last
		assertEquals(1, countItems(setup.part()));
		assertEquals("\u00A0", storedText(setup.part(), 1));
	}

	@Test
	@DisplayName("[items/R5-S6] Editing a theory item follows the theory rules")
	void editingATheoryItemFollowsTheTheoryRules() {
		// given a logged-in teacher A with a theory item with the payload {"text": "Old"} and version 1, created at a time T, and the clock advanced by one hour
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "theory", "{\"text\": \"Old\"}", 1);
		Instant created = Instant.now(clock);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits it with version 1 and a blank text, a text of 20001 letters and a payload with the property hint
		var blank = setup.client().patch(path, editBody(1, "{\"text\": \"   \"}"));
		var tooLong = setup.client().patch(path, editBody(1, "{\"text\": \"" + "Я".repeat(20001) + "\"}"));
		var unknown = setup.client().patch(path, editBody(1, "{\"text\": \"ok\", \"hint\": \"x\"}"));

		// then the responses are 400 VALIDATION_FAILED with payload.text REQUIRED, payload.text TOO_LONG and payload UNKNOWN_FIELD
		expectValidation(blank, error("payload.text", "REQUIRED"));
		expectValidation(tooLong, error("payload.text", "TOO_LONG"));
		expectValidation(unknown, error("payload", "UNKNOWN_FIELD"));
		// and the item is unchanged after them
		assertStoredPayload(item, "{\"text\": \"Old\"}");
		assertEquals(1, readItem(item).version());
		assertEquals(created, readItem(item).updatedAt());

		// when teacher A then edits it with the text "<b>Привет</b> $x^2$"
		String text = "<b>Привет</b> $x^2$";
		var response = setup.client().patch(path, Map.of("version", 1, "payload", Map.of("text", text)));

		// then the response is 200 with the type theory, the text as sent, version 2 and updatedAt T plus one hour
		response.then().statusCode(200)
				.body("type", equalTo("theory"))
				.body("payload.text", equalTo(text))
				.body("version", equalTo(2));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
	}
}
