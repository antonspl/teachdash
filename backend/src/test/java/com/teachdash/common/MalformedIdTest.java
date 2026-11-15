package com.teachdash.common;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R3. */
class MalformedIdTest extends ApiTest {

	private long count(String table) {
		return jdbc.sql("select count(*) from " + table).query(Long.class).single();
	}

	@Test
	@DisplayName("[api-errors/R3-S1] Malformed id on admin endpoints")
	void malformedIdOnAdminEndpoints() {
		// given a logged-in admin and a logged-in teacher
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long activeBefore = count("users where active");

		// when the admin deactivates, reactivates and issues a reset link for "not-a-uuid", and the teacher deactivates it
		var deactivate = admin.post("/api/admin/teachers/not-a-uuid/deactivate");
		var reactivate = admin.post("/api/admin/teachers/not-a-uuid/reactivate");
		var resetLink = admin.post("/api/admin/users/not-a-uuid/reset-link");
		var byTeacher = teacher.post("/api/admin/teachers/not-a-uuid/deactivate");

		// then the admin's responses are 404 NOT_FOUND with no errors, and the teacher's is 403
		for (var response : new io.restassured.response.Response[] { deactivate, reactivate, resetLink }) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")));
		}
		byTeacher.then().statusCode(403);
		// and nothing changed
		assertEquals(activeBefore, count("users where active"));
		assertEquals(0, count("one_time_tokens"));
	}

	@Test
	@DisplayName("[api-errors/R3-S2] Malformed id on enrolment endpoints stays not found")
	void malformedIdOnEnrolmentEndpointsStaysNotFound() {
		// given a logged-in teacher and a logged-in admin
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient admin = loggedInAdmin();

		// when the teacher requests the details of "not-a-uuid", and the admin lists the students of "not-a-uuid"
		var details = teacher.get("/api/teacher/students/not-a-uuid");
		var students = admin.get("/api/admin/teachers/not-a-uuid/students");

		// then each response is 404 NOT_FOUND with no errors
		details.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
		students.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
	}
}
