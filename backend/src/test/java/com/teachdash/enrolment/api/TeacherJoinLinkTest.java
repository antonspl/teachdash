package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R8. */
class TeacherJoinLinkTest extends ApiTest {

	private static final String LINK = "/api/teacher/join-link";
	private static final String PREVIEW = "/api/join-links/preview";
	private static final String JOIN = "/api/join-links/join";
	private static final String STUDENTS = "/api/teacher/students";

	private static String urlOf(String token) {
		return APP_BASE_URL + "/join#token=" + token;
	}

	/** A missing link gives a token that no link accepts, so the test fails at its THEN step. */
	private String tokenOf(Response response) {
		String link = response.path("link");
		return link == null ? "no-link" : tokenFrom(link);
	}

	private Response preview(String token) {
		return newClient().post(PREVIEW, Map.of("token", token));
	}

	private long linkCount() {
		return jdbc.sql("select count(*) from teacher_join_links").query(Long.class).single();
	}

	@Test
	@DisplayName("[student-enrolment/R8-S1] Teacher generates and views the link")
	void teacherGeneratesAndViewsTheLink() {
		// given a logged-in teacher with no join link
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher views the link, generates one, and views it again
		var firstView = teacher.get(LINK);
		var generated = teacher.post(LINK);
		var secondView = teacher.get(LINK);

		// then the first view is 200 with no link
		firstView.then().statusCode(200).body("link", nullValue());
		// and generating is 200 with a link that starts with the base URL and carries the token in the fragment
		generated.then().statusCode(200).body("link", startsWith(APP_BASE_URL + "/join#token="));
		// and the second view returns the same link
		secondView.then().statusCode(200).body("link", equalTo(generated.path("link")));
	}

	@Test
	@DisplayName("[student-enrolment/R8-S2] Generating again replaces the link")
	void generatingAgainReplacesTheLink() {
		// given a teacher with a join link L1
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		String tokenOne = createJoinLink(teacherId);
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher generates a new link L2, and the preview is requested for L1 and for L2
		var generated = teacher.post(LINK);
		var view = teacher.get(LINK);
		var previewOne = preview(tokenOne);
		var previewTwo = preview(tokenOf(generated));

		// then L2 differs from L1
		generated.then().statusCode(200).body("link", notNullValue());
		assertNotEquals(urlOf(tokenOne), generated.path("link"));
		// and the view returns L2
		view.then().statusCode(200).body("link", equalTo(generated.path("link")));
		// and the preview of L1 is 410 with code LINK_INVALID, and the preview of L2 is 200
		previewOne.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		previewTwo.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R8-S3] Disabled link stops working")
	void disabledLinkStopsWorking() {
		// given a teacher with a join link L1
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		String tokenOne = createJoinLink(teacherId);
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the teacher disables the link twice, views it, requests the preview of L1, and then generates a new link L2
		var firstDisable = teacher.delete(LINK);
		var secondDisable = teacher.delete(LINK);
		var view = teacher.get(LINK);
		var previewOne = preview(tokenOne);
		var generated = teacher.post(LINK);
		var previewTwo = preview(tokenOf(generated));

		// then both disable responses are 204
		firstDisable.then().statusCode(204);
		secondDisable.then().statusCode(204);
		// and the view is 200 with no link
		view.then().statusCode(200).body("link", nullValue());
		// and the preview of L1 is 410 with code LINK_INVALID
		previewOne.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the preview of L2 is 200
		previewTwo.then().statusCode(200);
	}

	@Test
	@DisplayName("[student-enrolment/R8-S4] Each teacher has their own link")
	void eachTeacherHasTheirOwnLink() {
		// given teachers A and B, each with a join link
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID teacherB = createTeacher("b@example.com", "teacher-pass-1");
		String tokenA = createJoinLink(teacherA);
		String tokenB = createJoinLink(teacherB);
		TestClient a = loggedInAs("a@example.com");
		TestClient b = loggedInAs("b@example.com");

		// when each teacher views their link, and a visitor joins through teacher B's link
		var viewA = a.get(LINK);
		var viewB = b.get(LINK);
		var join = newClient().post(JOIN, Map.of(
				"token", tokenB,
				"name", "Nina",
				"email", "nina@example.com",
				"timeZone", "UTC",
				"password", "new-password-1"));

		// then the two links differ
		viewA.then().statusCode(200).body("link", equalTo(urlOf(tokenA)));
		viewB.then().statusCode(200).body("link", equalTo(urlOf(tokenB)));
		assertNotEquals(tokenA, tokenB);
		// and the new student is in teacher B's list and not in teacher A's
		join.then().statusCode(201);
		b.get(STUDENTS).then().statusCode(200).body("id", contains(join.<String>path("studentId")));
		a.get(STUDENTS).then().statusCode(200).body("$", empty());
	}

