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
import java.util.Optional;
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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real app on a random port, shared Postgres container, tables emptied before each test. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
@TestPropertySource(properties = {
		"teachdash.rate-limit.join.max=1000000",
		"teachdash.rate-limit.preview.max=1000000" })
public abstract class ApiTest {

	protected static final String APP_BASE_URL = "http://localhost:5173";

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer("postgres:17-alpine");

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
				  and table_name not in ('flyway_schema_history', 'languages')
				""").query(String.class).list();
		if (!tables.isEmpty()) {
			jdbc.sql("truncate table " + String.join(", ", tables) + " restart identity cascade")
					.update();
		}
	}

	protected TestClient newClient() {
		return new TestClient(port);
	}

	protected long countRows(String table) {
		return jdbc.sql("select count(*) from " + table).query(Long.class).single();
	}

	protected static String tokenFrom(String link) {
		return link.substring(link.indexOf("token=") + "token=".length());
	}

	/** Password null means "not set yet". */
	protected UUID createUser(
			String role,
			String name,
			String email,
			String password,
			boolean active) {
		return createUser(role, name, email, password, active, "en");
	}

	/** The language is stored for students and teachers; an admin must not have one. */
	protected UUID createUser(
			String role,
			String name,
			String email,
			String password,
			boolean active,
			String nativeLanguage) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				insert into users (id, email, name, role, password_hash, time_zone, active, created_at, native_language)
				values (:id, :email, :name, :role, :hash, 'UTC', :active, :now, :language)
				""")
				.param("language", "ADMIN".equals(role) ? null : nativeLanguage)
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

	protected UUID createStudent(String email, String password, String nativeLanguage) {
		return createUser("STUDENT", "Student", email, password, true, nativeLanguage);
	}

	protected UUID createTeacher(String email, String password, String nativeLanguage) {
		return createUser("TEACHER", "Teacher", email, password, true, nativeLanguage);
	}

	/** The stored native language of any user; null for an admin. */
	protected String userLanguage(UUID id) {
		return jdbc.sql("select native_language from users where id = :id")
				.param("id", id)
				.query(String.class)
				.optional()
				.orElse(null);
	}

	protected String studentLanguage(UUID id) {
		return jdbc.sql("select native_language from users where id = :id")
				.param("id", id)
				.query(String.class)
				.single();
	}

	protected void assignStudent(UUID teacherId, UUID studentId) {
		jdbc.sql("""
				insert into teacher_students (teacher_id, student_id, created_at)
				values (:teacher, :student, :now)
				""")
				.param("teacher", teacherId)
				.param("student", studentId)
				.param("now", Timestamp.from(Instant.now(clock)))
				.update();
	}

	/** Inserts the teacher's join link directly and returns its token. */
	protected String createJoinLink(UUID teacherId) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		jdbc.sql("""
				insert into teacher_join_links (teacher_id, token, created_at)
				values (:teacher, :token, :now)
				""")
				.param("teacher", teacherId)
				.param("token", token)
				.param("now", Timestamp.from(Instant.now(clock)))
				.update();
		return token;
	}

	/** The teacher's current join link token, or empty when there is no link. */
	protected Optional<String> findJoinLinkToken(UUID teacherId) {
		return jdbc.sql("select token from teacher_join_links where teacher_id = :teacher")
				.param("teacher", teacherId)
				.query(String.class)
				.optional();
	}

	/** Inserts a course directly; created_at and updated_at are both the current clock time. */
	protected UUID createCourse(
			UUID teacherId,
			String title,
			String instructionLanguage,
			String targetLanguage) {
		UUID id = UUID.randomUUID();
		Timestamp now = Timestamp.from(Instant.now(clock));
		jdbc.sql("""
				insert into courses (id, teacher_id, title, instruction_language, target_language, created_at, updated_at)
				values (:id, :teacher, :title, :instruction, :target, :now, :now)
				""")
				.param("id", id)
				.param("teacher", teacherId)
				.param("title", title)
				.param("instruction", instructionLanguage)
				.param("target", targetLanguage)
				.param("now", now)
				.update();
		return id;
	}

	/** Inserts a course directly with the given id and creation time; updated_at equals created_at. */
	protected UUID createCourse(
			UUID id,
			UUID teacherId,
			String title,
			String instructionLanguage,
			String targetLanguage,
			Instant createdAt) {
		Timestamp created = Timestamp.from(createdAt);
		jdbc.sql("""
				insert into courses (id, teacher_id, title, instruction_language, target_language, created_at, updated_at)
				values (:id, :teacher, :title, :instruction, :target, :created, :created)
				""")
				.param("id", id)
				.param("teacher", teacherId)
				.param("title", title)
				.param("instruction", instructionLanguage)
				.param("target", targetLanguage)
				.param("created", created)
				.update();
		return id;
	}

	protected UUID createCourse(
			UUID teacherId,
			String title,
			String instructionLanguage,
			String targetLanguage,
			Instant createdAt) {
		return createCourse(UUID.randomUUID(), teacherId, title, instructionLanguage, targetLanguage, createdAt);
	}

	/** A course row as stored, read by direct SQL. */
	public record CourseRow(
			UUID teacherId,
			String title,
			String instructionLanguage,
			String targetLanguage,
			Instant createdAt,
			Instant updatedAt) {
	}

	protected CourseRow readCourse(UUID id) {
		return jdbc.sql("""
				select teacher_id, title, instruction_language, target_language, created_at, updated_at
				from courses where id = :id
				""")
				.param("id", id)
				.query((rs, i) -> new CourseRow(
						rs.getObject("teacher_id", UUID.class),
						rs.getString("title"),
						rs.getString("instruction_language"),
						rs.getString("target_language"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.single();
	}

	protected long countCourses(UUID teacherId) {
		return jdbc.sql("select count(*) from courses where teacher_id = :teacher")
				.param("teacher", teacherId)
				.query(Long.class)
				.single();
	}

	/** Inserts a course assignment directly for the course's teacher, created at the current clock time. */
	protected void assignCourse(UUID courseId, UUID studentId) {
		assignCourse(courseId, studentId, Instant.now(clock));
	}

	protected void assignCourse(UUID courseId, UUID studentId, Instant createdAt) {
		jdbc.sql("""
				insert into course_assignments (course_id, student_id, teacher_id, created_at)
				select id, :student, teacher_id, :created from courses where id = :course
				""")
				.param("course", courseId)
				.param("student", studentId)
				.param("created", Timestamp.from(createdAt))
				.update();
	}

	/** A course assignment row as stored, read by direct SQL. */
	public record AssignmentRow(UUID courseId, UUID studentId, UUID teacherId, Instant createdAt) {
	}

	protected List<AssignmentRow> readAssignments() {
		return jdbc.sql("""
				select course_id, student_id, teacher_id, created_at
				from course_assignments order by created_at, course_id, student_id
				""")
				.query((rs, i) -> new AssignmentRow(
						rs.getObject("course_id", UUID.class),
						rs.getObject("student_id", UUID.class),
						rs.getObject("teacher_id", UUID.class),
						rs.getTimestamp("created_at").toInstant()))
				.list();
	}

	/** Inserts a lesson into a course directly; both times are the current clock time. */
	protected UUID createLesson(UUID teacherId, UUID courseId, String title, int position) {
		return createLesson(teacherId, courseId, title, position, Instant.now(clock));
	}

	protected UUID createLesson(
			UUID teacherId,
			UUID courseId,
			String title,
			int position,
			Instant createdAt) {
		UUID id = UUID.randomUUID();
		Timestamp created = Timestamp.from(createdAt);
		jdbc.sql("""
				insert into lessons (id, teacher_id, title, created_at, updated_at, course_id, position)
				values (:id, :teacher, :title, :created, :created, :course, :position)
				""")
				.param("id", id)
				.param("teacher", teacherId)
				.param("title", title)
				.param("created", created)
				.param("course", courseId)
				.param("position", position)
				.update();
		return id;
	}

	protected long countLessons(UUID courseId) {
		return jdbc.sql("select count(*) from lessons where course_id = :course")
				.param("course", courseId)
				.query(Long.class)
				.single();
	}

	/** The positions of the course's lessons, ascending. */
	protected List<Integer> lessonPositions(UUID courseId) {
		return jdbc.sql("select position from lessons where course_id = :course order by position")
				.param("course", courseId)
				.query(Integer.class)
				.list();
	}

	/** Inserts a part into a lesson directly; both times are the current clock time. */
	protected UUID createPart(UUID teacherId, UUID lessonId, String title, int position) {
		UUID id = UUID.randomUUID();
		Timestamp now = Timestamp.from(Instant.now(clock));
		jdbc.sql("""
				insert into parts (id, teacher_id, lesson_id, title, position, created_at, updated_at)
				values (:id, :teacher, :lesson, :title, :position, :now, :now)
				""")
				.param("id", id)
				.param("teacher", teacherId)
				.param("lesson", lessonId)
				.param("title", title)
				.param("position", position)
				.param("now", now)
				.update();
		return id;
	}

	protected long countParts(UUID lessonId) {
		return jdbc.sql("select count(*) from parts where lesson_id = :lesson")
				.param("lesson", lessonId)
				.query(Long.class)
				.single();
	}

	/** The positions of the lesson's parts, ascending. */
	protected List<Integer> partPositions(UUID lessonId) {
		return jdbc.sql("select position from parts where lesson_id = :lesson order by position")
				.param("lesson", lessonId)
				.query(Integer.class)
				.list();
	}

	/** Inserts an item into a part directly: version 1, both times the clock time. */
	protected UUID createItem(UUID teacherId, UUID partId, String type, String payloadJson, int position) {
		UUID id = UUID.randomUUID();
		Timestamp now = Timestamp.from(Instant.now(clock));
		jdbc.sql("""
				insert into items (id, teacher_id, part_id, type, payload, version, position, created_at, updated_at)
				values (:id, :teacher, :part, :type, cast(:payload as jsonb), 1, :position, :now, :now)
				""")
				.param("id", id)
				.param("teacher", teacherId)
				.param("part", partId)
				.param("type", type)
				.param("payload", payloadJson)
				.param("position", position)
				.param("now", now)
				.update();
		return id;
	}

	protected long countItems(UUID partId) {
		return jdbc.sql("select count(*) from items where part_id = :part")
				.param("part", partId)
				.query(Long.class)
				.single();
	}

	/** The positions of the part's items, ascending. */
	protected List<Integer> itemPositions(UUID partId) {
		return jdbc.sql("select position from items where part_id = :part order by position")
				.param("part", partId)
				.query(Integer.class)
				.list();
	}

	/** An item row as stored; the payload is the jsonb text as Postgres prints it. */
	public record ItemRow(
			UUID teacherId,
			UUID partId,
			String type,
			String payload,
			long version,
			int position,
			Instant createdAt,
			Instant updatedAt) {
	}

	protected ItemRow readItem(UUID id) {
		return jdbc.sql("""
				select teacher_id, part_id, type, payload::text as payload, version, position,
				       created_at, updated_at
				from items where id = :id
				""")
				.param("id", id)
				.query((rs, i) -> new ItemRow(
						rs.getObject("teacher_id", UUID.class),
						rs.getObject("part_id", UUID.class),
						rs.getString("type"),
						rs.getString("payload"),
						rs.getLong("version"),
						rs.getInt("position"),
						rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant()))
				.single();
	}

	protected Instant readPartUpdatedAt(UUID partId) {
		return jdbc.sql("select updated_at from parts where id = :id")
				.param("id", partId)
				.query((rs, i) -> rs.getTimestamp("updated_at").toInstant())
				.single();
	}

	protected Instant readLessonUpdatedAt(UUID lessonId) {
		return jdbc.sql("select updated_at from lessons where id = :id")
				.param("id", lessonId)
				.query((rs, i) -> rs.getTimestamp("updated_at").toInstant())
				.single();
	}

	protected TestClient loggedInAdmin() {
		createAdmin("admin@example.com", "admin-password-1");
		return loggedInAs("admin@example.com");
	}

	/** Session is created in the store, not via /auth/login, so tests do not depend on it. */
	protected TestClient loggedInAs(String email) {
		var user = jdbc.sql("select id, role from users where lower(email) = lower(:email)")
				.param("email", email)
				.query((rs, i) -> new String[] { rs.getString("id"), rs.getString("role") })
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
		String cookie = Base64.getEncoder()
				.encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
		return newClient().withCookie("SESSION", cookie);
	}

	/** Inserts a token directly (SHA-256 hex hash stored); does not invalidate older tokens. */
	protected String createToken(UUID userId, String purpose, boolean used, UUID issuerId) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant now = Instant.now(clock);
		jdbc.sql("""
				insert into one_time_tokens (token_hash, user_id, purpose, expires_at, used_at, created_at, issued_by)
				values (:hash, :user, :purpose, :expires, :used, :now, :issuer)
				""")
				.param("hash", sha256Hex(token))
				.param("user", userId)
				.param("purpose", purpose)
				.param("expires", Timestamp.from(now.plus(Duration.ofDays(7))))
				.param("used", used ? Timestamp.from(now) : null)
				.param("now", Timestamp.from(now))
				.param("issuer", issuerId)
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
