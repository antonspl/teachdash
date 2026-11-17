package com.teachdash.languages.api;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R1. */
class LanguagesTest extends ApiTest {

	private static final String LANGUAGES = "/api/languages";

	private static final List<Map<String, Object>> EXPECTED = List.of(
			language("de", false),
			language("en", true),
			language("es", false),
			language("fr", false),
			language("it", false),
			language("pl", false),
			language("pt", false),
			language("ru", true),
			language("sr", false),
			language("uk", false));

	private static Map<String, Object> language(String code, boolean content) {
		return Map.of("code", code, "content", content);
	}

	@Test
	@DisplayName("[languages/R1-S1] A visitor reads the list prefilled by the DB migration")
	void aVisitorReadsTheListPrefilledByTheDbMigration() {
		// given a client with no session
		TestClient visitor = newClient();

		// when the client reads the language list
		var response = visitor.get(LANGUAGES);

		// then the response is 200 with the prefilled list
		response.then().statusCode(200).body("$", equalTo(EXPECTED));
	}

	@Test
	@DisplayName("[languages/R1-S2] Every role reads the same list")
	void everyRoleReadsTheSameList() {
		// given a logged-in admin, a logged-in teacher and a logged-in student
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");

		// when each of them reads the language list
		var asAdmin = admin.get(LANGUAGES);
		var asTeacher = teacher.get(LANGUAGES);
		var asStudent = student.get(LANGUAGES);

		// then each response is 200 with the same body as for a client with no session
		asAdmin.then().statusCode(200).body("$", equalTo(EXPECTED));
		asTeacher.then().statusCode(200).body("$", equalTo(EXPECTED));
		asStudent.then().statusCode(200).body("$", equalTo(EXPECTED));
	}
}
