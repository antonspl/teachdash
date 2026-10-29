package com.teachdash.accounts;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import com.teachdash.TeachdashApplication;
import com.teachdash.support.ApiTest;
import com.teachdash.support.TestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R1. The application is started a second time, against the same (emptied) database, with the
 * admin variables as properties named exactly ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NAME.
 */
class BootstrapTest extends ApiTest {

	@Autowired
	private Environment environment;

	private ConfigurableApplicationContext startApplication(String adminPassword) {
		return new SpringApplicationBuilder(TeachdashApplication.class).run(
				"--server.port=0",
				"--spring.datasource.url=" + environment.getProperty("spring.datasource.url"),
				"--spring.datasource.username=" + environment.getProperty("spring.datasource.username"),
				"--spring.datasource.password=" + environment.getProperty("spring.datasource.password"),
				"--ADMIN_EMAIL=boss@example.com",
				"--ADMIN_PASSWORD=" + adminPassword,
				"--ADMIN_NAME=The Boss");
	}

	private long countUsers() {
		return jdbc.sql("select count(*) from users").query(Long.class).single();
	}

	@Test
	@DisplayName("[accounts/R1-S1] First start creates the admin")
	void firstStartCreatesTheAdmin() {
		// given an empty database and the admin variables are set
		assertEquals(0, countUsers());

		// when the application starts
		try (ConfigurableApplicationContext app = startApplication("first-password-1")) {
			int appPort = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));

			// then an admin account with that email exists and can log in with that password
			assertEquals(1, jdbc.sql("select count(*) from users where role = 'ADMIN' and active "
					+ "and lower(email) = 'boss@example.com'").query(Long.class).single());
			TestClient client = newClient(appPort);
			client.login("boss@example.com", "first-password-1").then().statusCode(200);
			client.get("/api/me").then().statusCode(200)
					.body("role", equalTo("ADMIN"))
					.body("name", equalTo("The Boss"));
		}
	}

	@Test
	@DisplayName("[accounts/R1-S2] Existing admin is not changed")
	void existingAdminIsNotChanged() {
		// given an admin account exists with password "old-password"
		createAdmin("admin@example.com", "old-password");

		// when the application starts with ADMIN_PASSWORD set to a different value
		try (ConfigurableApplicationContext app = startApplication("different-password")) {
			int appPort = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));

			// then no new account is created and the admin still logs in with "old-password"
			assertEquals(1, countUsers());
			newClient(appPort).login("admin@example.com", "old-password").then().statusCode(200);
			newClient(appPort).login("admin@example.com", "different-password").then().statusCode(401);
		}
	}
}
