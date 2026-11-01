package com.teachdash.accounts.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.teachdash.common.ApiException;
import com.teachdash.support.ApiTest;

/** R9-S6 needs a race, so it calls the service directly with an outdated session version. */
class AccountServiceTest extends ApiTest {

	@Autowired
	private AccountService accounts;

	@Test
	@DisplayName("[accounts/R9-S6] Password change from an outdated session changes nothing")
	void passwordChangeFromAnOutdatedSessionChangesNothing() {
		// given a teacher whose session version was raised after the session's check, so the change holds an older version
		UUID teacherId = createTeacher("teacher@example.com", "teacher-pass-1");
		int outdatedVersion = 0;
		jdbc.sql("update users set session_version = session_version + 1 where id = :id")
				.param("id", teacherId)
				.update();
		String hashBefore = jdbc.sql("select password_hash from users where id = :id")
				.param("id", teacherId)
				.query(String.class)
				.single();

		// when the password change runs with that older version
		ApiException rejected = assertThrows(
				ApiException.class,
				() -> accounts.changePassword(teacherId, "teacher-pass-1", "brand-new-pass-1", outdatedVersion));

		// then it is rejected with 401, the old password still works, and the session version does not change
		assertEquals(HttpStatus.UNAUTHORIZED, rejected.status());
		assertEquals(hashBefore, jdbc.sql("select password_hash from users where id = :id")
				.param("id", teacherId)
				.query(String.class)
				.single());
		assertEquals(1, jdbc.sql("select session_version from users where id = :id")
				.param("id", teacherId)
				.query(Integer.class)
				.single());
		newClient().login("teacher@example.com", "teacher-pass-1").then().statusCode(200);
	}
}
