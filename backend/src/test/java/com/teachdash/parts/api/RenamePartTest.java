package com.teachdash.parts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** parts R3. */
class RenamePartTest extends ApiTest {

	private static String part(Object courseId, Object lessonId, Object partId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts/" + partId;
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
			UUID id,
			String title,
			int position,
			Instant createdAt,
			Instant updatedAt) {
	}

	private List<PartRow> storedParts(UUID lessonId) {
		return jdbc.sql("""
				select id, title, position, created_at, updated_at
				from parts
				where lesson_id = :lesson
				order by position
				""")
				.param("lesson", lessonId)
				.query((rs, i) -> new PartRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private static List<String> titles(List<PartRow> rows) {
		return rows.stream().map(PartRow::title).toList();
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
	@DisplayName("[parts/R3-S1] Rename an English part, also to the same title, titles are not unique")
	void renameAnEnglishPartAlsoToTheSameTitleTitlesAreNotUnique() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lesson "Present Simple" that has the parts "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		createPart(teacherA, lesson, "One", 1);
		UUID two = createPart(teacherA, lesson, "Two", 2);
		UUID three = createPart(teacherA, lesson, "Three", 3);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames the part "Two" to "Warm-up", then advances the clock by one more hour and renames it to "Warm-up" again, and then renames the part "Three" to "Warm-up"
		var first = teacher.patch(part(course, lesson, two), title("Warm-up"));
		testClock().advance(Duration.ofHours(1));
		var second = teacher.patch(part(course, lesson, two), title("Warm-up"));
		var third = teacher.patch(part(course, lesson, three), title("Warm-up"));

		// then the first response is 200 with the same id, the title "Warm-up", position 2, createdAt T and updatedAt T plus one hour
		first.then().statusCode(200)
				.body("id", equalTo(two.toString()))
				.body("title", equalTo("Warm-up"))
				.body("position", equalTo(2));
		assertEquals(created, Instant.parse(first.path("createdAt")));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(first.path("updatedAt")));
		// and exactly the properties id, title, position, createdAt and updatedAt
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"),
				first.<Map<String, Object>>path("$").keySet());
		// and the second is 200 with updatedAt T plus two hours
		second.then().statusCode(200);
		assertEquals(created.plus(Duration.ofHours(2)), Instant.parse(second.path("updatedAt")));
		// and the third is 200 with the title "Warm-up" and position 3
		third.then().statusCode(200)
				.body("title", equalTo("Warm-up"))
				.body("position", equalTo(3));
		// and the lesson has two parts titled "Warm-up" at the positions 2 and 3
		var stored = storedParts(lesson);
		assertEquals(List.of("One", "Warm-up", "Warm-up"), titles(stored));
		assertEquals(List.of(1, 2, 3), stored.stream().map(PartRow::position).toList());
		// and the part "One" keeps its title, position and updatedAt T
		assertEquals(created, stored.get(0).updatedAt());
		// and the updatedAt of the lesson and of the course is unchanged
		assertEquals(created, readLessonUpdatedAt(lesson));
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[parts/R3-S2] Rename a Russian part, the title is normalised")
	void renameARussianPartTheTitleIsNormalised() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи" that has the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID part = createPart(teacherA, lesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A renames the part to "  Склонение имён​  " (ending in a zero-width space)
		var response = teacher.patch(part(course, lesson, part), title("  Склонение имён​  "));

		// then the response is 200 with the title "Склонение имён"
		response.then().statusCode(200).body("title", equalTo("Склонение имён"));
		// and the stored part has the same title
		assertEquals(List.of("Склонение имён"), titles(storedParts(lesson)));
	}

