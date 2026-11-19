package com.teachdash.courses.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

/** courses R1. */
class CreateCourseTest extends ApiTest {

	private static final String COURSES = "/api/teacher/courses";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private static Map<String, Object> course(
			Object title,
			Object instructionLanguage,
			Object targetLanguage) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		body.put("instructionLanguage", instructionLanguage);
		body.put("targetLanguage", targetLanguage);
		return body;
	}

	private static Map<String, Object> course(Object title, Object instructionLanguage) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		body.put("instructionLanguage", instructionLanguage);
		return body;
	}

	private TestClient loggedInTeacher(String email) {
		createTeacher(email, "teacher-pass-1");
		return loggedInAs(email);
	}

	@Test
	@DisplayName("[courses/R1-S1] Create an English course taught in English")
	void createAnEnglishCourseTaughtInEnglish() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a course with the title "English B1", the instruction language "en" and the target language "en"
		var response = teacher.post(COURSES, course("English B1", "en", "en"));

		// then the response is 201 with a new id and the course data
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("title", equalTo("English B1"))
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("en"))
				.body("createdAt", notNullValue());
		// and updatedAt equals createdAt
		assertEquals(response.<String>path("createdAt"), response.<String>path("updatedAt"));
		// and the course is stored owned by teacher A
		assertEquals(1, jdbc.sql("select count(*) from courses where id = :id and teacher_id = :teacher")
				.param("id", UUID.fromString(response.path("id")))
				.param("teacher", teacherA)
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[courses/R1-S2] Create a Russian course without a target language")
	void createARussianCourseWithoutATargetLanguage() {
		// given a logged-in teacher A
		TestClient teacher = loggedInTeacher("a@example.com");

		// when teacher A creates a course without a target language, and another with the target language null
		var withoutTarget = teacher.post(COURSES, course("Литература 9 класс", "ru"));
		var nullTarget = teacher.post(COURSES, course("Maths", "en", null));

		// then both responses are 201 with a null targetLanguage
		withoutTarget.then().statusCode(201)
				.body("title", equalTo("Литература 9 класс"))
				.body("targetLanguage", equalTo(null));
		nullTarget.then().statusCode(201)
				.body("targetLanguage", equalTo(null));
	}

	@Test
	@DisplayName("[courses/R1-S3] A Russian course taught in Russian or in English")
	void aRussianCourseTaughtInRussianOrInEnglish() {
		// given a logged-in teacher A
		TestClient teacher = loggedInTeacher("a@example.com");

		// when teacher A creates a Russian course taught in Russian, and another taught in English
		var inRussian = teacher.post(COURSES, course("Русский A1", "ru", "ru"));
		var inEnglish = teacher.post(COURSES, course("Russian A1", "en", "ru"));

		// then both responses are 201 with the given languages
		inRussian.then().statusCode(201)
				.body("instructionLanguage", equalTo("ru"))
				.body("targetLanguage", equalTo("ru"));
		inEnglish.then().statusCode(201)
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("ru"));
	}

	@Test
	@DisplayName("[courses/R1-S4] A title may contain punctuation")
	void aTitleMayContainPunctuation() {
		// given a logged-in teacher A
		TestClient teacher = loggedInTeacher("a@example.com");

		// when teacher A creates courses with titles that contain punctuation
		var latin = teacher.post(COURSES, course("Business Result: Upper-Intermediate. Part 1?!", "en", "en"));
		var cyrillic = teacher.post(COURSES, course("Говорим по-русски, часть 2!", "ru", "ru"));

		// then both responses are 201 with exactly those titles
		latin.then().statusCode(201)
				.body("title", equalTo("Business Result: Upper-Intermediate. Part 1?!"));
		cyrillic.then().statusCode(201)
				.body("title", equalTo("Говорим по-русски, часть 2!"));
	}

	@Test
	@DisplayName("[courses/R1-S5] The title and language codes are normalised")
	void theTitleAndLanguageCodesAreNormalised() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a course with a title ending in a zero-width space and padded language codes
		var response = teacher.post(COURSES, course("  Грамматика​  ", " en ", " ru "));

		// then the response is 201 with the normalised values
		response.then().statusCode(201)
				.body("title", equalTo("Грамматика"))
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("ru"));
		// and the stored course has the same values
		var stored = jdbc.sql("select title, instruction_language, target_language from courses where teacher_id = :teacher")
				.param("teacher", teacherA)
				.query((rs, i) -> List.of(
						rs.getString("title"),
						rs.getString("instruction_language"),
						rs.getString("target_language")))
				.single();
		assertEquals(List.of("Грамматика", "en", "ru"), stored);
	}

	@Test
	@DisplayName("[courses/R1-S6] A title must have 1–200 characters and a letter or digit")
	void aTitleMustHave1To200CharactersAndALetterOrDigit() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates courses with a blank title, a title of 201 letters, a title without letters or digits, and a title of 200 letters
		var blank = teacher.post(COURSES, course("   ", "en"));
		var tooLong = teacher.post(COURSES, course("Я".repeat(201), "en"));
		var noLetters = teacher.post(COURSES, course("!!!", "en"));
		var longest = teacher.post(COURSES, course("Я".repeat(200), "en"));

		// then the first three responses are 400 naming the title
		blank.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		tooLong.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "TOO_LONG"))));
		noLetters.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "INVALID_FORMAT"))));
		// and the fourth is 201
		longest.then().statusCode(201);
		// and teacher A has exactly one course
		assertEquals(1, countCourses(teacherA));
	}

	@Test
	@DisplayName("[courses/R1-S7] The instruction language is a listed code")
	void theInstructionLanguageIsAListedCode() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates courses with a missing, null, empty, blank, unknown, upper-case and three-letter instruction language
		Map<String, Object> missing = new HashMap<>();
		missing.put("title", "Course");
		List<Response> required = List.of(
				teacher.post(COURSES, missing),
				teacher.post(COURSES, course("Course", null)),
				teacher.post(COURSES, course("Course", "")),
				teacher.post(COURSES, course("Course", "  ")));
		List<Response> invalid = List.of(
				teacher.post(COURSES, course("Course", "xx")),
				teacher.post(COURSES, course("Course", "EN")),
				teacher.post(COURSES, course("Course", "eng")));

		// then each response is 400 naming the instruction language
		for (Response response : required) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("instructionLanguage", "REQUIRED"))));
		}
		for (Response response : invalid) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("instructionLanguage", "INVALID_FORMAT"))));
		}
		// and no body contains "xx" or "eng"
		for (Response response : invalid) {
			response.then().body(not(containsString("xx"))).body(not(containsString("eng")));
		}
		// and teacher A has no course
		assertEquals(0, countCourses(teacherA));
	}

	@Test
	@DisplayName("[courses/R1-S8] The target language is a listed code when given")
	void theTargetLanguageIsAListedCodeWhenGiven() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates courses with an unknown, upper-case, three-letter, empty and blank target language
		List<Response> responses = new ArrayList<>();
		for (String target : List.of("xx", "RU", "rus", "", "  ")) {
			responses.add(teacher.post(COURSES, course("Course", "en", target)));
		}

		// then each response is 400 naming the target language
		for (Response response : responses) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("targetLanguage", "INVALID_FORMAT"))));
		}
		// and no body contains "xx" or "rus"
		for (Response response : responses) {
			response.then().body(not(containsString("xx"))).body(not(containsString("rus")));
		}
		// and teacher A has no course
		assertEquals(0, countCourses(teacherA));
	}

	@Test
	@DisplayName("[courses/R1-S9] A course cannot be created in an unsupported language")
	void aCourseCannotBeCreatedInAnUnsupportedLanguage() {
		// given a logged-in teacher A, and the language list with "de" and "uk" marked content false
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates courses with "de" as instruction language, with "uk" as target language, and with both
		var instruction = teacher.post(COURSES, course("Course", "de"));
		var target = teacher.post(COURSES, course("Course", "en", "uk"));
		var both = teacher.post(COURSES, course("Course", "de", "uk"));

		// then each response is 400 naming the unsupported language fields
		instruction.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("instructionLanguage", "UNSUPPORTED_COURSE_LANGUAGE"))));
		target.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("targetLanguage", "UNSUPPORTED_COURSE_LANGUAGE"))));
		both.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(
						error("instructionLanguage", "UNSUPPORTED_COURSE_LANGUAGE"),
						error("targetLanguage", "UNSUPPORTED_COURSE_LANGUAGE"))));
		// and teacher A has no course
		assertEquals(0, countCourses(teacherA));
	}

	@Test
	@DisplayName("[courses/R1-S10] All failing fields are reported together")
	void allFailingFieldsAreReportedTogether() {
		// given a logged-in teacher A
		TestClient teacher = loggedInTeacher("a@example.com");

		// when teacher A creates a course with a blank title, the instruction language "xx" and the target language "de"
		var response = teacher.post(COURSES, course(" ", "xx", "de"));

		// then the response is 400 with all three failing fields in order
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(
						error("instructionLanguage", "INVALID_FORMAT"),
						error("targetLanguage", "UNSUPPORTED_COURSE_LANGUAGE"),
						error("title", "REQUIRED"))));
	}

	@Test
	@DisplayName("[courses/R1-S11] A title is unique per teacher, ignoring case")
	void aTitleIsUniquePerTeacherIgnoringCase() {
		// given teacher A with courses "Grammar" and "Грамматика", and teacher B with no courses, both logged in
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		createTeacher("b@example.com", "teacher-pass-1");
		createCourse(teacherA, "Grammar", "en", "en");
		createCourse(teacherA, "Грамматика", "ru", "ru");
		TestClient clientA = loggedInAs("a@example.com");
		TestClient clientB = loggedInAs("b@example.com");

		// when teacher A creates courses titled "GRAMMAR" and "грамматика", and teacher B creates a course titled "Grammar"
		var upper = clientA.post(COURSES, course("GRAMMAR", "en"));
		var lower = clientA.post(COURSES, course("грамматика", "en"));
		var other = clientB.post(COURSES, course("Grammar", "en"));

		// then teacher A's responses are 409 TITLE_TAKEN naming the title
		for (Response response : List.of(upper, lower)) {
			response.then().statusCode(409)
					.body("code", equalTo("TITLE_TAKEN"))
					.body("errors", equalTo(List.of(error("title", "TITLE_TAKEN"))));
		}
		// and teacher B's response is 201
		other.then().statusCode(201);
		// and teacher A still has exactly two courses
		assertEquals(2, countCourses(teacherA));
	}

	@Test
	@DisplayName("[courses/R1-S12] At most 20 courses per teacher")
	void atMostTwentyCoursesPerTeacher() {
		// given teacher A with 20 courses, one of them titled "Course 1", and teacher B with 19 courses, both logged in
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		for (int i = 1; i <= 20; i++) {
			createCourse(teacherA, "Course " + i, "en", "en");
		}
		for (int i = 1; i <= 19; i++) {
			createCourse(teacherB, "Course " + i, "en", "en");
		}
		TestClient clientA = loggedInAs("a@example.com");
		TestClient clientB = loggedInAs("b@example.com");

		// when teacher A creates a new title, a taken title and a blank title, and teacher B creates "Course 20"
		var newTitle = clientA.post(COURSES, course("Course 21", "en"));
		var takenTitle = clientA.post(COURSES, course("Course 1", "en"));
		var blankTitle = clientA.post(COURSES, course(" ", "en"));
		var other = clientB.post(COURSES, course("Course 20", "en"));

		// then teacher A's first two responses are 409 COURSE_LIMIT_REACHED with no errors property
		for (Response response : List.of(newTitle, takenTitle)) {
			response.then().statusCode(409)
					.body("code", equalTo("COURSE_LIMIT_REACHED"))
					.body("$", not(hasKey("errors")));
		}
		// and the third is a validation error for the title
		blankTitle.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		// and teacher B's response is 201
		other.then().statusCode(201);
		// and both teachers have 20 courses
		assertEquals(20, countCourses(teacherA));
		assertEquals(20, countCourses(teacherB));
	}

	@Test
	@DisplayName("[courses/R1-S13] Parallel requests respect the limit and uniqueness")
	void parallelRequestsRespectTheLimitAndUniqueness() throws Exception {
		// given teacher A with 19 courses and teacher B with no courses, both logged in
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		for (int i = 1; i <= 19; i++) {
			createCourse(teacherA, "Course " + i, "en", "en");
		}
		TestClient clientA = loggedInAs("a@example.com");
		TestClient clientB = loggedInAs("b@example.com");

		// when teacher A sends 5 create requests with different titles and teacher B sends 5 with the same title, all at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			TestClient client = clientA.snapshot();
			String title = "New " + i;
			calls.add(() -> client.post(COURSES, course(title, "en")));
		}
		for (int i = 1; i <= 5; i++) {
			TestClient client = clientB.snapshot();
			calls.add(() -> client.post(COURSES, course("Same", "en")));
		}
		List<Response> responses = Parallel.run(calls);
		List<Response> responsesA = responses.subList(0, 5);
		List<Response> responsesB = responses.subList(5, 10);

		// then exactly one of teacher A's responses is 201 and the others are 409 COURSE_LIMIT_REACHED
		assertEquals(1, responsesA.stream().filter(r -> r.statusCode() == 201).count());
		assertEquals(4, responsesA.stream()
				.filter(r -> r.statusCode() == 409 && "COURSE_LIMIT_REACHED".equals(r.path("code")))
				.count());
		// and exactly one of teacher B's responses is 201 and the others are 409 TITLE_TAKEN
		assertEquals(1, responsesB.stream().filter(r -> r.statusCode() == 201).count());
		assertEquals(4, responsesB.stream()
				.filter(r -> r.statusCode() == 409 && "TITLE_TAKEN".equals(r.path("code")))
				.count());
		// and teacher A has 20 courses and teacher B has one course
		assertEquals(20, countCourses(teacherA));
		assertEquals(1, countCourses(teacherB));
	}

	@Test
	@DisplayName("[courses/R1-S14] The course belongs to the caller")
	void theCourseBelongsToTheCaller() {
		// given teachers A and B, with teacher A logged in
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A creates a course with the extra body field teacherId set to teacher B's id
		Map<String, Object> body = course("Course", "en");
		body.put("teacherId", teacherB.toString());
		var response = clientA.post(COURSES, body);

		// then the response is 201
		response.then().statusCode(201);
		// and the course is owned by teacher A, and teacher B has no course
		assertEquals(1, countCourses(teacherA));
		assertEquals(0, countCourses(teacherB));
	}

	@Test
	@DisplayName("[courses/R1-S15] Only active teachers create courses")
	void onlyActiveTeachersCreateCourses() {
		// given a logged-in admin, a logged-in student, a logged-in teacher who is then deactivated, and a client with a CSRF cookie and no session
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("teacher@example.com");
		jdbc.sql("update users set active = false where email = 'teacher@example.com'").update();
		// a new client starts with an XSRF-TOKEN cookie and sends the matching header
		TestClient anonymous = newClient();

		// when each of them creates a course with the title "Course" and the instruction language "en"
		var adminResponse = admin.post(COURSES, course("Course", "en"));
		var studentResponse = student.post(COURSES, course("Course", "en"));
		var deactivatedResponse = deactivated.post(COURSES, course("Course", "en"));
		var anonymousResponse = anonymous.post(COURSES, course("Course", "en"));

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
		// and no course exists
		assertEquals(0, countRows("courses"));
	}

	@Test
	@DisplayName("[courses/R1-S16] Wrong JSON types are rejected")
	void wrongJsonTypesAreRejected() {
		// given a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a course with a list as the title, and another with an object as the instruction language
		var listTitle = teacher.post(COURSES, course(List.of("Grammar"), "en"));
		var objectLanguage = teacher.post(COURSES, course("Grammar", Map.of("code", "en")));

		// then the responses are 400 naming the field with INVALID_TYPE
		listTitle.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))));
		objectLanguage.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("instructionLanguage", "INVALID_TYPE"))));
		// and neither body contains "Grammar"
		listTitle.then().body(not(containsString("Grammar")));
		objectLanguage.then().body(not(containsString("Grammar")));
		// and teacher A has no course
		assertEquals(0, countCourses(teacherA));
	}
}
