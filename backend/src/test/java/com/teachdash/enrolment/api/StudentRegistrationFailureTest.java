package com.teachdash.enrolment.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.accounts.domain.PasswordLinkService;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** The spy is reset by Spring after each test, so it does not reach other test classes. */
class StudentRegistrationFailureTest extends ApiTest {

	@MockitoSpyBean
	private PasswordLinkService links;

	@Test
	@DisplayName("[student-enrolment/R1-S12] Failed registration leaves nothing behind")
	void failedRegistrationLeavesNothingBehind() {
		// given a logged-in teacher and a failure while the set-password link is issued
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		doThrow(new IllegalStateException("link failure")).when(links).issue(any(), any(), any());
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();

		// when the teacher registers a student with a new email
		var response = teacher.post("/api/teacher/students",
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"));

		// then the response is 500
		response.then().statusCode(500);
		// and neither an account nor an assignment is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
		assertEquals(0, jdbc.sql("select count(*) from teacher_students").query(Long.class).single());
	}

	@Test
	@DisplayName("[student-enrolment/R1-S17] Failed assignment leaves nothing behind")
	void failedAssignmentLeavesNothingBehind() {
		// given a logged-in teacher, a student without a password and with no teacher, and a failure while the set-password link is issued
		createTeacher("teacher@example.com", "teacher-pass-1");
		createUser("STUDENT", "Nina", "nina@example.com", null, true);
		TestClient teacher = loggedInAs("teacher@example.com");
		doThrow(new IllegalStateException("link failure")).when(links).issue(any(), any(), any());

		// when the teacher registers a student with that email
		var response = teacher.post("/api/teacher/students",
				Map.of("name", "Nina", "email", "nina@example.com", "timeZone", "UTC"));

		// then the response is 500
		response.then().statusCode(500);
		// and the student is not in the teacher's list
		assertEquals(0, jdbc.sql("select count(*) from teacher_students").query(Long.class).single());
	}
}
