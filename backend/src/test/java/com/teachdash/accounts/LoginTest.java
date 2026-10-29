package com.teachdash.accounts;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R2 login, R3 logout. */
class LoginTest extends ApiTest {

	@Test
	@DisplayName("[accounts/R2-S1] Valid credentials start a session")
	void validCredentialsStartASession() {
		// given an active teacher account
		UUID id = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient client = newClient();

		// when the teacher logs in with the correct email and password
		var login = client.login("teacher@example.com", "teacher-pass-1");

		// then the response is 200 and the current-user endpoint returns id, name, email, role, time zone
		login.then().statusCode(200);
		client.get("/api/me").then().statusCode(200)
				.body("id", equalTo(id.toString()))
				.body("name", equalTo("Teacher"))
				.body("email", equalTo("teacher@example.com"))
				.body("role", equalTo("TEACHER"))
				.body("timeZone", equalTo("UTC"));
	}

	@Test
	@DisplayName("[accounts/R2-S2] Wrong password is rejected")
	void wrongPasswordIsRejected() {
		// given an active teacher account
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient client = newClient();

		// when someone logs in with the teacher's email and a wrong password
		var login = client.login("teacher@example.com", "not-the-password");

		// then the response is 401 with code WRONG_PASSWORD and no session is started
		login.then().statusCode(401).body("code", equalTo("WRONG_PASSWORD"));
		client.get("/api/me").then().statusCode(401);
	}

	@Test
	@DisplayName("[accounts/R2-S3] Unknown email is reported")
	void unknownEmailIsReported() {
		// given no account with the email "nobody@example.com"

		// when someone logs in with an email that has no account
		var login = newClient().login("nobody@example.com", "whatever-123");

		// then the response is 401 with code UNKNOWN_EMAIL
		login.then().statusCode(401).body("code", equalTo("UNKNOWN_EMAIL"));
	}

	@Test
	@DisplayName("[accounts/R2-S4] Email is case-insensitive")
	void emailIsCaseInsensitive() {
		// given an account with email "anna@example.com"
		createTeacher("anna@example.com", "anna-pass-123");

		// when the user logs in with "Anna@Example.COM" and the correct password
		var login = newClient().login("Anna@Example.COM", "anna-pass-123");

		// then the response is 200
		login.then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R2-S5] Deactivated account cannot log in")
	void deactivatedAccountCannotLogIn() {
		// given a deactivated teacher account
		createUser("TEACHER", "Teacher", "teacher@example.com", "teacher-pass-1", false);

		// when the teacher logs in with the correct email and password
		var login = newClient().login("teacher@example.com", "teacher-pass-1");

		// then the response is 401 with code ACCOUNT_DEACTIVATED
		login.then().statusCode(401).body("code", equalTo("ACCOUNT_DEACTIVATED"));
	}

	@Test
	@DisplayName("[accounts/R3-S1] Logout ends the session")
	void logoutEndsTheSession() {
		// given a logged-in student (and a copy of their session cookie)
		createStudent("student@example.com", "student-pass-1");
		TestClient client = loggedInAs("student@example.com", "student-pass-1");
		client.get("/api/me").then().statusCode(200).body("email", notNullValue());
		TestClient oldSession = client.snapshot();

		// when the student logs out
		client.logout().then().statusCode(org.hamcrest.Matchers.lessThan(300));

		// then the current-user endpoint returns 401 for that session
		oldSession.get("/api/me").then().statusCode(401);
		client.get("/api/me").then().statusCode(401);
	}
}
