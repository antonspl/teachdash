package com.teachdash.parts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** parts R2. */
class ListPartsTest extends ApiTest {

	private static String parts(Object courseId, Object lessonId) {
		return "/api/teacher/courses/" + courseId + "/lessons/" + lessonId + "/parts";
	}

	@Test
	@DisplayName("[parts/R2-S1] Parts are listed by position")
	void partsAreListedByPosition() {
		// given a logged-in teacher A with a lesson with the parts "Разминка" at position 2, "Grammar" at position 3 and "Warm-up" at position 1, inserted in that order
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		createPart(teacherA, lesson, "Разминка", 2);
		createPart(teacherA, lesson, "Grammar", 3);
		createPart(teacherA, lesson, "Warm-up", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the parts of the lesson
		var response = teacher.get(parts(course, lesson));

		// then the response is 200 with the titles in the order of the positions
		response.then().statusCode(200)
				.body("title", equalTo(List.of("Warm-up", "Разминка", "Grammar")))
				.body("position", equalTo(List.of(1, 2, 3)));
	}

	@Test
	@DisplayName("[parts/R2-S2] A list item has the part fields")
	void aListItemHasThePartFields() {
		// given a logged-in teacher A with a lesson with the part "Разминка" created at one time
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		createPart(teacherA, lesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the parts of the lesson
		var response = teacher.get(parts(course, lesson));

		// then the response is 200 with one item
		response.then().statusCode(200);
		List<Map<String, Object>> items = response.path("$");
		assertEquals(1, items.size());
		Map<String, Object> item = items.get(0);
		// and the item has exactly the part properties
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"), item.keySet());
		// and the title, position and equal times
		assertEquals("Разминка", item.get("title"));
		assertEquals(1, item.get("position"));
		assertEquals(item.get("createdAt"), item.get("updatedAt"));
	}

	@Test
	@DisplayName("[parts/R2-S3] A lesson without parts gives an empty list")
	void aLessonWithoutPartsGivesAnEmptyList() {
		// given a logged-in teacher A with a lesson with no parts and another lesson with one part
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID emptyLesson = createLesson(teacherA, course, "Empty", 1);
		UUID other = createLesson(teacherA, course, "Other", 2);
		createPart(teacherA, other, "Part", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the parts of the first lesson
		var response = teacher.get(parts(course, emptyLesson));

		// then the response is 200 with an empty array
		response.then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[parts/R2-S4] The list shows only that lesson's parts")
	void theListShowsOnlyThatLessonsParts() {
		// given a logged-in teacher A with the lessons "Grammar" with the part "Warm-up" and "Reading" with the part "Text", and a teacher B with a lesson with the part "Warm-up"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID grammar = createLesson(teacherA, course, "Grammar", 1);
		UUID reading = createLesson(teacherA, course, "Reading", 2);
		UUID part = createPart(teacherA, grammar, "Warm-up", 1);
		createPart(teacherA, reading, "Text", 1);
		UUID courseB = createCourse(teacherB, "English B1", "en", "en");
		UUID lessonB = createLesson(teacherB, courseB, "Grammar", 1);
		createPart(teacherB, lessonB, "Warm-up", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the parts of "Grammar"
		var response = teacher.get(parts(course, grammar));

		// then the response is 200 with exactly one item, the part "Warm-up" of "Grammar"
		response.then().statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].id", equalTo(part.toString()))
				.body("[0].title", equalTo("Warm-up"));
	}

	@Test
	@DisplayName("[parts/R2-S5] Another teacher's, unknown, malformed and wrong-course ids are not found")
	void anotherTeachersUnknownMalformedAndWrongCourseIdsAreNotFound() {
		// given teacher A logged in with the courses "English B1" and "Maths" and the lesson "Fractions" in "Maths", and a course "Грамматика" of teacher B with the lesson "Падежи" that has the part "Разминка"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID fractions = createLesson(teacherA, maths, "Fractions", 1);
		UUID foreignCourse = createCourse(teacherB, "Грамматика", "ru", null);
		UUID foreignLesson = createLesson(teacherB, foreignCourse, "Падежи", 1);
		createPart(teacherB, foreignLesson, "Разминка", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the parts of teacher B's lesson (under teacher B's course, and under "English B1"), of a lesson with an unknown id, of the lesson "not-a-uuid", of "Fractions" under "English B1", of "Fractions" under teacher B's course, and of "Fractions" under the course "not-a-uuid" and under an unknown course id
		List<Response> responses = List.of(
				teacher.get(parts(foreignCourse, foreignLesson)),
				teacher.get(parts(english, foreignLesson)),
				teacher.get(parts(maths, UUID.randomUUID())),
				teacher.get(parts(maths, "not-a-uuid")),
				teacher.get(parts(english, fractions)),
				teacher.get(parts(foreignCourse, fractions)),
				teacher.get(parts("not-a-uuid", fractions)),
				teacher.get(parts(UUID.randomUUID(), fractions)));

		// then all responses are 404 NOT_FOUND with no errors property, and none contains "Грамматика", "Падежи" or "Разминка"
		for (Response response : responses) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Грамматика")))
					.body(not(containsString("Падежи")))
					.body(not(containsString("Разминка")));
		}
	}

	@Test
	@DisplayName("[parts/R2-S6] Only active teachers list parts")
	void onlyActiveTeachersListParts() {
		// given a lesson of teacher A with a part, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID lesson = createLesson(teacherA, course, "Present Simple", 1);
		createPart(teacherA, lesson, "Warm-up", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		anonymous.get("/api/teacher/courses");

		// when each of them lists the parts of the lesson
		var adminResponse = admin.get(parts(course, lesson));
		var studentResponse = student.get(parts(course, lesson));
		var deactivatedResponse = deactivated.get(parts(course, lesson));
		var anonymousResponse = anonymous.get(parts(course, lesson));

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
	}

	@Test
	@DisplayName("[parts/R2-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A who is then deactivated
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();

		// when the admin and teacher A each list the parts of the lesson "not-a-uuid" in the course "not-a-uuid"
		var adminResponse = admin.get(parts("not-a-uuid", "not-a-uuid"));
		var deactivatedResponse = deactivated.get(parts("not-a-uuid", "not-a-uuid"));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and neither is a 404
		assertEquals(false, adminResponse.statusCode() == 404 || deactivatedResponse.statusCode() == 404);
	}
}
