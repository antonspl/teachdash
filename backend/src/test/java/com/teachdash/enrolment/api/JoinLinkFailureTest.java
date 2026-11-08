package com.teachdash.enrolment.api;

import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.enrolment.persistence.TeacherStudentRepository;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** The spy is reset by Spring after each test, so it does not reach other test classes. */
class JoinLinkFailureTest extends ApiTest {

	@MockitoSpyBean
	private TeacherStudentRepository assignments;

	@Test
	@DisplayName("[student-enrolment/R9-S9] Failed join leaves nothing behind")
	void failedJoinLeavesNothingBehind() {
		// given teacher A with a join link and a failure while the assignment is stored
		UUID teacherA = createTeacher("a@example.com", "teacher-pass-1");
		String token = createJoinLink(teacherA);
		TestClient teacher = loggedInAs("a@example.com");
		doThrow(new IllegalStateException("assignment failure")).when(assignments).insert(any(), any(), any());
		long usersBefore = jdbc.sql("select count(*) from users").query(Long.class).single();
		long sessionsBefore = jdbc.sql("select count(*) from spring_session").query(Long.class).single();
		TestClient visitor = newClient();

		// when a client with no session joins through the link with a new email
		var response = visitor.post("/api/join-links/join", Map.of(
				"token", token,
				"name", "Nina",
				"email", "nina@example.com",
				"timeZone", "UTC",
				"password", "new-password-1"));

		// then the response is 500
		response.then().statusCode(500);
		// and no account is created
		assertEquals(usersBefore, jdbc.sql("select count(*) from users").query(Long.class).single());
		// and no session is started
		assertNull(visitor.cookie("SESSION"));
		assertEquals(sessionsBefore, jdbc.sql("select count(*) from spring_session").query(Long.class).single());
		// and teacher A's list is empty
		teacher.get("/api/teacher/students").then().statusCode(200).body("$", empty());
	}
}
