package com.teachdash.accounts.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.teachdash.accounts.persistence.UserRepository;
import com.teachdash.support.ApiTest;

import jakarta.validation.Validator;

/**
 * R1. "The application starts" = running the real bootstrap runner after the tables are emptied.
 */
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

	@Test
	@DisplayName("[accounts/R1-S3] Admin with another email blocks bootstrap")
	void adminWithAnotherEmailBlocksBootstrap() throws Exception {
		// given an admin account exists with an email different from ADMIN_EMAIL
		createAdmin("other@example.com", "old-password");

		// when the application starts
		startApplication();

		// then no account is created and no account with the ADMIN_EMAIL email exists
		assertEquals(1, countUsers());
		assertEquals(0, jdbc.sql("select count(*) from users where lower(email) = 'boss@example.com'")
				.query(Long.class)
				.single());
	}

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock;

	@Autowired
	private Validator validator;

	@Test
	@DisplayName("[accounts/R1-S4] Invalid admin variables stop startup")
	void invalidAdminVariablesStopStartup() {
		// given no admin account exists and ADMIN_PASSWORD is "abc"
		AdminBootstrap invalidBootstrap = new AdminBootstrap(
				users,
				passwordEncoder,
				clock,
				validator,
				"boss@example.com",
				"abc",
				"The Boss");
		assertEquals(0, countUsers());

		// when the application starts
		RuntimeException failure = assertThrows(
				RuntimeException.class,
				() -> invalidBootstrap.run(new DefaultApplicationArguments()));

		// then startup fails with a message naming ADMIN_PASSWORD and not containing "abc", and no account is created
		assertThat(failure.getMessage(), containsString("ADMIN_PASSWORD"));
		assertFalse(failure.getMessage().contains("abc"));
		assertEquals(0, countUsers());
	}

	@Test
	@DisplayName("[accounts/R1-S5] An email the API would reject stops startup")
	void anEmailTheApiWouldRejectStopsStartup() {
		// given no admin account exists and ADMIN_EMAIL is "a,b@x"
		AdminBootstrap invalidBootstrap = new AdminBootstrap(
				users,
				passwordEncoder,
				clock,
				validator,
				"a,b@x",
				"valid-password-1",
				"The Boss");
		assertEquals(0, countUsers());

		// when the application starts
		RuntimeException failure = assertThrows(
				RuntimeException.class,
				() -> invalidBootstrap.run(new DefaultApplicationArguments()));

		// then startup fails with a message naming ADMIN_EMAIL, and no account is created
		assertThat(failure.getMessage(), containsString("ADMIN_EMAIL"));
		assertEquals(0, countUsers());
	}
}
