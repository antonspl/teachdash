package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R11 field rules. */
class AccountFieldRulesTest extends ApiTest {

	private static final String TEACHERS = "/api/admin/teachers";

	@Test
	@DisplayName("[accounts/R11-S1] Limits apply to teacher creation")
	void limitsApplyToTeacherCreation() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the admin creates a teacher with each of these invalid values
		var longName = admin.post(TEACHERS,
				Map.of("name", "a".repeat(201), "email", "anna@example.com", "timeZone", "UTC"));
		var longEmail = admin.post(TEACHERS,
				Map.of("name", "Anna", "email", "a".repeat(309) + "@example.com", "timeZone", "UTC"));
		var blankEmail = admin.post(TEACHERS,
				Map.of("name", "Anna", "email", " ", "timeZone", "UTC"));
		var blankTimeZone = admin.post(TEACHERS,
				Map.of("name", "Anna", "email", "anna@example.com", "timeZone", " "));
		var noTimeZone = admin.post(TEACHERS,
				Map.of("name", "Anna", "email", "anna@example.com"));
		var invisibleName = admin.post(TEACHERS,
				Map.of("name", " ", "email", "anna@example.com", "timeZone", "UTC"));

		// then each response is 400 with code VALIDATION_FAILED
		longName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		longEmail.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		blankEmail.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		blankTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		noTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		invisibleName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[accounts/R11-S2] Limits apply to the profile")
	void limitsApplyToTheProfile() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher updates the profile with each of these invalid values
		var longName = teacher.patch("/api/me", Map.of("name", "a".repeat(201), "timeZone", "Asia/Dubai"));
		var blankTimeZone = teacher.patch("/api/me", Map.of("name", "Anna", "timeZone", " "));
		var noTimeZone = teacher.patch("/api/me", Map.of("name", "Anna"));
		var invisibleName = teacher.patch("/api/me", Map.of("name", " ", "timeZone", "Asia/Dubai"));

		// then each response is 400 with code VALIDATION_FAILED
		longName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		blankTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		noTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		invisibleName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and the profile is unchanged
		var stored = jdbc.sql("select name, time_zone from users where email = 'teacher@example.com'")
				.query((rs, i) -> rs.getString("name") + "|" + rs.getString("time_zone"))
				.single();
		assertEquals("Teacher|UTC", stored);
		teacher.get("/api/me").then().statusCode(200)
				.body("name", equalTo("Teacher"))
				.body("timeZone", equalTo("UTC"));
	}

	@Test
	@DisplayName("[accounts/R11-S3] Whitespace around values is ignored")
	void whitespaceAroundValuesIsIgnored() {
		// given a logged-in admin and a logged-in teacher
		TestClient admin = loggedInAdmin();
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		String name = "a".repeat(199);

		// when the admin creates a teacher with a 199-character name followed by two spaces and email " anna@example.com ", and the teacher updates the profile with a 199-character name followed by two spaces
		var created = admin.post(TEACHERS,
				Map.of("name", name + "  ", "email", " anna@example.com ", "timeZone", "UTC"));
		var updated = teacher.patch("/api/me", Map.of("name", name + "  ", "timeZone", "UTC"));

		// then both responses are successful
		created.then().statusCode(201);
		updated.then().statusCode(200);
		// and the stored names and email have no surrounding whitespace
		var createdRow = jdbc.sql("select name, email from users where id = :id")
				.param("id", UUID.fromString(created.path("id")))
				.query((rs, i) -> rs.getString("name") + "|" + rs.getString("email"))
				.single();
		assertEquals(name + "|anna@example.com", createdRow);
		assertEquals(name, jdbc.sql("select name from users where id = :id")
				.param("id", teacherId)
				.query(String.class)
				.single());
	}
}
