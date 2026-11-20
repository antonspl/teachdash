package com.teachdash.lessons.api;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** lessons R1. */
class CreateLessonTest extends ApiTest {

	private static String lessons(Object courseId) {
		return "/api/teacher/courses/" + courseId + "/lessons";
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
			UUID teacherId,
			String title,
			Instant createdAt,
			Instant updatedAt) {
	}

	private List<LessonRow> storedLessons(UUID courseId) {
		return jdbc.sql("""
				select l.teacher_id, l.title, l.created_at, l.updated_at
				from lessons l join lesson_courses lc on lc.lesson_id = l.id
				where lc.course_id = :course
				order by lc.position
				""")
				.param("course", courseId)
				.query((rs, i) -> new LessonRow(
						rs.getObject("teacher_id", UUID.class),
						rs.getString("title"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.list();
	}

	private long countTeacherLessons(UUID teacherId) {
		return jdbc.sql("select count(*) from lessons where teacher_id = :teacher")
				.param("teacher", teacherId)
				.query(Long.class)
				.single();
	}

	@Test
	@DisplayName("[lessons/R1-S1] Create an English lesson")
	void createAnEnglishLesson() {
		// given a logged-in teacher A with the course "English B1" (instruction "en", target "en")
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson in it with the title "Present Simple"
		var response = teacher.post(lessons(course), title("Present Simple"));

		// then the response is 201 with a new id, the title, position 1 and a createdAt time
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("title", equalTo("Present Simple"))
				.body("position", equalTo(1))
				.body("createdAt", notNullValue());
		// and updatedAt equals createdAt
		assertEquals(response.<String>path("createdAt"), response.<String>path("updatedAt"));
		// and exactly these properties are returned
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"),
				response.<Map<String, Object>>path("$").keySet());
		// and the lesson is stored owned by teacher A and linked to the course at position 1
		var stored = storedLessons(course);
		assertEquals(1, stored.size());
		assertEquals(teacherA, stored.get(0).teacherId());
		assertEquals("Present Simple", stored.get(0).title());
		assertEquals(List.of(1), lessonPositions(course));
		assertEquals(1, jdbc.sql("select count(*) from lessons where id = :id")
				.param("id", UUID.fromString(response.path("id")))
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[lessons/R1-S2] Create a Russian lesson")
	void createARussianLesson() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target)
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson in it with the title "Падежи: именительный, родительный?"
		var response = teacher.post(lessons(course), title("Падежи: именительный, родительный?"));

		// then the response is 201 with exactly that title and position 1
		response.then().statusCode(201)
				.body("title", equalTo("Падежи: именительный, родительный?"))
				.body("position", equalTo(1));
		// and the stored lesson has the same title
		assertEquals(List.of("Падежи: именительный, родительный?"),
				storedLessons(course).stream().map(LessonRow::title).toList());
	}

	@Test
	@DisplayName("[lessons/R1-S3] Lessons are added at the end, per course")
	void lessonsAreAddedAtTheEndPerCourse() {
		// given a logged-in teacher A with the courses "English B1" and "Maths", where "English B1" already has the lessons "One" and "Two", created at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		createLesson(teacherA, english, "One", 1);
		createLesson(teacherA, english, "Two", 2);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A creates the lesson "Three" in "English B1" and the lesson "First" in "Maths"
		var three = teacher.post(lessons(english), title("Three"));
		var first = teacher.post(lessons(maths), title("First"));

		// then the responses are 201 with position 3 and position 1
		three.then().statusCode(201).body("position", equalTo(3));
		first.then().statusCode(201).body("position", equalTo(1));
		// and "English B1" has the positions 1, 2, 3 without gaps
		assertEquals(List.of(1, 2, 3), lessonPositions(english));
		// and the lessons "One" and "Two" keep their positions and their updatedAt T
		var stored = storedLessons(english);
		assertEquals(List.of("One", "Two", "Three"), stored.stream().map(LessonRow::title).toList());
		assertEquals(created, stored.get(0).updatedAt());
		assertEquals(created, stored.get(1).updatedAt());
		// and the updatedAt of both courses is unchanged
		assertEquals(created, readCourse(english).updatedAt());
		assertEquals(created, readCourse(maths).updatedAt());
	}

	@Test
	@DisplayName("[lessons/R1-S4] The title is normalised")
	void theTitleIsNormalised() {
		// given a logged-in teacher A with a course
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson with the title "  Грамматика​  " (ending in a zero-width space)
		var response = teacher.post(lessons(course), title("  Грамматика​  "));

		// then the response is 201 with the title "Грамматика"
		response.then().statusCode(201).body("title", equalTo("Грамматика"));
		// and the stored lesson has the same title
		assertEquals(List.of("Грамматика"), storedLessons(course).stream().map(LessonRow::title).toList());
	}

	@Test
	@DisplayName("[lessons/R1-S5] A title must have 1-200 characters and a letter or digit")
	void aTitleMustHave1To200CharactersAndALetterOrDigit() {
		// given a logged-in teacher A with a course
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates lessons with each title "   ", 201 letters, "!!!", and 200 letters, with the body {} and {"title": null}, and with the bodies {"title": and no body
		var blank = teacher.post(lessons(course), title("   "));
		var tooLong = teacher.post(lessons(course), title("Я".repeat(201)));
		var noLetters = teacher.post(lessons(course), title("!!!"));
		var longest = teacher.post(lessons(course), title("Я".repeat(200)));
		var empty = teacher.post(lessons(course), Map.of());
		var nullTitle = teacher.post(lessons(course), title(null));
		var truncated = teacher.post(lessons(course), "{\"title\":");
		var noBody = teacher.post(lessons(course));

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
		// and the course has exactly one lesson
		assertEquals(1, countLessons(course));
	}

	@Test
	@DisplayName("[lessons/R1-S6] Wrong JSON types and a number or a boolean are rejected")
	void wrongJsonTypesAndANumberOrABooleanAreRejected() {
		// given a logged-in teacher A with a course
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson with the title ["Grammar"], then 123, then true
		var list = teacher.post(lessons(course), title(List.of("Grammar")));
		var number = teacher.post(lessons(course), title(123));
		var bool = teacher.post(lessons(course), title(true));

		// then every response is 400 with INVALID_TYPE for the title
		for (Response response : List.of(list, number, bool)) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("title", "INVALID_TYPE"))));
		}
		// and no body contains "Grammar", "123" or "true"
		list.then().body(not(containsString("Grammar")));
		number.then().body(not(containsString("123")));
		bool.then().body(not(containsString("true")));
		// and the course has no lesson
		assertEquals(0, countLessons(course));
	}

	@Test
	@DisplayName("[lessons/R1-S7] Titles are not unique")
	void titlesAreNotUnique() {
		// given a logged-in teacher A with the courses "English B1" and "English B2", where "English B1" has the lesson "Grammar"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID b1 = createCourse(teacherA, "English B1", "en", "en");
		UUID b2 = createCourse(teacherA, "English B2", "en", "en");
		createLesson(teacherA, b1, "Grammar", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates the lessons "Grammar" and "GRAMMAR" in "English B1", and "Grammar" in "English B2"
		var same = teacher.post(lessons(b1), title("Grammar"));
		var upper = teacher.post(lessons(b1), title("GRAMMAR"));
		var other = teacher.post(lessons(b2), title("Grammar"));

		// then all three responses are 201
		for (Response response : List.of(same, upper, other)) {
			response.then().statusCode(201);
		}
		// and "English B1" has three lessons at the positions 1, 2, 3
		assertEquals(List.of(1, 2, 3), lessonPositions(b1));
	}

	@Test
	@DisplayName("[lessons/R1-S8] At most 100 lessons per course")
	void atMost100LessonsPerCourse() {
		// given a logged-in teacher A with a course "Full" with 100 lessons and a course "Other" with 99 lessons
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID full = createCourse(teacherA, "Full", "en", "en");
		UUID other = createCourse(teacherA, "Other", "en", "en");
		for (int i = 1; i <= 100; i++) {
			createLesson(teacherA, full, "Lesson " + i, i);
		}
		for (int i = 1; i <= 99; i++) {
			createLesson(teacherA, other, "Lesson " + i, i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates the lesson "Lesson 101" in "Full", then a lesson with a blank title in "Full", and then the lesson "Lesson 100" in "Other"
		var over = teacher.post(lessons(full), title("Lesson 101"));
		var blank = teacher.post(lessons(full), title(" "));
		var last = teacher.post(lessons(other), title("Lesson 100"));

		// then the first response is 409 LESSON_LIMIT_REACHED with no errors property
		over.then().statusCode(409)
				.body("code", equalTo("LESSON_LIMIT_REACHED"))
				.body("$", not(hasKey("errors")));
		// and the second is a validation error for the title
		blank.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))));
		// and the third is 201 with position 100
		last.then().statusCode(201).body("position", equalTo(100));
		// and both courses have 100 lessons
		assertEquals(100, countLessons(full));
		assertEquals(100, countLessons(other));
	}

	@Test
	@DisplayName("[lessons/R1-S9] Parallel requests keep positions distinct and the limit")
	void parallelRequestsKeepPositionsDistinctAndTheLimit() throws Exception {
		// given a logged-in teacher A with a course with 97 lessons
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		for (int i = 1; i <= 97; i++) {
			createLesson(teacherA, course, "Lesson " + i, i);
		}
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A sends 6 create requests at the same time with the titles "New 1" to "New 6"
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			TestClient client = clientA.snapshot();
			String title = "New " + i;
			calls.add(() -> client.post(lessons(course), title(title)));
		}
		List<Response> responses = Parallel.run(calls);

		// then exactly 3 responses are 201 with the distinct positions 98, 99 and 100
		List<Response> created = responses.stream().filter(r -> r.statusCode() == 201).toList();
		assertEquals(3, created.size());
		assertEquals(Set.of(98, 99, 100), Set.copyOf(created.stream().map(r -> r.<Integer>path("position")).toList()));
		// and the other 3 are 409 LESSON_LIMIT_REACHED
		assertEquals(3, responses.stream()
				.filter(r -> r.statusCode() == 409 && "LESSON_LIMIT_REACHED".equals(r.path("code")))
				.count());
		// and the course has exactly 100 lessons with the positions 1 to 100 without gaps
		assertEquals(100, countLessons(course));
		assertEquals(java.util.stream.IntStream.rangeClosed(1, 100).boxed().toList(), lessonPositions(course));
	}

	@Test
	@DisplayName("[lessons/R1-S10] The lesson belongs to the caller")
	void theLessonBelongsToTheCaller() {
		// given teachers A and B, with teacher A logged in and owning a course
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient clientA = loggedInAs("a@example.com");

		// when teacher A creates a lesson in it with the extra body field teacherId set to teacher B's id
		Map<String, Object> body = title("Lesson");
		body.put("teacherId", teacherB.toString());
		var response = clientA.post(lessons(course), body);

		// then the response is 201
		response.then().statusCode(201);
		// and the lesson is owned by teacher A, and teacher B has no lesson
		assertEquals(1, countTeacherLessons(teacherA));
		assertEquals(0, countTeacherLessons(teacherB));
	}

	@Test
	@DisplayName("[lessons/R1-S11] Another teacher's, unknown and malformed course ids are not found")
	void anotherTeachersUnknownAndMalformedCourseIdsAreNotFound() {
		// given teacher A logged in, and a course "Грамматика" of teacher B with 100 lessons, one of them "Падежи"
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		createLesson(teacherB, foreign, "Падежи", 1);
		for (int i = 2; i <= 100; i++) {
			createLesson(teacherB, foreign, "Lesson " + i, i);
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson with the title "Intruder" in teacher B's course, in a course with an unknown id and in the course "not-a-uuid"
		var foreignResponse = teacher.post(lessons(foreign), title("Intruder"));
		var unknownResponse = teacher.post(lessons(UUID.randomUUID()), title("Intruder"));
		var malformedResponse = teacher.post(lessons("not-a-uuid"), title("Intruder"));

		// then all three responses are 404 NOT_FOUND with no errors property
		for (Response response : List.of(foreignResponse, unknownResponse, malformedResponse)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Грамматика")))
					.body(not(containsString("Падежи")));
		}
		// and teacher B's course still has exactly 100 lessons
		assertEquals(100, countLessons(foreign));
	}

	@Test
	@DisplayName("[lessons/R1-S12] Only active teachers create lessons")
	void onlyActiveTeachersCreateLessons() {
		// given a course of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each create a lesson in the course with the title "Lesson", and teacher B does so without a CSRF token
		var adminResponse = admin.post(lessons(course), title("Lesson"));
		var studentResponse = student.post(lessons(course), title("Lesson"));
		var deactivatedResponse = deactivated.post(lessons(course), title("Lesson"));
		var anonymousResponse = anonymous.post(lessons(course), title("Lesson"));
		var csrfResponse = withoutCsrf.post(lessons(course), title("Lesson"));

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
		// and the course has no lesson
		assertEquals(0, countLessons(course));
	}

	@Test
	@DisplayName("[lessons/R1-S13] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given teacher A logged in with a course, and a course "Грамматика" of teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		createCourse(teacherA, "English B1", "en", "en");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A creates a lesson with a blank title in the course "not-a-uuid", and then a lesson with a blank title in teacher B's course
		var malformed = teacher.post(lessons("not-a-uuid"), title(" "));
		var foreignResponse = teacher.post(lessons(foreign), title(" "));

		// then the first response is 404 NOT_FOUND with no errors property
		malformed.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		// and the second is 400 VALIDATION_FAILED for the title and does not contain "Грамматика"
		foreignResponse.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("title", "REQUIRED"))))
				.body(not(containsString("Грамматика")));
		// and teacher B's course has no new lesson
		assertEquals(0, countLessons(foreign));
	}
}
