package com.teachdash.enrolment.api.admin;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** The spy is reset by Spring after each test, so it does not reach other test classes. */
class TeacherDeactivationJoinLinkFailureTest extends ApiTest {

	@MockitoSpyBean
	private PasswordLinkService links;

	@Test
	@DisplayName("[student-enrolment/R8-S9] Failed deactivation keeps the link")
	void failedDeactivationKeepsTheLink() {
		// given teacher A with a password, a student and a join link L1, and a failure while the students are released
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		String token = createJoinLink(teacherA);
		TestClient admin = loggedInAdmin();
		TestClient teacher = loggedInAs("a@example.com");
		doThrow(new IllegalStateException("release failure")).when(links).invalidateLinks(any(), any());

		// when the admin deactivates teacher A, and the preview of L1 is requested
		var deactivation = admin.post("/api/admin/teachers/" + teacherA + "/deactivate");
		var preview = newClient().post("/api/join-links/preview", Map.of("token", token));

		// then the deactivation response is 500
		deactivation.then().statusCode(500);
		// and the preview of L1 is 200
		preview.then().statusCode(200);
		// and the student is still in teacher A's list
		teacher.get("/api/teacher/students").then().statusCode(200).body("id", contains(student.toString()));
	}
}
