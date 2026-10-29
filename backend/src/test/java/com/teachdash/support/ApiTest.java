package com.teachdash.support;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for API tests (design D11): real app on a random port, one Postgres
 * Testcontainer shared by all tests, tables emptied before each test, movable clock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
public abstract class ApiTest {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static {
		POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	protected static final PasswordEncoder PASSWORD_ENCODER =
			PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@LocalServerPort
	protected int port;

	@Autowired
	protected JdbcClient jdbc;

	@Autowired
	protected Clock clock;

	protected MutableClock testClock() {
		return (MutableClock) clock;
	}

	@BeforeEach
	void cleanDatabaseAndClock() {
		testClock().reset();
		List<String> tables = jdbc.sql("""
				select table_name from information_schema.tables
				where table_schema = 'public' and table_type = 'BASE TABLE'
				  and table_name <> 'flyway_schema_history'
				""").query(String.class).list();
		if (!tables.isEmpty()) {
			jdbc.sql("truncate table " + String.join(", ", tables) + " restart identity cascade").update();
		}
	}

	protected TestClient newClient() {
		return new TestClient(port);
	}

	/** Client for another running instance of the app (e.g. started by a test), not logged in. */
	protected TestClient newClient(int otherPort) {
		return new TestClient(otherPort);
	}

	/** Extracts the token from a link like {base}/set-password?token=... */
	protected static String tokenFrom(String link) {
		return link.substring(link.indexOf("token=") + "token=".length());
	}

	protected TestClient loggedInAdmin() {
		createAdmin("admin@example.com", "admin-password-1");
		return loggedInAs("admin@example.com", "admin-password-1");
	}

	/** Inserts a user directly (design D9 schema); password null means "not set yet". */
	protected UUID createUser(String role, String name, String email, String password, boolean active) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				insert into users (id, email, name, role, password_hash, time_zone, active, created_at)
				values (:id, :email, :name, :role, :hash, 'UTC', :active, :now)
				""")
				.param("id", id)
				.param("email", email)
				.param("name", name)
				.param("role", role)
				.param("hash", password == null ? null : PASSWORD_ENCODER.encode(password))
				.param("active", active)
				.param("now", java.sql.Timestamp.from(Instant.now(clock)))
				.update();
		return id;
	}

	protected UUID createAdmin(String email, String password) {
		return createUser("ADMIN", "Admin", email, password, true);
	}

	protected UUID createTeacher(String email, String password) {
		return createUser("TEACHER", "Teacher", email, password, true);
	}

	protected UUID createStudent(String email, String password) {
		return createUser("STUDENT", "Student", email, password, true);
	}

	/** Returns a new client already logged in as the given user. */
	protected TestClient loggedInAs(String email, String password) {
		TestClient client = newClient();
		int status = client.login(email, password).getStatusCode();
		if (status != 200) {
			throw new AssertionError("Setup: login as " + email + " expected 200 but was " + status);
		}
		return client;
	}
}
