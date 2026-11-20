package com.teachdash.lessons.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** lessons R2. */
class ListLessonsTest extends ApiTest {

	private static String lessons(Object courseId) {
		return "/api/teacher/courses/" + courseId + "/lessons";
	}

	@Test
	@DisplayName("[lessons/R2-S1] Lessons are listed by position")
	void lessonsAreListedByPosition() {
		// given a logged-in teacher A with a course with the lessons "Падежи" at position 2, "Present Simple" at position 3 and "Alphabet" at position 1, inserted in that order
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "Падежи", 2);
		createLesson(teacherA, course, "Present Simple", 3);
		createLesson(teacherA, course, "Alphabet", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the lessons of the course
		var response = teacher.get(lessons(course));

		// then the response is 200 with the titles in the order of the positions
		response.then().statusCode(200)
				.body("title", equalTo(List.of("Alphabet", "Падежи", "Present Simple")))
				.body("position", equalTo(List.of(1, 2, 3)));
	}

	@Test
	@DisplayName("[lessons/R2-S2] A list item has the lesson fields")
	void aListItemHasTheLessonFields() {
		// given a logged-in teacher A with a course with the lesson "Падежи" created at one time
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "Падежи", 1, Instant.now(clock));
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the lessons of the course
		var response = teacher.get(lessons(course));

		// then the response is 200 with one item
		response.then().statusCode(200);
		List<Map<String, Object>> items = response.path("$");
		assertEquals(1, items.size());
		Map<String, Object> item = items.get(0);
		// and the item has exactly the lesson properties
		assertEquals(Set.of("id", "title", "position", "createdAt", "updatedAt"), item.keySet());
		// and the title, position and equal times
		assertEquals("Падежи", item.get("title"));
		assertEquals(1, item.get("position"));
		assertEquals(item.get("createdAt"), item.get("updatedAt"));
	}

	@Test
	@DisplayName("[lessons/R2-S3] A course without lessons gives an empty list")
	void aCourseWithoutLessonsGivesAnEmptyList() {
		// given a logged-in teacher A with a course with no lessons and another course with one lesson
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID emptyCourse = createCourse(teacherA, "Empty", "en", "en");
		UUID other = createCourse(teacherA, "Other", "en", "en");
		createLesson(teacherA, other, "Lesson", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the lessons of the first course
		var response = teacher.get(lessons(emptyCourse));

		// then the response is 200 with an empty array
		response.then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[lessons/R2-S4] The list shows only that course's lessons")
	void theListShowsOnlyThatCoursesLessons() {
		// given a logged-in teacher A with the courses "English B1" with the lesson "Present Simple" and "Maths" with the lesson "Fractions", and a teacher B with a course with the lesson "Present Simple"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "ru", null);
		UUID lesson = createLesson(teacherA, english, "Present Simple", 1);
		createLesson(teacherA, maths, "Fractions", 1);
		UUID courseB = createCourse(teacherB, "English B1", "en", "en");
		createLesson(teacherB, courseB, "Present Simple", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the lessons of "English B1"
		var response = teacher.get(lessons(english));

		// then the response is 200 with exactly one item, the lesson "Present Simple" of "English B1"
		response.then().statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].id", equalTo(lesson.toString()))
				.body("[0].title", equalTo("Present Simple"));
	}

	@Test
	@DisplayName("[lessons/R2-S5] Another teacher's, unknown and malformed course ids are not found")
	void anotherTeachersUnknownAndMalformedCourseIdsAreNotFound() {
		// given teacher A logged in, and a course "Грамматика" of teacher B with the lesson "Падежи"
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", null);
		createLesson(teacherB, foreign, "Падежи", 1);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the lessons of teacher B's course, of a course with an unknown id and of the course "not-a-uuid"
		var foreignResponse = teacher.get(lessons(foreign));
		var unknownResponse = teacher.get(lessons(UUID.randomUUID()));
		var malformedResponse = teacher.get(lessons("not-a-uuid"));

		// then all three responses are 404 NOT_FOUND with no errors property, and none contains "Грамматика" or "Падежи"
		for (Response response : List.of(foreignResponse, unknownResponse, malformedResponse)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Грамматика")))
					.body(not(containsString("Падежи")));
		}
	}

	@Test
	@DisplayName("[lessons/R2-S6] Only active teachers list lessons")
	void onlyActiveTeachersListLessons() {
		// given a course of teacher A with a lesson, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		createLesson(teacherA, course, "Lesson", 1);
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();

		// when each of them lists the lessons of the course
		var adminResponse = admin.get(lessons(course));
		var studentResponse = student.get(lessons(course));
		var deactivatedResponse = deactivated.get(lessons(course));
		var anonymousResponse = anonymous.get(lessons(course));

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
	@DisplayName("[lessons/R2-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A who is then deactivated
		TestClient admin = loggedInAdmin();
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();

		// when the admin and teacher A each list the lessons of the course "not-a-uuid"
		var adminResponse = admin.get(lessons("not-a-uuid"));
		var deactivatedResponse = deactivated.get(lessons("not-a-uuid"));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}
}
