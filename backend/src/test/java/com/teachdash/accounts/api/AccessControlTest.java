package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;

/** R4 role-based access. */
class AccessControlTest extends ApiTest {

	@Test
	@DisplayName("[accounts/R4-S1] No session is rejected")
	void noSessionIsRejected() {
		// given a request without a session

		// when it calls the current-user endpoint
		var response = newClient().get("/api/me");

		// then the response is 401
		response.then().statusCode(401);
	}

	@Test
	@DisplayName("[accounts/R4-S2] Teacher cannot call admin endpoints")
	void teacherCannotCallAdminEndpoints() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		var teacher = loggedInAs("teacher@example.com");

		// when the teacher calls the admin endpoint that lists teachers
		var response = teacher.get("/api/admin/teachers");

		// then the response is 403
		response.then().statusCode(403);
	}

	@Test
	@DisplayName("[accounts/R4-S3] Student cannot call admin endpoints")
	void studentCannotCallAdminEndpoints() {
		// given a logged-in student
		createStudent("student@example.com", "student-pass-1");
		var student = loggedInAs("student@example.com");

		// when the student calls the admin endpoint that lists teachers
		var response = student.get("/api/admin/teachers");

		// then the response is 403
		response.then().statusCode(403);
	}

	@Test
	@DisplayName("[accounts/R4-S4] Request without CSRF token is rejected")
	void requestWithoutCsrfTokenIsRejected() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		var teacher = loggedInAs("teacher@example.com");

		// when the teacher changes their profile without sending the CSRF token
		var response = teacher.withoutCsrfHeader()
				.patch("/api/me", Map.of("name", "Changed Name", "timeZone", "Asia/Dubai"));

		// then the response is 403 and the profile does not change
		response.then().statusCode(403);
		var row = jdbc.sql("select name, time_zone from users where email = 'teacher@example.com'")
				.query().singleRow();
		assertEquals("Teacher", row.get("name"));
		assertEquals("UTC", row.get("time_zone"));
	}
}
