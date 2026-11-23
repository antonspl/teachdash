package com.teachdash.items.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

/** items R6. */
class MoveItemTest extends ItemApiTest {

	private static String movePath(Object courseId, Object lessonId, Object partId, Object itemId) {
		return items(courseId, lessonId, partId) + "/" + itemId + "/position";
	}

	private static Map<String, Object> position(Object position) {
		Map<String, Object> body = new HashMap<>();
		body.put("position", position);
		return body;
	}

	private record StoredItem(
			UUID id,
			String text,
			int position,
			long version,
			Instant updatedAt) {
	}

	private List<StoredItem> storedItems(UUID partId) {
		return jdbc.sql("""
				select id, payload->>'text' as text, position, version, updated_at
				from items
				where part_id = :part
				order by position
				""")
				.param("part", partId)
				.query((rs, i) -> new StoredItem(
						rs.getObject("id", UUID.class),
						rs.getString("text"),
						rs.getInt("position"),
						rs.getLong("version"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private List<String> texts(UUID partId) {
		return storedItems(partId).stream().map(StoredItem::text).toList();
	}

	private List<UUID> createNotes(UUID teacherId, UUID partId, String... texts) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < texts.length; i++) {
			ids.add(createItem(teacherId, partId, "test-note", "{\"text\": \"" + texts[i] + "\"}", i + 1));
		}
		return ids;
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")))
				.body(not(containsString("Вопрос")));
	}

	@Test
	@DisplayName("[items/R6-S1] Move an item down and up, a Russian payload is unchanged")
	void moveAnItemDownAndUp() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lesson "Падежи" that has the parts "Разминка" and "Итоги", where "Разминка" has four items "Один", "Два", "Три" and "Четыре", all created at a time T with version 1, "Итоги" has four items, a teacher B with a part with four items created at T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID warmUp = createPart(teacherA, lesson, "Разминка", 1);
		UUID summary = createPart(teacherA, lesson, "Итоги", 2);
		List<UUID> ids = createNotes(teacherA, warmUp, "Один", "Два", "Три", "Четыре");
		createNotes(teacherA, summary, "Первый", "Второй", "Третий", "Четвёртый");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Part", 1);
		createNotes(teacherB, foreignPart, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var summaryBefore = storedItems(summary);
		var foreignBefore = storedItems(foreignPart);
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "Один" to position 3, then advances the clock by one more hour and moves "Четыре" to position 1
		var first = teacher.patch(movePath(course, lesson, warmUp, ids.get(0)), position(3));
		testClock().advance(Duration.ofHours(1));
		var second = teacher.patch(movePath(course, lesson, warmUp, ids.get(3)), position(1));

		// then the first response is 200 with the text "Один", the type test-note, position 3, version 1 and updatedAt T plus one hour
		first.then().statusCode(200)
				.body("payload.text", equalTo("Один"))
				.body("type", equalTo("test-note"))
				.body("position", equalTo(3))
				.body("version", equalTo(1));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		// and exactly the properties id, type, payload, position, version, createdAt and updatedAt, createdAt T
		assertEquals(Set.of("id", "type", "payload", "position", "version", "createdAt", "updatedAt"),
				first.<Map<String, Object>>path("$").keySet());
		assertEquals(created, Instant.parse(first.path("createdAt")));
		// and the second is 200 with the text "Четыре", position 1 and updatedAt T plus two hours
		second.then().statusCode(200)
				.body("payload.text", equalTo("Четыре"))
				.body("position", equalTo(1));
		assertEquals(created.plus(Duration.ofHours(2)), Instant.parse(second.path("updatedAt")));
		// and the final order of "Разминка" is "Четыре", "Два", "Три", "Один" at the positions 1, 2, 3, 4
		var stored = storedItems(warmUp);
		assertEquals(List.of("Четыре", "Два", "Три", "Один"), stored.stream().map(StoredItem::text).toList());
		assertEquals(List.of(1, 2, 3, 4), stored.stream().map(StoredItem::position).toList());
		// and the shifted items "Два" and "Три" have updatedAt T and version 1
		for (int index : new int[] { 1, 2 }) {
			assertEquals(created, stored.get(index).updatedAt());
			assertEquals(1, stored.get(index).version());
		}
		// and "Один" keeps updatedAt T plus one hour, its payload and version 1
		assertEquals(created.plus(Duration.ofHours(1)), stored.get(3).updatedAt());
		assertEquals(1, stored.get(3).version());
		assertEquals("Один", stored.get(3).text());
		// and the order and the updatedAt of the items of "Итоги" and of teacher B's part are unchanged
		assertEquals(summaryBefore, storedItems(summary));
		assertEquals(foreignBefore, storedItems(foreignPart));
		// and the updatedAt of the parts, the lesson and the course is unchanged
		assertEquals(created, readPartUpdatedAt(warmUp));
		assertEquals(created, readPartUpdatedAt(summary));
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[items/R6-S2] Moving to the same position bumps updatedAt, not version")
	void movingToTheSamePositionBumpsUpdatedAtNotVersion() {
		// given a logged-in teacher A with the course "English B1" with a part with the items "One", "Two" and "Three" created at a time T with version 1, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Warm-up", 1);
		List<UUID> ids = createNotes(teacherA, part, "One", "Two", "Three");
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "Two" to position 2
		var response = teacher.patch(movePath(course, lesson, part, ids.get(1)), position(2));

		// then the response is 200 with position 2, version 1 and updatedAt T plus one hour
		response.then().statusCode(200)
				.body("position", equalTo(2))
				.body("version", equalTo(1));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and the order is unchanged, "One" and "Three" have updatedAt T and version 1
		var stored = storedItems(part);
		assertEquals(List.of("One", "Two", "Three"), stored.stream().map(StoredItem::text).toList());
		for (int index : new int[] { 0, 2 }) {
			assertEquals(created, stored.get(index).updatedAt());
			assertEquals(1, stored.get(index).version());
		}
		// and the updatedAt of the part, the lesson and the course is unchanged
		assertEquals(created, readPartUpdatedAt(part));
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[items/R6-S3] Any integer is clamped to the part range")
	void anyIntegerIsClampedToThePartRange() {
		// given a logged-in teacher A with a part "Main" with the items "A", "B", "C", a part "Single" with the one item "Only", a part "Sibling" with four items, and a teacher B with a part with four items, all created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID main = createPart(teacherA, lesson, "Main", 1);
		UUID single = createPart(teacherA, lesson, "Single", 2);
		UUID sibling = createPart(teacherA, lesson, "Sibling", 3);
		List<UUID> mainItems = createNotes(teacherA, main, "A", "B", "C");
		UUID only = createNotes(teacherA, single, "Only").get(0);
		createNotes(teacherA, sibling, "S1", "S2", "S3", "S4");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Part", 1);
		createNotes(teacherB, foreignPart, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var siblingBefore = storedItems(sibling);
		var foreignBefore = storedItems(foreignPart);
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4, and moves "Only" to position 7, then to 0, then to -3
		UUID a = mainItems.get(0);
		UUID c = mainItems.get(2);
		var huge = teacher.patch(movePath(course, lesson, main, a), position(new BigInteger("99999999999999999999")));
		var negative = teacher.patch(movePath(course, lesson, main, a), position(-5));
		var zero = teacher.patch(movePath(course, lesson, main, c), position(0));
		var four = teacher.patch(movePath(course, lesson, main, c), position(4));
		var seven = teacher.patch(movePath(course, lesson, single, only), position(7));
		var onlyZero = teacher.patch(movePath(course, lesson, single, only), position(0));
		var onlyNegative = teacher.patch(movePath(course, lesson, single, only), position(-3));

		// then the first four responses are 200 with the positions 3, 1, 1 and 3
		huge.then().statusCode(200).body("position", equalTo(3));
		negative.then().statusCode(200).body("position", equalTo(1));
		zero.then().statusCode(200).body("position", equalTo(1));
		four.then().statusCode(200).body("position", equalTo(3));
		// and the final order of "Main" is "A", "B", "C" without gaps
		assertEquals(List.of("A", "B", "C"), texts(main));
		assertEquals(List.of(1, 2, 3), itemPositions(main));
		// and the last three responses are 200 with position 1
		for (Response response : List.of(seven, onlyZero, onlyNegative)) {
			response.then().statusCode(200).body("position", equalTo(1));
		}
		// and "Single" has exactly one item at position 1
		assertEquals(List.of(1), itemPositions(single));
		// and the order and updatedAt T of the items of "Sibling" and of teacher B's part are unchanged
		assertEquals(siblingBefore, storedItems(sibling));
		assertEquals(foreignBefore, storedItems(foreignPart));
	}

	@Test
	@DisplayName("[items/R6-S4] The position must be an integer")
	void thePositionMustBeAnInteger() {
		// given a logged-in teacher A with a part with the items "A" and "B"
		Setup setup = teacherWithPart();
		UUID a = createNotes(setup.teacher(), setup.part(), "A", "B").get(0);
		var before = storedItems(setup.part());

		// when teacher A moves "A" with the body {}, with {"position": null}, with the position "2", with the position "", with {"position": {"n": 2}}, with the positions 1.5, 2.0 and 1e2, with the position true, and with the bodies {"position": and no body
		String path = movePath(setup.course(), setup.lesson(), setup.part(), a);
		var empty = setup.client().patch(path, Map.of());
		var nullPosition = setup.client().patch(path, position(null));
		var string = setup.client().patch(path, position("2"));
		var emptyString = setup.client().patch(path, position(""));
		var object = setup.client().patch(path, position(Map.of("n", 2)));
		var fraction = setup.client().patch(path, "{\"position\": 1.5}");
		var float2 = setup.client().patch(path, "{\"position\": 2.0}");
		var exponent = setup.client().patch(path, "{\"position\": 1e2}");
		var bool = setup.client().patch(path, position(true));
		var truncated = setup.client().patch(path, "{\"position\":");
		var noBody = setup.client().patch(path);

		// then the first and the second responses are 400 VALIDATION_FAILED with errors exactly position REQUIRED
		for (Response response : List.of(empty, nullPosition)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("position", "REQUIRED"))));
		}
		// and the third to the ninth are 400 with errors exactly position INVALID_TYPE
		for (Response response : List.of(string, emptyString, object, fraction, float2, exponent, bool)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("position", "INVALID_TYPE"))));
		}
		// and the tenth and the eleventh are 400 MALFORMED_REQUEST with no errors property
		for (Response response : List.of(truncated, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and the order is still "A", "B" with updatedAt and version unchanged
		assertEquals(before, storedItems(setup.part()));
	}

	@Test
	@DisplayName("[items/R6-S5] Another teacher's, unknown, malformed and misplaced ids are not found")
	void anotherTeachersUnknownMalformedAndMisplacedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" and "Quarters" in "Fractions" and "Tenths" in "Decimals", the items "Half" in "Halves" and "Tenth" in "Tenths", and a course "Грамматика" of teacher B with the lesson "Падежи", the part "Разминка" and its item "Вопрос"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		UUID quarters = createPart(teacherA, fractions, "Quarters", 2);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID half = createNotes(teacherA, halves, "Half").get(0);
		UUID tenth = createNotes(teacherA, tenths, "Tenth").get(0);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID question = createNotes(teacherB, foreignPart, "Вопрос").get(0);
		TestClient teacher = loggedInAs("a@example.com");
		var halvesBefore = storedItems(halves);
		var tenthsBefore = storedItems(tenths);
		var foreignBefore = storedItems(foreignPart);

		// when teacher A moves each to position 1: sixteen requests under the ids listed in the scenario
		List<Response> responses = List.of(
				teacher.patch(movePath(foreignCourse, foreignLesson, foreignPart, question), position(1)),
				teacher.patch(movePath(maths, fractions, halves, question), position(1)),
				teacher.patch(movePath(maths, fractions, foreignPart, question), position(1)),
				teacher.patch(movePath(maths, foreignLesson, foreignPart, question), position(1)),
				teacher.patch(movePath(foreignCourse, foreignLesson, foreignPart, half), position(1)),
				teacher.patch(movePath(maths, fractions, halves, UUID.randomUUID()), position(1)),
				teacher.patch(movePath(maths, fractions, halves, "not-a-uuid"), position(1)),
				teacher.patch(movePath(maths, fractions, quarters, half), position(1)),
				teacher.patch(movePath(maths, fractions, halves, tenth), position(1)),
				teacher.patch(movePath(english, fractions, halves, half), position(1)),
				teacher.patch(movePath(maths, fractions, UUID.randomUUID(), half), position(1)),
				teacher.patch(movePath(maths, fractions, "not-a-uuid", half), position(1)),
				teacher.patch(movePath(maths, UUID.randomUUID(), halves, half), position(1)),
				teacher.patch(movePath(maths, "not-a-uuid", halves, half), position(1)),
				teacher.patch(movePath(UUID.randomUUID(), fractions, halves, half), position(1)),
				teacher.patch(movePath("not-a-uuid", fractions, halves, half), position(1)));

		// then all responses are 404 NOT_FOUND with no errors property, none contains "Грамматика", "Падежи", "Разминка" or "Вопрос"
		responses.forEach(this::assertNotFound);
		// and the order, updatedAt and version of all items are unchanged
		assertEquals(halvesBefore, storedItems(halves));
		assertEquals(tenthsBefore, storedItems(tenths));
		assertEquals(foreignBefore, storedItems(foreignPart));
	}

