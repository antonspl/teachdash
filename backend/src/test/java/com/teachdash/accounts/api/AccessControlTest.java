package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;

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
		var change = Map.of("name", "Changed Name", "timeZone", "Asia/Dubai");

		// when the teacher changes their profile without the CSRF token, and then with it
		var withoutToken = teacher.snapshot().withoutCsrfHeader().patch("/api/me", change);
		var afterFirst = jdbc.sql("select name, time_zone from users where email = 'teacher@example.com'")
				.query().singleRow();
		var withToken = teacher.patch("/api/me", change);

		// then the first response is 403 and leaves the profile unchanged, and the second is 200
		withoutToken.then().statusCode(403);
		assertEquals("Teacher", afterFirst.get("name"));
		assertEquals("UTC", afterFirst.get("time_zone"));
		withToken.then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R4-S5] Non-admins cannot call admin endpoints that change data")
	void nonAdminsCannotChangeData() {
		// given a logged-in teacher, a logged-in student, the admin account and another teacher
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		UUID otherId = createTeacher("other@example.com", "other-pass-123");
		createTeacher("teacher@example.com", "teacher-pass-1");
		createStudent("student@example.com", "student-pass-1");
		var teacher = loggedInAs("teacher@example.com");
		var student = loggedInAs("student@example.com");
		var usersBefore = jdbc.sql("select * from users order by email").query().listOfRows();

		// when each of them calls every admin endpoint that changes data
		List<Response> responses = new ArrayList<>();
		for (TestClient client : List.of(teacher, student)) {
			responses.add(client.post("/api/admin/teachers",
					Map.of("name", "New", "email", "new@example.com", "timeZone", "UTC")));
			responses.add(client.post("/api/admin/teachers/" + otherId + "/deactivate"));
			responses.add(client.post("/api/admin/teachers/" + otherId + "/reactivate"));
			responses.add(client.post("/api/admin/users/" + adminId + "/reset-link"));
		}

		// then every response is 403, no account is created or changed, and no link is issued
		responses.forEach(r -> r.then().statusCode(403));
		assertEquals(usersBefore, jdbc.sql("select * from users order by email").query().listOfRows());
		assertEquals(0, jdbc.sql("select count(*) from one_time_tokens").query(Long.class).single());
	}
}
