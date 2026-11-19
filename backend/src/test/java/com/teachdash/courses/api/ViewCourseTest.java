package com.teachdash.courses.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** courses R4. */
class ViewCourseTest extends ApiTest {

	private static final String COURSES = "/api/teacher/courses";

	@Test
	@DisplayName("[courses/R4-S1] A teacher views their course")
	void aTeacherViewsTheirCourse() {
		// given a logged-in teacher A with the courses "English B1" and "Литература 9 класс"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant now = Instant.now(clock);
		UUID english = createCourse(teacherA, "English B1", "en", "en", now.minus(Duration.ofHours(1)));
		UUID russian = createCourse(teacherA, "Литература 9 класс", "ru", null, now);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A views each of them by id
		var englishResponse = teacher.get(COURSES + "/" + english);
		var russianResponse = teacher.get(COURSES + "/" + russian);

		// then both responses are 200 with that course's data
		englishResponse.then().statusCode(200)
				.body("id", equalTo(english.toString()))
				.body("title", equalTo("English B1"))
				.body("instructionLanguage", equalTo("en"))
				.body("targetLanguage", equalTo("en"));
		russianResponse.then().statusCode(200)
				.body("id", equalTo(russian.toString()))
				.body("title", equalTo("Литература 9 класс"))
				.body("instructionLanguage", equalTo("ru"))
				.body("targetLanguage", equalTo(null));
		// and createdAt and updatedAt are the stored values
		for (var pair : List.of(
				new Object[] { english, englishResponse },
				new Object[] { russian, russianResponse })) {
			var stored = readCourse((UUID) pair[0]);
			Response response = (Response) pair[1];
			assertEquals(stored.createdAt(), Instant.parse(response.path("createdAt")));
			assertEquals(stored.updatedAt(), Instant.parse(response.path("updatedAt")));
		}
	}

	@Test
	@DisplayName("[courses/R4-S2] Another teacher's, unknown and malformed ids are not found")
	void anotherTeachersUnknownAndMalformedIdsAreNotFound() {
		// given teacher A logged in, and a course "Грамматика" of teacher B
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID foreign = createCourse(teacherB, "Грамматика", "ru", "ru");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A views teacher B's course, an unknown id and the id "not-a-uuid"
		var foreignResponse = teacher.get(COURSES + "/" + foreign);
		var unknownResponse = teacher.get(COURSES + "/" + UUID.randomUUID());
		var malformedResponse = teacher.get(COURSES + "/not-a-uuid");

		// then all three responses are 404 NOT_FOUND without errors, and none contains "Грамматика"
		for (Response response : List.of(foreignResponse, unknownResponse, malformedResponse)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Грамматика")));
		}
	}

	@Test
	@DisplayName("[courses/R4-S3] Only active teachers view a course")
	void onlyActiveTeachersViewACourse() {
		// given a course of teacher A, a logged-in admin, a logged-in student, teacher A logged in and then deactivated, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Grammar", "en", "en");
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();

		// when each of them views the course
		var adminResponse = admin.get(COURSES + "/" + course);
		var studentResponse = student.get(COURSES + "/" + course);
		var deactivatedResponse = deactivated.get(COURSES + "/" + course);
		var anonymousResponse = anonymous.get(COURSES + "/" + course);

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
	}
}
