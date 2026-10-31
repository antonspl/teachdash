package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
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
				.body("setPasswordLink", containsString("/set-password?token="));
		// and a login before the password is set gets 401 with code PASSWORD_NOT_SET
		newClient().login("anna@example.com", "any-password-1").then()
				.statusCode(401).body("code", equalTo("PASSWORD_NOT_SET"));
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

		// then the call succeeds, the teacher's existing session gets 401 and a new login gets 401
		response.then().statusCode(lessThan(300));
		teacher.get("/api/me").then().statusCode(401);
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(401);
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
