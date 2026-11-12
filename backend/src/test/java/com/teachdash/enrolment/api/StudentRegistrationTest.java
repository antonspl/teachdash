package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

class StudentRegistrationTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";

	/** A missing link gives a token that no link accepts, so the test fails at its THEN step. */
	private String linkToken(Response response) {
		String link = response.path("setPasswordLink");
		return link == null ? "no-link" : tokenFrom(link);
	}

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
		String token = createToken(student, "SET_PASSWORD", false, teacherB);
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
		// and the student is still in teacher B's list
		loggedInAs("b@example.com").get(STUDENTS).then().statusCode(200)
				.body("id", contains(student.toString()));
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
		// given a student with email "student@example.com", assigned to teacher B
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", "student-pass-1", true);
		assignStudent(teacherB, student);
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when teacher A registers a student with email "STUDENT@example.com"
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
		// given a logged-in teacher and a student account with email "nina@example.com" and no teacher
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
		// and the student is not in the teacher's list
		teacher.get(STUDENTS).then().statusCode(200).body("$", empty());
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
		// given a logged-in teacher A and a student with email "nina@example.com", assigned to teacher B
		createTeacher("teacher@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID nina = createUser("STUDENT", "Nina", "nina@example.com", null, true);
		assignStudent(teacherB, nina);
		TestClient teacher = loggedInAs("teacher@example.com");
		String name = "a".repeat(199);

		// when teacher A registers a student with a 199-character name followed by two spaces and email " anna@example.com ", then with email " nina@example.com "
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
	@DisplayName("[student-enrolment/R1-S14] Registering a student with no teacher and a password assigns their account")
	void registeringStudentWithNoTeacherAndPasswordAssignsTheirAccount() {
		// given a student with no teacher, with a name, time zone and the password "old-password-1"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Анна", "anna@example.com", "old-password-1", true);
		jdbc.sql("update users set time_zone = 'Europe/Moscow' where id = :id")
				.param("id", student)
				.update();
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with email " ANNA@example.com ", name "Anna" and time zone "Europe/Berlin"
		var response = teacher.post(STUDENTS,
				Map.of("name", "Anna", "email", " ANNA@example.com ", "timeZone", "Europe/Berlin"));

		// then the response is 201 with the student's id and no set-password link
		response.then().statusCode(201)
				.body("id", equalTo(student.toString()))
				.body("setPasswordLink", nullValue());
		// and there is still exactly one account with that email
		assertEquals(1, jdbc.sql("select count(*) from users where lower(email) = 'anna@example.com'")
				.query(Long.class).single());
		// and the student is in teacher A's list with the old name and time zone
		teacher.get(STUDENTS).then().statusCode(200)
				.body("id", contains(student.toString()))
				.body("name", contains("Анна"))
				.body("timeZone", contains("Europe/Moscow"));
		// and teacher A's request for the student's details returns 200
		teacher.get(STUDENTS + "/" + student).then().statusCode(200);
		// and teacher A is in the student's list of teachers
		loggedInAs("anna@example.com").get("/api/student/teachers").then().statusCode(200)
				.body("id", contains(teacherA.toString()));
		// and the student logs in with "old-password-1"
		newClient().login("anna@example.com", "old-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R1-S15] Registering a student with no teacher and no password assigns them with a new link")
	void registeringStudentWithNoTeacherAndNoPasswordAssignsThemWithNewLink() {
		// given a student without a password and with email "nina@example.com", assigned to teacher B, with an unused set-password link
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Nina", "nina@example.com", null, true);
		assignStudent(teacherB, student);
		createToken(student, "SET_PASSWORD", false, teacherB);
		TestClient b = loggedInAs("b@example.com");
		TestClient a = loggedInAs("a@example.com");

		// when teacher B removes the student, teacher A then registers a student with email "Nina@Example.com", and the link returned to teacher A is used to set the password "new-password-1"
		var removal = b.delete(STUDENTS + "/" + student);
		var response = a.post(STUDENTS,
				Map.of("name", "Nina", "email", "Nina@Example.com", "timeZone", "UTC"));
		var passwordSet = newClient().post(
				"/api/password-links",
				Map.of("token", linkToken(response), "password", "new-password-1"));

		// then the response to registering is 201 with the student's id and a set-password link
		removal.then().statusCode(204);
		response.then().statusCode(201)
				.body("id", equalTo(student.toString()))
				.body("setPasswordLink", startsWith(APP_BASE_URL + "/set-password#token="));
		// and there is still exactly one account with that email
		assertEquals(1, jdbc.sql("select count(*) from users where lower(email) = 'nina@example.com'")
				.query(Long.class).single());
		// and the student is in teacher A's list
		a.get(STUDENTS).then().statusCode(200).body("id", contains(student.toString()));
		// and the student logs in with "new-password-1"
		passwordSet.then().statusCode(200);
		newClient().login("nina@example.com", "new-password-1").then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R1-S16] Parallel requests assign a student with no teacher once")
	void parallelRequestsAssignAStudentWithNoTeacherOnce() throws Exception {
		// given a student without a password and with no teacher, and two logged-in teachers
		createTeacher("a@example.com", "teacher-pass-1");
		createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Nina", "nina@example.com", null, true);
		int requests = 8;
		List<TestClient> clients = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			clients.add(loggedInAs(i % 2 == 0 ? "a@example.com" : "b@example.com"));
		}
		CountDownLatch ready = new CountDownLatch(requests);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(requests);

		// when both teachers send several parallel requests to register that student's email
		List<Future<Response>> responses = new ArrayList<>();
		try {
			for (TestClient client : clients) {
				Callable<Response> call = () -> {
					ready.countDown();
					start.await();
					return client.post(STUDENTS,
							Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"));
				};
				responses.add(executor.submit(call));
			}
			ready.await();
			start.countDown();
			List<Response> results = new ArrayList<>();
			for (Future<Response> response : responses) {
				results.add(response.get());
			}

			// then exactly one response is 201
			List<Integer> codes = results.stream().map(Response::statusCode).toList();
			assertEquals(1, codes.stream().filter(code -> code == 201).count(), codes.toString());
			// and the other responses to the winning teacher are 409 with code ALREADY_ENROLLED,
			// and the responses to the other teacher are 409 with code STUDENT_EXISTS
			int winner = codes.indexOf(201) % 2;
			for (int i = 0; i < requests; i++) {
				if (codes.get(i) == 201) {
					continue;
				}
				String expected = i % 2 == winner ? "ALREADY_ENROLLED" : "STUDENT_EXISTS";
				assertEquals(409, codes.get(i), codes.toString());
				assertEquals(expected, results.get(i).path("code"), codes.toString());
			}
		} finally {
			executor.shutdownNow();
		}
		// and the student has exactly one assignment and one unused link
		assertEquals(1, jdbc.sql("select count(*) from teacher_students where student_id = :id")
				.param("id", student)
				.query(Long.class).single());
		assertEquals(1, jdbc.sql("select count(*) from one_time_tokens where used_at is null")
				.query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S18] New link voids an admin's unused link")
	void newLinkVoidsAnAdminsUnusedLink() {
		// given a student without a password and with no teacher, with an unused reset link issued by an admin
		createTeacher("a@example.com", "teacher-pass-1");
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		UUID student = createUser("STUDENT", "Nina", "nina@example.com", null, true);
		String adminToken = createToken(student, "RESET_PASSWORD", false, adminId);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with that email, the admin's link is then used to set the password "admin-password-1", and the link returned to teacher A is used to set the password "new-password-1"
		var response = teacher.post(STUDENTS,
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"));
		var adminLink = newClient().post(
				"/api/password-links",
				Map.of("token", adminToken, "password", "admin-password-1"));
		var teacherLink = newClient().post(
				"/api/password-links",
				Map.of("token", linkToken(response), "password", "new-password-1"));

		// then the response to registering is 201 with a set-password link
		response.then().statusCode(201).body("setPasswordLink", notNullValue());
		// and the admin's link response is 410 with code LINK_INVALID
		adminLink.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the student logs in with "new-password-1"
		teacherLink.then().statusCode(200);
		newClient().login("nina@example.com", "new-password-1").then().statusCode(200);
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
