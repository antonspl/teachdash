package com.teachdash.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

class ApiErrorsTest extends ApiTest {

	private static final String TEACHERS = "/api/admin/teachers";
	private static final String STUDENTS = "/api/teacher/students";
	private static final String PASSWORD = "/api/me/password";
	private static final String LOGIN = "/api/auth/login";
	private static final String PREVIEW = "/api/join-links/preview";
	private static final String JOIN = "/api/join-links/join";
	private static final String TEST_QUERY = "/api/teacher/test-query";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	@Test
	@DisplayName("[api-errors/R1-S1] Missing and blank fields are required")
	void missingAndBlankFieldsAreRequired() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = countRows("users");

		// when the admin creates a teacher with a blank name and no email, and once more with a null email and a blank time zone
		var blankNameNoEmail = admin.post(TEACHERS, """
				{"name": "   ", "timeZone": "Europe/Berlin"}""");
		var nullEmailBlankZone = admin.post(TEACHERS, """
				{"name": "Anna", "email": null, "timeZone": " "}""");

		// then both responses are 400 VALIDATION_FAILED with the required fields named
		blankNameNoEmail.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("email", "REQUIRED"), error("name", "REQUIRED"))));
		nullEmailBlankZone.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("email", "REQUIRED"), error("timeZone", "REQUIRED"))));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S2] Too-long values in Cyrillic and Latin")
	void tooLongValuesInCyrillicAndLatin() {
		// given a logged-in admin and a logged-in teacher
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		String longEmail = "a".repeat(309) + "@example.com";
		long usersBefore = countRows("users");
		long sessionsBefore = countRows("spring_session");

		// when the admin creates a teacher with a long Cyrillic name and a long email, the teacher updates the profile with a long Latin name, and someone logs in with a long email
		var created = admin.post(TEACHERS,
				Map.of("name", "Я".repeat(201), "email", longEmail, "timeZone", "UTC"));
		var updated = teacher.patch("/api/me", Map.of("name", "a".repeat(201), "timeZone", "UTC"));
		var loggedIn = newClient().post(LOGIN, Map.of("email", longEmail, "password", "password-1"));

		// then the responses are 400 with the too-long fields named
		created.then().statusCode(400)
				.body("errors", equalTo(List.of(error("email", "TOO_LONG"), error("name", "TOO_LONG"))));
		updated.then().statusCode(400)
				.body("errors", equalTo(List.of(error("name", "TOO_LONG"))));
		loggedIn.then().statusCode(400)
				.body("errors", equalTo(List.of(error("email", "TOO_LONG"))));
		assertEquals(usersBefore, countRows("users"));
		assertEquals("Teacher", jdbc.sql("select name from users where email = 'teacher@example.com'")
				.query(String.class).single());
		assertEquals(sessionsBefore, countRows("spring_session"));
	}

	@Test
	@DisplayName("[api-errors/R1-S3] Invalid formats are reported without the values")
	void invalidFormatsAreReportedWithoutTheValues() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = countRows("users");

		// when the teacher registers a student with an invisible name, a broken email and an unknown time zone
		var response = teacher.post(STUDENTS,
				Map.of("name", "ㅤ", "email", "anna@", "timeZone", "Mars/Olympus"));

		// then the invalid fields are named and the body holds none of the values
		response.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(
						error("email", "INVALID_FORMAT"),
						error("name", "INVALID_FORMAT"),
						error("timeZone", "INVALID_FORMAT"))))
				.body(not(containsString("anna@")))
				.body(not(containsString("Mars/Olympus")));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S4] Password rules have separate codes")
	void passwordRulesHaveSeparateCodes() {
		// given a logged-in teacher with the password "old-password-1"
		createTeacher("teacher@example.com", "old-password-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher changes the password with each invalid new password, and once more with a blank current password
		var empty = changePassword(teacher, "old-password-1", "");
		var tooShort = changePassword(teacher, "old-password-1", "1234567");
		var tooManyChars = changePassword(teacher, "old-password-1", "a".repeat(73));
		var tooManyBytes = changePassword(teacher, "old-password-1", "я".repeat(37));
		var blankCurrent = changePassword(teacher, " ", "new-password-1");

		// then each response is 400 VALIDATION_FAILED with the failing password field named
		empty.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("newPassword", "REQUIRED"))));
		tooShort.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("newPassword", "TOO_SHORT"))));
		tooManyChars.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("newPassword", "TOO_LONG"))));
		tooManyBytes.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("newPassword", "TOO_LONG"))));
		blankCurrent.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("currentPassword", "REQUIRED"))));
		newClient().login("teacher@example.com", "old-password-1").then().statusCode(200);
	}

	private io.restassured.response.Response changePassword(
			TestClient client,
			String currentPassword,
			String newPassword) {
		return client.post(PASSWORD, Map.of("currentPassword", currentPassword, "newPassword", newPassword));
	}

	@Test
	@DisplayName("[api-errors/R1-S5] Join-link input names the failing fields")
	void joinLinkInputNamesTheFailingFields() {
		// given teacher A with a join link, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient client = newClient();
		long usersBefore = countRows("users");

		// when the client previews with a blank token, joins with a blank name and a short password, and joins with a blank token, a broken email and no time zone
		var preview = client.post(PREVIEW, Map.of("token", " "));
		var firstJoin = client.post(JOIN, Map.of(
				"token", token,
				"name", "",
				"email", "anna@example.com",
				"timeZone", "Europe/Moscow",
				"password", "short"));
		var secondJoin = client.post(JOIN, Map.of(
				"token", " ",
				"name", "Анна",
				"email", "anna@",
				"password", "password-1"));

		// then each response is 400 with the failing fields named
		preview.then().statusCode(400)
				.body("errors", equalTo(List.of(error("token", "REQUIRED"))));
		firstJoin.then().statusCode(400)
				.body("errors", equalTo(List.of(error("name", "REQUIRED"), error("password", "TOO_SHORT"))));
		secondJoin.then().statusCode(400)
				.body("errors", equalTo(List.of(
						error("email", "INVALID_FORMAT"),
						error("timeZone", "REQUIRED"),
						error("token", "REQUIRED"))));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S6] A NUL character names its field")
	void nulCharacterNamesItsField() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = countRows("users");
		long sessionsBefore = countRows("spring_session");

		// when someone logs in with a NUL in the email, and the teacher registers a student with a NUL in the name and a blank email
		var login = newClient().post(LOGIN, """
				{"email": "a\\u0000@example.com", "password": "password-1"}""");
		var registration = teacher.post(STUDENTS, """
				{"name": "Ан\\u0000на", "email": " ", "timeZone": "Europe/Moscow"}""");

		// then both responses are 400 VALIDATION_FAILED naming only the field with the NUL
		login.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("email", "INVALID_FORMAT"))));
		registration.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("name", "INVALID_FORMAT"))))
				.body(not(containsString("Ан")));
		assertEquals(sessionsBefore, countRows("spring_session"));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S7] A wrong JSON type is a validation error")
	void wrongJsonTypeIsAValidationError() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = countRows("users");

		// when the admin creates a teacher with an array as the name, and once more with an object as the email
		var arrayName = admin.post(TEACHERS, """
				{"name": ["Anna"], "email": "anna@example.com", "timeZone": "Europe/Berlin"}""");
		var objectEmail = admin.post(TEACHERS, """
				{"name": "Анна", "email": {"value": "anna@example.com"}, "timeZone": "Europe/Berlin"}""");

		// then both responses are 400 VALIDATION_FAILED with the field named as a wrong type
		arrayName.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("name", "INVALID_TYPE"))))
				.body(not(containsString("Anna")))
				.body(not(containsString("anna@example.com")));
		objectEmail.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("email", "INVALID_TYPE"))))
				.body(not(containsString("Anna")))
				.body(not(containsString("anna@example.com")));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S8] Broken JSON stays a malformed request")
	void brokenJsonStaysAMalformedRequest() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = countRows("users");

		// when the admin creates a teacher with a truncated body, with an array body, and with no body
		var truncated = admin.post(TEACHERS, "{\"name\":");
		var array = admin.post(TEACHERS, "[]");
		var noBody = admin.post(TEACHERS);

		// then each response is 400 MALFORMED_REQUEST with no errors property
		truncated.then().statusCode(400)
				.body("code", equalTo("MALFORMED_REQUEST"))
				.body("$", not(hasKey("errors")));
		array.then().statusCode(400)
				.body("code", equalTo("MALFORMED_REQUEST"))
				.body("$", not(hasKey("errors")));
		noBody.then().statusCode(400)
				.body("code", equalTo("MALFORMED_REQUEST"))
				.body("$", not(hasKey("errors")));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R1-S9] Query parameters are named")
	void queryParametersAreNamed() {
		// given an endpoint with a required numeric page and an optional text q of at most 10 characters, and a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher calls it with no page, with page=abc, and with page=1 and q of 11 Cyrillic letters
		var noPage = teacher.get(TEST_QUERY);
		var wrongType = teacher.get(TEST_QUERY + "?page=abc");
		var tooLong = teacher.get(TEST_QUERY + "?page=1&q=" + "Ж".repeat(11));

		// then each response is 400 VALIDATION_FAILED naming the parameter
		noPage.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("page", "REQUIRED"))));
		wrongType.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("page", "INVALID_TYPE"))));
		tooLong.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("q", "TOO_LONG"))));
		for (var response : List.of(noPage, wrongType, tooLong)) {
			response.then().body(not(containsString("abc"))).body(not(containsString("Ж")));
		}
	}
}
