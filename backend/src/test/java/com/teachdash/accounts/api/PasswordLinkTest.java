package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.startsWith;
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
		return client.post("/api/password-links", Map.of("token", token, "password", password));
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
		// given an unused link issued 8 days ago for an account with a password
		UUID annaId = createTeacher("anna@example.com", "old-password-1");
		String token = createToken(annaId, "SET_PASSWORD", false);
		testClock().advance(Duration.ofDays(8));

		// when someone uses it to set a new password
		var response = useLink(newClient(), token, "new-password-1");

		// then the response is 410 and the old password still works
		response.then().statusCode(410);
		newClient().login("anna@example.com", "old-password-1").then().statusCode(200);
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
	@DisplayName("[accounts/R7-S6] Link for a deactivated account is rejected")
	void linkForADeactivatedAccountIsRejected() {
		// given an admin created a teacher and got a set-password link
		TestClient admin = loggedInAdmin();
		var created = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "UTC"));
		String token = tokenFrom(created.path("setPasswordLink"));
		UUID teacherId = UUID.fromString(created.path("id"));
		TestClient holder = newClient();

		// when the admin deactivates the teacher and the link is then used to set a password
		var deactivated = admin.post("/api/admin/teachers/" + teacherId + "/deactivate");
		var response = useLink(holder, token, "new-password-1");

		// then the response is 401 with code ACCOUNT_DEACTIVATED, the teacher still has no password, and no session is started
		created.then().statusCode(201);
		deactivated.then().statusCode(org.hamcrest.Matchers.lessThan(300));
		response.then().statusCode(401).body("code", equalTo("ACCOUNT_DEACTIVATED"));
		org.junit.jupiter.api.Assertions.assertNull(jdbc.sql("select password_hash from users where id = :id")
				.param("id", teacherId).query(String.class).single());
		org.junit.jupiter.api.Assertions.assertNull(holder.cookie("SESSION"));
		org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.sql(
				"select count(*) from spring_session where principal_name = :id")
				.param("id", teacherId.toString()).query(Long.class).single());
	}

	@Test
	@DisplayName("[accounts/R8-S1] Admin issues a reset link")
	void adminIssuesAResetLink() {
		// given a logged-in admin and a teacher account
		TestClient admin = loggedInAdmin();
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");

		// when the admin requests a reset link for the teacher
		var response = admin.post("/api/admin/users/" + teacherId + "/reset-link");

		// then the response is 201 with a link that starts with the base URL
		response.then().statusCode(201).body("link", startsWith(APP_BASE_URL + "/set-password#token="));
	}

	@Test
	@DisplayName("[accounts/R8-S2] Reset replaces the old password and ends sessions")
	void resetReplacesTheOldPasswordAndEndsSessions() {
		// given a logged-in student and a reset link issued for them
		UUID studentId = createStudent("student@example.com", "old-password-1");
		TestClient student = loggedInAs("student@example.com");
		String token = createToken(studentId, "RESET_PASSWORD", false);
		TestClient holder = newClient();

		// when the link is used to set a new password
		var response = useLink(holder, token, "new-password-1");

		// then the old password gets 401, the earlier session gets 401, and the link's session stays valid
		response.then().statusCode(200);
		holder.get("/api/me").then().statusCode(200);
		newClient().login("student@example.com", "old-password-1").then().statusCode(401);
		student.get("/api/me").then().statusCode(401);
	}
}
