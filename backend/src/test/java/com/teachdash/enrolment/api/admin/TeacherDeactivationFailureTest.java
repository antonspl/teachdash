package com.teachdash.enrolment.api.admin;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** The spy is reset by Spring after each test, so it does not reach other test classes. */
class TeacherDeactivationFailureTest extends ApiTest {

	@MockitoSpyBean
	private PasswordLinkService links;

	@Test
	@DisplayName("[student-enrolment/R7-S4] Failed release keeps the teacher active")
	void failedReleaseKeepsTheTeacherActive() {
		// given teacher A with a password and a student, and a failure while the students are released
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		UUID student = createStudent("student@example.com", "student-pass-1");
		assignStudent(teacherA, student);
		TestClient admin = loggedInAdmin();
		TestClient teacher = loggedInAs("a@example.com");
		doThrow(new IllegalStateException("release failure")).when(links).invalidateLinks(any(), any());

		// when the admin deactivates teacher A
		var response = admin.post("/api/admin/teachers/" + teacherA + "/deactivate");

		// then the response is 500
		response.then().statusCode(500);
		// and teacher A can log in
		newClient().login("a@example.com", "teacher-pass-1").then().statusCode(200);
		// and the student is still in teacher A's list
		teacher.get("/api/teacher/students").then().statusCode(200).body("id", contains(student.toString()));
	}
}
