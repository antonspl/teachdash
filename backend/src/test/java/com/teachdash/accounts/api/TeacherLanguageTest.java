package com.teachdash.accounts.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R5 (language at creation), R9 (language in the profile) and R11 (one rule) for teachers. */
class TeacherLanguageTest extends ApiTest {

	private static final String TEACHERS = "/api/admin/teachers";
	private static final String ME = "/api/me";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	/** A creation body; a null language JSON leaves the field out. */
	private static String creation(String name, String email, String languageJson) {
		return """
				{"name": "%s", "email": "%s", "timeZone": "Europe/Berlin"%s}"""
				.formatted(name, email, languageJson == null ? "" : ", \"nativeLanguage\": " + languageJson);
	}

	/** A profile body; a null language JSON leaves the field out. */
	private static String profile(String name, String timeZone, String languageJson) {
		return """
				{"name": "%s", "timeZone": "%s"%s}"""
				.formatted(name, timeZone, languageJson == null ? "" : ", \"nativeLanguage\": " + languageJson);
	}

	private static List<Map<String, String>> errorsOf(Response response) {
		return response.path("errors");
	}

	@Test
	@DisplayName("[accounts/R5-S4] Creation needs a listed native language")
	void creationNeedsAListedNativeLanguage() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = countRows("users");
		List<String> languages = java.util.Arrays.asList(null, "null", "\"\"", "\"  \"", "\"xx\"", "\"SR\"", "\"srp\"");

		// when the admin creates teachers with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn, each of these languages
		List<Response> responses = new ArrayList<>();
		for (int i = 0; i < languages.size(); i++) {
			responses.add(admin.post(TEACHERS, creation("Anna", "anna" + i + "@example.com", languages.get(i))));
		}

