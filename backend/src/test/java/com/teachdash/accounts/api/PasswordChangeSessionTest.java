package com.teachdash.accounts.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

/** R9-S9 needs a request that overlaps the password change, so its session save is done by hand. */
class PasswordChangeSessionTest extends ApiTest {

	@Autowired
	private JdbcIndexedSessionRepository sessionRepository;

	@Test
	@DisplayName("[accounts/R9-S9] A parallel request cannot undo the session replacement")
	void aParallelRequestCannotUndoTheSessionReplacement() {
		// given a logged-in teacher and a parallel request that loaded the session before the password change
		createTeacher("teacher@example.com", "teacher-pass-1");
		TestClient teacher = loggedInAs("teacher@example.com");
		TestClient oldSessionCookie = teacher.snapshot();
		String sessionId = new String(
				Base64.getDecoder().decode(teacher.cookie("SESSION")),
				StandardCharsets.UTF_8);
		Session parallelRequestCopy = sessionRepository.findById(sessionId);
		assertNotNull(parallelRequestCopy);
		parallelRequestCopy.setLastAccessedTime(Instant.now());

		// when the teacher changes the password, and the parallel request then saves its copy of the session
		var change = teacher.post(
				"/api/me/password",
				Map.of("currentPassword", "teacher-pass-1", "newPassword", "brand-new-pass-1"));
		saveCopy(parallelRequestCopy);

		// then the teacher's client, with the cookie from the response, gets 200 and the old session cookie gets 401
		change.then().statusCode(204);
		teacher.get("/api/me").then().statusCode(200);
		oldSessionCookie.get("/api/me").then().statusCode(401);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void saveCopy(Session session) {
		((SessionRepository) sessionRepository).save(session);
	}
}