	@Test
	@DisplayName("[student-enrolment/R8-S5] Deactivation disables the link for good")
	void deactivationDisablesTheLinkForGood() {
		// given teacher A with a password and a join link L1
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String tokenOne = createJoinLink(teacherA);
		TestClient admin = loggedInAdmin();

		// when the admin deactivates teacher A, the preview of L1 is requested, the admin reactivates teacher A, teacher A logs in and views the link, and the preview of L1 is requested again
		admin.post("/api/admin/teachers/" + teacherA + "/deactivate").then().statusCode(204);
		var previewWhileInactive = preview(tokenOne);
		admin.post("/api/admin/teachers/" + teacherA + "/reactivate").then().statusCode(204);
		TestClient teacher = newClient();
		teacher.login("a@example.com", "teacher-pass-1").then().statusCode(200);
		var view = teacher.get(LINK);
		var previewAfterwards = preview(tokenOne);

		// then both previews are 410 with code LINK_INVALID
		previewWhileInactive.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		previewAfterwards.then().statusCode(410).body("code", equalTo("LINK_INVALID"));
		// and the view is 200 with no link
		view.then().statusCode(200).body("link", nullValue());
	}

	@Test
	@DisplayName("[student-enrolment/R8-S6] Students and admins cannot manage links")
	void studentsAndAdminsCannotManageLinks() {
		// given a logged-in student and a logged-in admin
		createStudent("student@example.com", "student-pass-1");
		TestClient student = loggedInAs("student@example.com");
		TestClient admin = loggedInAdmin();
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when each views, generates and disables a join link through the teacher endpoints
		var studentView = student.get(LINK);
		var studentGenerate = student.post(LINK);
		var studentDisable = student.delete(LINK);
		var adminView = admin.get(LINK);
		var adminGenerate = admin.post(LINK);
		var adminDisable = admin.delete(LINK);

		// then all responses are 403
		studentView.then().statusCode(403);
		studentGenerate.then().statusCode(403);
		studentDisable.then().statusCode(403);
		adminView.then().statusCode(403);
		adminGenerate.then().statusCode(403);
		adminDisable.then().statusCode(403);
		// and no link is created
		assertEquals(0, linkCount());
		// and a logged-in teacher's view of their link is then 200
		teacher.get(LINK).then().statusCode(200).body("link", nullValue());
	}

	@Test
	@DisplayName("[student-enrolment/R8-S7] Link endpoints require a session")
	void linkEndpointsRequireASession() {
		// given a client with a valid CSRF token and no session
		TestClient anonymous = newClient();
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the client views, generates and disables a join link
		var view = anonymous.get(LINK);
		var generate = anonymous.post(LINK);
		var disable = anonymous.delete(LINK);

		// then all responses are 401 with code UNAUTHENTICATED
		view.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		generate.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		disable.then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
		// and a logged-in teacher's view of their link is then 200
		teacher.get(LINK).then().statusCode(200).body("link", nullValue());
	}

	@Test
	@DisplayName("[student-enrolment/R8-S8] Request without a CSRF token is rejected")
	void requestWithoutACsrfTokenIsRejected() {
		// given a teacher with a join link L1
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		String tokenOne = createJoinLink(teacherId);
		TestClient teacher = loggedInAs("teacher@example.com").withoutCsrfHeader();

		// when the teacher generates a link without a CSRF token, then disables the link without a CSRF token
		var generate = teacher.post(LINK);
		var disable = teacher.delete(LINK);
		var view = teacher.get(LINK);
		var previewOne = preview(tokenOne);

		// then both responses are 403 with code CSRF_INVALID
		generate.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		disable.then().statusCode(403).body("code", equalTo("CSRF_INVALID"));
		// and the view still returns L1, and the preview of L1 is 200
		view.then().statusCode(200).body("link", equalTo(urlOf(tokenOne)));
		previewOne.then().statusCode(200);
	}
}
