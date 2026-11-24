package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** items R4. */
class EditItemTest extends ItemApiTest {

	private static final Set<String> ITEM_PROPERTIES =
			Set.of("id", "type", "title", "payload", "position", "version", "createdAt", "updatedAt");

	@Test
	@DisplayName("[items/R4-S1] Edit an English item: the payload is replaced, the type and position are kept")
	void editAnEnglishItem() {
		// given a logged-in teacher A with a part with the items "One" and "Two" and a third test-note item with a hint, all created at a time T with version 1, a teacher B, and the clock advanced by one hour
		var setup = teacherWithPart();
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID one = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"One\"}", 1);
		UUID two = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Two\"}", 2);
		UUID third = createItem(setup.teacher(), setup.part(), "test-note",
				"{\"text\": \"Read the story aloud.\", \"hint\": \"Page 3\"}", 3);
		testClock().advance(Duration.ofHours(1));
		String path = item(setup.course(), setup.lesson(), setup.part(), third);

		// when teacher A edits the third item with extra body fields type, position and teacherId, then advances the clock by one more hour and edits it again with the same payload
		Map<String, Object> request = new HashMap<>();
		request.put("version", 1);
		request.put("payload", Map.of("text", "Read it twice."));
		request.put("type", "poll");
		request.put("position", 1);
		request.put("teacherId", teacherB.toString());
		var first = setup.client().patch(path, request);

		// then the first response is 200 with the same id, the type, the replaced payload, position 3, version 2, createdAt T and updatedAt T plus one hour
		first.then().statusCode(200)
				.body("id", equalTo(third.toString()))
				.body("type", equalTo("test-note"))
				.body("title", nullValue())
				.body("payload", equalTo(Map.of("text", "Read it twice.")))
				.body("position", equalTo(3))
				.body("version", equalTo(2));
		assertEquals(created, Instant.parse(first.path("createdAt")));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		// and exactly the item properties are returned
		assertEquals(ITEM_PROPERTIES, first.<Map<String, Object>>path("$").keySet());
		// and the stored item has the same type, payload, version and times
		var stored = readItem(third);
		assertEquals("test-note", stored.type());
		assertStoredPayload(third, "{\"text\": \"Read it twice.\"}");
		assertEquals(2, stored.version());
		assertEquals(created, stored.createdAt());
		assertEquals(created.plus(Duration.ofHours(1)), stored.updatedAt());
		// and listing the part shows the new payload at position 3 with version 2
		var listed = setup.client().get(setup.items());
		listed.then().statusCode(200)
				.body("[2].payload", equalTo(Map.of("text", "Read it twice.")))
				.body("[2].position", equalTo(3))
				.body("[2].version", equalTo(2));

		// when the clock advances and the same payload is sent with version 2
		testClock().advance(Duration.ofHours(1));
		var second = setup.client().patch(path, Map.of("version", 2, "payload", Map.of("text", "Read it twice.")));

		// then the second response is 200 with version 3 and updatedAt T plus two hours
		second.then().statusCode(200)
				.body("payload", equalTo(Map.of("text", "Read it twice.")))
				.body("version", equalTo(3));
		assertEquals(created.plus(Duration.ofHours(2)), Instant.parse(second.path("updatedAt")));
		assertEquals(3, readItem(third).version());
		setup.client().get(setup.items()).then().statusCode(200).body("[2].version", equalTo(3));
		// and the items "One" and "Two" keep their positions, version 1 and updatedAt T
		for (UUID id : List.of(one, two)) {
			assertEquals(1, readItem(id).version());
			assertEquals(created, readItem(id).updatedAt());
		}
		assertEquals(1, readItem(one).position());
		assertEquals(2, readItem(two).position());
		// and the updatedAt of the part, the lesson and the course is unchanged
		assertEquals(created, readPartUpdatedAt(setup.part()));
		assertEquals(created, readLessonUpdatedAt(setup.lesson()));
		assertEquals(created, readCourse(setup.course()).updatedAt());
	}

	@Test
	@DisplayName("[items/R4-S2] Edit a Russian item: Cyrillic text is kept exactly, the version grows")
	void editARussianItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with a part with a test-note item, created at a time T with version 1, and the clock advanced by one hour
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		UUID item = createItem(teacher, part, "test-note", "{\"text\": \"Просклоняйте слово «мышь».\"}", 1);
		TestClient client = loggedInAs("a@example.com");
		Instant created = Instant.now(clock);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits it with a text with spaces at both ends and a hint
		String text = "  Ёж & ёлка: именительный, родительный?  ";
		var response = client.patch(item(course, lesson, part, item),
				Map.of("version", 1, "payload", Map.of("text", text, "hint", "Ответ: «ёж»")));

		// then the response is 200 with the text exactly as sent, the hint, version 2 and updatedAt T plus one hour
		response.then().statusCode(200)
				.body("payload.text", equalTo(text))
				.body("payload.hint", equalTo("Ответ: «ёж»"))
				.body("version", equalTo(2));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and the stored payload has the same values
		assertEquals(text, storedText(part, 1));
		assertStoredPayload(item, "{\"text\": \"" + text + "\", \"hint\": \"Ответ: «ёж»\"}");
	}

	@Test
	@DisplayName("[items/R4-S3] The version and the payload must be present and of the right JSON type")
	void theVersionAndThePayloadMustBePresentAndOfTheRightJsonType() {
		// given a logged-in teacher A with a test-note item with the payload {"text": "Hi"} and version 1
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		TestClient client = setup.client();

		// when teacher A edits it with the fourteen bodies of the scenario
		String newPayload = "\"payload\": {\"text\": \"New\"}";
		List<Response> versionRequired = List.of(
				client.patch(path, "{" + newPayload + "}"),
				client.patch(path, "{\"version\": null, " + newPayload + "}"));
		List<Response> versionInvalid = List.of(
				client.patch(path, "{\"version\": \"1\", " + newPayload + "}"),
				client.patch(path, "{\"version\": 1.0, " + newPayload + "}"),
				client.patch(path, "{\"version\": true, " + newPayload + "}"),
				client.patch(path, "{\"version\": 99999999999999999999, " + newPayload + "}"));
		List<Response> payloadRequired = List.of(
				client.patch(path, "{\"version\": 1}"),
				client.patch(path, "{\"version\": 1, \"payload\": null}"));
		List<Response> payloadInvalid = List.of(
				client.patch(path, "{\"version\": 1, \"payload\": []}"),
				client.patch(path, "{\"version\": 1, \"payload\": \"New\"}"));
		var empty = client.patch(path, "{}");
		var bindingStops = client.patch(path, "{\"version\": \"1\", \"payload\": []}");
		var truncated = client.patch(path, "{\"version\": 1, \"payload\":");
		var noBody = client.patch(path);

		// then the first two responses are 400 VALIDATION_FAILED with version REQUIRED
		for (Response response : versionRequired) {
			expectValidation(response, error("version", "REQUIRED"));
		}
		// and the next four have version INVALID_TYPE
		for (Response response : versionInvalid) {
			expectValidation(response, error("version", "INVALID_TYPE"));
		}
		// and the seventh and the eighth have payload REQUIRED
		for (Response response : payloadRequired) {
			expectValidation(response, error("payload", "REQUIRED"));
		}
		// and the ninth and the tenth have payload INVALID_TYPE
		for (Response response : payloadInvalid) {
			expectValidation(response, error("payload", "INVALID_TYPE"));
		}
		// and the eleventh has payload and version REQUIRED, sorted by field
		expectValidation(empty, error("payload", "REQUIRED"), error("version", "REQUIRED"));
		// and the twelfth has version INVALID_TYPE only
		expectValidation(bindingStops, error("version", "INVALID_TYPE"));
		// and the last two are 400 MALFORMED_REQUEST with no errors property
		for (Response response : List.of(truncated, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and no body contains "New" as a value
		List<Response> all = new ArrayList<>();
		all.addAll(versionRequired);
		all.addAll(versionInvalid);
		all.addAll(payloadRequired);
		all.addAll(payloadInvalid);
		all.addAll(List.of(empty, bindingStops, truncated, noBody));
		for (Response response : all) {
			response.then().body(not(containsString("\"New\"")));
		}
		// and the item is unchanged with version 1
		assertEquals(1, readItem(item).version());
		assertStoredPayload(item, "{\"text\": \"Hi\"}");
	}

	@Test
	@DisplayName("[items/R4-S4] The payload is validated by the stored type, and a bad payload changes nothing")
	void thePayloadIsValidatedByTheStoredType() {
		// given a logged-in teacher A with a test-note item with the payload {"text": "Hi"}, version 1 and updatedAt T
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		Instant created = Instant.now(clock);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits it with version 1 and the five bad payloads
		List<Response> responses = List.of(
				setup.client().patch(path, editBody(1, "{}")),
				setup.client().patch(path, editBody(1, "{\"text\": 42}")),
				setup.client().patch(path, editBody(1, "{\"text\": \"" + "Я".repeat(1001) + "\"}")),
				setup.client().patch(path, editBody(1, "{\"text\": \"ok\", \"extra\": {\"deep\": [1]}}")),
				setup.client().patch(path, editBody(1, "{\"text\": \"a\\u0000b\"}")));

		// then the responses are 400 VALIDATION_FAILED with the errors of the scenario
		expectValidation(responses.get(0), error("payload.text", "REQUIRED"));
		expectValidation(responses.get(1), error("payload.text", "INVALID_TYPE"));
		expectValidation(responses.get(2), error("payload.text", "TOO_LONG"));
		expectValidation(responses.get(3), error("payload", "UNKNOWN_FIELD"));
		expectValidation(responses.get(4), error("payload", "INVALID_FORMAT"));
		// and no body contains "extra", "deep" or a rejectedValue
		for (Response response : responses) {
			response.then()
					.body(not(containsString("extra")))
					.body(not(containsString("deep")))
					.body(not(containsString("rejectedValue")));
		}
		// and the item still has the payload, version 1 and updatedAt T
		var stored = readItem(item);
		assertStoredPayload(item, "{\"text\": \"Hi\"}");
		assertEquals(1, stored.version());
		assertEquals(created, stored.updatedAt());
	}

	@Test
	@DisplayName("[items/R4-S5] A stale version is a conflict and changes nothing")
	void aStaleVersionIsAConflictAndChangesNothing() {
		// given a logged-in teacher A with a test-note item with the payload {"text": "Привет"}, version 3 and updatedAt T, and the clock advanced by one hour
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Привет\"}", 1, 3);
		Instant created = Instant.now(clock);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits it with the text "Мир" and version 2, then 4, then 0, then -1, and then 3
		List<Response> stale = new ArrayList<>();
		for (int version : List.of(2, 4, 0, -1)) {
			stale.add(setup.client().patch(path, Map.of("version", version, "payload", Map.of("text", "Мир"))));
		}
		var storedAfterConflicts = readItem(item);
		var last = setup.client().patch(path, Map.of("version", 3, "payload", Map.of("text", "Мир")));

		// then the first four responses are 409 VERSION_CONFLICT with no errors property and neither text
		for (Response response : stale) {
			response.then().statusCode(409)
					.body("code", equalTo("VERSION_CONFLICT"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Привет")))
					.body(not(containsString("Мир")));
		}
		// and after them the item still has the payload, version 3 and updatedAt T
		assertEquals(3, storedAfterConflicts.version());
		assertEquals(created, storedAfterConflicts.updatedAt());
		assertTrue(storedAfterConflicts.payload().contains("Привет"));
		// and the last response is 200 with the text "Мир", version 4 and updatedAt T plus one hour
		last.then().statusCode(200)
				.body("payload.text", equalTo("Мир"))
				.body("version", equalTo(4));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(last.path("updatedAt")));
	}

	@Test
	@DisplayName("[items/R4-S6] Another teacher's, unknown, malformed and wrong-parent ids are not found")
	void anotherTeachersUnknownMalformedAndWrongParentIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", each with one item, and a course of teacher B with a lesson, a part and the item "Секрет"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID quarters = createPart(teacherA, fractions, "Quarters", 2);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID halvesItem = createItem(teacherA, halves, "test-note", "{\"text\": \"Half\"}", 1);
		UUID quartersItem = createItem(teacherA, quarters, "test-note", "{\"text\": \"Quarter\"}", 1);
		UUID tenthsItem = createItem(teacherA, tenths, "test-note", "{\"text\": \"Tenth\"}", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID foreignItem = createItem(teacherB, foreignPart, "test-note", "{\"text\": \"Секрет\"}", 1);
		TestClient teacher = loggedInAs("a@example.com");
		Instant created = Instant.now(clock);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits with version 1 and a new text each of the fifteen wrong combinations of ids
		List<String> paths = List.of(
				item(foreignCourse, foreignLesson, foreignPart, foreignItem),
				item(maths, fractions, halves, foreignItem),
				item(foreignCourse, foreignLesson, foreignPart, halvesItem),
				item(maths, fractions, foreignPart, halvesItem),
				item(maths, fractions, halves, UUID.randomUUID()),
				item(maths, fractions, halves, "not-a-uuid"),
				item(maths, fractions, halves, quartersItem),
				item(maths, fractions, halves, tenthsItem),
				item(maths, fractions, "not-a-uuid", halvesItem),
				item(maths, fractions, UUID.randomUUID(), halvesItem),
				item(maths, "not-a-uuid", halves, halvesItem),
				item(maths, UUID.randomUUID(), halves, halvesItem),
				item(english, fractions, halves, halvesItem),
				item("not-a-uuid", fractions, halves, halvesItem),
				item(UUID.randomUUID(), fractions, halves, halvesItem));
		List<Response> responses = new ArrayList<>();
		for (String path : paths) {
			responses.add(teacher.patch(path, Map.of("version", 1, "payload", Map.of("text", "Changed"))));
		}

		// then all responses are 404 NOT_FOUND with no errors property and no text of teacher B
		for (Response response : responses) {
			expectNotFound(response);
		}
		// and the payload, version and updatedAt of every item are unchanged
		Map<UUID, String> texts = Map.of(
				halvesItem, "Half", quartersItem, "Quarter", tenthsItem, "Tenth", foreignItem, "Секрет");
		texts.forEach((id, text) -> {
			assertStoredPayload(id, "{\"text\": \"" + text + "\"}");
			assertEquals(1, readItem(id).version());
			assertEquals(created, readItem(id).updatedAt());
		});
	}

	@Test
	@DisplayName("[items/R4-S7] Only active teachers edit items")
	void onlyActiveTeachersEditItems() {
		// given an item of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		UUID item = createItem(teacherA, part, "test-note", "{\"text\": \"Hi\"}", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each edit it, and teacher B does so without a CSRF token
		String path = item(course, lesson, part, item);
		Map<String, Object> request = Map.of("version", 1, "payload", Map.of("text", "Changed"));
		var adminResponse = admin.patch(path, request);
		var studentResponse = student.patch(path, request);
		var deactivatedResponse = deactivated.patch(path, request);
		var anonymousResponse = anonymous.patch(path, request);
		var csrfResponse = withoutCsrf.patch(path, request);

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
		// and teacher B's is 403 CSRF_INVALID
		csrfResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and the item still has the payload and version 1
		assertStoredPayload(item, "{\"text\": \"Hi\"}");
		assertEquals(1, readItem(item).version());
	}

	@Test
	@DisplayName("[items/R4-S8] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with an item I, a course of teacher B with a part with the item "Секрет", a logged-in admin, and a logged-in teacher A2 who is then deactivated
		var setup = teacherWithPart();
		UUID itemI = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID foreignItem = createItem(teacherB, foreignPart, "test-note", "{\"text\": \"Секрет\"}", 1);
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();
		String own = item(setup.course(), setup.lesson(), setup.part(), itemI);
		String foreign = item(foreignCourse, foreignLesson, foreignPart, foreignItem);

		// when teacher A sends {} for (1) the item "not-a-uuid", (2) teacher B's item, (3) an unknown item id; then a request with an empty payload for (4) teacher B's item, (5) I with version 9, (6) I with version 9 and a valid payload; and the admin and teacher A2 send {} for the item "not-a-uuid" of the part, lesson and course "not-a-uuid"
		var malformed = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), "not-a-uuid"), "{}");
		var foreignEmpty = setup.client().patch(foreign, "{}");
		var unknownEmpty = setup.client().patch(
				item(setup.course(), setup.lesson(), setup.part(), UUID.randomUUID()), "{}");
		var foreignPayload = setup.client().patch(foreign, editBody(1, "{}"));
		var payloadBeforeVersion = setup.client().patch(own, editBody(9, "{}"));
		var conflict = setup.client().patch(own, editBody(9, "{\"text\": \"New\"}"));
		String notUuid = item("not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid");
		var adminResponse = admin.patch(notUuid, "{}");
		var deactivatedResponse = deactivated.patch(notUuid, "{}");

		// then (1) is 404 NOT_FOUND with no errors property
		expectNotFound(malformed);
		// and (2) and (3) are 400 VALIDATION_FAILED with payload and version REQUIRED, without "Секрет"
		for (Response response : List.of(foreignEmpty, unknownEmpty)) {
			expectValidation(response, error("payload", "REQUIRED"), error("version", "REQUIRED"));
			response.then().body(not(containsString("Секрет")));
		}
		// and (4) is 404 NOT_FOUND
		expectNotFound(foreignPayload);
		// and (5) is 400 with payload.text REQUIRED
		expectValidation(payloadBeforeVersion, error("payload.text", "REQUIRED"));
		// and (6) is 409 VERSION_CONFLICT
		conflict.then().statusCode(409).body("code", equalTo("VERSION_CONFLICT"));
		// and the admin's response is 403 FORBIDDEN and the deactivated teacher's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and I and teacher B's item are unchanged
		assertStoredPayload(itemI, "{\"text\": \"Hi\"}");
		assertEquals(1, readItem(itemI).version());
		assertStoredPayload(foreignItem, "{\"text\": \"Секрет\"}");
		assertEquals(1, readItem(foreignItem).version());
	}

	@Test
	@DisplayName("[items/R4-S9] Parallel edits with the same version: one wins")
	void parallelEditsWithTheSameVersionOneWins() throws Exception {
		// given a logged-in teacher A with a test-note item with the title "Original" and version 1
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Start\"}", 1, "Original");
		String path = item(setup.course(), setup.lesson(), setup.part(), item);

		// when teacher A sends 6 edit requests at the same time, all with version 1, with the texts "Edit 1" to "Edit 6" and the titles "Title 1" to "Title 6"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			TestClient client = setup.client().snapshot();
			String text = "Edit " + i;
			String title = "Title " + i;
			calls.add(() -> client.patch(path,
					Map.of("version", 1, "payload", Map.of("text", text), "title", title)));
		}
		List<Response> responses = Parallel.run(calls);

		// then exactly one response is 200 with version 2
		List<Response> won = responses.stream().filter(r -> r.statusCode() == 200).toList();
		assertEquals(1, won.size());
		assertEquals(2, (int) won.get(0).path("version"));
		// and the other 5 are 409 VERSION_CONFLICT
		assertEquals(5, responses.stream()
				.filter(r -> r.statusCode() == 409 && "VERSION_CONFLICT".equals(r.path("code")))
				.count());
		// and the item has version 2 and the text and the title of the successful response
		assertEquals(2, readItem(item).version());
		assertEquals(won.get(0).path("title"), readItem(item).title());
		assertEquals(won.get(0).path("payload.text"),
				jdbc.sql("select payload->>'text' from items where id = :id")
						.param("id", item).query(String.class).single());
	}

	@Test
	@DisplayName("[items/R4-S10] Editing items while the part is deleted")
	void editingItemsWhileThePartIsDeleted() throws Exception {
		// given a logged-in teacher A with a part with 5 items
		var setup = teacherWithPart();
		UUID first = null;
		for (int i = 1; i <= 5; i++) {
			UUID id = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Item " + i + "\"}", i);
			first = first == null ? id : first;
		}
		String path = item(setup.course(), setup.lesson(), setup.part(), first);

		// when 1 request deleting the part and 4 requests editing one of its items with the current version are sent at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		TestClient deleter = setup.client().snapshot();
		calls.add(() -> deleter.delete("/api/teacher/courses/" + setup.course() + "/lessons/" + setup.lesson()
				+ "/parts/" + setup.part()));
		for (int i = 1; i <= 4; i++) {
			TestClient client = setup.client().snapshot();
			String text = "Edit " + i;
			calls.add(() -> client.patch(path, Map.of("version", 1, "payload", Map.of("text", text))));
		}
		List<Response> responses = Parallel.run(calls);

		// then the delete response is 204
		responses.get(0).then().statusCode(204);
		// and each edit response is 200, 404 or 409, at most one is 200, and none is 500
		List<Response> edits = responses.subList(1, 5);
		for (Response response : edits) {
			int status = response.statusCode();
			assertTrue(status == 200 || status == 404 || status == 409, "status " + status);
		}
		assertTrue(edits.stream().filter(r -> r.statusCode() == 200).count() <= 1);
		// and the part is not stored
		assertEquals(0, countParts(setup.lesson()));
		// and no item row references it
		assertEquals(0, countItems(setup.part()));
	}

	private static Map<String, Object> edit(Object version, Map<String, Object> payload) {
		Map<String, Object> request = new HashMap<>();
		request.put("version", version);
		request.put("payload", payload);
		return request;
	}

	private static Map<String, Object> edit(Object version, Map<String, Object> payload, Object title) {
		Map<String, Object> request = edit(version, payload);
		request.put("title", title);
		return request;
	}

	private static void expectNoRejectedValue(Response response) {
		response.then().body(not(containsString("rejectedValue")));
	}

	@Test
	@DisplayName("[items/R4-S11] Edit an English item: the title is set, replaced and cleared with the payload")
	void theTitleIsSetReplacedAndClearedWithThePayload() {
		// given a logged-in teacher A with an item of the type test-note with the payload {"text": "Hi"}, no title and version 1, created at a time T, and the clock advanced by one hour
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		Instant created = Instant.now(clock);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		testClock().advance(Duration.ofHours(1));
		Map<String, Object> hi = Map.of("text", "Hi");
		Map<String, Object> hello = Map.of("text", "Hello");

		// when teacher A edits it with the title "Greeting", then advances the clock and edits it with "Greeting 2", with no title, with "Back" and with null
		var first = setup.client().patch(path, edit(1, hi, "Greeting"));
		testClock().advance(Duration.ofHours(1));
		var second = setup.client().patch(path, edit(2, hello, "Greeting 2"));
		var third = setup.client().patch(path, edit(3, hello));
		var fourth = setup.client().patch(path, edit(4, hello, "Back"));
		var fifth = setup.client().patch(path, edit(5, hello, null));

		// then the responses are 200 with the titles "Greeting", "Greeting 2", null, "Back" and null, and version 2 to 6
		List<Response> responses = List.of(first, second, third, fourth, fifth);
		List<String> titles = new ArrayList<>(List.of("Greeting", "Greeting 2"));
		titles.add(null);
		titles.add("Back");
		titles.add(null);
		for (int i = 0; i < responses.size(); i++) {
			responses.get(i).then().statusCode(200)
					.body("title", equalTo(titles.get(i)))
					.body("version", equalTo(i + 2))
					.body("type", equalTo("test-note"))
					.body("position", equalTo(1));
			assertEquals(created, Instant.parse(responses.get(i).path("createdAt")));
		}
		// and the payloads are as sent
		first.then().body("payload", equalTo(hi));
		for (Response response : responses.subList(1, 5)) {
			response.then().body("payload", equalTo(hello));
		}
		// and updatedAt is T plus one hour for the first and T plus two hours for the others
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		for (Response response : responses.subList(1, 5)) {
			assertEquals(created.plus(Duration.ofHours(2)), Instant.parse(response.path("updatedAt")));
		}
		// and the stored item has the last title and version
		assertNull(readItem(item).title());
		assertEquals(6, readItem(item).version());
		// and listing the part shows the same title
		setup.client().get(setup.items()).then().statusCode(200).body("[0].title", nullValue());
	}

	@Test
	@DisplayName("[items/R4-S12] Edit a Russian item: a Cyrillic title is kept exactly, a title-only change still needs the payload")
	void aCyrillicTitleIsKeptAndATitleOnlyChangeNeedsThePayload() {
		// given a logged-in teacher A with the course "Литература 9 класс" with a theory item with the payload {"text": "Падежи"}, the title "Теория" and version 1, created at a time T, and the clock advanced by one hour
		UUID teacher = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacher, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacher, course, "Падежи", 1);
		UUID part = createPart(teacher, lesson, "Разминка", 1);
		UUID item = createItem(teacher, part, "theory", "Теория", "{\"text\": \"Падежи\"}", 1, 1);
		Instant created = Instant.now(clock);
		TestClient client = loggedInAs("a@example.com");
		String path = item(course, lesson, part, item);
		testClock().advance(Duration.ofHours(1));

		// when teacher A edits it with the title "  Немного теории" plus a zero-width space and two spaces, and then with {"version": 2, "title": "Итоги"} and no payload
		var first = client.patch(path, edit(1, Map.of("text", "Падежи"), "  Немного теории​  "));
		var second = client.patch(path, Map.of("version", 2, "title", "Итоги"));

		// then the first response is 200 with the title "Немного теории", the payload, version 2 and updatedAt T plus one hour
		first.then().statusCode(200)
				.body("title", equalTo("Немного теории"))
				.body("payload", equalTo(Map.of("text", "Падежи")))
				.body("version", equalTo(2));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		// and the second is 400 with payload REQUIRED
		expectValidation(second, error("payload", "REQUIRED"));
		// and the stored item still has the title "Немного теории" and version 2
		assertEquals("Немного теории", readItem(item).title());
		assertEquals(2, readItem(item).version());
	}

	@Test
	@DisplayName("[items/R4-S13] An invalid title is rejected and changes nothing")
	void anInvalidTitleIsRejectedAndChangesNothing() {
		// given a logged-in teacher A with a test-note item with the payload {"text": "Hi"}, the title "Keep" and version 1
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "Keep", "{\"text\": \"Hi\"}", 1, 1);
		String path = item(setup.course(), setup.lesson(), setup.part(), item);
		Map<String, Object> payload = Map.of("text", "New");

		// when teacher A edits it with a valid payload, version 1 and a title of 201 letters, "!!!", 123, true, ["Keep"], {"a": "Keep"}, and then "   "
		var tooLong = setup.client().patch(path, edit(1, payload, "Я".repeat(201)));
		var invalidFormat = setup.client().patch(path, edit(1, payload, "!!!"));
		List<Response> invalidType = List.of(
				setup.client().patch(path, edit(1, payload, 123)),
				setup.client().patch(path, edit(1, payload, true)),
				setup.client().patch(path, edit(1, payload, List.of("Keep"))),
				setup.client().patch(path, edit(1, payload, Map.of("a", "Keep"))));
		var itemAfterRejections = readItem(item);
		var blank = setup.client().patch(path, edit(1, payload, "   "));

		// then the first response is 400 with title TOO_LONG, the second INVALID_FORMAT, the next four INVALID_TYPE
		expectValidation(tooLong, error("title", "TOO_LONG"));
		expectValidation(invalidFormat, error("title", "INVALID_FORMAT"));
		for (Response response : invalidType) {
			expectValidation(response, error("title", "INVALID_TYPE"));
		}
		// and no body has a rejectedValue
		expectNoRejectedValue(tooLong);
		expectNoRejectedValue(invalidFormat);
		invalidType.forEach(EditItemTest::expectNoRejectedValue);
		// and the item still has the title "Keep", the payload {"text": "Hi"} and version 1 after those six
		assertEquals("Keep", itemAfterRejections.title());
		assertEquals(1, itemAfterRejections.version());
		assertEquals("{\"text\": \"Hi\"}", itemAfterRejections.payload());
		// and the last response is 200 with title null and version 2
		blank.then().statusCode(200).body("title", nullValue()).body("version", equalTo(2));
	}

	@Test
	@DisplayName("[items/R4-S14] A title error joins the envelope errors, before the item lookup and the payload checks")
	void aTitleErrorJoinsTheEnvelopeErrors() {
		// given teacher A logged in with an item I with the payload {"text": "Hi"} and version 1, and a course "Грамматика" of teacher B with a part with the item "Секрет"
		var setup = teacherWithPart();
		UUID itemI = createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Hi\"}", 1);
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID secret = createItem(teacherB, foreignPart, "test-note", "{\"text\": \"Секрет\"}", 1);
		String own = item(setup.course(), setup.lesson(), setup.part(), itemI);

		// when teacher A sends {"title": "!!!"} for (1) teacher B's item and (2) an unknown item id, then {"version": 1, "payload": {}, "title": "!!!"} for (3) I, then {"version": 9, "payload": {}, "title": ""} for (4) I
		var foreign = setup.client().patch(item(foreignCourse, foreignLesson, foreignPart, secret), Map.of("title", "!!!"));
		var unknown = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), UUID.randomUUID()),
				Map.of("title", "!!!"));
		var third = setup.client().patch(own, edit(1, Map.of(), "!!!"));
		var fourth = setup.client().patch(own, edit(9, Map.of(), ""));

		// then (1) and (2) are 400 with payload REQUIRED, title INVALID_FORMAT and version REQUIRED, and do not contain "Секрет"
		for (Response response : List.of(foreign, unknown)) {
			expectValidation(response, error("payload", "REQUIRED"), error("title", "INVALID_FORMAT"),
					error("version", "REQUIRED"));
			response.then().body(not(containsString("Секрет")));
		}
		// and (3) is 400 with title INVALID_FORMAT only
		expectValidation(third, error("title", "INVALID_FORMAT"));
		// and (4) is 400 with payload.text REQUIRED
		expectValidation(fourth, error("payload.text", "REQUIRED"));
		// and I and teacher B's item are unchanged
		assertEquals(1, readItem(itemI).version());
		assertStoredPayload(itemI, "{\"text\": \"Hi\"}");
		assertEquals(1, readItem(secret).version());
		assertStoredPayload(secret, "{\"text\": \"Секрет\"}");
	}

	@Test
	@DisplayName("[items/R4-S15] A non-string title is a binding error and hides the other envelope errors")
	void aNonStringTitleIsABindingError() {
		// given a logged-in teacher A with an item with the payload {"text": "Hi"}, the title "Keep" and version 1
		var setup = teacherWithPart();
		UUID item = createItem(setup.teacher(), setup.part(), "test-note", "Keep", "{\"text\": \"Hi\"}", 1, 1);

		// when teacher A edits it with {"version": 1, "title": 123}
		var response = setup.client().patch(item(setup.course(), setup.lesson(), setup.part(), item),
				Map.of("version", 1, "title", 123));

		// then the response is 400 with title INVALID_TYPE only
		expectValidation(response, error("title", "INVALID_TYPE"));
		// and the item still has the title "Keep", the payload and version 1
		assertEquals("Keep", readItem(item).title());
		assertStoredPayload(item, "{\"text\": \"Hi\"}");
		assertEquals(1, readItem(item).version());
	}
}
