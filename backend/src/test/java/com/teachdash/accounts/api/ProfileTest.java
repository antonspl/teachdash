package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R9 profile. */
class ProfileTest extends ApiTest {

	@Test
	@DisplayName("[accounts/R9-S1] Update name and time zone")
	void updateNameAndTimeZone() {
		// given a logged-in student
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");

		// when the student sets name "Ivan P." and time zone "Asia/Dubai"
		var response = student.patch("/api/me", Map.of("name", "Ivan P.", "timeZone", "Asia/Dubai"));

		// then the response is 200 and the current-user endpoint returns the new name and time zone
		response.then().statusCode(200);
		student.get("/api/me").then().statusCode(200)
				.body("name", equalTo("Ivan P."))
				.body("timeZone", equalTo("Asia/Dubai"));
	}

	@Test
	@DisplayName("[accounts/R9-S2] Invalid time zone is rejected")
	void invalidTimeZoneIsRejected() {
		// given a logged-in student
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");

		// when the student sets time zone "Mars/Olympus"
		var response = student.patch(
				"/api/me",
				Map.of("name", "Student", "timeZone", "Mars/Olympus"));

		// then the response is 400 and the time zone does not change
		response.then().statusCode(400);
		student.get("/api/me").then().statusCode(200).body("timeZone", equalTo("UTC"));
	}

	@Test
	@DisplayName("[accounts/R9-S3] Password change needs the current password")
	void passwordChangeNeedsTheCurrentPassword() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher changes the password giving a wrong current password
		var response = teacher.post("/api/me/password",
				Map.of("currentPassword", "wrong-current-1", "newPassword", "brand-new-pass-1"));

		// then the response is 400 and the old password still works
		response.then().statusCode(400);
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S4] Password change ends other sessions")
	void passwordChangeEndsOtherSessions() {
		// given a teacher logged in on two devices
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient first = loggedInAs("teacher@example.com");
		TestClient second = loggedInAs("teacher@example.com");

		// when the teacher changes the password on the first device, giving the correct current password
		var response = first.post("/api/me/password",
				Map.of("currentPassword", "teacher-pass-1", "newPassword", "brand-new-pass-1"));

		// then the response is 204, the second device gets 401, the first still works, and the new password logs in
		response.then().statusCode(204);
		second.get("/api/me").then().statusCode(401);
		first.get("/api/me").then().statusCode(200);
		newClient().login("teacher@example.com", "brand-new-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S5] Password change invalidates unused links")
	void passwordChangeInvalidatesUnusedLinks() {
		// given a logged-in teacher with an unused reset link
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		String token = createToken(teacherId, "RESET_PASSWORD", false);

		// when the teacher changes the password, and the link is then used
		var change = teacher.post("/api/me/password",
				Map.of("currentPassword", "teacher-pass-1", "newPassword", "brand-new-pass-1"));
		var useLink = newClient().post("/api/password-links",
				Map.of("token", token, "password", "attacker-pass-1"));

		// then using the link returns 410 and the new password still works
		change.then().statusCode(204);
		useLink.then().statusCode(410);
		newClient().login("teacher@example.com", "brand-new-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S7] Password change replaces the session id")
	void passwordChangeReplacesTheSessionId() {
		// given a logged-in teacher and a copy of the same session cookie
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient copy = teacher.snapshot();

		// when the teacher changes the password
		var change = teacher.post("/api/me/password",
				Map.of("currentPassword", "teacher-pass-1", "newPassword", "brand-new-pass-1"));

		// then the copy gets 401 and the teacher's client, with the cookie from the response, gets 200
		change.then().statusCode(204);
		copy.get("/api/me").then().statusCode(401);
		teacher.get("/api/me").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S8] An over-long current password counts as wrong")
	void anOverLongCurrentPasswordCountsAsWrong() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher changes the password giving a current password of 40 Cyrillic characters (80 bytes)
		var response = teacher.post("/api/me/password",
				Map.of("currentPassword", "ж".repeat(40), "newPassword", "brand-new-pass-1"));

		// then the response is 400 with code WRONG_PASSWORD and the old password still works
		response.then().statusCode(400).body("code", equalTo("WRONG_PASSWORD"));
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S10] An over-long ASCII current password counts as wrong")
	void anOverLongAsciiCurrentPasswordCountsAsWrong() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher changes the password giving a current password of 73 ASCII characters
		var response = teacher.post("/api/me/password",
				Map.of("currentPassword", "a".repeat(73), "newPassword", "brand-new-pass-1"));

		// then the response is 400 with code WRONG_PASSWORD and the old password still works
		response.then().statusCode(400).body("code", equalTo("WRONG_PASSWORD"));
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[accounts/R9-S11] A name of only Unicode spaces is rejected")
	void aNameOfOnlyUnicodeSpacesIsRejected() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher sets the name to a single U+2003 space
		var response = teacher.patch("/api/me", Map.of("name", "\u2003", "timeZone", "UTC"));

		// then the response is 400 and the name does not change
		response.then().statusCode(400);
		teacher.get("/api/me").then().statusCode(200).body("name", equalTo("Teacher"));
	}
}
