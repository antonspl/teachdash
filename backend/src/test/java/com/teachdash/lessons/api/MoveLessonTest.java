package com.teachdash.lessons.api;

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

/** lessons R4. */
class MoveLessonTest extends ApiTest {

	private static String position(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/position";
	}

	private static Map<String, Object> position(Object position) {
		Map<String, Object> body = new HashMap<>();
		body.put("position", position);
		return body;
	}

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private record LessonRow(
			UUID id,
			String title,
			int position,
			Instant updatedAt) {
	}

	private List<LessonRow> storedLessons(UUID courseId) {
		return jdbc.sql("""
				select id, title, position, updated_at
				from lessons
				where course_id = :course
				order by position
				""")
				.param("course", courseId)
				.query((rs, i) -> new LessonRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private List<String> titles(UUID courseId) {
		return storedLessons(courseId).stream().map(LessonRow::title).toList();
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")))
				.body(not(containsString("Роды")));
	}

	@Test
	@DisplayName("[lessons/R4-S1] Move a lesson down and up")
	void moveALessonDownAndUp() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru") with the lessons "Один", "Два", "Три" and "Четыре" at the positions 1 to 4 created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID one = createLesson(teacherA, course, "Один", 1);
		createLesson(teacherA, course, "Два", 2);
		createLesson(teacherA, course, "Три", 3);
		UUID four = createLesson(teacherA, course, "Четыре", 4);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));
		Instant later = created.plus(Duration.ofHours(1));

		// when teacher A moves "Один" to position 3, and then moves "Четыре" to position 1
		var first = teacher.patch(position(course, one), position(3));
		var second = teacher.patch(position(course, four), position(1));

		// then the first response is 200 with the title "Один", position 3 and updatedAt T plus one hour
		first.then().statusCode(200).body("title", equalTo("Один")).body("position", equalTo(3));
		assertEquals(later, Instant.parse(first.path("updatedAt")));
		// and the second is 200 with the title "Четыре", position 1 and updatedAt T plus one hour
		second.then().statusCode(200).body("title", equalTo("Четыре")).body("position", equalTo(1));
		assertEquals(later, Instant.parse(second.path("updatedAt")));
		// and the final order is "Четыре", "Два", "Три", "Один" at the positions 1, 2, 3, 4
		assertEquals(List.of("Четыре", "Два", "Три", "Один"), titles(course));
		assertEquals(List.of(1, 2, 3, 4), lessonPositions(course));
		// and the lessons "Два" and "Три" have updatedAt T
		var stored = storedLessons(course);
		assertEquals(created, stored.get(1).updatedAt());
		assertEquals(created, stored.get(2).updatedAt());
		// and the updatedAt of the course is unchanged
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[lessons/R4-S2] Moving to the same position bumps updatedAt")
	void movingToTheSamePositionBumpsUpdatedAt() {
		// given a logged-in teacher A with a course "English B1" with the lessons "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "One", 1);
		UUID two = createLesson(teacherA, course, "Two", 2);
		createLesson(teacherA, course, "Three", 3);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A moves "Two" to position 2
		var response = teacher.patch(position(course, two), position(2));

		// then the response is 200 with position 2 and updatedAt T plus one hour
		response.then().statusCode(200).body("position", equalTo(2));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and the order is unchanged, and "One" and "Three" have updatedAt T
		assertEquals(List.of("One", "Two", "Three"), titles(course));
		var stored = storedLessons(course);
		assertEquals(created, stored.get(0).updatedAt());
		assertEquals(created, stored.get(2).updatedAt());
	}

