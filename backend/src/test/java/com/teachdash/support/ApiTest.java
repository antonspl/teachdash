package com.teachdash.support;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
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

	@Autowired
	private JdbcIndexedSessionRepository sessions;

	protected MutableClock testClock() {
		return (MutableClock) clock;
	}

	@BeforeEach
	void resetClock() {
		testClock().reset();
	}

	@BeforeEach
	void cleanDatabase() {
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
				.param("now", Timestamp.from(Instant.now(clock)))
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

	/** Creates an admin and returns a client with a session for it. */
	protected TestClient loggedInAdmin() {
		createAdmin("admin@example.com", "admin-password-1");
		return loggedInAs("admin@example.com");
	}

	/**
	 * Returns a client with a session for the existing user, created directly in the Spring
	 * Session JDBC store (design D11), never through /api/auth/login. The session holds a
	 * security context for the user (authority ROLE_&lt;role&gt;, principal name = email) and the
	 * client gets the SESSION cookie (Base64 of the session id, as Spring Session writes it) plus
	 * a client-made XSRF-TOKEN cookie/header pair.
	 */
	protected TestClient loggedInAs(String email) {
		String role = jdbc.sql("select role from users where lower(email) = lower(:email)")
				.param("email", email).query(String.class).single();
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				new User(email, "", AuthorityUtils.createAuthorityList("ROLE_" + role)), null,
				AuthorityUtils.createAuthorityList("ROLE_" + role));
		org.springframework.session.Session session = sessions.createSession();
		session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
		session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, email);
		session.setMaxInactiveInterval(Duration.ofMinutes(30));
		saveSession(session);
		String cookie = Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
		return newClient().withCookie("SESSION", cookie);
	}

	/**
	 * Inserts a one-time token directly (design D5: only the SHA-256 hash is stored, hex-encoded),
	 * expiring 7 days after the test clock's now, and returns the raw token. Unlike the service it
	 * does not mark older tokens as used.
	 */
	protected String createToken(UUID userId, String purpose, boolean used) {
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(
				(UUID.randomUUID().toString() + UUID.randomUUID()).substring(0, 32).getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now(clock);
		jdbc.sql("""
				insert into one_time_tokens (token_hash, user_id, purpose, expires_at, used_at, created_at)
				values (:hash, :user, :purpose, :expires, :used, :now)
				""")
				.param("hash", sha256Hex(token))
				.param("user", userId)
				.param("purpose", purpose)
				.param("expires", Timestamp.from(now.plus(Duration.ofDays(7))))
				.param("used", used ? Timestamp.from(now) : null)
				.param("now", Timestamp.from(now))
				.update();
		return token;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void saveSession(org.springframework.session.Session session) {
		((org.springframework.session.SessionRepository) sessions).save(session);
	}

	private static String sha256Hex(String value) {
		try {
			byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(hash);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
