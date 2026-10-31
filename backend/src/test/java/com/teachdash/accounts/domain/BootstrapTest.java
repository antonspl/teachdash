package com.teachdash.accounts.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.TestPropertySource;

import com.teachdash.support.ApiTest;

/** R1. "The application starts" = running the real bootstrap runner after the tables are emptied. */
@TestPropertySource(properties = { "ADMIN_EMAIL=boss@example.com", "ADMIN_NAME=The Boss",
		"ADMIN_PASSWORD=different-password" })
class BootstrapTest extends ApiTest {

	@Autowired
	private AdminBootstrap bootstrap;

	private void startApplication() throws Exception {
		bootstrap.run(new DefaultApplicationArguments());
	}

	private long countUsers() {
		return jdbc.sql("select count(*) from users").query(Long.class).single();
	}

	private Map<String, Object> adminRow() {
		return jdbc.sql("""
				select email, name, password_hash, role, active, created_at from users
				where email = 'boss@example.com'
				""").query().singleRow();
	}

	@Test
	@DisplayName("[accounts/R1-S1] First start creates the admin")
	void firstStartCreatesTheAdmin() throws Exception {
		// given an empty database and the admin variables are set
		assertEquals(0, countUsers());

		// when the application starts
		startApplication();

		// then an admin account with that email exists and can log in with that password
		assertEquals(1, jdbc.sql("select count(*) from users where role = 'ADMIN' and active "
				+ "and lower(email) = 'boss@example.com'").query(Long.class).single());
		var client = newClient();
		client.login("boss@example.com", "different-password").then().statusCode(200);
		client.get("/api/me").then().statusCode(200)
				.body("role", equalTo("ADMIN"))
				.body("name", equalTo("The Boss"));
	}

	@Test
	@DisplayName("[accounts/R1-S2] Existing admin is not changed")
	void existingAdminIsNotChanged() throws Exception {
		// given an admin account exists with the ADMIN_EMAIL email and password "old-password"
		createAdmin("boss@example.com", "old-password");
		Map<String, Object> before = adminRow();

		// when the application starts with ADMIN_PASSWORD set to a different value
		startApplication();

		// then no new account is created, the admin account is unchanged, and it still logs in with "old-password"
		assertEquals(1, countUsers());
		assertEquals(before, adminRow());
		newClient().login("boss@example.com", "old-password").then().statusCode(200);
		newClient().login("boss@example.com", "different-password").then().statusCode(401);
	}
}
