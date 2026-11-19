package com.teachdash.courses.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** courses R5. */
class RenameCourseTest extends ApiTest {

	private static final String COURSES = "/api/teacher/courses";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private static Map<String, Object> title(Object title) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		return body;
	}

	@Test
	@DisplayName("[courses/R5-S1] A teacher renames a course")
	void aTeacherRenamesACourse() {
		// given a logged-in teacher A with the course "English B1" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames the course to "English B2"
		var response = teacher.patch(COURSES + "/" + course, title("English B2"));

		// then the response is 200 with the new title and the other data unchanged
		response.then().statusCode(200)
				.body("id", equalTo(course.toString()))
				.body("title", equalTo("English B2"))
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("en"));
		assertEquals(created, Instant.parse(response.path("createdAt")));
		assertEquals(created.plus(Duration.ofHours(1)), Instant.parse(response.path("updatedAt")));
		// and the stored course has the same values
		var stored = readCourse(course);
		assertEquals("English B2", stored.title());
		assertEquals("en", stored.instructionLanguage());
		assertEquals("en", stored.targetLanguage());
		assertEquals(created, stored.createdAt());
		assertEquals(created.plus(Duration.ofHours(1)), stored.updatedAt());
	}

	@Test
	@DisplayName("[courses/R5-S2] A course is renamed to a Cyrillic title")
	void aCourseIsRenamedToACyrillicTitle() {
		// given a logged-in teacher A with the course "Maths" (instruction "ru", no target), and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Maths", "ru", null);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames it to "  Литература 9 класс  "
		var response = teacher.patch(COURSES + "/" + course, title("  Литература 9 класс  "));

		// then the response is 200 with the trimmed title and the languages unchanged
		response.then().statusCode(200)
				.body("title", equalTo("Литература 9 класс"))
				.body("instructionLanguage", equalTo("ru"))
				.body("targetLanguage", equalTo(null));
		// and the stored title is "Литература 9 класс"
		assertEquals("Литература 9 класс", readCourse(course).title());
	}

	@Test
	@DisplayName("[courses/R5-S3] The own title and a case-only change are allowed")
	void theOwnTitleAndACaseOnlyChangeAreAllowed() {
		// given a logged-in teacher A with the courses "Grammar" and "Грамматика" created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID latin = createCourse(teacherA, "Grammar", "en", "en");
		UUID cyrillic = createCourse(teacherA, "Грамматика", "ru", "ru");
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames "Grammar" to "Grammar", and "Грамматика" to "ГРАММАТИКА"
		var same = teacher.patch(COURSES + "/" + latin, title("Grammar"));
		var caseOnly = teacher.patch(COURSES + "/" + cyrillic, title("ГРАММАТИКА"));

		// then both responses are 200 with the titles
		same.then().statusCode(200).body("title", equalTo("Grammar"));
		caseOnly.then().statusCode(200).body("title", equalTo("ГРАММАТИКА"));
		// and both updatedAt are one hour after T and both createdAt are T
		for (UUID id : List.of(latin, cyrillic)) {
			var stored = readCourse(id);
			assertEquals(created, stored.createdAt());
			assertEquals(created.plus(Duration.ofHours(1)), stored.updatedAt());
		}
		assertEquals("ГРАММАТИКА", readCourse(cyrillic).title());
	}

	@Test
	@DisplayName("[courses/R5-S4] Invalid titles are rejected")
	void invalidTitlesAreRejected() {
		// given a logged-in teacher A with the course "Grammar"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Grammar", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");
		String path = COURSES + "/" + course;

		// when teacher A sends the titles "", "   ", 201 characters, "!?", the bodies {} and {"title": null}, a broken body and no body
		var empty = teacher.patch(path, title(""));
		var blank = teacher.patch(path, title("   "));
		var tooLong = teacher.patch(path, title("Я".repeat(201)));
		var noLetters = teacher.patch(path, title("!?"));
		var emptyObject = teacher.patch(path, Map.of());
		var nullTitle = teacher.patch(path, title(null));
		var broken = teacher.patch(path, "{\"title\":");
		var noBody = teacher.patch(path, "");

		// then "", "   ", {} and null are 400 VALIDATION_FAILED with REQUIRED
		for (Response response : List.of(empty, blank, emptyObject, nullTitle)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		}
		// and the broken body and no body are 400 MALFORMED_REQUEST without errors
		for (Response response : List.of(broken, noBody)) {
			response.then().statusCode(400)
					.body("code", equalTo("MALFORMED_REQUEST"))
					.body("$", not(hasKey("errors")));
		}
		// and 201 characters is TOO_LONG, "!?" is INVALID_FORMAT
		tooLong.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "TOO_LONG"))));
		noLetters.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "INVALID_FORMAT"))));
		// and the title is still "Grammar"
		assertEquals("Grammar", readCourse(course).title());
	}

	@Test
	@DisplayName("[courses/R5-S5] Wrong JSON types are rejected")
	void wrongJsonTypesAreRejected() {
		// given a logged-in teacher A with the course "Grammar"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Grammar", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");
		String path = COURSES + "/" + course;

		// when teacher A sends the title ["Grammar"], and another request with the title {"text": "Grammar"}
		var list = teacher.patch(path, title(List.of("Grammar")));
		var object = teacher.patch(path, title(Map.of("text", "Grammar")));

		// then the responses are 400 VALIDATION_FAILED with INVALID_TYPE and neither body contains "Grammar"
		for (Response response : List.of(list, object)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))))
					.body(not(containsString("Grammar")));
		}
		// and the title is still "Grammar"
		assertEquals("Grammar", readCourse(course).title());
	}

	@Test
	@DisplayName("[courses/R5-S6] A title is unique per teacher, ignoring case")
	void aTitleIsUniquePerTeacherIgnoringCase() {
		// given teacher A with "Grammar", "Грамматика" and "Vocabulary", teacher B with "Reading", and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID grammar = createCourse(teacherA, "Grammar", "en", "en");
		createCourse(teacherA, "Грамматика", "ru", "ru");
		UUID vocabulary = createCourse(teacherA, "Vocabulary", "en", "en");
		createCourse(teacherB, "Reading", "en", "en");
		Instant vocabularyUpdated = readCourse(vocabulary).updatedAt();
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames "Vocabulary" to "GRAMMAR" and to "грамматика", and "Grammar" to "Reading"
		var upper = teacher.patch(COURSES + "/" + vocabulary, title("GRAMMAR"));
		var lower = teacher.patch(COURSES + "/" + vocabulary, title("грамматика"));
		var other = teacher.patch(COURSES + "/" + grammar, title("Reading"));

		// then the first two responses are 409 TITLE_TAKEN naming the title
		for (Response response : List.of(upper, lower)) {
			response.then().statusCode(409)
					.body("code", equalTo("TITLE_TAKEN"))
					.body("errors", equalTo(List.of(error("title", "TITLE_TAKEN"))));
		}
		// and the third is 200 and "Grammar" is now stored as "Reading"
		other.then().statusCode(200);
		assertEquals("Reading", readCourse(grammar).title());
		// and "Vocabulary" keeps its title and its updatedAt
		assertEquals("Vocabulary", readCourse(vocabulary).title());
		assertEquals(vocabularyUpdated, readCourse(vocabulary).updatedAt());
	}

	@Test
	@DisplayName("[courses/R5-S7] Only the title changes")
	void onlyTheTitleChanges() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en"), teacher B, and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames the course to "English B2" with the extra fields instructionLanguage, targetLanguage and teacherId
		Map<String, Object> body = title("English B2");
		body.put("instructionLanguage", "ru");
		body.put("targetLanguage", "ru");
		body.put("teacherId", teacherB.toString());
		var response = teacher.patch(COURSES + "/" + course, body);

		// then the response is 200 with the new title and the original languages
		response.then().statusCode(200)
				.body("title", equalTo("English B2"))
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("en"));
		// and the course is still owned by teacher A, and teacher B has no course
		assertEquals(teacherA, readCourse(course).teacherId());
		assertEquals(0, countCourses(teacherB));
	}

	@Test
	@DisplayName("[courses/R5-S8] Another teacher's, unknown and malformed ids are not found")
	void anotherTeachersUnknownAndMalformedIdsAreNotFound() {
		// given teacher A logged in, a course "Грамматика" of teacher B, and the clock advanced
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", "ru");
		Instant foreignUpdated = readCourse(foreign).updatedAt();
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames teacher B's course, an unknown id and "not-a-uuid" to "Grammar", and "not-a-uuid" to ""
		var foreignResponse = teacher.patch(COURSES + "/" + foreign, title("Grammar"));
		var unknownResponse = teacher.patch(COURSES + "/" + UUID.randomUUID(), title("Grammar"));
		var malformedResponse = teacher.patch(COURSES + "/not-a-uuid", title("Grammar"));
		var malformedInvalidResponse = teacher.patch(COURSES + "/not-a-uuid", title(""));

		// then all four responses are 404 NOT_FOUND without errors
		for (Response response : List.of(foreignResponse, unknownResponse, malformedResponse, malformedInvalidResponse)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")));
		}
		// and teacher B's course keeps its title and its updatedAt
		var stored = readCourse(foreign);
		assertEquals("Грамматика", stored.title());
		assertEquals(foreignUpdated, stored.updatedAt());
	}

	@Test
	@DisplayName("[courses/R5-S9] Only active teachers rename a course")
	void onlyActiveTeachersRenameACourse() {
		// given a course of teacher A, a logged-in admin, a logged-in student, teacher A logged in and then deactivated, a client with a CSRF cookie and no session, a logged-in teacher B, and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Grammar", "en", "en");
		Instant updated = readCourse(course).updatedAt();
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com").withoutCsrfHeader();
		testClock().advance(Duration.ofHours(1));
		String path = COURSES + "/" + course;

		// when the admin, the student, teacher A and the client each rename the course to "Renamed", and teacher B does so without a CSRF token
		var adminResponse = admin.patch(path, title("Renamed"));
		var studentResponse = student.patch(path, title("Renamed"));
		var deactivatedResponse = deactivated.patch(path, title("Renamed"));
		var anonymousResponse = anonymous.patch(path, title("Renamed"));
		var noCsrfResponse = teacherB.patch(path, title("Renamed"));

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and teacher A's and the client's are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
		// and teacher B's is 403 CSRF_INVALID
		noCsrfResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and the course keeps its title and its updatedAt
		var stored = readCourse(course);
		assertEquals("Grammar", stored.title());
		assertEquals(updated, stored.updatedAt());
	}

	@Test
	@DisplayName("[courses/R5-S10] Parallel renames to one title give one winner")
	void parallelRenamesToOneTitleGiveOneWinner() throws Exception {
		// given a logged-in teacher A with the courses "One", "Two", "Three" and "Four", and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Map<String, UUID> courses = new HashMap<>();
		for (String name : List.of("One", "Two", "Three", "Four")) {
			courses.put(name, createCourse(teacherA, name, "en", "en"));
		}
		Instant updated = readCourse(courses.get("One")).updatedAt();
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames all four to "Same" at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		for (UUID id : courses.values()) {
			TestClient client = teacher.snapshot();
			calls.add(() -> client.patch(COURSES + "/" + id, title("Same")));
		}
		List<Response> responses = Parallel.run(calls);

		// then exactly one response is 200 and the other three are 409 TITLE_TAKEN
		assertEquals(1, responses.stream().filter(r -> r.statusCode() == 200).count());
		assertEquals(3, responses.stream()
				.filter(r -> r.statusCode() == 409 && "TITLE_TAKEN".equals(r.path("code")))
				.count());
		// and exactly one course has the title "Same"
		assertEquals(1, courses.values().stream().filter(id -> "Same".equals(readCourse(id).title())).count());
		// and the other three keep their titles and their updatedAt
		for (var entry : courses.entrySet()) {
			var stored = readCourse(entry.getValue());
			if (!"Same".equals(stored.title())) {
				assertEquals(entry.getKey(), stored.title());
				assertEquals(updated, stored.updatedAt());
			}
		}
	}
}
