package com.teachdash.common;

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

class FieldRejectionErrorsTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	@Test
	@DisplayName("[api-errors/R2-S1] Taken email at teacher creation")
	void takenEmailAtTeacherCreation() {
		// given a logged-in admin and a teacher with email "t@example.com"
		TestClient admin = loggedInAdmin();
		createTeacher("t@example.com", "teacher-pass-1");

		// when the admin creates another teacher with the email " T@example.com "
		var response = admin.post("/api/admin/teachers",
				Map.of("name", "Anna", "email", " T@example.com ", "timeZone", "UTC", "nativeLanguage", "en"));

		// then the response is 409 EMAIL_TAKEN naming the email field
		response.then().statusCode(409)
				.body("code", equalTo("EMAIL_TAKEN"))
				.body("errors", equalTo(List.of(error("email", "EMAIL_TAKEN"))));
	}

	@Test
	@DisplayName("[api-errors/R2-S2] Taken email at student registration")
	void takenEmailAtStudentRegistration() {
		// given a logged-in teacher B and an admin with email "a@example.com"
		createAdmin("a@example.com", "admin-password-1");
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");

		// when teacher B registers a student with the email "a@example.com"
		var response = teacherB.post(STUDENTS,
				Map.of("name", "Анна", "email", "a@example.com", "timeZone", "Europe/Moscow", "nativeLanguage", "en"));

		// then the response is 409 EMAIL_TAKEN naming the email field
		response.then().statusCode(409)
				.body("code", equalTo("EMAIL_TAKEN"))
				.body("errors", equalTo(List.of(error("email", "EMAIL_TAKEN"))));
		assertEquals(0, countRows("teacher_students"));
	}

	@Test
	@DisplayName("[api-errors/R2-S3] Taken email when joining through a link")
	void takenEmailWhenJoiningThroughALink() {
		// given teacher A with a join link, a teacher with email "t@example.com", and a client with no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		createTeacher("t@example.com", "teacher-pass-2");
		TestClient client = newClient();
		long usersBefore = countRows("users");

		// when the client joins through the link with the email "t@example.com"
		var response = client.post("/api/join-links/join", Map.of(
				"token", token,
				"name", "Anna",
				"email", "t@example.com",
				"timeZone", "Europe/Berlin",
				"nativeLanguage", "en",
				"password", "password-1"));

		// then the response is 409 EMAIL_TAKEN naming the email field
		response.then().statusCode(409)
				.body("code", equalTo("EMAIL_TAKEN"))
				.body("errors", equalTo(List.of(error("email", "EMAIL_TAKEN"))));
		assertEquals(usersBefore, countRows("users"));
	}

	@Test
	@DisplayName("[api-errors/R2-S4] Wrong current password")
	void wrongCurrentPassword() {
		// given a logged-in teacher with the password "пароль-старый-1"
		createTeacher("teacher@example.com", "пароль-старый-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher changes the password with a wrong current password
		var response = teacher.post("/api/me/password",
				Map.of("currentPassword", "пароль-неверный-1", "newPassword", "new-password-1"));

		// then the response is 400 WRONG_PASSWORD naming the current password field
		response.then().statusCode(400)
				.body("code", equalTo("WRONG_PASSWORD"))
				.body("errors", equalTo(List.of(error("currentPassword", "WRONG_PASSWORD"))));
		newClient().login("teacher@example.com", "пароль-старый-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[api-errors/R2-S5] Other errors carry no field list")
	void otherErrorsCarryNoFieldList() {
		// given teacher A with a student assigned to them, and a logged-in teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when someone logs in with a wrong password, teacher A registers the assigned student's email again, and teacher A opens a malformed student id
		var login = newClient().login("student@example.com", "wrong-password-1");
		var registration = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "UTC", "nativeLanguage", "en"));
		var malformedId = teacher.get(STUDENTS + "/not-a-uuid");

		// then the responses are 401 INVALID_CREDENTIALS, 409 ALREADY_ENROLLED and 404 NOT_FOUND, none with errors
		login.then().statusCode(401)
				.body("code", equalTo("INVALID_CREDENTIALS"))
				.body("$", not(hasKey("errors")));
		registration.then().statusCode(409)
				.body("code", equalTo("ALREADY_ENROLLED"))
				.body("$", not(hasKey("errors")));
		malformedId.then().statusCode(404)
				.body("code", equalTo("NOT_FOUND"))
				.body("$", not(hasKey("errors")));
	}

	@Test
	@DisplayName("[api-errors/R2-S6] Taken course title")
	void takenCourseTitle() {
		// given a logged-in teacher with a course titled "Грамматика"
		UUID teacher = createTeacher("teacher@example.com", "teacher-pass-1");
		createCourse(teacher, "Грамматика", "ru", "ru");
		TestClient client = loggedInAs("teacher@example.com");

		// when the teacher creates a course titled " ГРАММАТИКА " with the instruction language "ru"
		var response = client.post("/api/teacher/courses",
				Map.of("title", " ГРАММАТИКА ", "instructionLanguage", "ru"));

		// then the response is 409 TITLE_TAKEN naming the title field
		response.then().statusCode(409)
				.body("code", equalTo("TITLE_TAKEN"))
				.body("errors", equalTo(List.of(error("title", "TITLE_TAKEN"))));
	}
}
