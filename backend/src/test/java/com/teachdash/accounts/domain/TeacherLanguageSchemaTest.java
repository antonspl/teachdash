package com.teachdash.accounts.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** R13-S2: the migration that backfills teachers. */
class TeacherLanguageSchemaTest {

	// its own database, so it can stop at an earlier version
	private static final String POSTGRES_IMAGE = "postgres:17-alpine";

	@Test
	@DisplayName("[accounts/R13-S2] The migration gives existing teachers English")
	void theMigrationGivesExistingTeachersEnglish() throws SQLException {
		try (PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)) {
			postgres.start();
			// given a database migrated to version 12, holding a teacher and an admin with no language and a student with "sr"
			migrateTo(postgres, "12");
			try (Connection connection = connect(postgres)) {
				insert(connection, "TEACHER", "teacher@example.com", null);
				insert(connection, "ADMIN", "admin@example.com", null);
				insert(connection, "STUDENT", "student@example.com", "sr");
			}

			// when Flyway migrates on (this Flyway has no way to target a missing version 13, so it migrates to latest)
			migrateTo(postgres, "latest");

			// then version 13 is applied, the teacher has "en", the student still has "sr" and the admin has none
			try (Connection connection = connect(postgres)) {
				assertTrue(version13Applied(connection), "migration version 13 should be applied successfully");
				assertEquals("en", language(connection, "teacher@example.com"));
				assertEquals("sr", language(connection, "student@example.com"));
				assertEquals(null, language(connection, "admin@example.com"));
			}
		}
	}

	private static void migrateTo(PostgreSQLContainer postgres, String version) {
		Flyway.configure()
				.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
				.target(version)
				.load()
				.migrate();
	}

	private static Connection connect(PostgreSQLContainer postgres) throws SQLException {
		return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private static void insert(Connection connection, String role, String email, String language) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				insert into users (id, email, name, role, time_zone, active, created_at, native_language)
				values (?, ?, 'Name', ?, 'UTC', true, now(), ?)
				""")) {
			statement.setObject(1, UUID.randomUUID());
			statement.setString(2, email);
			statement.setString(3, role);
			statement.setString(4, language);
			statement.executeUpdate();
		}
	}

	private static boolean version13Applied(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("select count(*) from flyway_schema_history where version = '13' and success")) {
			try (ResultSet rows = statement.executeQuery()) {
				rows.next();
				return rows.getInt(1) == 1;
			}
		}
	}

	private static String language(Connection connection, String email) throws SQLException {
		try (PreparedStatement statement = connection
				.prepareStatement("select native_language from users where email = ?")) {
			statement.setString(1, email);
			try (ResultSet rows = statement.executeQuery()) {
				rows.next();
				return rows.getString(1);
			}
		}
	}
}
