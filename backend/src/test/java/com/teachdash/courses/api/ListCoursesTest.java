package com.teachdash.courses.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
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

/** courses R3. */
class ListCoursesTest extends ApiTest {

	private static final String COURSES = "/api/teacher/courses";

	private static UUID id(int n) {
		return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(n));
	}

	@Test
	@DisplayName("[courses/R3-S1] Courses are listed newest first")
	void coursesAreListedNewestFirst() {
		// given a logged-in teacher A with three courses created at the middle, oldest and newest times, inserted in that order
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant newest = Instant.now(clock);
		createCourse(teacherA, "Литература 9 класс", "ru", null, newest.minus(Duration.ofHours(1)));
		createCourse(teacherA, "English B1", "en", "en", newest.minus(Duration.ofHours(2)));
		createCourse(teacherA, "Maths", "en", null, newest);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the response is 200 with the titles newest first
		response.then().statusCode(200)
				.body("title", contains("Maths", "Литература 9 класс", "English B1"));
	}

	@Test
	@DisplayName("[courses/R3-S2] Renaming does not move a course in the list")
	void renamingDoesNotMoveACourseInTheList() {
		// given a logged-in teacher A with the courses "Old", "Middle" and "New" created at increasing times, and the clock advanced
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant start = Instant.now(clock);
		UUID old = createCourse(teacherA, "Old", "en", null, start);
		createCourse(teacherA, "Middle", "en", null, start.plus(Duration.ofMinutes(1)));
		createCourse(teacherA, "New", "en", null, start.plus(Duration.ofMinutes(2)));
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A renames "Old" to "Oldest" and then lists the courses
		teacher.patch(COURSES + "/" + old, Map.of("title", "Oldest"));
		var response = teacher.get(COURSES);

		// then the list has the titles in the order "New", "Middle", "Oldest"
		response.then().statusCode(200)
				.body("title", contains("New", "Middle", "Oldest"));
	}

	@Test
	@DisplayName("[courses/R3-S3] Equal creation times are ordered by id")
	void equalCreationTimesAreOrderedById() {
		// given a logged-in teacher A with three courses created at the same time, with fixed ids inserted in the order 0002, 0003, 0001
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant created = Instant.now(clock);
		createCourse(id(2), teacherA, "Second", "en", null, created);
		createCourse(id(3), teacherA, "Third", "en", null, created);
		createCourse(id(1), teacherA, "First", "en", null, created);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the response is 200 with the courses in the order of ids 0001, 0002, 0003
		response.then().statusCode(200)
				.body("id", contains(id(1).toString(), id(2).toString(), id(3).toString()));
	}

	@Test
	@DisplayName("[courses/R3-S4] A teacher without courses gets an empty list")
	void aTeacherWithoutCoursesGetsAnEmptyList() {
		// given a logged-in teacher A with no courses and a teacher B with one course
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		createCourse(teacherB, "Grammar", "en", "en");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the response is 200 with an empty array
		response.then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[courses/R3-S5] All twenty courses are listed")
	void allTwentyCoursesAreListed() {
		// given a logged-in teacher A with 20 courses
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		Instant start = Instant.now(clock);
		for (int i = 1; i <= 20; i++) {
			createCourse(teacherA, "Course " + i, "en", null, start.plus(Duration.ofMinutes(i)));
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the response is 200 with an array of 20 items, all different
		response.then().statusCode(200).body("$", hasSize(20));
		List<String> ids = response.path("id");
		assertEquals(20, Set.copyOf(ids).size());
	}

	@Test
	@DisplayName("[courses/R3-S6] The list never shows another teacher's courses")
	void theListNeverShowsAnotherTeachersCourses() {
		// given a logged-in teacher A with the course "Grammar" and a teacher B with the courses "Grammar" and "Грамматика"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID own = createCourse(teacherA, "Grammar", "en", "en");
		createCourse(teacherB, "Grammar", "en", "en");
		createCourse(teacherB, "Грамматика", "ru", "ru");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the response is 200 with exactly one item, the course "Grammar" owned by teacher A
		response.then().statusCode(200)
				.body("$", hasSize(1))
				.body("[0].id", equalTo(own.toString()))
				.body("[0].title", equalTo("Grammar"));
		assertEquals(teacherA, readCourse(UUID.fromString(response.path("[0].id"))).teacherId());
	}

	@Test
	@DisplayName("[courses/R3-S7] A list item has the same fields as a created course")
	void aListItemHasTheSameFieldsAsACreatedCourse() {
		// given a logged-in teacher A with a Russian course without a target language, created at one time
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		createCourse(teacherA, "Литература 9 класс", "ru", null, Instant.now(clock));
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the courses
		var response = teacher.get(COURSES);

		// then the item has exactly the six course properties with the stored values
		response.then().statusCode(200).body("$", hasSize(1));
		Map<String, Object> item = response.path("[0]");
		assertEquals(Set.of("id", "title", "instructionLanguage", "targetLanguage", "createdAt", "updatedAt"),
				item.keySet());
		assertEquals("Литература 9 класс", item.get("title"));
		assertEquals("ru", item.get("instructionLanguage"));
		assertEquals(null, item.get("targetLanguage"));
		// and updatedAt equals createdAt
		assertEquals(item.get("createdAt"), item.get("updatedAt"));
	}

	@Test
	@DisplayName("[courses/R3-S8] Only active teachers list courses")
	void onlyActiveTeachersListCourses() {
		// given a logged-in admin, a logged-in student, a logged-in teacher A with a course who is then deactivated, and a client with a CSRF cookie and no session
		TestClient admin = loggedInAdmin();
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		createCourse(teacherA, "Grammar", "en", "en");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();

		// when each of them lists the courses
		var adminResponse = admin.get(COURSES);
		var studentResponse = student.get(COURSES);
		var deactivatedResponse = deactivated.get(COURSES);
		var anonymousResponse = anonymous.get(COURSES);

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
