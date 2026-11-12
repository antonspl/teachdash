package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;

class TeacherResetLinkTest extends ApiTest {

	private static final String PASSWORD_LINKS = "/api/password-links";

	private String resetLink(UUID student) {
		return "/api/teacher/students/" + student + "/reset-link";
	}

	private Response useLink(String token, String password) {
		return newClient().post(PASSWORD_LINKS, Map.of("token", token, "password", password));
	}

	/** A made-up token when the response has no link, so a test fails in THEN, not here. */
	private String tokenOf(Response response) {
		String link = response.statusCode() == 201 ? response.path("link") : null;
		return link == null ? "missing-token" : tokenFrom(link);
	}

	private long countTokens(UUID userId, boolean unusedOnly) {
		return jdbc.sql("select count(*) from one_time_tokens where user_id = :id"
				+ (unusedOnly ? " and used_at is null" : ""))
				.param("id", userId)
				.query(Long.class)
				.single();
	}

	@Test
	@DisplayName("[student-enrolment/R12-S1] Teacher issues a reset link for own student")
	void teacherIssuesAResetLinkForOwnStudent() {
		// given a student with the password "old-password-1", assigned to teacher A, and logged in on another client
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "old-password-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient earlierSession = loggedInAs("student@example.com");

		// when teacher A requests a reset link for the student and the link is used to set the password "new-password-1"
		var issued = teacher.post(resetLink(student));
		var link = useLink(tokenOf(issued), "new-password-1");
		var oldLogin = newClient().login("student@example.com", "old-password-1");
		var earlier = earlierSession.get("/api/me");
		var newLogin = newClient().login("student@example.com", "new-password-1");

		// then the response to the request is 201 with a link that starts with the base URL and carries the token in the fragment
		issued.then().statusCode(201).body("link", startsWith(APP_BASE_URL + "/set-password#token="));
		// and the link response is 200
		link.then().statusCode(200);
		// and logging in with "old-password-1" returns 401, and the student's earlier session gets 401
		oldLogin.then().statusCode(401);
		earlier.then().statusCode(401);
		// and the student logs in with "new-password-1"
		newLogin.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S2] Another teacher's student is not found")
	void anotherTeachersStudentIsNotFound() {
		// given a student without a password, assigned to teacher B, with an unused set-password link issued by teacher B
		createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", null, true);
		assignStudent(teacherB, student);
		String token = createToken(student, "SET_PASSWORD", false, teacherB);
		TestClient a = loggedInAs("a@example.com");

		// when teacher A requests a reset link for that student
		var response = a.post(resetLink(student));
		var link = useLink(token, "new-password-1");

		// then the response is 404 with code NOT_FOUND
		response.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		// and the set-password link still sets the password
		link.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S3] Ids that are not own students are not found")
	void idsThatAreNotOwnStudentsAreNotFound() {
		// given a logged-in teacher A, teacher B with an unused link, a student with no teacher and an unused link, and an id with no account
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		createTeacher("a@example.com", "teacher-pass-1");
		TestClient a = loggedInAs("a@example.com");
		UUID teacherB = createUser("TEACHER", "Teacher B", "b@example.com", null, true);
		createToken(teacherB, "SET_PASSWORD", false, adminId);
		UUID student = createUser("STUDENT", "Student", "student@example.com", null, true);
		createToken(student, "SET_PASSWORD", false, adminId);
		UUID unknown = UUID.randomUUID();

		// when teacher A requests a reset link for teacher B's id, the student's id, the id with no account and the id "not-a-uuid"
		var forTeacher = a.post(resetLink(teacherB));
		var forStudent = a.post(resetLink(student));
		var forUnknown = a.post(resetLink(unknown));
		var forMalformed = a.post("/api/teacher/students/not-a-uuid/reset-link");

		// then each of the four responses is 404 with code NOT_FOUND
		forTeacher.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		forStudent.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		forUnknown.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		forMalformed.then().statusCode(404).body("code", equalTo("NOT_FOUND"));
		// and the existing links of teacher B and the student are still unused, and no other link is stored for either
		assertEquals(1, countTokens(teacherB, true));
		assertEquals(1, countTokens(teacherB, false));
		assertEquals(1, countTokens(student, true));
		assertEquals(1, countTokens(student, false));
	}

