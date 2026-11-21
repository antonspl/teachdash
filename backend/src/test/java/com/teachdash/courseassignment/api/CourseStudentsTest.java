package com.teachdash.courseassignment.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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

/** course-assignment R2. */
class CourseStudentsTest extends ApiTest {

	private static String students(Object courseId) {
		return "/api/teacher/courses/" + courseId + "/students";
	}

	private UUID student(UUID teacher, String name, String email) {
		UUID id = createUser("STUDENT", name, email, "student-pass-1", true);
		assignStudent(teacher, id);
		return id;
	}

	@Test
	@DisplayName("[course-assignment/R2-S1] Students are listed by name, in English and Russian")
	void studentsAreListedByNameInEnglishAndRussian() {
		// given a logged-in teacher A with the course "English B1" and the students "Boris", "anna", "Ёлка", "Анна", "яна" and two students both named "Zed" assigned to it, inserted in that order
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		String[] names = { "Boris", "anna", "Ёлка", "Анна", "яна", "Zed", "Zed" };
		for (int i = 0; i < names.length; i++) {
			assignCourse(course, student(teacherA, names[i], "s" + i + "@example.com"));
		}
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the students of the course
		var response = teacher.get(students(course));

		// then the response is 200 with the names in that order
		response.then().statusCode(200)
				.body("name", org.hamcrest.Matchers.contains(
						"anna", "Boris", "Zed", "Zed", "Анна", "Ёлка", "яна"));
		// and the two "Zed" items are ordered by id
		List<String> zedIds = response.path("findAll { it.name == 'Zed' }.id");
		assertEquals(zedIds.stream().sorted().toList(), zedIds);
	}

	@Test
	@DisplayName("[course-assignment/R2-S2] A list item has only the student's id, name and assignment time")
	void aListItemHasOnlyTheStudentsIdNameAndAssignmentTime() {
		// given a logged-in teacher A with the course "Литература 9 класс" and the student "Анна Иванова" with an email, assigned at a time T
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID anna = student(teacherA, "Анна Иванова", "anna.ivanova@example.com");
		Instant t = Instant.now(clock).minus(Duration.ofDays(1));
		assignCourse(course, anna, t);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the students of the course
		var response = teacher.get(students(course));

		// then the item has exactly the properties id, name and assignedAt, with the student's id, the name and T
		response.then().statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].keySet()", containsInAnyOrder("id", "name", "assignedAt"))
				.body("[0].id", equalTo(anna.toString()))
				.body("[0].name", equalTo("Анна Иванова"));
		assertEquals(t, Instant.parse(response.path("[0].assignedAt")));
		// and the response does not contain the email
		response.then().body(not(containsString("anna.ivanova@example.com")));
	}

	@Test
	@DisplayName("[course-assignment/R2-S3] A course without students gives an empty list")
	void aCourseWithoutStudentsGivesAnEmptyList() {
		// given a logged-in teacher A with a course with no assigned students, and an own student who is assigned to another course only
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID empty = createCourse(teacherA, "English B1", "en", "en");
		UUID other = createCourse(teacherA, "Maths", "en", null);
		assignCourse(other, student(teacherA, "Anna", "anna@example.com"));
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the students of the first course
		var response = teacher.get(students(empty));

		// then the response is 200 with an empty array
		response.then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[course-assignment/R2-S4] The list shows only that course's students")
	void theListShowsOnlyThatCoursesStudents() {
		// given a logged-in teacher A with the courses "English B1" and "Maths", the students "Anna" (both), "Boris" ("Maths" only) and "Вера" (none), and a teacher B with a course and the student "Глеб" assigned to it
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID maths = createCourse(teacherA, "Maths", "en", null);
		UUID anna = student(teacherA, "Anna", "anna@example.com");
		UUID boris = student(teacherA, "Boris", "boris@example.com");
		student(teacherA, "Вера", "vera@example.com");
		assignCourse(english, anna);
		assignCourse(maths, anna);
		assignCourse(maths, boris);
		UUID courseB = createCourse(teacherB, "Грамматика", "ru", null);
		assignCourse(courseB, student(teacherB, "Глеб", "gleb@example.com"));
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the students of "English B1"
		var response = teacher.get(students(english));

		// then the response is 200 with exactly one item, "Anna"
		response.then().statusCode(200)
				.body("size()", equalTo(1))
				.body("[0].name", equalTo("Anna"));
		// and it does not contain "Boris", "Вера" or "Глеб"
		response.then()
				.body(not(containsString("Boris")))
				.body(not(containsString("Вера")))
				.body(not(containsString("Глеб")));
	}

	@Test
	@DisplayName("[course-assignment/R2-S5] Another teacher's, unknown and malformed course ids are not found")
	void anotherTeachersUnknownAndMalformedCourseIdsAreNotFound() {
		// given teacher A logged in, and a course "Грамматика" of teacher B with the assigned student "Глеб"
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID grammar = createCourse(teacherB, "Грамматика", "ru", null);
		assignCourse(grammar, student(teacherB, "Глеб", "gleb@example.com"));
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists the students of teacher B's course, of a course with an unknown id and of the course "not-a-uuid"
		List<Response> responses = List.of(
				teacher.get(students(grammar)),
				teacher.get(students(UUID.randomUUID())),
				teacher.get(students("not-a-uuid")));

		// then all three responses are 404 NOT_FOUND with no errors property, and none contains "Грамматика" or "Глеб"
		for (Response response : responses) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")))
					.body(not(containsString("Грамматика")))
					.body(not(containsString("Глеб")));
		}
	}

	@Test
	@DisplayName("[course-assignment/R2-S6] Only active teachers read, and checks run in a fixed order")
	void onlyActiveTeachersReadAndChecksRunInAFixedOrder() {
		// given a course of teacher A with an assigned student, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		assignCourse(course, student(teacherA, "Anna", "anna@example.com"));
		TestClient admin = loggedInAdmin();
		TestClient student = loggedInAs("anna@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();

		// when each of them lists the students of the course, and the admin and teacher A then each list the students of the course "not-a-uuid"
		var adminResponse = admin.get(students(course));
		var studentResponse = student.get(students(course));
		var deactivatedResponse = deactivated.get(students(course));
		var anonymousResponse = anonymous.get(students(course));
		var adminMalformed = admin.get(students("not-a-uuid"));
		var deactivatedMalformed = deactivated.get(students("not-a-uuid"));

		// then the admin's and the student's responses are 403 FORBIDDEN
		for (Response response : List.of(adminResponse, studentResponse)) {
			response.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		}
		// and the deactivated teacher's and the client's responses are 401 UNAUTHENTICATED
		for (Response response : List.of(deactivatedResponse, anonymousResponse)) {
			response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		}
		// and for "not-a-uuid" the admin's response is 403 and teacher A's is 401, neither a 404
		adminMalformed.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedMalformed.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}
}
