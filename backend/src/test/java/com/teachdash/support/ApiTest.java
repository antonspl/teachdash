package com.teachdash.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real app on a random port, shared Postgres container, tables emptied before each test. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
public abstract class ApiTest {

	protected static final String APP_BASE_URL = "http://localhost:5173";

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static {
		POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("APP_BASE_URL", () -> APP_BASE_URL);
	}

	// low cost keeps tests fast; the app's delegating encoder verifies the {bcrypt} prefix
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

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

	protected static String tokenFrom(String link) {
		return link.substring(link.indexOf("token=") + "token=".length());
	}

	/** Password null means "not set yet". */
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
				.param("hash", password == null ? null : "{bcrypt}" + PASSWORD_ENCODER.encode(password))
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

	protected TestClient loggedInAdmin() {
		createAdmin("admin@example.com", "admin-password-1");
		return loggedInAs("admin@example.com");
	}

	/** Session is created in the store, not via /auth/login, so tests do not depend on it. */
	protected TestClient loggedInAs(String email) {
		var user = jdbc.sql("select id, role from users where lower(email) = lower(:email)")
				.param("email", email).query((rs, i) -> new String[] { rs.getString("id"), rs.getString("role") })
				.single();
		String principal = user[0];
		String role = user[1];
		var authorities = AuthorityUtils.createAuthorityList("ROLE_" + role);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				new User(principal, "", authorities), null, authorities);
		org.springframework.session.Session session = sessions.createSession();
		session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
		session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
		session.setMaxInactiveInterval(Duration.ofMinutes(30));
		saveSession(session);
		String cookie = Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
		return newClient().withCookie("SESSION", cookie);
	}

	/** Inserts a token directly (SHA-256 hex hash stored); does not invalidate older tokens. */
	protected String createToken(UUID userId, String purpose, boolean used) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
