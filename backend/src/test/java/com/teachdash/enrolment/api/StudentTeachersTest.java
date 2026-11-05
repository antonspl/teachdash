package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R5. */
class StudentTeachersTest extends ApiTest {

	private static final String TEACHERS = "/api/student/teachers";

	private UUID teacher(String name, String email) {
		return createUser("TEACHER", name, email, "teacher-pass-1", true);
	}

	@Test
	@DisplayName("[student-enrolment/R5-S1] Student sees only their own teacher")
	void studentSeesOnlyTheirOwnTeacher() {
		// given a student assigned to teacher A, and teacher C with another student
		UUID teacherA = teacher("Anna", "a@example.com");
		UUID teacherC = teacher("Clara", "c@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		UUID other = createUser("STUDENT", "Other", "other@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		assignStudent(teacherC, other);
		TestClient client = loggedInAs("student@example.com");

		// when the student lists their teachers
		var response = client.get(TEACHERS);

		// then the response is 200, the list contains A only, and the entry has only the fields id and name
		response.then().statusCode(200).body("id", contains(teacherA.toString()));
		List<Map<String, Object>> entries = response.jsonPath().getList("$");
		for (Map<String, Object> entry : entries) {
			assertEquals(Set.of("id", "name"), entry.keySet());
		}
	}

	@Test
	@DisplayName("[student-enrolment/R5-S2] Student without teachers gets an empty list")
	void studentWithoutTeachersGetsAnEmptyList() {
		// given a logged-in student with no teachers
		createStudent("student@example.com", "student-pass-1");
		TestClient client = loggedInAs("student@example.com");

		// when the student lists their teachers
		var response = client.get(TEACHERS);

		// then the response is 200 with an empty list
		response.then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R5-S6] Teachers and admins cannot call the student endpoint")
	void teachersAndAdminsCannotCallTheStudentEndpoint() {
		// given a logged-in teacher and a logged-in admin
		teacher("Anna", "a@example.com");
		TestClient teacher = loggedInAs("a@example.com");
		TestClient admin = loggedInAdmin();

		// when each calls the student endpoint that lists teachers
		var byTeacher = teacher.get(TEACHERS);
		var byAdmin = admin.get(TEACHERS);

		// then both responses are 403
		byTeacher.then().statusCode(403);
		byAdmin.then().statusCode(403);
	}

	@Test
	@DisplayName("[student-enrolment/R5-S7] Student endpoint requires a session")
	void studentEndpointRequiresASession() {
		// given no session
		TestClient anonymous = newClient();

		// when a client calls the student endpoint that lists teachers
		var response = anonymous.get(TEACHERS);

		// then the response is 401 with code UNAUTHENTICATED
		response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}
}
