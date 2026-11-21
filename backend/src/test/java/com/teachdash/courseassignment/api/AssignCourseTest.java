package com.teachdash.courseassignment.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** course-assignment R1. */
class AssignCourseTest extends ApiTest {

	private static String assign(Object courseId, Object studentId) {
		return "/api/teacher/courses/" + courseId + "/students/" + studentId;
	}

	private UUID student(String name, String email) {
		return createUser("STUDENT", name, email, "student-pass-1", true);
	}

	private UUID teacherStudent(UUID teacher, String name, String email) {
		UUID id = student(name, email);
		assignStudent(teacher, id);
		return id;
	}

	private static void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
	}

	@Test
	@DisplayName("[course-assignment/R1-S1] Assign an English course to a student")
	void assignAnEnglishCourseToAStudent() {
		// given a logged-in teacher A with the course "English B1" and the student "Anna Smith", and the clock fixed
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = teacherStudent(teacherA, "Anna Smith", "anna@example.com");
		TestClient teacher = loggedInAs("a@example.com");
		Instant now = Instant.now(clock);

		// when teacher A assigns the course to the student
		var response = teacher.put(assign(course, anna));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and exactly one assignment is stored, for that course and that student, owned by teacher A, created at the clock value
		var rows = readAssignments();
		assertEquals(1, rows.size());
		assertEquals(course, rows.get(0).courseId());
		assertEquals(anna, rows.get(0).studentId());
		assertEquals(teacherA, rows.get(0).teacherId());
		assertEquals(now, rows.get(0).createdAt());
	}

	@Test
	@DisplayName("[course-assignment/R1-S2] Assign a Russian course to a student with a Cyrillic name")
	void assignARussianCourseToAStudentWithACyrillicName() {
		// given a logged-in teacher A with the course "Литература 9 класс" (instruction "ru", no target) and the student "Анна Иванова"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID anna = teacherStudent(teacherA, "Анна Иванова", "anna@example.com");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A assigns the course to the student
		var response = teacher.put(assign(course, anna));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and the student is stored as assigned to the course
		var rows = readAssignments();
		assertEquals(1, rows.size());
		assertEquals(course, rows.get(0).courseId());
		assertEquals(anna, rows.get(0).studentId());
	}

	@Test
	@DisplayName("[course-assignment/R1-S3] Assigning again is a no-op")
	void assigningAgainIsANoOp() {
		// given a logged-in teacher A with a course and a student, assigned at a time T, and the clock advanced by one hour
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = teacherStudent(teacherA, "Anna", "anna@example.com");
		Instant t = Instant.now(clock);
		assignCourse(course, anna, t);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A assigns the course to the student again
		var response = teacher.put(assign(course, anna));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and exactly one assignment is stored, and its creation time is still T
		var rows = readAssignments();
		assertEquals(1, rows.size());
		assertEquals(t, rows.get(0).createdAt());
	}

	@Test
	@DisplayName("[course-assignment/R1-S4] Many courses and many students, assignments are independent")
	void manyCoursesAndManyStudentsAssignmentsAreIndependent() {
		// given a logged-in teacher A with the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Борис", where "Anna" is already assigned to "English B1"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID literature = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID anna = teacherStudent(teacherA, "Anna", "anna@example.com");
		UUID boris = teacherStudent(teacherA, "Борис", "boris@example.com");
		Instant earlier = Instant.now(clock);
		assignCourse(english, anna, earlier);
		TestClient teacher = loggedInAs("a@example.com");
		testClock().advance(Duration.ofHours(1));

		// when teacher A assigns "English B1" to "Борис", and "Литература 9 класс" to "Anna" and to "Борис"
		var first = teacher.put(assign(english, boris));
		var second = teacher.put(assign(literature, anna));
		var third = teacher.put(assign(literature, boris));

		// then all responses are 204
		first.then().statusCode(204);
		second.then().statusCode(204);
		third.then().statusCode(204);
		// and exactly four assignments are stored, with the assignment of "Anna" to "English B1" unchanged
		var rows = readAssignments();
		assertEquals(4, rows.size());
		assertEquals(Set.of(
				english + "|" + anna, english + "|" + boris,
				literature + "|" + anna, literature + "|" + boris),
				Set.copyOf(rows.stream().map(r -> r.courseId() + "|" + r.studentId()).toList()));
		assertEquals(earlier, rows.stream()
				.filter(r -> r.courseId().equals(english) && r.studentId().equals(anna))
				.findFirst().orElseThrow().createdAt());
	}

	@Test
	@DisplayName("[course-assignment/R1-S5] Another teacher's, unknown and malformed ids are not found")
	void anotherTeachersUnknownAndMalformedIdsAreNotFound() {
		// given teacher A logged in with a course "English B1", a student "Anna" of teacher A and a student "Борис" of teacher B, a student "Вера" with no teacher, an admin, and teacher B's course "Грамматика"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = teacherStudent(teacherA, "Anna", "anna@example.com");
		UUID boris = teacherStudent(teacherB, "Борис", "boris@example.com");
		UUID vera = student("Вера", "vera@example.com");
		UUID admin = createAdmin("admin@example.com", "admin-password-1");
		UUID grammar = createCourse(teacherB, "Грамматика", "ru", null);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A assigns "English B1" to "Борис", to "Вера", to the admin's id, to teacher B's id, to an unknown id and to the id "not-a-uuid"
		List<Response> responses = new ArrayList<>();
		for (Object studentId : List.of(boris, vera, admin, teacherB, UUID.randomUUID(), "not-a-uuid")) {
			responses.add(teacher.put(assign(english, studentId)));
		}
		// and assigns teacher B's course, a course with an unknown id and the course "not-a-uuid" to "Anna"
		for (Object courseId : List.of(grammar, UUID.randomUUID(), "not-a-uuid")) {
			responses.add(teacher.put(assign(courseId, anna)));
		}

		// then all nine responses are 404 with code NOT_FOUND and no errors property, and none contains the other teacher's data
		assertEquals(9, responses.size());
		for (Response response : responses) {
			assertNotFound(response);
			response.then()
					.body(not(containsString("Борис")))
					.body(not(containsString("Вера")))
					.body(not(containsString("Грамматика")));
		}
		// and no assignment is stored
		assertEquals(0, countRows("course_assignments"));
	}

	@Test
	@DisplayName("[course-assignment/R1-S6] Only active teachers assign")
	void onlyActiveTeachersAssign() {
		// given a course and a student of teacher A, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = teacherStudent(teacherA, "Anna", "anna@example.com");
		TestClient admin = loggedInAdmin();
		TestClient student = loggedInAs("anna@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each assign the course to the student, and teacher B does so without a CSRF token
		var adminResponse = admin.put(assign(course, anna));
		var studentResponse = student.put(assign(course, anna));
		var deactivatedResponse = deactivated.put(assign(course, anna));
		var anonymousResponse = anonymous.put(assign(course, anna));
		var csrfResponse = withoutCsrf.put(assign(course, anna));

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
		// and no assignment is stored
		assertEquals(0, countRows("course_assignments"));
	}

	@Test
	@DisplayName("[course-assignment/R1-S7] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, a logged-in teacher A who is then deactivated, and a logged-in teacher B with a course of teacher A's and a student of teacher A's
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID courseA = createCourse(teacherA, "English B1", "en", "en");
		UUID courseB = createCourse(teacherB, "Грамматика", "ru", null);
		UUID studentA = teacherStudent(teacherA, "Anna", "anna@example.com");
		UUID studentB = teacherStudent(teacherB, "Борис", "boris@example.com");
		TestClient admin = loggedInAdmin();
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient teacherBClient = loggedInAs("b@example.com");

		// when the admin and teacher A each assign the course "not-a-uuid" to the student id "not-a-uuid"
		var adminResponse = admin.put(assign("not-a-uuid", "not-a-uuid"));
		var deactivatedResponse = deactivated.put(assign("not-a-uuid", "not-a-uuid"));
		// and teacher B assigns the course of teacher A to a student of teacher B and then a course of teacher B to the student of teacher A
		var foreignCourse = teacherBClient.put(assign(courseA, studentB));
		var foreignStudent = teacherBClient.put(assign(courseB, studentA));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and teacher B's two responses are 404 NOT_FOUND, not 400
		assertNotFound(foreignCourse);
		assertNotFound(foreignStudent);
		// and no assignment is stored
		assertEquals(0, countRows("course_assignments"));
	}

	@Test
	@DisplayName("[course-assignment/R1-S8] Assigning in parallel with the student's removal leaves no assignment of a removed student")
	void assigningInParallelWithTheStudentsRemovalLeavesNoAssignmentOfARemovedStudent() throws Exception {
		// given a logged-in teacher A with 5 courses and a student "Анна", and the clock fixed
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID anna = teacherStudent(teacherA, "Анна", "anna@example.com");
		List<UUID> courses = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			courses.add(createCourse(teacherA, "Course " + i, "en", "en"));
		}
		TestClient client = loggedInAs("a@example.com");

		// when teacher A removes the student and assigns each of the 5 courses to the student at the same time
		List<Callable<Response>> calls = new ArrayList<>();
		calls.add(() -> client.delete("/api/teacher/students/" + anna));
		for (UUID course : courses) {
			calls.add(() -> client.put(assign(course, anna)));
		}
		List<Response> responses = Parallel.run(calls);

		// then the removal response is 204
		responses.get(0).then().statusCode(204);
		// and each assignment response is 204 or 404 NOT_FOUND, never a 500
		for (Response response : responses.subList(1, responses.size())) {
			assertTrue(response.statusCode() == 204 || response.statusCode() == 404,
					"unexpected status " + response.statusCode());
			if (response.statusCode() == 404) {
				response.then().body("code", equalTo("NOT_FOUND"));
			}
		}
		// and the student is no longer teacher A's student, and no assignment of the student is stored
		assertEquals(0, jdbc.sql("select count(*) from teacher_students where student_id = :student")
				.param("student", anna).query(Long.class).single());
		assertEquals(0, jdbc.sql("select count(*) from course_assignments where student_id = :student")
				.param("student", anna).query(Long.class).single());
	}
}
