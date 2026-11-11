package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
				.param("id", teacherId).query(String.class).optional().orElse(null));
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
		response.then()
				.statusCode(201)
				.body("link", startsWith(APP_BASE_URL + "/set-password#token="));
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

	@Test
	@DisplayName("[accounts/R8-S3] A session from an older session version is rejected")
	void sessionFromAnOlderSessionVersionIsRejected() {
		// given a logged-in teacher whose session still exists
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the account's session version is raised in the database without the session being touched
		jdbc.sql("update users set session_version = session_version + 1 where email = 'teacher@example.com'")
				.update();

		// then the teacher's next request gets 401
		teacher.get("/api/me").then().statusCode(401);
	}

	private long countTokens(UUID userId, boolean unusedOnly) {
		return jdbc.sql("select count(*) from one_time_tokens where user_id = :id"
				+ (unusedOnly ? " and used_at is null" : ""))
				.param("id", userId)
				.query(Long.class)
				.single();
	}

	@Test
	@DisplayName("[accounts/R8-S4] Reset link for a deactivated account is rejected")
	void resetLinkForADeactivatedAccountIsRejected() {
		// given a logged-in admin and a deactivated teacher with a password and an unused link
		TestClient admin = loggedInAdmin();
		UUID teacherId = createUser("TEACHER", "Teacher", "teacher@example.com", "teacher-pass-1", false);
		createToken(teacherId, "RESET_PASSWORD", false);

		// when the admin requests a reset link for the teacher
		var response = admin.post("/api/admin/users/" + teacherId + "/reset-link");

		// then the response is 409 with code ACCOUNT_DEACTIVATED
		response.then().statusCode(409).body("code", equalTo("ACCOUNT_DEACTIVATED"));
		// and the teacher's existing link is still unused, and no other link is stored for the teacher
		assertEquals(1, countTokens(teacherId, true));
		assertEquals(1, countTokens(teacherId, false));
	}

	@Test
	@DisplayName("[accounts/R8-S5] Reset link for the admin's own account is rejected")
	void resetLinkForTheAdminsOwnAccountIsRejected() {
		// given a logged-in admin with an unused reset link for their own account, and a second admin account
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		TestClient admin = loggedInAs("admin@example.com");
		createToken(adminId, "RESET_PASSWORD", false);
		UUID otherAdminId = createAdmin("admin2@example.com", "admin-password-2");

		// when the admin requests a reset link for their own account and then for the second admin's account
		var own = admin.post("/api/admin/users/" + adminId + "/reset-link");
		var other = admin.post("/api/admin/users/" + otherAdminId + "/reset-link");

		// then the first response is 409 with code OWN_ACCOUNT
		own.then().statusCode(409).body("code", equalTo("OWN_ACCOUNT"));
		// and the admin's existing link is still unused, and no other link is stored for the admin
		assertEquals(1, countTokens(adminId, true));
		assertEquals(1, countTokens(adminId, false));
		// and the second response is 201 with a link
		other.then().statusCode(201).body("link", startsWith(APP_BASE_URL + "/set-password#token="));
	}
}
