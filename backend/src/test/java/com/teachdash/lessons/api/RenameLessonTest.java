package com.teachdash.lessons.api;

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

/** lessons R3. */
class RenameLessonTest extends ApiTest {

	private static String lesson(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId;
	}

	private static Map<String, Object> title(Object title) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		return body;
	}

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private record LessonRow(
			UUID id,
			String title,
			int position,
			Instant createdAt,
			Instant updatedAt) {
	}

	private List<LessonRow> storedLessons(UUID courseId) {
		return jdbc.sql("""
				select id, title, position, created_at, updated_at
				from lessons
				where course_id = :course
				order by position
				""")
				.param("course", courseId)
				.query((rs, i) -> new LessonRow(
						rs.getObject("id", UUID.class),
						rs.getString("title"),
						rs.getInt("position"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")));
	}

	@Test
	@DisplayName("[lessons/R3-S1] Rename an English lesson")
	void renameAnEnglishLesson() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en") with the lessons "One", "Two" and "Three" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "One", 1);
		UUID two = createLesson(teacherA, course, "Two", 2);
		createLesson(teacherA, course, "Three", 3);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames the lesson "Two" to "Present Simple"
		var response = teacher.patch(lesson(course, two), title("Present Simple"));

		// then the response is 200 with the same id, the title "Present Simple", position 2, createdAt T and updatedAt T plus one hour
		response.then().statusCode(200)
				.body("id", equalTo(two.toString()))
				.body("title", equalTo("Present Simple"))
				.body("position", equalTo(2));
		assertEquals(created, Instant.parse(response.path("createdAt")));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and exactly the properties id, title, position, createdAt and updatedAt
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"),
				response.<Map<String, Object>>path("$").keySet());
		// and the stored title is "Present Simple", and "One" and "Three" keep their titles, positions and updatedAt T
		var stored = storedLessons(course);
		assertEquals(List.of("One", "Present Simple", "Three"), stored.stream().map(LessonRow::title).toList());
		assertEquals(List.of(1, 2, 3), stored.stream().map(LessonRow::position).toList());
		assertEquals(created, stored.get(0).updatedAt());
		assertEquals(created, stored.get(2).updatedAt());
		// and the updatedAt of the course is unchanged
		assertEquals(created, readCourse(course).updatedAt());
	}

	@Test
	@DisplayName("[lessons/R3-S2] Rename a Russian lesson, the title is normalised")
	void renameARussianLessonTheTitleIsNormalised() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A renames it to "  Склонение имён\u200B  " (ending in a zero-width space)
		var response = teacher.patch(lesson(course, lesson), title("  Склонение имён\u200B  "));

		// then the response is 200 with the title "Склонение имён"
		response.then().statusCode(200).body("title", equalTo("Склонение имён"));
		// and the stored lesson has the same title
		assertEquals(List.of("Склонение имён"), storedLessons(course).stream().map(LessonRow::title).toList());
	}

	@Test
	@DisplayName("[lessons/R3-S3] A title must have 1-200 characters and a letter or digit")
	void aTitleMustHave1To200CharactersAndALetterOrDigit() {
		// given a logged-in teacher A with a course with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A renames it to each title "   ", 201 letters, "!!!" and 200 letters, with the body {} and {"title": null}, and with the bodies {"title": and no body
		String path = lesson(course, lesson);
		var blank = teacher.patch(path, title("   "));
		var tooLong = teacher.patch(path, title("Я".repeat(201)));
		var noLetters = teacher.patch(path, title("!!!"));
		var longest = teacher.patch(path, title("Я".repeat(200)));
		var empty = teacher.patch(path, Map.of());
		var nullTitle = teacher.patch(path, title(null));
		var truncated = teacher.patch(path, "{\"title\":");
		var noBody = teacher.patch(path);

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
		// and the stored title is the 200-letter title
		assertEquals(List.of("Я".repeat(200)), storedLessons(course).stream().map(LessonRow::title).toList());
	}

	@Test
	@DisplayName("[lessons/R3-S4] Wrong JSON types are rejected, titles are not unique")
	void wrongJsonTypesAreRejectedTitlesAreNotUnique() {
		// given a logged-in teacher A with a course with the lessons "Grammar" and "Verbs"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "Grammar", 1);
		UUID verbs = createLesson(teacherA, course, "Verbs", 2);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A renames "Verbs" with the title ["Grammar"], then 123, then true, and then "Grammar"
		String path = lesson(course, verbs);
		var list = teacher.patch(path, title(List.of("Grammar")));
		var number = teacher.patch(path, title(123));
		var bool = teacher.patch(path, title(true));
		var same = teacher.patch(path, title("Grammar"));

		// then the first three responses are 400 INVALID_TYPE for the title
		for (Response response : List.of(list, number, bool)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))));
		}
		// and the fourth is 200
		same.then().statusCode(200);
		// and the course has two lessons titled "Grammar" at the positions 1 and 2
		var stored = storedLessons(course);
		assertEquals(List.of("Grammar", "Grammar"), stored.stream().map(LessonRow::title).toList());
		assertEquals(List.of(1, 2), stored.stream().map(LessonRow::position).toList());
	}

	@Test
	@DisplayName("[lessons/R3-S5] Another teacher's, unknown, malformed and wrong-course ids are not found")
	void anotherTeachersUnknownMalformedAndWrongCourseIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreign, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");
		var foreignBefore = storedLessons(foreign);
		var mathsBefore = storedLessons(maths);

		// when teacher A renames teacher B's lesson (under teacher B's course, and under "English B1"), a lesson with an unknown id, the lesson id "not-a-uuid", the lesson "Fractions" under "English B1", and under the course "not-a-uuid" and under an unknown course id
		List<Response> responses = List.of(
				teacher.patch(lesson(foreign, foreignLesson), title("Changed")),
				teacher.patch(lesson(english, foreignLesson), title("Changed")),
				teacher.patch(lesson(english, UUID.randomUUID()), title("Changed")),
				teacher.patch(lesson(english, "not-a-uuid"), title("Changed")),
				teacher.patch(lesson(english, fractions), title("Changed")),
				teacher.patch(lesson("not-a-uuid", fractions), title("Changed")),
				teacher.patch(lesson(UUID.randomUUID(), fractions), title("Changed")));

		// then all responses are 404 NOT_FOUND with no errors property and none contains "Грамматика" or "Падежи"
		responses.forEach(this::assertNotFound);
		// and the titles and updatedAt of "Падежи" and "Fractions" are unchanged
		assertEquals(foreignBefore, storedLessons(foreign));
		assertEquals(mathsBefore, storedLessons(maths));
	}

	@Test
	@DisplayName("[lessons/R3-S6] Only active teachers rename lessons")
	void onlyActiveTeachersRenameLessons() {
		// given a course of teacher A with the lesson "Lesson", a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Lesson", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each rename the lesson to "Changed", and teacher B does so without a CSRF token
		String path = lesson(course, lesson);
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
		// and the lesson is still titled "Lesson"
		assertEquals(List.of("Lesson"), storedLessons(course).stream().map(LessonRow::title).toList());
	}

	@Test
	@DisplayName("[lessons/R3-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a course with a lesson, and a course "Грамматика" of teacher B with the lesson "Падежи"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "Lesson", 1);
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreign, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");
		var foreignBefore = storedLessons(foreign);

		// when teacher A renames the lesson "not-a-uuid" with a blank title, and then teacher B's lesson "Падежи" with a blank title
		var malformed = teacher.patch(lesson(course, "not-a-uuid"), title(" "));
		var foreignResponse = teacher.patch(lesson(foreign, foreignLesson), title(" "));

		// then the first response is 404 NOT_FOUND with no errors property
		malformed.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED for the title and does not contain "Грамматика" or "Падежи"
		foreignResponse.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))))
				.body(not(containsString("Грамматика")))
				.body(not(containsString("Падежи")));
		// and teacher B's lesson is unchanged
		assertEquals(foreignBefore, storedLessons(foreign));
	}

	@Test
	@DisplayName("[lessons/R3-S8] Renaming to the same title still updates")
	void renamingToTheSameTitleStillUpdates() {
		// given a logged-in teacher A with a course with the lesson "Падежи" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID lesson = createLesson(teacherA, course, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames it to "Падежи"
		var response = teacher.patch(lesson(course, lesson), title("Падежи"));

		// then the response is 200 with the title "Падежи" and updatedAt T plus one hour
		response.then().statusCode(200).body("title", equalTo("Падежи"));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
	}
}
