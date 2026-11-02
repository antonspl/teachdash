package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R5 create teachers, R6 deactivate and reactivate. */
class TeacherManagementTest extends ApiTest {

	@Test
	@DisplayName("[accounts/R5-S1] Admin creates a teacher")
	void adminCreatesATeacher() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin creates a teacher with a new email
		var response = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "Europe/Moscow"));

		// then the response is 201 with the teacher's id and a set-password link
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("setPasswordLink", startsWith(APP_BASE_URL + "/set-password#token="));
		// and the id is the id of the stored account
		assertEquals(jdbc.sql("select id from users where lower(email) = 'anna@example.com'")
				.query(UUID.class).single().toString(), response.path("id"));
		// and a login before the password is set gets 401 with code INVALID_CREDENTIALS
		newClient().login("anna@example.com", "any-password-1").then()
				.statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("[accounts/R5-S3] Link from teacher creation sets the password")
	void linkFromTeacherCreationSetsThePassword() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin creates a teacher and the returned link is used to set the password "new-password-1"
		var created = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "UTC"));
		String link = created.getContentType() != null && created.getContentType().contains("json")
				? created.path("setPasswordLink") : null;
		String token = link == null ? "no-link" : tokenFrom(link);
		var response = newClient().post(
				"/api/password-links",
				Map.of("token", token, "password", "new-password-1"));

		// then creation is 201, setting the password is 200, and the teacher logs in with "new-password-1"
		created.then().statusCode(201);
		response.then().statusCode(200);
		newClient().login("anna@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R5-S2] Duplicate email is rejected")
	void duplicateEmailIsRejected() {
		// given an account with email "anna@example.com"
		createStudent("anna@example.com", "anna-pass-123");
		TestClient admin = loggedInAdmin();
		long before = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the admin creates a teacher with email "ANNA@example.com"
		var response = admin.post("/api/admin/teachers",
				Map.of("name", "Anna 2", "email", "ANNA@example.com", "timeZone", "UTC"));

		// then the response is 409 and no account is created
		response.then().statusCode(409);
		assertEquals(before, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[accounts/R6-S1] Deactivation ends sessions and blocks login")
	void deactivationEndsSessionsAndBlocksLogin() {
		// given a logged-in teacher
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient admin = loggedInAdmin();

		// when the admin deactivates the teacher
		var response = admin.post("/api/admin/teachers/" + teacherId + "/deactivate");

		// then the call succeeds, the teacher's existing session gets 401 and a new login gets 401 with code ACCOUNT_DEACTIVATED
		response.then().statusCode(lessThan(300));
		teacher.get("/api/me").then().statusCode(401);
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(401)
				.body("code", equalTo("ACCOUNT_DEACTIVATED"));
	}

	@Test
	@DisplayName("[accounts/R6-S3] Only teachers can be deactivated")
	void onlyTeachersCanBeDeactivated() {
		// given a logged-in admin and a student account
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		UUID studentId = createStudent("student@example.com", "student-pass-1");
		TestClient admin = loggedInAs("admin@example.com");

		// when the admin tries to deactivate their own account and the student's account
		var ownResponse = admin.post("/api/admin/teachers/" + adminId + "/deactivate");
		var studentResponse = admin.post("/api/admin/teachers/" + studentId + "/deactivate");

		// then both responses are 404, both accounts stay active, and the admin's session still works
		ownResponse.then().statusCode(404);
		studentResponse.then().statusCode(404);
		assertEquals(2, jdbc.sql("select count(*) from users where active")
				.query(Long.class)
				.single());
		admin.get("/api/me").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R6-S2] Reactivated teacher can log in")
	void reactivatedTeacherCanLogIn() {
		// given a deactivated teacher
		UUID teacherId = createUser(
				"TEACHER",
				"Teacher",
				"teacher@example.com",
				"teacher-pass-1",
				false);
		TestClient admin = loggedInAdmin();

		// when the admin reactivates the teacher
		var response = admin.post("/api/admin/teachers/" + teacherId + "/reactivate");

		// then the call succeeds and the teacher can log in with their password
		response.then().statusCode(lessThan(300));
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R6-S4] Every request re-checks that the account is active")
	void everyRequestRechecksThatTheAccountIsActive() {
		// given a logged-in teacher whose session still exists
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the account becomes inactive in the database without the session being touched
		jdbc.sql("update users set active = false where email = 'teacher@example.com'").update();

		// then the teacher's next request gets 401
		teacher.get("/api/me").then().statusCode(401);
	}

	@Test
	@DisplayName("[accounts/R6-S5] Teacher list shows who is active")
	void teacherListShowsWhoIsActive() {
		// given a logged-in admin, an active teacher and a deactivated teacher
		TestClient admin = loggedInAdmin();
		createUser("TEACHER", "Active", "active@example.com", "teacher-pass-1", true);
		createUser("TEACHER", "Inactive", "inactive@example.com", "teacher-pass-1", false);

		// when the admin lists teachers
		var response = admin.get("/api/admin/teachers");

		// then each list item has an active field with the right value
		response.then()
				.statusCode(200)
				.body("find { it.email == 'active@example.com' }.active", equalTo(true))
				.body("find { it.email == 'inactive@example.com' }.active", equalTo(false));
	}

	@Test
	@DisplayName("[accounts/R6-S6] Reactivation does not revive an old session")
	void reactivationDoesNotReviveAnOldSession() {
		// given a logged-in teacher
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient admin = loggedInAdmin();

		// when the admin deactivates and then reactivates the teacher
		var deactivated = admin.post("/api/admin/teachers/" + teacherId + "/deactivate");
		var reactivated = admin.post("/api/admin/teachers/" + teacherId + "/reactivate");

		// then the teacher's old session gets 401 and a new login returns 200
		deactivated.then().statusCode(lessThan(300));
		reactivated.then().statusCode(lessThan(300));
		teacher.get("/api/me").then().statusCode(401);
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R6-S7] Reactivating a teacher with no password returns a new link")
	void reactivatingATeacherWithNoPasswordReturnsANewLink() {
		// given a logged-in admin and a deactivated teacher who never set a password
		TestClient admin = loggedInAdmin();
		var created = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "UTC"));
		UUID teacherId = UUID.fromString(created.path("id"));
		admin.post("/api/admin/teachers/" + teacherId + "/deactivate");

		// when the admin reactivates the teacher, and the returned link is used to set a password
		var reactivated = admin.post("/api/admin/teachers/" + teacherId + "/reactivate");
		String link = reactivated.getContentType() != null && reactivated.getContentType().contains("json")
				? reactivated.path("setPasswordLink") : null;
		String token = link == null ? "no-link" : tokenFrom(link);
		var response = newClient().post(
				"/api/password-links",
				Map.of("token", token, "password", "new-password-1"));

		// then reactivation returns 200 with a set-password link that starts with the base URL, and the link sets the password
		reactivated.then()
				.statusCode(200)
				.body("setPasswordLink", startsWith(APP_BASE_URL + "/set-password#token="));
		response.then().statusCode(200);
		newClient().login("anna@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R6-S8] Reactivating an active teacher keeps their link")
	void reactivatingAnActiveTeacherKeepsTheirLink() {
		// given a logged-in admin and an active teacher with an unused set-password link
		TestClient admin = loggedInAdmin();
		var created = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", "UTC"));
		String link = created.getContentType() != null && created.getContentType().contains("json")
				? created.path("setPasswordLink") : null;
		String token = link == null ? "no-link" : tokenFrom(link);
		UUID teacherId = UUID.fromString(created.path("id"));

		// when the admin reactivates the teacher
		var reactivated = admin.post("/api/admin/teachers/" + teacherId + "/reactivate");
		var response = newClient().post(
				"/api/password-links",
				Map.of("token", token, "password", "new-password-1"));

		// then the response is 204 and the existing link still sets the password
		reactivated.then().statusCode(204);
		response.then().statusCode(200);
	}
}
