package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
	@DisplayName("[student-enrolment/R5-S1] Student sees only their own teachers")
	void studentSeesOnlyTheirOwnTeachers() {
		// given a student assigned to teachers A and B, and teacher C with another student
		UUID teacherA = teacher("Anna", "a@example.com");
		UUID teacherB = teacher("Boris", "b@example.com");
		UUID teacherC = teacher("Clara", "c@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		UUID other = createUser("STUDENT", "Other", "other@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		assignStudent(teacherB, student);
		assignStudent(teacherC, other);
		TestClient client = loggedInAs("student@example.com");

		// when the student lists their teachers
		var response = client.get(TEACHERS);

		// then the response is 200, the list contains A and B only, and each entry has only the fields id and name
		response.then().statusCode(200)
				.body("id", containsInAnyOrder(teacherA.toString(), teacherB.toString()));
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
	@DisplayName("[student-enrolment/R5-S3] Deactivated teacher is hidden")
	void deactivatedTeacherIsHidden() {
		// given a student assigned to an active teacher and a deactivated teacher
		UUID active = teacher("Anna", "a@example.com");
		UUID inactive = createUser("TEACHER", "Boris", "b@example.com", "teacher-pass-1", false);
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(active, student);
		assignStudent(inactive, student);
		TestClient client = loggedInAs("student@example.com");

		// when the student lists their teachers
		var response = client.get(TEACHERS);

		// then the list contains the active teacher only
		response.then().statusCode(200).body("id", contains(active.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R5-S4] Reactivated teacher appears again")
	void reactivatedTeacherAppearsAgain() {
		// given a student assigned to a deactivated teacher who has a password
		UUID teacher = createUser("TEACHER", "Anna", "a@example.com", "teacher-pass-1", false);
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacher, student);
		TestClient admin = loggedInAdmin();
		TestClient client = loggedInAs("student@example.com");

		// when the admin reactivates the teacher and the student lists their teachers
		var reactivated = admin.post("/api/admin/teachers/" + teacher + "/reactivate");
		var response = client.get(TEACHERS);

		// then the list contains that teacher
		reactivated.then().statusCode(204);
		response.then().statusCode(200).body("id", contains(teacher.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R5-S5] Name order ignores case and follows the alphabet")
	void nameOrderIgnoresCaseAndFollowsTheAlphabet() {
		// given a student with teachers "Яков", "борис" and "Anna"
		UUID yakov = teacher("Яков", "yakov@example.com");
		UUID boris = teacher("борис", "boris@example.com");
		UUID anna = teacher("Anna", "anna@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(yakov, student);
		assignStudent(boris, student);
		assignStudent(anna, student);
		TestClient client = loggedInAs("student@example.com");

		// when the student lists their teachers
		var response = client.get(TEACHERS);

		// then the order is "Anna", "борис", "Яков"
		response.then().statusCode(200).body("name", contains("Anna", "борис", "Яков"));
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
