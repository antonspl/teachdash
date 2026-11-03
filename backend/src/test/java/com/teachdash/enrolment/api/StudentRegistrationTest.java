package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;

class StudentRegistrationTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";

	@Test
	@DisplayName("[student-enrolment/R1-S1] New email creates and assigns a student")
	void newEmailCreatesAndAssignsAStudent() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher registers a student with a new email and the returned link sets the password
		var response = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "Europe/Berlin"));
		var passwordSet = newClient().post(
				"/api/password-links",
				Map.of("token", tokenFromResponse(response), "password", "new-password-1"));

		// then the response is 201 with the student's id and a link with the base URL and a fragment token
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("setPasswordLink", startsWith(APP_BASE_URL + "/set-password#token="));
		String id = response.path("id");
		// and the student appears in the teacher's list
		teacher.get(STUDENTS).then().statusCode(200).body("id", contains(id));
		// and the link sets the password, the student logs in with it and has the role student
		passwordSet.then().statusCode(200);
		TestClient student = newClient();
		student.login("nina@example.com", "new-password-1").then().statusCode(200);
		student.get("/api/me").then().statusCode(200)
				.body("id", equalTo(id))
				.body("role", equalTo("STUDENT"));
	}

	@Test
	@DisplayName("[student-enrolment/R1-S2] Existing student is not assigned")
	void existingStudentIsNotAssigned() {
		// given a student without a password, assigned to teacher B, with an unused set-password link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Existing", "student@example.com", null, true);
		assignStudent(teacherB, student);
		String token = createToken(student, "SET_PASSWORD", false);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with that email and a different name and time zone
		var response = teacher.post(STUDENTS,
				Map.of("name", "Other Name", "email", "student@example.com", "timeZone", "Asia/Tokyo"));

		// then the response is 409 with code STUDENT_EXISTS
		response.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is not in teacher A's list
		assertEquals(0, jdbc.sql("select count(*) from teacher_students where teacher_id = :teacher")
				.param("teacher", teacherA)
				.query(Long.class)
				.single());
		teacher.get(STUDENTS).then().statusCode(200).body("$", empty());
		// and the student's name and time zone are unchanged
		var stored = jdbc.sql("select name, time_zone from users where id = :id")
				.param("id", student)
				.query((rs, i) -> rs.getString("name") + "|" + rs.getString("time_zone"))
				.single();
		assertEquals("Existing|UTC", stored);
		// and the unused link still sets the password
		newClient().post("/api/password-links", Map.of("token", token, "password", "new-password-1"))
				.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R1-S3] Email of a teacher or admin is rejected")
	void emailOfATeacherOrAdminIsRejected() {
		// given a teacher account and an admin account, and another teacher
		createUser("TEACHER", "Other", "t@example.com", "teacher-pass-1", true);
		createAdmin("a@example.com", "admin-password-1");
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the other teacher registers a student with each of these emails
		var withTeacherEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "t@example.com", "timeZone", "UTC"));
		var withAdminEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "a@example.com", "timeZone", "UTC"));

		// then both responses are 409 with code EMAIL_TAKEN
		withTeacherEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		withAdminEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		// and nothing is assigned
		assertEquals(0, jdbc.sql("select count(*) from teacher_students").query(Long.class).single());
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S4] Already assigned student is rejected")
	void alreadyAssignedStudentIsRejected() {
		// given a student assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with that email again
		var response = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "UTC"));

		// then the response is 409 with code ALREADY_ENROLLED
		response.then().statusCode(409).body("code", equalTo("ALREADY_ENROLLED"));
		// and there is still exactly one assignment between them
		assertEquals(1, jdbc.sql("""
				select count(*) from teacher_students
				where teacher_id = :teacher and student_id = :student
				""")
				.param("teacher", teacherA)
				.param("student", student)
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S5] Email case is ignored")
	void emailCaseIsIgnored() {
		// given a student account with email "student@example.com"
		createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when a teacher registers a student with email "STUDENT@example.com"
		var response = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "STUDENT@example.com", "timeZone", "UTC"));

		// then the response is 409 with code STUDENT_EXISTS
		response.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is not in the teacher's list
		teacher.get(STUDENTS).then().statusCode(200).body("$", empty());
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S6] Invalid input is rejected")
	void invalidInputIsRejected() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with a blank name, an invalid email and an unknown time zone
		var blankName = teacher.post(STUDENTS,
				Map.of("name", " ", "email", "student@example.com", "timeZone", "UTC"));
		var invalidEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "not-an-email", "timeZone", "UTC"));
		var unknownTimeZone = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "Mars/Base"));

		// then each response is 400 with code VALIDATION_FAILED
		blankName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		invalidEmail.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		unknownTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S7] Email with invisible characters is rejected")
	void emailWithInvisibleCharactersIsRejected() {
		// given a logged-in teacher and a student account with email "nina@example.com"
		createTeacher("teacher@example.com", "teacher-pass-1");
		createUser("STUDENT", "Nina", "nina@example.com", null, true);
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with email "nina\u00A0@example.com", then with email "\u2003@example.com"
		var noBreakSpace = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina\u00A0@example.com", "timeZone", "UTC"));
		var emSpace = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "\u2003@example.com", "timeZone", "UTC"));

		// then each response is 400 with code VALIDATION_FAILED
		noBreakSpace.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		emSpace.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	/** A stub answer has no link; "no-link" keeps the setup from failing before THEN. */
	private static String tokenFromResponse(Response response) {
		String link = response.getContentType() != null && response.getContentType().contains("json")
				? response.path("setPasswordLink") : null;
		return link == null ? "no-link" : tokenFrom(link);
	}
}
