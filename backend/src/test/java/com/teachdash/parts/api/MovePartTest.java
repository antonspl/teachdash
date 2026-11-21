package com.teachdash.parts.api;

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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** parts R4. */
class MovePartTest extends ApiTest {

	private static String position(Object courseId, Object lessonId, Object partId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts/" + partId + "/position";
	}

	private static Map<String, Object> position(Object position) {
		Map<String, Object> body = new HashMap<>();
		body.put("position", position);
		return body;
	}

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private record PartRow(
			UUID id,
			String title,
			int position,
			Instant updatedAt) {
	}

	private List<PartRow> storedParts(UUID lessonId) {
		return jdbc.sql("""
				select id, title, position, updated_at
				from parts
				where lesson_id = :lesson
				order by position
				""")
				.param("lesson", lessonId)
				.query((rs, i) -> new PartRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private List<String> titles(UUID lessonId) {
		return storedParts(lessonId).stream().map(PartRow::title).toList();
	}

	private List<UUID> createParts(UUID teacherId, UUID lessonId, String... titles) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < titles.length; i++) {
			ids.add(createPart(teacherId, lessonId, titles[i], i + 1));
		}
		return ids;
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Разминка")));
	}

	@Test
	@DisplayName("[parts/R4-S1] Move a part down and up")
	void moveAPartDownAndUp() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lessons "Падежи" and "Роды", each with four parts at the positions 1 to 4, where "Падежи" has the parts "Один", "Два", "Три" and "Четыре", all created at a time T, a teacher B with a lesson with four parts at the positions 1 to 4 created at T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID cases = createLesson(teacherA, course, "Падежи", 1);
		UUID genders = createLesson(teacherA, course, "Роды", 2);
		List<UUID> casesParts = createParts(teacherA, cases, "Один", "Два", "Три", "Четыре");
		createParts(teacherA, genders, "Первый", "Второй", "Третий", "Четвёртый");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		createParts(teacherB, foreignLesson, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var gendersBefore = storedParts(genders);
		var foreignBefore = storedParts(foreignLesson);
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "Один" to position 3, then advances the clock by one more hour and moves "Четыре" to position 1
		var first = teacher.patch(position(course, cases, casesParts.get(0)), position(3));
		testClock().advance(Duration.ofHours(1));
		var second = teacher.patch(position(course, cases, casesParts.get(3)), position(1));

		// then the first response is 200 with the title "Один", position 3 and updatedAt T plus one hour
		first.then().statusCode(200)
				.body("title", equalTo("Один"))
				.body("position", equalTo(3));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		// and the second is 200 with the title "Четыре", position 1 and updatedAt T plus two hours
		second.then().statusCode(200)
				.body("title", equalTo("Четыре"))
				.body("position", equalTo(1));
		assertEquals(created.plus(Duration.ofHours(2)), Instant.parse(second.path("updatedAt")));
		// and the final order of "Падежи" is "Четыре", "Два", "Три", "Один" at the positions 1, 2, 3, 4
		var stored = storedParts(cases);
		assertEquals(List.of("Четыре", "Два", "Три", "Один"), stored.stream().map(PartRow::title).toList());
		assertEquals(List.of(1, 2, 3, 4), stored.stream().map(PartRow::position).toList());
		// and the shifted parts "Два" and "Три" have updatedAt T, "Один" keeps updatedAt T plus one hour
		assertEquals(created, stored.get(1).updatedAt());
		assertEquals(created, stored.get(2).updatedAt());
		assertEquals(created.plus(Duration.ofHours(1)), stored.get(3).updatedAt());
		// and the order and the updatedAt of the parts of "Роды" and of teacher B's lesson are unchanged
		assertEquals(gendersBefore, storedParts(genders));
		assertEquals(foreignBefore, storedParts(foreignLesson));
		// and the updatedAt of the lessons and of the course is unchanged
		assertEquals(created, readLessonUpdatedAt(cases));
		assertEquals(created, readLessonUpdatedAt(genders));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[parts/R4-S2] Moving to the same position bumps updatedAt")
	void movingToTheSamePositionBumpsUpdatedAt() {
		// given a logged-in teacher A with the course "English B1" with a lesson with the parts "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		List<UUID> parts = createParts(teacherA, lesson, "One", "Two", "Three");
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "Two" to position 2
		var response = teacher.patch(position(course, lesson, parts.get(1)), position(2));

		// then the response is 200 with position 2 and updatedAt T plus one hour
		response.then().statusCode(200).body("position", equalTo(2));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and the order is unchanged, "One" and "Three" have updatedAt T
		var stored = storedParts(lesson);
		assertEquals(List.of("One", "Two", "Three"), stored.stream().map(PartRow::title).toList());
		assertEquals(created, stored.get(0).updatedAt());
		assertEquals(created, stored.get(2).updatedAt());
		// and the updatedAt of the lesson and of the course is unchanged
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[parts/R4-S3] Any integer is clamped to the lesson range")
	void anyIntegerIsClampedToTheLessonRange() {
		// given a logged-in teacher A with a lesson "Main" with the parts "A", "B", "C" at the positions 1, 2, 3, a lesson "Single" with the one part "Only" at position 1, a lesson "Sibling" with four parts at the positions 1 to 4, and a teacher B with a lesson with four parts at the positions 1 to 4, all created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID main = createLesson(teacherA, course, "Main", 1);
		UUID single = createLesson(teacherA, course, "Single", 2);
		UUID sibling = createLesson(teacherA, course, "Sibling", 3);
		List<UUID> mainParts = createParts(teacherA, main, "A", "B", "C");
		UUID only = createPart(teacherA, single, "Only", 1);
		createParts(teacherA, sibling, "S1", "S2", "S3", "S4");
		UUID foreignCourse = createCourse(teacherB, "Grammar", "en", "en");
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Tenses", 1);
		createParts(teacherB, foreignLesson, "W", "X", "Y", "Z");
		TestClient teacher = loggedInAs("a@example.com");
		var siblingBefore = storedParts(sibling);
		var foreignBefore = storedParts(foreignLesson);
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4, and moves "Only" to position 7, then to 0, then to -3
		UUID a = mainParts.get(0);
		UUID c = mainParts.get(2);
		var huge = teacher.patch(position(course, main, a), position(new BigInteger("99999999999999999999")));
		var negative = teacher.patch(position(course, main, a), position(-5));
		var zero = teacher.patch(position(course, main, c), position(0));
		var four = teacher.patch(position(course, main, c), position(4));
		var seven = teacher.patch(position(course, single, only), position(7));
		var onlyZero = teacher.patch(position(course, single, only), position(0));
		var onlyNegative = teacher.patch(position(course, single, only), position(-3));

		// then the first four responses are 200 with the positions 3, 1, 1 and 3
		huge.then().statusCode(200).body("position", equalTo(3));
		negative.then().statusCode(200).body("position", equalTo(1));
		zero.then().statusCode(200).body("position", equalTo(1));
		four.then().statusCode(200).body("position", equalTo(3));
		// and the final order of "Main" is "A", "B", "C" without gaps
		assertEquals(List.of("A", "B", "C"), titles(main));
		assertEquals(List.of(1, 2, 3), partPositions(main));
		// and the last three responses are 200 with position 1
		for (Response response : List.of(seven, onlyZero, onlyNegative)) {
			response.then().statusCode(200).body("position", equalTo(1));
		}
		// and "Single" has exactly one part at position 1
		assertEquals(List.of(1), partPositions(single));
		// and the order and updatedAt T of the parts of "Sibling" and of teacher B's lesson are unchanged
		assertEquals(siblingBefore, storedParts(sibling));
		assertEquals(foreignBefore, storedParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R4-S4] The position must be an integer")
	void thePositionMustBeAnInteger() {
		// given a logged-in teacher A with a lesson with the parts "A" and "B"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID a = createPart(teacherA, lesson, "A", 1);
		createPart(teacherA, lesson, "B", 2);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A moves "A" with the body {}, with {"position": null}, with the position "2", with the position "", with {"position": {"n": 2}}, with the positions 1.5, 2.0 and 1e2, with the position true, and with the bodies {"position": and no body
		String path = position(course, lesson, a);
		var empty = teacher.patch(path, Map.of());
		var nullPosition = teacher.patch(path, position(null));
		var string = teacher.patch(path, position("2"));
		var emptyString = teacher.patch(path, position(""));
		var object = teacher.patch(path, position(Map.of("n", 2)));
		var fraction = teacher.patch(path, "{\"position\": 1.5}");
		var float2 = teacher.patch(path, "{\"position\": 2.0}");
		var exponent = teacher.patch(path, "{\"position\": 1e2}");
		var bool = teacher.patch(path, position(true));
		var truncated = teacher.patch(path, "{\"position\":");
		var noBody = teacher.patch(path);

		// then the first and the second responses are 400 REQUIRED
		for (Response response : List.of(empty, nullPosition)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("position", "REQUIRED"))));
		}
		// and the third to the ninth are 400 INVALID_TYPE
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
		// and the order is still "A", "B"
		assertEquals(List.of("A", "B"), titles(lesson));
	}

	@Test
	@DisplayName("[parts/R4-S5] Another teacher's, unknown, malformed and misplaced ids are not found")
	void anotherTeachersUnknownMalformedAndMisplacedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves", "Quarters" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID halves = createPart(teacherA, fractions, "Halves", 1);
		createPart(teacherA, fractions, "Quarters", 2);
		UUID tenths = createPart(teacherA, decimals, "Tenths", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");
		var fractionsBefore = storedParts(fractions);
		var decimalsBefore = storedParts(decimals);
		var foreignBefore = storedParts(foreignLesson);

		// when teacher A moves each to position 1: twelve parts under the ids listed in the scenario
		List<Response> responses = List.of(
				teacher.patch(position(foreignCourse, foreignLesson, foreignPart), position(1)),
				teacher.patch(position(english, foreignLesson, foreignPart), position(1)),
				teacher.patch(position(maths, fractions, foreignPart), position(1)),
				teacher.patch(position(foreignCourse, foreignLesson, halves), position(1)),
				teacher.patch(position(maths, fractions, UUID.randomUUID()), position(1)),
				teacher.patch(position(maths, fractions, "not-a-uuid"), position(1)),
				teacher.patch(position(maths, fractions, tenths), position(1)),
				teacher.patch(position(english, fractions, halves), position(1)),
				teacher.patch(position(maths, "not-a-uuid", halves), position(1)),
				teacher.patch(position(maths, UUID.randomUUID(), halves), position(1)),
				teacher.patch(position("not-a-uuid", fractions, halves), position(1)),
				teacher.patch(position(UUID.randomUUID(), fractions, halves), position(1)));

		// then all responses are 404 NOT_FOUND with no errors property, none contains "Грамматика", "Падежи" or "Разминка"
		responses.forEach(this::assertNotFound);
		// and the orders and updatedAt of all three lessons are unchanged
		assertEquals(fractionsBefore, storedParts(fractions));
		assertEquals(decimalsBefore, storedParts(decimals));
		assertEquals(foreignBefore, storedParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R4-S6] Only active teachers move parts")
	void onlyActiveTeachersMoveParts() {
		// given a lesson of teacher A with the parts "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		createPart(teacherA, lesson, "One", 1);
		UUID two = createPart(teacherA, lesson, "Two", 2);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
		String path = position(course, lesson, two);
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
		assertEquals(List.of("One", "Two"), titles(lesson));
	}

	@Test
	@DisplayName("[parts/R4-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a course C with a lesson L that has the part P, a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка", a logged-in admin, and a logged-in teacher A2 who is then deactivated
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID courseC = createCourse(teacherA, "English B1", "en", "en");
		UUID lessonL = createLesson(teacherA, courseC, "Present Simple", 1);
		UUID partP = createPart(teacherA, lessonL, "Part", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		UUID foreignPart = createPart(teacherB, foreignLesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();
		var foreignBefore = storedParts(foreignLesson);

		// when teacher A sends the body {} for (1) the part "not-a-uuid" under C and L, (2) teacher B's part "Разминка" under teacher B's course and lesson, (3) a part with an unknown id under C and L, and (4) P under C and the lesson "not-a-uuid"; and the admin and the deactivated teacher A2 each move the part "not-a-uuid" to position 1 under the lesson "not-a-uuid" of the course "not-a-uuid"
		var malformed = teacher.patch(position(courseC, lessonL, "not-a-uuid"), Map.of());
		var foreignResponse = teacher.patch(position(foreignCourse, foreignLesson, foreignPart), Map.of());
		var unknown = teacher.patch(position(courseC, lessonL, UUID.randomUUID()), Map.of());
		var badLesson = teacher.patch(position(courseC, "not-a-uuid", partP), Map.of());
		var adminResponse = admin.patch(position("not-a-uuid", "not-a-uuid", "not-a-uuid"), position(1));
		var deactivatedResponse = deactivated.patch(position("not-a-uuid", "not-a-uuid", "not-a-uuid"), position(1));

		// then teacher A's first response is 404 NOT_FOUND with no errors property
		malformed.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second and the third are 400 VALIDATION_FAILED for the position and do not contain "Падежи" or "Разминка"
		for (Response response : List.of(foreignResponse, unknown)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("position", "REQUIRED"))))
					.body(not(containsString("Падежи")))
					.body(not(containsString("Разминка")));
		}
		// and the fourth is 404 NOT_FOUND
		badLesson.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		// and the admin's response is 403 FORBIDDEN and the deactivated teacher's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher B's part is unchanged
		assertEquals(foreignBefore, storedParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R4-S8] Parallel moves keep positions distinct (the teacher, course and lesson row locks serialise them)")
	void parallelMovesKeepPositionsDistinct() throws Exception {
		// given a logged-in teacher A with a lesson with 10 parts at the positions 1 to 10, whose ids are read in GIVEN
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		List<UUID> parts = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			parts.add(createPart(teacherA, lesson, "Part " + i, i));
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends 10 move requests at the same time, each moving a different part, addressed by its id, to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
		int[] targets = {5, 1, 10, 3, 7, 2, 9, 4, 8, 6};
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			TestClient client = clientA.snapshot();
			UUID part = parts.get(i);
			int target = targets[i];
			calls.add(() -> client.patch(position(course, lesson, part), position(target)));
		}
		List<Response> responses = Parallel.run(calls);

		// then every response is 200
		for (Response response : responses) {
			response.then().statusCode(200);
		}
		// and the lesson has exactly 10 parts with the positions 1 to 10 without gaps and without duplicates
		assertEquals(10, countParts(lesson));
		assertEquals(IntStream.rangeClosed(1, 10).boxed().toList(), partPositions(lesson));
	}
}
