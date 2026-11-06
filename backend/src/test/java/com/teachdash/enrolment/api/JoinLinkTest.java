package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.Parallel;
import com.teachdash.support.TestClient;

/** R9. */
class JoinLinkTest extends ApiTest {

	private static final String PREVIEW = "/api/join-links/preview";
	private static final String JOIN = "/api/join-links/join";
	private static final String STUDENTS = "/api/teacher/students";

	private Map<String, Object> joinBody(String token, String email) {
		Map<String, Object> body = new HashMap<>();
		body.put("token", token);
		body.put("name", "Nina");
		body.put("email", email);
		body.put("timeZone", "UTC");
		body.put("password", "new-password-1");
		return body;
	}

	private Response preview(TestClient client, String token) {
		return client.post(PREVIEW, Map.of("token", token));
	}

	private long userCount() {
		return jdbc.sql("select count(*) from users").query(Long.class).single();
	}

	private long countByEmail(String email) {
		return jdbc.sql("select count(*) from users where lower(email) = lower(:email)")
				.param("email", email)
				.query(Long.class)
				.single();
	}

	private long assignmentCount() {
		return jdbc.sql("select count(*) from teacher_students").query(Long.class).single();
	}

	@Test
	@DisplayName("[student-enrolment/R9-S1] Visitor registers and is assigned")
	void visitorRegistersAndIsAssigned() {
		// given teacher A with a join link, and a client with a CSRF cookie and no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");
		TestClient visitor = newClient();
		String oldCsrf = visitor.cookie("XSRF-TOKEN");

		// when the client joins through the link with a new email and the password "new-password-1", and then sends a state-changing request with the CSRF token from the join response
		var join = visitor.post(JOIN, joinBody(token, "nina@example.com"));
		var nextRequest = visitor.patch("/api/me", Map.of("name", "Changed Name", "timeZone", "UTC"));

		// then the join response is 201 with the student's id
		join.then().statusCode(201).body("studentId", notNullValue());
		String studentId = join.path("studentId");
		// and a session is started for the student
		assertNotNull(join.cookie("SESSION"));
		// and the response sets a new XSRF-TOKEN cookie different from the old one
		assertNotNull(join.cookie("XSRF-TOKEN"));
		assertNotEquals(oldCsrf, join.cookie("XSRF-TOKEN"));
		// and the next request is not rejected for CSRF
		nextRequest.then().statusCode(200);
		// and the student is in teacher A's list, and teacher A is the student's only teacher
		teacher.get(STUDENTS).then().statusCode(200).body("id", contains(studentId));
		visitor.get("/api/student/teachers").then().statusCode(200).body("id", contains(teacherA.toString()));
		// and the student logs in with "new-password-1" and has the role student
		TestClient loggedIn = newClient();
		loggedIn.login("nina@example.com", "new-password-1").then().statusCode(200);
		loggedIn.get("/api/me").then().statusCode(200).body("role", equalTo("STUDENT"));
		// and the student has no unused set-password link
		assertEquals(0, jdbc.sql("select count(*) from one_time_tokens where user_id = :id and used_at is null")
				.param("id", UUID.fromString(studentId))
				.query(Long.class)
				.single());
	}

	@Test
	@DisplayName("[student-enrolment/R9-S2] Preview shows the teacher's name")
	void previewShowsTheTeachersName() {
		// given teacher A with the name "Мария" and a join link, and a client with no session
		UUID teacherA = createUser("TEACHER", "Мария", "maria@example.com", "teacher-pass-1", true);
		String token = createJoinLink(teacherA);

		// when the client requests the preview of the link
		var response = preview(newClient(), token);

		// then the response is 200 with the teacher's name "Мария" and no other details of the teacher
		response.then().statusCode(200).body("teacherName", equalTo("Мария"));
		assertEquals(List.of("teacherName"), new ArrayList<>(response.<Map<String, Object>>path("$").keySet()));
	}

	@Test
	@DisplayName("[student-enrolment/R9-S3] Invalid links get one answer")
	void invalidLinksGetOneAnswer() {
		// given a link with an unknown token, a link replaced by a newer one, a disabled link, and the link of a teacher who was then deactivated
		String unknown = "unknown-token-value";
		UUID replacedTeacher = createTeacher("replaced@example.com", "teacher-pass-1");
		String replaced = createJoinLink(replacedTeacher);
		jdbc.sql("delete from teacher_join_links where teacher_id = :id").param("id", replacedTeacher).update();
		createJoinLink(replacedTeacher);
		UUID disabledTeacher = createTeacher("disabled@example.com", "teacher-pass-1");
		String disabled = createJoinLink(disabledTeacher);
		jdbc.sql("delete from teacher_join_links where teacher_id = :id").param("id", disabledTeacher).update();
		UUID deactivatedTeacher = createTeacher("deactivated@example.com", "teacher-pass-1");
		String deactivated = createJoinLink(deactivatedTeacher);
		loggedInAdmin().post("/api/admin/teachers/" + deactivatedTeacher + "/deactivate").then().statusCode(204);
		List<String> tokens = List.of(unknown, replaced, disabled, deactivated);
		long usersBefore = userCount();

		// when a client with no session requests the preview of each and joins through each with a new email
		List<Response> previews = new ArrayList<>();
		List<Response> joins = new ArrayList<>();
		for (String token : tokens) {
			previews.add(preview(newClient(), token));
			joins.add(newClient().post(JOIN, joinBody(token, "nina@example.com")));
		}

		// then every response is 410 with code LINK_INVALID
		for (Response response : previews) {
			response.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		}
		for (Response response : joins) {
			response.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		}
		// and all preview responses have the same body and all join responses have the same body
		for (Response response : previews) {
			assertEquals(previews.get(0).asString(), response.asString());
		}
		for (Response response : joins) {
			assertEquals(joins.get(0).asString(), response.asString());
		}
		// and no account is created
		assertEquals(usersBefore, userCount());
	}

