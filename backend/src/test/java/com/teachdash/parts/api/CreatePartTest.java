package com.teachdash.parts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** parts R1. */
class CreatePartTest extends ApiTest {

	private static String parts(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts";
	}

	private static Map<String, Object> title(Object title) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		return body;
	}

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private record PartRow(
			UUID teacherId,
			String title,
			int position,
			Instant createdAt,
			Instant updatedAt) {
	}

	private List<PartRow> storedParts(UUID lessonId) {
		return jdbc.sql("""
				select teacher_id, title, position, created_at, updated_at
				from parts
				where lesson_id = :lesson
				order by position
				""")
				.param("lesson", lessonId)
				.query((rs, i) -> new PartRow(
						rs.getObject("teacher_id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private Instant lessonUpdatedAt(UUID lessonId) {
		return jdbc.sql("select updated_at from lessons where id = :id")
				.param("id", lessonId)
				.query((rs, i) -> rs.getTimestamp("updated_at").toInstant())
				.single();
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")));
	}

	@Test
	@DisplayName("[parts/R1-S1] Create an English part")
	void createAnEnglishPart() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a part in it with the title "Warm-up"
		var response = teacher.post(parts(course, lesson), title("Warm-up"));

		// then the response is 201 with a new id, the title, position 1 and a createdAt time
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("title", equalTo("Warm-up"))
				.body("position", equalTo(1))
				.body("createdAt", notNullValue());
		// and updatedAt equals createdAt
		assertEquals(response.<String>path("createdAt"), response.<String>path("updatedAt"));
		// and exactly these properties are returned
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"),
				response.<Map<String, Object>>path("$").keySet());
		// and the part is stored owned by teacher A in that lesson at position 1
		var stored = storedParts(lesson);
		assertEquals(1, stored.size());
		assertEquals(teacherA, stored.get(0).teacherId());
		assertEquals("Warm-up", stored.get(0).title());
		assertEquals(1, stored.get(0).position());
	}

	@Test
	@DisplayName("[parts/R1-S2] Create a Russian part")
	void createARussianPart() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a part in it with the title "Разминка: именительный, родительный?"
		var response = teacher.post(parts(course, lesson), title("Разминка: именительный, родительный?"));

		// then the response is 201 with exactly that title and position 1
		response.then().statusCode(201)
				.body("title", equalTo("Разминка: именительный, родительный?"))
				.body("position", equalTo(1));
		// and the stored part has the same title
		assertEquals(List.of("Разминка: именительный, родительный?"),
				storedParts(lesson).stream().map(PartRow::title).toList());
	}

	@Test
	@DisplayName("[parts/R1-S3] Parts are added at the end, per lesson")
	void partsAreAddedAtTheEndPerLesson() {
		// given a logged-in teacher A with a course with the lessons "Grammar" and "Reading", where "Grammar" already has the parts "One" and "Two" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID grammar = createLesson(teacherA, course, "Grammar", 1);
		UUID reading = createLesson(teacherA, course, "Reading", 2);
		createPart(teacherA, grammar, "One", 1);
		createPart(teacherA, grammar, "Two", 2);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A creates the part "Three" in "Grammar" and the part "First" in "Reading"
		var three = teacher.post(parts(course, grammar), title("Three"));
		var first = teacher.post(parts(course, reading), title("First"));

		// then the responses are 201 with position 3 and position 1
		three.then().statusCode(201).body("position", equalTo(3));
		first.then().statusCode(201).body("position", equalTo(1));
		// and "Grammar" has the positions 1, 2, 3 without gaps
		assertEquals(List.of(1, 2, 3), partPositions(grammar));
		// and the parts "One" and "Two" keep their positions and their updatedAt T
		var stored = storedParts(grammar);
		assertEquals(List.of("One", "Two", "Three"), stored.stream().map(PartRow::title).toList());
		assertEquals(created, stored.get(0).updatedAt());
		assertEquals(created, stored.get(1).updatedAt());
		// and the updatedAt of the course and of both lessons is unchanged
		assertEquals(created, readCourse(course).updatedAt());
		assertEquals(created, lessonUpdatedAt(grammar));
		assertEquals(created, lessonUpdatedAt(reading));
	}

	@Test
	@DisplayName("[parts/R1-S4] The title is normalised")
	void theTitleIsNormalised() {
		// given a logged-in teacher A with a lesson
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a part with the title "  Грамматика​  " (ending in a zero-width space)
		var response = teacher.post(parts(course, lesson), title("  Грамматика​  "));

		// then the response is 201 with the title "Грамматика"
		response.then().statusCode(201).body("title", equalTo("Грамматика"));
		// and the stored part has the same title
		assertEquals(List.of("Грамматика"), storedParts(lesson).stream().map(PartRow::title).toList());
	}

	@Test
	@DisplayName("[parts/R1-S5] A title must have 1-200 characters and a letter or digit")
	void aTitleMustHave1To200CharactersAndALetterOrDigit() {
		// given a logged-in teacher A with a lesson
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates parts with each title "   ", 201 letters, "!!!", and 200 letters, with the body {} and {"title": null}, and with the bodies {"title": and no body
		var blank = teacher.post(parts(course, lesson), title("   "));
		var tooLong = teacher.post(parts(course, lesson), title("Я".repeat(201)));
		var noLetters = teacher.post(parts(course, lesson), title("!!!"));
		var longest = teacher.post(parts(course, lesson), title("Я".repeat(200)));
		var empty = teacher.post(parts(course, lesson), Map.of());
		var nullTitle = teacher.post(parts(course, lesson), title(null));
		var truncated = teacher.post(parts(course, lesson), "{\"title\":");
		var noBody = teacher.post(parts(course, lesson));

		// then the first, the fifth and the sixth responses are 400 REQUIRED
		for (Response response : List.of(blank, empty, nullTitle)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		}
		// and the second is TOO_LONG
		tooLong.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "TOO_LONG"))));
		// and the third is INVALID_FORMAT
		noLetters.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "INVALID_FORMAT"))));
		// and the fourth is 201
		longest.then().statusCode(201);
		// and the seventh and the eighth are 400 MALFORMED_REQUEST with no errors property
		for (Response response : List.of(truncated, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and the lesson has exactly one part
		assertEquals(1, countParts(lesson));
	}

	@Test
	@DisplayName("[parts/R1-S6] Wrong JSON types and a number or a boolean are rejected")
	void wrongJsonTypesAndANumberOrABooleanAreRejected() {
		// given a logged-in teacher A with a lesson
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a part with the title ["Grammar"], then 123, then true
		var list = teacher.post(parts(course, lesson), title(List.of("Grammar")));
		var number = teacher.post(parts(course, lesson), title(123));
		var bool = teacher.post(parts(course, lesson), title(true));

		// then every response is 400 with INVALID_TYPE for the title
		for (Response response : List.of(list, number, bool)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))));
		}
		// and no body has a rejectedValue
		for (Response response : List.of(list, number, bool)) {
			response.then().body(not(containsString("rejectedValue")));
		}
		// and the lesson has no part
		assertEquals(0, countParts(lesson));
	}

	@Test
	@DisplayName("[parts/R1-S7] Titles are not unique")
	void titlesAreNotUnique() {
		// given a logged-in teacher A with the lessons "Grammar" and "Reading", where "Grammar" has the part "Warm-up"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID grammar = createLesson(teacherA, course, "Grammar", 1);
		UUID reading = createLesson(teacherA, course, "Reading", 2);
		createPart(teacherA, grammar, "Warm-up", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates the parts "Warm-up" and "WARM-UP" in "Grammar", and "Warm-up" in "Reading"
		var same = teacher.post(parts(course, grammar), title("Warm-up"));
		var upper = teacher.post(parts(course, grammar), title("WARM-UP"));
		var other = teacher.post(parts(course, reading), title("Warm-up"));

		// then all three responses are 201
		for (Response response : List.of(same, upper, other)) {
			response.then().statusCode(201);
		}
		// and "Grammar" has three parts at the positions 1, 2, 3
		assertEquals(List.of(1, 2, 3), partPositions(grammar));
	}

	@Test
	@DisplayName("[parts/R1-S8] At most 20 parts per lesson")
	void atMost20PartsPerLesson() {
		// given a logged-in teacher A with a lesson "Full" with 20 parts and a lesson "Other" with 19 parts
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID full = createLesson(teacherA, course, "Full", 1);
		UUID other = createLesson(teacherA, course, "Other", 2);
		for (int i = 1; i <= 20; i++) {
			createPart(teacherA, full, "Part " + i, i);
		}
		for (int i = 1; i <= 19; i++) {
			createPart(teacherA, other, "Part " + i, i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates the part "Part 21" in "Full", then a part with a blank title in "Full", and then the part "Part 20" in "Other"
		var over = teacher.post(parts(course, full), title("Part 21"));
		var blank = teacher.post(parts(course, full), title(" "));
		var last = teacher.post(parts(course, other), title("Part 20"));

		// then the first response is 409 PART_LIMIT_REACHED with no errors property
		over.then().statusCode(409)
				.body("code", equalTo("PART_LIMIT_REACHED"))
				.body("$", not(hasKey("errors")));
		// and the second is a validation error for the title
		blank.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		// and the third is 201 with position 20
		last.then().statusCode(201).body("position", equalTo(20));
		// and both lessons have 20 parts
		assertEquals(20, countParts(full));
		assertEquals(20, countParts(other));
	}

	@Test
	@DisplayName("[parts/R1-S9] Parallel requests keep positions distinct and the limit")
	void parallelRequestsKeepPositionsDistinctAndTheLimit() throws Exception {
		// given a logged-in teacher A with a lesson with 17 parts
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		for (int i = 1; i <= 17; i++) {
			createPart(teacherA, lesson, "Part " + i, i);
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends 6 create requests at the same time with the titles "New 1" to "New 6"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			TestClient client = clientA.snapshot();
			String title = "New " + i;
			calls.add(() -> client.post(parts(course, lesson), title(title)));
		}
		List<Response> responses = Parallel.run(calls);

		// then exactly 3 responses are 201 with the distinct positions 18, 19 and 20
		List<Response> created = responses.stream().filter(r -> r.statusCode() == 201).toList();
		assertEquals(3, created.size());
		assertEquals(Set.of(18, 19, 20), Set.copyOf(created.stream().map(r -> r.<Integer>path("position")).toList()));
		// and the other 3 are 409 PART_LIMIT_REACHED
		assertEquals(3, responses.stream()
				.filter(r -> r.statusCode() == 409 && "PART_LIMIT_REACHED".equals(r.path("code")))
				.count());
		// and the lesson has exactly 20 parts with the positions 1 to 20 without gaps
		assertEquals(20, countParts(lesson));
		assertEquals(IntStream.rangeClosed(1, 20).boxed().toList(), partPositions(lesson));
	}

	@Test
	@DisplayName("[parts/R1-S10] Another teacher's, unknown, malformed and wrong-course ids are not found")
	void anotherTeachersUnknownMalformedAndWrongCourseIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with a lesson "Падежи" that has 20 parts
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		for (int i = 1; i <= 20; i++) {
			createPart(teacherB, foreignLesson, "Part " + i, i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a part with the title "Intruder" in teacher B's lesson (under teacher B's course, and under "English B1"), in a lesson with an unknown id, in the lesson "not-a-uuid", in "Fractions" under "English B1", in "Fractions" under teacher B's course, and in "Fractions" under the course "not-a-uuid" and under an unknown course id
		List<Response> responses = List.of(
				teacher.post(parts(foreignCourse, foreignLesson), title("Intruder")),
				teacher.post(parts(english, foreignLesson), title("Intruder")),
				teacher.post(parts(maths, UUID.randomUUID()), title("Intruder")),
				teacher.post(parts(maths, "not-a-uuid"), title("Intruder")),
				teacher.post(parts(english, fractions), title("Intruder")),
				teacher.post(parts(foreignCourse, fractions), title("Intruder")),
				teacher.post(parts("not-a-uuid", fractions), title("Intruder")),
				teacher.post(parts(UUID.randomUUID(), fractions), title("Intruder")));

		// then all responses are 404 NOT_FOUND with no errors property, and none contains "Грамматика" or "Падежи"
		for (Response response : responses) {
			assertNotFound(response);
		}
		// and teacher B's lesson still has exactly 20 parts
		assertEquals(20, countParts(foreignLesson));
		// and "Fractions" has no part
		assertEquals(0, countParts(fractions));
	}

	@Test
	@DisplayName("[parts/R1-S11] Only active teachers create parts")
	void onlyActiveTeachersCreateParts() {
		// given a lesson of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each create a part in the lesson with the title "Part", and teacher B does so without a CSRF token
		var adminResponse = admin.post(parts(course, lesson), title("Part"));
		var studentResponse = student.post(parts(course, lesson), title("Part"));
		var deactivatedResponse = deactivated.post(parts(course, lesson), title("Part"));
		var anonymousResponse = anonymous.post(parts(course, lesson), title("Part"));
		var csrfResponse = withoutCsrf.post(parts(course, lesson), title("Part"));

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
		// and the lesson has no part
		assertEquals(0, countParts(lesson));
	}

	@Test
	@DisplayName("[parts/R1-S12] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a lesson, a lesson "Падежи" of teacher B, a logged-in admin, and a logged-in teacher A2 who is then deactivated
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient admin = loggedInAdmin();
		createTeacher("a2@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a2@example.com");
		jdbc.sql("update users set active = false where email = 'a2@example.com'").update();

		// when teacher A creates a part with a blank title in the lesson "not-a-uuid", then a part with a blank title in teacher B's lesson, then a part with a blank title in the lesson of a course "not-a-uuid"; and the admin and the deactivated teacher A2 each create a part with a blank title in the lesson "not-a-uuid" of the course "not-a-uuid"
		var malformedLesson = teacher.post(parts(course, "not-a-uuid"), title(" "));
		var foreignResponse = teacher.post(parts(foreignCourse, foreignLesson), title(" "));
		var malformedCourse = teacher.post(parts("not-a-uuid", lesson), title(" "));
		var adminResponse = admin.post(parts("not-a-uuid", "not-a-uuid"), title(" "));
		var deactivatedResponse = deactivated.post(parts("not-a-uuid", "not-a-uuid"), title(" "));

		// then teacher A's first response is 404 NOT_FOUND with no errors property
		malformedLesson.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED for the title and does not contain "Падежи"
		foreignResponse.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))))
				.body(not(containsString("Падежи")));
		// and the third is 404 NOT_FOUND
		malformedCourse.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		// and the admin's response is 403 FORBIDDEN and the deactivated teacher's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher B's lesson has no new part
		assertEquals(0, countParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R1-S13] Creating parts while the lesson is deleted")
	void creatingPartsWhileTheLessonIsDeleted() throws Exception {
		// given a logged-in teacher A with a lesson with 5 parts
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		for (int i = 1; i <= 5; i++) {
			createPart(teacherA, lesson, "Part " + i, i);
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when 1 request deleting the lesson and 4 requests creating a part in it are sent at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		TestClient deleter = clientA.snapshot();
		calls.add(() -> deleter.delete("/api/teacher/courses/" + course + "/lessons/" + lesson));
		for (int i = 1; i <= 4; i++) {
			TestClient client = clientA.snapshot();
			String title = "New " + i;
			calls.add(() -> client.post(parts(course, lesson), title(title)));
		}
		List<Response> responses = Parallel.run(calls);

		// then the delete response is 204
		responses.get(0).then().statusCode(204);
		// and each create response is 201 or 404 and none is 500
		for (Response response : responses.subList(1, 5)) {
			assertEquals(true, response.statusCode() == 201 || response.statusCode() == 404);
		}
		// and the lesson is not stored
		assertEquals(0, countLessons(course));
		// and no part row references it
		assertEquals(0, countParts(lesson));
	}
}