	@Test
	@DisplayName("[parts/R3-S3] A title must have 1-200 characters and a letter or digit, wrong types are rejected")
	void aTitleMustHave1To200CharactersAndALetterOrDigitWrongTypesAreRejected() {
		// given a logged-in teacher A with a lesson with the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		UUID part = createPart(teacherA, lesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A renames it to each title "   ", 201 Cyrillic letters, "!!!" and 200 Cyrillic letters, with the body {} and {"title": null}, with the bodies {"title": and no body, and with the title ["Grammar"], then 123, then true
		String path = part(course, lesson, part);
		var blank = teacher.patch(path, title("   "));
		var tooLong = teacher.patch(path, title("Я".repeat(201)));
		var noLetters = teacher.patch(path, title("!!!"));
		var longest = teacher.patch(path, title("Я".repeat(200)));
		var empty = teacher.patch(path, Map.of());
		var nullTitle = teacher.patch(path, title(null));
		var truncated = teacher.patch(path, "{\"title\":");
		var noBody = teacher.patch(path);
		var list = teacher.patch(path, title(List.of("Grammar")));
		var number = teacher.patch(path, title(123));
		var bool = teacher.patch(path, title(true));

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
		// and the fourth is 200
		longest.then().statusCode(200);
		// and the seventh and the eighth are 400 MALFORMED_REQUEST with no errors property
		for (Response response : List.of(truncated, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and the ninth to the eleventh are 400 INVALID_TYPE for the title with no rejectedValue
		for (Response response : List.of(list, number, bool)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))))
					.body("errors[0]", not(hasKey("rejectedValue")));
		}
		// and the stored title is the 200-letter title
		assertEquals(List.of("Я".repeat(200)), titles(storedParts(lesson)));
	}

	@Test
	@DisplayName("[parts/R3-S4] Another teacher's, unknown, malformed and misplaced ids are not found")
	void anotherTeachersUnknownMalformedAndMisplacedIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths", the lessons "Fractions" and "Decimals" in "Maths" with the parts "Halves" in "Fractions" and "Tenths" in "Decimals", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
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
		TestClient teacher = loggedInAs("a@example.com");
		var fractionsBefore = storedParts(fractions);
		var decimalsBefore = storedParts(decimals);
		var foreignBefore = storedParts(foreignLesson);

		// when teacher A renames, each time to "Changed", twelve parts under the ids listed in the scenario
		List<Response> responses = List.of(
				teacher.patch(part(foreignCourse, foreignLesson, foreignPart), title("Changed")),
				teacher.patch(part(english, foreignLesson, foreignPart), title("Changed")),
				teacher.patch(part(maths, fractions, foreignPart), title("Changed")),
				teacher.patch(part(foreignCourse, foreignLesson, halves), title("Changed")),
				teacher.patch(part(maths, fractions, UUID.randomUUID()), title("Changed")),
				teacher.patch(part(maths, fractions, "not-a-uuid"), title("Changed")),
				teacher.patch(part(maths, fractions, tenths), title("Changed")),
				teacher.patch(part(english, fractions, halves), title("Changed")),
				teacher.patch(part(maths, "not-a-uuid", halves), title("Changed")),
				teacher.patch(part(maths, UUID.randomUUID(), halves), title("Changed")),
				teacher.patch(part("not-a-uuid", fractions, halves), title("Changed")),
				teacher.patch(part(UUID.randomUUID(), fractions, halves), title("Changed")));

		// then all responses are 404 NOT_FOUND with no errors property, none contains "Грамматика", "Падежи" or "Разминка"
		responses.forEach(this::assertNotFound);
		// and the titles and updatedAt of "Halves", "Tenths" and "Разминка" are unchanged
		assertEquals(fractionsBefore, storedParts(fractions));
		assertEquals(decimalsBefore, storedParts(decimals));
		assertEquals(foreignBefore, storedParts(foreignLesson));
	}

	@Test
	@DisplayName("[parts/R3-S5] Only active teachers rename parts")
	void onlyActiveTeachersRenameParts() {
		// given a lesson of teacher A with the part "Part", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		UUID part = createPart(teacherA, lesson, "Part", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each rename the part to "Changed", and teacher B does so without a CSRF token
		String path = part(course, lesson, part);
		var adminResponse = admin.patch(path, title("Changed"));
		var studentResponse = student.patch(path, title("Changed"));
		var deactivatedResponse = deactivated.patch(path, title("Changed"));
		var anonymousResponse = anonymous.patch(path, title("Changed"));
		var csrfResponse = withoutCsrf.patch(path, title("Changed"));

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
		// and the part is still titled "Part"
		assertEquals(List.of("Part"), titles(storedParts(lesson)));
	}

	@Test
	@DisplayName("[parts/R3-S6] Checks run in a fixed order")
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

		// when teacher A renames with a blank title (1) the part "not-a-uuid" under C and L, (2) teacher B's part under teacher B's course and lesson, (3) a part with an unknown id under C and L, and (4) P under C and the lesson "not-a-uuid"; and the admin and the deactivated teacher A2 each rename the part "not-a-uuid" with a blank title in the lesson "not-a-uuid" of the course "not-a-uuid"
		var malformed = teacher.patch(part(courseC, lessonL, "not-a-uuid"), title(" "));
		var foreignResponse = teacher.patch(part(foreignCourse, foreignLesson, foreignPart), title(" "));
		var unknown = teacher.patch(part(courseC, lessonL, UUID.randomUUID()), title(" "));
		var badLesson = teacher.patch(part(courseC, "not-a-uuid", partP), title(" "));
		var adminResponse = admin.patch(part("not-a-uuid", "not-a-uuid", "not-a-uuid"), title(" "));
		var deactivatedResponse = deactivated.patch(part("not-a-uuid", "not-a-uuid", "not-a-uuid"), title(" "));

		// then teacher A's first response is 404 NOT_FOUND with no errors property
		malformed.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second and the third are 400 VALIDATION_FAILED for the title and do not contain "Падежи" or "Разминка"
		for (Response response : List.of(foreignResponse, unknown)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "REQUIRED"))))
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
}