	@Test
	@DisplayName("[items/R6-S6] Only active teachers move items")
	void onlyActiveTeachersMoveItems() {
		// given a part of teacher A with the items "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		Setup setup = teacherWithPart();
		UUID two = createNotes(setup.teacher(), setup.part(), "One", "Two").get(1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
		String path = movePath(setup.course(), setup.lesson(), setup.part(), two);
		var adminResponse = admin.patch(path, position(1));
		var studentResponse = student.patch(path, position(1));
		var deactivatedResponse = deactivated.patch(path, position(1));
		var anonymousResponse = anonymous.patch(path, position(1));
		var csrfResponse = withoutCsrf.patch(path, position(1));

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
		// and the order is still "One", "Two"
		assertEquals(List.of("One", "Two"), texts(setup.part()));
	}

	@Test
	@DisplayName("[items/R6-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a course C with a lesson L that has the part P with the item I, a logged-in teacher B with a course "Грамматика" with the lesson "Падежи", the part "Разминка" and its item "Вопрос", a logged-in admin, and a logged-in teacher A2 who is then deactivated
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID courseC = createCourse(teacherA, "English B1", "en", "en");
		UUID lessonL = createLesson(teacherA, courseC, "Present Simple", 1);
		UUID partP = createPart(teacherA, lessonL, "Part", 1);
		UUID itemI = createNotes(teacherA, partP, "Item").get(0);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		UUID question = createNotes(teacherB, foreignPart, "Вопрос").get(0);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();
		TestClient teacherBWithoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();
		var foreignBefore = storedItems(foreignPart);

		// when teacher A sends the body {} for (1) the item "not-a-uuid" under C, L and P, (1a) the body {"position": and (1b) no body under the item "not-a-uuid" under C, L and P, (2) teacher B's item "Вопрос" under teacher B's course, lesson and part, (3) an item with an unknown id under C, L and P, and (4) I under C, L and the part "not-a-uuid"; and the admin and the deactivated teacher A2 each move the item "not-a-uuid" to position 1 under the part "not-a-uuid", the lesson "not-a-uuid" and the course "not-a-uuid"
		var malformed = teacher.patch(movePath(courseC, lessonL, partP, "not-a-uuid"), Map.of());
		var malformedTruncated = teacher.patch(movePath(courseC, lessonL, partP, "not-a-uuid"), "{\"position\":");
		var malformedNoBody = teacher.patch(movePath(courseC, lessonL, partP, "not-a-uuid"));
		var foreignResponse = teacher.patch(movePath(foreignCourse, foreignLesson, foreignPart, question), Map.of());
		var unknown = teacher.patch(movePath(courseC, lessonL, partP, UUID.randomUUID()), Map.of());
		var badPart = teacher.patch(movePath(courseC, lessonL, "not-a-uuid", itemI), Map.of());
		String garbled = movePath("not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid");
		var adminResponse = admin.patch(garbled, position(1));
		var deactivatedResponse = deactivated.patch(garbled, position(1));
		var csrfResponse = teacherBWithoutCsrf.patch(garbled, position(1));

		// then teacher A's first, (1a) and (1b) responses are 404 NOT_FOUND with no errors property
		for (Response response : List.of(malformed, malformedTruncated, malformedNoBody)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")));
		}
		// and the second and the third are 400 VALIDATION_FAILED for the position and do not contain "Падежи", "Разминка" or "Вопрос"
		for (Response response : List.of(foreignResponse, unknown)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("position", "REQUIRED"))))
					.body(not(containsString("Падежи")))
					.body(not(containsString("Разминка")))
					.body(not(containsString("Вопрос")));
		}
		// and the fourth is 404 NOT_FOUND
		badPart.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		// and the admin's response is 403 FORBIDDEN and the deactivated teacher's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher B's item is unchanged
		assertEquals(foreignBefore, storedItems(foreignPart));
		// and teacher B without a CSRF token gets 403 CSRF_INVALID
		csrfResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
	}

	@Test
	@DisplayName("[items/R6-S8] Parallel moves keep positions distinct (the lock chain serialises them)")
	void parallelMovesKeepPositionsDistinct() throws Exception {
		// given a logged-in teacher A with a part with 10 items at the positions 1 to 10, whose ids are read in GIVEN
		Setup setup = teacherWithPart();
		List<UUID> ids = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			ids.add(createItem(setup.teacher(), setup.part(), "test-note", "{\"text\": \"Item " + i + "\"}", i));
		}

		// when teacher A sends 10 move requests at the same time, each moving a different item, addressed by its id, to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
		int[] targets = { 5, 1, 10, 3, 7, 2, 9, 4, 8, 6 };
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			TestClient client = setup.client().snapshot();
			UUID id = ids.get(i);
			int target = targets[i];
			calls.add(() -> client.patch(movePath(setup.course(), setup.lesson(), setup.part(), id), position(target)));
		}
		List<Response> responses = Parallel.run(calls);

		// then every response is 200
		for (Response response : responses) {
			response.then().statusCode(200);
		}
		// and the part has exactly 10 items with the positions 1 to 10 without gaps and without duplicates
		assertEquals(10, countItems(setup.part()));
		assertEquals(IntStream.rangeClosed(1, 10).boxed().toList(), itemPositions(setup.part()));
		// and the final order is a permutation of the original ids
		assertEquals(Set.copyOf(ids), Set.copyOf(storedItems(setup.part()).stream().map(StoredItem::id).toList()));
	}
}