		// then the responses are 400 VALIDATION_FAILED with REQUIRED for the first four and INVALID_FORMAT for the others
		for (int i = 0; i < responses.size(); i++) {
			String code = i < 4 ? "REQUIRED" : "INVALID_FORMAT";
			responses.get(i).then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("nativeLanguage", code))));
		}
		// and no body contains "xx" or "srp"
		responses.get(4).then().body(not(containsString("xx")));
		responses.get(6).then().body(not(containsString("srp")));
		// and no account is created
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[accounts/R5-S5] All failing fields are reported together")
	void allFailingFieldsAreReportedTogether() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();

		// when the admin creates a teacher with a blank name, a new email, the time zone "Europe/Berlin" and the native language "xx"
		var response = admin.post(TEACHERS, creation("   ", "anna@example.com", "\"xx\""));

		// then the response is 400 with the errors for name and nativeLanguage
		response.then().statusCode(400)
				.body("errors", equalTo(List.of(error("name", "REQUIRED"), error("nativeLanguage", "INVALID_FORMAT"))));
	}

	@Test
	@DisplayName("[accounts/R5-S6] A wrong JSON type is reported")
	void aWrongJsonTypeIsReported() {
		// given a logged-in admin
		TestClient admin = loggedInAdmin();
		long usersBefore = countRows("users");
		List<String> languages = List.of("[\"sr\"]", "{\"code\": \"sr\"}", "5", "true");

		// when the admin creates teachers with each of these native languages
		List<Response> responses = new ArrayList<>();
		for (int i = 0; i < languages.size(); i++) {
			responses.add(admin.post(TEACHERS, creation("Anna", "anna" + i + "@example.com", languages.get(i))));
		}

		// then all four responses are 400 VALIDATION_FAILED with INVALID_TYPE for nativeLanguage
		for (Response response : responses) {
			response.then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_TYPE"))));
		}
		// and no account is created
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[accounts/R9-S12] A teacher changes their native language")
	void aTeacherChangesTheirNativeLanguage() {
		// given a logged-in teacher with native language "en"
		createTeacher("teacher@example.com", "teacher-pass-1", "en");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher updates the profile with the name "Anna S.", the time zone "Asia/Dubai" and the native language " ru ", and then reads the current user
		var update = teacher.patch(ME, profile("Anna S.", "Asia/Dubai", "\" ru \""));
		var read = teacher.get(ME);

		// then the update returns 200 with nativeLanguage "ru"
		update.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		// and the current user shows "ru", "Anna S." and "Asia/Dubai"
		read.then().statusCode(200)
				.body("nativeLanguage", equalTo("ru"))
				.body("name", equalTo("Anna S."))
				.body("timeZone", equalTo("Asia/Dubai"));
	}

	@Test
	@DisplayName("[accounts/R9-S13] Omitting the native language keeps it")
	void omittingTheNativeLanguageKeepsIt() {
		// given a logged-in teacher with native language "pl"
		createTeacher("teacher@example.com", "teacher-pass-1", "pl");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher updates the profile with a new name and time zone and no native language
		var update = teacher.patch(ME, profile("Anna S.", "Asia/Dubai", null));
		var read = teacher.get(ME);

		// then the response is 200 with nativeLanguage "pl"
		update.then().statusCode(200).body("nativeLanguage", equalTo("pl"));
		// and the new name and time zone are saved
		read.then().statusCode(200)
				.body("name", equalTo("Anna S."))
				.body("timeZone", equalTo("Asia/Dubai"))
				.body("nativeLanguage", equalTo("pl"));
	}

	@Test
	@DisplayName("[accounts/R9-S14] A teacher's invalid language changes nothing")
	void aTeachersInvalidLanguageChangesNothing() {
		// given a logged-in teacher with the name "Anna" and native language "en"
		UUID teacherId = createUser("TEACHER", "Anna", "teacher@example.com", "teacher-pass-1", true, "en");
		TestClient teacher = loggedInAs("teacher@example.com");
		List<String> languages = List.of(
				"null", "\"\"", "\"  \"", "\"xx\"", "\"SR\"", "[\"sr\"]", "{\"code\": \"sr\"}", "5", "true");

		// when the teacher updates the profile with a valid new name and time zone and, in turn, each of these languages
		List<Response> responses = new ArrayList<>();
		for (String language : languages) {
			responses.add(teacher.patch(ME, profile("Anna New", "Asia/Dubai", language)));
		}

		// then the responses are 400 VALIDATION_FAILED with REQUIRED for the first three, INVALID_FORMAT for "xx" and "SR", and INVALID_TYPE for the last four
		List<String> codes = List.of(
				"REQUIRED", "REQUIRED", "REQUIRED", "INVALID_FORMAT", "INVALID_FORMAT",
				"INVALID_TYPE", "INVALID_TYPE", "INVALID_TYPE", "INVALID_TYPE");
		for (int i = 0; i < responses.size(); i++) {
			responses.get(i).then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("nativeLanguage", codes.get(i)))));
		}
		// and the name is still "Anna" and the native language "en"
		assertEquals("Anna", jdbc.sql("select name from users where id = :id")
				.param("id", teacherId).query(String.class).single());
		assertEquals("en", userLanguage(teacherId));
	}

	@Test
	@DisplayName("[accounts/R9-S15] All failing fields are reported together")
	void allFailingProfileFieldsAreReportedTogether() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1", "en");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher updates the profile with a blank name, a valid time zone and the native language "xx"
		var response = teacher.patch(ME, profile("   ", "Asia/Dubai", "\"xx\""));

		// then the response is 400 with the errors for name and nativeLanguage
		response.then().statusCode(400)
				.body("errors", equalTo(List.of(error("name", "REQUIRED"), error("nativeLanguage", "INVALID_FORMAT"))));
	}

	@Test
	@DisplayName("[accounts/R9-S16] A student's or an admin's value is ignored")
	void aStudentsOrAnAdminsValueIsIgnored() {
		// given a logged-in student with native language "ru" and a logged-in admin
		UUID studentId = createStudent("student@example.com", "student-pass-1", "ru");
		TestClient student = loggedInAs("student@example.com");
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		TestClient admin = loggedInAs("admin@example.com");
		List<String> languages = java.util.Arrays.asList(null, "null", "\"xx\"", "\"de\"", "[\"de\"]", "{\"code\": \"de\"}");

		// when each updates the profile with a valid new name and time zone and, in turn, each of these languages
		List<Response> studentResponses = new ArrayList<>();
		List<Response> adminResponses = new ArrayList<>();
		for (String language : languages) {
			studentResponses.add(student.patch(ME, profile("Ivan New", "Asia/Dubai", language)));
			adminResponses.add(admin.patch(ME, profile("Admin New", "Asia/Dubai", language)));
		}

		// then all twelve responses are 200, the student's show nativeLanguage "ru" and the admin's have no nativeLanguage property
		for (Response response : studentResponses) {
			response.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		}
		for (Response response : adminResponses) {
			response.then().statusCode(200).body("$", not(hasKey("nativeLanguage")));
		}
		// and the new names are saved
		student.get(ME).then().body("name", equalTo("Ivan New")).body("nativeLanguage", equalTo("ru"));
		admin.get(ME).then().body("name", equalTo("Admin New")).body("$", not(hasKey("nativeLanguage")));
		assertEquals("ru", userLanguage(studentId));
		assertEquals(null, userLanguage(adminId));
	}

	@Test
	@DisplayName("[accounts/R9-S17] A teacher's change does not touch other accounts")
	void aTeachersChangeDoesNotTouchOtherAccounts() {
		// given a logged-in teacher with native language "en", their student with native language "sr", another teacher with native language "fr" and an admin
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1", "en");
		UUID studentId = createStudent("student@example.com", "student-pass-1", "sr");
		assignStudent(teacherId, studentId);
		UUID otherTeacherId = createTeacher("other@example.com", "teacher-pass-1", "fr");
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher updates the profile with the native language "de"
		var response = teacher.patch(ME, profile("Teacher", "UTC", "\"de\""));

		// then the response is 200 and the teacher's own language is "de"
		response.then().statusCode(200).body("nativeLanguage", equalTo("de"));
		assertEquals("de", userLanguage(teacherId));
		// and the student, the other teacher and the admin still have "sr", "fr" and no language
		assertEquals("sr", userLanguage(studentId));
		assertEquals("fr", userLanguage(otherTeacherId));
		assertEquals(null, userLanguage(adminId));
	}

	@Test
	@DisplayName("[accounts/R11-S6] The native language has one rule in creation and in the profile")
	void theNativeLanguageHasOneRuleInCreationAndInTheProfile() {
		// given a logged-in admin and a logged-in teacher with native language "en"
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1", "en");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = countRows("users");
		List<String> languages = List.of("\"xx\"", "\"SR\"", "\"  \"", "[\"sr\"]");

		// when the admin creates teachers and the teacher updates the profile, each with these languages and otherwise valid fields
		List<Response> created = new ArrayList<>();
		List<Response> updated = new ArrayList<>();
		for (int i = 0; i < languages.size(); i++) {
			created.add(admin.post(TEACHERS, creation("Anna", "anna" + i + "@example.com", languages.get(i))));
			updated.add(teacher.patch(ME, profile("Teacher New", "Asia/Dubai", languages.get(i))));
		}

		// then the creation and the profile responses are 400 with the same errors for each value
		List<String> codes = List.of("INVALID_FORMAT", "INVALID_FORMAT", "REQUIRED", "INVALID_TYPE");
		for (int i = 0; i < languages.size(); i++) {
			created.get(i).then().statusCode(400);
			updated.get(i).then().statusCode(400);
			assertEquals(List.of(error("nativeLanguage", codes.get(i))), errorsOf(created.get(i)));
			assertEquals(errorsOf(created.get(i)), errorsOf(updated.get(i)));
		}
		// and nothing is created or changed
		assertEquals(usersBefore, countRows("users"));
		teacher.get(ME).then().body("name", equalTo("Teacher")).body("nativeLanguage", equalTo("en"));
	}
}