	@Test
	@DisplayName("[lessons/R4-S3] Any integer is clamped to the course range")
	void anyIntegerIsClampedToTheCourseRange() {
		// given a logged-in teacher A with a course with the lessons "A", "B", "C" at the positions 1, 2, 3
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID a = createLesson(teacherA, course, "A", 1);
		createLesson(teacherA, course, "B", 2);
		UUID c = createLesson(teacherA, course, "C", 3);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A moves "A" to position 99999999999999999999, then moves "A" to position -5, then moves "C" to position 0, then moves "C" to position 4
		var huge = teacher.patch(position(course, a), position(new BigInteger("99999999999999999999")));
		var negative = teacher.patch(position(course, a), position(-5));
		var zero = teacher.patch(position(course, c), position(0));
		var past = teacher.patch(position(course, c), position(4));

		// then all four responses are 200 with the positions 3, 1, 1 and 3
		huge.then().statusCode(200).body("position", equalTo(3));
		negative.then().statusCode(200).body("position", equalTo(1));
		zero.then().statusCode(200).body("position", equalTo(1));
		past.then().statusCode(200).body("position", equalTo(3));
		// and the final order is "A", "B", "C" without gaps
		assertEquals(List.of("A", "B", "C"), titles(course));
		assertEquals(List.of(1, 2, 3), lessonPositions(course));
	}

	@Test
	@DisplayName("[lessons/R4-S4] The position must be an integer")
	void thePositionMustBeAnInteger() {
		// given a logged-in teacher A with a course with the lessons "A" and "B"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID a = createLesson(teacherA, course, "A", 1);
		createLesson(teacherA, course, "B", 2);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A moves "A" with the body {}, with {"position": null}, with the position "2", with the position "", with {"position": {"n": 2}}, with the positions 1.5, 2.0 and 1e2, with the position true, and with the bodies {"position": and no body
		String path = position(course, a);
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
		assertEquals(List.of("A", "B"), titles(course));
	}

	@Test
	@DisplayName("[lessons/R4-S5] Another teacher's, unknown, malformed and wrong-course ids are not found")
	void anotherTeachersUnknownMalformedAndWrongCourseIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths" and the lessons "Fractions" and "Decimals" in "Maths", and a course "Грамматика" of teacher B with the lessons "Падежи" and "Роды"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		createLesson(teacherA, maths, "Fractions", 1);
		UUID decimals = createLesson(teacherA, maths, "Decimals", 2);
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		createLesson(teacherB, foreign, "Падежи", 1);
		UUID rody = createLesson(teacherB, foreign, "Роды", 2);
		TestClient teacher = loggedInAs("a@example.com");
		var foreignBefore = storedLessons(foreign);
		var mathsBefore = storedLessons(maths);
		var englishBefore = storedLessons(english);

		// when teacher A moves teacher B's lesson "Роды" to position 1 (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", "Decimals" under "English B1", and "Decimals" under the course "not-a-uuid" and under an unknown course id
		List<Response> responses = List.of(
				teacher.patch(position(foreign, rody), position(1)),
				teacher.patch(position(english, rody), position(1)),
				teacher.patch(position(english, UUID.randomUUID()), position(1)),
				teacher.patch(position(english, "not-a-uuid"), position(1)),
				teacher.patch(position(english, decimals), position(1)),
				teacher.patch(position("not-a-uuid", decimals), position(1)),
				teacher.patch(position(UUID.randomUUID(), decimals), position(1)));

		// then all responses are 404 NOT_FOUND with no errors property and none contains "Грамматика", "Падежи" or "Роды"
		responses.forEach(this::assertNotFound);
		// and the orders and updatedAt of both courses are unchanged
		assertEquals(foreignBefore, storedLessons(foreign));
		assertEquals(mathsBefore, storedLessons(maths));
		assertEquals(englishBefore, storedLessons(english));
	}

	@Test
	@DisplayName("[lessons/R4-S6] Only active teachers move lessons")
	void onlyActiveTeachersMoveLessons() {
		// given a course of teacher A with the lessons "One" and "Two", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "One", 1);
		UUID two = createLesson(teacherA, course, "Two", 2);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each move "Two" to position 1, and teacher B does so without a CSRF token
		String path = position(course, two);
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
		assertEquals(List.of("One", "Two"), titles(course));
	}

