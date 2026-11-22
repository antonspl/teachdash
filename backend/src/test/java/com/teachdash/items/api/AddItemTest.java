package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** items R1. */
class AddItemTest extends ItemApiTest {

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")));
	}

	@Test
	@DisplayName("[items/R1-S1] Add an English item")
	void addAnEnglishItem() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple" that has the part "Warm-up", the item type test-note, and another teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A adds an item with the type test-note and the payload to the part, with the extra body fields teacherId and title
		Map<String, Object> request = note("Read the story aloud.");
		request.put("teacherId", teacherB.toString());
		request.put("title", "Ignored");
		var response = teacher.post(items(course, lesson, part), request);

		// then the response is 201 with a new id, the type, the payload, position 1 and version 1
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("type", equalTo("test-note"))
				.body("payload", equalTo(Map.of("text", "Read the story aloud.")))
				.body("position", equalTo(1))
				.body("version", equalTo(1))
				.body("createdAt", notNullValue());
		// and updatedAt equals createdAt
		assertEquals(response.<String>path("createdAt"), response.<String>path("updatedAt"));
		// and exactly these properties are returned
		assertEquals(Set.of("id", "type", "payload", "position", "version", "createdAt", "updatedAt"),
				response.<Map<String, Object>>path("$").keySet());
		// and the item is stored owned by teacher A in that part at position 1
		assertEquals(1, countItems(part));
		var stored = readItem(UUID.fromString(response.path("id")));
		assertEquals(teacherA, stored.teacherId());
		assertEquals(part, stored.partId());
		assertEquals(1, stored.position());
	}

	@Test
	@DisplayName("[items/R1-S2] Add a Russian item, Cyrillic text is kept exactly")
	void addARussianItem() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID part = createPart(teacherA, lesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A adds an item of the type test-note with a Cyrillic text to the part
		String text = "Просклоняйте слово «мышь»: именительный, родительный?  Ёж & ёлка.";
		var response = teacher.post(items(course, lesson, part), note(text));

		// then the response is 201 with position 1 and a payload whose text is exactly that text
		response.then().statusCode(201)
				.body("position", equalTo(1))
				.body("payload.text", equalTo(text));
		// and the stored payload has the same text
		assertEquals(text, storedText(part, 1));
	}

	@Test
	@DisplayName("[items/R1-S3] Items are added at the end, per part")
	void itemsAreAddedAtTheEndPerPart() {
		// given a logged-in teacher A with a lesson with the parts "Reading" and "Writing", where "Reading" already has the items "One" and "Two" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID reading = createPart(teacherA, lesson, "Reading", 1);
		UUID writing = createPart(teacherA, lesson, "Writing", 2);
		UUID one = createItem(teacherA, reading, "test-note", "{\"text\": \"One\"}", 1);
		UUID two = createItem(teacherA, reading, "test-note", "{\"text\": \"Two\"}", 2);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A adds the item "Three" to "Reading" and the item "First" to "Writing"
		var three = teacher.post(items(course, lesson, reading), note("Three"));
		var first = teacher.post(items(course, lesson, writing), note("First"));

		// then the responses are 201 with position 3 and position 1
		three.then().statusCode(201).body("position", equalTo(3));
		first.then().statusCode(201).body("position", equalTo(1));
		// and "Reading" has the positions 1, 2, 3 without gaps
		assertEquals(List.of(1, 2, 3), itemPositions(reading));
		// and the items "One" and "Two" keep their positions, their version 1 and their updatedAt T
		for (UUID id : List.of(one, two)) {
			var row = readItem(id);
			assertEquals(1, row.version());
			assertEquals(created, row.updatedAt());
		}
		assertEquals(1, readItem(one).position());
		assertEquals(2, readItem(two).position());
		// and the updatedAt of the course, of the lesson and of both parts is unchanged
		assertEquals(created, readCourse(course).updatedAt());
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readPartUpdatedAt(reading));
		assertEquals(created, readPartUpdatedAt(writing));
	}

	@Test
	@DisplayName("[items/R1-S4] At most 50 items per part")
	void atMost50ItemsPerPart() {
		// given a logged-in teacher A with a part "Full" with 50 items and a part "Other" with 49 items
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID full = createPart(teacherA, lesson, "Full", 1);
		UUID other = createPart(teacherA, lesson, "Other", 2);
		for (int i = 1; i <= 50; i++) {
			createItem(teacherA, full, "test-note", "{\"text\": \"Item " + i + "\"}", i);
		}
		for (int i = 1; i <= 49; i++) {
			createItem(teacherA, other, "test-note", "{\"text\": \"Item " + i + "\"}", i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A adds an item to "Full", then an item with the payload {} to "Full", and then an item to "Other"
		var over = teacher.post(items(course, lesson, full), note("Fifty-one"));
		var empty = teacher.post(items(course, lesson, full), body("test-note", Map.of()));
		var last = teacher.post(items(course, lesson, other), note("Fiftieth"));

		// then the first response is 409 ITEM_LIMIT_REACHED with no errors property
		over.then().statusCode(409)
				.body("code", equalTo("ITEM_LIMIT_REACHED"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED with payload.text REQUIRED
		empty.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("payload.text", "REQUIRED"))));
		// and the third is 201 with position 50
		last.then().statusCode(201).body("position", equalTo(50));
		// and both parts have 50 items
		assertEquals(50, countItems(full));
		assertEquals(50, countItems(other));
	}

	@Test
	@DisplayName("[items/R1-S5] Parallel requests keep positions distinct and the limit (the lock chain serialises them)")
	void parallelRequestsKeepPositionsDistinctAndTheLimit() throws Exception {
		// given a logged-in teacher A with a part with 47 items
		var setup = teacherWithPart();
		for (int i = 1; i <= 47; i++) {
			createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Item " + i + "\"}", i);
		}

		// when teacher A sends 6 add requests at the same time with the texts "New 1" to "New 6"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			TestClient client = setup.client().snapshot();
			String text = "New " + i;
			calls.add(() -> client.post(setup.items(), note(text)));
		}
		List<Response> responses = Parallel.run(calls);

		// then exactly 3 responses are 201 with the distinct positions 48, 49 and 50
		List<Response> created = responses.stream().filter(r -> r.statusCode() == 201).toList();
		assertEquals(3, created.size());
		assertEquals(Set.of(48, 49, 50), Set.copyOf(created.stream().map(r -> r.<Integer>path("position")).toList()));
		// and the other 3 are 409 ITEM_LIMIT_REACHED
		assertEquals(3, responses.stream()
				.filter(r -> r.statusCode() == 409 && "ITEM_LIMIT_REACHED".equals(r.path("code")))
				.count());
		// and the part has exactly 50 items with the positions 1 to 50 without gaps
		assertEquals(50, countItems(setup.part()));
		assertEquals(IntStream.rangeClosed(1, 50).boxed().toList(), itemPositions(setup.part()));
	}

	@Test
	@DisplayName("[items/R1-S6] Another teacher's, unknown and malformed course, lesson and part ids are not found")
	void anotherTeachersUnknownAndMalformedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи" and the part "Разминка", which has 50 items
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		for (int i = 1; i <= 50; i++) {
			createItem(teacherB, foreignPart, "test-note", "{\"text\": \"Item " + i + "\"}", i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A adds a valid item of the type test-note to the ten wrong parent and part combinations
		Map<String, Object> valid = note("Intruder");
		List<Response> responses = List.of(
				teacher.post(items(foreignCourse, foreignLesson, foreignPart), valid),
				teacher.post(items(maths, fractions, foreignPart), valid),
				teacher.post(items(english, foreignLesson, foreignPart), valid),
				teacher.post(items(foreignCourse, foreignLesson, halves), valid),
				teacher.post(items(maths, fractions, UUID.randomUUID()), valid),
				teacher.post(items(maths, fractions, "not-a-uuid"), valid),
				teacher.post(items(maths, UUID.randomUUID(), halves), valid),
				teacher.post(items(maths, "not-a-uuid", halves), valid),
				teacher.post(items(UUID.randomUUID(), fractions, halves), valid),
				teacher.post(items("not-a-uuid", fractions, halves), valid));

		// then all responses are 404 NOT_FOUND with no errors property, and none contains "Грамматика", "Падежи" or "Разминка"
		for (Response response : responses) {
			assertNotFound(response);
		}
		// and teacher B's part still has exactly 50 items
		assertEquals(50, countItems(foreignPart));
		// and "Halves" and "Tenths" have no item
		assertEquals(0, countItems(halves));
		assertEquals(0, countItems(tenths));
	}

	@Test
	@DisplayName("[items/R1-S7] A part of another lesson or course of the same teacher is not found under the wrong parent")
	void aPartUnderTheWrongParentIsNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Tenths", and a lesson "Tenses" in "English B1"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID tenses = createLesson(teacherA, english, "Tenses", 1);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A adds a valid item to "Tenths" under "Maths" and "Fractions", to "Halves" under "Maths" and "Decimals", to "Halves" under "English B1" and "Fractions", and to "Halves" under "English B1" and "Tenses"
		Map<String, Object> valid = note("Intruder");
		List<Response> responses = List.of(
				teacher.post(items(maths, fractions, tenths), valid),
				teacher.post(items(maths, decimals, halves), valid),
				teacher.post(items(english, fractions, halves), valid),
				teacher.post(items(english, tenses, halves), valid));

		// then all responses are 404 NOT_FOUND with no errors property
		for (Response response : responses) {
			assertNotFound(response);
		}
		// and "Halves" and "Tenths" have no item
		assertEquals(0, countItems(halves));
		assertEquals(0, countItems(tenths));
	}

	@Test
	@DisplayName("[items/R1-S8] Only active teachers add items")
	void onlyActiveTeachersAddItems() {
		// given a part of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each add a valid item to the part, and teacher B does so without a CSRF token
		String path = items(course, lesson, part);
		var adminResponse = admin.post(path, note("Hi"));
		var studentResponse = student.post(path, note("Hi"));
		var deactivatedResponse = deactivated.post(path, note("Hi"));
		var anonymousResponse = anonymous.post(path, note("Hi"));
		var csrfResponse = withoutCsrf.post(path, note("Hi"));

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
		// and the part has no item
		assertEquals(0, countItems(part));
	}

	@Test
	@DisplayName("[items/R1-S9] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a part, a part of teacher B with the lesson "Падежи", a logged-in admin, and a logged-in teacher A2 who is then deactivated
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();

		// when teacher A adds an item with the payload {} to the part "not-a-uuid", then to teacher B's part, then to the part under the course "not-a-uuid"; and the admin and the deactivated teacher A2 each add an item with the payload {} to the part "not-a-uuid" of the lesson "not-a-uuid" of the course "not-a-uuid"
		Map<String, Object> empty = body("test-note", Map.of());
		var malformedPart = teacher.post(items(course, lesson, "not-a-uuid"), empty);
		var foreignResponse = teacher.post(items(foreignCourse, foreignLesson, foreignPart), empty);
		var malformedCourse = teacher.post(items("not-a-uuid", lesson, part), empty);
		var adminResponse = admin.post(items("not-a-uuid", "not-a-uuid", "not-a-uuid"), empty);
		var deactivatedResponse = deactivated.post(items("not-a-uuid", "not-a-uuid", "not-a-uuid"), empty);

		// then teacher A's first response is 404 NOT_FOUND with no errors property
		malformedPart.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED with payload.text REQUIRED and does not contain "Падежи"
		foreignResponse.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("payload.text", "REQUIRED"))))
				.body(not(containsString("Падежи")));
		// and the third is 404 NOT_FOUND with no errors property
		malformedCourse.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the admin's response is 403 FORBIDDEN and the deactivated teacher's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher B's part has no new item
		assertEquals(0, countItems(foreignPart));
	}

	@Test
	@DisplayName("[items/R1-S10] Adding items while the part is deleted")
	void addingItemsWhileThePartIsDeleted() throws Exception {
		// given a logged-in teacher A with a part with 5 items
		var setup = teacherWithPart();
		for (int i = 1; i <= 5; i++) {
			createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Item " + i + "\"}", i);
		}

		// when 1 request deleting the part and 4 requests adding an item to it are sent at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		TestClient deleter = setup.client().snapshot();
		calls.add(() -> deleter.delete("/api/teacher/courses/" + setup.course() + "/lessons/" + setup.lesson()
				+ "/parts/" + setup.part()));
		for (int i = 1; i <= 4; i++) {
			TestClient client = setup.client().snapshot();
			String text = "New " + i;
			calls.add(() -> client.post(setup.items(), note(text)));
		}
		List<Response> responses = Parallel.run(calls);

		// then the delete response is 204
		responses.get(0).then().statusCode(204);
		// and each add response is 201 or 404 and none is 500
		for (Response response : responses.subList(1, 5)) {
			assertTrue(response.statusCode() == 201 || response.statusCode() == 404, "status " + response.statusCode());
		}
		// and the part is not stored
		assertEquals(0, countParts(setup.lesson()));
		// and no item row references it
		assertEquals(0, countItems(setup.part()));
	}
}
