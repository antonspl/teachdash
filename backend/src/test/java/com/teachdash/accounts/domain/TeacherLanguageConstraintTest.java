package com.teachdash.accounts.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import com.teachdash.support.ApiTest;

/** R13-S1: who has a native language is enforced by the database. */
class TeacherLanguageConstraintTest extends ApiTest {

	@Test
	@DisplayName("[accounts/R13-S1] The database enforces who has a language")
	void theDatabaseEnforcesWhoHasALanguage() {
		// given the migrated database

		// when rows are inserted directly: a teacher with no language, a student with no language, an admin with "en", a teacher with "xx", and then a teacher with "en", a student with "en" and an admin with no language
		List<Boolean> accepted = new ArrayList<>();
		accepted.add(insert("TEACHER", "t1@example.com", null));
		accepted.add(insert("STUDENT", "s1@example.com", null));
		accepted.add(insert("ADMIN", "a1@example.com", "en"));
		accepted.add(insert("TEACHER", "t2@example.com", "xx"));
		accepted.add(insert("TEACHER", "t3@example.com", "en"));
		accepted.add(insert("STUDENT", "s3@example.com", "en"));
		accepted.add(insert("ADMIN", "a3@example.com", null));

		// then the first four inserts are rejected and the last three succeed
		assertEquals(List.of(false, false, false, false, true, true, true), accepted);
	}

	private boolean insert(String role, String email, String language) {
		try {
			jdbc.sql("""
					insert into users (id, email, name, role, time_zone, active, created_at, native_language)
					values (:id, :email, 'Name', :role, 'UTC', true, :now, :language)
					""")
					.param("id", UUID.randomUUID())
					.param("email", email)
					.param("role", role)
					.param("now", Timestamp.from(Instant.now(clock)))
					.param("language", language)
					.update();
			return true;
		} catch (DataAccessException e) {
			return false;
		}
	}
}