	@Test
	@DisplayName("[lessons/R4-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher C with a course
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		UUID teacherC = createTeacher("c@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherC, "English B1", "en", "en");
		createLesson(teacherC, course, "Lesson", 1);
		TestClient teacher = loggedInAs("c@example.com");

		// when the admin moves the lesson "not-a-uuid" to position 1, teacher A sends the body {} for it, and teacher C sends the body {} for the lesson "not-a-uuid" and then for an unknown lesson id, both in her own course
		var adminResponse = admin.patch(position(course, "not-a-uuid"), position(1));
		var deactivatedResponse = deactivated.patch(position(course, "not-a-uuid"), Map.of());
		var malformed = teacher.patch(position(course, "not-a-uuid"), Map.of());
		var unknown = teacher.patch(position(course, UUID.randomUUID()), Map.of());

		// then the admin's response is 403 FORBIDDEN
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		// and teacher A's is 401 UNAUTHENTICATED
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher C's first is 404 NOT_FOUND with no errors property
		malformed.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED for the position
		unknown.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("position", "REQUIRED"))));
	}

	@Test
	@DisplayName("[lessons/R4-S8] Parallel moves keep positions dense")
	void parallelMovesKeepPositionsDense() throws Exception {
		// given a logged-in teacher A with a course with 10 lessons at the positions 1 to 10
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		List<UUID> lessons = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			lessons.add(createLesson(teacherA, course, "Lesson " + i, i));
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends 10 move requests at the same time, each moving a different lesson to position 5, 1, 10, 3, 7, 2, 9, 4, 8 or 6
		int[] targets = {5, 1, 10, 3, 7, 2, 9, 4, 8, 6};
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			TestClient client = clientA.snapshot();
			UUID lesson = lessons.get(i);
			int target = targets[i];
			calls.add(() -> client.patch(position(course, lesson), position(target)));
		}
		List<Response> responses = Parallel.run(calls);

		// then every response is 200
		for (Response response : responses) {
			response.then().statusCode(200);
		}
		// and the course has exactly 10 lessons with the positions 1 to 10 without gaps and without duplicates
		assertEquals(10, countLessons(course));
		assertEquals(IntStream.rangeClosed(1, 10).boxed().toList(), lessonPositions(course));
	}

	@Test
	@DisplayName("[lessons/R4-S9] A single lesson stays at position 1")
	void aSingleLessonStaysAtPosition1() {
		// given a logged-in teacher A with a course with the one lesson "Only" at position 1
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID only = createLesson(teacherA, course, "Only", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A moves it to position 7, then to 0, then to -3
		var seven = teacher.patch(position(course, only), position(7));
		var zero = teacher.patch(position(course, only), position(0));
		var negative = teacher.patch(position(course, only), position(-3));

		// then all three responses are 200 with position 1
		for (Response response : List.of(seven, zero, negative)) {
			response.then().statusCode(200).body("position", equalTo(1));
		}
		// and the course has exactly one lesson at position 1
		assertEquals(List.of(1), lessonPositions(course));
	}

	@Test
	@DisplayName("[lessons/R4-S10] Input is validated before a foreign or unknown lesson is looked up")
	void inputIsValidatedBeforeAForeignOrUnknownLessonIsLookedUp() {
		// given teacher A logged in, and a course "Грамматика" of teacher B with the lesson "Падежи"
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreign, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");
		var foreignBefore = storedLessons(foreign);

		// when teacher A moves teacher B's lesson with the body {}, and moves a lesson with an unknown id in teacher B's course with the position "2"
		var empty = teacher.patch(position(foreign, foreignLesson), Map.of());
		var string = teacher.patch(position(foreign, UUID.randomUUID()), position("2"));

		// then the first response is 400 VALIDATION_FAILED with REQUIRED for the position
		empty.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("position", "REQUIRED"))));
		// and the second is 400 with INVALID_TYPE for the position
		string.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("position", "INVALID_TYPE"))));
		// and neither contains "Грамматика" or "Падежи"
		for (Response response : List.of(empty, string)) {
			response.then().body(not(containsString("Грамматика"))).body(not(containsString("Падежи")));
		}
		// and teacher B's course is unchanged
		assertEquals(foreignBefore, storedLessons(foreign));
	}
}
