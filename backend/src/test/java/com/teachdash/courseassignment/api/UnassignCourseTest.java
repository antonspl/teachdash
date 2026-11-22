package com.teachdash.courseassignment.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
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

/** course-assignment R3. */
class UnassignCourseTest extends ApiTest {

	private static String assignment(Object courseId, Object studentId) {
		return "/api/teacher/courses/" + courseId + "/students/" + studentId;
	}

	private UUID student(UUID teacher, String name, String email) {
		UUID id = createUser("STUDENT", name, email, "student-pass-1", true);
		assignStudent(teacher, id);
		return id;
	}

	private Set<String> assignedPairs() {
		return Set.copyOf(readAssignments().stream()
				.map(r -> r.courseId() + "|" + r.studentId())
				.toList());
	}

	private static void assertNotFound(Response response) {
		response.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
	}

	@Test
	@DisplayName("[course-assignment/R3-S1] Unassign one course")
	void unassignOneCourse() {
		// given a logged-in teacher A with the courses "English B1" and "Литература 9 класс" and the student "Анна", assigned to both, and the student "Boris", assigned to "English B1"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID literature = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID anna = student(teacherA, "Анна", "anna@example.com");
		UUID boris = student(teacherA, "Boris", "boris@example.com");
		assignCourse(english, anna);
		assignCourse(literature, anna);
		assignCourse(english, boris);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A unassigns "English B1" from "Анна"
		var response = teacher.delete(assignment(english, anna));

		// then the response is 204 with an empty body
		response.then().statusCode(204).body(emptyOrNullString());
		// and "Анна" is assigned to "Литература 9 класс" only, and "Boris" is still assigned to "English B1"
		assertEquals(Set.of(literature + "|" + anna, english + "|" + boris), assignedPairs());
		// and "Анна" is still teacher A's student
		assertEquals(1, jdbc.sql("select count(*) from teacher_students where teacher_id = :t and student_id = :s")
				.param("t", teacherA).param("s", anna).query(Long.class).single());
	}

	@Test
	@DisplayName("[course-assignment/R3-S2] A second unassign and an unassigned student are not found")
	void aSecondUnassignAndAnUnassignedStudentAreNotFound() {
		// given a logged-in teacher A with a course and the students "Anna" (assigned to it) and "Борис" (assigned to no course)
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = student(teacherA, "Anna", "anna@example.com");
		UUID boris = student(teacherA, "Борис", "boris@example.com");
		assignCourse(course, anna);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A unassigns the course from "Anna", then again from "Anna", and then from "Борис"
		var first = teacher.delete(assignment(course, anna));
		var second = teacher.delete(assignment(course, anna));
		var third = teacher.delete(assignment(course, boris));

		// then the first response is 204
		first.then().statusCode(204);
		// and the second and the third are 404 NOT_FOUND with no errors property
		assertNotFound(second);
		assertNotFound(third);
		// and no assignment is stored
		assertEquals(0, countRows("course_assignments"));
	}

	@Test
	@DisplayName("[course-assignment/R3-S3] Another teacher's, unknown and malformed ids are not found")
	void anotherTeachersUnknownAndMalformedIdsAreNotFound() {
		// given teacher A logged in with a course "English B1" and a student "Anna" assigned to it, a student "Борис" of teacher B assigned to teacher B's course "Грамматика"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = student(teacherA, "Anna", "anna@example.com");
		assignCourse(english, anna);
		UUID grammar = createCourse(teacherB, "Грамматика", "ru", null);
		UUID boris = student(teacherB, "Борис", "boris@example.com");
		assignCourse(grammar, boris);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A unassigns "Борис" from "English B1" and from "Грамматика", "Anna" from "Грамматика", from a course with an unknown id and from the course "not-a-uuid", and an unknown student id and the student id "not-a-uuid" from "English B1"
		List<Response> responses = List.of(
				teacher.delete(assignment(english, boris)),
				teacher.delete(assignment(grammar, boris)),
				teacher.delete(assignment(grammar, anna)),
				teacher.delete(assignment(UUID.randomUUID(), anna)),
				teacher.delete(assignment("not-a-uuid", anna)),
				teacher.delete(assignment(english, UUID.randomUUID())),
				teacher.delete(assignment(english, "not-a-uuid")));

		// then all seven responses are 404 NOT_FOUND with no errors property, and none contains "Борис" or "Грамматика"
		assertEquals(7, responses.size());
		for (Response response : responses) {
			assertNotFound(response);
			response.then()
					.body(not(containsString("Борис")))
					.body(not(containsString("Грамматика")));
		}
		// and both assignments are still stored
		assertEquals(Set.of(english + "|" + anna, grammar + "|" + boris), assignedPairs());
	}

	@Test
	@DisplayName("[course-assignment/R3-S4] Only active teachers unassign")
	void onlyActiveTeachersUnassign() {
		// given a course and a student of teacher A with an assignment, a logged-in admin, a logged-in student, a logged-in teacher A who is then deactivated, a client with a CSRF cookie and no session, and a logged-in teacher B
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID course = createCourse(teacherA, "English B1", "en", "en");
		UUID anna = student(teacherA, "Anna", "anna@example.com");
		assignCourse(course, anna);
		TestClient admin = loggedInAdmin();
		TestClient student = loggedInAs("anna@example.com");
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();
		TestClient anonymous = newClient();
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient withoutCsrf = loggedInAs("b@example.com").withoutCsrfHeader();

		// when the admin, the student, teacher A and the client each unassign the course from the student, and teacher B does so without a CSRF token
		var adminResponse = admin.delete(assignment(course, anna));
		var studentResponse = student.delete(assignment(course, anna));
		var deactivatedResponse = deactivated.delete(assignment(course, anna));
		var anonymousResponse = anonymous.delete(assignment(course, anna));
		var csrfResponse = withoutCsrf.delete(assignment(course, anna));

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
		// and the assignment is still stored
		assertEquals(Set.of(course + "|" + anna), assignedPairs());
	}

