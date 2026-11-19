package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R13, R14 and R15. */
class StudentLanguageTest extends ApiTest {

	private static final String STUDENTS = "/api/teacher/students";
	private static final String JOIN = "/api/join-links/join";

	private static Map<String, String> error(String field, String code) {
		return Map.of("field", field, "code", code);
	}

	private Map<String, Object> registration(String name, String email, String timeZone) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		body.put("email", email);
		body.put("timeZone", timeZone);
		return body;
	}

	private Map<String, Object> joinBody(String token, String email) {
		Map<String, Object> body = new HashMap<>();
		body.put("token", token);
		body.put("name", "Anna");
		body.put("email", email);
		body.put("timeZone", "Europe/Berlin");
		body.put("password", "password-1");
		return body;
	}

	private Map<String, Object> languageBody(Object nativeLanguage) {
		Map<String, Object> body = new HashMap<>();
		body.put("nativeLanguage", nativeLanguage);
		return body;
	}

	private String studentPath(UUID student) {
		return STUDENTS + "/" + student;
	}

	private String expiredLink(UUID teacher) {
		String token = createJoinLink(teacher);
		testClock().advance(Duration.ofDays(8));
		return token;
	}

	@Test
	@DisplayName("[student-enrolment/R13-S1] Teacher registers a student with a native language")
	void teacherRegistersAStudentWithANativeLanguage() {
		// given a logged-in teacher A
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers two students, the second with whitespace around the language
		Map<String, Object> marko = registration("Marko", "marko@example.com", "Europe/Belgrade");
		marko.put("nativeLanguage", "sr");
		Map<String, Object> oksana = registration("Оксана", "oksana@example.com", "Europe/Kyiv");
		oksana.put("nativeLanguage", " uk ");
		var markoResponse = teacher.post(STUDENTS, marko);
		var oksanaResponse = teacher.post(STUDENTS, oksana);

		// then both responses are 201
		markoResponse.then().statusCode(201);
		oksanaResponse.then().statusCode(201);
		// and the student details show the native languages
		teacher.get(studentPath(UUID.fromString(markoResponse.path("id")))).then().statusCode(200)
				.body("nativeLanguage", equalTo("sr"));
		teacher.get(studentPath(UUID.fromString(oksanaResponse.path("id")))).then().statusCode(200)
				.body("nativeLanguage", equalTo("uk"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S2] Registration needs a listed native language")
	void registrationNeedsAListedNativeLanguage() {
		// given a logged-in teacher A
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers students with no native language, null, "", "  ", "xx", "SR" and "srp"
		var responses = new ArrayList<Response>();
		responses.add(teacher.post(STUDENTS, registration("Anna", "anna0@example.com", "Europe/Berlin")));
		List<String> values = Arrays.asList(null, "", "  ", "xx", "SR", "srp");
		for (int i = 0; i < values.size(); i++) {
			Map<String, Object> body = registration("Anna", "anna" + (i + 1) + "@example.com", "Europe/Berlin");
			body.put("nativeLanguage", values.get(i));
			responses.add(teacher.post(STUDENTS, body));
		}

		// then the first four responses are 400 REQUIRED and the others INVALID_FORMAT
		for (int i = 0; i < responses.size(); i++) {
			String code = i < 4 ? "REQUIRED" : "INVALID_FORMAT";
			responses.get(i).then().statusCode(400)
					.body("code", equalTo("VALIDATION_FAILED"))
					.body("errors", equalTo(List.of(error("nativeLanguage", code))));
		}
		// and no body contains the rejected values
		assertFalse(responses.get(4).asString().contains("xx"));
		assertFalse(responses.get(6).asString().contains("srp"));
		// and no account is created
		assertEquals(1, countRows("users"));
		assertEquals(0, countRows("teacher_students"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S3] All failing fields are reported together")
	void allFailingFieldsAreReportedTogether() {
		// given a logged-in teacher A
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A registers a student with a blank name and the native language "xx"
		Map<String, Object> body = registration(" ", "anna@example.com", "Europe/Berlin");
		body.put("nativeLanguage", "xx");
		var response = teacher.post(STUDENTS, body);

		// then the response is 400 with both errors, in field order
		response.then().statusCode(400)
				.body("errors", equalTo(List.of(
						error("name", "REQUIRED"),
						error("nativeLanguage", "INVALID_FORMAT"))));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S4] An existing student keeps their language")
	void anExistingStudentKeepsTheirLanguage() {
		// given a logged-in teacher A, and a student with no teacher and no password, email "anna@example.com" and native language "de"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("a@example.com");
		UUID student = createStudent("anna@example.com", null, "de");

		// when teacher A registers a student with that email and the native language "fr"
		Map<String, Object> body = registration("Anna", "anna@example.com", "Europe/Berlin");
		body.put("nativeLanguage", "fr");
		var response = teacher.post(STUDENTS, body);

		// then the response is 201
		response.then().statusCode(201);
		// and the student is assigned to teacher A, with the native language "de"
		assertEquals(1, jdbc.sql("""
				select count(*) from teacher_students
				where teacher_id = :teacher and student_id = :student
				""")
				.param("teacher", teacherA)
				.param("student", student)
				.query(Long.class)
				.single());
		teacher.get(studentPath(student)).then().statusCode(200).body("nativeLanguage", equalTo("de"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S5] A visitor joins with a native language")
	void aVisitorJoinsWithANativeLanguage() {
		// given teacher A with a join link, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient visitor = newClient();

		// when the client joins through the link with the native language "pl"
		Map<String, Object> body = joinBody(token, "kasia@example.com");
		body.put("name", "Kasia");
		body.put("timeZone", "Europe/Warsaw");
		body.put("nativeLanguage", "pl");
		var response = visitor.post(JOIN, body);

		// then the response is 201
		response.then().statusCode(201);
		// and the student details show the native language "pl"
		teacher.get(studentPath(UUID.fromString(response.path("studentId")))).then().statusCode(200)
				.body("nativeLanguage", equalTo("pl"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S6] A visitor must give a listed native language")
	void aVisitorMustGiveAListedNativeLanguage() {
		// given teacher B with an expired join link, teacher A with an active join link, and a client with a CSRF cookie and no session
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String expired = expiredLink(teacherB);
		String active = createJoinLink(teacherA);
		TestClient visitor = newClient();

		// when the client joins through the active link with no native language, then with "xx", then through the expired link with no native language
		var missing = visitor.post(JOIN, joinBody(active, "anna@example.com"));
		Map<String, Object> unlisted = joinBody(active, "anna@example.com");
		unlisted.put("nativeLanguage", "xx");
		var invalid = visitor.post(JOIN, unlisted);
		var viaExpired = visitor.post(JOIN, joinBody(expired, "anna@example.com"));

		// then the responses are 400 with the nativeLanguage errors
		missing.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "REQUIRED"))));
		invalid.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_FORMAT"))));
		viaExpired.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "REQUIRED"))));
		// and no account is created
		assertEquals(2, countRows("users"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S7] A logged-in student keeps their language when joining")
	void aLoggedInStudentKeepsTheirLanguageWhenJoining() {
		// given teacher A with a join link, and a logged-in student with no teacher and native language "it"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");
		UUID student = createStudent("student@example.com", "student-pass-1", "it");
		TestClient studentClient = loggedInAs("student@example.com");

		// when the student joins through the link, sending the native language "es"
		Map<String, Object> body = joinBody(token, "student@example.com");
		body.put("nativeLanguage", "es");
		var response = studentClient.post(JOIN, body);

		// then the response is 200
		response.then().statusCode(200);
		// and the student details show the native language "it"
		teacher.get(studentPath(student)).then().statusCode(200).body("nativeLanguage", equalTo("it"));
	}

	@Test
	@DisplayName("[student-enrolment/R13-S8] A wrong JSON type is reported")
	void aWrongJsonTypeIsReported() {
		// given a logged-in teacher A, teacher A's join link, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient visitor = newClient();

		// when teacher A registers a student with the native language ["sr"], and the client joins with {"code": "sr"}
		Map<String, Object> registrationBody = registration("Anna", "anna@example.com", "Europe/Berlin");
		registrationBody.put("nativeLanguage", List.of("sr"));
		var registered = teacher.post(STUDENTS, registrationBody);
		Map<String, Object> joinBody = joinBody(token, "anna@example.com");
		joinBody.put("nativeLanguage", Map.of("code", "sr"));
		var joined = visitor.post(JOIN, joinBody);

		// then both responses are 400 with INVALID_TYPE
		registered.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_TYPE"))));
		joined.then().statusCode(400)
				.body("code", equalTo("VALIDATION_FAILED"))
				.body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_TYPE"))));
		// and no account is created
		assertEquals(1, countRows("users"));
	}

	@Test
	@DisplayName("[student-enrolment/R14-S1] Teacher changes the native language")
	void teacherChangesTheNativeLanguage() {
		// given teacher A with a student "Анна" whose native language is "ru"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Анна", "anna@example.com", "student-pass-1", true, "ru");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A sets the native language to "uk", sending the name "Anna" in the same request
		Map<String, Object> body = languageBody("uk");
		body.put("name", "Anna");
		var response = teacher.patch(studentPath(student), body);

		// then the response is 200 with the student's details
		response.then().statusCode(200)
				.body("id", equalTo(student.toString()))
				.body("name", equalTo("Анна"))
				.body("email", equalTo("anna@example.com"))
				.body("nativeLanguage", equalTo("uk"));
		// and the student details show "uk" and the name "Анна"
		teacher.get(studentPath(student)).then().statusCode(200)
				.body("nativeLanguage", equalTo("uk"))
				.body("name", equalTo("Анна"));
	}

	@Test
	@DisplayName("[student-enrolment/R14-S2] Invalid values change nothing")
	void invalidValuesChangeNothing() {
		// given teacher A with a student whose native language is "ru"
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("anna@example.com", "student-pass-1", "ru");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A sets the native language to "xx", to null, to ["uk"], and sends the body {}
		var unlisted = teacher.patch(studentPath(student), languageBody("xx"));
		var nullValue = teacher.patch(studentPath(student), languageBody(null));
		var array = teacher.patch(studentPath(student), languageBody(List.of("uk")));
		var empty = teacher.patch(studentPath(student), Map.of());

		// then the responses are 400 with the nativeLanguage errors
		unlisted.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_FORMAT"))));
		nullValue.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "REQUIRED"))));
		array.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "INVALID_TYPE"))));
		empty.then().statusCode(400).body("errors", equalTo(List.of(error("nativeLanguage", "REQUIRED"))));
		// and the native language is still "ru"
		assertEquals("ru", studentLanguage(student));
	}

	@Test
	@DisplayName("[student-enrolment/R14-S3] Only the teacher's own students")
	void onlyTheTeachersOwnStudents() {
		// given teacher A, teacher B with a student S whose native language is "ru", a student with no teacher, and teacher A logged in
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID studentS = createStudent("s@example.com", "student-pass-1", "ru");
		assignStudent(teacherB, studentS);
		UUID unassigned = createStudent("free@example.com", "student-pass-1");
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A sets the native language of S, of the unassigned student, of an unknown id and of "not-a-uuid" to "de", of "not-a-uuid" to "xx", and of S to "xx"
		var foreign = teacher.patch(studentPath(studentS), languageBody("de"));
		var free = teacher.patch(studentPath(unassigned), languageBody("de"));
		var unknown = teacher.patch(studentPath(UUID.randomUUID()), languageBody("de"));
		var malformed = teacher.patch(STUDENTS + "/not-a-uuid", languageBody("de"));
		var malformedInvalid = teacher.patch(STUDENTS + "/not-a-uuid", languageBody("xx"));
		var foreignInvalid = teacher.patch(studentPath(studentS), languageBody("xx"));

		// then the first five responses are 404 NOT_FOUND with no errors
		for (Response response : List.of(foreign, free, unknown, malformed, malformedInvalid)) {
			response.then().statusCode(404)
					.body("code", equalTo("NOT_FOUND"))
					.body("$", not(hasKey("errors")));
		}
		// and the last is 400 VALIDATION_FAILED
		foreignInvalid.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and S's native language is still "ru"
		assertEquals("ru", studentLanguage(studentS));
	}

	@Test
	@DisplayName("[student-enrolment/R14-S4] Only teachers change it")
	void onlyTeachersChangeIt() {
		// given teacher A with a student S, a logged-in admin, S logged in, a client with a CSRF cookie and no session, and a logged-in teacher B who is then deactivated
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID studentS = createStudent("s@example.com", "student-pass-1", "ru");
		assignStudent(teacherA, studentS);
		TestClient teacherAClient = loggedInAs("a@example.com").withoutCsrfHeader();
		TestClient admin = loggedInAdmin();
		TestClient student = loggedInAs("s@example.com");
		TestClient anonymous = newClient();
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherBClient = loggedInAs("b@example.com");
		admin.post("/api/admin/teachers/" + teacherB + "/deactivate").then().statusCode(204);

		// when the admin, S, the client and teacher B each set S's native language to "de", and teacher A does so without a CSRF token
		var asAdmin = admin.patch(studentPath(studentS), languageBody("de"));
		var asStudent = student.patch(studentPath(studentS), languageBody("de"));
		var asAnonymous = anonymous.patch(studentPath(studentS), languageBody("de"));
		var asDeactivated = teacherBClient.patch(studentPath(studentS), languageBody("de"));
		var withoutCsrf = teacherAClient.patch(studentPath(studentS), languageBody("de"));

		// then the responses carry the expected status and code
		asAdmin.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		asStudent.then().statusCode(403).body("code", equalTo("FORBIDDEN"));
		asAnonymous.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		asDeactivated.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		withoutCsrf.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and S's native language is unchanged
		assertEquals("ru", studentLanguage(studentS));
	}

	@Test
	@DisplayName("[student-enrolment/R15-S1] The teacher sees it")
	void theTeacherSeesIt() {
		// given teacher A with students "Анна" (native language "ru") and "Ben" (native language "en")
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID anna = createUser("STUDENT", "Анна", "anna@example.com", "student-pass-1", true, "ru");
		UUID ben = createUser("STUDENT", "Ben", "ben@example.com", "student-pass-1", true, "en");
		assignStudent(teacherA, anna);
		assignStudent(teacherA, ben);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A lists students and reads Анна's details
		var list = teacher.get(STUDENTS);
		var details = teacher.get(studentPath(anna));

		// then the list shows the native languages and the details show "ru"
		list.then().statusCode(200)
				.body("find { it.id == '" + anna + "' }.nativeLanguage", equalTo("ru"))
				.body("find { it.id == '" + ben + "' }.nativeLanguage", equalTo("en"));
		details.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
	}

	@Test
	@DisplayName("[student-enrolment/R15-S2] The admin sees it on students only")
	void theAdminSeesItOnStudentsOnly() {
		// given a logged-in admin, and teacher A with native language "fr" and student S, email "s@example.com", native language "pt"
		TestClient admin = loggedInAdmin();
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1", "fr");
		UUID student = createStudent("s@example.com", "student-pass-1", "pt");
		assignStudent(teacherA, student);

		// when the admin lists teacher A's students, looks up "s@example.com" and lists S's teachers
		var students = admin.get("/api/admin/teachers/" + teacherA + "/students");
		var lookup = admin.post("/api/admin/students/lookup", Map.of("email", "s@example.com"));
		var teachers = admin.get("/api/admin/students/" + student + "/teachers");

		// then the first two responses show "pt" for S
		students.then().statusCode(200)
				.body("find { it.id == '" + student + "' }.nativeLanguage", equalTo("pt"));
		lookup.then().statusCode(200).body("nativeLanguage", equalTo("pt"));
		// and the entry for teacher A in the third shows "fr"
		teachers.then().statusCode(200)
				.body("find { it.id == '" + teacherA + "' }.nativeLanguage", equalTo("fr"));
	}

	@Test
	@DisplayName("[student-enrolment/R15-S3] The student sees it and cannot change it")
	void theStudentSeesItAndCannotChangeIt() {
		// given a logged-in student with the name "Анна" and native language "ru", and a logged-in teacher with native language "fr"
		createUser("STUDENT", "Анна", "anna@example.com", "student-pass-1", true, "ru");
		createTeacher("a@example.com", "teacher-pass-1", "fr");
		TestClient teacher = loggedInAs("a@example.com");
		TestClient student = newClient();

		// when the student logs in, reads the current user, sets the name "Anna" with the native language "de", and reads the current user again; and the teacher reads the current user
		var login = student.login("anna@example.com", "student-pass-1");
		var firstRead = student.get("/api/me");
		var update = student.patch("/api/me",
				Map.of("name", "Anna", "timeZone", "UTC", "nativeLanguage", "de"));
		var secondRead = student.get("/api/me");
		var teacherRead = teacher.get("/api/me");

		// then the student's responses show "ru"
		login.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		firstRead.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		update.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		secondRead.then().statusCode(200).body("nativeLanguage", equalTo("ru"));
		// and the second read shows the name "Anna"
		secondRead.then().body("name", equalTo("Anna"));
		// and the teacher's response shows "fr"
		teacherRead.then().statusCode(200).body("nativeLanguage", equalTo("fr"));
	}

	@Test
	@DisplayName("[student-enrolment/R15-S4] Setting a password returns it")
	void settingAPasswordReturnsIt() {
		// given teacher A with a student with no password, native language "sr" and a valid set-password link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("anna@example.com", null, "sr");
		assignStudent(teacherA, student);
		String token = createToken(student, "SET_PASSWORD", false, teacherA);

		// when the student sets the password "password-1" through the link
		var response = newClient().post("/api/password-links", Map.of("token", token, "password", "password-1"));

		// then the response shows the native language "sr"
		response.then().statusCode(200).body("nativeLanguage", equalTo("sr"));
	}

	@Test
	@DisplayName("[student-enrolment/R15-S5] A teacher's login and password link return it, an admin's data does not")
	void aTeachersLoginAndPasswordLinkReturnItAnAdminsDataDoesNot() {
		// given a teacher with native language "es", no password and a valid set-password link, and a logged-in admin
		UUID admin = createAdmin("admin@example.com", "admin-password-1");
		UUID teacher = createTeacher("t@example.com", null, "es");
		String token = createToken(teacher, "SET_PASSWORD", false, admin);

		// when the teacher sets the password "password-1" through the link and then logs in with it, and the admin logs in and reads the current user
		var setPassword = newClient().post("/api/password-links", Map.of("token", token, "password", "password-1"));
		var teacherLogin = newClient().login("t@example.com", "password-1");
		TestClient adminClient = newClient();
		var adminLogin = adminClient.login("admin@example.com", "admin-password-1");
		var adminRead = adminClient.get("/api/me");

		// then both of the teacher's responses show "es"
		setPassword.then().statusCode(200).body("nativeLanguage", equalTo("es"));
		teacherLogin.then().statusCode(200).body("nativeLanguage", equalTo("es"));
		// and the admin's responses have no nativeLanguage property
		adminLogin.then().statusCode(200).body("$", not(hasKey("nativeLanguage")));
		adminRead.then().statusCode(200).body("$", not(hasKey("nativeLanguage")));
	}
}