	@Test
	@DisplayName("[student-enrolment/R9-S4] Link is checked before the email")
	void linkIsCheckedBeforeTheEmail() {
		// given a student account with email "anna@example.com" and an unknown link token
		createStudent("anna@example.com", "student-pass-1");

		// when a client with no session joins through the unknown link with email "anna@example.com"
		var response = newClient().post(JOIN, joinBody("unknown-token-value", "anna@example.com"));

		// then the response is 410 with code LINK_INVALID
		response.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
	}

	@Test
	@DisplayName("[student-enrolment/R9-S5] Email that has an account is rejected")
	void emailThatHasAnAccountIsRejected() {
		// given teacher A with a join link, a student with email "anna@example.com" and no teacher, a teacher with email "t@example.com", and an admin with email "a@example.com"
		UUID teacherA = createTeacher("teacher@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		UUID student = createStudent("anna@example.com", "student-pass-1");
		createTeacher("t@example.com", "teacher-pass-1");
		createAdmin("a@example.com", "admin-password-1");
		long usersBefore = userCount();
		String studentBefore = studentState(student);

		// when a client with no session joins through the link with emails " ANNA@example.com ", "t@example.com" and "a@example.com"
		Map<String, Object> first = joinBody(token, " ANNA@example.com ");
		first.put("name", "Other Name");
		first.put("timeZone", "Asia/Tokyo");
		first.put("password", "other-password-1");
		var withStudentEmail = newClient().post(JOIN, first);
		var withTeacherEmail = newClient().post(JOIN, joinBody(token, "t@example.com"));
		var withAdminEmail = newClient().post(JOIN, joinBody(token, "a@example.com"));

		// then each response is 409 with code EMAIL_TAKEN
		withStudentEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		withTeacherEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		withAdminEmail.then().statusCode(409).body("code", equalTo("EMAIL_TAKEN"));
		// and no account is created, and nothing is assigned to teacher A
		assertEquals(usersBefore, userCount());
		assertEquals(0, assignmentCount());
		// and the student still has no teacher and keeps their name, time zone and password
		assertEquals(studentBefore, studentState(student));
	}

	private String studentState(UUID id) {
		return jdbc.sql("select name || '|' || time_zone || '|' || password_hash from users where id = :id")
				.param("id", id)
				.query(String.class)
				.single();
	}

	@Test
	@DisplayName("[student-enrolment/R9-S6] Invalid input is rejected")
	void invalidInputIsRejected() {
		// given teacher A with a join link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		long usersBefore = userCount();
		List<Map<String, Object>> bodies = new ArrayList<>();
		bodies.add(with(joinBody(token, "nina@example.com"), "name", "   "));
		bodies.add(with(joinBody(token, "not-an-email"), "name", "Nina"));
		bodies.add(with(joinBody(token, "nina@example.com"), "timeZone", "Mars/Olympus"));
		bodies.add(without(joinBody(token, "nina@example.com"), "timeZone"));
		bodies.add(with(joinBody(token, "nina@example.com"), "name", "a".repeat(201)));
		bodies.add(joinBody(token, "a".repeat(309) + "@example.com"));
		bodies.add(with(joinBody(token, "nina@example.com"), "password", "short77"));
		bodies.add(with(joinBody(token, "nina@example.com"), "password", "a".repeat(73)));
		bodies.add(joinBody("  ", "nina@example.com"));
		bodies.add(joinBody("t".repeat(65), "nina@example.com"));

		// when a client with no session joins through the link with each of these bodies
		List<Response> responses = new ArrayList<>();
		for (Map<String, Object> body : bodies) {
			responses.add(newClient().post(JOIN, body));
		}

		// then each response is 400 with code VALIDATION_FAILED
		for (int i = 0; i < responses.size(); i++) {
			responses.get(i).then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		}
		// and no account is created
		assertEquals(usersBefore, userCount());
	}

	private static Map<String, Object> with(Map<String, Object> body, String key, Object value) {
		body.put(key, value);
		return body;
	}

	private static Map<String, Object> without(Map<String, Object> body, String key) {
		body.remove(key);
		return body;
	}

	@Test
	@DisplayName("[student-enrolment/R9-S7] Submitted values are stored")
	void submittedValuesAreStored() {
		// given teacher A with a join link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");

		// when a client with no session joins through the link with name " Анна ", email " Anna@Example.com " and time zone "Europe/Moscow"
		Map<String, Object> body = joinBody(token, " Anna@Example.com ");
		body.put("name", " Анна ");
		body.put("timeZone", "Europe/Moscow");
		var join = newClient().post(JOIN, body);
		var details = teacher.get(STUDENTS + "/" + join.path("studentId"));

		// then the response is 201
		join.then().statusCode(201);
		// and teacher A's request for the student's details returns the trimmed name, the email and the time zone
		details.then().statusCode(200)
				.body("name", equalTo("Анна"))
				.body("email", equalTo("Anna@Example.com"))
				.body("timeZone", equalTo("Europe/Moscow"));
	}

	@Test
	@DisplayName("[student-enrolment/R9-S8] Request without a CSRF token is rejected")
	void requestWithoutACsrfTokenIsRejected() {
		// given teacher A with a join link and a client with no session
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient withoutCsrf = newClient().withoutCsrfHeader();

		// when the client requests the preview and joins through the link with a new email, both without a CSRF token, and then joins again with the token
		var previewResponse = preview(withoutCsrf, token);
		var joinResponse = withoutCsrf.post(JOIN, joinBody(token, "nina@example.com"));
		var joinWithToken = newClient().post(JOIN, joinBody(token, "nina@example.com"));

		// then the first two responses are 403 with code CSRF_INVALID
		previewResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		joinResponse.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and the join with the token is 201
		joinWithToken.then().statusCode(201);
		// and there is exactly one account with that email
		assertEquals(1, countByEmail("nina@example.com"));
	}

	@Test
	@DisplayName("[student-enrolment/R9-S10] Parallel joins create one account")
	void parallelJoinsCreateOneAccount() throws Exception {
		// given teachers A and B, each with a join link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		String tokenA = createJoinLink(teacherA);
		String tokenB = createJoinLink(teacherB);
		int requests = 8;
		List<Callable<Response>> calls = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			TestClient client = newClient();
			String token = i % 2 == 0 ? tokenA : tokenB;
			calls.add(() -> client.post(JOIN, joinBody(token, "nina@example.com")));
		}

		// when clients send several parallel join requests with the same new email through both links
		List<Response> results = Parallel.run(calls);

		// then exactly one response is 201, all others are 409 with code EMAIL_TAKEN
		List<Integer> codes = results.stream().map(Response::statusCode).toList();
		assertEquals(1, codes.stream().filter(code -> code == 201).count(), codes.toString());
		for (Response response : results) {
			if (response.statusCode() != 201) {
				assertEquals(409, response.statusCode(), codes.toString());
				assertEquals("EMAIL_TAKEN", response.path("code"), codes.toString());
			}
		}
		// and there is exactly one account with that email and one assignment
		assertEquals(1, countByEmail("nina@example.com"));
		assertEquals(1, assignmentCount());
	}

