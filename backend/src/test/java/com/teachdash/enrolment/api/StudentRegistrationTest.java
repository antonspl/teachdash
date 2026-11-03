package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

class StudentRegistrationTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";

	@Test
	@DisplayName("[student-enrolment/R1-S1] New email creates and assigns a student")
	void newEmailCreatesAndAssignsAStudent() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher registers a student with a new email and the returned link sets the password
		var response = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "Europe/Berlin"));
		var passwordSet = newClient().post(
				"/api/password-links",
				Map.of("token", tokenFrom(response.path("setPasswordLink")), "password", "new-password-1"));

		// then the response is 201 with the student's id and a link with the base URL and a fragment token
		response.then().statusCode(201)
				.body("id", notNullValue())
				.body("setPasswordLink", startsWith(APP_BASE_URL + "/set-password#token="));
		String id = response.path("id");
		// and the student appears in the teacher's list
		teacher.get(STUDENTS).then().statusCode(200).body("id", contains(id));
		// and the link sets the password, the student logs in with it and has the role student
		passwordSet.then().statusCode(200);
		TestClient student = newClient();
		student.login("nina@example.com", "new-password-1").then().statusCode(200);
		student.get("/api/me").then().statusCode(200)
				.body("id", equalTo(id))
				.body("role", equalTo("STUDENT"));
	}

	@Test
	@DisplayName("[student-enrolment/R1-S2] Existing student is not assigned")
	void existingStudentIsNotAssigned() {
		// given a student without a password, assigned to teacher B, with an unused set-password link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Existing", "student@example.com", null, true);
		assignStudent(teacherB, student);
		String token = createToken(student, "SET_PASSWORD", false);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with that email and a different name and time zone
		var response = teacher.post(STUDENTS,
				Map.of("name", "Other Name", "email", "student@example.com", "timeZone", "Asia/Tokyo"));

		// then the response is 409 with code STUDENT_EXISTS
		response.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is not in teacher A's list
		assertEquals(0, jdbc.sql("select count(*) from teacher_students where teacher_id = :teacher")
				.param("teacher", teacherA)
				.query(Long.class)
				.single());
		teacher.get(STUDENTS).then().statusCode(200).body("$", empty());
		// and the student's name and time zone are unchanged
		var stored = jdbc.sql("select name, time_zone from users where id = :id")
				.param("id", student)
				.query((rs, i) -> rs.getString("name") + "|" + rs.getString("time_zone"))
				.single();
		assertEquals("Existing|UTC", stored);
		// and the unused link still sets the password
		newClient().post("/api/password-links", Map.of("token", token, "password", "new-password-1"))
				.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R1-S3] Email of a teacher or admin is rejected")
	void emailOfATeacherOrAdminIsRejected() {
		// given a teacher account and an admin account, and another teacher
		createUser("TEACHER", "Other", "t@example.com", "teacher-pass-1", true);
		createAdmin("a@example.com", "admin-password-1");
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the other teacher registers a student with each of these emails
		var withTeacherEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "t@example.com", "timeZone", "UTC"));
		var withAdminEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "a@example.com", "timeZone", "UTC"));

		// then both responses are 409 with code EMAIL_TAKEN
		withTeacherEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		withAdminEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		// and nothing is assigned
		assertEquals(0, jdbc.sql("select count(*) from teacher_students").query(Long.class).single());
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S4] Already assigned student is rejected")
	void alreadyAssignedStudentIsRejected() {
		// given a student assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with that email again
		var response = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "UTC"));

		// then the response is 409 with code ALREADY_ENROLLED
		response.then().statusCode(409).body("code", equalTo("ALREADY_ENROLLED"));
		// and there is still exactly one assignment between them
		assertEquals(1, jdbc.sql("""
				select count(*) from teacher_students
				where teacher_id = :teacher and student_id = :student
				""")
				.param("teacher", teacherA)
				.param("student", student)
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S5] Email case is ignored")
	void emailCaseIsIgnored() {
		// given a student account with email "student@example.com"
		createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when a teacher registers a student with email "STUDENT@example.com"
		var response = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "STUDENT@example.com", "timeZone", "UTC"));

		// then the response is 409 with code STUDENT_EXISTS
		response.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
		// and the student is not in the teacher's list
		teacher.get(STUDENTS).then().statusCode(200).body("$", empty());
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S6] Invalid input is rejected")
	void invalidInputIsRejected() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with a blank name, an invalid email and an unknown time zone
		var blankName = teacher.post(STUDENTS,
				Map.of("name", " ", "email", "student@example.com", "timeZone", "UTC"));
		var invalidEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "not-an-email", "timeZone", "UTC"));
		var unknownTimeZone = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", "Mars/Base"));

		// then each response is 400 with code VALIDATION_FAILED
		blankName.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		invalidEmail.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		unknownTimeZone.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S7] Email with invisible characters is rejected")
	void emailWithInvisibleCharactersIsRejected() {
		// given a logged-in teacher and a student account with email "nina@example.com"
		createTeacher("teacher@example.com", "teacher-pass-1");
		createUser("STUDENT", "Nina", "nina@example.com", null, true);
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with email "nina\u00A0@example.com", then with email "\u2003@example.com"
		var noBreakSpace = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina\u00A0@example.com", "timeZone", "UTC"));
		var emSpace = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "\u2003@example.com", "timeZone", "UTC"));

		// then each response is 400 with code VALIDATION_FAILED
		noBreakSpace.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		emSpace.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S8] Field limits apply")
	void fieldLimitsApply() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with each of these invalid values
		var longName = teacher.post(STUDENTS,
				Map.of("name", "a".repeat(201), "email", "student@example.com", "timeZone", "UTC"));
		var longEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "a".repeat(309) + "@example.com", "timeZone", "UTC"));
		var blankEmail = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", " ", "timeZone", "UTC"));
		var blankTimeZone = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com", "timeZone", " "));
		var noTimeZone = teacher.post(STUDENTS,
				Map.of("name", "Student", "email", "student@example.com"));
		var invisibleName = teacher.post(STUDENTS,
				Map.of("name", "\u00A0", "email", "student@example.com", "timeZone", "UTC"));

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
	@DisplayName("[student-enrolment/R1-S9] Surrounding whitespace is ignored")
	void surroundingWhitespaceIsIgnored() {
		// given a logged-in teacher and a student account with email "nina@example.com"
		createTeacher("teacher@example.com", "teacher-pass-1");
		createUser("STUDENT", "Nina", "nina@example.com", null, true);
		TestClient teacher = loggedInAs("teacher@example.com");
		String name = "a".repeat(199);

		// when the teacher registers a student with a 199-character name followed by two spaces and email " anna@example.com ", then with email " nina@example.com "
		var padded = teacher.post(STUDENTS,
				Map.of("name", name + "  ", "email", " anna@example.com ", "timeZone", "UTC"));
		var existing = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", " nina@example.com ", "timeZone", "UTC"));

		// then the first response is 201 and the account stores the name and email without the whitespace
		padded.then().statusCode(201);
		var stored = jdbc.sql("select name, email from users where id = :id")
				.param("id", UUID.fromString(padded.path("id")))
				.query((rs, i) -> rs.getString("name") + "|" + rs.getString("email"))
				.single();
		assertEquals(name + "|anna@example.com", stored);
		// and the second response is 409 with code STUDENT_EXISTS
		existing.then().statusCode(409).body("code", equalTo("STUDENT_EXISTS"));
	}

	@Test
	@DisplayName("[student-enrolment/R1-S10] Submitted values are stored")
	void submittedValuesAreStored() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher registers a student with name "\u0410\u043d\u043d\u0430", email "anna@example.com" and time zone "Europe/Moscow"
		var created = teacher.post(STUDENTS,
				Map.of("name", "\u0410\u043d\u043d\u0430", "email", "anna@example.com", "timeZone", "Europe/Moscow"));

		// then the student's details return that name, email and time zone
		created.then().statusCode(201);
		teacher.get(STUDENTS + "/" + created.path("id")).then().statusCode(200)
				.body("name", equalTo("\u0410\u043d\u043d\u0430"))
				.body("email", equalTo("anna@example.com"))
				.body("timeZone", equalTo("Europe/Moscow"));
	}

	@Test
	@DisplayName("[student-enrolment/R1-S11] Request without a CSRF token is rejected")
	void requestWithoutACsrfTokenIsRejected() {
		// given a logged-in teacher
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com").withoutCsrfHeader();
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student without a CSRF token
		var response = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"));

		// then the response is 403 with code CSRF_INVALID
		response.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S13] Parallel requests create one account")
	void parallelRequestsCreateOneAccount() throws Exception {
		// given two logged-in teachers
		createTeacher("a@example.com", "teacher-pass-1");
		createTeacher("b@example.com", "teacher-pass-1");
		int requests = 8;
		List<TestClient> clients = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			clients.add(loggedInAs(i % 2 == 0 ? "a@example.com" : "b@example.com"));
		}
		CountDownLatch ready = new CountDownLatch(requests);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(requests);

		// when both send several parallel requests to register the same new email
		List<Future<Integer>> statuses = new ArrayList<>();
		try {
			for (TestClient client : clients) {
				Callable<Integer> call = () -> {
					ready.countDown();
					start.await();
					return client.post(STUDENTS,
							Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"))
							.statusCode();
				};
				statuses.add(executor.submit(call));
			}
			ready.await();
			start.countDown();
			List<Integer> codes = new ArrayList<>();
			for (Future<Integer> status : statuses) {
				codes.add(status.get());
			}

			// then exactly one response is 201 and all others are 409
			assertEquals(1, codes.stream().filter(code -> code == 201).count(), codes.toString());
			assertEquals(requests - 1, codes.stream().filter(code -> code == 409).count(), codes.toString());
		} finally {
			executor.shutdownNow();
		}
		// and there is exactly one account, one assignment and one unused link
		assertEquals(1, jdbc.sql("select count(*) from users where email = 'nina@example.com'")
				.query(Long.class).single());
		assertEquals(1, jdbc.sql("select count(*) from teacher_students").query(Long.class).single());
		assertEquals(1, jdbc.sql("select count(*) from one_time_tokens where used_at is null")
				.query(Long.class).single());
	}
}
