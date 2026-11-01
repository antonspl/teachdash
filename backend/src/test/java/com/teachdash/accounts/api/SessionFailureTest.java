package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.teachdash.accounts.domain.Role;
import com.teachdash.accounts.domain.User;
import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import io.restassured.response.Response;

/** The session table is renamed to make the store fail, and always renamed back. */
class SessionFailureTest extends ApiTest {

	private static final String BROKEN_TABLE = "spring_session_broken";

	@MockitoSpyBean
	private UserRepository users;

	@AfterEach
	void restoreSessionTable() {
		jdbc.sql("alter table if exists " + BROKEN_TABLE + " rename to spring_session").update();
	}

	@Test
	@DisplayName("[accounts/R4-S9] The coded error survives a failure saving the session")
	void theCodedErrorSurvivesAFailureSavingTheSession() {
		// given a logged-in teacher
		UUID id = createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		doAnswer(invocation -> {
			breakSessionStore();
			throw new IllegalStateException("connection to db-host-7 refused");
		}).when(users).findById(id);

		// when the database fails during the session check, and saving the session fails too
		Response response = teacher.get("/api/me");

		// then the response is 500 with code INTERNAL_ERROR and no internal details in the body
		assertInternalError(response);
	}

	@Test
	@DisplayName("[accounts/R4-S10] A failure loading the session returns a coded error")
	void aFailureLoadingTheSessionReturnsACodedError() {
		// given a request with a session cookie
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");

		// when the database fails while the session is loaded
		breakSessionStore();
		Response response = teacher.get("/api/me");

		// then the response is 500 with code INTERNAL_ERROR and no internal details in the body
		assertInternalError(response);
	}

	@Test
	@DisplayName("[accounts/R4-S11] The coded error replaces a large response")
	void theCodedErrorReplacesALargeResponse() {
		// given a logged-in admin and a teacher list too large for the response buffer
		for (int i = 0; i < 150; i++) {
			createTeacher("teacher" + i + "@example.com", null);
		}
		TestClient admin = loggedInAdmin();
		List<User> teachers = users.findByRoleOrderByName(Role.TEACHER);
		doAnswer(invocation -> {
			breakSessionStore();
			return teachers;
		}).when(users).findByRoleOrderByName(Role.TEACHER);

		// when the admin requests the list and saving the session fails
		Response response = admin.get("/api/admin/teachers");

		// then the response is 500 with code INTERNAL_ERROR and no internal details in the body
		assertInternalError(response);
	}

	private void breakSessionStore() {
		jdbc.sql("alter table spring_session rename to " + BROKEN_TABLE).update();
	}

	private static void assertInternalError(Response response) {
		String body = response.asString();
		assertEquals(500, response.statusCode(), body);
		assertEquals("INTERNAL_ERROR", response.jsonPath().getString("code"), body);
		assertFalse(body.contains("spring_session"), body);
		assertFalse(body.contains("SQL"), body);
		assertFalse(body.contains("Exception"), body);
		assertFalse(body.contains("db-host-7"), body);
		assertFalse(body.contains("detail"), body);
	}
}