	@Test
	@DisplayName("[course-assignment/R3-S5] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in admin, and a logged-in teacher A who is then deactivated
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient admin = loggedInAdmin();
		TestClient deactivated = loggedInAs("a@example.com");
		jdbc.sql("update users set active = false where email = 'a@example.com'").update();

		// when the admin and teacher A each unassign the student id "not-a-uuid" from the course "not-a-uuid"
		var adminResponse = admin.delete(assignment("not-a-uuid", "not-a-uuid"));
		var deactivatedResponse = deactivated.delete(assignment("not-a-uuid", "not-a-uuid"));

		// then the admin's response is 403 FORBIDDEN and teacher A's is 401 UNAUTHENTICATED, neither is a 404
		adminResponse.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		deactivatedResponse.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}

	@Test
	@DisplayName("[course-assignment/R3-S6] Removing a student deletes all of the student's assignments")
	void removingAStudentDeletesAllOfTheStudentsAssignments() {
		// given teacher A with the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Анна", both assigned to both courses, and teacher B with a course and the student "Глеб" assigned to it
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID literature = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID latinAnna = createUser("STUDENT", "Anna", "anna@example.com", null, true);
		assignStudent(teacherA, latinAnna);
		UUID cyrillicAnna = createUser("STUDENT", "Анна", "anna.ru@example.com", null, true);
		assignStudent(teacherA, cyrillicAnna);
		for (UUID course : List.of(english, literature)) {
			assignCourse(course, latinAnna);
			assignCourse(course, cyrillicAnna);
		}
		UUID grammar = createCourse(teacherB, "Грамматика", "ru", null);
		UUID gleb = student(teacherB, "Глеб", "gleb@example.com");
		assignCourse(grammar, gleb);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A removes "Anna", and then registers "Anna" again as the student of teacher A
		var removal = teacher.delete("/api/teacher/students/" + latinAnna);
		var registration = teacher.post("/api/teacher/students",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "UTC", "nativeLanguage", "en"));

		// then the removal response is 204 and the registration response is 201
		removal.then().statusCode(204);
		registration.then().statusCode(201);
		// and no assignment of "Anna" is stored, "Анна" is still assigned to both courses, "Глеб" to teacher B's course
		assertEquals(Set.of(
				english + "|" + cyrillicAnna, literature + "|" + cyrillicAnna, grammar + "|" + gleb),
				assignedPairs());
		// and teacher A's lists of the students of both courses do not contain "Anna"
		for (UUID course : List.of(english, literature)) {
			teacher.get("/api/teacher/courses/" + course + "/students").then().statusCode(200)
					.body("name", not(org.hamcrest.Matchers.hasItem("Anna")));
		}
	}

	@Test
	@DisplayName("[course-assignment/R3-S7] Deactivating a teacher deletes the assignments of the teacher's courses")
	void deactivatingATeacherDeletesTheAssignmentsOfTheTeachersCourses() {
		// given teacher A with a password, the courses "English B1" and "Литература 9 класс" and the students "Anna" and "Анна" assigned to them, and teacher B with a course and the student "Глеб" assigned to it
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID english = createCourse(teacherA, "English B1", "en", "en");
		UUID literature = createCourse(teacherA, "Литература 9 класс", "ru", null);
		UUID anna = student(teacherA, "Anna", "anna@example.com");
		UUID cyrillicAnna = student(teacherA, "Анна", "anna.ru@example.com");
		for (UUID course : List.of(english, literature)) {
			assignCourse(course, anna);
			assignCourse(course, cyrillicAnna);
		}
		UUID grammar = createCourse(teacherB, "Грамматика", "ru", null);
		UUID gleb = student(teacherB, "Глеб", "gleb@example.com");
		assignCourse(grammar, gleb);
		TestClient admin = loggedInAdmin();

		// when the admin deactivates teacher A and then reactivates teacher A
		var deactivation = admin.post("/api/admin/teachers/" + teacherA + "/deactivate");
		var assignmentsAfterDeactivation = assignedPairs();
		var reactivation = admin.post("/api/admin/teachers/" + teacherA + "/reactivate");

		// then the deactivation response is 204 and the reactivation response is 204
		deactivation.then().statusCode(204);
		reactivation.then().statusCode(204);
		// and no assignment of teacher A's courses was stored after the deactivation, and teacher B's "Глеб" is still assigned
		assertEquals(Set.of(grammar + "|" + gleb), assignmentsAfterDeactivation);
		assertEquals(Set.of(grammar + "|" + gleb), assignedPairs());
		// and teacher A can log in again, has both courses, has no student, and the lists of the students of both courses are empty
		TestClient teacher = newClient();
		teacher.login("a@example.com", "teacher-pass-1").then().statusCode(200);
		teacher.get("/api/teacher/courses").then().statusCode(200).body("size()", equalTo(2));
		teacher.get("/api/teacher/students").then().statusCode(200).body("$", empty());
		for (UUID course : List.of(english, literature)) {
			teacher.get("/api/teacher/courses/" + course + "/students").then().statusCode(200).body("$", empty());
		}
	}
}
