package com.teachdash.accounts.api;

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

/** R7, R8. Tokens not under test are inserted directly (createToken). */
class PasswordLinkTest extends ApiTest {

	private Response useLink(TestClient client, String token, String password) {
		return client.post("/api/password-links/" + token, Map.of("password", password));
	}

	@Test
	@DisplayName("[accounts/R7-S1] Valid link sets the password")
	void validLinkSetsThePassword() {
		// given an unused set-password link issued 1 day ago
		UUID annaId = createUser("TEACHER", "Anna", "anna@example.com", null, true);
		String token = createToken(annaId, "SET_PASSWORD", false);
		testClock().advance(Duration.ofDays(1));
		TestClient holder = newClient();

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
		// given a set-password link that was already used, and the password it set
		UUID annaId = createTeacher("anna@example.com", "first-password-1");
		String token = createToken(annaId, "SET_PASSWORD", true);

		// when someone uses it again
		var response = useLink(newClient(), token, "second-password-2");

		// then the response is 410 and the password does not change
		response.then().statusCode(410);
		newClient().login("anna@example.com", "first-password-1").then().statusCode(200);
		newClient().login("anna@example.com", "second-password-2").then().statusCode(401);
	}

	@Test
	@DisplayName("[accounts/R7-S3] Expired link is rejected")
	void expiredLinkIsRejected() {
		// given an unused link issued 8 days ago
		UUID annaId = createUser("TEACHER", "Anna", "anna@example.com", null, true);
		String token = createToken(annaId, "SET_PASSWORD", false);
		testClock().advance(Duration.ofDays(8));

		// when someone uses it
		var response = useLink(newClient(), token, "new-password-1");

		// then the response is 410
		response.then().statusCode(410);
	}

	@Test
	@DisplayName("[accounts/R7-S4] Newer link replaces older one")
	void newerLinkReplacesOlderOne() {
		// given an unused link (inserted directly; the newer one is issued via the API as replacement is under test)
		UUID teacherId = createTeacher("anna@example.com", "old-password-1");
		String older = createToken(teacherId, "RESET_PASSWORD", false);
		TestClient admin = loggedInAdmin();
		testClock().advance(Duration.ofSeconds(1));

		// when the admin issues a newer link for the same account
		var issued = admin.post("/api/admin/users/" + teacherId + "/reset-link");

		// then the older link gets 410 and the newer link still works
		issued.then().statusCode(201);
		String newer = tokenFrom(issued.path("link"));
		useLink(newClient(), older, "new-password-1").then().statusCode(410);
		useLink(newClient(), newer, "new-password-2").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R7-S5] Short password is rejected and link stays valid")
	void shortPasswordIsRejectedAndLinkStaysValid() {
		// given an unused link
		UUID annaId = createUser("TEACHER", "Anna", "anna@example.com", null, true);
		String token = createToken(annaId, "SET_PASSWORD", false);

		// when its holder sets a 7-character password
		var response = useLink(newClient(), token, "1234567");

		// then the response is 400 and the link can still be used with a valid password
		response.then().statusCode(400);
		useLink(newClient(), token, "12345678").then().statusCode(200);
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
		TestClient student = loggedInAs("student@example.com");
		String token = createToken(studentId, "RESET_PASSWORD", false);

		// when the link is used to set a new password
		var response = useLink(newClient(), token, "new-password-1");

		// then the link works, the old password gets 401 on login and the student's earlier session gets 401
		response.then().statusCode(200);
		newClient().login("student@example.com", "old-password-1").then().statusCode(401);
		student.get("/api/me").then().statusCode(401);
	}
}
