package com.teachdash.enrolment.api.admin;

import static org.hamcrest.Matchers.equalTo;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R6. */
class AdminStudentLookupTest extends ApiTest {

	private static final String LOOKUP = "/api/admin/students/lookup";

	private static Map<String, String> email(String value) {
		return Map.of("email", value);
	}

	@Test
	@DisplayName("[student-enrolment/R6-S1] Admin finds a student")
	void adminFindsAStudent() {
		// given a logged-in admin and a student account with email "student@example.com"
		TestClient admin = loggedInAdmin();
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);

		// when the admin looks up "student@example.com"
		var response = admin.post(LOOKUP, email("student@example.com"));

		// then the response is 200 with the student's id, name, email, time zone and active flag
		response.then().statusCode(200)
				.body("id", equalTo(student.toString()))
				.body("name", equalTo("Student"))
				.body("email", equalTo("student@example.com"))
				.body("timeZone", equalTo("UTC"))
				.body("active", equalTo(true));
	}

	@Test
	@DisplayName("[student-enrolment/R6-S2] Removed student without a password can be recovered")
	void removedStudentWithoutAPasswordCanBeRecovered() {
		// given a logged-in admin and a student without a password, with no teachers and an expired set-password link
		TestClient admin = loggedInAdmin();
		UUID student = createUser("STUDENT", "Student", "student@example.com", null, true);
		createToken(student, "SET_PASSWORD", false);
		jdbc.sql("update one_time_tokens set expires_at = :past")
				.param("past", Timestamp.from(Instant.now(clock).minus(Duration.ofDays(1))))
				.update();

		// when the admin looks up the student's email, issues a reset link for the returned id, and the link is used to set the password "new-password-1"
		var lookup = admin.post(LOOKUP, email("student@example.com"));
		lookup.then().statusCode(200);
		var issued = admin.post("/api/admin/users/" + lookup.path("id") + "/reset-link");
		String token = tokenFrom(issued.path("link"));
		var used = newClient().post("/api/password-links", Map.of("token", token, "password", "new-password-1"));

		// then the lookup response is 200, and the student logs in with "new-password-1"
		lookup.then().statusCode(200);
		issued.then().statusCode(201);
		used.then().statusCode(200);
		newClient().login("student@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R6-S3] Email case and surrounding whitespace are ignored")
	void emailCaseAndSurroundingWhitespaceAreIgnored() {
		// given a logged-in admin and a student account with email "student@example.com"
		TestClient admin = loggedInAdmin();
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);

		// when the admin looks up " STUDENT@example.com "
		var response = admin.post(LOOKUP, email(" STUDENT@example.com "));

		// then the response is 200 with that student's id
		response.then().statusCode(200).body("id", equalTo(student.toString()));
	}

	@Test
	@DisplayName("[student-enrolment/R6-S4] Email without a student account is not found")
	void emailWithoutAStudentAccountIsNotFound() {
		// given a logged-in admin and a teacher account
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1");

		// when the admin looks up an email with no account, the teacher's email, and the admin's own email
		var unknown = admin.post(LOOKUP, email("nobody@example.com"));
		var teacher = admin.post(LOOKUP, email("teacher@example.com"));
		var self = admin.post(LOOKUP, email("admin@example.com"));

		// then each response is 404 with code NOT_FOUND
		unknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		teacher.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		self.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
	}

	@Test
	@DisplayName("[student-enrolment/R6-S5] Invalid email is rejected")
	void invalidEmailIsRejected() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin looks up a blank email, then a malformed email, then sends no email
		var blank = admin.post(LOOKUP, email("   "));
		var malformed = admin.post(LOOKUP, email("not-an-email"));
		var missing = admin.post(LOOKUP, Map.of());

		// then each response is 400 with code VALIDATION_FAILED
		blank.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		malformed.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		missing.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("[student-enrolment/R6-S6] Teachers and students cannot look up")
	void teachersAndStudentsCannotLookUp() {
		// given a student account, a logged-in teacher and a logged-in student
		createTeacher("teacher@example.com", "teacher-pass-1");
		createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient student = loggedInAs("student@example.com");

		// when the teacher and the student each look up that student's email
		var byTeacher = teacher.post(LOOKUP, email("student@example.com"));
		var byStudent = student.post(LOOKUP, email("student@example.com"));

		// then both responses are 403
		byTeacher.then().statusCode(403);
		byStudent.then().statusCode(403);
	}

	@Test
	@DisplayName("[student-enrolment/R6-S7] Lookup requires a session")
	void lookupRequiresASession() {
		// given a client with a valid CSRF token and no session
		TestClient anonymous = newClient();

		// when the client looks up an email
		var response = anonymous.post(LOOKUP, email("student@example.com"));

		// then the response is 401 with code UNAUTHENTICATED
		response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}

	@Test
	@DisplayName("[student-enrolment/R6-S8] Request without a CSRF token is rejected")
	void requestWithoutCsrfTokenIsRejected() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin looks up an email without a CSRF token
		var response = admin.snapshot().withoutCsrfHeader().post(LOOKUP, email("student@example.com"));

		// then the response is 403 with code CSRF_INVALID
		response.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
	}
}
