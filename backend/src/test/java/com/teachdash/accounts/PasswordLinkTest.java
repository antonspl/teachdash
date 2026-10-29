package com.teachdash.accounts;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;

/** R7 single-use links, R8 admin reset links. Links are obtained through the admin API. */
class PasswordLinkTest extends ApiTest {

	/** Admin creates a teacher without a password and returns the set-password token. */
	private String issueSetPasswordToken(TestClient admin, String email) {
		String link = admin.post("/api/admin/teachers", Map.of("name", "New Teacher", "email", email, "timeZone", "UTC"))
				.then().statusCode(201).extract().path("setPasswordLink");
		return tokenFrom(link);
	}

	private String issueResetToken(TestClient admin, UUID userId) {
		String link = admin.post("/api/admin/users/" + userId + "/reset-link")
				.then().statusCode(201).extract().path("link");
		return tokenFrom(link);
	}

	private Response useLink(TestClient client, String token, String password) {
		return client.post("/api/password-links/" + token, Map.of("password", password));
	}

	@Test
	@DisplayName("[accounts/R7-S1] Valid link sets the password")
	void validLinkSetsThePassword() {
		// given an unused set-password link issued 1 day ago
		TestClient admin = loggedInAdmin();
		String token = issueSetPasswordToken(admin, "anna@example.com");
		testClock().advance(Duration.ofDays(1));
		TestClient holder = newClient().primeCsrf();

		// when its holder sets the password "new-password-1"
		var response = useLink(holder, token, "new-password-1");

		// then the response is 200, a session is started, and the account logs in with "new-password-1"
		response.then().statusCode(200);
		holder.get("/api/me").then().statusCode(200).body("email", equalTo("anna@example.com"));
		newClient().login("anna@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R7-S2] Used link is rejected")
	void usedLinkIsRejected() {
		// given a set-password link that was already used
		TestClient admin = loggedInAdmin();
		String token = issueSetPasswordToken(admin, "anna@example.com");
		useLink(newClient().primeCsrf(), token, "first-password-1").then().statusCode(200);

		// when someone uses it again
		var response = useLink(newClient().primeCsrf(), token, "second-password-2");

		// then the response is 410 and the password does not change
		response.then().statusCode(410);
		newClient().login("anna@example.com", "first-password-1").then().statusCode(200);
		newClient().login("anna@example.com", "second-password-2").then().statusCode(401);
	}

	@Test
	@DisplayName("[accounts/R7-S3] Expired link is rejected")
	void expiredLinkIsRejected() {
		// given an unused link issued 8 days ago
		TestClient admin = loggedInAdmin();
		String token = issueSetPasswordToken(admin, "anna@example.com");
		testClock().advance(Duration.ofDays(8));

		// when someone uses it
		var response = useLink(newClient().primeCsrf(), token, "new-password-1");

		// then the response is 410
		response.then().statusCode(410);
	}

	@Test
	@DisplayName("[accounts/R7-S4] Newer link replaces older one")
	void newerLinkReplacesOlderOne() {
		// given two links issued for the same account, one after the other
		UUID teacherId = createTeacher("anna@example.com", "old-password-1");
		TestClient admin = loggedInAdmin();
		String older = issueResetToken(admin, teacherId);
		testClock().advance(Duration.ofSeconds(1));
		String newer = issueResetToken(admin, teacherId);

		// when someone uses the older link
		var response = useLink(newClient().primeCsrf(), older, "new-password-1");

		// then the response is 410 and the newer link still works
		response.then().statusCode(410);
		useLink(newClient().primeCsrf(), newer, "new-password-2").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R7-S5] Short password is rejected and link stays valid")
	void shortPasswordIsRejectedAndLinkStaysValid() {
		// given an unused link
		TestClient admin = loggedInAdmin();
		String token = issueSetPasswordToken(admin, "anna@example.com");

		// when its holder sets a 7-character password
		var response = useLink(newClient().primeCsrf(), token, "1234567");

		// then the response is 400 and the link can still be used with a valid password
		response.then().statusCode(400);
		useLink(newClient().primeCsrf(), token, "12345678").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R8-S1] Admin issues a reset link")
	void adminIssuesAResetLink() {
		// given a logged-in admin and a teacher account
		TestClient admin = loggedInAdmin();
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");

		// when the admin requests a reset link for the teacher
		var response = admin.post("/api/admin/users/" + teacherId + "/reset-link");

		// then the response is 201 with a link
		response.then().statusCode(201).body("link", containsString("token="));
	}

	@Test
	@DisplayName("[accounts/R8-S2] Reset replaces the old password and ends sessions")
	void resetReplacesTheOldPasswordAndEndsSessions() {
		// given a logged-in student and a reset link issued for them
		UUID studentId = createStudent("student@example.com", "old-password-1");
		TestClient student = loggedInAs("student@example.com", "old-password-1");
		student.get("/api/me").then().statusCode(200);
		TestClient admin = loggedInAdmin();
		String token = issueResetToken(admin, studentId);

		// when the link is used to set a new password
		useLink(newClient().primeCsrf(), token, "new-password-1").then().statusCode(200);

		// then the old password gets 401 on login and the student's earlier session gets 401
		newClient().login("student@example.com", "old-password-1").then().statusCode(401);
		student.get("/api/me").then().statusCode(401);
	}
}
