package com.teachdash.accounts;

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
		var teacher = loggedInAs("teacher@example.com", "teacher-pass-1");

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
		var student = loggedInAs("student@example.com", "student-pass-1");

		// when the student calls the admin endpoint that lists teachers
		var response = student.get("/api/admin/teachers");

		// then the response is 403
		response.then().statusCode(403);
	}
}