	@Test
	@DisplayName("[student-enrolment/R9-S11] Teachers and admins cannot join")
	void teachersAndAdminsCannotJoin() {
		// given teacher A with a join link, a logged-in teacher B and a logged-in admin
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		createTeacher("b@example.com", "teacher-pass-1");
		TestClient teacherB = loggedInAs("b@example.com");
		TestClient admin = loggedInAdmin();
		TestClient teacherA2 = loggedInAs("a@example.com");
		long usersBefore = userCount();

		// when teacher B and the admin each request the preview and join through the link, with and without a new email
		List<Response> responses = new ArrayList<>();
		for (TestClient client : List.of(teacherB, admin)) {
			responses.add(preview(client, token));
			responses.add(client.post(JOIN, joinBody(token, "nina@example.com")));
			responses.add(client.post(JOIN, Map.of("token", token)));
		}

		// then all responses are 403
		for (Response response : responses) {
			response.then().statusCode(403);
		}
		// and no account is created, and teacher A's list is empty
		assertEquals(usersBefore, userCount());
		teacherA2.get(STUDENTS).then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R9-S12] Checks run in a fixed order")
	void checksRunInAFixedOrder() {
		// given a logged-in teacher, a student account with email "anna@example.com", and an unknown link token
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		createStudent("anna@example.com", "student-pass-1");
		String unknown = "unknown-token-value";

		// when the teacher joins through the unknown link with an invalid email, and a client with no session joins through the unknown link with an invalid email, then with email "anna@example.com"
		var asTeacher = teacher.post(JOIN, joinBody(unknown, "not-an-email"));
		var invalidEmail = newClient().post(JOIN, joinBody(unknown, "not-an-email"));
		var existingEmail = newClient().post(JOIN, joinBody(unknown, "anna@example.com"));

		// then the teacher's response is 403
		asTeacher.then().statusCode(403);
		// and the second response is 400 with code VALIDATION_FAILED
		invalidEmail.then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
		// and the third is 410 with code LINK_INVALID
		existingEmail.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
	}
}