	@Test
	@DisplayName("[student-enrolment/R12-S4] Student without a password gets a working link")
	void studentWithoutAPasswordGetsAWorkingLink() {
		// given a student without a password, assigned to teacher A, with an unused set-password link issued by teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createUser("STUDENT", "Student", "student@example.com", null, true);
		assignStudent(teacherA, student);
		String setPasswordToken = createToken(student, "SET_PASSWORD", false, teacherA);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests a reset link for the student, the set-password link is then used to set the password "old-link-1", and the reset link to set the password "new-password-1"
		var issued = teacher.post(resetLink(student));
		var setPasswordLink = useLink(setPasswordToken, "old-link-1");
		var link = useLink(tokenOf(issued), "new-password-1");
		var login = newClient().login("student@example.com", "new-password-1");

		// then the response to the request is 201 with a link
		issued.then().statusCode(201).body("link", startsWith(APP_BASE_URL));
		// and the set-password link response is 410 with code LINK_INVALID
		setPasswordLink.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the student logs in with "new-password-1"
		login.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S5] Teacher's link voids an admin's unused link")
	void teachersLinkVoidsAnAdminsUnusedLink() {
		// given a student with a password, assigned to teacher A, with an unused reset link issued by an admin
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		UUID adminId = createAdmin("admin@example.com", "admin-password-1");
		String adminToken = createToken(student, "RESET_PASSWORD", false, adminId);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests a reset link for the student, the admin's link is then used to set the password "admin-password-1", and the teacher's link to set the password "new-password-1"
		var issued = teacher.post(resetLink(student));
		var adminLink = useLink(adminToken, "admin-password-1");
		useLink(tokenOf(issued), "new-password-1");
		var login = newClient().login("student@example.com", "new-password-1");

		// then the admin's link response is 410 with code LINK_INVALID
		adminLink.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the student logs in with "new-password-1"
		login.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S6] Students and admins cannot issue through the teacher endpoint")
	void studentsAndAdminsCannotIssueThroughTheTeacherEndpoint() {
		// given a student assigned to teacher A, a logged-in student and a logged-in admin
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient studentClient = loggedInAs("student@example.com");
		TestClient admin = loggedInAdmin();

		// when the logged-in student and the admin each request a reset link for that student through the teacher endpoint
		var byStudent = studentClient.post(resetLink(student));
		var byAdmin = admin.post(resetLink(student));

		// then both responses are 403
		byStudent.then().statusCode(403);
		byAdmin.then().statusCode(403);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S7] Issuing requires a session")
	void issuingRequiresASession() {
		// given a student assigned to teacher A and a client with a valid CSRF token and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient anonymous = newClient();

		// when the client requests a reset link for that student
		var response = anonymous.post(resetLink(student));

		// then the response is 401 with code UNAUTHENTICATED
		response.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
	}

	@Test
	@DisplayName("[student-enrolment/R12-S8] Request without a CSRF token is rejected")
	void requestWithoutACsrfTokenIsRejected() {
		// given a student with the password "old-password-1", assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "old-password-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests a reset link for the student without a CSRF token
		var response = teacher.snapshot().withoutCsrfHeader().post(resetLink(student));
		var login = newClient().login("student@example.com", "old-password-1");

		// then the response is 403 with code CSRF_INVALID, and the student still logs in with "old-password-1"
		response.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		login.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R12-S9] Only the newest link works")
	void onlyTheNewestLinkWorks() {
		// given a student with a password, assigned to teacher A
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient teacher = loggedInAs("a@example.com");

		// when teacher A requests a reset link twice, the first link is then used to set the password "first-password-1", and the second to set the password "new-password-1"
		var first = teacher.post(resetLink(student));
		var second = teacher.post(resetLink(student));
		var firstLink = useLink(tokenOf(first), "first-password-1");
		useLink(tokenOf(second), "new-password-1");
		var login = newClient().login("student@example.com", "new-password-1");

		// then both requests return 201
		first.then().statusCode(201);
		second.then().statusCode(201);
		// and the first link response is 410 with code LINK_INVALID
		firstLink.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the student logs in with "new-password-1"
		login.then().statusCode(200);
	}
}
