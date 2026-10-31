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
		// and a login before the password is set gets 401 with code PASSWORD_NOT_SET
		newClient().login("anna@example.com", "any-password-1").then()
				.statusCode(401).body("code", equalTo("PASSWORD_NOT_SET"));
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
		var response = newClient().post("/api/password-links", Map.of("token", token, "password", "new-password-1"));

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
		assertEquals(2, jdbc.sql("select count(*) from users where active").query(Long.class).single());
		admin.get("/api/me").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R6-S2] Reactivated teacher can log in")
	void reactivatedTeacherCanLogIn() {
		// given a deactivated teacher
		UUID teacherId = createUser("TEACHER", "Teacher", "teacher@example.com", "teacher-pass-1", false);
		TestClient admin = loggedInAdmin();

		// when the admin reactivates the teacher
		var response = admin.post("/api/admin/teachers/" + teacherId + "/reactivate");

		// then the call succeeds and the teacher can log in with their password
		response.then().statusCode(lessThan(300));
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}
}
